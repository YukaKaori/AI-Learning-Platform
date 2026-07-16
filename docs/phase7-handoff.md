# Phase 7 Handoff — Commercial Product Foundation

Status as of 2026-07-14 (fifth session — step 7 completed). This document is the resume point for continuing Phase 7 in a new session. Read this first, together with `docs/DEVELOPMENT_GUIDE.md` (the long-term engineering reference, added this session); the approved plan with full rationale lives at `D:\claude-data\plans\refactored-wiggling-floyd.md` (outside the repo — confirm it exists, or regenerate from this handoff plus `docs/architecture.md`).

## What Phase 7 is

The repo was restored to the final Phase 6 commit (`85f7df3`) and Phase 7 was **redesigned from scratch** as "Commercial Product Foundation" — a per-user, database-driven SaaS foundation with a premium 3-mode theme and a productivity-app Workspace. Full context, design decisions D1–D10, and the 13-step plan are in the plan file — **do not re-derive them.** Ignore any reference to an earlier "Phase 7 — Data Realization" attempt (discarded).

Out of scope this phase (do not build): registration/email/password-reset, payments, OSS/file upload, external auth, spaced-repetition engine, WebSockets, Docker/CI.

## Session summary (2026-07-14, step 7)

Created `docs/DEVELOPMENT_GUIDE.md` (`7825ceb`), then implemented B3+B4 as one step (`cbbff8b`). Decisions made this session (downstream steps should respect them):

- **`stores/subjects.ts` is the single client-side subject source.** Options-API Pinia store: `load()` (once; `force` re-fetches, used by error retry), `byId(id)` for enrichment, write-through `create/update/remove`, and `refresh(id)` to re-pull derived counts after linked-content changes. Every view that shows subject names/accents goes through it — **steps 8–10 must use it too** (Workspace `continueLearning`, Calendar session accents, Profile counts where applicable) instead of re-fetching or reading mocks.
- **Backend `color`/`icon` are opaque strings; the frontend narrows them** via `subjectAccentOf()` / `subjectIconOf()` in `features/subjects/types.ts` (fallbacks `indigo` / `book-open`), with `SUBJECT_ACCENTS` / `SUBJECT_ICONS` as the dialog choice sets. Never render `subject.color` directly.
- **Shared subject UI lives in `features/subjects/components/`**: `SubjectPicker.vue` (themed `el-select`, `null` = unlinked; callers map `null` → `''` wire sentinel on update paths) and `SubjectFormDialog.vue` (create/edit; status+progress only in edit mode; inline `t(errorKey)` failure line).
- **Linkage conventions**: note picker persists immediately on change (`updateNote(id, { subjectId: v ?? '' })`); deck dialog sends `subjectId: form.subjectId ?? ''` on edit (always-send = clear-on-empty) and `?? undefined` on create; AI Tutor keeps a `selectedSubjectId` synced per active conversation and sends it on create (`subjectId ?? undefined`) and on every SSE send (`subjectId ?? ''` — re-sending the same id is an idempotent keep, cleared picker unlinks on next message). Subject-detail "Ask AI" now links via `subjectId` (server-side context resolution).
- **`toApiError()` extracted to `api/types.ts`** — the one error normalizer, used by `useAsync` and the store. Use it for any new load-state code.
- Mutation-dialog failures show an **inline `t(errorKey)` line inside the dialog** (form/delete dialogs); list-row mutation failures still `console.error` until C2's feedback pass.
- `excerptOf`/`outlineOf` now accept `{ content: string }` so `NoteDto` works without adapters.

Known cosmetic nit (deliberately left for C2): dialogs whose title derives from `form?.mode` flip to the edit title during the close animation (pattern inherited from FlashcardsView) — visible only mid-fade.

Commits this session: `7825ceb` (DEVELOPMENT_GUIDE), `cbbff8b` (feat, step 7), plus the docs commit containing this file.

## Current project status

