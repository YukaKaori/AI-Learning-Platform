# AI Learning Platform

A modern, AI-native learning workspace — built as a commercial-quality SaaS
application, not a course project. Every account is fully isolated (own
subjects, materials, notes, flashcards, tasks, sessions, AI conversations,
and preferences) and every product surface is backed by real per-user data —
there is no shared seed content and no mock/fixture data anywhere in the
running app.

## Product surface

| Module | What it does |
| --- | --- |
| **Workspace** | The daily landing dashboard — streak, study-progress ring vs. your real daily goal, due flashcards, continue-learning rail, upcoming tasks, today's sessions, recent notes/AI chats. |
| **Subjects** | The organizing anchor for everything you study — materials (links today, file upload reserved), progress, and every note/task/session that references it. |
| **Notes** | Markdown notes, optionally linked to a subject, with an AI toolbar (explain/rewrite/continue/simplify/expand/translate/summarize/generate-flashcards). |
| **Flashcards** | Decks and cards, including AI-generated decks from a note, subject, or chat. |
| **AI Tutor** | Streaming chat (DeepSeek) with real conversation history and optional subject context, so the tutor answers using your actual materials/notes. |
| **Tasks & Calendar** | Study sessions and due-dated tasks in one week/month view; tasks are also quick-addable from Workspace. |
| **Analytics** | Real weekly activity, subject time distribution, and a study heatmap — server-aggregated from your own data, never fabricated numbers. |
| **Profile & Settings** | Real account stats and member-since date; theme (light/dark/system)/locale/daily-goal preferences persist server-side and sync across devices. |

Full domain model and module responsibilities: `docs/product-domain.md`.
Engineering constitution: `docs/architecture.md`. AI implementation detail:
`docs/ai-engine.md`.

## Repository layout

```
ai-learning-platform/
├── ai-learning-web/       # Frontend · Vue 3 + TypeScript + Vite + Pinia + Element Plus
├── ai-learning-server/    # Backend  · Spring Boot 4 + MyBatis-Plus + MySQL + Flyway
├── docs/                  # Architecture decisions and design documents
├── database/              # Schema design docs (migrations live in the server module)
├── docker/                # Containerization (not yet built — see docker/README.md)
└── README.md
```

## Prerequisites

- JDK 22
- Node.js ≥ 22.18
- MySQL 8+ (local database `ai_learning`)

## Quick start

Backend (starts on <http://localhost:8080>):

```bash
cd ai-learning-server
# dev profile is the default; override credentials via DB_USERNAME / DB_PASSWORD
./mvnw spring-boot:run
```

Frontend (starts on <http://localhost:5173>, proxies `/api` to the backend):

```bash
cd ai-learning-web
npm install
npm run dev
```

Create the dev database once:

```sql
CREATE DATABASE IF NOT EXISTS ai_learning
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## Authentication

Stateless JWT authentication (Phase 2). Sign in at `/login`; on the dev profile a
seed account is created automatically: **`demo` / `Demo123456`**.

| Endpoint | Purpose |
| --- | --- |
| `POST /api/v1/auth/login` | Username **or** email + password → access + refresh token |
| `POST /api/v1/auth/refresh` | Rotate the refresh token, get a new pair |
| `POST /api/v1/auth/logout` | Revoke the refresh token |
| `GET /api/v1/auth/me` | Current user (requires `Authorization: Bearer <token>`) |

Environment variables (prod requires all; dev has local defaults):

| Variable | Purpose |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Database connection |
| `JWT_SECRET` | HMAC-SHA256 signing key, **at least 32 bytes** |

The full security architecture (token lifecycles, rotation, error codes, sequence
diagram) is documented in [`docs/architecture.md`](docs/architecture.md).

## Verification

| Check | Command |
| --- | --- |
| Backend tests | `./mvnw test` (H2 in-memory, no MySQL needed) |
| Frontend lint | `npm run lint` |
| Frontend type-check + build | `npm run build` |
| Frontend unit tests | `npm run test:unit` |

## Engineering standards

Architecture decisions, module conventions, error-code ranges and the development
roadmap are documented in [`docs/architecture.md`](docs/architecture.md). Read it
before adding a module.

Database changes are **migration-only** via Flyway
(`ai-learning-server/src/main/resources/db/migration`) — see
[`database/README.md`](database/README.md).
