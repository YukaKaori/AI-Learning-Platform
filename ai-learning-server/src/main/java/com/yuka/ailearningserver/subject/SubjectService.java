package com.yuka.ailearningserver.subject;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yuka.ailearningserver.ai.entity.AiConversation;
import com.yuka.ailearningserver.ai.mapper.AiConversationMapper;
import com.yuka.ailearningserver.calendar.entity.StudySession;
import com.yuka.ailearningserver.calendar.mapper.StudySessionMapper;
import com.yuka.ailearningserver.common.OwnershipGuard;
import com.yuka.ailearningserver.common.exception.BusinessException;
import com.yuka.ailearningserver.flashcard.entity.FlashcardDeck;
import com.yuka.ailearningserver.flashcard.mapper.FlashcardDeckMapper;
import com.yuka.ailearningserver.material.entity.LearningMaterial;
import com.yuka.ailearningserver.material.mapper.LearningMaterialMapper;
import com.yuka.ailearningserver.note.entity.Note;
import com.yuka.ailearningserver.note.mapper.NoteMapper;
import com.yuka.ailearningserver.subject.dto.CreateSubjectRequest;
import com.yuka.ailearningserver.subject.dto.SubjectResponse;
import com.yuka.ailearningserver.subject.dto.UpdateSubjectRequest;
import com.yuka.ailearningserver.subject.entity.Subject;
import com.yuka.ailearningserver.subject.entity.SubjectStatus;
import com.yuka.ailearningserver.subject.mapper.SubjectMapper;
import com.yuka.ailearningserver.task.entity.LearningTask;
import com.yuka.ailearningserver.task.mapper.LearningTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Subject CRUD plus the derived aggregates (counts, study time) the product
 * shows on every subject card. Deleting a subject soft-deletes its materials
 * (existentially owned, {@code subject_id NOT NULL}) and unlinks everything
 * else (notes, decks, tasks, sessions, AI conversations keep the user's
 * content, {@code subjectId} becomes null) — see docs/architecture.md.
 */
@Service
public class SubjectService {

    private final SubjectMapper subjectMapper;
    private final LearningMaterialMapper materialMapper;
    private final NoteMapper noteMapper;
    private final FlashcardDeckMapper deckMapper;
    private final LearningTaskMapper taskMapper;
    private final StudySessionMapper sessionMapper;
    private final AiConversationMapper conversationMapper;

    public SubjectService(SubjectMapper subjectMapper, LearningMaterialMapper materialMapper,
                          NoteMapper noteMapper, FlashcardDeckMapper deckMapper,
                          LearningTaskMapper taskMapper, StudySessionMapper sessionMapper,
                          AiConversationMapper conversationMapper) {
        this.subjectMapper = subjectMapper;
        this.materialMapper = materialMapper;
        this.noteMapper = noteMapper;
        this.deckMapper = deckMapper;
        this.taskMapper = taskMapper;
        this.sessionMapper = sessionMapper;
        this.conversationMapper = conversationMapper;
    }

    public List<SubjectResponse> list(Long userId) {
        List<Subject> subjects = subjectMapper.selectList(new LambdaQueryWrapper<Subject>()
                .eq(Subject::getUserId, userId)
                .orderByAsc(Subject::getStatus)
                .orderByDesc(Subject::getUpdatedAt));
        Map<Long, SubjectResponse.Derived> derived = deriveAll(userId);
        return subjects.stream()
                .map(subject -> SubjectResponse.from(subject,
                        derived.getOrDefault(subject.getId(), SubjectResponse.Derived.NONE)))
                .toList();
    }

    public SubjectResponse get(Long userId, Long id) {
        Subject subject = requireOwned(userId, id);
        return SubjectResponse.from(subject,
                deriveAll(userId).getOrDefault(subject.getId(), SubjectResponse.Derived.NONE));
    }

    public SubjectResponse create(Long userId, CreateSubjectRequest request) {
        Subject subject = new Subject();
        subject.setUserId(userId);
        subject.setName(request.name());
        subject.setColor(request.color());
        subject.setIcon(request.icon());
        subject.setDescription(request.description());
        subject.setStatus(SubjectStatus.ACTIVE);
        subject.setProgress(0);
        subjectMapper.insert(subject);
        return SubjectResponse.from(subject, SubjectResponse.Derived.NONE);
    }