- **Phase**: 7 (Commercial Product Foundation), phases 1–6 complete.
- **Step**: 7 of 13 **done**. Next: **step 8 (B5)** — Calendar + Tasks frontend.
- **Branch**: `main`. Working tree: clean after the docs commit. Not pushed (push via SSH when asked; port 22 may be blocked → ssh.github.com:443).
- Phase commits: `1a20f23` → `d93f07c` → `ccdd41a` → `cac1706` → `b04b8b3` (docs) → `cdea352` → `a9b1ed2` (docs) → `c73f203` → `d235259` (docs) → `7825ceb` (guide) → `cbbff8b` (step 7) → this docs commit.

## Progress: steps 1–7 of 13 done

| # | Step | Status |
|---|------|--------|
| 1 | A1+A2 — Migrations V4/V5, pom fix, `common/OwnershipGuard` | **Done**, `1a20f23` |
| 2 | A3+A4 — Subject (110000s) + Material (120000s) modules | **Done**, `d93f07c` |
| 3 | A5+A6+A7 — Task (150000s), Calendar (160000s), Note/Deck subjectId | **Done**, `ccdd41a` |
| 4 | A8 — Preferences (200000s) + auth prep | **Done**, `cac1706` |
| 5 | A9+A10 — Analytics (180000s), Workspace (170000s), AI subjectId context | **Done**, `cdea352` |
| 6 | B1+B2 — Frontend api modules, `useAsync`, `StatTile` | **Done**, `c73f203` |
| 7 | B3+B4 — Subjects frontend + cross-feature subject linkage | **Done**, `cbbff8b` |
| 8 | B5 — Calendar + Tasks frontend | **Not started — resume here** |
| 9 | B6 — Workspace redesign (centerpiece) | Not started |
| 10 | B7+B8 — Analytics/Profile frontend, delete all 8 mock.ts, 404 route, index.html title | Not started |
| 11 | B9 — Preferences wiring (Settings, theme/locale reconciliation) | Not started |
| 12 | C1+C2 — Dark theme black+purple luxury glass re-skin, UX unification pass | Not started |
| 13 | D — Docs (architecture.md Phase 7 section, new mock-migration.md, etc.) | Not started |

## What exists now

**Backend** — complete and stable since step 5; steps 8–11 consume it, don't change it (53 tests green). Reminders: `AnalyticsService.streakDays()/activity()` are the only streak/per-day math; `AiConversation` has ALWAYS-update fields (never partial `updateById`); `resolveOwnedSubject` for subject fields; task/calendar wire conventions documented on the DTOs (`dueAt: 0` unschedules, `endsAt > startsAt` validated, calendar list requires `?from=&to=`).

**Frontend (through step 7)** — full `api/modules/` coverage; `useAsync` + `toApiError`; `StatTile`; subjects store + picker + form dialog; SubjectsView/SubjectDetailView fully real (CRUD, materials tab with external links, D2 delete dialog, empty state with working starter chips); note/deck/AI-conversation subject linkage live end-to-end; NotesView/FlashcardsView/AiTutorView free of `subjects/mock`.

**Mocks remaining** (deleted in step 10 after rewiring): `subjects/mock.ts` still exists but is now only imported by WorkspaceView, CalendarView, ProfileView and `analytics|workspace/mock.ts`; `tasks|calendar|notes|flashcards|analytics|workspace|ai-tutor` mock files still present.

## Exact resume point

Start **step 8 (B5)** — Calendar + Tasks frontend:

