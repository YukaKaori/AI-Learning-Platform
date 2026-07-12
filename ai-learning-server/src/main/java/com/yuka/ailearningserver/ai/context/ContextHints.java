package com.yuka.ailearningserver.ai.context;

/**
 * Caller-supplied hints for {@link LearningContextService#build}. Subjects
 * are still frontend-mock-only (see {@code docs/ai-engine.md}), so
 * subject name/description arrive as plain text from the client rather than
 * being resolved server-side from a {@code subjectId}. {@code focusLabel}/
 * {@code focusContent} carry the primary text an action is operating on
 * (e.g. a note's content) — the caller is responsible for having already
 * fetched it with an ownership check; this class never fetches it itself.
 */
public record ContextHints(
        String subjectName,
        String subjectDescription,
        String statsSnapshot,
        String focusLabel,
        String focusContent) {

    public static ContextHints empty() {
        return new ContextHints(null, null, null, null, null);
    }
}
