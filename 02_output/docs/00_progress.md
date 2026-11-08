# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-018
- Result: done
- Gate: DoD-S of US-018 passed, except DoD-S-01 in part (all of `Us018SecurityAcceptanceIT` and `ac_018_15_security_log` set up through Contact and Invitation endpoints of US-003 and US-014; AC-018-10 needs every operation of the contract; D-44) and DoD-S-07 in part (one subject of 73 characters; not rewritten, commits.md)
- Notes for the next session: Group endpoints are secured by `@RequiresAction` + `GroupAccessInterceptor` (registered in `config/WebConfig`; `GroupAccessService.requireAllowed` gives 404 `group.not_found` or 403 `auth.role_forbidden`); a controller method with `{groupId}` in its path needs `@RequiresAction`, objects must be loaded with the Group id (R-018-05, add a test per object type). Filter chain in `SecurityConfig`: SecurityLogger, RequestSizeFilter, HttpsFilter, CORS, FirebaseTokenFilter, RateLimitFilter, EmailVerifiedFilter. Unknown body properties give 400 `request.unknown_property`. `IsolationIT` and `ContractParityIT` cover every new endpoint automatically; `ContractParityIT` fails when an operation id differs from the contract. Test-only `GroupPathProbe` (profile `group-path-probe`, `@Hidden`). Results: backend acceptance 20 of 366 (unchanged: Us001 7/7, Us002 13/15), own-context 4 of 5 (AC-018-12, AC-018-14, EX-018-03, EX-018-09 pass); unit 42, integration 60 and frontend 106 tests green; Checkstyle, SpotBugs, PMD, Spotless and gitleaks clean. Open for review: D-40..D-45.

## Next
- Command: /implement US-019
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | 1 | done | passed (DoD-S-07: two subjects over 72 characters) |
| US-002 | 1 | done | passed (DoD-S-01: EX-002-06/07 wait for US-020/US-014, D-42; DoD-S-07: one subject over 72 characters) |
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
| US-018 | 1 | done | passed (DoD-S-01: frozen tests wait for US-003, US-014 and the last story, D-44; DoD-S-07: one subject over 72 characters) |
| US-019 | 1 | ready | — |
| US-020 | 2 | ready | — |
| US-021 | 2 | ready | — |
| US-022 | 2 | ready | — |
| US-023 | 2 | ready | — |
| US-024 | 2 | ready | — |
| US-025 | 2 | ready | — |
| US-026 | 2 | ready | — |
