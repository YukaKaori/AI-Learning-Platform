# Phase 15 Handoff — The Memory Engine (real FSRS-6 spaced repetition)

**Status: Phase 15 is COMPLETE and release-gate verified** as of 2026-07-23
(implemented across 7 steps; **not committed — awaiting approval**). Phase 15
turns flashcards from a flip-toy into a real scheduling engine: a due queue,
graded review sessions, honest retention statistics, and one source of truth
(the Memory Engine) reflected across Workspace, Flashcards, and Analytics.
**Do not start Phase 16 (Notes 2.0) without approval.**

## Architecture summary

The engine is layered so the scheduling math is a pure, testable island and
everything impure sits above it:

- **`flashcard/review/` — the pure scheduler (Step 1).** `ReviewScheduler`
  (interface, the evolution seam) + `Fsrs6Scheduler` (a faithful, deterministic
  transcription of `open-spaced-repetition/py-fsrs`, default 21-weight vector,
  fuzzing off). Works only in `Instant` / `SchedulingState` / `Rating` /
  `ReviewState` / `ReviewParameters`. No Spring, no persistence, no clock — a
  card in, a card out. `FlashcardConfig` is the single place that lifts it into
  the container as a `@Bean` (swap the algorithm there, invisible to callers).
- **`flashcard/ReviewService` — the impure bridge (Step 3).** The only place
  that touches mappers, `LocalDateTime`, transactions, and client-timezone day
  math. It maps a `Flashcard` row ↔ `SchedulingState`, runs the pure scheduler,
  and writes back atomically. Stateless: a "review session" is a client pass
  over the queue; the server owns the day-truth summary, not a session row.
- **Persistence (Step 2).** V6 migration ADDs FSRS state to `flashcards`
  (`stability`, `difficulty`, `state`, `step`; reuses `due_at`/`interval_days`;
  legacy `ease` kept, unwritten) and CREATEs the immutable, append-only
  `review_logs`. `common/ClientZone` resolves `X-Client-Timezone` for
  day-bucketing only (absolute instants are compared zone-independently).
- **Read models (Step 4).** Workspace and Analytics stay zero-table façades.
  Workspace `dueCards` delegates to `ReviewService.dueCount` (= the queue total,
  by construction). Analytics gains `reviewsThisWeek` + nullable
  `retentionPercent` (mature reviews only) and a per-day `reviews` series.
- **AI (Step 5).** The `FLASHCARDS` generation prompt now encodes
  spaced-repetition card rules (atomic, brief answers, self-contained
  questions) so generated decks are schedulable. JSON wire shape unchanged.
- **Frontend (Step 6).** A full-screen, keyboard-first `ReviewSessionView`
  (solid stage, no glass), a live Flashcards page, and an Analytics retention
  panel. A global `X-Client-Timezone` header makes the whole app timezone-aware.

### The daily loop (verified end to end)

```
create deck → generate/add cards → GET /review/queue (in-progress-due, then
capped new) → reveal → grade (Again/Hard/Good/Easy) → POST /review/{cardId}
(card advances + immutable ReviewLog appended, atomically) → GET /review/summary
(day truth) → Workspace dueCards & Analytics reviews/retention all reflect the
same review_logs. One source of truth: workspace dueCards == queue total.
```

## Implementation summary (by step)

| Step | Deliverable |
| --- | --- |
| 1 | Pure FSRS-6 scheduler + reference-vector unit tests (12) |
| 2 | V6 migration, FSRS state on `flashcards`, append-only `review_logs`, `ClientZone`, entities/mapper, H2 schema mirror (26 tests) |
| 3 | `ReviewService` + `ReviewController` (`/queue`, `POST /{cardId}`, `/summary`), DTOs, `FlashcardConfig` bean, `app.flashcard.new-cards-per-day`, daily cap (6 tests) |
| 4 | Workspace live due-count (via `ReviewService.dueCount`), Analytics `reviewsThisWeek`/`retentionPercent`/per-day `reviews` |
| 5 | `FLASHCARDS` prompt-quality pass + `PromptTemplateTest` (2) |
| 6 | `ReviewSessionView` (keyboard-first), rewired `FlashcardsView`, Analytics tiles, deck new/due badges, timezone header, locales |
| 7 | Release-gate verification + this handoff; one fix applied (below) |

