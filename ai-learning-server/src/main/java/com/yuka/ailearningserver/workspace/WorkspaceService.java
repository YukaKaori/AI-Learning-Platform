package com.yuka.ailearningserver.workspace;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuka.ailearningserver.ai.dto.ConversationSummaryResponse;
import com.yuka.ailearningserver.ai.entity.AiConversation;
import com.yuka.ailearningserver.ai.mapper.AiConversationMapper;
import com.yuka.ailearningserver.analytics.AnalyticsService;
import com.yuka.ailearningserver.analytics.dto.ActivityDayResponse;
import com.yuka.ailearningserver.calendar.dto.StudySessionResponse;
import com.yuka.ailearningserver.calendar.entity.StudySession;
import com.yuka.ailearningserver.calendar.mapper.StudySessionMapper;
import com.yuka.ailearningserver.flashcard.ReviewService;
import com.yuka.ailearningserver.flashcard.entity.FlashcardDeck;
import com.yuka.ailearningserver.flashcard.mapper.FlashcardDeckMapper;
import com.yuka.ailearningserver.material.entity.LearningMaterial;
import com.yuka.ailearningserver.material.mapper.LearningMaterialMapper;
import com.yuka.ailearningserver.note.entity.Note;
import com.yuka.ailearningserver.note.mapper.NoteMapper;
import com.yuka.ailearningserver.preference.PreferenceService;
import com.yuka.ailearningserver.subject.entity.Subject;
import com.yuka.ailearningserver.subject.entity.SubjectStatus;
import com.yuka.ailearningserver.subject.mapper.SubjectMapper;
import com.yuka.ailearningserver.task.dto.TaskResponse;
import com.yuka.ailearningserver.task.entity.LearningTask;
import com.yuka.ailearningserver.task.entity.TaskStatus;
import com.yuka.ailearningserver.task.mapper.LearningTaskMapper;
import com.yuka.ailearningserver.workspace.dto.WorkspaceSummaryResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-model façade behind the workspace dashboard: one aggregate summary
 * composed from the other domains, never written to directly (see
 * package-info). Streak and week-activity numbers come from
 * {@link AnalyticsService} so the dashboard can never disagree with the
 * analytics page.
 */
@Service
public class WorkspaceService {

    private static final int WEEK_DAYS = 7;
    private static final int CONTINUE_LEARNING_LIMIT = 3;
    private static final int UPCOMING_TASKS_LIMIT = 5;
    private static final int RECENT_CONVERSATIONS_LIMIT = 3;
    private static final int RECENT_NOTES_LIMIT = 3;

    private final AnalyticsService analyticsService;
    private final ReviewService reviewService;
    private final PreferenceService preferenceService;
    private final SubjectMapper subjectMapper;
    private final LearningMaterialMapper materialMapper;
    private final NoteMapper noteMapper;
    private final FlashcardDeckMapper deckMapper;
    private final LearningTaskMapper taskMapper;
    private final StudySessionMapper sessionMapper;
    private final AiConversationMapper conversationMapper;

    public WorkspaceService(AnalyticsService analyticsService, ReviewService reviewService,
                            PreferenceService preferenceService, SubjectMapper subjectMapper,
                            LearningMaterialMapper materialMapper, NoteMapper noteMapper,
                            FlashcardDeckMapper deckMapper, LearningTaskMapper taskMapper,
                            StudySessionMapper sessionMapper, AiConversationMapper conversationMapper) {
        this.analyticsService = analyticsService;
        this.reviewService = reviewService;
        this.preferenceService = preferenceService;
        this.subjectMapper = subjectMapper;
        this.materialMapper = materialMapper;
        this.noteMapper = noteMapper;
        this.deckMapper = deckMapper;
        this.taskMapper = taskMapper;
        this.sessionMapper = sessionMapper;
        this.conversationMapper = conversationMapper;
    }

    /**
     * @param zone the caller's timezone (from {@code X-Client-Timezone}); only
     *             the live due-count's new-card budget is day-bucketed by it.
     */
    public WorkspaceSummaryResponse summary(Long userId, ZoneId zone) {
        List<ActivityDayResponse> weekActivity = analyticsService.activity(userId, WEEK_DAYS);
        return new WorkspaceSummaryResponse(
                stats(userId, weekActivity.getLast().minutes(), zone),
                continueLearning(userId),
                upcomingTasks(userId),
                recentConversations(userId),
                recentNotes(userId),
                todaySessions(userId),
                weekActivity);
    }

    private WorkspaceSummaryResponse.Stats stats(Long userId, int studiedTodayMinutes, ZoneId zone) {
        long activeSubjects = subjectMapper.selectCount(new LambdaQueryWrapper<Subject>()
                .eq(Subject::getUserId, userId)
                .eq(Subject::getStatus, SubjectStatus.ACTIVE));
        return new WorkspaceSummaryResponse.Stats(
                analyticsService.streakDays(userId),
                studiedTodayMinutes,
                preferenceService.get(userId).dailyGoalMinutes(),
                reviewService.dueCount(userId, zone),
                Math.toIntExact(activeSubjects));
    }

