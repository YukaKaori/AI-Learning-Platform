package com.yuka.ailearningserver.workspace;

import com.yuka.ailearningserver.ai.dto.ConversationSummaryResponse;
import com.yuka.ailearningserver.ai.dto.CreateConversationRequest;
import com.yuka.ailearningserver.ai.dto.UpdateConversationRequest;
import com.yuka.ailearningserver.ai.service.AiConversationService;
import com.yuka.ailearningserver.calendar.StudySessionService;
import com.yuka.ailearningserver.calendar.dto.CreateStudySessionRequest;
import com.yuka.ailearningserver.note.NoteService;
import com.yuka.ailearningserver.note.dto.CreateNoteRequest;
import com.yuka.ailearningserver.note.dto.NoteResponse;
import com.yuka.ailearningserver.preference.PreferenceService;
import com.yuka.ailearningserver.preference.dto.UpdatePreferencesRequest;
import com.yuka.ailearningserver.subject.SubjectService;
import com.yuka.ailearningserver.subject.dto.CreateSubjectRequest;
import com.yuka.ailearningserver.subject.dto.SubjectResponse;
import com.yuka.ailearningserver.task.TaskService;
import com.yuka.ailearningserver.task.dto.CreateTaskRequest;
import com.yuka.ailearningserver.task.dto.TaskResponse;
import com.yuka.ailearningserver.task.dto.UpdateTaskRequest;
import com.yuka.ailearningserver.workspace.dto.WorkspaceSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Workspace summary façade: honest zeros on an empty account, stats composed
 * from real domain data, per-section caps/ordering, and continue-learning
 * ranking by most recent linked activity.
 */
@SpringBootTest
@ActiveProfiles("test")
class WorkspaceServiceTest {

    private static final Long USER = 1L;
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 3_600_000L;
    private static final long DAY = 86_400_000L;

    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private SubjectService subjectService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private StudySessionService sessionService;
    @Autowired
    private PreferenceService preferenceService;
    @Autowired
    private AiConversationService conversationService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long base;