## API contract

- `GET /api/v1/flashcards/review/queue?deckId=` → `{ cards[], dueCount, newCount, total }`
- `POST /api/v1/flashcards/review/{cardId}` body `{ "rating": 1..4 }` → `{ cardId, state, dueAt, intervalDays }`
- `GET /api/v1/flashcards/review/summary` → `{ reviewedToday, again/hard/good/easyCount, dueRemaining, newRemaining }`

All three read `X-Client-Timezone` for day-bucketing; grading is zone-independent.
No new error codes — ownership reuses `CARD_*`/`DECK_*` (140002/140003/140000/140001);
rating bounds are Bean Validation (`@Min(1) @Max(4)`).

## Files changed

**New (backend):** `flashcard/review/{Rating,ReviewState,SchedulingState,ReviewParameters,ReviewScheduler,Fsrs6Scheduler,package-info}`,
`flashcard/{FlashcardConfig,ReviewService,ReviewController}`,
`flashcard/dto/{GradeCardRequest,ReviewCardResponse,ReviewQueueResponse,GradeResponse,ReviewSummaryResponse}`,
`flashcard/entity/ReviewLog`, `flashcard/mapper/ReviewLogMapper`, `common/ClientZone`,
`db/migration/V6__flashcard_review_engine.sql`.
**New (tests):** `flashcard/review/Fsrs6SchedulerTest`, `flashcard/ReviewServiceTest`, `ai/prompt/PromptTemplateTest`.
**New (frontend):** `features/flashcards/ReviewSessionView.vue`.
**Modified (backend):** `flashcard/{FlashcardService,entity/Flashcard,dto/DeckResponse}`,
`workspace/{WorkspaceService,WorkspaceController}`, `analytics/{AnalyticsService,dto/AnalyticsSummaryResponse,dto/ActivityDayResponse}`,
`config/AppProperties`, `ai/prompt/PromptTemplate`, `application.yml`, test `schema.sql`,
`AnalyticsServiceTest`, `WorkspaceServiceTest`.
**Modified (frontend):** `api/http.ts`, `api/modules/{flashcard,analytics,workspace}.ts`,
`features/flashcards/FlashcardsView.vue`, `features/analytics/AnalyticsView.vue`, `locales/{zh-CN,en-US}.ts`.
**Docs:** `docs/ai-engine.md`.

## Important design decisions

- **TRUE FSRS-6, not SM-2** (user choice) — schema grew beyond the reserved
  `interval_days`/`ease`. Algorithm transcribed verbatim from py-fsrs, fuzzing
  off, verified against an independent Python oracle.
- **Purity firewall** — the scheduler is annotation-free and clock-free; all
  impurity lives in `ReviewService`. This is what keeps the math unit-testable.
- **`review_logs` is append-only** — grading inserts a new log; existing logs
  are never mutated. It is the retention source and a future FSRS-optimizer's
  replay input.
- **Daily new-card cap** (`app.flashcard.new-cards-per-day`, default 20) —
  in-progress due cards are uncapped; only first-introductions are capped, and
  the count is client-timezone day-bucketed. Mitigates "migrated deck → 500 due
  at once."
- **One source of truth** — `ReviewService.dueCount` == `queue().total()` by
  construction (shared predicate builders), so the Workspace tile and the
  session never disagree.
- **Retention on mature reviews only** — a review counts toward retention only
  after a real interval (`elapsed_days ≥ 1`); same-day learning reps and
  first-introductions are excluded. Null (→ "—") when there's no baseline.
