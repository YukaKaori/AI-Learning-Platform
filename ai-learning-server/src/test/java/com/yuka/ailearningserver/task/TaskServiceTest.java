package com.yuka.ailearningserver.task;

import com.yuka.ailearningserver.common.exception.BusinessException;
import com.yuka.ailearningserver.subject.SubjectErrorCode;
import com.yuka.ailearningserver.subject.SubjectService;
import com.yuka.ailearningserver.subject.dto.CreateSubjectRequest;
import com.yuka.ailearningserver.subject.dto.SubjectResponse;
import com.yuka.ailearningserver.task.dto.CreateTaskRequest;
import com.yuka.ailearningserver.task.dto.TaskResponse;
import com.yuka.ailearningserver.task.dto.UpdateTaskRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task behavior: per-user isolation, filters, the completedAt lifecycle owned
 * by status transitions, and validated optional subject linkage.
 */
@SpringBootTest
@ActiveProfiles("test")
class TaskServiceTest {

    private static final Long USER = 1L;
    private static final Long OTHER_USER = 2L;

    @Autowired
    private TaskService taskService;
    @Autowired
    private SubjectService subjectService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        for (String table : List.of("subjects", "learning_tasks")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    @Test
    void crudRoundTrip() {
        TaskResponse created = taskService.create(USER,
                new CreateTaskRequest("Finish chapter 4", "exercises 1-10", null, null, null));
        assertThat(created.status()).isEqualTo("todo");
        assertThat(created.priority()).isEqualTo("medium");
        assertThat(created.dueAt()).isNull();
        assertThat(created.completedAt()).isNull();

        long due = Instant.now().plusSeconds(86400).toEpochMilli();
        TaskResponse updated = taskService.update(USER, Long.valueOf(created.id()),
                new UpdateTaskRequest("Finish chapter 5", null, "inProgress", "high", due, null));
        assertThat(updated.title()).isEqualTo("Finish chapter 5");
        assertThat(updated.status()).isEqualTo("inProgress");
        assertThat(updated.priority()).isEqualTo("high");
        assertThat(updated.dueAt()).isNotNull();

        assertThat(taskService.list(USER, null, null, null)).hasSize(1);
        taskService.delete(USER, Long.valueOf(created.id()));
        assertThat(taskService.list(USER, null, null, null)).isEmpty();
    }

    @Test
    void statusTransitionOwnsCompletedAt() {
        TaskResponse task = taskService.create(USER,
                new CreateTaskRequest("Review notes", null, null, null, null));
        Long id = Long.valueOf(task.id());

        TaskResponse done = taskService.update(USER, id,
                new UpdateTaskRequest(null, null, "done", null, null, null));
        assertThat(done.status()).isEqualTo("done");
        assertThat(done.completedAt()).isNotNull();

        TaskResponse reopened = taskService.update(USER, id,
                new UpdateTaskRequest(null, null, "todo", null, null, null));
        assertThat(reopened.status()).isEqualTo("todo");
        assertThat(reopened.completedAt()).isNull();
        // Cleared value must also be persisted, not just mutated in memory.
        assertThat(taskService.get(USER, id).completedAt()).isNull();
    }

    @Test
    void listFilters() {
        SubjectResponse subject = subjectService.create(USER,
                new CreateSubjectRequest("Math", null, null, null));
        long tomorrow = Instant.now().plusSeconds(86400).toEpochMilli();
        long nextWeek = Instant.now().plusSeconds(7 * 86400).toEpochMilli();

        TaskResponse dueSoon = taskService.create(USER,
                new CreateTaskRequest("Due soon", null, "high", tomorrow, subject.id()));
        taskService.create(USER, new CreateTaskRequest("Due later", null, null, nextWeek, null));
        TaskResponse backlog = taskService.create(USER,
                new CreateTaskRequest("Backlog", null, null, null, null));
        taskService.update(USER, Long.valueOf(backlog.id()),
                new UpdateTaskRequest(null, null, "done", null, null, null));

        assertThat(taskService.list(USER, "todo", null, null)).hasSize(2);
        assertThat(taskService.list(USER, "done", null, null)).hasSize(1);
        assertThat(taskService.list(USER, null, Instant.now().plusSeconds(2 * 86400).toEpochMilli(), null))
                .extracting(TaskResponse::id).containsExactly(dueSoon.id());
        assertThat(taskService.list(USER, null, null, Long.valueOf(subject.id())))
                .extracting(TaskResponse::id).containsExactly(dueSoon.id());
        assertThatThrownBy(() -> taskService.list(USER, "blocked", null, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TaskErrorCode.TASK_STATUS_INVALID));
    }

    @Test
    void dueAtClearSentinelUnschedules() {
        long due = Instant.now().plusSeconds(3600).toEpochMilli();
        TaskResponse task = taskService.create(USER,
                new CreateTaskRequest("Scheduled", null, null, due, null));
        assertThat(task.dueAt()).isNotNull();

        taskService.update(USER, Long.valueOf(task.id()),
                new UpdateTaskRequest(null, null, null, null, 0L, null));
        assertThat(taskService.get(USER, Long.valueOf(task.id())).dueAt()).isNull();
    }

    @Test
    void crossUserAccessIsDenied() {
        TaskResponse task = taskService.create(USER,
                new CreateTaskRequest("Private", null, null, null, null));
        assertThatThrownBy(() -> taskService.get(OTHER_USER, Long.valueOf(task.id())))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TaskErrorCode.TASK_ACCESS_DENIED));
        assertThat(taskService.list(OTHER_USER, null, null, null)).isEmpty();
    }

    @Test
    void subjectLinkIsValidatedAndClearable() {
        SubjectResponse mine = subjectService.create(USER, new CreateSubjectRequest("Mine", null, null, null));
        SubjectResponse theirs = subjectService.create(OTHER_USER, new CreateSubjectRequest("Theirs", null, null, null));

        assertThatThrownBy(() -> taskService.create(USER,
                new CreateTaskRequest("Linked", null, null, null, theirs.id())))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(SubjectErrorCode.SUBJECT_ACCESS_DENIED));

        TaskResponse task = taskService.create(USER,
                new CreateTaskRequest("Linked", null, null, null, mine.id()));
        assertThat(task.subjectId()).isEqualTo(mine.id());

        taskService.update(USER, Long.valueOf(task.id()),
                new UpdateTaskRequest(null, null, null, null, null, ""));
        assertThat(taskService.get(USER, Long.valueOf(task.id())).subjectId()).isNull();
    }

    @Test
    void subjectDeleteUnlinksTasks() {
        SubjectResponse subject = subjectService.create(USER, new CreateSubjectRequest("Doomed", null, null, null));
        TaskResponse task = taskService.create(USER,
                new CreateTaskRequest("Survives", null, null, null, subject.id()));

        subjectService.delete(USER, Long.valueOf(subject.id()));

        TaskResponse kept = taskService.get(USER, Long.valueOf(task.id()));
        assertThat(kept.subjectId()).isNull();
        assertThat(kept.title()).isEqualTo("Survives");
    }
}
