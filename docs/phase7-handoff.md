# Phase 7 Handoff — Commercial Product Foundation

Status as of 2026-07-16 (ninth session — step 11 completed). This document is the resume point for continuing Phase 7 in a new session. Read this first, together with `docs/DEVELOPMENT_GUIDE.md` (the long-term engineering reference); the approved plan with full rationale lives at `D:\claude-data\plans\refactored-wiggling-floyd.md` (outside the repo — confirm it exists, or regenerate from this handoff plus `docs/architecture.md`).

## What Phase 7 is

The repo was restored to the final Phase 6 commit (`85f7df3`) and Phase 7 was **redesigned from scratch** as "Commercial Product Foundation" — a per-user, database-driven SaaS foundation with a premium 3-mode theme and a productivity-app Workspace. Full context, design decisions D1–D10, and the 13-step plan are in the plan file — **do not re-derive them.** Ignore any reference to an earlier "Phase 7 — Data Realization" attempt (discarded).

Out of scope this phase (do not build): registration/email/password-reset, payments, OSS/file upload, external auth, spaced-repetition engine, WebSockets, Docker/CI.

## Session summary (2026-07-16, step 11)

Repo state matched the previous handoff exactly (clean tree at `8e49f6f`); implemented B9 (`921fa57`) — Preferences wiring (Settings view + `stores/app.ts` reconciliation with the database-backed preferences module built in step 4). Backend for this step was already complete and untouched (`PreferenceController`/`PreferenceService`/`UserPreference` from A8); this step was pure frontend wiring.

Decisions/conventions established this session (downstream steps should respect them):

- **`stores/app.ts` keeps localStorage as the FOUC-safe boot path** (unauthenticated pages — login/welcome — still get instant correct theme/locale with no server round trip), but now also holds `dailyGoalMinutes` (default 60, matching `PreferencesResponse.DEFAULTS`) and two new actions:
  - `reconcileFromServer()` — `GET /preferences`, then applies the result (server wins). Swallows its own errors (offline/transient failure just means the local values stand until the next successful sync) — **never throws**, so callers don't need to guard it.
  - `updatePreferences(payload)` — applies the change locally first (instant UI feedback, matching the existing `setThemeMode`/`setLocale` chip-click feel), then `PUT /preferences` and adopts the server echo (same "adopt the server echo" pattern as `authStore.updateProfile`). **Throws** on failure so the caller (`SettingsView`) can show an inline error.