- **No new glass** — `ReviewSessionView` is a solid, quiet, keyboard-first
  stage built from existing tokens. Displacement-filter budget unchanged at 2
  (login card + dock).
- **Session model is client-driven** — a single linear pass over the fetched
  queue; the server stays stateless. (Anki-style intra-session re-queuing of
  lapses was deliberately deferred; see Risks.)

## Future extension points

- **Per-user FSRS optimization** — `review_logs` already stores everything an
  optimizer needs; swap the `ReviewScheduler` bean in `FlashcardConfig` for a
  per-user-weighted variant. `ReviewParameters` is the tuning seam.
- **Client-timezone analytics** — Analytics day-bucketing stays server-zone by
  its documented contract; `ClientZone` is the seam if per-user-zone analytics
  is wanted later (deliberately not retrofitted onto the session streak).
- **Fuzzing** — off for determinism; a future toggle could spread review load.
- **Per-deck due badge vs the daily cap** — badges show raw new/due counts
  (cap is a session concern); if per-deck cap display is wanted, thread the
  zone into `toDeckResponse`.

## Risks & known limitations

- **No intra-session re-queue** — a card graded "Again" is rescheduled ~1 min
  out but is not re-shown within the same session (single linear pass). Correct
  and finished, but less drill-y than Anki. Revisit if users want it.
- **AI-context due count is a separate heuristic** — `LearningContextService`
  still counts "cards with a past due date" for prompt flavor; it does not go
  through `ReviewService.dueCount`, so the tutor's sense of "due" can differ
  slightly from the Workspace tile. Pre-existing, out of Phase 15 scope, not a
  correctness bug. Candidate cleanup for a future AI phase.
- **`X-Client-Timezone` is a best-effort hint** — a missing/invalid header
  falls back to the server zone (never fails).

## Step 7 verification fix

One real issue was found and fixed during the release gate: `AnalyticsService.activity`
carried a `.le(reviewedAt, now)` upper bound copied from the sessions query
(where it correctly excludes *future planned* sessions). Reviews are never in
the future, so the bound was redundant — and it introduced a sub-second race
(a just-written `reviewed_at` can round up under MySQL `DATETIME` to a second
fractionally ahead of `now` and be dropped for ~1 s, briefly disagreeing with
the review summary). Removed the bound; added a defensive window-index guard.
Analytics unit tests and the full suite stay green.

## Verification results (2026-07-23)

- **Backend:** `./mvnw test` → **75/75 green, BUILD SUCCESS**.
- **Frontend:** `type-check` ✓, `lint` ✓, `test:unit` **8/8** (locale parity +
  no-empty-strings) ✓, `build` ✓.
- **Live API (curl):** full lifecycle — new deck (0/0) → 4 cards → queue (4 new)
  → grade all four ways (EASY→REVIEW/8-day; Again/Hard/Good→learning) → summary
  (5 reviews, 1/1/1/1 breakdown, newRemaining 20→16) → re-grade appends an
  immutable log without consuming budget → analytics/activity/workspace all
  agree (5) → **workspace dueCards == queue total**. 23/23 checks.
- **Live UI (Playwright):** front → **Space** reveal → four grade buttons →
  **1–4** grading → advance → session summary ("复习了 3 张") → finish closes;
  **Esc** exits; reduced-motion shows content. 11/11 checks.
- **Data hygiene:** all verification decks/cards/logs deleted; demo account
  restored to baseline (reviewedToday=0, reviewsThisWeek=0, dueCards=0).

## Resume point — Phase 16 (Notes 2.0)

Next is **Phase 16 — Notes 2.0**: replace the read-only note preview with a real
editor (TipTap/ProseMirror, the era's one sanctioned heavy FE dependency),
markdown storage, `[[wiki-links]]` + backlinks (V7 `note_links`), autosave, slash
commands, inline AI actions on selection. It is the highest-UI-risk phase and
introduces the **first in-app glass beyond login/dock** (the floating selection
toolbar — filter budget 2 → 3). Do not begin it until Phase 15 is approved.
