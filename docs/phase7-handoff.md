# Phase 7 Handoff — Commercial Product Foundation

Status as of 2026-07-14 (third session — step 5 completed; **backend workstream A is now fully done**). This document is the resume point for continuing Phase 7 in a new session. Read this first; the approved plan with full rationale lives at `D:\claude-data\plans\refactored-wiggling-floyd.md` (outside the repo — confirm it exists in the new environment, or regenerate from this handoff plus `docs/architecture.md`).

## What Phase 7 is

The repo was restored to the final Phase 6 commit (`85f7df3`) and Phase 7 was **redesigned from scratch** as "Commercial Product Foundation" — turning the Phase 6 codebase into a per-user, database-driven SaaS foundation with a premium 3-mode theme and a productivity-app Workspace. Full context, all 10 design decisions (D1–D10), and the complete 13-step plan are in the plan file above — **do not re-derive them.**

**Ignore any reference to an earlier "Phase 7 — Data Realization" attempt** (different error-code ranges, e.g. Subject 140000) — discarded before this redesign.

Out of scope this phase (do not build): registration/email/password-reset, payments, OSS/file upload, external auth, spaced-repetition engine, WebSockets, Docker/CI.

## Session summary (2026-07-14, step 5)

Implemented A9 + A10 as one step and committed it as `cdea352`. Architectural decisions made this session (all downstream steps should respect them):

- **Attribution rules for analytics** (documented on `AnalyticsService`): a session's minutes count on the day it **ends**; only sessions that have **already ended** count (calendar sessions planned in the future are never study time); "this week" is the **rolling 7-day window ending today** vs the 7 days before it. Streak per D6 (consecutive end-days, anchored today *or* yesterday, server TZ).
- **Honest nulls, not fake zeros**: `weekDeltaPercent` is null when the previous week is 0; `taskCompletionPercent` is null when there are no tasks. Frontend (step 10) must render these as "—".
- **Aggregation style**: in-memory grouping over slim column-projected selects (mirrors `SubjectService.deriveAll()`) — dialect-free (H2 tests + MySQL), no new SQL. Materialize only if measurably slow (D5).
- **Workspace reuses other modules' DTOs** (`TaskResponse`, `ConversationSummaryResponse`, `StudySessionResponse`, `ActivityDayResponse`) rather than redefining wire shapes; the one exception is the slim `RecentNote` record (no content payload). `WorkspaceService` injects `AnalyticsService` + `PreferenceService` so the dashboard can never disagree with the analytics page or the user's goal.
- **AI subject context**: `ContextHints` gained a **resolved** `Long subjectId` as its first component (callers resolve via `SubjectService`, the context service never trusts raw input); `LearningContext` gained `subjectMaterialTitles`; when a subject resolves, note count/titles are **scoped to that subject** and the DB name/description win over string hints; a stale id (subject deleted later) degrades gracefully to hints. Conversations persist `subjectId` + a refreshed `subjectName` snapshot on create **and** send; on send, `subjectId: null` keeps the current link, `""` unlinks (partial-update convention). `AiConversation.subjectId/subjectName` now carry `@TableField(updateStrategy = ALWAYS)`.
- **`SubjectService.resolveOwnedSubject(userId, wireId)`** added (returns the entity; `resolveOwnedSubjectId` now delegates to it) — use it when a caller needs subject fields, not just the id.
- Workspace error range 170000 is **intentionally unused** (a pure read model has no failure modes); analytics uses exactly one code, `180000 ANALYTICS_RANGE_INVALID` (days outside 1..90).

Commits this session: `cdea352` (feat, step 5) and the docs commit containing this file.

## Current project status

- **Phase**: 7 (Commercial Product Foundation), phases 1–6 complete.
- **Step**: 5 of 13 **done** — backend workstream A (A1–A10) is complete. Next: **step 6 (B1+B2)**, first frontend step.
- **Branch**: `main`. Working tree: clean after the docs commit. Not pushed (push via SSH when asked; port 22 may be blocked → ssh.github.com:443).
- Commit history for the phase: `1a20f23` (step 1) → `d93f07c` (step 2) → `ccdd41a` (step 3) → `cac1706` (step 4) → `b04b8b3` (docs) → `cdea352` (step 5) → this docs commit.

## Progress: steps 1–5 of 13 done