- **`stores/auth.ts` calls `useAppStore().reconcileFromServer()`** right after `login()` and after the `getCurrentUser()` call inside `restoreSession()` — this is the one-time-per-session sync point per D4 ("server value reconciles after `/me` and wins"). Placed *inside* `restoreSession()`'s existing try/catch (harmless either way since `reconcileFromServer` never throws, but keeps the call conditional on a successful session restore). The router guard (`router/guards.ts`) already `await`s `restoreSession()` before resolving the first protected navigation, so there's no separate wiring needed to avoid a flash of stale-device preferences on a protected route.
- **`SettingsView.vue`**: the appearance/language chip handlers (`selectThemeMode`/`selectLocale`) now call `appStore.updatePreferences(...)` instead of the old local-only `setThemeMode`/`setLocale` calls directly (those two actions still exist unchanged and are still what `updatePreferences`/`reconcileFromServer`/`applyServerPreferences` call under the hood — no duplicate logic). A shared `preferenceErrorKey` renders one inline error line below the two chip cards on failure (no toast system exists in this codebase yet — deferred to C2, matches the existing "toast feedback lands in C2" comment already in `WorkspaceView.vue`).
- **New daily-goal-minutes control**: a native `<form @submit.prevent>` wrapping an `AppInput type="number"` + a submit `AppButton` (mirrors `ProfileView`'s save-button pattern, not a dialog since it's one field inline in a card). Client-side range validation (1–1440, integer) runs before any network call — an out-of-range value never reaches the API, shows `settings.dailyGoal.invalid`, and reverts the draft to the last-known-good value only on an actual API failure (not on validation failure, so the user's typed value stays visible to correct). This directly feeds `WorkspaceStatsDto.dailyGoalMinutes`, which `WorkspaceService` already reads from `PreferenceService` server-side (confirmed in code, no workspace change needed — the ring updates on next Workspace load).
- **No lifetime-total-style workaround needed here** — unlike step 10's Profile decision, this step's data (theme/locale/goal) already had a real backend from step 4; the only "missing piece" was frontend wiring, not a data-modeling question.

Commits this session: `921fa57` (feat, step 11), plus the docs commit containing this file.

## Current project status

- **Phase**: 7 (Commercial Product Foundation), phases 1–6 complete.
- **Step**: 11 of 13 **done**. Next: **step 12 (C1+C2)** — Dark theme black+purple luxury glass re-skin + UX unification pass.
- **Branch**: `main`. Working tree: clean after the docs commit. Not pushed (push via SSH when asked; port 22 may be blocked → ssh.github.com:443).
- Phase commits: `1a20f23` → `d93f07c` → `ccdd41a` → `cac1706` → `b04b8b3` (docs) → `cdea352` → `a9b1ed2` (docs) → `c73f203` → `d235259` (docs) → `7825ceb` (guide) → `cbbff8b` (step 7) → `cd65d8d` (docs) → `f16a81a` (step 8) → `358ee52` (docs) → `3f0cc94` (step 9) → `4dcdd73` (docs) → `c0c6d3a` (step 10) → `8e49f6f` (docs) → `921fa57` (step 11) → this docs commit.

## Progress: steps 1–11 of 13 done

| # | Step | Status |
|---|------|--------|
| 1 | A1+A2 — Migrations V4/V5, pom fix, `common/OwnershipGuard` | **Done**, `1a20f23` |
| 2 | A3+A4 — Subject (110000s) + Material (120000s) modules | **Done**, `d93f07c` |
| 3 | A5+A6+A7 — Task (150000s), Calendar (160000s), Note/Deck subjectId | **Done**, `ccdd41a` |
| 4 | A8 — Preferences (200000s) + auth prep | **Done**, `cac1706` |
| 5 | A9+A10 — Analytics (180000s), Workspace (170000s), AI subjectId context | **Done**, `cdea352` |
| 6 | B1+B2 — Frontend api modules, `useAsync`, `StatTile` | **Done**, `c73f203` |
| 7 | B3+B4 — Subjects frontend + cross-feature subject linkage | **Done**, `cbbff8b` |
| 8 | B5 — Calendar + Tasks frontend | **Done**, `f16a81a` |
| 9 | B6 — Workspace redesign (centerpiece) | **Done**, `3f0cc94` |
| 10 | B7+B8 — Analytics/Profile frontend, delete all 8 mock.ts, 404 route, index.html title | **Done**, `c0c6d3a` |
| 11 | B9 — Preferences wiring (Settings, theme/locale reconciliation) | **Done**, `921fa57` |
| 12 | C1+C2 — Dark theme black+purple luxury glass re-skin, UX unification pass | **Not started — resume here** |
| 13 | D — Docs (architecture.md Phase 7 section, new mock-migration.md, etc.) | Not started |

## What exists now

**Backend** — complete and stable since step 5; step 11 consumed the step-4 Preferences module unchanged (53 tests green, unchanged this session). Reminders: `AnalyticsService.streakDays()/activity()` are the only streak/per-day math **and count only ended sessions**; `AiConversation` has ALWAYS-update fields (never partial `updateById`); `resolveOwnedSubject` for subject fields; task/calendar wire conventions documented on the DTOs (`dueAt: 0` unschedules, `endsAt > startsAt` validated, calendar list requires `?from=&to=`); `WorkspaceService` already reads `dailyGoalMinutes` from `PreferenceService` — no backend change was needed for step 11.

**Frontend (through step 11)** — full `api/modules/` coverage; `useAsync` + `toApiError`; `StatTile`; subjects store + picker + form dialog; SubjectsView/SubjectDetailView fully real; note/deck/AI-conversation subject linkage; CalendarView fully real; `TaskFormDialog`/`SessionFormDialog` shared by Calendar and Workspace; WorkspaceView fully real; AnalyticsView fully real; ProfileView fully real; **SettingsView fully real** (theme/locale/daily-goal all persist to the database and reconcile cross-device on login/session-restore — no more localStorage-only settings). No `mock.ts` files remain anywhere in `features/`. Branded `NotFoundView` + catch-all route. Real `index.html` title/meta/favicon.

## Exact resume point

Start **step 12 (C1+C2)** — Dark theme black+purple luxury glass re-skin + UX unification pass:

1. Read this file, `docs/DEVELOPMENT_GUIDE.md`, and the C1/C2 spec in the plan file (§ Workstream C), then `ai-learning-web/src/styles/tokens.css` (the `html.dark` block specifically) and `ai-learning-web/src/styles/element-theme.css` (the EP bridge).
2. **C1** — re-skin dark mode only, tokens-only: near-black purple-tinted surfaces (~`#09090e` base, elevation steps never gray-blue), refined muted purple primary ramp (validate 4.5:1 contrast, no neon), glass-group components get more blur/translucency + purple-tinted border/inner highlight, layered shadows + very-low-alpha purple glow **only** on interactive emphasis (buttons/active nav/focused cards — never ambient background glow), scene group matched, light theme gets only micro-polish. Re-validate the `element-theme.css` bridge across every EP surface used in the app (dialogs, `el-date-picker`, `el-time-picker`, `el-tooltip`, `el-select`) — flagged as a standing gap since step 9's handoff ("generic CSS-var bridge only", never re-validated in dark mode).
3. **C2** — UX unification pass: consistent spacing/typography/dialog/toast/hover/focus-visible/transitions from motion tokens across all views built in steps 6–11; skeletons match final layouts; keyboard reachability of every dialog added since step 6 (Subject form, Task/Session form, Profile edit, and this session's inline goal-save form); `prefers-reduced-motion` honored; both locales checked for overflow at 375px. This step is also the natural place to finally add a lightweight toast/message primitive if one is needed for background-persist errors (Settings' `preferenceErrorKey` and Profile's future soft failures reference "toast feedback lands in C2" — check whether a lightweight toast is actually needed or whether the existing inline-error pattern is sufficient before building one; don't build a toast component speculatively).
4. Verify per `docs/DEVELOPMENT_GUIDE.md` + `.claude/skills/verify/SKILL.md`: static baseline, i18n mirror, then a Playwright drive across all 3 theme modes on every view (including `/design-system`), both locales, at 1440px and 375px, with the usual data hygiene (nothing created this step should need cleanup unless a toast/message demo is added).

## Verification performed (step 11)

- **Static**: `vue-tsc --build` clean; vitest 8/8 (unchanged suite — no new unit tests needed, i18n mirror still enforced); `oxlint`+`eslint --fix` clean; `vite build` clean.
- **Backend**: `./mvnw test` — 53/53 green, unchanged (Preferences module untouched, consumed as-is).
- **API** (curl, demo user): full preferences round trip — GET baseline (`system/zh-CN/60`), PUT partial updates (theme, then dailyGoalMinutes independently — confirmed partial-update semantics preserve the other field), PUT with an invalid theme value correctly rejected (`code 200000`, "Unknown theme preference"), restored to defaults.
- **Runtime (Playwright, chromium, demo user, 1440px)**: two drives, 13 checks total, **all passed**, zero uncaught page errors.
  - Drive 1 (same browser context throughout): initial goal reads 60; selecting the dark theme chip applies `html.dark` instantly and survives a reload; selecting English updates `document.documentElement.lang` instantly and survives a reload; setting the goal to 90 via the inline save form survives a reload; the Workspace view's rendered text reflects the new 90-minute goal (confirms `WorkspaceService` reading the updated preference, no workspace code change needed); an out-of-range goal (99999) shows the inline `settings.dailyGoal.invalid` error and is never sent to the API.
  - Drive 2 (**fresh browser context, no localStorage carried over** — simulates a different device): logging in as `demo` adopts the server's saved `dark`/`en-US` theme/locale immediately (proves `reconcileFromServer()` actually runs on login and wins over the fresh context's empty localStorage, which is the core D4 requirement this step exists to satisfy) and Settings shows the server's goal of 90; then restored the account to `system`/`zh-CN`/`60` through the UI (not the API) and confirmed the restoration persisted across a reload.
  - Confirmed via a direct API call after both drives that the demo account's preferences are exactly `{"theme":"system","locale":"zh-CN","dailyGoalMinutes":60}` — byte-for-byte the original baseline.
