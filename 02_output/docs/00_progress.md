# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S4 /acceptance-tests
- Result: done
- Gate: phase 3 gate passed, with D-39 pending review (EX-019-03 has no screen that shows a decimal number, so it has no frozen test)
- Notes for the next session: Frozen tests (`docs/03_acceptance-manifest.sha256`, freeze commit): backend `backend/src/test/java/si/contactgroups/acceptance/` (371 executions), UI `frontend/acceptance/` (95), E2E `frontend/e2e/` (E2E-01..06). All fail on the skeleton for missing behaviour (`docs/03_test-strategy.md`). Harness readings D-38: the product must take every time from the `java.time.Clock` bean; acceptance contexts read `RATE_LIMIT_PER_MINUTE`, `REQUIRE_HTTPS`, `TRASH_PURGE_CRON` as properties; the product registers one named FirebaseApp per JVM, so own-environment classes run in failsafe execution `acceptance-own-context`. While implementing, run single classes with `-Dit.test=<Class>` or skip with `-DskipAcceptance=true`; browser tests need `scripts/test-stack.sh up`, and the backend tests need port 9099 free (stop the stack first). Hidden suite in `hidden-tests/` (ignored by git; the runner moves it to the vault; manifest `docs/03_hidden-manifest.sha256`). Two test commits of this session exceed 400 lines (494 and 466: the US-005 and US-009 commits that also carry multi-story UI specs); not rewritten (commits.md). D-38 and D-39 are pending review, as are D-29..D-35.

## Next
- Command: /implement US-001
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
