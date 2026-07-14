# Phase 7 Handoff — Commercial Product Foundation

Status as of 2026-07-14 (fourth session — step 6 completed; frontend workstream B has begun). This document is the resume point for continuing Phase 7 in a new session. Read this first; the approved plan with full rationale lives at `D:\claude-data\plans\refactored-wiggling-floyd.md` (outside the repo — confirm it exists in the new environment, or regenerate from this handoff plus `docs/architecture.md`).

## What Phase 7 is

The repo was restored to the final Phase 6 commit (`85f7df3`) and Phase 7 was **redesigned from scratch** as "Commercial Product Foundation" — turning the Phase 6 codebase into a per-user, database-driven SaaS foundation with a premium 3-mode theme and a productivity-app Workspace. Full context, all 10 design decisions (D1–D10), and the complete 13-step plan are in the plan file above — **do not re-derive them.**

**Ignore any reference to an earlier "Phase 7 — Data Realization" attempt** (different error-code ranges, e.g. Subject 140000) — discarded before this redesign.

Out of scope this phase (do not build): registration/email/password-reset, payments, OSS/file upload, external auth, spaced-repetition engine, WebSockets, Docker/CI.

## Session summary (2026-07-14, step 6)

Implemented B1 + B2 as one step and committed it as `c73f203`. Decisions made this session (downstream steps should respect them):

