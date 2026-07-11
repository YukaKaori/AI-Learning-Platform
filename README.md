# AI Learning Platform

A modern, AI-native learning workspace — built as a commercial-quality SaaS application.

## Repository layout

```
ai-learning-platform/
├── ai-learning-web/       # Frontend · Vue 3 + TypeScript + Vite + Pinia + Element Plus
├── ai-learning-server/    # Backend  · Spring Boot 4 + MyBatis-Plus + MySQL + Flyway
├── docs/                  # Architecture decisions and design documents
├── database/              # Schema design docs (migrations live in the server module)
├── docker/                # Containerization (Phase 5)
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