    public SubjectResponse update(Long userId, Long id, UpdateSubjectRequest request) {
        Subject subject = requireOwned(userId, id);
        if (request.name() != null && !request.name().isBlank()) {
            subject.setName(request.name());
        }
        if (request.color() != null) {
            subject.setColor(request.color());
        }
        if (request.icon() != null) {
            subject.setIcon(request.icon());
        }
        if (request.description() != null) {
            subject.setDescription(request.description());
        }
        if (request.status() != null) {
            try {
                subject.setStatus(SubjectStatus.valueOf(request.status().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(SubjectErrorCode.SUBJECT_STATUS_INVALID);
            }
        }
        if (request.progress() != null) {
            subject.setProgress(request.progress());
        }
        subjectMapper.updateById(subject);
        return SubjectResponse.from(subject,
                deriveAll(userId).getOrDefault(subject.getId(), SubjectResponse.Derived.NONE));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Subject subject = requireOwned(userId, id);
        materialMapper.delete(new LambdaQueryWrapper<LearningMaterial>()
                .eq(LearningMaterial::getUserId, userId)
                .eq(LearningMaterial::getSubjectId, subject.getId()));
        noteMapper.update(null, new LambdaUpdateWrapper<Note>()
                .eq(Note::getUserId, userId)
                .eq(Note::getSubjectId, subject.getId())
                .set(Note::getSubjectId, null));
        deckMapper.update(null, new LambdaUpdateWrapper<FlashcardDeck>()
                .eq(FlashcardDeck::getUserId, userId)
                .eq(FlashcardDeck::getSubjectId, subject.getId())
                .set(FlashcardDeck::getSubjectId, null));
        taskMapper.update(null, new LambdaUpdateWrapper<LearningTask>()
                .eq(LearningTask::getUserId, userId)
                .eq(LearningTask::getSubjectId, subject.getId())
                .set(LearningTask::getSubjectId, null));
        sessionMapper.update(null, new LambdaUpdateWrapper<StudySession>()
                .eq(StudySession::getUserId, userId)
                .eq(StudySession::getSubjectId, subject.getId())
                .set(StudySession::getSubjectId, null));
        conversationMapper.update(null, new LambdaUpdateWrapper<AiConversation>()
                .eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getSubjectId, subject.getId())
                .set(AiConversation::getSubjectId, null));
        subjectMapper.deleteById(subject.getId());
    }

    /** Shared ownership check for features that hang content off a subject. */
    public Subject requireOwned(Long userId, Long id) {
        return OwnershipGuard.require(subjectMapper.selectById(id), Subject::getUserId, userId,
                SubjectErrorCode.SUBJECT_NOT_FOUND, SubjectErrorCode.SUBJECT_ACCESS_DENIED);
    }

    /**
     * Resolves an optional wire subject id (string form, per the string-id API
     * convention) to a validated owned subject id. Null or blank means "no
     * subject" and resolves to null; anything else must reference a subject
     * owned by {@code userId}. Shared by every feature with an optional
     * subject link (notes, decks, tasks, sessions).
     */
    public Long resolveOwnedSubjectId(Long userId, String subjectId) {
        Subject subject = resolveOwnedSubject(userId, subjectId);
        return subject != null ? subject.getId() : null;
    }

    /**
     * Same contract as {@link #resolveOwnedSubjectId}, returning the entity —
     * for callers that also need the subject's fields (e.g. AI conversations
     * persisting a {@code subjectName} display snapshot).
     */
    public Subject resolveOwnedSubject(Long userId, String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            return null;
        }
        long id;
        try {
            id = Long.parseLong(subjectId.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(SubjectErrorCode.SUBJECT_NOT_FOUND);
        }
        return requireOwned(userId, id);
    }

    /**
     * Per-subject aggregates for one user, computed from slim column-projected
     * selects and grouped in memory — dialect-free and bounded by the user's
     * own data volume.
     */
    private Map<Long, SubjectResponse.Derived> deriveAll(Long userId) {
        Map<Long, int[]> counts = new HashMap<>(); // [materials, notes, decks]
        materialMapper.selectList(new LambdaQueryWrapper<LearningMaterial>()
                        .select(LearningMaterial::getSubjectId)
                        .eq(LearningMaterial::getUserId, userId))
                .forEach(m -> counts.computeIfAbsent(m.getSubjectId(), k -> new int[3])[0]++);
        noteMapper.selectList(new LambdaQueryWrapper<Note>()
                        .select(Note::getSubjectId)
                        .eq(Note::getUserId, userId)
                        .isNotNull(Note::getSubjectId))
                .forEach(n -> counts.computeIfAbsent(n.getSubjectId(), k -> new int[3])[1]++);
        deckMapper.selectList(new LambdaQueryWrapper<FlashcardDeck>()
                        .select(FlashcardDeck::getSubjectId)
                        .eq(FlashcardDeck::getUserId, userId)
                        .isNotNull(FlashcardDeck::getSubjectId))
                .forEach(d -> counts.computeIfAbsent(d.getSubjectId(), k -> new int[3])[2]++);

        Map<Long, long[]> study = new HashMap<>(); // [minutes, lastStudiedAt epoch ms]
        sessionMapper.selectList(new LambdaQueryWrapper<StudySession>()
                        .select(StudySession::getSubjectId, StudySession::getStartsAt, StudySession::getEndsAt)
                        .eq(StudySession::getUserId, userId)
                        .isNotNull(StudySession::getSubjectId))
                .forEach(s -> {
                    long[] slot = study.computeIfAbsent(s.getSubjectId(), k -> new long[2]);
                    slot[0] += Duration.between(s.getStartsAt(), s.getEndsAt()).toMinutes();
                    long endedAt = s.getEndsAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    slot[1] = Math.max(slot[1], endedAt);
                });

        Map<Long, SubjectResponse.Derived> derived = new HashMap<>();
        counts.keySet().forEach(subjectId -> derived.put(subjectId, null));
        study.keySet().forEach(subjectId -> derived.put(subjectId, null));
        derived.replaceAll((subjectId, ignored) -> {
            int[] c = counts.getOrDefault(subjectId, new int[3]);
            long[] s = study.get(subjectId);
            return new SubjectResponse.Derived(c[0], c[1], c[2],
                    s != null ? s[0] : 0, s != null && s[1] > 0 ? s[1] : null);
        });
        return derived;
    }
}
