---
name: verify
description: Build, launch and drive the AI Learning Platform (Spring Boot :8080 + Vite :5173) to verify changes at the real surface — API via curl, UI via Playwright from the npx cache.
---

# Verifying the AI Learning Platform

## Launch

- Backend: `cd ai-learning-server && ./mvnw -q spring-boot:run` (JDK 22, MySQL 9.1 running locally, DB `ai_learning`). Ready when `curl http://localhost:8080/api/v1/system/info` returns 200.
- Frontend: `cd ai-learning-web && npm run dev` → http://localhost:5173.
- Ports often stay held after stopping tasks: `netstat -ano | grep -E ':(8080|5173).*LISTEN'` then `taskkill //PID <pid> //F` (Git Bash).

## API surface

- Login: `POST /api/v1/auth/login` with `{"usernameOrEmail":"demo","password":"Demo123456"}` (field is `usernameOrEmail`, not `username`) → `data.accessToken`, then `Authorization: Bearer`.
- Responses are the `{code, message, data, timestamp}` envelope; `code: 0` = OK.
- Avoid non-ASCII in Git-Bash curl `-d` payloads (UTF-8 mangling → spurious 500).

## Browser surface (Playwright, no local install needed)

Playwright 1.61 + Chromium live in the npx cache. Run scripts with:

```bash
NODE_PATH="C:/Users/10529/AppData/Local/npm-cache/_npx/361ceb562f3b3235/node_modules" node script.cjs
```

(If that hash is gone, `find "$(npm config get cache)/_npx" -maxdepth 4 -name playwright -type d`.)

- Login page: fill `input:not([type="password"])` with `demo`, `input[type="password"]` with `Demo123456`, press Enter, wait for URL to leave `/login`. Default locale is zh-CN (retry button says 重试).
- Force the D3 error state with `page.route('**/v1/<endpoint>', r => r.abort())` + reload; `unroute` before clicking retry.
- Gotcha: in list rows (e.g. `.conv-row`), the row item itself is a `<button>` — action buttons by index are off by one (nth(2) is archive, not delete).

## Data hygiene

Dev DB is shared and NOT clean: two Phase 6 notes (未命名笔记, Verify Note) and one conversation (Hello, explain recursion) are deliberately kept. Delete anything you create (UI or `DELETE` via API) and re-verify the list afterwards. AI streaming without `DEEPSEEK_API_KEY` yields the graceful error reply (抱歉，这次没能获得回复，请重试。) — the send/persist path still exercises fully.
