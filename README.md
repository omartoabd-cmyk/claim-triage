# Claim Triage Center

Spring Boot 4 (Java 17) API + React/Vite UI. Users submit insurance claims; an
OpenAI model produces an advisory triage (summary, category, missing info,
risk, next steps) that is saved with the claim.

## Run

```bash
# 1. Backend  (never put the key in a file that is committed)
export OPENAI_API_KEY=sk-...          # PowerShell: $env:OPENAI_API_KEY="sk-..."
./mvnw spring-boot:run                # http://localhost:8080

# 2. Frontend
cd frontend && npm install && npm run dev   # http://localhost:5173 (proxies /api to :8080)
```

Claims and analyses are stored in `./data/claims` (file-based H2).

## API

| Method | Path | Notes |
|---|---|---|
| GET | `/api/claims` | newest first, includes saved analysis |
| GET | `/api/claims/{id}` | 404 if missing |
| POST | `/api/claims` | validated; 400 `fieldErrors`, 409 duplicate claim number |
| PATCH | `/api/claims/{id}/status` | `SUBMITTED, IN_REVIEW, APPROVED, DENIED` |
| POST | `/api/claims/{id}/analyze` | returns saved analysis; `?refresh=true` re-runs the AI. 429 when rate-limited, 503 if AI fails |

Errors are always `{ "status": n, "message": "...", "fieldErrors": {...} }`.

## Config (`application.properties`)

`openai.api.key` (from `OPENAI_API_KEY`), `openai.model`, `app.cors.allowed-origins`,
`app.rate-limit.per-client-per-minute`, `app.rate-limit.global-per-hour`.

## Not included (needs a decision)

Authentication/authorization (e.g. Spring Security) is not implemented: anyone
who can reach the API can read and change claims. Rate limiting only caps AI
cost. Add auth before exposing this beyond localhost.