| # | Step | Status |
|---|------|--------|
| 1 | A1+A2 — Migrations V4/V5, pom fix, `common/OwnershipGuard` | **Done**, `1a20f23` |
| 2 | A3+A4 — Subject (110000s) + Material (120000s) modules | **Done**, `d93f07c` |
| 3 | A5+A6+A7 — Task (150000s), Calendar (160000s), Note/Deck subjectId | **Done**, `ccdd41a` |
| 4 | A8 — Preferences (200000s) + auth prep (`/me` createdAt, `PUT /auth/profile`) | **Done**, `cac1706` |
| 5 | A9+A10 — Analytics (180000s), Workspace summary (170000s), AI subjectId context | **Done**, `cdea352` |
| 6 | B1+B2 — Frontend api modules, `useAsync` composable, `StatTile` component | **Not started — resume here** |
| 7 | B3+B4 — Subjects frontend + cross-feature subject linkage | Not started |
| 8 | B5 — Calendar + Tasks frontend | Not started |
| 9 | B6 — Workspace redesign (centerpiece) | Not started |
| 10 | B7+B8 — Analytics/Profile frontend, delete all 8 mock.ts, 404 route, index.html title | Not started |
| 11 | B9 — Preferences wiring (Settings, theme/locale reconciliation) | Not started |
| 12 | C1+C2 — Dark theme black+purple luxury glass re-skin, UX unification pass | Not started |
| 13 | D — Docs (architecture.md Phase 7 section, new mock-migration.md, etc.) | Not started |

## What exists now (backend — complete; steps 6–11 consume it, don't change it)

Everything from the previous handoff (OwnershipGuard; subject/material/task/calendar/preference modules; auth prep; wire conventions) still holds. New this session:

- `analytics/`: `AnalyticsController` (`GET /api/v1/analytics/summary`, `/activity?days=N` default 30 max 90, `/subject-shares?days=N` default 30), `AnalyticsService`, `AnalyticsErrorCode` (180000), DTOs `AnalyticsSummaryResponse` / `ActivityDayResponse` / `SubjectShareResponse`.
  - `ActivityDayResponse.date` is an **ISO local date string** (`yyyy-MM-dd`) — a calendar bucket, not an instant; the epoch-ms convention deliberately doesn't apply. One `activity` series feeds both the bar chart and the heatmap (`days=84`).
  - `subject-shares` returns a **null-id bucket** for minutes not linked to any (surviving) subject, so shares always sum to the real total; percent is computed client-side.
  - **`AnalyticsService.streakDays()` and `activity()` are public and reused by `WorkspaceService`** — never re-implement streak or per-day math elsewhere.
- `workspace/`: `WorkspaceController` (`GET /api/v1/workspace/summary`), `WorkspaceService`, `WorkspaceSummaryResponse` (nested `Stats`, `ContinueLearningItem`, `RecentNote`). Sections: stats (streak, studiedToday, dailyGoal, dueCards = `due_at ≤ now`, activeSubjects), continueLearning (top-3 **active** subjects by latest `updatedAt` across subject/materials/notes/decks/sessions), upcomingTasks (5, open, dueAt asc nulls-last), recentConversations (3, unarchived), recentNotes (3, slim), todaySessions (overlap semantics), weekActivity (7 days).
- AI: `CreateConversationRequest`/`SendMessageRequest` gained optional `subjectId`; `ConversationSummaryResponse`/`ConversationDetailResponse` now include `subjectId` (string, nullable) — **frontend `api/modules/ai.ts` types must add these fields in step 6 (B1)**. Legacy hint-only requests behave exactly as before.
- `docs/ai-engine.md` updated: the "subjects are mock-only" limitation section replaced with the Phase 7 subject-resolution contract; record shapes, conversation flow, and DB section refreshed.

## What exists now (test infra)

- `src/test/resources/schema.sql` unchanged (V1–V5 already mirrored; step 5 added no migrations). Keep in sync with any future migration.
- Test suite: **53 tests, all green** (`./mvnw test`). New classes:
  - `analytics/AnalyticsServiceTest` — streak empty/gap/today-missing/consecutive, future-session exclusion, empty-account nulls, rolling-week delta, activity zero-fill + window validation, subject-share buckets.
  - `workspace/WorkspaceServiceTest` — empty-account zeros (goal default 60), stats composition, task cap/order, recent-section caps + archived exclusion, continue-learning ranking.
  - `ai/AiSubjectContextTest` — create/link + name snapshot, foreign/malformed rejection, server-side context resolution (materials + subject-scoped notes), hint fallback, streamReply persist/keep/clear semantics, foreign subject rejected before any message persists.
- Test gotchas that bit (or nearly bit) this session: whole-second time anchors (`Instant.now().truncatedTo(SECONDS)`) because DATETIME rounds fractional seconds; **ordering assertions on `updatedAt` need jdbcTemplate backdating** — rows created in a fast loop land in the same second and `ORDER BY updated_at` ties are unstable; `streamReply` is testable without a provider key (link/message persistence happens before the async stream; the provider error arrives asynchronously and persists nothing).

## Nothing changed on the frontend yet

All 8 `features/*/mock.ts` files are still in place and untouched. Frontend work starts at step 6 (B1).

## Exact resume point

Start **step 6 (B1+B2)** — frontend API modules + shared infra:

