# Architecture & Engineering Constitution

Decisions recorded here are binding until explicitly revised. When adding code,
match these conventions — do not invent parallel ones.

## Product positioning

An **AI-native learning workspace** (not an educational admin system). Commercial
SaaS quality is the bar. UI language: modern, premium, minimal — in the spirit of
Apple, Linear, Notion, Raycast, Vercel, Stripe.

## Confirmed platform decisions (2026-07)

| Topic | Decision |
| --- | --- |
| Java | 22 — use modern language features (records, pattern matching, virtual threads where useful) |
| Repository | Single monorepo: `github.com/YukaKaori/AI-Learning-Platform` |
| Migrations | Flyway only; production schema is never changed manually |
| i18n | i18n-ready from day one; default `zh-CN`, fallback `en-US`; no hard-coded UI text |
| Frameworks | Spring Boot 4 / Vue 3 + TS + Vite / Pinia / Element Plus (themed) / MyBatis-Plus / MySQL |

## Backend

**Style: modular monolith, package-by-feature.** Each feature owns
`controller / service / mapper / entity / dto` under its package. Cross-cutting
code lives in `common/`; framework wiring in `config/`.

### Conventions

- **API prefix**: `/api/v1/...`. Controllers return `ApiResponse<T>`; DTOs are records.
- **Envelope**: `{ code, message, data, timestamp }`; `code = 0` means success.
  HTTP status still carries transport semantics (400/401/404/500…).
- **Errors**: services throw `BusinessException(ErrorCode)`. Only
  `GlobalExceptionHandler` builds error responses. Error-code ranges:
  40000–49999 common client, 50000–59999 common server, then 10000 per feature
  module starting at 100000 (auth: 100000–109999, …).
- **Entities** extend `BaseEntity` (snowflake id, `created_at`, `updated_at`,
  `deleted`); audit fields are filled automatically. Entities never cross the API
  boundary — map to DTOs.
- **Validation**: Bean Validation annotations on request DTOs; no manual checks in
  controllers.
- **Configuration**: YAML per profile (`dev` default, `prod`); all custom settings
  under `app.*` bound via `AppProperties`. Secrets only from environment variables.

### External-service abstraction (mandatory)

Business code never talks to a vendor SDK directly. Define the interface in
`infrastructure/` when the capability is first needed:

- `StorageService` → Aliyun OSS / MinIO / S3 / local
- `AiService` → Claude / OpenAI / Gemini / DeepSeek / local models
  (provider implementations + configuration layer + conversation management)
- `NotificationService`, cache, search, MQ — same pattern.

Reserved (do not implement early): Redis, OSS, WebSocket, Elasticsearch, MQ,
scheduler, audit log.

## Frontend

**Structure**: `api/` (axios + typed endpoint modules) · `components/` (design-system)
· `composables/` · `layouts/` · `locales/` · `router/` · `stores/` · `styles/` ·
`views/` (grow into `features/` when modules appear).

### Conventions

- **Design tokens first**: every color/spacing/radius/shadow comes from
  `styles/tokens.css`. Element Plus is bridged to the tokens in
  `styles/element-theme.css` — never style against EP defaults.
- **Dark mode**: `html.dark` class, three-way preference (light/dark/system) in the
  app store.
- **i18n**: all user-visible text through vue-i18n keys; `en-US` must mirror
  `zh-CN` key-for-key (enforced by unit test).
- **HTTP**: all requests go through `api/http.ts` helpers, which unwrap the
  envelope and normalize failures to `ApiError` (with i18n message key).
- **Element Plus**: on-demand via unplugin resolvers; prefer custom token-based
  components for signature surfaces, EP for complex primitives (tables, pickers).

## Database

See `database/README.md`: snake_case, utf8mb4, mandatory audit columns, logical
foreign keys, migration-only changes.

## Roadmap

1. **Phase 1 — Foundation** (this phase): plumbing, standards, design system, zero business features.
2. **Phase 2 — Identity**: Spring Security 7 + JWT (access/refresh), user schema (V1 migration), frontend auth flow + route guards.
3. **Phase 3 — Domain design, then core workspace**: written domain model first, then the non-AI workspace skeleton.
4. **Phase 4 — AI integration**: `AiService` abstraction, SSE streaming chat, Redis, WebSocket where truly bidirectional.
5. **Phase 5 — Production**: Docker, CI/CD, observability, security hardening, OSS storage.

## Git

- Trunk: `main`. Feature branches `feature/<topic>`, fixes `fix/<topic>`.
- Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`).
- Never commit secrets; `.env*.local` are ignored.
