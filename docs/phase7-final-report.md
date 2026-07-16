# Phase 7 Final Report — Commercial Product Foundation

Session-closeout artifact for Phase 7 (step 13/13, final step). Distinct from
`docs/phase7-delivery-report.md` (the plan's own Workstream D deliverable,
narrating what was built and why) — this document is the acceptance/closeout
record: verification results, defects found and fixed during this session's
acceptance pass, known limitations, production readiness, metrics, and the
Phase 8 recommendation.

## Executive summary

Phase 7 is **complete**. All 13 planned steps shipped across 12 prior
sessions; this session completed step 13 (documentation) and ran a full
project acceptance pass across the integrated product — not just the pieces
built step-by-step. Two verified defects were found during acceptance, both
fixed, both re-verified, neither requiring any architectural change or scope
expansion:

1. **Backend**: requests hitting a real route with the wrong HTTP method
   (e.g. `GET` on a `PUT`/`DELETE`-only path) returned a fake `500 Internal
   Server Error` instead of `405 Method Not Allowed`, because
   `GlobalExceptionHandler` had no handler for
   `HttpRequestMethodNotSupportedException` — even though the correct error
   code (`CommonErrorCode.METHOD_NOT_ALLOWED`, 40500) already existed,
   reserved but unwired. Fixed with one `@ExceptionHandler` method.
2. **Frontend**: switching language from the Settings page (without a route
   navigation) left the browser tab `<title>` in the previous language until
   the next navigation, because `document.title` was only set from
   `router.afterEach`, never reactively on a locale change. Fixed with a
   `watch(i18n.global.locale, ...)` in `router/index.ts`.

Both fixes are minimal, behavior-only, and covered by re-run verification
(backend: 53/53 tests still green + a live curl reproduction; frontend:
type-check/lint/unit/build green + a live Playwright reproduction). No other
defects were found. The product is functionally complete, has zero remaining
mock data, and every module the user prompt asked to be reviewed was actually
exercised this session (see § Verification results).

## Architecture summary

Modular monolith, package-by-feature backend (Spring Boot 4 / MyBatis-Plus /
MySQL / Flyway); feature-first Vue 3 + TS + Vite + Pinia + Element Plus
frontend. Phase 7's structural additions: seven backend packages
(`subject`, `material`, `task`, `calendar`, `workspace`, `analytics`,
`preference`) went from schema-only to fully real; `V4`/`V5` migrations; the
D1 `OwnershipGuard` shared-ownership-check pattern; D2's soft-delete +
nullify subject-delete cascade; server-authoritative preferences with a
FOUC-safe localStorage boot path; a real Workspace/Analytics read-model
façade layer with zero owned tables. Full detail:
`docs/architecture.md` § Phase 7, `docs/product-domain.md`,
`docs/phase7-delivery-report.md`.

## Features delivered

Every module in the user prompt's review list is real, per-user-isolated,
and backed by a real endpoint — see `docs/mock-migration.md` for the
per-module mock-to-real mapping:

Authentication (unchanged, Phase 2, +`PUT /auth/profile` this phase) ·
Subjects (new) · Materials (new, links only — upload deferred) · Notes
(real since Phase 6, +subject linkage) · Flashcards (real since Phase 6,
+subject linkage) · Tasks (new) · Calendar (new) · Workspace (real read
model replacing the mock KPI dashboard) · Analytics (real read model
replacing seeded-random fake charts) · AI Tutor (real since Phase 6,
+server-resolved subject context) · Preferences (new, server-authoritative)
· Profile (real `memberSince`, editable identity) · Settings (persists to
`preference` module) · Theme system (dark re-skinned to a black+purple
luxury identity) · Responsive behavior (confirmed at 375px/1440px) · Error
handling (`useAsync` error-with-retry everywhere, plus this session's
405 fix) · Empty states (every section has a designed empty state, honest
`—` for metrics with no baseline) · Loading states (skeletons matching final
layouts) · Navigation (branded 404 + catch-all route) · User isolation
(`OwnershipGuard` on every user-scoped query) · Design consistency (tokens
-only styling, UX unification pass in step 12).

## Technical decisions

D1–D10 plus the AI `subjectId` closure — see `docs/phase7-delivery-report.md`
§ Design decisions for the full table and outcomes. No decisions were
revisited or reversed this session; the two fixes above are bug fixes, not
design changes.

## Verification results

### Static

| Check | Result |
| --- | --- |
| Backend `./mvnw test` | **53/53 green** (both before and after the `GlobalExceptionHandler` fix) |
| Frontend `npm run type-check` | Clean |
| Frontend `npm run lint` (oxlint + eslint) | Clean |
| Frontend `npm run test:unit` | **8/8 green** |
| Frontend `npm run build` | Clean |

### API (live curl, `demo` account, both before and after the two backend/DB restarts this session)

