# Phase 7 Handoff — Commercial Product Foundation

Status as of 2026-07-16 (seventh session — step 9 completed). This document is the resume point for continuing Phase 7 in a new session. Read this first, together with `docs/DEVELOPMENT_GUIDE.md` (the long-term engineering reference); the approved plan with full rationale lives at `D:\claude-data\plans\refactored-wiggling-floyd.md` (outside the repo — confirm it exists, or regenerate from this handoff plus `docs/architecture.md`).

## What Phase 7 is

The repo was restored to the final Phase 6 commit (`85f7df3`) and Phase 7 was **redesigned from scratch** as "Commercial Product Foundation" — a per-user, database-driven SaaS foundation with a premium 3-mode theme and a productivity-app Workspace. Full context, design decisions D1–D10, and the 13-step plan are in the plan file — **do not re-derive them.** Ignore any reference to an earlier "Phase 7 — Data Realization" attempt (discarded).

Out of scope this phase (do not build): registration/email/password-reset, payments, OSS/file upload, external auth, spaced-repetition engine, WebSockets, Docker/CI.

## Session summary (2026-07-16, step 9)

Repo state matched the previous handoff exactly (clean tree at `358ee52`); implemented B6 (`3f0cc94`) — the Workspace redesign. Decisions made this session (downstream steps should respect them):

- **WorkspaceView renders entirely off `getWorkspaceSummary()`** (one `useAsync`, per D7); after any task/session mutation the view calls `reload()` to re-pull the aggregate instead of hand-patching it. The only local working copy is the `tasks` ref (optimistic status toggle, CalendarView pattern).
- **Analytics/workspace study math counts only *ended* sessions** (`endsAt <= now` — see `AnalyticsService.endedSessions`). A scheduled future session appears in `todaySessions` but adds nothing to `studiedTodayMinutes`/streak/weekActivity. This is honest-by-design; it bit verification once (seeding a future session and expecting minutes).
- **Today's Focus and AI Suggestions are client-computed, honest rules** over the aggregate: focus = today's earliest-due open task → due cards → plan-your-day; nudges = streak-at-risk, due cards, overdue (among the 5 upcoming), no subjects, capped at 3, with an all-clear fallback. A server-side AI version is a documented future extension (step 13 docs note it), not built.
- **The Knowledge Growth chart is hand-rolled token-only SVG-free DOM columns** (no chart lib, per no-new-dependencies): single series in `--color-primary` (validated with the dataviz palette validator against both light `#ffffff`* and dark `#17171a` surfaces — both pass; *validator default light surface), ≤24px columns with 4px rounded data-ends, selective direct label on the most recent max day only, per-mark `AppTooltip` + `tabindex` focus, 3px `--color-muted-soft` stubs for honest zero days. `weekActivity[6]` is always today. ISO `yyyy-MM-dd` bucket dates are parsed component-wise (`chartDate`) — never `new Date(iso)` (UTC shift).
- **Quick actions**: "New note" mirrors NotesView semantics exactly (create untitled → navigate `notes?note=id`); "Start session" opens the shared `SessionFormDialog`; hero renders before data (greeting needs none).
- **Goal ring**: SVG circle with `pathLength="100"`, fill circle is `v-if`-hidden at 0% (a round linecap paints a dot even at `0 100` — a fake mark on an honest zero), flips to `--color-success` at 100%.
- Post-login lands on `/welcome`, not the workspace — Playwright drives must `goto('/workspace')` explicitly after login.

Commits this session: `3f0cc94` (feat, step 9), plus the docs commit containing this file.

## Current project status

- **Phase**: 7 (Commercial Product Foundation), phases 1–6 complete.
- **Step**: 9 of 13 **done**. Next: **step 10 (B7+B8)** — Analytics/Profile frontend + de-demo shell.
- **Branch**: `main`. Working tree: clean after the docs commit. Not pushed (push via SSH when asked; port 22 may be blocked → ssh.github.com:443).
- Phase commits: `1a20f23` → `d93f07c` → `ccdd41a` → `cac1706` → `b04b8b3` (docs) → `cdea352` → `a9b1ed2` (docs) → `c73f203` → `d235259` (docs) → `7825ceb` (guide) → `cbbff8b` (step 7) → `cd65d8d` (docs) → `f16a81a` (step 8) → `358ee52` (docs) → `3f0cc94` (step 9) → this docs commit.

