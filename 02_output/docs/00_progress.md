# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-016
- Result: done
- Gate: DoD-S of US-016 passed in part: DoD-S-01 (the 15 frozen backend tests and the UI tests wait for US-014, US-012, US-007), DoD-S-05 (no screenshots or accessibility scan) and DoD-S-07 (one commit over the size limit) are open, see D-47
- Notes for the next session: Backend: `HistoryWriter` (only way to write entries, joins the caller's transaction), `HistoryRepository`, `HistoryQueryService`, `HistoryController` (`GET .../contacts/{c}/history?page`, 25 per page), `ContactChanges`, `HistoryAction`; `ContactService` writes `CONTACT_CREATED` and `CONTACT_EDITED` (no entry when nothing changed). Later stories add their actions through `HistoryWriter.write` (make it package-visible methods per action; details shapes in D-47). `ApiExceptionHandler` answers 405 for writes on `/history/**`. Frontend: `api/history.ts`, `ContactHistoryTab` (shows `details.changes` and `details.values`; other actions show action, time and user only, so US-004/005/011..15 add their detail lines), tabs on `ContactPage` (Owner, Editor). Backend quality (`-Pquality`: Spotless, Checkstyle, SpotBugs, PMD) clean; frontend prettier, eslint, tsc and 135 unit tests green; gitleaks clean. Re-check after US-014, US-012, US-007: `Us016HistoryAcceptanceIT`, `us016-019-history-language.spec.ts`. Run `./mvnw -Pquality spotless:apply` before committing Java (plain `spotless:apply` does nothing).

## Next
- Command: /implement US-004
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | 1 | done | passed (DoD-S-07: two subjects over 72 characters) |
| US-002 | 1 | done | passed (DoD-S-01: EX-002-06/07 wait for US-020/US-014, D-42; DoD-S-07: one subject over 72 characters) |
| US-003 | 1 | done | passed in part (DoD-S-01: frozen tests wait for US-007, US-014, US-004, US-005; DoD-S-05 screens; DoD-S-07: size, D-46) |
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
| US-016 | 1 | done | passed in part (DoD-S-01: frozen tests wait for US-014, US-012, US-007; DoD-S-05 screens; DoD-S-07: size, D-47) |
| US-017 | 1 | ready | — |
| US-018 | 1 | done | passed (DoD-S-01: frozen tests wait for US-003, US-014 and the last story, D-44; DoD-S-07: one subject over 72 characters) |
| US-019 | 1 | done | passed in part (frozen UI tests wait for US-003, US-005, US-016) |
| US-020 | 2 | ready | — |
| US-021 | 2 | ready | — |
| US-022 | 2 | ready | — |
| US-023 | 2 | ready | — |
| US-024 | 2 | ready | — |
| US-025 | 2 | ready | — |
| US-026 | 2 | ready | — |
