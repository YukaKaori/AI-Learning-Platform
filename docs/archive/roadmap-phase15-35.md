# AI Learning Platform — Master Roadmap (Phase 15 → 35)

> **Status:** Strategic planning only. No implementation has started. Awaiting approval before Phase 15.
> **Author perspective:** CTO / Product Lead of a commercial AI-native SaaS.
> **Date:** 2026-07-20

## Framing

Through Phase 14 the project is a **beautiful product surface with a thin engine behind it** — premium landing, glass design language, AI engine, tutor/notes/flashcards/knowledge-graph, auth, workspace, analytics, settings. That is the startup inflection point: the storefront is done; the business isn't.

This roadmap plans the transition across three things a real SaaS must earn, in order:

1. **Infrastructure that makes AI features real** — memory, retrieval, cost control, permissions. Today's AI is stateless and unmetered, which caps every future feature.
2. **A retention loop that survives without novelty** — adaptive review, missions, analytics, mentor. Turns a signup into a daily habit.
3. **Monetization and multi-user reality** — billing, teams, community, mobile. Turns a habit into revenue.

**Complexity scale:** `M` ≈ 3–5 dev-days · `L` ≈ 5–8 dev-days · `XL` ≈ 8–15 dev-days.

---

## 1. Master Roadmap

### Arc A — Foundation Hardening (make the AI real) · Phases 15–18

| # | Title | Objective | Major features | Depends on | Cx |
|---|-------|-----------|----------------|-----------|----|
| **15** | **AI Memory & Retrieval Foundation (RAG)** | Give the AI durable, grounded knowledge of each user's own content. | Vector DB (pgvector/Qdrant); ingestion pipeline for notes/flashcards/knowledge-graph; embeddings + semantic retrieval; per-user "AI Memory" store (facts, preferences, progress); retrieval-augmented tutor answers with citations. | Existing AI engine, notes, knowledge graph | XL |
| **16** | **AI Cost Metering & Credits Ledger** | Never ship another AI feature that can't be measured or priced. | Token/cost accounting per request; per-user credit ledger; rate limits & quotas; model routing (cheap vs premium per task); usage dashboard; graceful "out of credits" states. | Phase 15, AI engine | L |
| **17** | **Platform Hardening: Permissions, Multi-tenancy & Observability** | Turn single-user assumptions into a multi-tenant, auditable system. | Role/permission model (owner/member/viewer/admin); resource-level authorization; org/tenant boundaries in data model; audit log; structured logging, tracing, error monitoring; background job queue. | Auth, workspace | XL |
| **18** | **Design System 2.0 (Glass 2.0 + Motion + DataViz)** | Give every future dashboard, chart, and AI surface one visual language before we build 15 of them. | Glass 2.0 tokens/components; motion system (spring primitives, orchestration); data-viz component library (charts, meters, KPI tiles) with light/dark parity; empty states; AI "thinking/streaming" animation kit. | Existing glass.css | L |

### Arc B — The Retention Loop (make it a daily habit) · Phases 19–23

| # | Title | Objective | Major features | Depends on | Cx |
|---|-------|-----------|----------------|-----------|----|
| **19** | **AI Study Planner & Learning Path Generator** | Convert a vague goal into a concrete, adaptive schedule. | Goal intake → generated learning path; milestone/module breakdown; calendar-integrated scheduling; path adapts to progress; "what should I do today" surface. | Phase 15, calendar, subjects | L |
| **20** | **Adaptive Review Engine (Spaced Repetition)** | Make knowledge *stick* — the core learning-science moat. | FSRS/SM-2 scheduling over flashcards & notes; daily review queue; AI-generated review items from notes; difficulty adaptation; recall analytics. | Flashcards, notes, Phase 15 | L |
| **21** | **Engagement Loop: Missions, Streaks, XP & Achievements** | The dopamine layer that brings users back tomorrow. | Daily missions & weekly goals; streak system w/ freezes; XP + levels; achievements/badges; progress celebrations (uses Phase 18 motion kit). | Phase 20, Phase 18 | M |
| **22** | **Learning Analytics & Personalized Recommendations** | Show progress and tell users what to do next. | Learning analytics dashboard (time, retention, mastery per subject); mastery/knowledge-state model; personalized "next best action"; weekly recap. | Phases 18, 20 | L |
| **23** | **AI Mentor / Coach (Longitudinal & Proactive)** | An AI that remembers your journey and reaches out, not just responds. | Persistent mentor persona reading AI Memory + analytics; proactive nudges; weekly check-ins; goal accountability; tone/mentoring settings. | Phases 15, 19, 22 | L |

### Arc C — AI Workspace Depth (the Cursor/Notion-AI layer) · Phases 24–29

