# Roadmap to Version 1.0 (Phase 15 → 35)

> **Status:** Planning only — no code, no implementation started. Awaiting approval before Phase 15.
> **Supersedes:** the high-level `docs/roadmap-phase15-35.md` (kept for reference; this is the canonical, code-grounded plan).
> **Author lens:** design lead + product architect of a commercial AI-native learning platform.
> **Date:** 2026-07-20

## What exists today (verified against the code, not assumed)

- **Backend** — Spring Boot 4 modular monolith, Java 22, MyBatis-Plus, MySQL, Flyway (V1–V5), servlet MVC + virtual threads. Feature packages: `auth, user, subject, material, note, flashcard, task, calendar, workspace(façade), analytics(façade), preference, ai, system, common, config`.
- **AI** — DeepSeek behind `ai/provider/AiProvider`; true SSE streaming (`SseRelay` on virtual threads), one-shot generation, `LearningContextService` + `PromptBuilder` pipeline. **Stateless: no embeddings, no vector store, no per-user AI memory.**
- **Frontend** — Vue 3 + TS + Vite, Pinia, Element Plus (token-bridged), vue-i18n (zh-CN/en-US mirror-enforced), 10 feature modules under `features/`, custom SVG charts (no chart lib), `useAsync` view-state, `AppX` design system on `styles/tokens.css`.
- **Reserved seams already in place** — RBAC `roles`/`permissions` tables + empty `UserPrincipal` authorities; flashcard `due_at`/`interval_days`/`ease` scheduling columns; `StorageService` interface point; glass theme mode; and the architecture doc's explicit "reserved, do not implement early" list: **Redis, OSS, WebSocket, Elasticsearch, MQ, scheduler, audit log**.
- **Notable gaps** — Notes is a **read-only preview + AI toolbar** (no real editor); Flashcards is **flip-only** (no spaced repetition despite reserved columns); no file upload; no notifications; single-user only (auth has no registration/reset/OAuth yet); no payments; no PWA.

The roadmap turns these reserved seams into real products in dependency order, then commercializes.

**Complexity scale:** `S` ≈ 2–3 dev-days · `M` ≈ 3–5 · `L` ≈ 5–8 · `XL` ≈ 8–15.
Every phase is independently shippable (ships behind a flag if partial value only).

---

## Arc I — Platform Infrastructure (turn reserved seams into real capabilities) · P15–17

These three phases build the horizontal capabilities almost every later phase needs. Doing them first prevents each feature phase from inventing its own storage/search/jobs — the "no duplicated systems" rule.

---

### Phase 15 — File & Media Pipeline (Storage + Upload + Ingestion)

- **Goal:** Real file upload/download behind the reserved `StorageService`, so `LearningMaterial.storageKey` becomes real and documents can enter the system.
- **Why it exists:** Materials today can only be external URLs; PDF chat, OCR, the browser clipper, avatars, and community templates all need durable object storage. It's the lowest-level unmet dependency.
- **User value:** Upload PDFs/images/docs to a subject; they're stored, previewable, and downloadable.
- **Technical scope:** `infrastructure/StorageService` interface + `LocalStorageProvider` (dev) and `OssStorageProvider` (prod, MinIO/S3/Aliyun) behind config; presigned upload/download; MIME/size validation; virus/type guard; text-extraction hook (PDF→text) as an interface for later OCR/RAG; `material` module gains upload endpoints.
- **Files likely affected:** new `com/yuka/ailearningserver/infrastructure/storage/*`; `material/` controller+service+dto; `AppProperties` (`app.storage.*`); new migration `V6__material_storage.sql` (storage metadata cols); `api/modules/material.ts`; `features/subjects/**` upload UI; new `AppFileUpload` component; `docs/architecture.md` (unreserve OSS).
- **Complexity:** L (5–8d).
- **Risks:** Storage vendor lock-in (mitigate: interface-first, never SDK in business code); large-file streaming vs. servlet memory; security of presigned URLs; unbounded storage cost.
- **Verification:** curl upload→download round-trip; Playwright upload on Subject detail; `./mvnw test` for size/type rejection; verify per-user ownership on download.
- **Dependencies:** none (foundational).
- **Acceptance criteria:** authenticated user uploads a PDF to a subject, sees it listed, downloads it; cross-user download 403s; provider swap is config-only; extracted text is retrievable for the ingestion hook.

---

### Phase 16 — Retrieval Core (Embeddings + Vector Store + Semantic Index)

