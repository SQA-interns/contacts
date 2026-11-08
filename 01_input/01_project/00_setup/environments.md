# Environments

> For: agent (reads), human (reviews) · Owner: Architect · Agent: read-only

Source: Q-49, Q-52. The customer decides where production runs and operates it; the product must therefore be deployable in cloud or on own infrastructure, with all configuration through environment variables and nothing tied to one provider.

## Environments

| Environment | Purpose | How it runs |
|---|---|---|
| test | Automated tests of the backend, the frontend and the end-to-end scenarios; no internet and no real accounts (Q-49). | Backend tests use Testcontainers with a throwaway PostgreSQL container; sign-in is the Firebase Local Emulator Suite (Authentication emulator); frontend unit tests run in jsdom with mocked API; the E2E tests run Playwright against the Compose stack of the local environment with the emulator and an empty database. No TLS. |
| local | Development and manual checks by the team (Q-52: the team's internal server or a local Docker setup). | `02_output/docker-compose.yml` starts PostgreSQL, the backend and the frontend; the Firebase emulator is started with it for development, or the one real development Firebase project is used for manual checks (Q-49). HTTP only on `localhost`. |
| production | Used by the customer. The customer decides and operates it (Q-52). | The same container images and a Compose setup with PostgreSQL, started with the customer's environment variables. TLS ends at the customer's reverse proxy or load balancer (HTTPS only, US-018 R-018-12). Nobody is on call. Host, domain and budget are the customer's concern and unknown. |

Persistent data: the PostgreSQL data directory lives in a named Docker volume (local and production) and survives container recreation; the test environment's database is discarded after each run. Backups: a daily database dump kept 14 days, run by the operator as described in the install and backup guide (Q-50).

## Configuration

Settings that differ between environments (ES-01). Secrets are listed in `project/00_setup/secrets.env.example`, not here.

| Setting | Environments | Default allowed? |
|---|---|---|
| `DB_URL` (JDBC address of PostgreSQL) | test, local, production | No: set per environment |
| `FIREBASE_PROJECT_ID` | local, production (test uses a fixed fake id for the emulator) | No |
| `FIREBASE_AUTH_EMULATOR_HOST` (when set, Firebase calls go to the emulator) | test, local | Yes: empty means real Firebase |
| `CORS_ALLOWED_ORIGIN` (origin of the front end) | local, production | No in production; `http://localhost:5173` allowed for local |
| `RATE_LIMIT_PER_MINUTE` | all | Yes: 100 (US-018) |
| `MAX_REQUEST_SIZE` | all | Yes: 1 MB (US-018) |
| `TRASH_PURGE_CRON` (when the clean-up job of BR-04 runs) | all | Yes: once per hour [ASSUMPTION] |
| `LOG_LEVEL` | all | Yes: `INFO` |
| `SERVER_PORT` | all | Yes: 8080 |
| `VITE_API_BASE_URL` (address of the backend as the browser sees it) | local, production | No |
| `VITE_FIREBASE_PROJECT_ID`, `VITE_FIREBASE_AUTH_DOMAIN`, `VITE_FIREBASE_API_KEY` | local, production | No |
| `VITE_FIREBASE_AUTH_EMULATOR_URL` | test, local | Yes: empty means real Firebase |

## External services

| Service | Production | Local/test substitute | Must be tested manually by a human? |
|---|---|---|---|
| Firebase Authentication (e-mail with password, Google) | The customer's own Firebase project (existence and owner unknown, OQ-P06) | Firebase Local Emulator Suite (test and local); one real development Firebase project for manual checks (Q-49) | Yes: "Sign in with Google" and the real verification and reset e-mails cannot be fully emulated |
| PostgreSQL | A PostgreSQL container or the customer's own server | Testcontainers throwaway container (test); Compose container (local) | No |
| Microsoft Excel (opens the exported `.xlsx`) | Not a service: the file is produced with Apache POI | A library read-back in tests (`exceljs`) | Yes: once, open an exported file in real Excel (check 3 of Q-06) |
| Backup storage | The customer's choice | A local folder | Yes: one restore test before release (DoD-P07) |
