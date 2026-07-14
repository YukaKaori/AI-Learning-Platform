package com.yuka.ailearningserver.calendar;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuka.ailearningserver.calendar.dto.CreateStudySessionRequest;
import com.yuka.ailearningserver.calendar.dto.StudySessionResponse;
import com.yuka.ailearningserver.calendar.dto.UpdateStudySessionRequest;
import com.yuka.ailearningserver.calendar.entity.StudySession;
import com.yuka.ailearningserver.calendar.mapper.StudySessionMapper;
import com.yuka.ailearningserver.common.OwnershipGuard;
import com.yuka.ailearningserver.common.exception.BusinessException;
import com.yuka.ailearningserver.subject.SubjectService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Study-session CRUD behind the calendar. Listing is always windowed
 * ({@code from}/{@code to} are mandatory) and returns every session
 * overlapping the window, so month views render sessions that cross the
 * window boundary.
 */
@Service
public class StudySessionService {

    private final StudySessionMapper sessionMapper;
    private final SubjectService subjectService;

    public StudySessionService(StudySessionMapper sessionMapper, SubjectService subjectService) {
        this.sessionMapper = sessionMapper;
        this.subjectService = subjectService;
    }

    public List<StudySessionResponse> list(Long userId, Long from, Long to) {
        if (from >= to) {
            throw new BusinessException(CalendarErrorCode.SESSION_WINDOW_INVALID);
        }
        return sessionMapper.selectList(new LambdaQueryWrapper<StudySession>()
                        .eq(StudySession::getUserId, userId)
                        .lt(StudySession::getStartsAt, toLocalDateTime(to))
                        .gt(StudySession::getEndsAt, toLocalDateTime(from))
                        .orderByAsc(StudySession::getStartsAt))
                .stream()
                .map(StudySessionResponse::from)
                .toList();
    }

    public StudySessionResponse create(Long userId, CreateStudySessionRequest request) {
        LocalDateTime startsAt = toLocalDateTime(request.startsAt());
        LocalDateTime endsAt = toLocalDateTime(request.endsAt());
        requireValidRange(startsAt, endsAt);
        StudySession session = new StudySession();
        session.setUserId(userId);
        session.setSubjectId(subjectService.resolveOwnedSubjectId(userId, request.subjectId()));
        session.setTitle(request.title());
        session.setStartsAt(startsAt);
        session.setEndsAt(endsAt);
        sessionMapper.insert(session);
        return StudySessionResponse.from(session);
    }

    public StudySessionResponse update(Long userId, Long id, UpdateStudySessionRequest request) {
        StudySession session = requireOwned(userId, id);
        if (request.title() != null) {
            session.setTitle(request.title().isBlank() ? null : request.title());
        }
        if (request.subjectId() != null) {
            session.setSubjectId(subjectService.resolveOwnedSubjectId(userId, request.subjectId()));
        }
        LocalDateTime startsAt = request.startsAt() != null
                ? toLocalDateTime(request.startsAt()) : session.getStartsAt();
        LocalDateTime endsAt = request.endsAt() != null
                ? toLocalDateTime(request.endsAt()) : session.getEndsAt();
        requireValidRange(startsAt, endsAt);
        session.setStartsAt(startsAt);
        session.setEndsAt(endsAt);
        sessionMapper.updateById(session);
        return StudySessionResponse.from(session);
    }

    public void delete(Long userId, Long id) {
        StudySession session = requireOwned(userId, id);
        sessionMapper.deleteById(session.getId());
    }

    private StudySession requireOwned(Long userId, Long id) {
        return OwnershipGuard.require(sessionMapper.selectById(id), StudySession::getUserId, userId,
                CalendarErrorCode.SESSION_NOT_FOUND, CalendarErrorCode.SESSION_ACCESS_DENIED);
    }

    private static void requireValidRange(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new BusinessException(CalendarErrorCode.SESSION_TIME_INVALID);
        }
    }

    private static LocalDateTime toLocalDateTime(Long epochMilli) {
        return Instant.ofEpochMilli(epochMilli).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
