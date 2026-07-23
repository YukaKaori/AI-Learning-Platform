package com.yuka.ailearningserver.analytics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuka.ailearningserver.ai.entity.AiConversation;
import com.yuka.ailearningserver.ai.mapper.AiConversationMapper;
import com.yuka.ailearningserver.analytics.dto.ActivityDayResponse;
import com.yuka.ailearningserver.analytics.dto.AnalyticsSummaryResponse;
import com.yuka.ailearningserver.analytics.dto.SubjectShareResponse;
import com.yuka.ailearningserver.calendar.entity.StudySession;
import com.yuka.ailearningserver.calendar.mapper.StudySessionMapper;
import com.yuka.ailearningserver.common.exception.BusinessException;
import com.yuka.ailearningserver.flashcard.entity.ReviewLog;
import com.yuka.ailearningserver.flashcard.mapper.ReviewLogMapper;
import com.yuka.ailearningserver.subject.entity.Subject;
import com.yuka.ailearningserver.subject.mapper.SubjectMapper;
import com.yuka.ailearningserver.task.entity.LearningTask;
import com.yuka.ailearningserver.task.entity.TaskStatus;
import com.yuka.ailearningserver.task.mapper.LearningTaskMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * On-the-fly read model over study_sessions / learning_tasks /
 * ai_conversations — no tables of its own (see package-info). Aggregation
 * happens in memory over slim column-projected selects, mirroring
 * {@code SubjectService.deriveAll()}: dialect-free and bounded by the user's
 * own data volume.
 *
 * <p>Attribution rules (documented, applied consistently): a session's
 * minutes count on the day it <em>ends</em>, and only sessions that have
 * already ended count — calendar sessions planned in the future are never
 * study time. The streak is the run of consecutive days with at least one
 * session ending on them, anchored at today or yesterday (server default
 * timezone; client-timezone support is a documented extension point).
 */
@Service
public class AnalyticsService {

    private static final int MAX_WINDOW_DAYS = 90;
    private static final int WEEK_DAYS = 7;

    private final StudySessionMapper sessionMapper;
    private final LearningTaskMapper taskMapper;
    private final AiConversationMapper conversationMapper;
    private final SubjectMapper subjectMapper;
    private final ReviewLogMapper reviewLogMapper;

    public AnalyticsService(StudySessionMapper sessionMapper, LearningTaskMapper taskMapper,
                            AiConversationMapper conversationMapper, SubjectMapper subjectMapper,
                            ReviewLogMapper reviewLogMapper) {
        this.sessionMapper = sessionMapper;
        this.taskMapper = taskMapper;
        this.conversationMapper = conversationMapper;
        this.subjectMapper = subjectMapper;
        this.reviewLogMapper = reviewLogMapper;
    }

    public AnalyticsSummaryResponse summary(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime weekStart = today.minusDays(WEEK_DAYS - 1).atStartOfDay();
        LocalDateTime previousWeekStart = today.minusDays(2L * WEEK_DAYS - 1).atStartOfDay();

        long weekMinutes = 0;
        long previousWeekMinutes = 0;
        for (StudySession session : endedSessions(userId, previousWeekStart, now)) {
            long minutes = Duration.between(session.getStartsAt(), session.getEndsAt()).toMinutes();
            if (!session.getEndsAt().isBefore(weekStart)) {
                weekMinutes += minutes;
            } else {
                previousWeekMinutes += minutes;
            }
        }
        Integer weekDeltaPercent = previousWeekMinutes > 0
                ? Math.toIntExact(Math.round((weekMinutes - previousWeekMinutes) * 100.0 / previousWeekMinutes))
                : null;

        long totalTasks = taskMapper.selectCount(new LambdaQueryWrapper<LearningTask>()
                .eq(LearningTask::getUserId, userId));
        Integer taskCompletionPercent = null;
        if (totalTasks > 0) {
            long doneTasks = taskMapper.selectCount(new LambdaQueryWrapper<LearningTask>()
                    .eq(LearningTask::getUserId, userId)
                    .eq(LearningTask::getStatus, TaskStatus.DONE));
            taskCompletionPercent = Math.toIntExact(Math.round(doneTasks * 100.0 / totalTasks));
        }

        long aiChatsThisWeek = conversationMapper.selectCount(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getUserId, userId)
                .ge(AiConversation::getCreatedAt, weekStart));

        List<ReviewLog> weekReviews = reviewLogMapper.selectList(new LambdaQueryWrapper<ReviewLog>()
                .select(ReviewLog::getRating, ReviewLog::getElapsedDays)
                .eq(ReviewLog::getUserId, userId)
                .ge(ReviewLog::getReviewedAt, weekStart));
        int matureReviews = 0;
        int remembered = 0;
        for (ReviewLog review : weekReviews) {
            // Retention is measured only on genuine recall attempts: cards tested
            // after a real interval (elapsed ≥ 1 day). First-introductions
            // (elapsed null) and same-day learning reps (elapsed 0) are excluded.
            if (review.getElapsedDays() != null && review.getElapsedDays() >= 1) {
                matureReviews++;
                if (review.getRating() >= 2) { // Hard/Good/Easy = recalled; Again = lapse
                    remembered++;
                }
            }
        }
        Integer retentionPercent = matureReviews > 0
                ? Math.toIntExact(Math.round(remembered * 100.0 / matureReviews))
                : null;

