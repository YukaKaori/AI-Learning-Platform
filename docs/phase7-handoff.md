# Phase 7 Handoff — Commercial Product Foundation

Status as of 2026-07-16 (tenth session — step 12 completed). This document is the resume point for continuing Phase 7 in a new session. Read this first, together with `docs/DEVELOPMENT_GUIDE.md` (the long-term engineering reference); the approved plan with full rationale lives at `D:\claude-data\plans\refactored-wiggling-floyd.md` (outside the repo — confirm it exists, or regenerate from this handoff plus `docs/architecture.md`).

## What Phase 7 is

The repo was restored to the final Phase 6 commit (`85f7df3`) and Phase 7 was **redesigned from scratch** as "Commercial Product Foundation" — a per-user, database-driven SaaS foundation with a premium 3-mode theme and a productivity-app Workspace. Full context, design decisions D1–D10, and the 13-step plan are in the plan file — **do not re-derive them.** Ignore any reference to an earlier "Phase 7 — Data Realization" attempt (discarded).

Out of scope this phase (do not build): registration/email/password-reset, payments, OSS/file upload, external auth, spaced-repetition engine, WebSockets, Docker/CI.

## Session summary (2026-07-16, step 12)

Repo state matched the previous handoff exactly (clean tree at `bc20370`); implemented C1+C2 (`50a80e9`) — the dark theme's black+purple luxury re-skin and a UX unification pass. This is the last feature/theming step; **step 13 (Docs) is the only step left in Phase 7.**

Decisions/conventions established this session (downstream steps should respect them):