- **Goal:** A `RetrievalService` seam that embeds and semantically searches user content (notes, materials, flashcards) — the backbone for RAG, AI search, knowledge graph, and agents.
- **Why it exists:** The AI is stateless and ungrounded. Every "ask across your knowledge" feature dies without retrieval. Building it once, centrally, avoids three feature phases each bolting on their own search.
- **User value:** (Foundational; surfaced fully in P21.) Immediate payoff: "related notes/materials" suggestions.
- **Technical scope:** `infrastructure/retrieval/RetrievalService` (embed, upsert, query) with a `pgvector`-style store (start with MySQL + a vector column/ext or a sidecar store behind the interface); `EmbeddingProvider` behind the same abstraction pattern as `AiProvider`; ingestion pipeline (chunk → embed → index) triggered on content write and backfill job; retrieval feeds `LearningContextService` (grounded RAG replies with citations).
- **Files likely affected:** new `infrastructure/retrieval/*`, `ai/provider/EmbeddingProvider*`; hook into `note`/`material`/`flashcard` services on write; extend `ai/context/LearningContextService` for retrieval-augmented context; migration `V7__embeddings.sql`; `app.ai.embedding.*` config; `docs/ai-engine.md` (RAG section).
- **Complexity:** XL (8–15d).
- **Risks:** **Highest-risk phase.** Vector store choice (avoid premature Elasticsearch/pinecone — interface so it's swappable); embedding cost at scale (batch + dedup + cache); re-embedding on model change; chunking quality; keeping index consistent with soft-deletes.
- **Verification:** ingest known corpus, assert semantic recall on curated queries; latency budget check; RAG answer cites the right source; backfill job idempotency test.
- **Dependencies:** P15 (for material text), reuses P17 jobs for backfill (can ship backfill synchronously first).
- **Acceptance criteria:** writing a note indexes it; a semantic query returns it above lexical-only matches; tutor answers ground in and cite the user's own notes; provider/store swap is config-only.

---

### Phase 17 — Async Backbone (Scheduler + Job Queue + Notifications)

- **Goal:** Background job execution, scheduled tasks, and a `NotificationService` + in-app notification center.
- **Why it exists:** Spaced-repetition reminders, daily digests, streak rollovers, embedding backfills, billing webhooks, and email all need work that isn't a synchronous HTTP request. Reserved seams (scheduler, MQ, NotificationService) get activated here.
- **User value:** In-app notifications (due reviews, reminders, mentions later); the plumbing behind every proactive nudge.
- **Technical scope:** scheduler (Spring `@Scheduled` + `ShedLock` for single-fire across instances); a job abstraction (in-DB queue first; MQ only when measured); `NotificationService` interface + in-app channel (table + endpoints + bell UI) and an email channel stub; `EmailService` seam (transactional email for P28).
- **Files likely affected:** new `infrastructure/jobs/*`, `notification/*` feature package (entity/mapper/service/controller/dto); migration `V8__notifications_jobs.sql`; `config/` scheduler wiring; frontend `features/notifications/*`, `AppLayout` bell, `api/modules/notification.ts`, Pinia `stores/notifications.ts`.
- **Complexity:** L (5–8d).
- **Risks:** at-least-once vs exactly-once semantics; job failure/retry/dead-letter; multi-instance double-fire (ShedLock); notification spam (batching/preferences).
- **Verification:** scheduled job fires once under two app instances; notification appears in bell + marks read; failed job retries then dead-letters; `./mvnw test`.
- **Dependencies:** none hard; unlocks P18, P24, P26, P27, P28, P29.
- **Acceptance criteria:** a scheduled job runs on cron and is observable; a domain event produces an in-app notification the user can read/dismiss; email seam sends via provider in prod.

---

## Arc II — Deepen thin features into real products · P18–21

The workspace has the *shape* of Notion/Anki/Obsidian but not the depth. This arc makes the existing modules genuinely competitive.

---

### Phase 18 — Spaced-Repetition Review Engine (Flashcard Studio)

- **Goal:** Turn flip-only flashcards into a real SRS using the reserved `due_at`/`interval_days`/`ease` columns.
- **Why it exists:** The single biggest learning-science moat and daily-return driver; the schema was designed for it since Phase 5.
- **User value:** Daily review queue, "cards due today," grading (again/hard/good/easy), retention that actually compounds — the Anki/Duolingo hook.
- **Technical scope:** FSRS (or SM-2) scheduler service; review-session endpoints (fetch due, grade, reschedule); review stats feeding real Analytics (replacing the composed mock snapshot); daily "cards due" job + notification (P17); AI-generated review items from notes (reuses P16 chunks).
- **Files likely affected:** `flashcard/` service+controller+dto (review endpoints), migration `V9__review_logs.sql` (per-grade history); `features/flashcards/**` review-session UI; `features/analytics/**` (real retention); `features/workspace/**` (due-cards stat becomes real); `api/modules/flashcard.ts`.
- **Complexity:** L (5–8d).
- **Risks:** scheduling-algorithm correctness (unit-test the interval math hard); timezone-correct "today" (client-tz contract already noted as deferred); migration of existing decks into scheduling.
- **Verification:** deterministic unit tests on interval/ease transitions; Playwright a full review session; due-count matches DB; analytics retention curve renders.
- **Dependencies:** P17 (due reminders), P16 (AI review-item generation, optional).
- **Acceptance criteria:** grading a card reschedules it per the algorithm; the review queue respects `due_at` and user timezone; Workspace/Analytics show real review data.

---

### Phase 19 — Notes 2.0 (Real Editor + Backlinks)

- **Goal:** Replace the read-only note preview with a real block/markdown editor supporting `[[wiki-links]]` and backlinks.
- **Why it exists:** Notes is the content substrate for the knowledge graph, community sharing, and AI writing — it can't stay read-only. This is the Notion/Obsidian core.
- **User value:** Actually write and structure notes; link notes together; see backlinks; slash-commands and inline AI (reusing the existing AI toolbar).
- **Technical scope:** editor (evaluate TipTap/ProseMirror vs. a lean markdown editor — new dependency, justify per constitution); block model or markdown-with-extensions; wiki-link parsing + backlink index; autosave; the existing `NOTE_*` AI actions become inline. Keep markdown as the storage format (per domain doc) to avoid lock-in.
- **Files likely affected:** `features/notes/**` (major); `note/` service (backlink resolution endpoint), migration `V10__note_links.sql`; `api/modules/note.ts`; new editor dependency + `docs/design-system.md` (editor tokens).
- **Complexity:** XL (8–15d).
- **Risks:** editor is a deep rabbit hole (scope: shippable v1, not Notion parity); new heavy dependency; collaborative editing explicitly out of scope (defer to a future CRDT phase); mobile editing.
- **Verification:** create/edit/link/backlink round-trip; autosave survives reload; AI inline action edits in place; Playwright editor flows; i18n intact.
- **Dependencies:** none hard; enables P20, P32.
- **Acceptance criteria:** a user writes a note with `[[links]]`, sees backlinks on the target, edits persist via autosave, and inline AI actions work.

---

### Phase 20 — Knowledge Graph

- **Goal:** An interactive graph of subjects, notes, and their links/semantic relations.
- **Why it exists:** Differentiator that turns scattered notes into a navigable knowledge base (Obsidian graph, but AI-aware). It needs Notes 2.0 links and P16 embeddings.
- **User value:** See how knowledge connects; discover related material; navigate by concept, not folder.
- **Technical scope:** graph read-model (façade, owns no tables — nodes/edges derived from subjects, note wiki-links, and P16 semantic neighbors); graph endpoint with depth/filter; force-directed graph UI (SVG/canvas, respecting the no-heavy-dep preference — evaluate a lightweight lib); "related by AI" edges from retrieval.
- **Files likely affected:** new `graph/` façade package (controller/service/dto, no entities); `api/modules/graph.ts`; new `features/knowledge-graph/*`; `router/index.ts` + nav; reuse `styles/tokens.css` accent palette for node categories.
- **Complexity:** L (5–8d).
- **Risks:** graph performance/legibility at scale (cap nodes, cluster); canvas rendering perf; semantic edges being noisy (threshold tuning).
- **Verification:** graph renders real links; clicking a node navigates; semantic edges match retrieval; large-account perf budget.
- **Dependencies:** P19 (links), P16 (semantic edges).
- **Acceptance criteria:** a user opens the graph, sees notes/subjects connected by real links and AI-related edges, and navigates to any node.

---

### Phase 21 — AI Search & Command Bar (Ask-Your-Knowledge)

- **Goal:** A global ⌘K command bar with semantic search + a Perplexity-style "ask across everything you have," answered with citations.
- **Why it exists:** The retrieval core (P16) becomes a headline user-facing feature; ⌘K is the connective tissue of Linear/Raycast/Notion-class products.
- **User value:** Instantly find any note/card/material/subject; ask a question and get a cited answer synthesized from your own corpus; jump to any action.
- **Technical scope:** unified search endpoint (semantic + lexical hybrid over P16 index, scoped per user); RAG answer endpoint (retrieve → prompt → stream with source citations, reusing `SseRelay`); ⌘K palette UI (navigation + search + AI ask in one surface).
- **Files likely affected:** `ai/` (RAG answer controller/service), `graph`/retrieval reuse; new `features/search/*` + global `AppCommandBar` in `AppLayout`; `api/modules/search.ts`; keyboard-shortcut composable.
- **Complexity:** L (5–8d).
- **Risks:** answer quality/hallucination (strict grounding + "no answer if no source"); latency of hybrid search; keyboard/focus/a11y of the palette.
- **Verification:** query returns ranked cross-entity results; AI answer cites real sources and refuses when ungrounded; ⌘K works from any route; Playwright.
- **Dependencies:** P16 (retrieval), P17 (optional analytics of searches).
- **Acceptance criteria:** ⌘K opens anywhere, searches across all content types, and answers a knowledge question with clickable citations.

---

## Arc III — AI-native depth (the reason a power user pays) · P22–25

---

### Phase 22 — AI Workspace 2.0 (Projects + Prompt Library + AI Memory)

- **Goal:** Evolve single-thread AI Tutor into a workspace: multiple conversations, AI Projects (scoped context/files), a prompt library, and a durable per-user AI Memory.
- **Why it exists:** The premium ceiling that differentiates from generic ChatGPT and justifies the top tier; foundation for agents (P25).
- **User value:** Organize AI work by project; reuse saved prompts; the AI remembers your goals/preferences/progress across sessions.
- **Technical scope:** `ai_projects` + membership of conversations/materials to a project; `prompt_library` (personal + shared later); `ai_memory` store (facts/preferences/progress, retrieved into context via P16); extend `LearningContextService` to layer project + memory context; conversation search (P21).
- **Files likely affected:** `ai/` (project + memory services, extend context pipeline), migration `V11__ai_projects_memory.sql`; `features/ai-tutor/**` → `features/ai-workspace/**` (major); `api/modules/ai.ts`; `docs/ai-engine.md`.
- **Complexity:** XL (8–15d).
- **Risks:** memory correctness/privacy (user-editable, explainable, deletable); context-window budgeting across project+memory+history; scope creep vs. Tutor.
- **Verification:** create project, scope a chat to it, save/reuse a prompt, confirm memory recall in a later session; ownership tests; token-budget guard.
- **Dependencies:** P16 (memory retrieval), P21 (conversation search).
- **Acceptance criteria:** conversations live inside projects, prompts are reusable, and the AI demonstrably recalls a stored fact in a new session.

---

### Phase 23 — PDF / Document Chat & AI Reader

- **Goal:** Chat over uploaded documents with citations, plus a distraction-free reader with highlights → auto-notes (Readwise-style).
- **Why it exists:** One of the highest-intent learning workflows ("explain this PDF"); directly monetizable; leverages P15 storage + P16 retrieval.
- **User value:** Upload a paper/textbook chapter, ask questions grounded in it, highlight passages, and turn highlights into notes/flashcards.
- **Technical scope:** document ingestion (PDF→text→chunk→embed, from P15/P16); doc-scoped RAG chat; reader view with highlight anchors persisted; "highlight → note/flashcard" actions reusing existing generation endpoints.
- **Files likely affected:** `material/` (reader + highlight endpoints), migration `V12__highlights.sql`; new `features/reader/*`; `api/modules/material.ts`, `ai.ts`; reuse `SseRelay`.
- **Complexity:** L (5–8d).
- **Risks:** PDF layout/scan quality (OCR gap — flag as follow-on); large docs vs. context window (retrieval, not full-stuff); highlight anchoring stability.
- **Verification:** upload PDF, ask a question answered with page citations, highlight → flashcard round-trip; Playwright.
- **Dependencies:** P15 (storage/extraction), P16 (retrieval), P22 (optional project scoping).
- **Acceptance criteria:** a user chats with an uploaded PDF and gets page-cited answers; highlights convert into review cards.

---

### Phase 24 — Learning Planner & Adaptive Paths

- **Goal:** Turn a goal into a scheduled, adaptive learning path across subjects/materials/reviews, wired into the calendar.
- **Why it exists:** Converts intent into daily action and ties every module (subjects, tasks, calendar, SRS) into one loop — the retention engine's brain.
- **User value:** "I want to learn X by date Y" → a concrete plan of tasks/reviews that adapts to progress and shows up on the calendar.
- **Technical scope:** planner service (goal → milestones → scheduled tasks/sessions via existing `task`/`calendar` modules); adaptation job (P17) that reschedules based on completion + SRS due load; AI plan generation reuses the existing `STUDY_PLAN` template, now grounded (P16).
- **Files likely affected:** new `planner/` package (service/controller/dto, composes task/calendar), migration `V13__learning_plans.sql`; `features/planner/*`; integrate into `features/calendar/**` and `features/workspace/**`; `api/modules/planner.ts`.
- **Complexity:** L (5–8d).
- **Risks:** over-scheduling/unrealistic plans (user-adjustable, not rigid); interaction with SRS due load; timezone/calendar correctness.
- **Verification:** generate a plan, see tasks on the calendar, complete some, confirm adaptation; unit tests on scheduling math.
- **Dependencies:** P18 (review load), P17 (adaptation job), existing task/calendar.
- **Acceptance criteria:** a goal produces a calendar-visible plan that adapts when the user falls ahead/behind.

---

### Phase 25 — AI Agents & Workflows

- **Goal:** Agentic AI that performs multi-step tasks over the user's knowledge (e.g., "build me a study guide + quiz for this subject") with tool use.
- **Why it exists:** The frontier feature (Cursor/agents) that showcases the platform's depth; only feasible once retrieval, memory, projects, and generation all exist.
- **User value:** Delegate compound learning tasks; an agent researches, drafts, generates cards, and schedules — with a reviewable plan.
- **Technical scope:** an agent runtime (plan → tool-call loop) behind an interface, using existing capabilities as tools (retrieval, note/flashcard/task creation, generation); run persistence + step trace UI; guardrails (step/cost caps via the metering added in P29 — ship gated). Servlet + virtual threads already support long-running streamed work.
- **Files likely affected:** new `ai/agent/*` (runtime, tool registry), migration `V14__agent_runs.sql`; `features/ai-workspace/**` (agent UI + trace); `api/modules/ai.ts`.
- **Complexity:** XL (8–15d).
- **Risks:** unbounded cost/loops (hard step+token caps); tool-call safety (only user-scoped, reversible tools); reliability/observability of runs; keep provider-agnostic.
- **Verification:** an agent completes a scripted multi-step task with a visible trace; cost/step caps enforced; failure surfaces cleanly.
- **Dependencies:** P16, P22, P18/P24 (tools), ideally after P29 (cost caps).
- **Acceptance criteria:** a user runs an agent that produces a study guide + deck + scheduled reviews, with a reviewable, bounded execution trace.

---

## Arc IV — Engagement & retention · P26–27

---

### Phase 26 — Focus System (Pomodoro, Focus Mode, Deep Work)

- **Goal:** Own the study session itself — timers, focus mode, deep-work logging feeding analytics.
- **Why it exists:** Cheap, high-frequency engagement surface that generates the session data everything else measures; pairs naturally with SRS reviews.
- **User value:** Start a focus timer, enter distraction-free mode, and have sessions auto-logged to the calendar/analytics/streak.
- **Technical scope:** focus-session UI + timer state; auto-create `StudySession` on completion (existing module); focus-mode layout (hide chrome); optional soundscape (static assets); ties into gamification (P27).
- **Files likely affected:** new `features/focus/*`; reuse `calendar`/`StudySessionService`; `AppLayout` focus-mode variant; `stores/focus.ts`; `api/modules/calendar.ts`.
- **Complexity:** M (3–5d).
- **Risks:** timer accuracy across tab-sleep; double-logged sessions; scope restraint (not a full habit tracker).
- **Verification:** run a Pomodoro, confirm a StudySession is logged once and appears in analytics/calendar; Playwright.
- **Dependencies:** existing calendar; P27 for streak credit (optional).
- **Acceptance criteria:** completing a focus timer logs a session that shows up in analytics and contributes to the streak.

---

### Phase 27 — Gamification (Missions, Streaks, XP, Achievements) + Learning Feed

- **Goal:** A retention layer of daily missions, streaks, XP/levels, achievements, and a personal learning feed.
- **Why it exists:** The dopamine loop that converts a signup into a daily habit — Duolingo's core mechanic — now that there are real activities (reviews, focus, plans) to reward.
- **User value:** Streaks, daily goals, XP for reviews/focus/notes, badges, and a feed of progress and nudges.
- **Technical scope:** event → XP/mission rules engine (consumes domain events from SRS/focus/notes via P17); streak rollover job (timezone-correct); achievements catalog; learning feed (activity + recommendations from P16); celebration motion (design-system 2.0 tokens).
- **Files likely affected:** new `gamification/` package, migration `V15__xp_streaks_achievements.sql`; `features/workspace/**` (streak/XP/missions widgets), new `features/feed/*`; `styles/motion.css` (celebrations); `api/modules/gamification.ts`.
- **Complexity:** L (5–8d).
- **Risks:** gaming the system (idempotent, event-sourced XP); streak-timezone correctness; notification fatigue; keeping it tasteful (Apple/Linear, not carnival).
- **Verification:** completing reviews grants XP once; streak increments/breaks correctly across tz; achievement unlocks fire; feed renders real events.
- **Dependencies:** P17 (events/jobs), P18/P26 (activities), P16 (feed recs).
- **Acceptance criteria:** a day's activity yields correct XP/streak/mission progress and a badge unlock, all idempotent.

---

## Arc V — Commercialization (turn habit into revenue) · P28–31

Foundations first (identity), then money, then multi-user, then admin. This ordering means billing and teams are assembly, not re-architecture.

---

### Phase 28 — Identity 2.0 (Registration, Email Verification, Password Reset, OAuth)

- **Goal:** Public self-service accounts using the reserved auth extension points.
- **Why it exists:** You cannot commercialize or open community features with an invite-only, no-reset auth system. This unblocks P29–P33.
- **User value:** Sign up, verify email, reset password, and log in with Google/GitHub.
- **Technical scope:** registration + email verification + password reset (uses P17 `EmailService`); OAuth2 login via additional `TokenService` issuance paths (the reserved seam); `app.security.password-policy`; account-state flows already scaffolded in Phase 2.
- **Files likely affected:** `auth/` (new endpoints/flows, no new error range — extends 100000 block), migration `V16__email_verification_oauth.sql`; `features/*` login/register/reset views (LoginView exists); `api/modules/auth.ts`; `docs/architecture.md` § Identity.
- **Complexity:** L (5–8d).
- **Risks:** account enumeration (keep the "always INVALID_CREDENTIALS" discipline); token/link expiry + reuse; OAuth callback security; email deliverability.
- **Verification:** full register→verify→login→reset cycle; OAuth round-trip; reuse/expiry rejected; `./mvnw test`.
- **Dependencies:** P17 (email).
- **Acceptance criteria:** a stranger self-registers, verifies, resets a password, and logs in via OAuth — all securely.

---

### Phase 29 — Billing, Subscriptions & AI Credits/Metering

- **Goal:** Charge money reliably, and meter/limit AI cost with a per-user credit ledger.
- **Why it exists:** No revenue exists until this ships — and every AI phase before it multiplies cost that must be metered. Credits protect gross margin.
- **User value:** Plans (Free/Pro/Team), checkout, invoices; transparent AI usage/credits; graceful limits instead of surprise bills.
- **Technical scope:** `billing` module + Stripe behind a `PaymentProvider` interface; subscription + webhook reconciliation (idempotent, P17); plan→feature/credit gating middleware; AI cost metering (token accounting per request in the `ai` package) + credit ledger; usage dashboard.
- **Files likely affected:** new `billing/` package, migration `V17__billing_credits.sql`; instrument `ai/` for metering; `features/settings/**` (billing/usage), pricing view; `api/modules/billing.ts`; `AppProperties` (`app.billing.*`, secrets via env).
- **Complexity:** XL (8–15d).
- **Risks:** payments correctness (Stripe as source of truth, idempotent webhooks, never store cards); tax/dunning; metering accuracy vs. streaming; feature-gate consistency across UI/API.
- **Verification:** test-mode subscribe/upgrade/cancel; webhook replay idempotency; AI call debits credits; gated feature blocks correctly at API and UI; `./mvnw test`.
- **Dependencies:** P28 (accounts), P17 (webhooks), all AI phases (things to meter).
- **Acceptance criteria:** a user subscribes in test mode, AI usage debits credits, exceeding a plan limit is handled gracefully, and webhook replays never double-charge.

---

### Phase 30 — Teams, Organizations & RBAC

- **Goal:** Activate the reserved `roles`/`permissions` tables into real orgs/teams with shared workspaces and seat billing.
- **Why it exists:** B2B/education seats are the highest-ACV, lowest-churn revenue; multi-tenancy is a data-model decision best made on the reserved seam, not retrofitted later.
- **User value:** Create an org, invite members with roles, share subjects/notes/decks, and manage seats.
- **Technical scope:** org/team model + membership + roles/permissions (fill `UserPrincipal` authorities, `@PreAuthorize` already enabled); resource-level authorization extending `OwnershipGuard` to org-scoped access; shared content visibility; seat-based billing (P29).
- **Files likely affected:** new `org/` package + `auth` RBAC wiring, migration `V18__orgs_membership_roles.sql`; extend `common/OwnershipGuard` → org-aware guard; many services gain org scope; `features/team/*`, org switcher in `AppLayout`; `router/guards.ts` (roles/permissions meta — already reserved).
- **Complexity:** XL (8–15d).
- **Risks:** **authorization is the top security risk** — every user-scoped query must become correctly org-scoped; migration of personal→org content; permission model creep.
- **Verification:** cross-org access is denied; role changes take effect; shared content visible only to members; seat count matches billing; exhaustive authz tests.
- **Dependencies:** P28, P29 (seats), P17.
- **Acceptance criteria:** an org owner invites a member, assigns a role, shares a subject, and access is correctly enforced at every endpoint.

---

### Phase 31 — Admin & Product Analytics Console

- **Goal:** Internal admin dashboard: user/org management, usage/revenue analytics, AI-cost monitoring, moderation, feature flags.
- **Why it exists:** You can't operate a paid, multi-user, community product blind; this is the operator's cockpit.
- **User value:** (Internal.) Support, moderation, and business visibility; enables safe rollout via flags.
- **Technical scope:** admin-role-gated module (RBAC from P30); product metrics (DAU/retention/conversion/AI cost) as read-model façades; moderation queue for community (P32); feature-flag service; audit log (the reserved seam) for sensitive actions.
- **Files likely affected:** new `admin/` package + `audit/` (activate reserved audit log), migration `V19__audit_feature_flags.sql`; `features/admin/*` (admin-only routes); `api/modules/admin.ts`.
- **Complexity:** L (5–8d).
- **Risks:** admin authz blast radius; PII exposure in admin views; audit-log completeness; metric correctness.
- **Verification:** admin-only access enforced; metrics reconcile with source tables; a moderation/flag action is audited; non-admin 403s.
- **Dependencies:** P30 (RBAC), P29 (revenue), P32 (moderation targets).
- **Acceptance criteria:** an admin views real usage/revenue, toggles a feature flag, moderates content, and every sensitive action is audited.

---

## Arc VI — Community & ecosystem (network effects) · P32–33

---

### Phase 32 — Community: Public Notes, Templates & Sharing

- **Goal:** Let users publish notes/decks/templates publicly, discover others' content, follow, and comment.
- **Why it exists:** Content becomes a distribution channel (SEO, virality) and a retention driver; a template library seeds new users fast.
- **User value:** Publish a study guide/deck, browse a public library, clone a template into your workspace, follow creators.
- **Technical scope:** publish/visibility model on notes/decks; public read pages (SSR/SEO-friendly, or prerendered); discovery feed + search (P16/P21); clone-to-workspace; comments + follows; moderation hooks (P31).
- **Files likely affected:** `note`/`flashcard` (visibility + publish endpoints), new `community/` package, migration `V20__publishing_comments_follows.sql`; new `features/community/*` + public route tree; `api/modules/community.ts`.
- **Complexity:** L (5–8d).
- **Risks:** moderation/abuse/spam; SEO/public-page architecture (currently SPA — may need prerender); copyright; privacy leaks from over-sharing.
- **Verification:** publish→appears publicly→clone works; comment/follow flows; private content never leaks; moderation removes content.
- **Dependencies:** P19 (real notes), P28 (public identities), P31 (moderation), P16 (discovery).
- **Acceptance criteria:** a user publishes a template, another user discovers and clones it, and moderation can remove it.

---

### Phase 33 — Marketplace, Plugin System & Partner Showcase

- **Goal:** A marketplace for premium templates/tools, a plugin seam for extensibility, and a tasteful partner/sponsor showcase.
- **Why it exists:** Extends the platform into an ecosystem and adds monetization surfaces beyond subscriptions — done in the Apple/Linear register, never spammy.
- **User value:** Discover/buy premium learning packs and integrations; creators earn; curated partners surfaced elegantly (opt-in, integrated, never intrusive).
- **Technical scope:** marketplace listings + purchase (reuses P29 payments, revenue share); a plugin/integration interface (webhooks/embeds behind a stable contract); a curated "featured" placement system with strict design constraints (limited promo cards, event banners) governed by tokens and admin (P31).
- **Files likely affected:** new `marketplace/` package + `billing` reuse, migration `V21__marketplace_plugins.sql`; `features/marketplace/*`; `api/modules/marketplace.ts`; design-system placements in `styles/tokens.css`.
- **Complexity:** L (5–8d).
- **Risks:** plugin security/sandboxing (start with vetted, server-side integrations only); ad taste/UX debt (hard design guardrails, opt-out); revenue-share accounting.
- **Verification:** purchase a listing (test mode), install a template, render a sponsor card within design constraints; non-intrusiveness review.
- **Dependencies:** P29 (payments), P32 (creator content), P31 (curation/admin).
- **Acceptance criteria:** a creator lists a pack, a user buys/installs it, and a curated partner card appears tastefully and dismissibly.

---

## Arc VII — Platform reach · P34

---

### Phase 34 — Mobile, PWA, Offline & Browser Extension

- **Goal:** Meet daily-habit users everywhere: responsive/mobile-first pass, installable PWA, offline review, push notifications, and a browser clipper.
- **Why it exists:** Reviews/focus/reading are daily, on-the-go habits; a PWA + offline review is the mobile MVP without a native app; the clipper feeds the knowledge base (Readwise/Arc).
- **User value:** Install to home screen, review flashcards offline, get push reminders, clip web content into your workspace.
- **Technical scope:** responsive audit across all `features/*`; service worker + PWA manifest (new build tooling); offline cache + sync for the review queue and notes (conflict policy); web push (P17 notifications); a browser-extension clipper posting to P15 storage/P16 ingestion.
- **Files likely affected:** `ai-learning-web` build config (PWA plugin), `AppLayout`/responsive tokens; offline sync layer in `api/http.ts` + Pinia; new `extension/` workspace; `docs/design-system.md` (responsive).
- **Complexity:** L (5–8d).
- **Risks:** offline sync conflicts (keep scope to reviews/notes, last-write-wins + clear rules); PWA platform quirks; push permissions/deliverability; extension store review.
- **Verification:** install PWA, review offline, sync on reconnect, receive a push; clipper saves a page; Lighthouse PWA pass.
- **Dependencies:** P18 (offline reviews), P17 (push), P15/P16 (clipper ingest).
- **Acceptance criteria:** a user installs the PWA, completes reviews offline, syncs cleanly, and receives a study reminder push.

---

## Version 1.0 — Phase 35: Hardening & Launch

- **Goal:** Production-grade reliability, performance, security, and polish — the GA gate.
- **Why it exists:** Everything above is features; v1.0 is trust. Ship nothing new; make what exists bulletproof.
- **User value:** A fast, reliable, secure product that's ready for real paying users at scale.
- **Technical scope:** observability (metrics/tracing/error monitoring — actuator already present); performance + the reserved **Redis** cache where measured (read-model caching, rate-limit backing); load/scale testing; full security review (authz sweep, dependency audit, secrets); i18n completeness pass; empty/error-state audit; accessibility; docs/onboarding; Docker/CI-CD hardening (the `docker/` dir exists).
- **Files likely affected:** `config/` (observability, cache), `docker/` + CI; cross-cutting hardening across packages; `docs/*` (ops runbook, launch checklist).
- **Complexity:** XL (8–15d).
- **Risks:** scope discipline (resist new features); finding deep issues late (do incremental hardening earlier too); cache invalidation correctness.
- **Verification:** load test to target concurrency; security review sign-off; SLO dashboards green; full e2e regression via the `verify` skill.
- **Dependencies:** all prior arcs.
- **Acceptance criteria:** the platform meets defined SLOs under load, passes a security review, and clears a launch checklist — declared Version 1.0.

---

## Sequencing rationale (one paragraph)

Infrastructure (15–17) is first because storage, retrieval, and async are *shared dependencies* — building them once prevents every later phase from inventing a parallel system (the "no duplicated systems" mandate). Depth phases (18–21) turn already-shipped-but-thin modules into competitive products using seams reserved since Phase 5, giving fast, visible wins on low architectural risk. AI depth (22–25) sits on the retrieval/memory foundation so agents and PDF-chat are assembly, not re-architecture. Engagement (26–27) comes only after there are real activities (reviews, focus, plans) to reward — gamifying an empty product is hollow. Commercialization (28–31) is deliberately ordered identity → billing → teams → admin so each is built on a finished dependency, and multi-tenant authorization (30) lands on the reserved RBAC seam rather than being retrofitted after community exists. Community/marketplace (32–33) needs real notes, public identities, and moderation first. Reach (34) waits until the daily-habit core is stable so the mobile surface is worth installing. v1.0 (35) is trust, not features.

## MVP vs. Version 2.0 vs. v1.0-GA

- **Commercial MVP (fastest path to revenue):** P15–18 (infra + SRS), P21 (AI search), P28–29 (accounts + billing/credits), P34-lite (responsive/PWA). This is a chargeable, sticky product.
- **Core v1.0 (this roadmap's target):** all of 15–35.
- **Version 2.0 / post-v1.0 (explicitly deferred — only if they fit):** AI Voice tutor, AI Video summary, real-time collaborative editing (CRDT), whiteboard/canvas, native desktop app, enterprise edition/SSO/SCIM, OCR for scanned docs. Named here so they aren't smuggled into earlier phases.

## Cross-cutting risks to hold across all phases

- **AI cost** — meter early (P29 gates the expensive P22–25); never ship unmetered AI to paid scale.
- **Vector store choice (P16)** — interface-first; avoid premature Elasticsearch/managed vector DB lock-in; plan for re-embedding.
- **Authorization (P30)** — the retrofit from user-scoped to org-scoped is the single highest-risk security change; test exhaustively.
- **Payments (P29)** — Stripe as source of truth, idempotent webhooks, never store cards.
- **Scalability** — the monolith is fine; make AI/jobs async (P17) and cache read-models (P35) rather than prematurely splitting services.
- **Editor & PWA new dependencies (P19/P34)** — the two places heavy deps enter; justify against the constitution's "no speculative dependencies."

## Should any previous phase be revisited before Phase 15?

No full revisit. Two lightweight, non-cosmetic confirmations, folded *into* early phases rather than done as standalone rework:
1. **Auth schema multi-tenant readiness** — before P30, confirm the reserved `roles`/`permissions` + user model can carry org boundaries without a destructive migration (a review, not a rebuild).
2. **AI context pipeline seam** — P16 extends `LearningContextService` for retrieval; confirm it stays provider-agnostic. Both are already designed as extension points, so this is verification, not redesign.

The landing/login/product-presentation and the design system are treated as **complete** and are not reopened except where a phase legitimately extends the design system (P19 editor tokens, P27 celebration motion, P33 placement tokens, P34 responsive).
