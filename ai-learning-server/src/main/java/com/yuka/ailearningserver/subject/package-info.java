/**
 * Subject — the anchor domain of the learning platform. Every other learning
 * artifact (materials, notes, flashcard decks, tasks, study sessions) may hang
 * off a subject via a logical {@code subject_id} FK.
 *
 * <p>Phase 5 lays down the domain skeleton only: entity + mapper + schema
 * (V2 migration). Services/controllers/DTOs arrive when the module gains its
 * first real API. Reserved error-code range: 110000–119999.
 */
package com.yuka.ailearningserver.subject;
