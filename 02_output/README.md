# Contact Groups (Skupine kontaktov)

> For: agent (writes), human (reads) · Written in: phase 7 (first version in phase 2) · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

Contact Groups is a multi-tenant web system. Users create Groups for a project or a company and keep Contacts in them. Only the Members of a Group see its Contacts. Contacts have predefined fields, free Attributes and Tags; every change is recorded in an append-only History; users search, filter and export selected Contacts to Excel. Sign-in is Firebase Authentication; authorisation (Owner, Editor, Viewer) is the product's own. The user interface is Slovene (default) and English.

## Components

| Component | Folder | What it is |
|---|---|---|
| backend | [`backend/`](backend/README.md) | Spring Boot REST service, PostgreSQL through Flyway migrations |
| frontend | [`frontend/`](frontend/README.md) | React single-page application served by nginx |
| emulator | `emulator/` | Firebase Authentication emulator image for development and tests only (never shipped) |
| tools, scripts, seed, perf | `tools/`, `scripts/`, `seed/`, `perf/` | contract validation, helper scripts, local seed data, load script |

## Quick start (local)

Prerequisites: Docker with Compose; for development JDK 21 and Node 22.20.0. Copy `01_input/01_project/00_setup/secrets.env.example` to `.env` at the repository root and fill the names (never commit it).

```bash
docker compose -f 02_output/docker-compose.yml --profile emulator --env-file .env up --build
./02_output/scripts/seed-local.sh      # fake users and Contacts, no real personal data
```

The frontend is on `http://localhost:5173`, the backend on `http://localhost:8080` (`/health`), the Auth emulator on `localhost:9099`.

## Documentation

| Document | Content |
|---|---|
| [`docs/00_progress.md`](docs/00_progress.md) | handover between sessions, stories table |
| [`docs/02_specification.md`](docs/02_specification.md) | modules, data model, behaviour per story, security, threat model, NFR checks, UI inventory, traceability |
| [`docs/02_adr/`](docs/02_adr) | architecture decisions ADR-01 to ADR-16 |
| [`docs/02_contracts/`](docs/02_contracts) | `openapi.yaml`, `configuration.schema.json`, `xlsx-export.schema.json`, `firebase-id-token.schema.json` |
| [`docs/02_tasks.md`](docs/02_tasks.md) | increments, tasks |
| [`docs/decisions-log.md`](docs/decisions-log.md) | decisions D-01 and later |