| # | Title | Objective | Major features | Depends on | Cx |
|---|-------|-----------|----------------|-----------|----|
| **24** | **Unified AI Workspace** | One home for all AI interaction: multiple conversations, projects, reusable prompts. | Multi-conversation threads w/ history & search; AI Projects (scoped context/files); Prompt Library (save/share/version); pinned context; per-project memory. | Phases 15, 16, 17 | XL |
| **25** | **Specialized AI Tutors: Coding + Writing** | Depth in the two highest-value verticals. | AI Coding Tutor (code exec/sandbox, step-through, hints not answers); AI Writing Assistant (drafting, feedback, rubric grading); mode-specific UIs. | Phases 15, 24 | L |
| **26** | **AI Exam Simulator & Interview Trainer** | Turn learning into measurable outcomes users pay for. | Exam generation from user material; timed simulation; scoring + gap analysis; interview trainer (behavioral/technical) with feedback; results feed analytics. | Phases 15, 20, 22 | L |
| **27** | **AI Research Assistant & Knowledge Inbox** | Capture-anywhere → synthesize. | Knowledge Inbox (save links/PDFs/clips); AI summarization & auto-tagging into knowledge graph; research assistant Q&A across saved sources w/ citations; bookmarking. | Phases 15, 24 | L |
| **28** | **AI Voice Tutor** | Hands-free, conversational learning. | Realtime STT/TTS; voice conversations with tutor/mentor; pronunciation & spoken-recall drills; voice notes → transcribed knowledge. | Phases 15, 23 | M |
| **29** | **Focus & Productivity Suite** | Own the study session itself. | Pomodoro; Focus/Deep Work mode; Reading Mode; distraction shielding; session logging into analytics. | Phases 18, 22 | M |

### Arc D — Commercialization (turn habit into revenue) · Phases 30–32

| # | Title | Objective | Major features | Depends on | Cx |
|---|-------|-----------|----------------|-----------|----|
| **30** | **Pricing, Subscriptions & Billing** | Charge money, reliably and compliantly. | Pricing/plans page; Stripe subscriptions + checkout; plan → feature/credit gating; invoices, receipts, tax; upgrade/downgrade/dunning; free→paid conversion surfaces. | Phase 16 | XL |
| **31** | **Teams, Organizations & Admin Dashboard** | Sell to groups, not just individuals. | Org creation, member invites, seat billing; team workspaces; admin dashboard (members, usage, spend); org-level policies. | Phases 17, 30 | L |
| **32** | **Growth: Referral, Partner & Sponsor Showcase** | Lower CAC and add a tasteful, premium promotional surface. | Referral program (credits for invites); featured AI tools / educational partner showcase; limited promotional & event cards — all in the Apple/Linear design language, opt-in and integrated, never spammy. | Phases 18, 30 | M |

### Arc E — Community & Collaboration (network effects) · Phases 33–34

| # | Title | Objective | Major features | Depends on | Cx |
|---|-------|-----------|----------------|-----------|----|
| **33** | **Community: Public Notes, Knowledge Sharing & Smart Search** | Let great content become a distribution channel. | Publish notes/decks publicly; discovery feed; global smart search (semantic, across own + public); follow/save; SEO-friendly public pages. | Phases 15, 17 | L |
| **34** | **Collaboration: Study Groups, Classrooms & Discussions** | Multiplayer learning — the deepest retention driver. | Study groups & shared workspaces; classrooms (teacher/student roles); comments, threads, discussions; shared review sessions; realtime presence. | Phases 17, 31, 33 | XL |

### Arc F — Platform Reach · Phase 35

| # | Title | Objective | Major features | Depends on | Cx |
|---|-------|-----------|----------------|-----------|----|
| **35** | **Mobile & Platform: PWA, Offline & Push** | Meet daily-habit users where they are. | Responsive/mobile-first pass; installable PWA; offline mode (review queue, notes) w/ sync; push notifications for missions/streaks/mentor nudges. | Phases 20, 21, 23 | L |

---

## 2. Why this ordering is optimal

- **Infrastructure before features (Arc A first).** Memory/RAG (15), metering (16), permissions (17) are load-bearing walls. Building the workspace/mentor/exam simulator on a stateless, unmetered, single-tenant engine means rebuilding all of them later.
- **Metering before expensive AI (16 early).** Phases 23–28 multiply AI spend. Without the credits ledger you cannot control gross margin or price the product.
- **Retention before monetization (Arc B before D).** Charging for a product users don't return to just accelerates churn. The habit loop (19–23) must exist before the paywall (30).
- **Design System 2.0 mid-Arc-A (18).** Phases 22, 26, 29, 31 are dashboard-heavy. Define the chart/motion/empty-state language once — after infra churn settles the data shapes it must visualize.
- **Permissions/orgs early (17), teams/community late (31, 33, 34).** Multi-tenancy is a data-model decision — cheap early, catastrophic to retrofit. User-facing team/community features can wait.
- **Mobile last (35).** A PWA/offline layer over a moving feature set is wasted work. Ship it once the core loop is stable.