## Progress: steps 1–9 of 13 done

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
| 10 | B7+B8 — Analytics/Profile frontend, delete all 8 mock.ts, 404 route, index.html title | **Not started — resume here** |
| 11 | B9 — Preferences wiring (Settings, theme/locale reconciliation) | Not started |
| 12 | C1+C2 — Dark theme black+purple luxury glass re-skin, UX unification pass | Not started |
| 13 | D — Docs (architecture.md Phase 7 section, new mock-migration.md, etc.) | Not started |

## What exists now

**Backend** — complete and stable since step 5; steps 10–11 consume it, don't change it (53 tests green). Reminders: `AnalyticsService.streakDays()/activity()` are the only streak/per-day math **and count only ended sessions**; `AiConversation` has ALWAYS-update fields (never partial `updateById`); `resolveOwnedSubject` for subject fields; task/calendar wire conventions documented on the DTOs (`dueAt: 0` unschedules, `endsAt > startsAt` validated, calendar list requires `?from=&to=`).

**Frontend (through step 9)** — full `api/modules/` coverage; `useAsync` + `toApiError`; `StatTile`; subjects store + picker + form dialog; SubjectsView/SubjectDetailView fully real; note/deck/AI-conversation subject linkage; CalendarView fully real; `TaskFormDialog`/`SessionFormDialog` shared by Calendar **and Workspace**; WorkspaceView fully real (12-col productivity layout: hero + focus + quick actions, 4 StatTiles incl. goal ring, Continue Learning, Recent Conversations/Notes, Knowledge Growth chart, Upcoming Tasks with quick-add, Today's Sessions, rule-based AI Suggestions; designed empty state + CTA in every section). NotesView/FlashcardsView/AiTutorView/CalendarView/WorkspaceView free of mocks.

**Mocks remaining** (all deleted in step 10 after rewiring): `analytics/mock.ts` (used by AnalyticsView), `subjects|notes|workspace/mock.ts` (used by ProfileView and by other mocks), `tasks/mock.ts`, `calendar/mock.ts`, `ai-tutor/mock.ts`, `flashcards/mock.ts` (no view imports left — only mock-to-mock). Grep truth: `subjects/mock` ← ProfileView, `analytics/mock.ts`, `workspace/mock.ts`; `notes/mock` ← ProfileView; `workspace/mock` ← ProfileView.

## Exact resume point

Start **step 10 (B7+B8)** — Analytics + Profile frontend, then de-demo the shell:

1. Read this file, `docs/DEVELOPMENT_GUIDE.md`, the B7/B8 spec in the plan file (§ Workstream B), then `features/analytics/AnalyticsView.vue` + its `mock.ts`, `features/profile/ProfileView.vue`, and `api/modules/analytics.ts` / `auth.ts` (`createdAt`, `updateProfile` already exist from step 6).
2. **B7 Analytics**: wire the 3 real endpoints — `getAnalyticsSummary()` (nullable `weekDeltaPercent`/`taskCompletionPercent` render "—", never 0), `getActivity(days)` (bar chart; heatmap reshapes `days=84`), `getSubjectShares(30)` (a null-id row = unlinked time; percentages client-side). Delete the "Demo data" locale strings (`en-US.ts` § analytics + zh mirror). Reuse `useAsync`, `StatTile` where tiles exist, and follow the growth-chart conventions from B6 (token hues, selective labels, tooltips, honest zeros/empties). **Analytics counts only ended sessions** — don't fight it in verification.
3. **B7 Profile**: real `memberSince` from `authStore.user.createdAt`, real stats (subjects/notes via API — check what the view actually shows before choosing endpoints), enabled nickname/avatar edit via `updateProfile` (auth store must refresh its user).
4. **B8**: delete all 8 `features/*/mock.ts` (grep-verify no imports remain), real `index.html` title/meta/favicon, `NotFoundView.vue` + catch-all route (branded 404), verify routes stay lazy-loaded.
5. Verify per `docs/DEVELOPMENT_GUIDE.md` + `.claude/skills/verify/SKILL.md`: static baseline, i18n mirror, then a Playwright drive (analytics empty account → seed sessions/tasks across several days via API with **past** end times → summary/chart/heatmap/shares all real; profile edit round-trip; garbage URL → 404; mobile) with full data cleanup. Remember: login lands on `/welcome`; navigate explicitly.

## Verification performed (step 9)

- **Static**: `vue-tsc` clean; vitest 8/8 (incl. locale mirror over the rebuilt `workspace.*` keys); `oxlint`+`eslint` clean; `vite build` clean.
- **Palette (dataviz skill)**: `--color-primary` light `#5e6ad2` and dark `#7b86e8` both pass all validator checks (lightness band, chroma floor, contrast ≥3:1) against their surfaces.
- **Runtime (Playwright, chromium, demo user, zh locale)**: 54-check drive, all green — empty account: 4 honest-zero tiles, empty goal ring (no dot), fresh focus line, designed empty+CTA in Continue Learning/growth/tasks/sessions, noSubjects nudge, baseline chats/notes render; seeded (subject teal + task due today + overdue task + **ended** 75-min session + linked note): streak 1, ring reached at 75/60, focus names the task, continue card with computed `--accent-teal` equality, overdue due-date styled, session row with real times, chart bars + "1 小时 15 分" max label + week total + hover tooltip, overdue nudge replaces noSubjects; interactions: quick-add persists (API-asserted) + input clears, toggle → `done`+`completedAt` (API) → row leaves Upcoming on reload, dialog rename updates focus line, task delete via form-dialog→confirm flow clears the nudge, session rename + delete via confirm flow returns empty state and stats reconcile to 0/60, Ask AI + New note navigate (note created+opened), blocked summary → error+retry recovers, 375px zero horizontal overflow with side column stacked.
- **Visual pass**: light/dark/mobile screenshots eyeballed — dark-mode chart/ring/glass card legible on existing dark tokens (C1 re-skin still pending).
- **Data hygiene**: everything created was deleted via API; final baseline re-verified (0 tasks, 0 subjects, 0 sessions, 2 Phase 6 notes, 1 Phase 6 conversation). Servers stopped, ports freed.
- **Intentionally not verified**: live DeepSeek streaming (no API key — standing gap).

## Known risks / gotchas

- **Post-login lands on `/welcome`** — Playwright must `goto('/workspace')` (or the target route) explicitly.
- **Analytics counts only ended sessions** — verification seeding must use past `endsAt` or minutes/streak stay 0.
- **Two-dialog confirm flows race in Playwright**: after the form dialog's Delete, the closing form dialog and the opening confirm dialog are both `:visible` mid-animation and both have a 删除 button — scope to the confirm dialog by its title text and wait out the animation (~500ms) before clicking.
- Delete-confirm dialog title flip during close animation — standing cosmetic nit for C2 (FlashcardsView/NotesView pattern; the shared form dialogs are unaffected).
- `el-date-picker`/`el-time-picker`/`el-tooltip` — **C1 must re-validate all EP surfaces in dark mode** (generic CSS-var bridge only).
- Unused-mock window: `tasks|calendar|flashcards|ai-tutor/mock.ts` now have no view importers; they are deliberately left for B8's single grep-verified deletion — don't "clean them up" in step 10's B7 half and break the plan's commit shape.
- `git commit -m` mangling → use `git commit -F <file>` written UTF-8 **without BOM** (`[System.IO.File]::WriteAllText` with `UTF8Encoding($false)`).
- Port cleanup (`netstat`+`taskkill //PID`; TaskStop does not kill child servers), login field `usernameOrEmail`, non-ASCII in Git-Bash curl bodies — unchanged; see the verify skill.
- **Dev DB baseline**: two Phase 6 notes (`未命名笔记`, `Verify Note`) and one conversation (`Hello, explain recursion`), all unlinked; everything step-9 verification created was removed.
- MySQL root pwd `1234`; JDK 22; backend :8080, frontend :5173; demo login `demo`/`Demo123456`.

## Documentation state

- `docs/DEVELOPMENT_GUIDE.md` — current; nothing added this session (the new conventions are phase-wiring, recorded above).
- `docs/ai-engine.md` — in sync (subject-resolution contract).
- `docs/architecture.md` — untouched; Phase 7 section + `docs/mock-migration.md` land in step 13 (must document: ended-session analytics semantics, client-side suggestion rules + server-AI extension point).
- `.claude/skills/verify/SKILL.md` — unchanged this session; the `/welcome` landing and two-dialog race gotchas above are candidates to fold in during a later docs pass.
- This file — rewritten for the step-9 boundary.