- **Environment note**: the backend process crashed once mid-verification with a native JVM access violation (`exit code -1073741819`) during an unrelated `SELECT ... WHERE username = ? OR email = ?` login query — no `hs_err_pid*` dump was found, and the crash occurred *after* the preferences state had already been confirmed restored to defaults in the server log, so it did not affect verification validity. Treated as an environment flake (JDK 22 / Windows), not a defect introduced this session; the backend was restarted and confirmed healthy (`/system/info` 200, preferences GET returned the correct restored baseline) before continuing. If this recurs in a future session, worth a closer look, but not chased further here (unrelated to this step's code).
- **Data hygiene**: the only state this step's verification touched was the demo account's own `user_preferences` row, and it was restored to `system/zh-CN/60` and re-confirmed via API after the backend restart. Both dev servers were stopped and ports 8080/5173 confirmed free.

## Known risks / gotchas

- **Post-login lands on `/welcome`** — Playwright must `goto('/workspace')` (or the target route) explicitly. Unchanged from step 9.
- **Analytics counts only ended sessions** — unchanged, not touched this session.
- **Router route order matters for the catch-all** — unchanged from step 10.
- **No toast/message system exists yet** — background-persist failures (Settings chip clicks, the goal-save form) surface as a plain inline error line, not a toast. This is intentional (matches the pre-existing "toast feedback lands in C2" comment in `WorkspaceView.vue`) but C1/C2 should make a deliberate decision here rather than accumulating more one-off inline-error patterns across views.
- **`reconcileFromServer()` runs on every login and every session restore** (i.e., on every full page load while a session exists, since `restoreSession()` guards on `this.initialized` for the *current* SPA session, not across reloads) — this is one extra GET per page load for an authenticated user. Acceptable at this scale (matches D5's "measure before materializing" — no caching layer added preemptively) but worth knowing if profiling ever flags it.
- Two-dialog confirm-flow race in Playwright (Workspace/Calendar delete flows) — unchanged, not exercised this session.
- `el-date-picker`/`el-time-picker`/`el-tooltip` — **C1 must re-validate all EP surfaces in dark mode** (generic CSS-var bridge only). Unchanged from steps 9–10, now the very next step.
- `git commit -m` mangling → use `git commit -F <file>` written UTF-8 **without BOM**. Confirmed still works via the Write tool's default UTF-8-no-BOM output this session.
- Port cleanup (`netstat`+`taskkill //PID`; TaskStop does not kill child servers), login field `usernameOrEmail`, non-ASCII in Git-Bash curl bodies — unchanged; see the verify skill.
- **A JVM native crash was observed once this session** (see Verification section) — not reproduced, not chased, but noted in case it recurs.
- **Dev DB baseline (unchanged)**: two Phase 6 notes (`未命名笔记`, `Verify Note`) and one conversation (`Hello, explain recursion`), all unlinked; zero subjects/tasks/sessions; demo account preferences at defaults (`system/zh-CN/60`).
- MySQL root pwd `1234`; JDK 22; backend :8080, frontend :5173; demo login `demo`/`Demo123456` (nickname "Demo", `createdAt` 2026-07-10 per `auth/me`).

## Files modified during this session

Modified: `ai-learning-web/src/features/settings/SettingsView.vue` (theme/locale chips now persist via `appStore.updatePreferences`; new daily-goal-minutes inline form + validation), `ai-learning-web/src/locales/en-US.ts` + `zh-CN.ts` (`settings.dailyGoal.*` keys), `ai-learning-web/src/stores/app.ts` (`dailyGoalMinutes` state, `applyServerPreferences`/`reconcileFromServer`/`updatePreferences` actions), `ai-learning-web/src/stores/auth.ts` (calls `reconcileFromServer()` after login/session-restore).

No files created or deleted this session.

## Documentation state

- `docs/DEVELOPMENT_GUIDE.md` — current; nothing added this session (conventions recorded above are phase-wiring, belong in this handoff not the guide).
- `docs/ai-engine.md` — in sync, untouched this session (no AI-engine surface changed).
- `docs/architecture.md` — untouched; Phase 7 section + `docs/mock-migration.md` land in step 13.
- `.claude/skills/verify/SKILL.md` — unchanged this session.
- This file — rewritten for the step-11 boundary.

## Recommended first task for next session

Read this handoff top to bottom, confirm `git log --oneline -3` shows `921fa57` at HEAD with a clean tree, then start step 12 exactly per the "Exact resume point" section above — begin by reading `ai-learning-web/src/styles/tokens.css`'s `html.dark` block and `element-theme.css` to see what the current dark theme actually looks like before redesigning it.