        return new AnalyticsSummaryResponse(Math.toIntExact(weekMinutes), weekDeltaPercent,
                streakDays(userId), taskCompletionPercent, Math.toIntExact(aiChatsThisWeek),
                weekReviews.size(), retentionPercent);
    }

    /**
     * Zero-filled per-day activity for the last {@code days} days, oldest
     * first, ending today — one series feeds both the bar chart and the
     * heatmap. Shared with the workspace summary ({@code days = 7}).
     */
    public List<ActivityDayResponse> activity(Long userId, int days) {
        requireValidWindow(days);
        LocalDateTime now = LocalDateTime.now();
        LocalDate firstDay = now.toLocalDate().minusDays(days - 1);

        int[] minutes = new int[days];
        int[] sessions = new int[days];
        for (StudySession session : endedSessions(userId, firstDay.atStartOfDay(), now)) {
            int index = Math.toIntExact(ChronoUnit.DAYS.between(firstDay, session.getEndsAt().toLocalDate()));
            minutes[index] += Duration.between(session.getStartsAt(), session.getEndsAt()).toMinutes();
            sessions[index]++;
        }

        // No upper bound: a review always happens at/before now (unlike sessions,
        // which can be planned in the future). Bounding by `now` would only add a
        // sub-second race — a just-written reviewed_at can round up (MySQL DATETIME)
        // to a second fractionally ahead of `now` and be dropped for ~1s, briefly
        // disagreeing with the review summary. The window floor is enough.
        int[] reviews = new int[days];
        for (ReviewLog review : reviewLogMapper.selectList(new LambdaQueryWrapper<ReviewLog>()
                .select(ReviewLog::getReviewedAt)
                .eq(ReviewLog::getUserId, userId)
                .ge(ReviewLog::getReviewedAt, firstDay.atStartOfDay()))) {
            int i = Math.toIntExact(ChronoUnit.DAYS.between(firstDay, review.getReviewedAt().toLocalDate()));
            if (i >= 0 && i < days) { // guard a clock-skewed/future-dated row from overflowing the window
                reviews[i]++;
            }
        }

        return IntStream.range(0, days)
                .mapToObj(i -> new ActivityDayResponse(
                        firstDay.plusDays(i).toString(), minutes[i], sessions[i], reviews[i]))
                .toList();
    }

    /**
     * Study minutes per subject over the last {@code days} days, largest
     * share first. Minutes from sessions without a subject (or whose subject
     * has since been deleted) land in a single null-subject bucket so the
     * shares always sum to the real total.
     */
    public List<SubjectShareResponse> subjectShares(Long userId, int days) {
        requireValidWindow(days);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.toLocalDate().minusDays(days - 1).atStartOfDay();

        Map<Long, Long> minutesBySubject = new HashMap<>();
        for (StudySession session : endedSessions(userId, from, now)) {
            minutesBySubject.merge(session.getSubjectId(),
                    Duration.between(session.getStartsAt(), session.getEndsAt()).toMinutes(), Long::sum);
        }
        if (minutesBySubject.isEmpty()) {
            return List.of();
        }

        Set<Long> subjectIds = new HashSet<>(minutesBySubject.keySet());
        subjectIds.remove(null);
        Map<Long, Subject> subjects = subjectIds.isEmpty() ? Map.of()
                : subjectMapper.selectBatchIds(subjectIds).stream()
                        .collect(HashMap::new, (map, subject) -> map.put(subject.getId(), subject), HashMap::putAll);

        Map<Long, Long> consolidated = new HashMap<>();
        minutesBySubject.forEach((subjectId, minutes) -> consolidated.merge(
                subjectId != null && subjects.containsKey(subjectId) ? subjectId : null, minutes, Long::sum));

        return consolidated.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .map(entry -> {
                    Subject subject = entry.getKey() != null ? subjects.get(entry.getKey()) : null;
                    return new SubjectShareResponse(
                            subject != null ? String.valueOf(subject.getId()) : null,
                            subject != null ? subject.getName() : null,
                            subject != null ? subject.getColor() : null,
                            entry.getValue());
                })
                .toList();
    }

    /**
     * Consecutive days with at least one study session ending on them,
     * anchored at today or yesterday (an unfinished today doesn't break the
     * streak). Public so the workspace summary reuses the same computation.
     */
    public int streakDays(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        Set<LocalDate> studyDays = new HashSet<>();
        sessionMapper.selectList(new LambdaQueryWrapper<StudySession>()
                        .select(StudySession::getEndsAt)
                        .eq(StudySession::getUserId, userId)
                        .le(StudySession::getEndsAt, now))
                .forEach(session -> studyDays.add(session.getEndsAt().toLocalDate()));

        LocalDate today = now.toLocalDate();
        LocalDate cursor = studyDays.contains(today) ? today
                : studyDays.contains(today.minusDays(1)) ? today.minusDays(1) : null;
        int streak = 0;
        while (cursor != null && studyDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /** Sessions that have already ended, with {@code endsAt} in {@code [from, to]}. */
    private List<StudySession> endedSessions(Long userId, LocalDateTime from, LocalDateTime to) {
        return sessionMapper.selectList(new LambdaQueryWrapper<StudySession>()
                .select(StudySession::getSubjectId, StudySession::getStartsAt, StudySession::getEndsAt)
                .eq(StudySession::getUserId, userId)
                .ge(StudySession::getEndsAt, from)
                .le(StudySession::getEndsAt, to));
    }

    private static void requireValidWindow(int days) {
        if (days < 1 || days > MAX_WINDOW_DAYS) {
            throw new BusinessException(AnalyticsErrorCode.ANALYTICS_RANGE_INVALID);
        }
    }
}