1. First file to read: this file, then `ai-learning-web/src/api/modules/note.ts` (the module pattern to mirror) and `ai-learning-web/src/api/http.ts` (`unwrap`/`ApiError`).
2. First module to inspect: `ai-learning-web/src/api/modules/` and `ai-learning-web/src/composables/` (see what exists before adding `useAsync.ts`), plus `features/*/mock.ts` to know the shapes being replaced.
3. First task to implement (B1): typed api modules `subject.ts`, `material.ts`, `task.ts`, `calendar.ts`, `workspace.ts`, `analytics.ts`, `preferences.ts`; extend `auth.ts` (`createdAt`, `updateProfile`) and `ai.ts` (`subjectId` on create/send requests and conversation responses). Wire types must match the backend DTOs exactly (epoch-ms numbers, string ids, ISO date strings in activity, nullable `weekDeltaPercent`/`taskCompletionPercent`).
4. Then B2: `composables/useAsync.ts` (+ unit test), `components/StatTile.vue` (barrel-registered, shown in `/design-system`), retrofit `.catch(console.error)` holes in `AiTutorView`/`NotesView` to the Skeleton → content | `AppEmpty` | error-with-retry pattern (D3).
5. Frontend verification baseline: `npm run type-check && npm run test:unit && npm run build`, i18n mirror test after any locale change.

Do not modify backend code in steps 6–11 unless a defect is found.

## Verification performed (step 5)

- **Build/package**: `./mvnw compile` and `./mvnw package -DskipTests` clean. (Type check = Java compile; no frontend change to type-check.)
- **Tests**: full suite 53/53 green.
- **API (dev server + curl, demo user)**: empty account → summary all zeros with null delta/completion, activity zero-filled 7 days, shares `[]`, workspace stats `0/0/60/0/0` — no 500s. `days=91` → 180000; `days=abc` → 40001; no token → 40100/401. Populated (subject + material + linked note + task + today 45-min session + 10-days-ago 30-min session + goal 120) → summary `{weekMinutes:45, weekDeltaPercent:50, streakDays:1, taskCompletionPercent:0}`, shares linked-60/unlinked-30 buckets (values include a pre-existing leftover conversation in `aiChatsThisWeek`, correctly), workspace reflected everything. AI: create with `subjectId` → link + "Step5 Verify" snapshot; SSE send with `subjectId` → link persisted even though the stream errored with 190000 (no `DEEPSEEK_API_KEY` in this environment — expected); `""` sentinel unlinked and persisted; malformed id → 110000.
- **Database (MySQL 9.1, `ai_learning`)**: `ai_conversations.subject_id`/`subject_name` and `user_preferences.daily_goal_minutes` inspected directly and matched API state at each stage.
- **Cleanup**: every verification row deleted via the API afterwards; preferences reset to 60; final workspace call re-verified honest zeros.
- **Intentionally not verified**: live DeepSeek streaming with real subject context (no API key here — same standing gap as Phase 6 Task 15; the pipeline up to the provider call is covered by tests and the SSE error path). **Browser verification intentionally skipped**: zero frontend files changed and the new endpoints have no UI consumers until step 6; existing pages still run on mocks and are unaffected.

## Known risks / gotchas

- **PowerShell mangles `""` in `git commit -m` here-strings** (native-arg quoting splits the message into pathspecs — a commit this session failed that way with HEAD untouched). Write the message to a file and use `git commit -F <file>`. Also avoid non-ASCII in Git-Bash curl `-d` payloads (UTF-8 mangling → spurious 500; shell artifact, not an app bug).
- **Stray Java processes hold port 8080 between sessions** — `netstat -ano | grep :8080`, then `taskkill //PID <pid> //F` (Git Bash) before relaunching. The dev server started this session was killed at the end; port verified free is not re-checked after that.
- **Login field is `usernameOrEmail`**, not `username` (`{"usernameOrEmail":"demo","password":"Demo123456"}`).
- **Dev DB is NOT fully clean**: two notes (`未命名笔记`, `Verify Note`) and one conversation (`Hello, explain recursion`) remain from *Phase 6* verification — pre-existing, deliberately left untouched (they weren't created this session). They show up in workspace `recentNotes`/`recentConversations` and count 1 into `aiChatsThisWeek` until they age out or are deleted. Everything step 5 created was removed.
- Analytics/streak use **server default timezone** and day-bucket assertions in tests assume the test doesn't straddle midnight — same accepted tolerance as the existing suite.
- `AiConversation` now has ALWAYS-update fields; like the other entities, **never write it via a partial entity `updateById`** — always load the row first (all current code does).
- MySQL root password `1234`; mysql CLI at `C:\Program Files\MySQL\MySQL Server 9.1\bin\mysql.exe`. JDK 22, backend :8080, frontend :5173, demo login `demo`/`Demo123456`.

## Documentation state

- `docs/ai-engine.md` — updated this session (subject-resolution contract). In sync.
- `docs/architecture.md` — **not** touched: error ranges 170000/180000 were already enumerated; no decision in this step contradicted it. The Phase 7 section, read-model contracts and D2/D6 write-ups land in step 13 per plan (plus the new `docs/mock-migration.md`).
- This file — rewritten for the step-5 boundary.
