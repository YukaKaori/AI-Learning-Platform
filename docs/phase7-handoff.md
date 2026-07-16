# Phase 7 Handoff — Commercial Product Foundation

**Status: Phase 7 is COMPLETE** as of 2026-07-16 (thirteenth/final session —
step 13 + full acceptance pass done). This document is the resume point for
**Phase 8** in a new session. The approved Phase 7 plan with full rationale
lives at `D:\claude-data\plans\refactored-wiggling-floyd.md` (outside the
repo) — it is now historical reference only, not an active resume target.

## What Phase 7 was

The repo was restored to the final Phase 6 commit (`85f7df3`) and Phase 7 was
built from scratch as "Commercial Product Foundation" — a per-user,
database-driven SaaS foundation with a premium 3-mode theme and a
productivity-app Workspace. Full context, design decisions D1–D10, and the
13-step plan are recorded in `docs/phase7-delivery-report.md` (implementation
narrative) and `docs/phase7-final-report.md` (verification/closeout) — **read
those, not this file, for Phase 7 content**. This handoff only exists now to
point at Phase 8's starting state.

Out of scope for Phase 7 (not built — see § Phase 8 candidates below):
registration/email/password-reset, payments, OSS/file upload, external auth,
spaced-repetition engine, WebSockets, Docker/CI.

## Session summary (2026-07-16, step 13 — final session)

Confirmed the repo matched the step-12 handoff exactly (`beaddb0`, clean
tree), then:

1. Wrote all remaining Phase 7 documentation: `docs/architecture.md` § Phase
   7 (error-range table, `OwnershipGuard`, D2 cascade, read-model contracts,
   preferences reconciliation, AI `subjectId` flow, auth extension points,
   roadmap update), `docs/product-domain.md` (updated in place — mock-era
   language replaced throughout), `docs/design-system.md` (new § Phase 7
   additions — dark identity, `StatTile`, view-state pattern, UX unification
   summary), new `docs/mock-migration.md` (per-mock replacement table +
   deferred items), `README.md` (product-surface framing), new
   `docs/phase7-delivery-report.md` (the plan's own Workstream D
   deliverable).
2. Ran the full static verification suite: backend `./mvnw test` (53/53),
   frontend `type-check`/`lint`/`test:unit` (8/8)/`build` — all clean.
3. Ran the plan's final-acceptance checklist live: fresh-state check, full
   E2E entity creation (subject → material → note → deck → task → session →
   AI chat, all linked), Workspace/Analytics/Calendar reflecting real data,
   the D2 delete-cascade exercised end-to-end (materials soft-deleted, notes/
   tasks/conversations nullified with the conversation's `subjectName`
   snapshot correctly retained), a 3-theme × 9-view Playwright sweep driven
   through the real Settings UI (not raw localStorage, which server
   preference reconciliation overwrites), a 2-locale sweep at both 1440px and
   375px, the branded 404 route, and a clean `grep -rn mock src/`.
4. **Found and fixed two verified defects** during that acceptance pass (not
   new feature work — see `docs/phase7-final-report.md` § Executive summary
   for full detail):
   - Backend: `GET` on a route that only supports other HTTP methods
     returned a fake `500` instead of `405` — `GlobalExceptionHandler` had no
     handler for `HttpRequestMethodNotSupportedException`, even though
     `CommonErrorCode.METHOD_NOT_ALLOWED` (40500) already existed, reserved
     but unwired. Fixed with one `@ExceptionHandler` method.
   - Frontend: switching language in Settings without a route navigation left
     the browser tab `<title>` stale in the old language until the next
     navigation — `document.title` was only set in `router.afterEach`, never
     reactively. Fixed with `watch(i18n.global.locale, ...)` in
     `router/index.ts`.
   Both re-verified live after the fix (curl for the first, an isolated
   Playwright repro for the second); the full static suite was re-run and
   stayed green after both fixes.
5. Restored the dev DB to its documented baseline (2 notes, 1 conversation, 0
   subjects, preferences at `system`/`zh-CN`/`60`) via API cleanup — confirmed
   with a final round of checks.
6. Wrote `docs/phase7-final-report.md` (verification results, known
   limitations, production readiness, metrics, Phase 8 candidates) and this
   handoff rewrite.

Commits this session: `f017ca0` (fix, the two acceptance defects), plus the
docs commit containing this file and the rest of the Phase 7 closeout
documentation.

## Repository state

