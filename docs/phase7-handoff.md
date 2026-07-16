# Phase 7 Handoff — Commercial Product Foundation

Status as of 2026-07-15 (sixth session — step 8 completed). This document is the resume point for continuing Phase 7 in a new session. Read this first, together with `docs/DEVELOPMENT_GUIDE.md` (the long-term engineering reference); the approved plan with full rationale lives at `D:\claude-data\plans\refactored-wiggling-floyd.md` (outside the repo — confirm it exists, or regenerate from this handoff plus `docs/architecture.md`).

## What Phase 7 is

The repo was restored to the final Phase 6 commit (`85f7df3`) and Phase 7 was **redesigned from scratch** as "Commercial Product Foundation" — a per-user, database-driven SaaS foundation with a premium 3-mode theme and a productivity-app Workspace. Full context, design decisions D1–D10, and the 13-step plan are in the plan file — **do not re-derive them.** Ignore any reference to an earlier "Phase 7 — Data Realization" attempt (discarded).

Out of scope this phase (do not build): registration/email/password-reset, payments, OSS/file upload, external auth, spaced-repetition engine, WebSockets, Docker/CI.

## Session summary (2026-07-15, step 8)

First committed the step-7 handoff rewrite left uncommitted when the previous session hit its limit (`cd65d8d` — that was the only repo/handoff discrepancy; everything else matched). Then implemented B5 (`f16a81a`). Decisions made this session (downstream steps should respect them):

- **`TaskFormDialog.vue` lives in `features/tasks/components/` and is the one task create/edit surface** — B6's Workspace must reuse it (import + `saved`/`delete` events), not rebuild a quick-add form with divergent semantics. Create mode when `task` is null; edit mode adds the status radio row and a footer-left Delete.
- **Form dialogs emit `delete` as a *request*; the owning view keeps the confirm dialog and the API call.** (TaskFormDialog and SessionFormDialog both follow this; the dialog closes itself after emitting.)
- **Both dialogs are self-contained over the API** (no tasks/sessions store — the data is view-windowed, unlike the cross-view subjects cache) and emit `saved(dto)`; callers reconcile their local working copies. CalendarView drops a saved session that moved outside the visible window.
- **Wire conventions applied**: task edit always sends `dueAt: form.dueAt?.getTime() ?? 0` (0 unschedules) and `subjectId: form.subjectId ?? ''`; session edit sends `title: ''` to clear and re-sends both bounds; create paths use `?? undefined`. Session date+time-range compose into `startsAt`/`endsAt` via wall-clock combination; `endsAt <= startsAt` is caught client-side (`calendar.sessionDialog.invalidRange`) before the server re-validates.
- **Calendar fetch**: one `useAsync` running `Promise.all([listStudySessions(from, to), listTasks()])`; the window is a `computed` from mode+anchor (week = 7 days, month = the full 42-day grid) and a `watch(range, reload)` refetches on any navigation or mode switch. Task status toggle is optimistic with revert-on-failure (`console.error` until C2, per the standing convention).
- EP `el-date-picker`/`el-time-picker` entered the bundle here (auto-registered in `components.d.ts`); they inherit the CSS-var bridge — C1 must re-validate them in dark mode along with selects.

Commits this session: `cd65d8d` (docs, step-7 handoff), `f16a81a` (feat, step 8), plus the docs commit containing this file (also adds Playwright gotchas to `.claude/skills/verify/SKILL.md`).

## Current project status

- **Phase**: 7 (Commercial Product Foundation), phases 1–6 complete.
- **Step**: 8 of 13 **done**. Next: **step 9 (B6)** — Workspace redesign (centerpiece).
- **Branch**: `main`. Working tree: clean after the docs commit. Not pushed (push via SSH when asked; port 22 may be blocked → ssh.github.com:443).
- Phase commits: `1a20f23` → `d93f07c` → `ccdd41a` → `cac1706` → `b04b8b3` (docs) → `cdea352` → `a9b1ed2` (docs) → `c73f203` → `d235259` (docs) → `7825ceb` (guide) → `cbbff8b` (step 7) → `cd65d8d` (docs) → `f16a81a` (step 8) → this docs commit.

## Progress: steps 1–8 of 13 done

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
| 9 | B6 — Workspace redesign (centerpiece) | **Not started — resume here** |
| 10 | B7+B8 — Analytics/Profile frontend, delete all 8 mock.ts, 404 route, index.html title | Not started |
| 11 | B9 — Preferences wiring (Settings, theme/locale reconciliation) | Not started |
| 12 | C1+C2 — Dark theme black+purple luxury glass re-skin, UX unification pass | Not started |
| 13 | D — Docs (architecture.md Phase 7 section, new mock-migration.md, etc.) | Not started |

## What exists now

**Backend** — complete and stable since step 5; steps 9–11 consume it, don't change it (53 tests green). Reminders: `AnalyticsService.streakDays()/activity()` are the only streak/per-day math; `AiConversation` has ALWAYS-update fields (never partial `updateById`); `resolveOwnedSubject` for subject fields; task/calendar wire conventions documented on the DTOs (`dueAt: 0` unschedules, `endsAt > startsAt` validated, calendar list requires `?from=&to=`).

**Frontend (through step 8)** — full `api/modules/` coverage; `useAsync` + `toApiError`; `StatTile`; subjects store + picker + form dialog; SubjectsView/SubjectDetailView fully real; note/deck/AI-conversation subject linkage; CalendarView fully real (windowed week/month fetch, session + task dialogs, status toggle, D3 states, period label, per-day quick-add); `TaskFormDialog` ready for Workspace reuse; NotesView/FlashcardsView/AiTutorView/CalendarView free of mocks.

