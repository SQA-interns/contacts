# Contact Groups (Skupine kontaktov)

> For: agent (writes), human (reads) · Written in: phase 7 (first version in phase 2) · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

Contact Groups is a multi-tenant web system. Users create Groups for a project or a company and keep Contacts in them. Only the Members of a Group see its Contacts. Contacts have predefined fields, free Attributes and Tags; every change is recorded in an append-only History; users search, filter and export selected Contacts to Excel. Sign-in is Firebase Authentication; authorisation (Owner, Editor, Viewer) is the product's own. The user interface is Slovene (default) and English.

Status of this delivery: all 26 stories are implemented. What is not finished, what a person must still test and what waits for a decision is in [`docs/release-notes.md`](docs/release-notes.md); read it before you rely on the product.

## Components

| Component | Folder | What it is |
|---|---|---|
| backend | [`backend/`](backend/README.md) | Spring Boot REST service, PostgreSQL through Flyway migrations |
| frontend | [`frontend/`](frontend/README.md) | React single-page application served by nginx |
| emulator | `emulator/` | Firebase Authentication emulator image for development and tests only (never shipped) |
| Compose | `docker-compose.yml`, `docker-compose.emulator.yml`, `docker-compose.test.yml`, `docker-compose.prod.yml` | local stack, emulator overlay, test stack, production |
| tools, scripts, seed, perf | `tools/`, `scripts/`, `seed/`, `perf/` | contract validation, helper scripts (backup, restore, seed, test stack, CI checks), local seed data, load script |

## Commands

One command per task and component (ES-05); details, configuration and troubleshooting are in the component READMEs.

| Task | backend (`02_output/backend`) | frontend (`02_output/frontend`) |
|---|---|---|
| install | none (Maven Wrapper) | `npm ci` |
| build | `./mvnw -B -DskipTests package` | `npm run build` |
| test | `./mvnw -B verify` | `npm test` |
| check (format, lint, type check, static analysis) | `./mvnw -B verify -Pquality` | `npm run check` |
| run | `./mvnw -B spring-boot:run` | `npm run dev` |

Contracts and traceability (from the repository root): `cd 02_output/tools && npm ci && npm run validate-contracts`, then `node 02_output/scripts/check-traceability.mjs`.

## Quick start (local)

Prerequisites: Docker with Compose; for development JDK 21 and Node 22.20.0.

1. Create `.env` at the **repository root** (never commit it; it is git-ignored). The names are in `01_input/01_project/00_setup/secrets.env.example`. With the emulator overlay below no real account is used, so these throw-away values work:

   ```
   POSTGRES_DB=contactgroups
   POSTGRES_USER=contactgroups_owner
   POSTGRES_PASSWORD=choose-a-local-password
   FIREBASE_PROJECT_ID=demo-contact-groups
   VITE_FIREBASE_API_KEY=fake-api-key
   VITE_FIREBASE_AUTH_DOMAIN=localhost
   ```

   For manual checks against the real development Firebase project put its values in the last three names and leave the emulator overlay out.

2. Start the stack and load the fake data, from the repository root:

   ```bash
   docker compose -f 02_output/docker-compose.yml -f 02_output/docker-compose.emulator.yml --env-file .env up -d --build
   ./02_output/scripts/seed-local.sh            # fake users in the emulator, fake Contacts (add --large for 10,000)
   ```

3. Open the frontend on `http://localhost:5173` and sign in as `ana@example.com` with the password in [`seed/README.md`](seed/README.md). The backend is on `http://localhost:8080` (`/health`), the Auth emulator on `localhost:9099`.

Ports 5173, 8080, 5432 and 9099 must be free. Stop the stack and remove its data with `docker compose -f 02_output/docker-compose.yml -f 02_output/docker-compose.emulator.yml --env-file .env down -v`. The integration tests of the backend publish port 9099 for their own emulator, so stop the stack before `./mvnw verify`.

## Tests

