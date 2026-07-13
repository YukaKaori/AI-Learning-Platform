# Phase 7 Handoff — Commercial Product Foundation

Status as of 2026-07-13. This document is the resume point for continuing Phase 7 in a new session. Read this first; the approved plan with full rationale lives at `D:\claude-data\plans\refactored-wiggling-floyd.md` (outside the repo — paste its contents into the new session if that path isn't reachable there, or regenerate from this handoff plus `docs/architecture.md`).

## What Phase 7 is

The repo was restored to the final Phase 6 commit (`85f7df3`) and Phase 7 was **redesigned from scratch** as "Commercial Product Foundation" — turning the Phase 6 codebase (real Notes/Flashcards/AI chat, mock Subjects/Tasks/Calendar/Analytics/Workspace) into a per-user, database-driven SaaS foundation with a premium 3-mode theme and a productivity-app Workspace. Full context, all 10 design decisions (D1–D10), and the complete 13-step plan are in the plan file above — **do not re-derive them, read the plan file.**

**Ignore any reference to an earlier "Phase 7 — Data Realization" attempt** (it used different error-code ranges, e.g. Subject 140000) — that attempt was discarded before this redesign began and no longer exists in the repo.

Out of scope this phase (do not build): registration/email/password-reset, payments, OSS/file upload, external auth, spaced-repetition engine, WebSockets, Docker/CI.

## Progress: steps 1–2 of 13 done

| # | Step | Status |
|---|------|--------|
| 1 | A1+A2 — Migrations (V4 user_preferences, V5 ai_conversations.subject_id), pom.xml flyway-mysql fix, `common/OwnershipGuard` + retrofit | **Done**, commit `1a20f23` |
| 2 | A3+A4 — Subject module (110000s) + Material module (120000s) | **Done**, commit `d93f07c` |
| 3 | A5+A6+A7 — Task (150000s), Calendar/StudySession (160000s), Note/Deck subjectId linkage | **Not started — resume here** |
| 4 | A8 — Preferences (200000s) + auth prep (`/me` createdAt, `PUT /auth/profile`) | Not started |
| 5 | A9+A10 — Analytics read model (180000s), Workspace summary (170000s), AI context subjectId upgrade | Not started |
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
- `AiConversation` entity now has `subjectId` (nullable) alongside `subjectName` (kept as denormalized display snapshot). **Not yet wired into `AiConversationService`/`ContextHints`** — that's step 5 (A10).
- Migrations: V4 (`user_preferences` — theme/locale/daily_goal_minutes, defaults implied when absent, no seed rows) and V5 (`ai_conversations.subject_id` + index) are applied and verified against dev MySQL (both fresh-DB and incremental-from-V3 paths tested).
- `pom.xml`: `flyway-mysql` no longer pins an undefined `${flyway.version}` property (Boot's BOM manages it now).

## What exists now (test infra)

- `src/test/resources/schema.sql` (H2, Flyway disabled on `test` profile) now mirrors **V1 through V5** — subjects, learning_materials, notes, flashcard_decks, flashcards, learning_tasks, study_sessions, ai_conversations, ai_messages, user_preferences. **Keep this file in sync any time you add a migration** — the note at the top of the file says so.
- `subject/SubjectServiceTest.java` (`@SpringBootTest`, `test` profile) covers: CRUD round-trip, cross-user 403 (`SUBJECT_ACCESS_DENIED` / material listing denied), derived counts + study minutes, and the full delete-cascade assertion (material soft-deleted, note/conversation survive with `subjectId` nulled, `subjectName` untouched). Use this as the pattern for a `TaskServiceTest`/`CalendarServiceTest` in step 3.
- Full suite: `cd ai-learning-server && ./mvnw test` — 14 tests, all green as of this handoff.

## Nothing changed on the frontend yet

Steps 1–2 were backend-only. All 8 `features/*/mock.ts` files are still in place and unmodified; frontend work starts at step 6 (B1).

## How to resume

1. Read `D:\claude-data\plans\refactored-wiggling-floyd.md` in full (or this file if that path is unavailable — it's outside the repo, so confirm it exists in the new environment first).
2. Continue at **step 3**: Task module (150000s, `/api/v1/tasks`, filters `status`/`dueBefore`/`subjectId`, `completedAt` set/cleared on status transition), Calendar module (160000s, `/api/v1/study-sessions`, mandatory `?from=&to=` window, `endsAt > startsAt` validation), then add optional validated `subjectId` to Note/Deck create+update requests (A7).
3. Mirror the Subject/Material pattern exactly: package-by-feature (`task/`, entity+mapper already exist), `OwnershipGuard` for ownership checks, `SubjectService.requireOwned()` when validating an optional `subjectId`, record DTOs with `from()`, reserved error-code ranges from `docs/architecture.md` (already confirmed: task 150000, calendar 160000).
4. Verify each module the same way steps 1–2 were verified: `./mvnw test`, then boot the dev server (`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`) and curl a matrix including cross-user 403s. **Known gotcha**: this Windows/Git Bash environment leaves stray Java processes holding port 8080 between sessions — check `netstat -ano | grep :8080` and `taskkill //PID <pid> //F` before re-launching; and avoid non-ASCII characters in curl `-d` payloads run through Git Bash (mangles UTF-8 and produces a spurious 500 — use ASCII test data, the mangling is a shell artifact, not an app bug).
5. Demo login: `demo` / `Demo123456` (seeded by `DevUserSeeder`, dev profile only, no domain content).
6. Local dev: JDK 22, MySQL 9.1 (root/`1234`, db `ai_learning`), backend `:8080`, frontend `:5173`.

## Verification baseline for every future step

Backend: `./mvnw compile test`. Frontend (once started): `npm run type-check && npm run test:unit && npm run build`, plus the i18n mirror test after any locale change. Full phase acceptance criteria are in the plan file's Verification section.
