# Phase 7 Delivery Report — Commercial Product Foundation

Workstream D deliverable per the phase plan (`D:\claude-data\plans\refactored-wiggling-floyd.md`).
This document narrates *what was built and why*, organized by workstream and
design decision. For verification results, known limitations, and the Phase 8
recommendation, see the companion `docs/phase7-final-report.md` (written after
this document, once the full acceptance pass was green).

## Scope

Phase 7 turned the Phase 5/6 mock-data shell into a real, per-user-isolated
SaaS foundation: real Subject/Material/Task/Calendar/Preferences CRUD, real
Workspace/Analytics read models, subject linkage through Notes/Flashcards/AI
Tutor, a premium 3-mode theme (dark now black+purple luxury), and a UX
unification pass. Out of scope by explicit constraint: registration/email/
password-reset, payments, OSS/file upload, external auth, a spaced-repetition
engine, WebSockets, Docker/CI.

## Design decisions (D1–D10)

| # | Decision | Outcome |
| --- | --- | --- |
| D1 | `common/OwnershipGuard.require(...)` — one ownership-check helper, not a generic base service | Retrofitted onto `note`/`flashcard`/`ai` (behavior-neutral); used by every new module. Replaced 8 copies of the same branch. |
| D2 | Subject delete: soft-delete materials, nullify `subjectId` elsewhere | Implemented exactly as specified in `SubjectService.delete` (`@Transactional`); delete dialog states the distinction explicitly. |
| D3 | `useAsync<T>` composable, no query library | One ~30-line composable backs every view's loading/empty/error sequence at this scale (~10 views). |
| D4 | `user_preferences` (V4), server-wins reconciliation, localStorage as FOUC-safe boot path | Implemented; see `docs/architecture.md` § Phase 7 preferences reconciliation contract. |
| D5 | Analytics: on-the-fly SQL aggregation, no new tables | Implemented; `weekDeltaPercent` and similar metrics are nullable (`—`, never a fabricated `0`) when there's no prior-week baseline. |
| D6 | Streak: consecutive days ending today-or-yesterday, server-default timezone | Implemented, unit-tested (empty/gap/today-missing cases). Client-TZ awareness deferred — see `docs/mock-migration.md` § Deferred items. |
| D7 | Workspace: single `GET /v1/workspace/summary` aggregate | Implemented — one round trip, one loading state for the entire dashboard. |
| D8 | Materials: metadata + external links only, `storage_key` reserved | Implemented; upload UI shows a disabled "coming soon" state rather than a broken control. |
| D9 | Preferences error range 200000–209999 | Reserved and documented in `docs/architecture.md`'s error-range table in the same phase. |
| D10 | `PUT /v1/auth/profile` (nickname/avatar) | Implemented — not cut despite being a designated time-pressure cut candidate. |

Plus the AI `subjectId` closure (not separately lettered in the plan but
equally binding): `ai_conversations` gained a nullable `subject_id` (V5),
`ContextHints.subjectId` resolves through `SubjectService.resolveOwnedSubject`,
and `LearningContextService` pulls real subject/material/note data server-side
instead of trusting client-supplied text. Full detail: `docs/ai-engine.md` §
Context pipeline.

## Workstream A — Backend

Seven previously entity-and-mapper-only packages (`subject`, `material`,
`task`, `calendar`, `workspace`, `analytics`) plus one new package
(`preference`) got their first real `service`/`controller`/`dto`, all
following the existing `note`/`flashcard` pattern (package-by-feature,
`ApiResponse<T>` envelope, record DTOs with `from()`, `BusinessException` +
reserved error range per module). `V4__create_user_preferences.sql` and
`V5__add_subject_id_to_ai_conversations.sql` shipped alongside a `pom.xml` fix
(the undefined `${flyway.version}` property, let Spring Boot's BOM manage it).
53 backend tests green throughout, growing as each module landed its own
suite (ownership 403s, D2 cascade, streak edge cases, preference upsert
defaults).

## Workstream B — Frontend

`api/modules/` gained one typed module per new backend endpoint (`subject.ts`,
`material.ts`, `task.ts`, `calendar.ts`, `workspace.ts`, `analytics.ts`,
`preferences.ts`), plus extensions to `auth.ts` (`createdAt`, `updateProfile`)
and `ai.ts` (`subjectId`). Shared infra: `useAsync` (+ unit test), `StatTile`
(dedupes Workspace/Profile tile markup, introduced once a second real
consumer existed). Every one of the 9 product views (Workspace, Subjects,
Notes, Flashcards, AI Tutor, Calendar, Analytics, Profile, Settings) was
retrofitted from its `mock.ts` fixture to the real API — full per-file mapping
in `docs/mock-migration.md`. Workspace was the centerpiece rewrite: a 12-col
productivity layout (header + 4 StatTiles + continue-learning rail + recent
AI/notes + a dataviz-skill-built knowledge-growth chart + upcoming tasks with
inline quick-add + today's sessions + client-side rule-based AI suggestions),
replacing the old admin-KPI layout entirely. All 8 `mock.ts` files were
deleted; `index.html` got a real title/meta/favicon; a branded `NotFoundView`
+ catch-all route replaced the previous no-404-route gap.

## Workstream C — Theme & UX

The dark theme's neutral ramp was re-hued to a low-saturation ~248°
indigo-violet through every step (previously flat gray-black), validated with
a WCAG contrast script against the prior shipped ramp (every ratio met or
beat baseline — see `docs/phase7-final-report.md` § Verification for exact
numbers) rather than by eye. A new `--shadow-glow-primary` token adds a
very-low-alpha purple halo to exactly three interactive-emphasis surfaces
(never ambient). Glass surfaces became more translucent/blurred with a
violet-tinted border/highlight in dark mode. Light mode received only
micro-polish — visually confirmed pixel-identical to its pre-change baseline
in a regression screenshot comparison. The categorical accent palette needed
no changes, re-validated against the new surface with the dataviz skill.
Alongside the re-skin, a UX unification pass fixed one real bug found by
audit: `AppSidebar`'s inline theme/locale chips were calling local-only store
actions instead of `updatePreferences(...)`, so a sidebar-driven theme change
silently failed to persist across devices — fixed and regression-tested with
a fresh, unauthenticated browser context.

## Workstream D — Docs (this step)

`docs/architecture.md` gained a § Phase 7 section (error-range table,
`OwnershipGuard`, D2 cascade, read-model contracts, preferences
reconciliation, AI `subjectId` flow, auth extension points) and an updated
roadmap entry. `docs/product-domain.md` and `docs/design-system.md` were
updated in place — mock-era language replaced with the real implementation,
new sections for the dark identity/`StatTile`/view-state pattern. New
`docs/mock-migration.md` records the per-mock replacement table and the
deferred-items list. `README.md` gained a product-surface section. This
document and `docs/phase7-final-report.md` close out the phase.

## Commit spine

`1a20f23` (A1+A2) → `d93f07c` (A3+A4) → `ccdd41a` (A5+A6+A7) → `cac1706` (A8)
→ `cdea352` (A9+A10) → `c73f203` (B1+B2) → `cbbff8b` (B3+B4) → `f16a81a` (B5)
→ `3f0cc94` (B6) → `c0c6d3a` (B7+B8) → `921fa57` (B9) → `50a80e9` (C1+C2) →
this docs step, each interleaved with `docs:` handoff commits per the
"one logical step = one focused commit" convention.
