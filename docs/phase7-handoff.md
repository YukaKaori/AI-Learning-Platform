# Phase 7 Handoff — Commercial Product Foundation

Status as of 2026-07-16 (eighth session — step 10 completed). This document is the resume point for continuing Phase 7 in a new session. Read this first, together with `docs/DEVELOPMENT_GUIDE.md` (the long-term engineering reference); the approved plan with full rationale lives at `D:\claude-data\plans\refactored-wiggling-floyd.md` (outside the repo — confirm it exists, or regenerate from this handoff plus `docs/architecture.md`).

## What Phase 7 is

The repo was restored to the final Phase 6 commit (`85f7df3`) and Phase 7 was **redesigned from scratch** as "Commercial Product Foundation" — a per-user, database-driven SaaS foundation with a premium 3-mode theme and a productivity-app Workspace. Full context, design decisions D1–D10, and the 13-step plan are in the plan file — **do not re-derive them.** Ignore any reference to an earlier "Phase 7 — Data Realization" attempt (discarded).

Out of scope this phase (do not build): registration/email/password-reset, payments, OSS/file upload, external auth, spaced-repetition engine, WebSockets, Docker/CI.

## Session summary (2026-07-16, step 10)

Repo state matched the previous handoff exactly (clean tree at `4dcdd73`); implemented B7+B8 (`c0c6d3a`) — Analytics + Profile frontend wired to real endpoints, then the shell de-demoed. This session ran under a tightened remaining-budget instruction from the user partway through: finish the current step cleanly, verify, commit, document, and **do not start step 11.** That instruction is honored — step 11 (B9 Preferences wiring) has not been touched.

Decisions/conventions established this session (downstream steps should respect them):

