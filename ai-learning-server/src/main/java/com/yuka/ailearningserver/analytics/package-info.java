/**
 * Learning analytics — derived, read-only views over the other domains:
 * study time (study_sessions), streaks (session days), completion
 * (subjects.progress, tasks), review load (flashcards), and — in Phase 6 —
 * AI usage. It owns NO tables in Phase 5; if aggregation ever becomes
 * expensive, materialized summary tables are added HERE by a new migration,
 * never by widening the source domains.
 *
 * <p>Reserved error-code range: 180000–189999. (Range 190000–199999 is
 * reserved for the Phase 6 {@code ai} module.)
 */
package com.yuka.ailearningserver.analytics;
