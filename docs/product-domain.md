# Product Domain

Phase 5 deliverable. This is the first document that describes the *product*
rather than the platform underneath it — read `docs/architecture.md` first for the
engineering constitution this domain model has to obey, and `docs/design-system.md`
for the visual language every screen below reuses unmodified.

## Positioning

The AI Learning Platform is an AI-native learning workspace — not an educational
admin system, not a course-delivery CMS. Everything revolves around one verb:
**learning**. The product surface is organized around what a learner actually does:
learn, practice, review, create, reflect, track progress — with AI assistance
arriving as a first-class capability in Phase 6, not bolted on afterward.

Phase 5 builds the full workspace shell and every product module with realistic
mock data and a real (empty) backend schema. No AI calls are made yet; the AI Tutor
module is a fully-built chat UI wired to a swappable `ChatProvider` that currently
streams a canned reply.

## Domain model

```
User (Phase 2)
  └─ Subject                          (anchor of the learning domain)
       ├─ LearningMaterial            (pdf / markdown / video / article / link / document)
       ├─ Note                        (optional subject link)
       ├─ FlashcardDeck → Flashcard   (optional subject link)
       ├─ LearningTask                (optional subject link)
       └─ StudySession                (optional subject link)

Workspace   — read-model façade, owns no tables, aggregates the above
Analytics   — read-model façade, owns no tables, derives from the above
```

**Subject is the anchor.** Every other learning artifact *may* hang off a subject via
a logical `subject_id` FK, but none of them *require* one — a note, task, or study
session can exist independently. This mirrors how people actually learn: not
everything is filed under a course.

**Workspace and Analytics own no tables.** They are aggregation façades. The
workspace dashboard endpoint (Phase 6+) will compose reads across subjects, tasks,
sessions and flashcards; if that composition ever gets expensive, a materialized
summary table is added *inside* the `analytics` package by a new migration — never by
widening the source domains to carry derived data they don't own.

### Entity relationships

| Entity | Belongs to | Optional subject link | Notes |
| --- | --- | --- | --- |
| `Subject` | User | — | `progress` is the only denormalized/user-curated field; study time and material counts are derived. |
| `LearningMaterial` | Subject (required) | — | Exactly one of `sourceUrl` (external) or `storageKey` (uploaded, future `StorageService`) is expected to be set. |
| `Note` | User | optional | Markdown source only; outline/excerpt are computed client-side, never stored. |
| `FlashcardDeck` / `Flashcard` | User | optional | Scheduling columns (`due_at`, `interval_days`, `ease`) are reserved for a future spaced-repetition engine — present in the schema now so no migration is needed when it lands. |
| `LearningTask` | User | optional | Deliberately not a project-management system: no assignees, no nesting, no workflow states beyond todo/in-progress/done. |
| `StudySession` | User | optional | Duration is always derived (`endsAt - startsAt`), never stored. Powers both the calendar and analytics. |

## Module responsibilities

| Product module | Route | Backend package | Responsibility |
| --- | --- | --- | --- |
| Workspace | `/workspace` | `workspace` (façade) | Learning dashboard: greeting, streak/goal/due-cards stats, continue-learning rail, today's tasks/sessions, recent notes/chats. |
| Subjects | `/subjects`, `/subjects/:id` | `subject`, `material` | Subject grid + detail: progress, materials, related notes. |
| AI Tutor | `/ai-tutor/:conversationId?` | *(none yet — Phase 6 `ai`)* | Conversation list + streaming chat thread, built against the `ChatProvider` abstraction (see below). |
| Flashcards | `/flashcards` | `flashcard` | Deck list, card list, review-mode preview (flip only — no scheduling logic yet). |
| Notes | `/notes` | `note` | Note list + outline rail + read-only content preview (full markdown editor is a later phase). |
| Calendar | `/calendar` | `calendar`, `task` | Week/month views merging study sessions with due-dated tasks. |
| Analytics | `/analytics` | `analytics` (façade) | Weekly activity, subject time distribution, study heatmap — all mock data validated against the dataviz six-checks palette. |
| Profile | `/profile` | `user` (existing) | Identity + learning-overview stats. |
| Settings | `/settings` | `system` (existing) | Appearance/language controls (moved from the old `HomeView`) + live backend status check. |

Every route above is wired in `router/index.ts` today, even where the underlying API
doesn't exist yet — per the roadmap's "every module already has routes, even if
temporarily placeholder."

## Frontend architecture

Feature-first, one directory per module under `src/features/`:

```
features/<module>/
  types.ts        — domain types (mirrors the backend entity/DTO shape)
  mock.ts         — realistic demo fixtures (replaced by API calls in a later phase)
  <Module>View.vue
  components/     — module-local components (e.g. subjects/components/SubjectCard.vue)
```

`src/views/` keeps only the pages that predate this phase (`LoginView`, `WelcomeView`,
`DesignSystemView`) — new product surfaces live under `features/`, per the
architecture doc's "grow into `features/` when modules appear."

Nothing here duplicates Phase 1–4 infrastructure: every view is built from the
existing `AppX` component library, existing layout/router/i18n/API-layer seams, and
existing design tokens. Two additions were made *to* those shared layers (not
around them), because every module needed them:

- **Accent tokens** (`--accent-indigo/teal/amber/rose/violet` in `tokens.css`) — the
  fixed-order categorical palette used for subject identity and every chart's
  categorical color. Validated per mode (light/dark) with the dataviz skill's
  six-checks script (lightness band, chroma floor, CVD separation, contrast) before
  being written into the token file.