- **AnalyticsView is one `useAsync` over `Promise.all([getAnalyticsSummary(), getActivity(84), getSubjectShares(30)])`** — a single loading/error/retry state for the whole page, matching D7/D3. The bar chart slices the last 7 days off the same 84-day series the heatmap uses (`weekActivity = activity.slice(-7)`), so there is exactly one fetch of activity data, not two.
- **Nullable analytics metrics render "—", never 0** — both `weekDeltaPercent` (as `较上周 —` / `— vs last week`) and `taskCompletionPercent`. This holds in both the stat tile *and* the AI-insights snapshot text sent to DeepSeek (`snapshotStudyTimeNoDelta` / omitted completion line when null) — the "demo data" locale note is gone; the subtitle is now a real one-liner.
- **Subject-shares null bucket renders as a themed "unlinked" row** (`analytics.subjectDistribution.unlinked`, `--color-muted` dot) rather than being dropped — percentages are computed client-side off the row set exactly as the backend returns it, shares always sum to 100%.
- **Heatmap intensity is relative to the user's own busiest day in the 84-day window** (`heatMax = max(activity.minutes)`), not a fixed constant like the old mock's `HEATMAP_MAX = 150` — with real (usually small) numbers a fixed ceiling would wash out every cell to "quiet."
- **`utils/date.ts` now owns `parseIsoDate`** (the ISO `yyyy-MM-dd` calendar-bucket parser — never `new Date(iso)`, which UTC-shifts the day). Extracted from Workspace (B6) since Analytics needed the identical function; WorkspaceView was updated to import it instead of keeping its own copy (behavior-neutral, rides with this step).
- **ProfileView's "learning overview" row is weekStudy/subjects/notes/streak** — deliberately *not* a "total study time" tile. The backend only exposes windowed analytics (`AnalyticsService` has no all-time aggregate), and summing subject `studyMinutes` would silently omit unlinked-session time; honest-data-over-fake-polish means not inventing that number client-side. If a lifetime total is wanted later, it needs a backend decision (new endpoint or documented approximation), not a frontend workaround.
- **`authStore.updateProfile()`** is a new store action (mirrors `login`/`restoreSession`'s pattern: call the API, adopt the server echo into `this.user`) so every consumer of `authStore.user` — layout avatar, workspace greeting, profile — reflects an edit immediately, no separate reload needed. `UpdateProfilePayload` fields are sent trimmed; an empty string is the documented clear-sentinel (nickname falls back to username).
- **B8 de-demo**: all 8 `features/*/mock.ts` deleted (grep-verified zero importers afterward — only two unrelated doc-comment strings containing the word "mock" remain, in `provider.ts` and `subjects/types.ts`, both fixed to not reference the deleted concept). `features/subjects/types.ts` lost its dead mock-shaped `Subject`/`LearningMaterial`/`SubjectStatus` exports (the real ones live in `api/modules/subject.ts`); kept `SubjectAccent`/`MaterialType`/`accentColor`/`subjectAccentOf`/`subjectIconOf`/`SUBJECT_ACCENTS`/`SUBJECT_ICONS`/`MATERIAL_TYPE_ICON` — all still imported elsewhere.
- **Branded 404**: `NotFoundView.vue` (full-bleed, outside `AppLayout`, no `requiresAuth`) + a catch-all `path: '/:pathMatch(.*)*'` route appended **after** the existing `AppLayout` route block in `router/index.ts` (route order matters — a catch-all before it would shadow every child route). CTA routes to `{ name: 'workspace' }`, which still passes through the auth guard like any deep link (unauthenticated visitors get bounced to `/login?redirect=...` first, which is correct).
- **`index.html`**: real title "AI Learning Platform", a `<meta name="description">`, `lang="zh-CN"` (matches `DEFAULT_LOCALE`), and a new inline-SVG favicon (`public/favicon.svg`, brand-indigo `#5e6ad2` square with a white four-point sparkle) replacing Vite's placeholder `.ico`. `nav.workspace` etc. titles were already wired via `router.afterEach` from step 6 — untouched.
- **Palette validation (dataviz skill)**: the subject-accent categorical set (`indigo/teal/amber/rose/violet`, used by the subject-shares chart) was re-validated with `scripts/validate_palette.js` against both light `#fcfcfb` and dark `#17171a` surfaces — both ALL CHECKS PASS (lightness band, chroma floor, CVD separation, contrast). No changes needed; this was a documentation-worthy re-check of an existing token set now used in a new chart, not a new palette.

Commits this session: `c0c6d3a` (feat, step 10), plus the docs commit containing this file.

## Current project status

- **Phase**: 7 (Commercial Product Foundation), phases 1–6 complete.
- **Step**: 10 of 13 **done**. Next: **step 11 (B9)** — Preferences wiring (Settings, theme/locale reconciliation).
- **Branch**: `main`. Working tree: clean after the docs commit. Not pushed (push via SSH when asked; port 22 may be blocked → ssh.github.com:443).
- Phase commits: `1a20f23` → `d93f07c` → `ccdd41a` → `cac1706` → `b04b8b3` (docs) → `cdea352` → `a9b1ed2` (docs) → `c73f203` → `d235259` (docs) → `7825ceb` (guide) → `cbbff8b` (step 7) → `cd65d8d` (docs) → `f16a81a` (step 8) → `358ee52` (docs) → `3f0cc94` (step 9) → `4dcdd73` (docs) → `c0c6d3a` (step 10) → this docs commit.

## Progress: steps 1–10 of 13 done

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
| 11 | B9 — Preferences wiring (Settings, theme/locale reconciliation) | **Not started — resume here** |
| 12 | C1+C2 — Dark theme black+purple luxury glass re-skin, UX unification pass | Not started |
| 13 | D — Docs (architecture.md Phase 7 section, new mock-migration.md, etc.) | Not started |

## What exists now

**Backend** — complete and stable since step 5; steps 10–11 consume it, don't change it (53 tests green, unchanged this session). Reminders: `AnalyticsService.streakDays()/activity()` are the only streak/per-day math **and count only ended sessions**; `AiConversation` has ALWAYS-update fields (never partial `updateById`); `resolveOwnedSubject` for subject fields; task/calendar wire conventions documented on the DTOs (`dueAt: 0` unschedules, `endsAt > startsAt` validated, calendar list requires `?from=&to=`). Preferences module (200000s, from step 4) exists and is untouched — step 11 wires its frontend.

**Frontend (through step 10)** — full `api/modules/` coverage; `useAsync` + `toApiError`; `StatTile`; subjects store + picker + form dialog; SubjectsView/SubjectDetailView fully real; note/deck/AI-conversation subject linkage; CalendarView fully real; `TaskFormDialog`/`SessionFormDialog` shared by Calendar and Workspace; WorkspaceView fully real; **AnalyticsView fully real** (4 stat tiles incl. honest "—" for null metrics, weekly bar chart sliced from the shared 84-day series, subject-shares list with an unlinked bucket, 12-week heatmap relative to the user's own max, AI insights panel unchanged in behavior but now fed real numbers); **ProfileView fully real** (real `memberSince`, weekStudy/subjects/notes/streak stat row, working nickname/avatar edit dialog wired through `authStore.updateProfile`). **No `mock.ts` files remain anywhere in `features/`.** Branded `NotFoundView` + catch-all route. Real `index.html` title/meta/favicon (`favicon.svg`). `utils/date.ts` holds the shared `parseIsoDate` used by both Workspace and Analytics.

## Exact resume point

Start **step 11 (B9)** — Preferences wiring (Settings view, theme/locale reconciliation with the backend):

1. Read this file, `docs/DEVELOPMENT_GUIDE.md`, the B9 spec in the plan file (§ Workstream B), then `features/settings/SettingsView.vue`, `api/modules/preferences.ts` (if it exists yet — check; A8 built the backend module in step 4 but the frontend api module may not exist), and the backend `preferences` package (`200000s` error range) for the actual DTO shape.
2. Establish how theme (light/dark/system) and locale (zh-CN/en-US) currently persist client-side (likely `localStorage` or a Pinia store set up pre-Phase-7) versus what the `preferences` backend module offers — the step's job is reconciling those two sources of truth into one, backed by the database, so a user's theme/locale follow them across devices/sessions.
3. Wire whatever other preference fields the backend exposes (check the DTO — daily goal minutes is referenced by `WorkspaceStatsDto.dailyGoalMinutes` per the step-9 handoff, so Settings almost certainly needs a control for it).
4. Verify per `docs/DEVELOPMENT_GUIDE.md` + `.claude/skills/verify/SKILL.md`: static baseline, i18n mirror, then a Playwright drive (change theme/locale/goal in Settings → persists across reload → persists across logout/login) with full data cleanup (restore the demo account's preferences to their original values, same pattern as this session's nickname restore in step E of the drive).

## Verification performed (step 10)

- **Static**: `vue-tsc --build` clean; vitest 8/8 (unchanged suite — no new unit tests needed, i18n mirror still enforced); `oxlint`+`eslint --fix` clean; `vite build` clean (new chunks: `AnalyticsView`, `ProfileView`, `NotFoundView`, `date.ts`).
- **Palette (dataviz skill)**: subject-accent categorical set (`#5e6ad2/#0d9488/#d97706/#e11d48/#7c3aed` light, `#6674dd/#0d9488/#d97706/#e11d48/#8b5cf6` dark) re-validated against `#fcfcfb` and `#17171a` — ALL CHECKS PASS both modes (lightness band, chroma floor, CVD separation ΔE 31.3/20.0 worst-adjacent, contrast ≥3:1).
- **API** (curl, zh locale, demo user): confirmed empty-account baseline (`weekMinutes:0, weekDeltaPercent:null, streakDays:0, taskCompletionPercent:null, aiChatsThisWeek:1`, empty shares, all-zero 84-day activity) and `auth/me` returns real `createdAt`/`nickname`.
- **Runtime (Playwright, chromium, demo user, zh locale, 1440px + 375px)**: 55-check drive, **54 passed** (the 1 "failure" was a wrong test expectation — the app's own zh-CN name is "AI 学习平台", not the English "AI Learning Platform"; not a defect, no code change needed). Covered: empty-account analytics (all 4 honest tiles, empty-state weekly chart + CTA, empty shares message, 84-cell heatmap all-muted, blocked-summary error state, unroute+retry recovery); empty-account profile (real memberSince, 0/0/2/0 stat row, enabled edit button); seeded via API (1 subject, 2 study sessions — one 75-min ended-today linked, one 30-min ended-yesterday unlinked — 1 done + 1 open task) verified against direct API summary math (105 min, streak 2, completion 50%, delta still null with only one week of data); seeded analytics UI (correct duration formatting, 2 non-zero bars + 5 honest zero stubs, selective max-day label, subject-shares 71%/29% split with the unlinked bucket row, heatmap exactly 2 intense cells, bar hover tooltip); seeded profile stats matching; **nickname edit round-trip** (dialog save → live identity-name update → API-persisted → account-detail row updated → restored to "Demo" afterward); 404 (branded page, correct doc title, CTA→workspace through the auth guard); shell (`favicon.svg` served, old `favicon.ico` gone, `analytics` route doc-title correct); 375px zero horizontal overflow on both Analytics and Profile; AI insights button exercised (error-or-content path, no `DEEPSEEK_API_KEY` in this environment so the graceful-failure path is what actually ran); zero uncaught page errors across the whole drive.
- **Data hygiene**: everything the drive created (1 subject, 2 sessions, 2 tasks) was deleted via API and the empty-account baseline was re-verified byte-for-byte identical afterward (0 subjects/tasks/sessions, 2 Phase-6 notes, `weekMinutes:0`, `streakDays:0`, `taskCompletionPercent:null`); the nickname edit was restored to "Demo". Both dev servers were stopped and ports 8080/5173 confirmed free.
- **Intentionally not verified**: live DeepSeek streaming content (no API key in this environment — standing gap, unchanged from prior sessions; the insights button's request/response/error-handling path was still exercised end-to-end).

## Known risks / gotchas

- **Post-login lands on `/welcome`** — Playwright must `goto('/workspace')` (or the target route) explicitly. Unchanged from step 9.
- **Analytics counts only ended sessions** — verification seeding must use past `endsAt` or minutes/streak/shares stay 0. Unchanged from step 9, now also load-bearing for the Analytics view itself (not just Workspace).
- **Router route order matters for the new catch-all**: `path: '/:pathMatch(.*)*'` must stay the *last* entry in the routes array — Vue Router matches top-to-bottom for equal specificity in some configurations, and this route is deliberately placed after the `AppLayout` block. Don't reorder without checking child routes still resolve first.
- **No lifetime "total study time" number exists anywhere in the product now** (Profile deliberately dropped the old mock's `totalStudy` tile — see session summary). If a future step wants one, it's a backend decision, not something to reconstruct client-side from `SubjectDto.studyMinutes` sums (would silently exclude unlinked-session time).
- Two-dialog confirm-flow race in Playwright (Workspace/Calendar delete flows) — unchanged from step 9, not exercised this session (Analytics/Profile have no delete-confirm dialogs).
- `el-date-picker`/`el-time-picker`/`el-tooltip` — **C1 must re-validate all EP surfaces in dark mode** (generic CSS-var bridge only). Unchanged from step 9.
- `git commit -m` mangling → use `git commit -F <file>` written UTF-8 **without BOM**. Confirmed still works via the Write tool's default UTF-8-no-BOM output this session.
- Port cleanup (`netstat`+`taskkill //PID`; TaskStop does not kill child servers), login field `usernameOrEmail`, non-ASCII in Git-Bash curl bodies — unchanged; see the verify skill.
- **Dev DB baseline (unchanged)**: two Phase 6 notes (`未命名笔记`, `Verify Note`) and one conversation (`Hello, explain recursion`), all unlinked; zero subjects/tasks/sessions. Everything this session's verification created was removed.
- MySQL root pwd `1234`; JDK 22; backend :8080, frontend :5173; demo login `demo`/`Demo123456` (nickname "Demo", `createdAt` 2026-07-10 per `auth/me`).

## Files modified during this session

New: `ai-learning-web/public/favicon.svg`, `ai-learning-web/src/utils/date.ts`, `ai-learning-web/src/views/NotFoundView.vue`.
Modified: `ai-learning-web/index.html`, `ai-learning-web/src/features/ai-tutor/provider.ts` (doc-comment only), `ai-learning-web/src/features/analytics/AnalyticsView.vue` (full rewrite), `ai-learning-web/src/features/profile/ProfileView.vue` (full rewrite), `ai-learning-web/src/features/subjects/types.ts` (pruned dead exports), `ai-learning-web/src/features/workspace/WorkspaceView.vue` (switched to shared `parseIsoDate`), `ai-learning-web/src/locales/en-US.ts`, `ai-learning-web/src/locales/zh-CN.ts`, `ai-learning-web/src/router/index.ts` (catch-all route), `ai-learning-web/src/stores/auth.ts` (`updateProfile` action).
Deleted: `ai-learning-web/public/favicon.ico`, all 8 `ai-learning-web/src/features/*/mock.ts`.

## Documentation state

- `docs/DEVELOPMENT_GUIDE.md` — current; nothing added this session (conventions recorded above are phase-wiring, belong in this handoff not the guide).
- `docs/ai-engine.md` — in sync (subject-resolution contract); Analytics' AI-insights snapshot text change (honest-null handling) doesn't touch the AI engine contract itself, no update needed.
- `docs/architecture.md` — untouched; Phase 7 section + `docs/mock-migration.md` land in step 13 (must document: ended-session analytics semantics, client-side suggestion rules + server-AI extension point, the no-lifetime-total decision from this session, the mock-deletion inventory).
- `.claude/skills/verify/SKILL.md` — unchanged this session.
- This file — rewritten for the step-10 boundary.

## Recommended first task for next session

Read this handoff top to bottom, confirm `git log --oneline -3` shows `c0c6d3a` at HEAD with a clean tree, then start step 11 exactly per the "Exact resume point" section above — begin by locating `features/settings/SettingsView.vue` and checking whether `api/modules/preferences.ts` already exists before assuming it needs to be created from scratch.
