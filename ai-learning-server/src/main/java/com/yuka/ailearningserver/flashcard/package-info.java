/**
 * Flashcards — spaced-repetition memory training. Decks group cards and
 * optionally belong to a subject.
 *
 * <p>Phase 5 is schema + entities only. The card schema reserves the
 * scheduling columns ({@code due_at}, {@code interval_days}, {@code ease}),
 * so a future review engine (SM-2 or FSRS) is a service-layer addition — no
 * migration required. AI card generation from materials/notes is Phase 6+.
 * Reserved error-code range: 140000–149999.
 */
package com.yuka.ailearningserver.flashcard;