- **Sidebar collapse state** (`stores/app.ts`) — persisted alongside the existing
  theme/locale preferences, since the fuller nine-item nav needed a rail mode.

### AI provider abstraction (`features/ai-tutor/provider.ts`)

```ts
interface ChatProvider {
  readonly id: string
  streamReply(history: ChatMessage[]): AsyncGenerator<string, void, undefined>
}
```

The chat UI (`AiTutorView.vue`) only knows this interface — it appends yielded
chunks, auto-scrolls, and shows a busy/streaming state. Today it's wired to
`MockChatProvider`, which types out an i18n-supplied canned reply with realistic
timing. Phase 6 adds a `server-sse` provider that opens an SSE connection to the
backend's `AiService`; the UI does not change when that swap happens — this is the
seam the roadmap's "SSE streaming chat" item plugs into.

## Backend architecture

Package-by-feature, matching every existing module (`auth`, `user`). Phase 5 adds
seven packages, each `entity` + `mapper` only — no `service`/`controller`/`dto` until
a module gets its first real endpoint (per "no business logic explosion"):

```
subject/      entity/Subject, entity/SubjectStatus, mapper/SubjectMapper
material/     entity/LearningMaterial, entity/MaterialType, mapper/LearningMaterialMapper
note/         entity/Note, mapper/NoteMapper
flashcard/    entity/FlashcardDeck, entity/Flashcard, mapper/FlashcardDeckMapper, mapper/FlashcardMapper
task/         entity/LearningTask, entity/TaskStatus, entity/TaskPriority, mapper/LearningTaskMapper
calendar/     entity/StudySession, mapper/StudySessionMapper
workspace/    (package reserved — façade only, no entities)
analytics/    (package reserved — façade only, no entities)
```

Schema lands in `V2__create_learning_domain_tables.sql`: seven tables (`subjects`,
`learning_materials`, `notes`, `flashcard_decks`, `flashcards`, `learning_tasks`,
`study_sessions`), same conventions as `V1` — snowflake ids, audit columns, logical
(indexed, unconstrained) foreign keys, `utf8mb4_unicode_ci`. No AI-related tables
(conversations/messages) are created yet — those arrive with `AiService` in Phase 6,
per the roadmap's explicit split between "Phase 5 — domain design + workspace" and
"Phase 6 — AI integration."

Reserved error-code ranges (contiguous with the existing `100000–109999` auth
block, 10000 per module):

| Range | Module |
| --- | --- |
| 110000–119999 | `subject` |
| 120000–129999 | `material` |
| 130000–139999 | `note` |
| 140000–149999 | `flashcard` |
| 150000–159999 | `task` |
| 160000–169999 | `calendar` |
| 170000–179999 | `workspace` |
| 180000–189999 | `analytics` |
| 190000–199999 | `ai` (Phase 6) |

## Data flow (today vs. Phase 6)

**Today:** every view reads synchronously from a module's `mock.ts`. Fixtures are
internally consistent (a subject's `studyMinutes`, its materials, its notes, and its
calendar sessions all reference the same subject ids) so the dashboard "feels alive"
without a backend round-trip.

**Phase 6+:** each `mock.ts` is replaced by an `api/modules/<module>.ts` following the
existing `auth`/`system` pattern (typed axios calls through `api/http.ts`, envelope
unwrapped, errors normalized to `ApiError`). View components change their data source,
not their template — the mock fixtures already have the exact shape the API will
return, because `types.ts` was written to mirror the backend entities from the start.

## Architecture decisions

| Decision | Rationale |
| --- | --- |
| Subject is optional everywhere except `LearningMaterial` | Materials without a subject have no home in the UI (there's no "unfiled materials" view); notes/tasks/sessions all have one. |
| No AI/conversation persistence in Phase 5 | Keeps this phase's migration free of a schema that Phase 6's `AiService` design might still change; the chat UI already proves the streaming UX without it. |
| `ChatProvider` abstraction introduced now, not in Phase 6 | The chat UI is real product surface today (Phase 6 explicitly says "prepare... do NOT implement AI yet") — building it against an interface instead of a hardcoded mock means Phase 6 is a provider swap, not a UI rewrite. |
| Flashcard scheduling columns reserved but unused | Same reasoning as the RBAC tables in `V1`: the schema is complete on day one so the review engine's arrival needs no migration, only a service. |
| Workspace/Analytics own no tables | Prevents duplicate-source-of-truth bugs (e.g. a cached "total study minutes" drifting from the sessions it's summed from). |
| Accent color is a token, not a per-subject hex in code | Keeps the categorical palette centrally validated (CVD-safe, contrast-safe in both themes) instead of allowing ad-hoc colors to creep in as subjects are added. |

## Phase 6 readiness

What Phase 6 finds waiting for it:

- `ChatProvider` interface + `MockChatProvider` reference implementation to replace.
- Empty `ai` backend package slot and reserved error-code range (190000–199999).
- `AiService` abstraction point already named in `docs/architecture.md`'s
  infrastructure section — the SSE provider implements against it.
- Every product surface that AI will eventually touch (AI Tutor chat, flashcard
  generation, note summarization, analytics "AI usage" stat) already has a stable UI
  and route, so Phase 6 is additive, not a redesign.
