# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S4 /acceptance-tests
- Result: failed (stopped by the usage limit; work in progress, nothing frozen)
- Gate: phase 3 gate not passed (tests incomplete, no manifests)
- Notes for the next session: resume /acceptance-tests at "write acceptance tests" for US-004. Done: measurement exclusions in `docs/03_test-strategy.md`; backend harness `backend/src/test/java/si/contactgroups/acceptance/support/` (Emulator on fixed port 9099 with retry, MutableClock replacing the `Clock` bean, per-test truncate + emulator wipe, Http/Resp/TestUser/Grp/Db/Xlsx); pom.xml failsafe split into `default` and `acceptance` executions (`-DskipAcceptance`, display names in JUnit XML); tests Us001 (backend part), Us002, Us003, compiling, not yet run. Forged expired tokens need `Emulator.setValidSince` (emulator checks revocation). Plan left: US-004..026 backend tests (US-018 in own classes with RATE_LIMIT 100 and REQUIRE_HTTPS=true contexts); Playwright UI acceptance in `frontend/acceptance/` + `playwright.acceptance.config.ts` against a Compose test stack (raised rate limit); move `frontend/e2e/smoke.spec.ts` to `frontend/ui-checks/` so `e2e/` holds only E2E-01..06; hidden suite in `02_output/hidden-tests/` (own pom compiling `../backend/src/main`, run.sh merging JUnit XML, hidden.json); non-blocking decision for EX-019-03 (no screen shows a decimal number) and the Google UI flow (manual); run, lint, commit per story, freeze commit.

## Next
- Command: /acceptance-tests
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