- **`useAsync` is the D3 contract**: `{ data, loading, error, reload }`, runs immediately on setup, stale-run guard (only the latest run writes), non-`ApiError` failures normalized so templates always render `t(error.messageKey)`. Every data-driven view in steps 7–11 uses it — don't hand-roll load state again.
- **Views that mutate list items keep a local working copy** (`ref<T[]>`) synced from `useAsync().data` by a `watch` — see NotesView (`notes`) and AiTutorView (`conversations`, which also carries loaded message threads across list rebuilds). Read-only views can bind `data` directly.
- **`StatTile` API**: `label` (required), `value` (pre-formatted string), optional `icon`; the default slot replaces the value for richer content (B6's progress ring goes there). Formatting (durations, units, honest "—" for null metrics) stays in the view. Visuals match the old per-view `.stat-tile` CSS exactly — B6/B7/B9 should swap the copy-pasted tiles for it.
- **API-module naming convention** (all 7 new modules follow it): `XxxDto` mirrors the backend response record, `CreateXxxPayload`/`UpdateXxxPayload` mirror the requests, string-literal unions for closed vocabularies (`SubjectStatus`, `TaskStatus`, `TaskPriority`, `MaterialType`, `ThemePreference`, `LocalePreference`). Clear sentinels (`subjectId: ''`, `dueAt: 0`, `title: ''`) are documented on the payload types.
- **AI send seam is typed**: `SendMessagePayload` lives in `api/modules/ai.ts`; `ServerSseChatProvider`'s constructor context is `Omit<SendMessagePayload, 'content'>` and already forwards `subjectId`. **B4 only needs to pass `subjectId` into the provider context and `CreateConversationPayload`** — no provider surgery.
- **AiTutorView deep-link fix** (landed as part of the retrofit): the `summaries` watch re-triggers `loadDetail(activeId)` once the list arrives, so `/ai-tutor/:id` opened directly now loads history (it silently didn't before, because the immediate `activeId` watcher ran against an empty list).
- `src/types/components.d.ts` is auto-generated (unplugin-vue-components) — it picked up `StatTile`; always commit it alongside new components.

Commits this session: `c73f203` (feat, step 6) and the docs commit containing this file (also adds `.claude/skills/verify/SKILL.md`, the repo's launch-and-drive verification recipe).

## Current project status

- **Phase**: 7 (Commercial Product Foundation), phases 1–6 complete.
- **Step**: 6 of 13 **done**. Next: **step 7 (B3+B4)** — Subjects frontend + cross-feature subject linkage.
- **Branch**: `main`. Working tree: clean after the docs commit. Not pushed (push via SSH when asked; port 22 may be blocked → ssh.github.com:443).
- Commit history for the phase: `1a20f23` (step 1) → `d93f07c` (step 2) → `ccdd41a` (step 3) → `cac1706` (step 4) → `b04b8b3` (docs) → `cdea352` (step 5) → `a9b1ed2` (docs) → `c73f203` (step 6) → this docs commit.

## Progress: steps 1–6 of 13 done

| # | Step | Status |
|---|------|--------|
| 1 | A1+A2 — Migrations V4/V5, pom fix, `common/OwnershipGuard` | **Done**, `1a20f23` |
| 2 | A3+A4 — Subject (110000s) + Material (120000s) modules | **Done**, `d93f07c` |
| 3 | A5+A6+A7 — Task (150000s), Calendar (160000s), Note/Deck subjectId | **Done**, `ccdd41a` |
| 4 | A8 — Preferences (200000s) + auth prep (`/me` createdAt, `PUT /auth/profile`) | **Done**, `cac1706` |
| 5 | A9+A10 — Analytics (180000s), Workspace summary (170000s), AI subjectId context | **Done**, `cdea352` |
| 6 | B1+B2 — Frontend api modules, `useAsync` composable, `StatTile` component | **Done**, `c73f203` |
| 7 | B3+B4 — Subjects frontend + cross-feature subject linkage | **Not started — resume here** |
| 8 | B5 — Calendar + Tasks frontend | Not started |
| 9 | B6 — Workspace redesign (centerpiece) | Not started |
| 10 | B7+B8 — Analytics/Profile frontend, delete all 8 mock.ts, 404 route, index.html title | Not started |
| 11 | B9 — Preferences wiring (Settings, theme/locale reconciliation) | Not started |
| 12 | C1+C2 — Dark theme black+purple luxury glass re-skin, UX unification pass | Not started |
| 13 | D — Docs (architecture.md Phase 7 section, new mock-migration.md, etc.) | Not started |

## What exists now (backend — complete; steps 7–11 consume it, don't change it)

Unchanged from the previous handoff: OwnershipGuard; subject/material/task/calendar/preference/analytics/workspace modules; AI subject context; auth prep; wire conventions; 53 backend tests green. Do not modify backend code in steps 7–11 unless a defect is found. Key reminders:

- `AnalyticsService.streakDays()`/`activity()` are the single source of streak/per-day math; `ActivityDayResponse.date` is an ISO local date string; subject-shares has a null-id bucket; workspace reuses other modules' DTOs.
- `AiConversation` has ALWAYS-update fields — never write it via a partial-entity `updateById`; load the row first.
- `SubjectService.resolveOwnedSubject(userId, wireId)` when a caller needs subject fields.

## What exists now (frontend — new this session)

- **`api/modules/`** now covers the entire Phase 7 backend: `subject.ts`, `material.ts`, `task.ts`, `calendar.ts`, `workspace.ts`, `analytics.ts`, `preferences.ts` (all new), plus extended `auth.ts` (`AuthUser.createdAt`, `updateProfile`) and `ai.ts` (`subjectId` on `ConversationSummaryDto`, `CreateConversationPayload`, `SendMessagePayload`). Wire types were written against the actual Java records, not the plan text.
- **`composables/useAsync.ts`** + `composables/__tests__/useAsync.spec.ts` (6 cases: resolve, ApiError pass-through, normalization, reload-after-error, stale success discarded, stale failure discarded).
- **`components/StatTile.vue`** — barrel-registered, showcased in `/design-system` under "Stat tiles" (4 demo tiles incl. "—" and slot content).
- **NotesView / AiTutorView** initial-list loads now render Skeleton → content | empty | error-with-retry (`.list-state`/`.conv-state` wrappers, `AppEmpty` + `common.retry`). Mutation-path `console.error` catches were deliberately left — feedback unification is C2's toast pass, not D3.
- **No locale changes were needed** — the error keys (`error.network` etc.) and `common.retry` already existed; the i18n mirror test was therefore not in scope this step.

## Frontend still on mocks (expected until later steps)

All 8 `features/*/mock.ts` files still exist and are still imported by Workspace/Analytics/Calendar/Profile/Subjects views and the `getSubject()` enrichment in Notes/AiTutor/Flashcards. They are deleted in step 10 (B8) after their consumers are rewired (B3–B7).

## Exact resume point

Start **step 7 (B3+B4)** — Subjects frontend + subject linkage:

1. Read this file, then `features/subjects/SubjectsView.vue`, `SubjectDetailView.vue`, `features/subjects/types.ts` + `mock.ts` (shapes being replaced), and `api/modules/subject.ts`/`material.ts` (already done).
2. B3: Pinia `subjects` store (list cache + `byId` — replaces every mock `getSubject()` enrichment); SubjectsView/SubjectDetailView real CRUD dialogs (accent-token colors, icon registry); materials tab (links only, upload "coming soon" per D8); delete dialog wording per D2 (materials deleted, notes/decks/tasks/sessions/conversations unlinked); empty state = "Create your first subject" CTA + suggested-starter chips creating real subjects.
3. B4: subject picker in Note editor & deck dialogs (uses note/deck `subjectId` from step 3's backend); AI-Tutor subject selector passes `subjectId` through `CreateConversationPayload` and the provider context (seam already typed); NotesView/FlashcardsView/AiTutorView drop their `subjects/mock` imports.
4. Use `useAsync` for all loads (working-copy watch pattern where views mutate items); keep `StatTile` in mind for any tile markup touched.
5. Verification baseline: `npm run type-check && npm run test:unit && npm run build`, i18n mirror test after any locale change (B3/B4 **will** add locale strings — mirror zh/en), then launch + drive per `.claude/skills/verify/SKILL.md`.

## Verification performed (step 6)

- **Static/tests/build**: `vue-tsc` clean; vitest 8/8 (locales mirror + 6 useAsync); `vite build` clean.
- **Runtime (Playwright drive of the real app, chromium, demo user)**: login → notes list loads (2 pre-existing notes); blocked `**/v1/notes` → error state rendered (`网络连接失败，请稍后重试` + 重试) → unblock + click retry → list recovered. Same cycle for AI Tutor conversations. Deep link `/ai-tutor/2076312481618776066` on a fresh mount → history loaded (the fixed path). SSE send with the new typed payload → conversation created, stream returned the graceful no-key error reply (expected; no `DEEPSEEK_API_KEY` here). `/design-system` → 4 StatTiles render correctly (icon chip, value, caption, "—", slot content). Screenshots captured for error state and StatTile section.
- **Data hygiene**: the drive-created conversation was deleted via API afterwards; conversations back to the single Phase 6 leftover. Dev servers stopped, ports 8080/5173 verified free.
- **Intentionally not verified**: live DeepSeek streaming (no API key — same standing gap as before); the 7 new API modules' request paths beyond what the app exercises today (their first UI consumers arrive in steps 7–11; shapes were verified against the Java records and the live conversations list already shows `subjectId`).

## Known risks / gotchas

- **PowerShell mangles `""` in `git commit -m` here-strings** — write the message to a file and `git commit -F <file>`. Avoid non-ASCII in Git-Bash curl `-d` payloads.
- **Stray Java/node processes hold ports 8080/5173 between sessions** — `netstat -ano | grep -E ':(8080|5173).*LISTEN'`, then `taskkill //PID <pid> //F`. TaskStop on the background task does **not** reliably kill the child server process.
- **Browser verification recipe** (Playwright via npx cache, login selectors, route-abort for error states, row-button off-by-one gotcha) is written down in `.claude/skills/verify/SKILL.md` — use it instead of re-deriving.
- **Login field is `usernameOrEmail`** (`{"usernameOrEmail":"demo","password":"Demo123456"}`).
- **Dev DB is NOT fully clean**: two notes (`未命名笔记`, `Verify Note`) and one conversation (`Hello, explain recursion`) remain from *Phase 6* verification — deliberately left. Everything created by step-6 verification was removed.
- Analytics/streak use **server default timezone**; day-bucket test assertions assume no midnight straddle.
- MySQL root password `1234`; mysql CLI at `C:\Program Files\MySQL\MySQL Server 9.1\bin\mysql.exe`. JDK 22, backend :8080, frontend :5173, demo login `demo`/`Demo123456`.

## Documentation state

- `docs/ai-engine.md` — in sync (subject-resolution contract, updated step 5).
- `docs/architecture.md` — untouched this session; Phase 7 section lands in step 13 per plan (plus the new `docs/mock-migration.md`).
- `.claude/skills/verify/SKILL.md` — **new**: project verification recipe (launch, API drive, Playwright drive, data hygiene).
- This file — rewritten for the step-6 boundary.