    @BeforeEach
    void cleanTables() {
        for (String table : List.of("subjects", "learning_materials", "notes", "flashcard_decks",
                "flashcards", "learning_tasks", "study_sessions", "ai_conversations", "ai_messages",
                "user_preferences")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
        base = Instant.now().truncatedTo(ChronoUnit.SECONDS).toEpochMilli();
    }

    @Test
    void emptyAccountGetsZerosAndEmptySectionsNotErrors() {
        WorkspaceSummaryResponse summary = workspaceService.summary(USER);

        assertThat(summary.stats())
                .isEqualTo(new WorkspaceSummaryResponse.Stats(0, 0, 60, 0, 0)); // 60 = preferences default
        assertThat(summary.continueLearning()).isEmpty();
        assertThat(summary.upcomingTasks()).isEmpty();
        assertThat(summary.recentConversations()).isEmpty();
        assertThat(summary.recentNotes()).isEmpty();
        assertThat(summary.todaySessions()).isEmpty();
        assertThat(summary.weekActivity()).hasSize(7)
                .allSatisfy(day -> assertThat(day.minutes()).isZero());
    }

    @Test
    void statsComposeRealDomainData() {
        preferenceService.update(USER, new UpdatePreferencesRequest(null, null, 120));
        sessionService.create(USER, new CreateStudySessionRequest(null, null, base - 30 * MINUTE, base));
        subjectService.create(USER, new CreateSubjectRequest("Piano", null, null, null));
        insertFlashcard(9001L, base - DAY);  // due
        insertFlashcard(9002L, base + DAY);  // not yet due

        WorkspaceSummaryResponse.Stats stats = workspaceService.summary(USER).stats();
        assertThat(stats.streakDays()).isEqualTo(1);
        assertThat(stats.studiedTodayMinutes()).isEqualTo(30);
        assertThat(stats.dailyGoalMinutes()).isEqualTo(120);
        assertThat(stats.dueCards()).isEqualTo(1);
        assertThat(stats.activeSubjects()).isEqualTo(1);
    }

    @Test
    void upcomingTasksAreOpenSoonestFirstCappedAtFive() {
        TaskResponse dueSecond = taskService.create(USER,
                new CreateTaskRequest("due in 2d", null, null, base + 2 * DAY, null));
        TaskResponse dueFirst = taskService.create(USER,
                new CreateTaskRequest("due in 1d", null, null, base + DAY, null));
        TaskResponse unscheduled = taskService.create(USER,
                new CreateTaskRequest("backlog", null, null, null, null));
        for (int i = 3; i <= 5; i++) {
            taskService.create(USER, new CreateTaskRequest("due in " + i + "d", null, null, base + i * DAY, null));
        }
        TaskResponse done = taskService.create(USER,
                new CreateTaskRequest("already done", null, null, base + HOUR, null));
        taskService.update(USER, Long.valueOf(done.id()), new UpdateTaskRequest(null, null, "done", null, null, null));

        List<TaskResponse> upcoming = workspaceService.summary(USER).upcomingTasks();
        assertThat(upcoming).hasSize(5);
        assertThat(upcoming.getFirst().id()).isEqualTo(dueFirst.id());
        assertThat(upcoming.get(1).id()).isEqualTo(dueSecond.id());
        assertThat(upcoming).extracting(TaskResponse::id)
                .doesNotContain(unscheduled.id(), done.id()); // nulls trail past the cap; done excluded
    }

    @Test
    void recentSectionsAreCappedOrderedAndSkipArchived() {
        NoteResponse[] notes = new NoteResponse[4];
        for (int i = 0; i < 4; i++) {
            notes[i] = noteService.create(USER, new CreateNoteRequest("note " + i, "content " + i, null, null));
            backdate("notes", Long.valueOf(notes[i].id()), base - (4 - i) * HOUR);
        }
        var conversations = new String[4];
        for (int i = 0; i < 4; i++) {
            conversations[i] = conversationService.create(USER,
                    new CreateConversationRequest("chat " + i, null, null)).id();
        }
        conversationService.update(USER, Long.valueOf(conversations[3]),
                new UpdateConversationRequest(null, true)); // archive the newest
        for (int i = 0; i < 4; i++) {
            backdate("ai_conversations", Long.valueOf(conversations[i]), base - (4 - i) * HOUR);
        }
        sessionService.create(USER, new CreateStudySessionRequest("today", null, base - 30 * MINUTE, base));
        sessionService.create(USER, new CreateStudySessionRequest("old", null, base - 3 * DAY, base - 3 * DAY + HOUR));

        WorkspaceSummaryResponse summary = workspaceService.summary(USER);
        assertThat(summary.recentNotes())
                .extracting(WorkspaceSummaryResponse.RecentNote::title)
                .containsExactly("note 3", "note 2", "note 1");
        assertThat(summary.recentConversations())
                .extracting(ConversationSummaryResponse::title)
                .containsExactly("chat 2", "chat 1", "chat 0"); // archived chat 3 skipped
        assertThat(summary.todaySessions())
                .singleElement()
                .satisfies(session -> assertThat(session.title()).isEqualTo("today"));
    }

    @Test
    void continueLearningRanksActiveSubjectsByRecentLinkedActivity() {
        SubjectResponse[] subjects = new SubjectResponse[4];
        for (int i = 0; i < 4; i++) {
            subjects[i] = subjectService.create(USER, new CreateSubjectRequest("subject " + i, null, null, null));
            backdate("subjects", Long.valueOf(subjects[i].id()), base - (10 - i) * DAY);
        }
        // Fresh linked activity resurrects the oldest subject to the top.
        noteService.create(USER, new CreateNoteRequest("linked", null, null, subjects[0].id()));

        List<WorkspaceSummaryResponse.ContinueLearningItem> items =
                workspaceService.summary(USER).continueLearning();
        assertThat(items).extracting(WorkspaceSummaryResponse.ContinueLearningItem::name)
                .containsExactly("subject 0", "subject 3", "subject 2");
    }

    private void insertFlashcard(long id, long dueAtEpochMilli) {
        jdbcTemplate.update("""
                        INSERT INTO flashcards (id, deck_id, user_id, front, back, due_at, review_count,
                                                created_at, updated_at, deleted)
                        VALUES (?, 1, ?, 'q', 'a', ?, 0, ?, ?, 0)""",
                id, USER, Timestamp.from(Instant.ofEpochMilli(dueAtEpochMilli)),
                Timestamp.from(Instant.ofEpochMilli(base)), Timestamp.from(Instant.ofEpochMilli(base)));
    }

    private void backdate(String table, Long id, long updatedAtEpochMilli) {
        jdbcTemplate.update("UPDATE " + table + " SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.ofEpochMilli(updatedAtEpochMilli)), id);
    }
}
