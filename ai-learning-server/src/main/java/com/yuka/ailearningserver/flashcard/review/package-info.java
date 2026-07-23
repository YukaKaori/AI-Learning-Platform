/**
 * The spaced-repetition memory engine (Phase 15).
 *
 * <p>The core is {@link com.yuka.ailearningserver.flashcard.review.ReviewScheduler}
 * — a <em>pure, deterministic</em> function from a card's current
 * {@link com.yuka.ailearningserver.flashcard.review.SchedulingState scheduling
 * state} plus a {@link com.yuka.ailearningserver.flashcard.review.Rating grade}
 * to its next scheduling state. It performs no I/O and holds no per-request
 * state, so it is exhaustively unit-testable and trivially cacheable.
 *
 * <p>The sole implementation,
 * {@link com.yuka.ailearningserver.flashcard.review.Fsrs6Scheduler}, is a
 * faithful transcription of FSRS-6 (the memory-state model and learning-step
 * state machine of the {@code open-spaced-repetition/py-fsrs} reference), using
 * the published default 21-parameter weight vector. Fuzzing is intentionally
 * omitted so scheduling is fully deterministic.
 *
 * <p>The interface is the evolution seam: a future re-tuning, a newer FSRS
 * version, or per-user optimized weights swap in behind
 * {@code ReviewScheduler} without touching callers, endpoints, or the frontend.
 */
package com.yuka.ailearningserver.flashcard.review;