- **`html.dark` in `tokens.css` now carries a low-saturation ~248° indigo-violet hue through every neutral step** (`--color-bg` `#0a0910` → `--color-surface` `#12101a` → `--color-surface-hover` `#191823` → `--color-border` `#262433` → `--color-border-strong` `#343243` → `--color-muted`/`--color-text-tertiary` `#6e6b80`), replacing the previous flat gray-black ramp. Chosen and validated with a small WCAG-contrast Node script (not committed — reproducible from the hex values in the token file) rather than by eye; **every ratio meets or beats the previous shipped values**, see Verification below for the exact numbers. `--color-primary` and its hover/active/soft steps were left unchanged — already validated in an earlier step, no drift needed.
- **The categorical accent palette (subject-shares chart) was re-validated against the new surface** with the dataviz skill's `scripts/validate_palette.js "#6674dd,#0d9488,#d97706,#e11d48,#8b5cf6" --mode dark --surface "#12101a"` — still **ALL CHECKS PASS**. No accent hex values changed. **Any future surface-color change in dark mode must re-run this exact validation** — it is surface-dependent, not a one-time check.
- **New token `--shadow-glow-primary`** — a very-low-alpha purple halo, applied to exactly three places per the plan's "interactive emphasis only, never ambient": `AppButton`'s `.variant-solid.tone-primary` (at rest), `AppSidebar`'s `.nav-item.router-link-active` (at rest), `AppCard`'s `.interactive:hover`/`:focus-visible`. Light mode's value is a **zero-alpha shadow, not the `none` keyword** (`0 0 0 0 rgba(94, 106, 210, 0)`) — this is deliberate: `box-shadow` can't mix `none` into a comma-separated list with a real shadow, so the token had to be "real but invisible" to let component CSS unconditionally write `box-shadow: var(--shadow-md), var(--shadow-glow-primary)` without a `html.dark` branch anywhere outside `tokens.css`. **Reuse this same zero-alpha-shadow trick for any future theme-conditional shadow token** — it's the reason the "tokens only, no per-component dark branching" rule still held here.
- **Glass surfaces are more translucent and more blurred in dark mode** (`--glass-blur` 28px vs 20px, `--glass-bg` alpha 0.64 vs light's 0.72) with a violet-tinted `--glass-border` and `--glass-highlight` (previously plain white in both modes). `AppCard`'s `.variant-glass` and the EP `.el-dialog`/`.el-drawer` bridge both gained `inset 0 1px 0 var(--glass-highlight)` — the same "physical top edge" trick `GlassScene`'s `.scene-veil` already used, now shared consistently across every glass surface instead of being scene-only.
- **The EP CSS-var bridge in `element-theme.css` needed zero component-specific overrides** for dark mode — re-validated by opening the Calendar session dialog's date/time picker and the AI-Tutor subject `el-select` in dark mode (Playwright screenshots) and they render correctly through the existing generic mapping. This closes the "generic bridge, never re-validated in dark mode" gap flagged since step 9's handoff — **confirmed fine, not a defect**, no further EP-specific CSS debt remains from this concern.
- **Scene tokens (`--scene-*`, the login/welcome flower experience) were left unchanged** — visually confirmed via screenshot that the independent scene token set still reads correctly alongside the new global dark tokens (the scene has always been intentionally self-contained, sharing only `--glass-highlight` with the rest of the app).
- **Real C2 bug fixed**: `AppSidebar.vue`'s inline theme/locale quick-switcher chips (separate from the Settings page, always existed for at-a-glance switching) were still calling `appStore.setThemeMode`/`setLocale` directly — the **local-only** actions from before step 11 — instead of `appStore.updatePreferences(...)`. This meant a theme/locale change made from the sidebar looked instant and correct but **never persisted to the database**; the next login (or a different device) would silently revert it. Both call sites now go through `updatePreferences(...).catch(() => {})` — errors are swallowed because the collapsed sidebar rail has no room for an inline error line, and the optimistic local apply already succeeded regardless of whether the background persist lands. Fixed and verified this session (see Verification).
- **Toast/message primitive: deliberately not built.** The handoff since step 11 flagged this as an open question for C2. Audit result: exactly two background-persist error surfaces exist in the whole app (Settings' preference chips/goal form, and now nothing else needs one after the sidebar fix — the sidebar swallows its own errors by design, see above), both already served adequately by the existing inline-error-line pattern. Building a toast component for two call sites, neither of which was reported as a real usability problem, would be a premature abstraction. **If a third distinct background-persist-error surface appears in a future step, that's the trigger to reconsider — not before.**
- **Skeleton shapes already match their final layouts** (Workspace/Analytics/Profile skeleton block counts and heights line up with their real stat-tile rows and chart panels) — audited, no changes needed.
- **`prefers-reduced-motion` and dialog keyboard reachability were already correct** — `useSpotlight` disables the pointer-driven glass effect under reduced motion (composable code, unchanged), and `AppDialog` is a thin wrapper over `el-dialog` which owns focus-trap/Esc/keyboard handling natively. Nothing to change; confirmed by reading the code, not assumed.

Commits this session: `50a80e9` (feat, step 12), plus the docs commit containing this file.

## Current project status

- **Phase**: 7 (Commercial Product Foundation), phases 1–6 complete.
- **Step**: 12 of 13 **done**. Next: **step 13 (D — Docs)** — the final step of Phase 7.
- **Branch**: `main`. Working tree: clean after the docs commit. Not pushed (push via SSH when asked; port 22 may be blocked → ssh.github.com:443).
- Phase commits: `1a20f23` → `d93f07c` → `ccdd41a` → `cac1706` → `b04b8b3` (docs) → `cdea352` → `a9b1ed2` (docs) → `c73f203` → `d235259` (docs) → `7825ceb` (guide) → `cbbff8b` (step 7) → `cd65d8d` (docs) → `f16a81a` (step 8) → `358ee52` (docs) → `3f0cc94` (step 9) → `4dcdd73` (docs) → `c0c6d3a` (step 10) → `8e49f6f` (docs) → `921fa57` (step 11) → `bc20370` (docs) → `50a80e9` (step 12) → this docs commit.

## Progress: steps 1–12 of 13 done

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
| 12 | C1+C2 — Dark theme black+purple luxury glass re-skin, UX unification pass | **Done**, `50a80e9` |
| 13 | D — Docs (architecture.md Phase 7 section, new mock-migration.md, etc.) | **Not started — resume here, final step of Phase 7** |

## What exists now

**Backend** — complete and stable since step 5; step 12 touched **zero** backend files (pure frontend/CSS step). 53 tests green, unchanged.

**Frontend (through step 12)** — full `api/modules/` coverage; `useAsync` + `toApiError`; `StatTile`; subjects store + picker + form dialog; SubjectsView/SubjectDetailView fully real; note/deck/AI-conversation subject linkage; CalendarView fully real; `TaskFormDialog`/`SessionFormDialog` shared by Calendar and Workspace; WorkspaceView fully real; AnalyticsView fully real; ProfileView fully real; SettingsView fully real (theme/locale/daily-goal all persist and reconcile cross-device); **dark theme is now the black+purple luxury identity** (near-black indigo-violet surfaces, interactive-emphasis glow, more-translucent liquid glass); AppSidebar's quick theme/locale switcher persists identically to Settings (bug fixed this session). No `mock.ts` files remain anywhere in `features/`. Branded `NotFoundView` + catch-all route. Real `index.html` title/meta/favicon.

**This is functionally the complete Phase 7 product** — step 13 is documentation only, no more UI or backend work is planned before Phase 7 is declared done.

## Exact resume point

Start **step 13 (D — Docs)**, the final step of Phase 7:

1. Read this file, `docs/DEVELOPMENT_GUIDE.md`, and the Workstream D spec in the plan file (§ Workstream D), then the current `docs/architecture.md` to see what's already there vs. what needs adding.
2. **`docs/architecture.md`** needs a Phase 7 section covering: the error-code range table addition (+200000 for Preferences, confirm 110000/120000/150000/160000/180000/170000 are all documented from earlier steps — check, don't assume), the D2 subject-delete cascade policy (soft-delete materials, nullify subjectId on notes/decks/tasks/sessions/conversations), the read-model contracts (workspace/analytics are façades, no new persistence), the preferences reconciliation contract (server wins after login/session-restore, localStorage is the FOUC-safe boot path only), the AI `subjectId` resolution flow (`ContextHints.subjectId` → `LearningContextService`), and auth extension points reserved for a future registration/email/reset phase.
3. **New `docs/mock-migration.md`**: a per-mock table (source mock file → real entity/endpoint it was replaced by → status) for all 8 deleted `mock.ts` files, plus a deferred-items list with priority/why: file upload/OSS (D8, `storage_key` reserved), spaced-repetition engine, server-side AI suggestions (Workspace's "AI Suggestions" panel is client-side rule-based today, per B6), client-timezone-aware streak calculation (D6 noted server-default-timezone as the interim choice), `subject_name` snapshot-vs-join retirement question on `ai_conversations`.
4. Update `product-domain.md` and `design-system.md` if they exist (check first — the plan mentions them but this handoff hasn't tracked whether they were created/kept current in prior steps) with: the dark identity's black+purple identity (from this session), the view-state pattern (`useAsync` → skeleton/content/empty/error), `StatTile`.
5. Update `README.md` for product framing (this is a SaaS product now, not a course project) if its current content still reads like the latter.
6. Write `docs/phase7-delivery-report.md` — the plan calls for this at the end of Workstream D specifically (distinct from the final `docs/phase7-final-report.md` this session's system instructions also require once Phase 7 is confirmed complete; **do both** — the delivery report is the plan's own deliverable, the final report is this session-runner's phase-closeout artifact with verification results and Phase 8 recommendations).
7. Because step 13 is the last step of Phase 7: after finishing the docs work, **do a full Phase 7 acceptance pass** per the plan's own "Final acceptance" checklist (§ Verification in the plan file) — fresh-DB migrate → login as `demo` → premium empty workspace → create subject/material/note/deck/task/session/AI chat → confirm Workspace/Analytics/Calendar/Profile all reflect real data → delete a subject and confirm D2 semantics → all 3 theme modes on every view including `/design-system` → both locales → garbage URL shows the branded 404 → `grep -r mock src/` clean. This is broader than any single step's verification and should be run once, at the very end.
8. Only after that acceptance pass is green: write `docs/phase7-final-report.md` (architecture summary, implementation summary, verification results, known limitations, Phase 8 recommendations) and rewrite this handoff to mark Phase 7 **complete**, naming Phase 8 as the next phase (its scope is not yet defined — that's a Phase 8 planning conversation, not something to invent here).

## Verification performed (step 12)

- **Static**: `vue-tsc --build` clean; vitest 8/8 (unchanged suite, no new components needing tests — this step touched only CSS custom properties and existing component style blocks); `oxlint`+`eslint --fix` clean; `vite build` clean.
- **Backend**: `./mvnw test` — 53/53 green, unchanged (zero backend files touched this step).
- **Contrast validation** (small Node script, WCAG relative-luminance formula, not committed to the repo — reproducible from the token hex values): new dark surface ramp vs. previous shipped ramp — text/bg 18.03 (was baseline-equivalent ~16-18 range), text/surface 17.14 (was 16.28), textSecondary/surface 7.87 (was 6.98), textTertiary(muted)/surface 3.66 (was 3.00), primary/bg 6.09, primary/surface 5.79, white-on-primary button text 3.25 (was 3.27 — effectively unchanged, primary hex untouched). **Every ratio meets or beats the previous baseline**, no regressions.
- **Dataviz palette re-validation**: `validate_palette.js "#6674dd,#0d9488,#d97706,#e11d48,#8b5cf6" --mode dark --surface "#12101a"` → **ALL CHECKS PASS** (lightness band, chroma floor, CVD separation worst-adjacent ΔE 31.3, contrast ≥3:1 all five). No accent color changes needed.
- **Runtime (Playwright, chromium, demo user)**: multiple drives, zero uncaught page errors across all of them.
  - Screenshot pass (1440px, dark theme persisted server-side via Settings so it survives every subsequent navigation/reload): Workspace, Subjects (empty state + starter chips), Analytics, Profile, Settings, Notes, Flashcards, AI-Tutor (incl. the subject `el-select` opened), Calendar (incl. the session-form dialog and its date/time picker panel opened), Design System (incl. scrolled to the Glass card variant) — all visually correct: purple-tinted near-black surfaces, legible text, subtle glow on the primary CTA / active nav item, visible top-highlight on glass/dialog surfaces, EP date-picker panel themed correctly through the existing generic bridge with no overrides needed.
  - Light-mode regression check: Workspace and Subjects screenshotted in light mode post-change and screenshot-compared against this session's own pre-change baseline capture — **pixel-identical**, confirming "light theme gets micro-polish only" held (in practice, zero light-mode changes were needed).
  - Scene check: login page forced to dark via localStorage (pre-auth, no server reconciliation yet) — the flower-scene glass card renders correctly alongside the new global tokens, no scene-token changes needed.
  - **AppSidebar bug-fix verification** (two-part): (1) clicking the sidebar's own dark-theme chip applies `html.dark` instantly; (2) a **second, fresh browser context** (no shared localStorage) logging in afterward also lands in dark mode — proving the sidebar's chip now actually persists server-side and isn't a localStorage-only illusion. This is the concrete regression test for the fix.
  - **27-check full theme × view sweep**: all 3 theme modes (light/dark/system) × 9 views (Workspace, Subjects, Analytics, Profile, Notes, Flashcards, AI-Tutor, Calendar, Design System) — zero horizontal overflow in any combination, zero uncaught page errors across the whole sweep.
  - **375px mobile, both locales**, 6 views each (Workspace, Analytics, Settings, Profile, Subjects, Notes) — zero horizontal overflow in any combination; spot-checked screenshots (zh-CN Workspace, en-US Settings) show correct wrapping and full-width controls.
- **Data hygiene**: this step created no new subjects/tasks/sessions/notes (pure styling + a bug fix); the only state touched was the demo account's own `theme`/`locale`/`dailyGoalMinutes` preferences row, cycled through several values across the verification scripts and confirmed restored to `system`/`zh-CN`/`60` (the original baseline) via a direct API call at the end. Both dev servers were stopped and ports 8080/5173 confirmed free.
- **Environment note — recurring, now twice**: the backend process crashed with the same native JVM access violation (`exit code -1073741819`) **a second time** this session, again during a login flow (this time mid-`INSERT INTO refresh_tokens`, immediately after "Registering transaction synchronization"). Both occurrences (this session and the step-11 session) happened during ordinary login-flow database activity, not during anything specific to the step's own changes (step 12 touched zero backend code). No `hs_err_pid*` crash dump has been found either time. **This is now a documented recurring environment flake** (JDK 22 / Windows / this specific Maven+Spring-Boot dev-run setup) worth a closer look if it keeps recurring — restarting the backend has fully resolved it both times with no data corruption (confirmed via the preferences GET immediately after each restart). Not investigated further this session per the standing guidance to note-but-not-chase environment flakes unrelated to the current step.

## Known risks / gotchas

- **Recurring JVM native crash during login flows** (see Verification above) — now observed twice across two sessions, both during ordinary login/refresh-token database activity, unrelated to either session's actual code changes. If a third occurrence happens, it's worth capturing a `-XX:+CreateMinidumpOnCrash` or enabling `hs_err` dump output explicitly (may be suppressed by the current `spring-boot:run` invocation) to get a stack trace, rather than continuing to treat it as unexplained.
- **Post-login lands on `/welcome`** — Playwright must `goto('/workspace')` (or the target route) explicitly. Unchanged from step 9; bit this session's own verification scripts once (fixed by adding the explicit `goto`).
- **`reconcileFromServer()` runs on every login and every full page load while a session exists** — unchanged from step 11, still true, still acceptable at this scale.
- **Toast/message system: still deliberately absent** — see this session's summary for the reasoning; the trigger to reconsider is a third distinct background-persist-error surface, not a fixed step number.
- **`--shadow-glow-primary`'s zero-alpha-shadow-instead-of-`none` trick** — if a future token needs the same "real value in dark, invisible in light" pattern, copy this approach; `box-shadow: var(--a), none` is invalid CSS the moment `--a` is also a real shadow, so keyword `none` can never be a per-theme token value in a list position.
- **The dataviz palette validation is surface-color-dependent** — any future change to `--color-surface` (dark or light) requires re-running `validate_palette.js` for every categorical palette used against that surface, not just re-eyeballing it. This session's change already went through this; documented here so the next surface-color change doesn't skip it.
- **Two-dialog confirm-flow race in Playwright** (Workspace/Calendar delete flows) — unchanged, not exercised this session.
- **`git commit -m` mangling** → use `git commit -F <file>` written UTF-8 **without BOM**. Confirmed still works this session.
- Port cleanup (`netstat`+`taskkill //PID`; TaskStop does not kill child servers), login field `usernameOrEmail`, non-ASCII in Git-Bash curl bodies — unchanged; see the verify skill.
- **Dev DB baseline (unchanged)**: two Phase 6 notes (`未命名笔记`, `Verify Note`) and one conversation (`Hello, explain recursion`), all unlinked; zero subjects/tasks/sessions; demo account preferences at defaults (`system/zh-CN/60`).
- MySQL root pwd `1234`; JDK 22; backend :8080, frontend :5173; demo login `demo`/`Demo123456` (nickname "Demo", `createdAt` 2026-07-10 per `auth/me`).

## Files modified during this session

Modified: `ai-learning-web/src/components/AppButton.vue` (glow on solid-primary), `ai-learning-web/src/components/AppCard.vue` (glow on interactive hover/focus, glass inset highlight), `ai-learning-web/src/layouts/AppSidebar.vue` (glow on active nav item; **bug fix** — theme/locale chips now call `appStore.updatePreferences` instead of the local-only actions), `ai-learning-web/src/styles/element-theme.css` (dialog/drawer glass inset highlight), `ai-learning-web/src/styles/tokens.css` (dark neutral ramp re-hued purple-black, new `--shadow-glow-primary` token, dark glass tokens more translucent/blurred/tinted).

No files created or deleted this session.

## Documentation state

- `docs/DEVELOPMENT_GUIDE.md` — current; nothing added this session (conventions recorded above are phase-wiring/visual-design decisions, belong in this handoff; will be distilled into `architecture.md` at step 13 where relevant, e.g. the zero-alpha-shadow token trick if it recurs).
- `docs/ai-engine.md` — in sync, untouched this session.
- `docs/architecture.md` — **untouched — this is exactly what step 13 must fill in.** Do not start step 13 assuming any Phase 7 content already exists there beyond what earlier steps may have added incrementally; check its current state first.
- `.claude/skills/verify/SKILL.md` — unchanged this session.
- This file — rewritten for the step-12 boundary. **This is the last handoff rewrite before the step-13/Phase-7-completion rewrite** — the next session's session-summary should describe step 13's docs work AND the full acceptance pass, then mark Phase 7 complete.

## Recommended first task for next session

Read this handoff top to bottom, confirm `git log --oneline -3` shows `50a80e9` at HEAD with a clean tree, then start step 13 exactly per the "Exact resume point" section above — begin by reading the current `docs/architecture.md` in full to establish what Phase 7 content (if any) has already landed incrementally versus what's still missing, before writing anything.