**Arc order in one line:** *make the AI real → make it a habit → make it deep → make it pay → make it social → make it portable.*

---

## 3. Phases with the biggest commercial value

1. **Phase 30 — Pricing & Billing.** No revenue exists until this ships (but only worth it after Arc B).
2. **Phase 20 — Adaptive Review Engine.** The learning-science moat and #1 daily-habit driver. Makes it "Duolingo-sticky" vs "another chatbot."
3. **Phase 24 — Unified AI Workspace.** The premium ceiling; justifies the top tier; differentiates from generic ChatGPT.
4. **Phase 21 — Engagement Loop.** Cheap (M), disproportionately lifts DAU/retention.
5. **Phase 31 — Teams & Orgs.** Unlocks B2B/education seat expansion — highest ACV, lowest churn.
6. **Phase 34 — Collaboration/Classrooms.** Network effects + institutional lock-in; strongest long-term defensibility, most expensive/latest.

---

## 4. MVP vs. Version 2.0

**MVP — "commercial launch" (Phases 15–23, 30, 35).**
Smallest set that is a real, chargeable, sticky product: grounded AI (15) + cost control (16) on hardened infra (17), a coherent design system (18), the full retention loop (19–23), billing (30), and a mobile/PWA reach layer (35).

> Pragmatic sequencing: build 15–23 in order, then **jump to 30** for monetization, then **35** for reach, before returning to V2 depth phases. Billing shouldn't wait behind six AI-depth phases.

**Version 2.0 — "platform & moat" (Phases 24–29, 31–34).**
AI Workspace depth (24–29), team/org expansion (31), growth surfaces (32), community/collaboration (33–34). Deepen value and defensibility but not required to charge money or retain the first cohort.

---

## 5. Future technical risks

| Area | Risk | Mitigation (owning phase) |
|------|------|---------------------------|
| **AI cost** | Unmetered AI spend destroys gross margin; power users can 100× costs. | Metering + model routing + quotas — **Phase 16**, before any cost-heavy phase. |
| **Vector DB / search** | Wrong choice doesn't scale; re-embedding is expensive. | Start with **pgvector** in **Phase 15**; abstract retrieval so Qdrant/pinecone is a swap. |
| **Permissions** | Retrofitting authz = security holes + rework. | Resource-level authz in **Phase 17**, before multi-user features. |
| **Payments** | PCI/tax/dunning/webhook reconciliation are hard; double-charging kills trust. | Stripe as source of truth, idempotent webhooks, ledger reconciliation — **Phase 30**. Never store card data. |
| **Scalability** | Spring Boot monolith + synchronous AI calls block threads under load. | Background job queue + async AI (**17**); stream responses; cache aggressively. |
| **Caching** | Repeated embeddings/LLM calls waste money and add latency. | Semantic + response caching and embedding dedup — from **16** onward. |
| **Database** | Multi-tenant growth (notes, embeddings, events, chat) outpaces single MySQL. | Partition/index strategy + separate analytics/event store; move vectors to purpose-built store as volume grows. Locked in **17**. |
| **Realtime (voice/collab)** | WebSockets/WebRTC (28, 34) don't fit request/response monolith. | Isolate realtime into a dedicated service/gateway. |
| **AI reliability** | Hallucinations/outages erode trust in a learning product. | Grounding + citations (**15**), eval harness, provider fallback/routing (**16**). |
| **Vendor lock-in** | Hard-coding one model/provider caps flexibility and pricing leverage. | Model-router abstraction in **16** so provider/model is config, not code. |

---

## 6. Should any previous phase be revisited first?

Mostly no — design/landing/product-site work (Phases 8–14) is finished and should not be reopened. Two targeted, non-cosmetic exceptions, to be handled *inside* Phases 15–17 rather than as separate rework:

1. **AI Engine (Phase 6) needs a memory/retrieval seam.** Built stateless. Phase 15 should wrap it with a retrieval + memory layer and a model-router rather than rebuild it. Everything in Arcs B/C assumes a stateful engine.
2. **Auth & data model (Phase 7) must be confirmed multi-tenant-ready.** Before Phase 17 builds permissions on top, verify the schema can carry org/tenant boundaries without a destructive migration. A review, not a rebuild — cheaper before 17 than after 31/34.

Everything else (glass design language, product website, tutor/notes/flashcards/graph UIs) is a solid base and should be left alone.

---

## Open decisions before starting Phase 15

- Confirm the MVP cut (§4): 15–23 → 30 → 35 as the launch set?
- Vector DB choice: start with pgvector (recommended) or a dedicated store?
- Optional: render this roadmap as a visual timeline artifact for review.