**Mocks remaining** (deleted in step 10 after rewiring): `subjects/mock.ts` now only imported by WorkspaceView, ProfileView and `analytics|workspace/mock.ts`; `tasks/mock.ts` + `calendar/mock.ts` now only imported by WorkspaceView; `notes|flashcards|analytics|workspace|ai-tutor` mock files still present.

## Exact resume point

Start **step 9 (B6)** — Workspace redesign (the phase centerpiece):

1. Read this file, `docs/DEVELOPMENT_GUIDE.md`, the B6 spec in the plan file (§ Workstream B), then `features/workspace/WorkspaceView.vue` + `workspace/mock.ts` (being replaced) and `api/modules/workspace.ts` (`getWorkspaceSummary` — one aggregate DTO, done in step 6).
2. Per the plan: 12-col productivity layout — header (greeting + **Today's Focus** + quick actions New note / Ask AI / Start session); 4 `StatTile`s (Streak, Study Progress ring vs. real goal, Due Cards, Active Subjects); main col: Continue Learning rail, Recent AI Conversations, Recent Notes, Knowledge Growth mini-chart (**use the dataviz skill**); side col: Upcoming Tasks with inline quick-add, Today's Sessions, AI Suggestions (honest rule-based nudges from real data). Single `useAsync(getWorkspaceSummary)` skeleton; every section needs a designed empty state + CTA.
3. Reuse, don't rebuild: `StatTile` (default slot takes the progress ring), `TaskFormDialog` (task create/edit; quick-add can pre-fill title then `createTask` directly — keep semantics identical), `SessionFormDialog` if "Start session" creates one, subjects store for `continueLearning` accents (`byId` + `subjectAccentOf`).
4. After a task/session mutation on the Workspace, re-pull the summary (`reload()`) rather than hand-patching the aggregate — it's one round trip by design (D7).
5. Verify per `docs/DEVELOPMENT_GUIDE.md` + `.claude/skills/verify/SKILL.md`: static baseline, i18n mirror, then a Playwright drive (empty-account premium empty states; then seed a subject/task/session/note via API and assert every section reflects real data; quick-add task; toggle from Upcoming Tasks; mobile viewport) with full data cleanup.

## Verification performed (step 8)

- **Static**: `vue-tsc` clean; vitest 8/8 (incl. locale mirror with the new `calendar.*`/`tasks.*` keys); `oxlint`+`eslint` clean; `vite build` clean.
- **Runtime (Playwright, chromium, demo user, zh locale)**: 20-step drive — week grid renders → task created via header dialog (title/description/priority high/due today 15:00/subject linked via picker) → chip in today's column → toggle → `done` + `completedAt` stamped (API-asserted) → toggle back → `todo` + `completedAt` cleared → task renamed via dialog → session created via the day-column "+" (09:00–10:15, subject linked) → block shows 09:00 + teal accent (computed `--accent-teal` equality) → `durationMinutes: 75` API-asserted → session renamed → month view shows the dot in today's cell → month navigation refetches the window (request-asserted) and the dot stays month-correct → blocked `GET /v1/study-sessions` shows error+retry → retry recovers → 375px viewport: no horizontal overflow → session and task deleted via dialog delete → confirm flow → today column back to the empty state → API: zero tasks/sessions remain.
- **API smoke**: window param mandatory (`from` missing → 40001, clean envelope); baseline empty lists.
- **Data hygiene**: verification subject deleted via API (D2 cascade); final baseline re-verified (0 subjects, 0 decks, 0 tasks, 0 sessions, 2 Phase 6 notes, 1 Phase 6 conversation). Servers stopped, ports freed.
- **Intentionally not verified**: live DeepSeek streaming (no API key — standing gap); week-view drag/resize interactions don't exist by design (dialog-only editing this phase).

## Known risks / gotchas

- **Playwright gotchas now recorded in `.claude/skills/verify/SKILL.md`** (updated this session): Vite cold-start warmup incl. opening dialogs that mount new EP components; Esc inside an AppDialog closes the dialog, not the picker panel (click `.el-dialog__header` to dismiss panels); `unroute` needs the same matcher/handler references; optimistic UI mutations require API polling, not immediate asserts.
- `el-date-picker`/`el-time-picker` are new EP surface — **C1 must re-validate them in dark mode** (they ride the generic CSS-var bridge; no picker-specific rules exist yet).
- Dialog-title flip during close animation (title derives from mode) — standing cosmetic nit, deliberately left for C2; the new dialogs use a `task`/`session` prop so they show the edit title correctly, but the flip pattern still exists in FlashcardsView/NotesView.
- `git commit -m` mangling → use `git commit -F <file>` written UTF-8 **without BOM** (`[System.IO.File]::WriteAllText` with `UTF8Encoding($false)`; PowerShell 5.1 `-Encoding utf8` adds a BOM that lands in the commit subject).
- Port cleanup (`netstat`+`taskkill //PID`; TaskStop does not kill child servers), login field `usernameOrEmail`, non-ASCII in Git-Bash curl bodies — unchanged; see the verify skill.
- **Dev DB baseline**: two Phase 6 notes (`未命名笔记`, `Verify Note`) and one conversation (`Hello, explain recursion`), all unlinked; everything step-8 verification created was removed.
- MySQL root pwd `1234`; JDK 22; backend :8080, frontend :5173; demo login `demo`/`Demo123456`.

## Documentation state

- `docs/DEVELOPMENT_GUIDE.md` — current; nothing added this session (the new dialog conventions are phase-wiring, recorded above).
- `docs/ai-engine.md` — in sync (subject-resolution contract).
- `docs/architecture.md` — untouched; Phase 7 section + `docs/mock-migration.md` land in step 13.
- `.claude/skills/verify/SKILL.md` — **updated this session** with the browser-surface gotchas listed above.
- This file — rewritten for the step-8 boundary.
