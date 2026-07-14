package com.yuka.ailearningserver.analytics;

import com.yuka.ailearningserver.ai.dto.CreateConversationRequest;
import com.yuka.ailearningserver.ai.service.AiConversationService;
import com.yuka.ailearningserver.analytics.dto.ActivityDayResponse;
import com.yuka.ailearningserver.analytics.dto.AnalyticsSummaryResponse;
import com.yuka.ailearningserver.analytics.dto.SubjectShareResponse;
import com.yuka.ailearningserver.calendar.StudySessionService;
import com.yuka.ailearningserver.calendar.dto.CreateStudySessionRequest;
import com.yuka.ailearningserver.common.exception.BusinessException;
import com.yuka.ailearningserver.subject.SubjectService;
import com.yuka.ailearningserver.subject.dto.CreateSubjectRequest;
import com.yuka.ailearningserver.subject.dto.SubjectResponse;
import com.yuka.ailearningserver.task.TaskService;
import com.yuka.ailearningserver.task.dto.CreateTaskRequest;
import com.yuka.ailearningserver.task.dto.TaskResponse;
import com.yuka.ailearningserver.task.dto.UpdateTaskRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Analytics read-model behavior: the D6 streak rule (empty / gap /
 * today-missing), rolling-week aggregation with a null delta when there is no
 * baseline, zero-filled activity buckets, subject shares, and the "only
 * sessions that already ended count" attribution rule.
 */
@SpringBootTest
@ActiveProfiles("test")
class AnalyticsServiceTest {

    private static final Long USER = 1L;
    private static final Long OTHER_USER = 2L;
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 3_600_000L;
    private static final long DAY = 86_400_000L;

    @Autowired
    private AnalyticsService analyticsService;
    @Autowired
    private StudySessionService sessionService;
    @Autowired
    private SubjectService subjectService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private AiConversationService conversationService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long base;

    @BeforeEach
    void cleanTables() {
        for (String table : List.of("subjects", "study_sessions", "learning_tasks",
                "ai_conversations", "ai_messages")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
        // Whole-second anchor — DATETIME columns round fractional seconds on
        // persist, which would skew derived durations by ±1s in assertions.
        base = Instant.now().truncatedTo(ChronoUnit.SECONDS).toEpochMilli();
    }

    @Test
    void streakIsZeroWithoutSessions() {
        assertThat(analyticsService.streakDays(USER)).isZero();
    }

    @Test
    void streakCountsConsecutiveDaysEndingToday() {
        endedSession(USER, null, base, 30);
        endedSession(USER, null, base - DAY, 30);
        endedSession(USER, null, base - 2 * DAY, 30);
        assertThat(analyticsService.streakDays(USER)).isEqualTo(3);
    }

    @Test
    void streakSurvivesAnUnfinishedToday() {
        endedSession(USER, null, base - DAY, 30);
        endedSession(USER, null, base - 2 * DAY, 30);
        assertThat(analyticsService.streakDays(USER)).isEqualTo(2);
    }

    @Test
    void streakBreaksOnAGap() {
        endedSession(USER, null, base, 30);
        endedSession(USER, null, base - 2 * DAY, 30);
        assertThat(analyticsService.streakDays(USER)).isEqualTo(1);
    }

    @Test
    void plannedFutureSessionsNeverCountAsStudyTime() {
        sessionService.create(USER, new CreateStudySessionRequest("planned", null, base + HOUR, base + 2 * HOUR));
        assertThat(analyticsService.streakDays(USER)).isZero();
        assertThat(analyticsService.summary(USER).weekMinutes()).isZero();
        assertThat(analyticsService.activity(USER, 7)).allSatisfy(day -> assertThat(day.minutes()).isZero());
    }

    @Test
    void summaryIsHonestZerosAndNullsOnAnEmptyAccount() {
        AnalyticsSummaryResponse summary = analyticsService.summary(USER);
        assertThat(summary.weekMinutes()).isZero();
        assertThat(summary.weekDeltaPercent()).isNull();
        assertThat(summary.streakDays()).isZero();
        assertThat(summary.taskCompletionPercent()).isNull();
        assertThat(summary.aiChatsThisWeek()).isZero();
    }

    @Test
    void summaryComparesRollingWeekAgainstThePreviousOne() {
        endedSession(USER, null, base - HOUR, 60);      // this week
        endedSession(USER, null, base - 10 * DAY, 30);  // previous week
        endedSession(OTHER_USER, null, base - HOUR, 45); // someone else's

        TaskResponse done = taskService.create(USER, new CreateTaskRequest("done one", null, null, null, null));
        taskService.create(USER, new CreateTaskRequest("open one", null, null, null, null));
        taskService.update(USER, Long.valueOf(done.id()),
                new UpdateTaskRequest(null, null, "done", null, null, null));

        conversationService.create(USER, new CreateConversationRequest(null, null, null));
        var oldConversation = conversationService.create(USER, new CreateConversationRequest(null, null, null));
        jdbcTemplate.update("UPDATE ai_conversations SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.ofEpochMilli(base - 10 * DAY)), Long.valueOf(oldConversation.id()));

        AnalyticsSummaryResponse summary = analyticsService.summary(USER);
        assertThat(summary.weekMinutes()).isEqualTo(60);
        assertThat(summary.weekDeltaPercent()).isEqualTo(100);
        assertThat(summary.taskCompletionPercent()).isEqualTo(50);
        assertThat(summary.aiChatsThisWeek()).isEqualTo(1);
    }

    @Test
    void activityZeroFillsTheWindowAndValidatesIt() {
        endedSession(USER, null, base - DAY, 45);

        List<ActivityDayResponse> days = analyticsService.activity(USER, 3);
        LocalDate today = LocalDate.now();
        assertThat(days).extracting(ActivityDayResponse::date)
                .containsExactly(today.minusDays(2).toString(), today.minusDays(1).toString(), today.toString());
        assertThat(days).extracting(ActivityDayResponse::minutes).containsExactly(0, 45, 0);
        assertThat(days).extracting(ActivityDayResponse::sessions).containsExactly(0, 1, 0);

        for (int invalid : new int[]{0, 91}) {
            assertThatThrownBy(() -> analyticsService.activity(USER, invalid))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(AnalyticsErrorCode.ANALYTICS_RANGE_INVALID));
        }
    }

    @Test
    void subjectSharesBucketLinkedAndUnlinkedMinutes() {
        SubjectResponse subject = subjectService.create(USER,
                new CreateSubjectRequest("ML", "#5e6ad2", null, null));
        endedSession(USER, subject.id(), base - HOUR, 60);
        endedSession(USER, null, base - 2 * HOUR, 30);
        endedSession(OTHER_USER, null, base - HOUR, 45);

        List<SubjectShareResponse> shares = analyticsService.subjectShares(USER, 30);
        assertThat(shares).hasSize(2);
        assertThat(shares.getFirst().subjectId()).isEqualTo(subject.id());
        assertThat(shares.getFirst().subjectName()).isEqualTo("ML");
        assertThat(shares.getFirst().color()).isEqualTo("#5e6ad2");
        assertThat(shares.getFirst().minutes()).isEqualTo(60);
        assertThat(shares.getLast().subjectId()).isNull();
        assertThat(shares.getLast().minutes()).isEqualTo(30);
    }

    /** A session that already finished: {@code minutes} long, ending at {@code endsAt}. */
    private void endedSession(Long userId, String subjectId, long endsAt, int minutes) {
        sessionService.create(userId,
                new CreateStudySessionRequest(null, subjectId, endsAt - minutes * MINUTE, endsAt));
    }
}