| What | How |
|---|---|
| Backend unit and integration tests (need Docker) | `cd 02_output/backend && ./mvnw -B verify` |
| Frontend unit tests | `cd 02_output/frontend && npm test` |
| UI checks (three browsers, three widths, accessibility) | `cd 02_output/frontend && npx playwright install chromium firefox webkit && npm run e2e` (starts its own preview server) |
| UI acceptance and E2E scenarios against a fresh stack | `./02_output/scripts/test-stack.sh up`, then `npm run acceptance` and `npm run e2e:scenarios` in `02_output/frontend`, then `./02_output/scripts/test-stack.sh down` |
| Load measurements (NFR-01..04) | `node 02_output/perf/measure.mjs` against a stack with the large seed |
| CI | `.github/workflows/ci.yml` runs every command above and the security scanners on every push. It has not run remotely yet (F-51); `02_output/scripts/check-ci-compose.sh` replays its Compose step locally. |

## Deploy

The customer runs production (any Docker host; TLS ends at the customer's reverse proxy). Everything is configured through environment variables; there are no secrets in the images.

```bash
cd 02_output
docker build -t contact-groups-backend:1.0.0 backend
docker build -t contact-groups-frontend:1.0.0 frontend
# create production.env next to docker-compose.prod.yml (chmod 600, never commit); names and meanings: docs/install-and-backup.md, section 3
docker compose -f docker-compose.prod.yml --env-file production.env up -d
curl -fsS http://127.0.0.1:8080/health        # {"status":"UP"}
```

The backend needs the Firebase service-account file (`GOOGLE_APPLICATION_CREDENTIALS`, a host path) and refuses to start without it (D-69). Backups (`scripts/backup.sh`, 14 days), the restore test (`scripts/restore.sh`), updates and the checks after the start are in [`docs/install-and-backup.md`](docs/install-and-backup.md); the guide for the people who use the product is [`docs/user-guide-sl.md`](docs/user-guide-sl.md) (Slovene).

## Documentation

| Document | Content |
|---|---|
| [`docs/release-notes.md`](docs/release-notes.md) | delivered stories, known limitations, manual tests, decisions pending review, data-retention duties |
| [`docs/install-and-backup.md`](docs/install-and-backup.md) | operator guide: install, configure, start, back up, restore, update |
| [`docs/user-guide-sl.md`](docs/user-guide-sl.md) | user guide (Slovene) |
| [`docs/openapi.json`](docs/openapi.json) | generated description of the REST interface |
| [`docs/00_progress.md`](docs/00_progress.md) | handover between sessions, stories table |
| [`docs/02_specification.md`](docs/02_specification.md) | modules, data model, behaviour per story, security, threat model, NFR checks, UI inventory, traceability |
| [`docs/02_adr/`](docs/02_adr) | architecture decisions ADR-01 to ADR-16 |
| [`docs/02_contracts/`](docs/02_contracts) | `openapi.yaml`, `configuration.schema.json`, `xlsx-export.schema.json`, `firebase-id-token.schema.json` |
| [`docs/02_tasks.md`](docs/02_tasks.md) | increments, tasks |
| [`docs/03_test-strategy.md`](docs/03_test-strategy.md) | how the tests are organised |
| [`docs/04_review-INC-1.md`](docs/04_review-INC-1.md), [`docs/04_review-INC-2.md`](docs/04_review-INC-2.md) | reviews of the two increments |
| [`docs/06_verification-report.md`](docs/06_verification-report.md), [`docs/06_hidden-evaluation.md`](docs/06_hidden-evaluation.md) | verification runs 1 to 4, held-out test evaluation |
| [`docs/07_retrospective.md`](docs/07_retrospective.md) | what went well, what held the run back, improvements |
| [`docs/decisions-log.md`](docs/decisions-log.md) | decisions D-01 and later |
| [`docs/wireframes/`](docs/wireframes) | approved wireframes WF-01 to WF-19 |
