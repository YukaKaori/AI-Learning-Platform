package com.yuka.ailearningserver.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuka.ailearningserver.common.OwnershipGuard;
import com.yuka.ailearningserver.common.exception.BusinessException;
import com.yuka.ailearningserver.subject.SubjectService;
import com.yuka.ailearningserver.task.dto.CreateTaskRequest;
import com.yuka.ailearningserver.task.dto.TaskResponse;
import com.yuka.ailearningserver.task.dto.UpdateTaskRequest;
import com.yuka.ailearningserver.task.entity.LearningTask;
import com.yuka.ailearningserver.task.entity.TaskPriority;
import com.yuka.ailearningserver.task.entity.TaskStatus;
import com.yuka.ailearningserver.task.mapper.LearningTaskMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Learning-task CRUD. {@code completedAt} is owned by the status transition
 * (set when a task becomes {@code done}, cleared when it leaves {@code done})
 * and is never written directly by clients.
 */
@Service
public class TaskService {

    private final LearningTaskMapper taskMapper;
    private final SubjectService subjectService;

    public TaskService(LearningTaskMapper taskMapper, SubjectService subjectService) {
        this.taskMapper = taskMapper;
        this.subjectService = subjectService;
    }

    public List<TaskResponse> list(Long userId, String status, Long dueBefore, Long subjectId) {
        LambdaQueryWrapper<LearningTask> query = new LambdaQueryWrapper<LearningTask>()
                .eq(LearningTask::getUserId, userId);
        if (status != null) {
            query.eq(LearningTask::getStatus, parseStatus(status));
        }
        if (dueBefore != null) {
            query.le(LearningTask::getDueAt, toLocalDateTime(dueBefore));
        }
        if (subjectId != null) {
            query.eq(LearningTask::getSubjectId, subjectId);
        }
        query.orderByAsc(LearningTask::getStatus)
                .orderByAsc(LearningTask::getDueAt)
                .orderByDesc(LearningTask::getCreatedAt);
        return taskMapper.selectList(query).stream()
                .map(TaskResponse::from)
                .toList();
    }

    public TaskResponse get(Long userId, Long id) {
        return TaskResponse.from(requireOwned(userId, id));
    }

    public TaskResponse create(Long userId, CreateTaskRequest request) {
        LearningTask task = new LearningTask();
        task.setUserId(userId);
        task.setSubjectId(subjectService.resolveOwnedSubjectId(userId, request.subjectId()));
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(TaskStatus.TODO);
        task.setPriority(request.priority() != null ? parsePriority(request.priority()) : TaskPriority.MEDIUM);
        task.setDueAt(toLocalDateTime(request.dueAt()));
        taskMapper.insert(task);
        return TaskResponse.from(task);
    }

    public TaskResponse update(Long userId, Long id, UpdateTaskRequest request) {
        LearningTask task = requireOwned(userId, id);
        if (request.title() != null && !request.title().isBlank()) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.status() != null) {
            TaskStatus next = parseStatus(request.status());
            if (next == TaskStatus.DONE && task.getStatus() != TaskStatus.DONE) {
                task.setCompletedAt(LocalDateTime.now());
            } else if (next != TaskStatus.DONE && task.getStatus() == TaskStatus.DONE) {
                task.setCompletedAt(null);
            }
            task.setStatus(next);
        }
        if (request.priority() != null) {
            task.setPriority(parsePriority(request.priority()));
        }
        if (request.dueAt() != null) {
            task.setDueAt(request.dueAt() == 0 ? null : toLocalDateTime(request.dueAt()));
        }
        if (request.subjectId() != null) {
            task.setSubjectId(subjectService.resolveOwnedSubjectId(userId, request.subjectId()));
        }
        taskMapper.updateById(task);
        return TaskResponse.from(task);
    }

    public void delete(Long userId, Long id) {
        LearningTask task = requireOwned(userId, id);
        taskMapper.deleteById(task.getId());
    }

    private LearningTask requireOwned(Long userId, Long id) {
        return OwnershipGuard.require(taskMapper.selectById(id), LearningTask::getUserId, userId,
                TaskErrorCode.TASK_NOT_FOUND, TaskErrorCode.TASK_ACCESS_DENIED);
    }

    private static TaskStatus parseStatus(String value) {
        return switch (value) {
            case "todo" -> TaskStatus.TODO;
            case "inProgress" -> TaskStatus.IN_PROGRESS;
            case "done" -> TaskStatus.DONE;
            default -> throw new BusinessException(TaskErrorCode.TASK_STATUS_INVALID);
        };
    }

    private static TaskPriority parsePriority(String value) {
        return switch (value) {
            case "low" -> TaskPriority.LOW;
            case "medium" -> TaskPriority.MEDIUM;
            case "high" -> TaskPriority.HIGH;
            default -> throw new BusinessException(TaskErrorCode.TASK_PRIORITY_INVALID);
        };
    }

    private static LocalDateTime toLocalDateTime(Long epochMilli) {
        return epochMilli != null
                ? Instant.ofEpochMilli(epochMilli).atZone(ZoneId.systemDefault()).toLocalDateTime()
                : null;
    }
}
