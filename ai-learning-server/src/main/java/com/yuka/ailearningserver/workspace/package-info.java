/**
 * Workspace — the aggregation façade behind the learning dashboard
 * ("continue learning", today's goal, due cards, streak). It owns NO tables
 * and NO entities: it composes read models over subject, material, note,
 * flashcard, task, calendar, preferences, analytics and AI conversations,
 * and must never be written to directly.
 *
 * <p>Implemented in Phase 7 as a single aggregate endpoint
 * ({@code GET /api/v1/workspace/summary} — one round trip, one loading
 * state). Reserved error-code range: 170000–179999 (unused so far — the read
 * model has no failure modes of its own).
 */
package com.yuka.ailearningserver.workspace;
