/**
 * Workspace — the aggregation façade behind the learning dashboard
 * ("continue learning", today's goal, due cards, streak). It owns NO tables
 * and NO entities: it composes read models over subject, material, note,
 * flashcard, task and calendar, and must never be written to directly.
 *
 * <p>Phase 5 reserves the package; the dashboard endpoint arrives when the
 * underlying domains gain their APIs. Reserved error-code range:
 * 170000–179999.
 */
package com.yuka.ailearningserver.workspace;