    /**
     * Active subjects ranked by their most recent linked activity — the
     * latest {@code updatedAt} across the subject row itself (so a freshly
     * created subject surfaces immediately) and its materials, notes, decks
     * and sessions. Grouped in memory like {@code SubjectService.deriveAll()}.
     */
    private List<WorkspaceSummaryResponse.ContinueLearningItem> continueLearning(Long userId) {
        List<Subject> subjects = subjectMapper.selectList(new LambdaQueryWrapper<Subject>()
                .eq(Subject::getUserId, userId)
                .eq(Subject::getStatus, SubjectStatus.ACTIVE));
        if (subjects.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> lastActivity = new HashMap<>();
        subjects.forEach(subject -> lastActivity.put(subject.getId(), toEpochMilli(subject.getUpdatedAt())));
        materialMapper.selectList(new LambdaQueryWrapper<LearningMaterial>()
                        .select(LearningMaterial::getSubjectId, LearningMaterial::getUpdatedAt)
                        .eq(LearningMaterial::getUserId, userId))
                .forEach(m -> mergeActivity(lastActivity, m.getSubjectId(), m.getUpdatedAt()));
        noteMapper.selectList(new LambdaQueryWrapper<Note>()
                        .select(Note::getSubjectId, Note::getUpdatedAt)
                        .eq(Note::getUserId, userId)
                        .isNotNull(Note::getSubjectId))
                .forEach(n -> mergeActivity(lastActivity, n.getSubjectId(), n.getUpdatedAt()));
        deckMapper.selectList(new LambdaQueryWrapper<FlashcardDeck>()
                        .select(FlashcardDeck::getSubjectId, FlashcardDeck::getUpdatedAt)
                        .eq(FlashcardDeck::getUserId, userId)
                        .isNotNull(FlashcardDeck::getSubjectId))
                .forEach(d -> mergeActivity(lastActivity, d.getSubjectId(), d.getUpdatedAt()));
        sessionMapper.selectList(new LambdaQueryWrapper<StudySession>()
                        .select(StudySession::getSubjectId, StudySession::getUpdatedAt)
                        .eq(StudySession::getUserId, userId)
                        .isNotNull(StudySession::getSubjectId))
                .forEach(s -> mergeActivity(lastActivity, s.getSubjectId(), s.getUpdatedAt()));

        return subjects.stream()
                .sorted(Comparator.comparingLong((Subject s) -> lastActivity.get(s.getId())).reversed())
                .limit(CONTINUE_LEARNING_LIMIT)
                .map(subject -> WorkspaceSummaryResponse.ContinueLearningItem.from(
                        subject, lastActivity.get(subject.getId())))
                .toList();
    }

    /** Open tasks, soonest due first; unscheduled tasks trail scheduled ones. */
    private List<TaskResponse> upcomingTasks(Long userId) {
        return taskMapper.selectList(new LambdaQueryWrapper<LearningTask>()
                        .eq(LearningTask::getUserId, userId)
                        .ne(LearningTask::getStatus, TaskStatus.DONE))
                .stream()
                .sorted(Comparator.comparing(LearningTask::getDueAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(LearningTask::getCreatedAt, Comparator.reverseOrder()))
                .limit(UPCOMING_TASKS_LIMIT)
                .map(TaskResponse::from)
                .toList();
    }

    private List<ConversationSummaryResponse> recentConversations(Long userId) {
        return conversationMapper.selectList(new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .eq(AiConversation::getArchived, false)
                        .orderByDesc(AiConversation::getUpdatedAt)
                        .last("limit " + RECENT_CONVERSATIONS_LIMIT))
                .stream()
                .map(ConversationSummaryResponse::from)
                .toList();
    }

    private List<WorkspaceSummaryResponse.RecentNote> recentNotes(Long userId) {
        return noteMapper.selectList(new LambdaQueryWrapper<Note>()
                        .select(Note::getId, Note::getSubjectId, Note::getTitle, Note::getUpdatedAt)
                        .eq(Note::getUserId, userId)
                        .orderByDesc(Note::getUpdatedAt)
                        .last("limit " + RECENT_NOTES_LIMIT))
                .stream()
                .map(WorkspaceSummaryResponse.RecentNote::from)
                .toList();
    }

    /** Sessions overlapping today, same overlap semantics as the calendar window. */
    private List<StudySessionResponse> todaySessions(Long userId) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return sessionMapper.selectList(new LambdaQueryWrapper<StudySession>()
                        .eq(StudySession::getUserId, userId)
                        .lt(StudySession::getStartsAt, startOfToday.plusDays(1))
                        .gt(StudySession::getEndsAt, startOfToday)
                        .orderByAsc(StudySession::getStartsAt))
                .stream()
                .map(StudySessionResponse::from)
                .toList();
    }

    private static void mergeActivity(Map<Long, Long> lastActivity, Long subjectId, LocalDateTime touchedAt) {
        // computeIfPresent: only ACTIVE subjects are ranked, so activity on
        // archived/completed subjects is ignored rather than resurrected.
        lastActivity.computeIfPresent(subjectId, (id, current) -> Math.max(current, toEpochMilli(touchedAt)));
    }

    private static long toEpochMilli(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
