# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S3 /plan
- Result: done
- Gate: phase 2 gate passed except the wireframes (they follow in /wireframes)
- Notes for the next session: Specification `docs/02_specification.md` (screen inventory SC-01 to SC-19 in "User interface" is the start of /wireframes; the wireframe theme is active in `frontend/src/styles`), ADR-01 to ADR-16, four contracts in `docs/02_contracts/`, 78 tasks in `docs/02_tasks.md` (one `/implement` per story), skeletons of backend and frontend, CI, Dockerfiles, Compose files, seed (`scripts/seed-local.sh`). Decisions D-29 to D-35 are non-blocking and "pending review": config keys, dev tools, readings, Boot 4 and Firebase library changes (D-32 adds `google-http-client-jackson2`; D-35: the backend dependency scan found 0 vulnerabilities in 86 libraries), frontend packages and `.npmrc` (D-33), scanner results (D-34). Verified: backend `./mvnw -B verify -Pquality` (24 unit and 19 integration tests), frontend `npm run check`, `npm test` (41), `npm run build`, Playwright smoke on Chromium and WebKit, three images built, production Compose healthy, seed loads, gitleaks, actionlint, contracts and traceability. Not verified: Firefox (crashes in the agent sandbox, D-09; CI runs it), the pipeline on GitHub itself (no runner). Stop the Compose stack before `./mvnw verify` (port 9099); on macOS `mvnw` needs a GNU-style `sha256sum` (backend README). Commits `2d6047d` and `85ebaf5` hold an `openapi.yaml` that does not parse; the fix commit `86dd8d4` after them corrects it (history is not rewritten).

## Next
- Command: /wireframes
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | 1 | ready | — |
| US-002 | 1 | ready | — |
| US-003 | 1 | ready | — |
| US-004 | 1 | ready | — |
| US-005 | 1 | ready | — |
| US-006 | 1 | ready | — |
| US-007 | 1 | ready | — |
| US-008 | 1 | ready | — |
| US-009 | 1 | ready | — |
| US-010 | 1 | ready | — |
| US-011 | 1 | ready | — |
| US-012 | 1 | ready | — |
| US-013 | 1 | ready | — |
| US-014 | 1 | ready | — |
| US-015 | 1 | ready | — |
| US-016 | 1 | ready | — |
| US-017 | 1 | ready | — |
| US-018 | 1 | ready | — |
| US-019 | 1 | ready | — |
| US-020 | 2 | ready | — |
| US-021 | 2 | ready | — |
| US-022 | 2 | ready | — |
| US-023 | 2 | ready | — |
| US-024 | 2 | ready | — |
| US-025 | 2 | ready | — |
| US-026 | 2 | ready | — |
