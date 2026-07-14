# Phase 7 Handoff — Commercial Product Foundation

Status as of 2026-07-13 (second session — steps 3 and 4 completed). This document is the resume point for continuing Phase 7 in a new session. Read this first; the approved plan with full rationale lives at `D:\claude-data\plans\refactored-wiggling-floyd.md` (outside the repo — paste its contents into the new session if that path isn't reachable there, or regenerate from this handoff plus `docs/architecture.md`).

## What Phase 7 is

The repo was restored to the final Phase 6 commit (`85f7df3`) and Phase 7 was **redesigned from scratch** as "Commercial Product Foundation" — turning the Phase 6 codebase (real Notes/Flashcards/AI chat, mock Subjects/Tasks/Calendar/Analytics/Workspace) into a per-user, database-driven SaaS foundation with a premium 3-mode theme and a productivity-app Workspace. Full context, all 10 design decisions (D1–D10), and the complete 13-step plan are in the plan file above — **do not re-derive them, read the plan file.**

**Ignore any reference to an earlier "Phase 7 — Data Realization" attempt** (it used different error-code ranges, e.g. Subject 140000) — that attempt was discarded before this redesign began and no longer exists in the repo.

Out of scope this phase (do not build): registration/email/password-reset, payments, OSS/file upload, external auth, spaced-repetition engine, WebSockets, Docker/CI.

## Progress: steps 1–4 of 13 done

| # | Step | Status |
|---|------|--------|
| 1 | A1+A2 — Migrations (V4 user_preferences, V5 ai_conversations.subject_id), pom.xml flyway-mysql fix, `common/OwnershipGuard` + retrofit | **Done**, commit `1a20f23` |
| 2 | A3+A4 — Subject module (110000s) + Material module (120000s) | **Done**, commit `d93f07c` |
| 3 | A5+A6+A7 — Task (150000s), Calendar/StudySession (160000s), Note/Deck subjectId linkage | **Done**, commit `ccdd41a` |
| 4 | A8 — Preferences (200000s) + auth prep (`/me` createdAt, `PUT /auth/profile`) | **Done**, commit `cac1706` |
| 5 | A9+A10 — Analytics read model (180000s), Workspace summary (170000s), AI context subjectId upgrade | **Not started — resume here** |
| 6 | B1+B2 — Frontend api modules, `useAsync` composable, `StatTile` component | Not started |
| 7 | B3+B4 — Subjects frontend + cross-feature subject linkage | Not started |
| 8 | B5 — Calendar + Tasks frontend | Not started |
| 9 | B6 — Workspace redesign (centerpiece) | Not started |
| 10 | B7+B8 — Analytics/Profile frontend, delete all 8 mock.ts, 404 route, index.html title | Not started |
| 11 | B9 — Preferences wiring (Settings, theme/locale reconciliation) | Not started |
| 12 | C1+C2 — Dark theme black+purple luxury glass re-skin, UX unification pass | Not started |
| 13 | D — Docs (architecture.md Phase 7 section, new mock-migration.md, etc.) | Not started |

## What exists now (backend)

- `common/OwnershipGuard.require(entity, ownerFn, userId, notFoundCode, deniedCode)` — used by Note, Flashcard (deck+card), AiConversation, Subject, Material services. **Use this for every new service's ownership check** (Task, Calendar next).
- `subject/` package: `SubjectController` (`/api/v1/subjects`, full CRUD), `SubjectService` (list/get/create/update/delete, derived `materialCount/noteCount/deckCount/studyMinutes/lastStudiedAt` computed in `deriveAll()`), `SubjectErrorCode` (110000–110002), DTOs (`SubjectResponse` incl. nested `Derived` record, `CreateSubjectRequest`, `UpdateSubjectRequest`).
  - **`SubjectService.requireOwned(userId, id)` is public** — call it from Task/Calendar services when validating an optional `subjectId` on create/update (per A5/A6/A7 in the plan).
  - `SubjectService.delete()` is `@Transactional` and implements the D2 cascade: soft-deletes materials, nullifies `subjectId` on notes/decks/tasks/sessions/conversations. **When you build Task and Calendar services (step 3), no extra wiring is needed for delete-cascade — it's already handled here** since it directly touches `LearningTaskMapper`/`StudySessionMapper` (already injected).
- `material/` package: `MaterialController` (nested `/api/v1/subjects/{id}/materials` for collection, flat `/api/v1/materials/{id}` for item — mirrors flashcard deck/card nesting), `MaterialService`, `MaterialErrorCode` (120000–120002). Metadata + external links only this phase (`storageKey`/`sizeBytes` reserved for future upload).
- `task/` package: `TaskController` (`/api/v1/tasks`, full CRUD + GET-by-id, list filters `status`/`dueBefore`/`subjectId` all optional), `TaskService`, `TaskErrorCode` (150000–150003). Create always starts `todo` (no status field); `completedAt` is owned by the status transition in `update()` — set entering `done`, cleared leaving it, never client-writable.
- `calendar/` package: `StudySessionController` (`/api/v1/study-sessions`, mandatory `?from=&to=` epoch-ms window on GET), `StudySessionService` (overlap semantics: `startsAt < to AND endsAt > from`, ordered by `startsAt`), `CalendarErrorCode` (160000–160003). `endsAt > startsAt` validated on create and update (combined range re-checked when either bound changes); `durationMinutes` derived in the response DTO.
- `preference/` package: `PreferenceController` (`GET/PUT /api/v1/preferences`), `PreferenceService` (no row → `PreferencesResponse.DEFAULTS` = system/zh-CN/60; PUT lazily upserts, partial application, `DuplicateKeyException` first-write race handled), `PreferenceErrorCode` (200000–200001). `dailyGoalMinutes` bean-validated `@Min(1) @Max(1440)`.
- Auth prep done: `AuthUserResponse` now carries `createdAt` (epoch ms); `PUT /api/v1/auth/profile` updates nickname/avatar via `AuthService.updateProfile()` (explicit `LambdaUpdateWrapper` because blank clears to null). `DevUserSeeder` untouched.
- **Established wire conventions (follow these in every remaining step):**
  - All instants are **epoch milliseconds** in both directions; ids are strings.
  - Task status wire vocabulary is **`todo` / `inProgress` / `done`** (camelCase, matching `features/tasks/types.ts`); priority `low`/`medium`/`high`.
  - Partial updates: null = no change; **nullable columns clear via sentinel** — `subjectId: ""` unlinks, task `dueAt: 0` unschedules, session `title: ""` / profile `nickname: ""` clear.
  - Nullable entity columns that must be clearable through `updateById` carry `@TableField(updateStrategy = FieldStrategy.ALWAYS)` (Note.subjectId, FlashcardDeck.subjectId, LearningTask.subjectId/dueAt/completedAt, StudySession.subjectId/title) — safe because every service loads the row before updating. **Do NOT add ALWAYS to `User` fields**: `AuthService.recordLogin()` writes a partial entity.
  - `SubjectService.resolveOwnedSubjectId(userId, wireId)` is the single resolver for optional wire subject ids (null/blank → null; malformed → SUBJECT_NOT_FOUND; foreign → SUBJECT_ACCESS_DENIED). Note/Deck/Task/Session create+update all use it.
- A7 done: `CreateNoteRequest`/`UpdateNoteRequest`/`CreateDeckRequest`/`UpdateDeckRequest` all have optional `subjectId`.
- `GlobalExceptionHandler` now maps `MissingServletRequestParameterException` and `MethodArgumentTypeMismatchException` to 40001 (previously escaped as 500 — mattered for the mandatory calendar window params). Also fixed: invalid subject status / material type now raise their reserved codes (110002/120002) instead of a raw `IllegalArgumentException` 500.
- `AiConversation` entity has `subjectId` (nullable) alongside `subjectName` (kept as denormalized display snapshot). **Not yet wired into `AiConversationService`/`ContextHints`** — that's step 5 (A10).
- Migrations: V4 (`user_preferences` — theme/locale/daily_goal_minutes, defaults implied when absent, no seed rows) and V5 (`ai_conversations.subject_id` + index) are applied and verified against dev MySQL (both fresh-DB and incremental-from-V3 paths tested).
- `pom.xml`: `flyway-mysql` no longer pins an undefined `${flyway.version}` property (Boot's BOM manages it now).

## What exists now (test infra)

- `src/test/resources/schema.sql` (H2, Flyway disabled on `test` profile) mirrors **V1 through V5** — subjects, learning_materials, notes, flashcard_decks, flashcards, learning_tasks, study_sessions, ai_conversations, ai_messages, user_preferences. **Keep this file in sync any time you add a migration** — the note at the top of the file says so. (Steps 3–4 added no migrations, so it is still current.)
- Service-level test classes, all `@SpringBootTest` + `@ActiveProfiles("test")`, error codes asserted via `isInstanceOfSatisfying(BusinessException.class, e -> ... getErrorCode())`:
  - `subject/SubjectServiceTest` — CRUD, cross-user 403, derived counts, delete cascade, note/deck subject linkage (validated create, foreign rejected, `""` unlink persisted).
  - `task/TaskServiceTest` — CRUD, completedAt lifecycle (incl. persistence of the cleared value), filters, dueAt=0 sentinel, cross-user, subject linkage + delete-unlink.
  - `calendar/StudySessionServiceTest` — window overlap, time-range validation on create/update, clear sentinels, cross-user, foreign subject rejected.
  - `preference/PreferenceServiceTest` — defaults without row creation, lazy upsert + partial application, invalid theme/locale, per-user isolation.
  - `auth/AuthFlowIntegrationTest` — full token lifecycle (MockMvc) + profile update/clear + `/me` createdAt.
- **Test gotcha**: `DATETIME` columns round fractional seconds on persist — anchor test instants with `Instant.now().truncatedTo(ChronoUnit.SECONDS)` or duration assertions drift by ±1s (bit us once in `StudySessionServiceTest`).
- Full suite: `cd ai-learning-server && ./mvnw test` — **32 tests, all green** as of this handoff. `./mvnw package -DskipTests` also verified.

## Nothing changed on the frontend yet

Steps 1–2 were backend-only. All 8 `features/*/mock.ts` files are still in place and unmodified; frontend work starts at step 6 (B1).

## How to resume

1. Read `D:\claude-data\plans\refactored-wiggling-floyd.md` in full (or this file if that path is unavailable — it's outside the repo, so confirm it exists in the new environment first).
2. Continue at **step 5 (A9+A10)** — the last backend step:
   - **A9 Analytics read model** (`analytics/` package, 180000s): `GET /v1/analytics/summary` (weekMinutes, weekDeltaPercent *nullable* to avoid div-by-zero, streakDays, taskCompletionPercent, aiChatsThisWeek), `GET /v1/analytics/activity?days=N≤90` (zero-filled per day — feeds both bar chart and heatmap; heatmap uses `days=84`), `GET /v1/analytics/subject-shares?days=30`. On-the-fly aggregation over existing tables (D5, no new tables). Streak per D6: consecutive days with ≥1 session *ending* today or yesterday, server default TZ. **Unit-test the streak: empty / gap / today-missing cases.**
   - **A10 Workspace summary** (`workspace/` package, 170000s): single `GET /v1/workspace/summary` DTO — stats (streak, studiedToday, dailyGoal from preferences, dueCards = count `due_at ≤ now`, activeSubjects), continueLearning (top-3 subjects by recent linked activity), upcomingTasks (5), recentConversations (3), recentNotes (3), todaySessions, weekActivity. Reuse the analytics streak logic — put the shared computation where both can call it (e.g. a package-visible service in `analytics/` injected into workspace, matching how MaterialService uses SubjectService).
   - **A10 AI context upgrade**: add `subjectId` to `ContextHints`; when present, `LearningContextService` resolves the *owned* subject (name/description/materials/notes) server-side; persist `subjectId` on the conversation (use `resolveOwnedSubjectId`); string hints stay backward-compatible (legacy hint-only chat must still stream). Read `ai/context/*`, `ai/service/AiConversationService`, and `docs/ai-engine.md` before touching this.
3. Mirror the established patterns exactly: package-by-feature, `OwnershipGuard`, `SubjectService.resolveOwnedSubjectId()` for optional subject ids, record DTOs with `from()`, epoch-ms wire instants, reserved error-code ranges (now enumerated in `docs/architecture.md`). Read models get controllers/services/DTOs but no new entities or migrations.
4. After step 5, backend is complete — steps 6–11 are frontend (start by reading `ai-learning-web/src/api/modules/note.ts`, `composables/`, and the mock files being replaced), step 12 theme, step 13 docs.
5. Verify each module the same way steps 1–4 were verified: `./mvnw test`, then boot the dev server (`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`) and curl a matrix including cross-user 403s. **Known gotcha**: this Windows/Git Bash environment leaves stray Java processes holding port 8080 between sessions — check `netstat -ano | grep :8080` and `taskkill //PID <pid> //F` before re-launching; and avoid non-ASCII characters in curl `-d` payloads run through Git Bash (mangles UTF-8 and produces a spurious 500 — use ASCII test data, the mangling is a shell artifact, not an app bug).
6. Demo login: `demo` / `Demo123456` (seeded by `DevUserSeeder`, dev profile only, no domain content). The dev DB is clean — everything created during step 3/4 curl verification was deleted afterwards (preferences reset to defaults, nickname restored to "Demo").
7. Local dev: JDK 22, MySQL 9.1 (root/`1234`, db `ai_learning`), backend `:8080`, frontend `:5173`.

## Verification baseline for every future step

Backend: `./mvnw compile test`. Frontend (once started): `npm run type-check && npm run test:unit && npm run build`, plus the i18n mirror test after any locale change. Full phase acceptance criteria are in the plan file's Verification section.