- **Phase**: 7 — **COMPLETE**. All 13 steps done, full acceptance pass green.
- **Branch**: `main`. Working tree: clean after the docs commit. Not pushed
  (push via SSH when asked; port 22 may be blocked → `ssh.github.com:443`).
- **Latest commits**: `f017ca0` (fix: two acceptance defects) → this docs
  commit. Full Phase 7 commit spine (13 feature/step commits, each preceded
  by its own `docs:` handoff commit): `1a20f23` → `d93f07c` → `ccdd41a` →
  `cac1706` → `cdea352` → `c73f203` → `cbbff8b` → `f16a81a` → `3f0cc94` →
  `c0c6d3a` → `921fa57` → `50a80e9` → `f017ca0` → this commit.

## Verification summary

Full detail: `docs/phase7-final-report.md` § Verification results and §
Metrics. Short version: backend 53/53 tests green, frontend 8/8 unit tests
green, type-check/lint/build all clean, full live-API acceptance pass green
(including the D2 cascade and empty-account honest-data checks), full
Playwright browser sweep green (3 themes × 9 views, 2 locales × 2 viewport
widths, zero console/page errors throughout), zero `mock` references
anywhere in `ai-learning-web/src`. Two defects found during acceptance, both
fixed and re-verified (see § Session summary above).

## Remaining known issues

- **Recurring JVM native crash** during backend dev-launch database activity
  (`exit code -1073741819`) — now observed **three times** across three
  sessions (steps 11, 12, this session), always during ordinary
  login/preference DB activity, never during step-specific code, never
  causing data corruption, always resolved by a simple restart. Per the
  step-12 handoff's own stated trigger, a third occurrence means this should
  be actively diagnosed (crash dump) rather than noted-and-skipped again —
  see § Exact starting point for Phase 8. This is a local dev-environment
  characteristic (JDK 22 / Windows / this `spring-boot:run` setup), not a
  product defect — it has never been observed to affect data correctness.
- Everything else in `docs/phase7-final-report.md` § Known limitations is a
  **deliberate deferral**, not an issue: file upload/OSS, spaced-repetition
  engine, server-side AI suggestions, client-timezone streak, the
  `subject_name` snapshot-vs-join question, and the explicitly-out-of-scope
  registration/payments/external-auth/WebSockets/Docker/CI items.
- No TODOs, partial features, or undocumented shortcuts were introduced this
  session or remain outstanding from Phase 7.

## Exact starting point for Phase 8

Phase 8's scope is **not yet decided** — this is deliberately not specified
further than the candidate list below. A dedicated planning session should
pick the actual scope before any implementation starts.

1. Read this handoff, then `docs/phase7-final-report.md` in full (executive
   summary, known limitations, production readiness assessment, recommended
   Phase 8 scope) for the complete picture of where Phase 7 left off.
2. Confirm `git log --oneline -3` shows this session's docs commit at HEAD
   with a clean tree.
3. Candidate Phase 8 areas (not prioritized, not committed — pick with the
   user): (a) diagnose the recurring JVM crash (enable
   `-XX:+CreateMinidumpOnCrash` / explicit `hs_err` output — three
   occurrences now warrant an actual stack trace instead of a bare exit
   code); (b) registration/email verification/password reset (schema and
   `TokenService` seams already reserved since Phase 2); (c) file upload/OSS
   for Materials (`storage_key` reserved since Phase 5); (d) production
   operations — Docker/CI/CD/observability (nothing built yet); (e)
   spaced-repetition review engine for Flashcards (schema reserved since
   Phase 5); (f) smaller items in `docs/mock-migration.md` § Deferred items.
4. Do not assume any of the above is definitely in scope — this is a
   candidate list, not a plan. Phase 8 needs its own design-decisions pass
   (like Phase 7's D1–D10) once its actual scope is chosen.

## Environment reference (unchanged)

- MySQL root pwd `1234`; JDK 22; backend `:8080`, frontend `:5173`; demo
  login `demo`/`Demo123456` (nickname "Demo").
- Port cleanup (`netstat`+`taskkill //PID`), login field `usernameOrEmail`,
  non-ASCII in Git-Bash curl bodies avoided — see `.claude/skills/verify/SKILL.md`.
- `git commit -m` mangling → use `git commit -F <file>` written UTF-8
  **without BOM**. Confirmed still works this session.
- **Dev DB baseline (unchanged, confirmed this session)**: two Phase 6 notes
  (`未命名笔记`, `Verify Note`) and one conversation
  (`Hello, explain recursion`), all unlinked; zero subjects/tasks/sessions;
  demo account preferences at defaults (`system`/`zh-CN`/`60`).
