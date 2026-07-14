package com.yuka.ailearningserver.workspace.dto;

import com.yuka.ailearningserver.ai.dto.ConversationSummaryResponse;
import com.yuka.ailearningserver.analytics.dto.ActivityDayResponse;
import com.yuka.ailearningserver.calendar.dto.StudySessionResponse;
import com.yuka.ailearningserver.note.entity.Note;
import com.yuka.ailearningserver.subject.entity.Subject;
import com.yuka.ailearningserver.task.dto.TaskResponse;

import java.time.ZoneId;
import java.util.List;

/**
 * Everything the workspace dashboard renders, in one round trip (one loading
 * state on the frontend). Section DTOs are reused from their owning modules —
 * the workspace is a read-model façade and must not redefine their wire
 * shapes. Notes are the exception: the dashboard only needs a title line, so
 * {@link RecentNote} deliberately omits the (potentially large) content.
 */
public record WorkspaceSummaryResponse(
        Stats stats,
        List<ContinueLearningItem> continueLearning,
        List<TaskResponse> upcomingTasks,
        List<ConversationSummaryResponse> recentConversations,
        List<RecentNote> recentNotes,
        List<StudySessionResponse> todaySessions,
        List<ActivityDayResponse> weekActivity) {

    /**
     * Headline tiles. {@code dailyGoalMinutes} comes from preferences (or its
     * default); {@code dueCards} counts flashcards with {@code dueAt ≤ now};
     * {@code activeSubjects} counts subjects in status {@code active}.
     */
    public record Stats(
            int streakDays,
            int studiedTodayMinutes,
            int dailyGoalMinutes,
            int dueCards,
            int activeSubjects) {
    }

    /**
     * An active subject ranked by most recent linked activity — the latest
     * {@code updatedAt} across the subject itself and its linked materials,
     * notes, decks and sessions (epoch ms).
     */
    public record ContinueLearningItem(
            String id,
            String name,
            String color,
            String icon,
            int progress,
            long lastActivityAt) {

        public static ContinueLearningItem from(Subject subject, long lastActivityAt) {
            return new ContinueLearningItem(
                    String.valueOf(subject.getId()),
                    subject.getName(),
                    subject.getColor(),
                    subject.getIcon(),
                    subject.getProgress() != null ? subject.getProgress() : 0,
                    lastActivityAt);
        }
    }

    /** Slim note row for the dashboard list — no content payload. */
    public record RecentNote(String id, String subjectId, String title, long updatedAt) {

        public static RecentNote from(Note note) {
            return new RecentNote(
                    String.valueOf(note.getId()),
                    note.getSubjectId() != null ? String.valueOf(note.getSubjectId()) : null,
                    note.getTitle(),
                    note.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
    }
}