- Fresh-empty-account check: `GET /workspace/summary`, `/analytics/summary`,
  `/subjects`, `/preferences` all return zero-filled, non-`500` responses
  with `weekDeltaPercent`/`taskCompletionPercent` correctly `null` (not a
  fabricated `0`) when there's no baseline — confirms D5's honest-data
  contract.
- Full E2E creation: subject → material → note (linked) → flashcard deck
  (linked) → task (linked) → study session (linked) → AI conversation
  (linked, `subjectId` persisted + `subjectName` snapshot correct).
- Confirmed `Subject.materialCount/noteCount/deckCount/studyMinutes` derive
  correctly from the linked entities.
- Confirmed `Workspace.summary` and `Analytics.summary` reflect the new data:
  `activeSubjects`, `continueLearning`, `upcomingTasks`,
  `recentConversations`, `recentNotes`, `todaySessions` all populated
  correctly from real writes.
- Confirmed the analytics "only sessions that have already ended count"
  attribution rule (documented in `AnalyticsService`'s Javadoc): a
  future-dated session was correctly excluded from `weekActivity`/streak
  until a past-dated session was also created, which was correctly included
  (`streakDays: 1`, `studiedTodayMinutes: 30`, `weekMinutes: 30`) — this is
  intentional behavior, not a defect, verified by testing both the exclusion
  and inclusion path.
- **D2 delete-cascade, exercised end-to-end**: deleting the test subject
  produced `404` on the subject and its materials (soft-deleted, confirmed
  by the material-list endpoint now `404`ing through the subject's now-gone
  parent route) while the note, task, and AI conversation all had
  `subjectId` nullified but remained otherwise intact — the conversation
  additionally retained its `subjectName` display snapshot exactly as
  documented in D2/the AI subject-resolution flow.
- Cross-user 403 enforcement: verified via the existing automated backend
  test suite (`OwnershipGuard`-covered service tests, part of the 53 green
  tests) rather than a second live account, since registration is
  out-of-scope this phase and no second seeded account exists.
- **Defect found and fixed**: `GET` on a `PUT`/`DELETE`-only route
  (`/flashcards/decks/{id}`) returned `500`/`50000` instead of
  `405`/`40500`. Root-caused to a missing `@ExceptionHandler` for
  `HttpRequestMethodNotSupportedException`. Fixed, backend restarted, and
  the same request re-verified to return `405`/`40500` with no stack trace
  logged.
- All test data created during this session was deleted afterward; the dev
  DB was confirmed back at its documented baseline (2 notes, 1 conversation,
  0 subjects, preferences at `system`/`zh-CN`/`60`) via a final API check.

### Browser (Playwright, chromium, demo user)

- Login → warmup pass over all 9 product views → zero page/console errors
  throughout.
- Theme sweep (light/dark/system) driven through the real Settings UI (not
  raw `localStorage`, which is overwritten by server preference
  reconciliation on every full page load) across all 9 views: zero
  horizontal overflow, zero errors, `html.dark` class toggled correctly and
  persisted across a full reload.
- Locale sweep (zh-CN/en-US) driven through the real Settings UI, spot-
  checked on Workspace and Settings, both at 1440px and 375px: zero
  overflow, `html.lang` updated correctly.
- **Defect found and fixed**: switching locale without navigating left the
  tab `<title>` stale until the next route change. Root-caused to
  `document.title` only being set in `router.afterEach`. Fixed with a
  `watch()` on `i18n.global.locale`; re-verified with an isolated repro
  script showing the title now updates the instant the locale chip is
  clicked, with no navigation required.
- `/design-system` route confirmed clean (no overflow) in both light and
  dark, screenshotted for the record.
- Garbage URL (`/this-route-does-not-exist-abc123`) confirmed rendering the
  branded `NotFoundView`, not a blank page or a framework error screen.
- `grep -rn mock ai-learning-web/src --include='*.ts' --include='*.vue' -i`
  → **clean**, zero references anywhere in application source.

### Environment note

The backend's documented recurring JVM native crash
(`exit code -1073741819`, see prior handoffs) occurred **a third time**
this session, again during ordinary database activity in a login/preference
flow, unrelated to any code this session touched. As documented in the
step-12 handoff, a third occurrence was flagged as the trigger to stop
treating it as unexplained — see § Known limitations below. The crash left
no data corruption (the in-flight `UPDATE` never committed; confirmed by
checking the preference row immediately after restart) and was resolved by
a simple restart, consistent with both prior occurrences.

## Known limitations

- **Recurring JVM native crash** (JDK 22 / Windows / this
  `spring-boot:run` dev-launch setup) — now observed **three times** across
  three sessions (steps 11, 12, and this session), always during ordinary
  login/preference database activity, never during anything specific to the
  step's own code changes, and never causing data corruption. Restarting the
  backend has fully resolved it every time. Per the step-12 handoff's own
  trigger condition ("if a third occurrence happens, it's worth capturing a
  crash dump rather than continuing to treat it as unexplained"), this is
  now a standing Phase 8 investigation candidate — see § Recommended Phase 8
  scope. It has never affected a real user-facing session; it is a local dev
  environment characteristic, not a product defect.
- **File upload / OSS for Materials** — deferred, `storage_key` reserved.
  See `docs/mock-migration.md` § Deferred items.
- **Spaced-repetition review engine** — deferred, schema columns reserved
  since Phase 5.
- **Server-side AI suggestions on Workspace** — today's panel is honest
  client-side rule-based nudges from real data, not a model call.
- **Client-timezone-aware streak calculation** — server-default timezone is
  the interim choice (D6).
- **`subject_name` snapshot vs. live join on `ai_conversations`** — open
  product question, not a technical blocker.
- **Registration/email/password-reset, payments, external auth,
  WebSockets, Docker/CI** — explicitly out of scope for Phase 7 by the
  phase's own constraints; extension points are reserved where relevant
  (see `docs/architecture.md` § Identity & security).

None of the above are regressions or incomplete work within Phase 7's actual
scope — they are deliberate, documented deferrals.

## Production readiness assessment

**Ready for continued internal/beta use; not yet hardened for public
production deployment.** Specifically:

- ✅ Per-user data isolation enforced consistently (`OwnershipGuard` on every
  user-scoped query, verified by both automated tests and this session's
  live D2 cascade test).
- ✅ No fabricated data anywhere — every metric either reflects real state or
  renders an honest empty/`—` value.
- ✅ Error handling is now consistent for both application errors
  (`BusinessException` → typed codes) and framework-level routing errors
  (this session's 405 fix closes the last gap in that consistency).
- ✅ Secrets (`JWT_SECRET`, `DEEPSEEK_API_KEY`) are environment-only, never
  committed.
- ⚠️ No containerization, CI/CD, or observability stack — local-dev-only
  today (`docker/README.md` still empty by design).
- ⚠️ No registration/password-reset path — the `demo` seed account is the
  only way in; fine for an internal/demo deployment, not for public
  self-serve signup.
- ⚠️ The recurring JVM crash (see above) has not affected correctness but
  has not been root-caused either; acceptable for a dev environment,
  worth resolving before a production deploy target is set.

## Metrics

| Metric | Value |
| --- | --- |
| Backend tests | 53/53 passing (unchanged count from step 12; this session added no new backend tests, only a bug fix covered by re-running the existing suite + a live repro) |
| Frontend unit tests | 8/8 passing |
| Frontend build | Clean, no errors/warnings surfaced |
| Backend modules with real endpoints (cumulative through Phase 7) | 14 (`auth`, `user`, `system`, `note`, `flashcard`, `ai`, `subject`, `material`, `task`, `calendar`, `workspace`, `analytics`, `preference`, plus `common`) |
| `mock.ts` files remaining | 0 (8 deleted across Phase 7, confirmed clean this session) |
| Product views verified this session (browser) | 9 (Workspace, Subjects, Notes, Flashcards, AI Tutor, Calendar, Analytics, Profile, Settings) + Design System + 404 |
| Defects found and fixed this session | 2 (backend 405 handling, frontend title reactivity) |
| Phase 7 total commits (through this session) | 13 feature/docs step commits + this session's closeout commit(s) |

## Recommended Phase 8 scope

Not decided here — this is a candidate list for a dedicated Phase 8 planning
conversation, drawn from this report's § Known limitations and the plan's
own out-of-scope list:

1. **Diagnose the recurring JVM crash** — now a 3-occurrence pattern; enable
   `-XX:+CreateMinidumpOnCrash` or explicit `hs_err` dump output on the dev
   launch command to finally get a stack trace instead of a bare exit code.
2. **Registration / email verification / password reset** — the schema and
   `TokenService` seams have been reserved since Phase 2 specifically for
   this.
3. **File upload / OSS storage** for Materials — `storage_key` reserved
   since Phase 5.
4. **Production operations**: Docker images, CI/CD, observability
   (logging/metrics/tracing) — nothing built yet, `docker/README.md` is
   still an empty placeholder.
5. **Spaced-repetition review engine** for Flashcards — schema reserved
   since Phase 5.
6. Smaller candidates noted in `docs/mock-migration.md` § Deferred items:
   server-side AI suggestions, client-timezone-aware streak, the
   `subject_name` snapshot-vs-join question.

## Session verification log (this session)

1. Confirmed repo state matched the step-12 handoff exactly (`beaddb0`,
   clean tree) before starting.
2. Wrote all Phase 7 documentation (architecture.md, product-domain.md,
   design-system.md, mock-migration.md, README.md, delivery report).
3. Ran the full static suite (backend tests, frontend type-check/lint/unit/
   build) — all green.
4. Launched both dev servers and ran the plan's final-acceptance checklist
   at the live API and browser surface — found and fixed two defects (see
   above), re-verified both, re-ran the full static suite again (still
   green), and restored the dev DB to its documented baseline.
5. Wrote this report and rewrote `docs/phase7-handoff.md` to mark Phase 7
   complete.