1. Read this file, `docs/DEVELOPMENT_GUIDE.md`, then `features/calendar/CalendarView.vue` + `features/calendar/mock.ts` + `features/tasks/mock.ts` + `features/tasks/types.ts` (shapes being replaced) and `api/modules/calendar.ts`/`task.ts` (done in step 6).
2. Per the plan: **windowed session fetch per visible month** (`listStudySessions(from, to)` — the window is mandatory); tasks render on their due dates; task status toggleable from the calendar; a create/edit task dialog **shared with Workspace** (B6 reuses it — put it under `features/tasks/components/`).
3. Sessions/tasks link to subjects — use `useSubjectsStore().byId` + `subjectAccentOf` for accents and `SubjectPicker` in dialogs (see the deck-dialog pattern for the `'' `-unlink convention).
4. Use `useAsync` (or the store pattern) for loads with the standard D3 states; month navigation refetches the window.
5. Verify per `docs/DEVELOPMENT_GUIDE.md` + `.claude/skills/verify/SKILL.md`: static baseline (type-check, test, lint, build), i18n mirror (B5 adds locale strings), then a Playwright drive (create/edit/toggle/delete a task, create a session in the visible month, month navigation refetch, error+retry, mobile viewport) with API-level assertions and full data cleanup.

## Verification performed (step 7)

- **Static**: `vue-tsc` clean; vitest 8/8 (incl. locale mirror with the new keys); `oxlint`+`eslint` clean; `vite build` clean.
- **Runtime (Playwright, chromium, demo user, zh locale)**: 14-step drive — subjects empty state (4 starter chips) → blocked `GET /v1/subjects` shows error+retry → retry recovers → starter chip creates a real subject and opens its detail → material created with external link (href verified) → subject edited via dialog (name + accent swatch, visible in grid) → second subject via New-subject dialog (icon picker) → grid shows both → note linked via editor picker (accent label appears; note then visible in subject's Related notes) → deck created linked (accent bar colored) and unlinked via cleared picker (bar back to muted) → AI chat created with subject context (stream finished on the expected no-key error reply) → 375px viewport: no horizontal overflow.
- **API-level assertions**: conversation carried `subjectId` + `subjectName` snapshot; note carried `subjectId`; subject derived counts (`materialCount`/`noteCount`) correct; deck `subjectId` null after unlink.
- **D2 exercised live**: deleting the subject with a material + linked note deleted the material and kept the note (`subjectId` → null); deleting the subject with a linked conversation kept the conversation (`subjectId` → null, name snapshot retained by design).
- **Data hygiene**: both subjects, the deck and the drive conversation deleted; final baseline re-verified (0 subjects, 0 decks, 2 Phase 6 notes, 1 Phase 6 conversation). Servers stopped, ports freed.
- **Intentionally not verified**: live DeepSeek streaming with real subject context (no API key — standing gap); material *edit* dialog round-trip was implemented but only create/delete were driven.

## Known risks / gotchas

- **Vite dev-server cold start breaks Playwright navigation**: the first render of a new Element Plus component (e.g. `el-select`) triggers "optimized dependencies changed" full-page reloads that abort SPA navigations mid-flight. **Warm up by visiting every touched route once before asserting** (see the warmup block in this session's drive; consider `optimizeDeps.include` if it keeps biting).
- `git commit -m` mangling, port cleanup (`netstat`+`taskkill //PID`; TaskStop does not kill child servers), login field `usernameOrEmail`, non-ASCII in Git-Bash curl bodies — all unchanged; see `.claude/skills/verify/SKILL.md`.
- **Dev DB baseline**: two Phase 6 notes (`未命名笔记`, `Verify Note`) and one conversation (`Hello, explain recursion`), all unlinked; everything step-7 verification created was removed.
- EP select theming rides on the existing `element-theme.css` CSS-var bridge; C1 explicitly re-validates selects/dropdowns in dark mode.
- MySQL root pwd `1234`; JDK 22; backend :8080, frontend :5173; demo login `demo`/`Demo123456`.

## Documentation state

- `docs/DEVELOPMENT_GUIDE.md` — **new this session**: long-term engineering reference (philosophy, conventions, verification workflow, commit/handoff strategy, review checklist). Constitution stays `docs/architecture.md`, which wins on conflict.
- `docs/ai-engine.md` — in sync (subject-resolution contract).
- `docs/architecture.md` — untouched; Phase 7 section + `docs/mock-migration.md` land in step 13.
- `.claude/skills/verify/SKILL.md` — current; add the Vite warmup note when next touched.
- This file — rewritten for the step-7 boundary.
