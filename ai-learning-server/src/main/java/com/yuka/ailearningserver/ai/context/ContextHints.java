package com.yuka.ailearningserver.ai.context;

/**
 * Caller-supplied hints for {@link LearningContextService#build}.
 * <p>
 * {@code subjectId} is a <em>resolved, ownership-validated</em> subject id —
 * callers obtain it via {@code SubjectService.resolveOwnedSubject*()}, never
 * from raw client input. When present, the context service loads the
 * subject's name, description, material titles and linked notes server-side
 * and the string hints are ignored for those parts. {@code subjectName}/
 * {@code subjectDescription} remain as the plain-text fallback used by
 * callers without a subject id (legacy chat clients, generation endpoints) —
 * both paths must keep working.
 * <p>
 * {@code focusLabel}/{@code focusContent} carry the primary text an action is
 * operating on (e.g. a note's content) — the caller is responsible for having
 * already fetched it with an ownership check; this class never fetches it
 * itself.
 */
public record ContextHints(
        Long subjectId,
        String subjectName,
        String subjectDescription,
        String statsSnapshot,
        String focusLabel,
        String focusContent) {

    public static ContextHints empty() {
        return new ContextHints(null, null, null, null, null, null);
    }
}
