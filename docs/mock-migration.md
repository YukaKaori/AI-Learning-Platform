# Mock Migration

Phase 7 deliverable (Workstream D). A per-mock record of what replaced each of
the 8 `features/*/mock.ts` fixture files that Phases 5–6 shipped, plus the
items that were deliberately deferred rather than built this phase. Written
once, at Phase 7's docs step, after all 8 files were confirmed deleted
(`grep -r mock src/` clean — see `docs/phase7-final-report.md` §
Verification). This document is a historical record, not a living reference —
it will not be updated as the codebase evolves further.

## Per-mock table

| Source mock file | Held | Real replacement | Status |
| --- | --- | --- | --- |
| `features/subjects/mock.ts` | `mockSubjects: Subject[]` (5 canned Chinese subjects), `LearningMaterial[]` per subject | `subject` module (110000s): `SubjectController`/`SubjectService`, `GET/POST/PUT/DELETE /api/v1/subjects`; `material` module (120000s) nested under it; frontend `api/modules/subject.ts` + `material.ts`, `stores/subjects.ts` (list cache + `byId`, replaces every mock `getSubject()` enrichment call site) | **Done.** Empty-account first run shows a designed empty state + "create your first subject" starter chips (real subjects, not fixtures). |
| `features/materials` (nested inside subjects mock) | 10 materials across the 5 subjects, links only | `material` module, nested `GET/POST /api/v1/subjects/{subjectId}/materials`, `PUT/DELETE /api/v1/materials/{id}` | **Done**, with a documented gap — see Deferred items (file upload/OSS). Upload UI is disabled "coming soon"; only external links work today. |
| `features/ai-tutor/mock.ts` | `mockConversations: Conversation[]` — 1 canned Chinese conversation with a scripted reply | `ai` module (Phase 6, unchanged this phase) — real conversation CRUD + DeepSeek streaming, `ContextHints.subjectId` added Phase 7 for real subject linkage | **Done** (conversation persistence landed Phase 6; the subject-linkage half of this specific mock's remaining gap closed Phase 7 — see `docs/ai-engine.md` § subject-resolution). |
| `features/analytics/mock.ts` | `DayActivity[]` seeded-random weekly bars, fake subject-time distribution, hard-coded "Demo data" subtitle | `analytics` module (180000s, façade, no tables): `GET /v1/analytics/summary`, `GET /v1/analytics/activity?days=N`, `GET /v1/analytics/subject-shares?days=30` — all on-the-fly SQL aggregation over real tables | **Done.** "Demo data" locale strings deleted (`en-US.ts`/`zh-CN.ts`). Empty account shows zero-filled real data (a flat heatmap, no charts pretending to have history), never fabricated numbers. |
| `features/calendar/mock.ts` | `StudySession[]` spread over the current week, hard-coded to always "feel alive" | `calendar` module (160000s): `GET/POST/PUT/DELETE /api/v1/study-sessions` with mandatory `?from=&to=` window; frontend windowed fetch per visible month | **Done.** |
| `features/flashcards/mock.ts` | `mockDecks`/`mockCards`, `totalDue()` helper (consumed by both `workspace/mock.ts` and `analytics/mock.ts`) | `flashcard` module (real since Phase 6, unchanged this phase) — `dueCards` in the Workspace summary now counts real `due_at <= now` rows via `FlashcardMapper` | **Done** (flashcard CRUD itself was already real since Phase 6; this phase only removed the mock's `totalDue()` cross-import once Workspace/Analytics moved server-side). |
| `features/notes/mock.ts` | `mockNotes: Note[]`, several referencing `subjectId: 'sub-ml'` (a fixture-only id) | `note` module (real since Phase 6, unchanged this phase) — gained a nullable, validated `subjectId` FK this phase (subject picker in the note editor) | **Done.** |
| `features/subjects/mock.ts` → tasks | *(tasks lived in their own mock, cross-referencing `mockSubjects` ids)* | see next row | — |
| `features/tasks/mock.ts` | `mockTasks: LearningTask[]`, `subjectId: 'sub-ml'`-style fixture references | `task` module (150000s): `GET/POST/PUT/DELETE /api/v1/tasks` with `status`/`dueBefore`/`subjectId` filters; status→done sets `completedAt`, leaving done clears it | **Done.** Rendered inside both Calendar (due-dated) and Workspace (upcoming list with inline quick-add) — no separate Tasks nav item, per the phase's "tasks managed inside Workspace + Calendar" user decision. |
| `features/workspace/mock.ts` | `DashboardStats` (hard-coded `streakDays: 12`, `studiedToday: 62`, goal `90`), `continueLearning` derived from `mockSubjects` | `workspace` module (170000s, façade, no tables): single aggregate `GET /v1/workspace/summary` — stats (real streak via D6's consecutive-day algorithm, `studiedToday` from real sessions, `dailyGoal` from `user_preferences`, real `dueCards`/`activeSubjects`), `continueLearning` (top-3 by recent linked activity), `upcomingTasks`, `recentConversations`, `recentNotes`, `todaySessions`, `weekActivity` | **Done.** One round trip, one loading state, one designed empty state per section. |

No mock file existed for Profile or Settings — both read from real `user`/`auth`
data since earlier phases; Phase 7 added `PUT /v1/auth/profile` (nickname/
avatar, killing the "disabled edit button" demo tell) and real `memberSince`
from `AuthUserResponse.createdAt`, plus `preference`-backed Settings
persistence (theme/locale/daily-goal, replacing the local-only toggles).

## Deferred items

Not built this phase — listed here (rather than silently dropped) so a future
phase doesn't have to rediscover the gap from scratch.

| Item | Priority | Why deferred |
| --- | --- | --- |
| File upload / OSS for Materials | High — most visible remaining "coming soon" | `storage_key` column reserved on `learning_materials` since Phase 5 so no migration is needed when this lands; needs a `StorageService` abstraction (Aliyun OSS/MinIO/S3/local, per `docs/architecture.md`'s external-service-abstraction rule) that doesn't exist yet. Explicitly out of scope for Phase 7 per the phase's own constraints. |
| Spaced-repetition review engine | Medium | `flashcard`'s `due_at`/`interval_days`/`ease` columns have been reserved since Phase 5 specifically for this; today `dueCards` is a simple `due_at <= now` count, not a real SM-2-style scheduler. No UI currently promises scheduling beyond flip-to-review. |
| Server-side AI suggestions (Workspace "AI Suggestions" panel) | Medium | Today the panel is client-side rule-based nudges derived from the real `workspace/summary` data (e.g. "you have 3 cards due" from real `dueCards`) — honest, not fake, but not an AI call. A server endpoint that has the model generate the nudge text would need its own prompt template and cost/latency budget; not justified yet by user demand. |
| Client-timezone-aware streak calculation | Low | D6 chose server-default-timezone as the interim streak boundary (consecutive days with ≥1 session ending today or yesterday, in the server's timezone). A user in a very different timezone from the server could see a streak boundary shift at an unexpected local hour. Documented as the known interim choice; low priority because the server and expected user base are both currently single-timezone. |
| `subject_name` snapshot vs. join retirement on `ai_conversations` | Low | `ai_conversations.subject_name` is a deliberate display snapshot (kept readable after a subject rename/delete) rather than always joining `subjects` live. Whether to retire the snapshot in favor of a live join (accepting that a deleted subject's conversations would then show nothing instead of the last-known name) is an open product question, not a technical blocker — revisit if the snapshot's staleness ever causes a real user complaint. |
