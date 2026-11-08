# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-019
- Result: done
- Gate: DoD-S of US-019 passed in part: frozen acceptance tests AC-019-02..07 need screens of US-003, US-005 and US-016 and the backend stack, so they were not run (same pattern as D-44)
- Notes for the next session: The language foundation (catalogs, formats, switch, storage fallback) already existed from earlier commits; this session added `errorMessage(t, error)` in `frontend/src/api/errors.ts` (server text never shown; unknown code gives `error.generic`) and tests for it and for `LanguageSwitch`. Screens must show API errors with `errorMessage`, dates/numbers/countries with `i18n/format`. Frontend unit tests 110 green; eslint, tsc, prettier clean. Frozen UI acceptance tests of US-019 to re-check after US-003/US-016. No new decisions.

## Next
- Command: /implement US-003
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
| US-019 | 1 | done | passed in part (frozen UI tests wait for US-003, US-005, US-016) |
| US-020 | 2 | ready | — |
| US-021 | 2 | ready | — |
| US-022 | 2 | ready | — |
| US-023 | 2 | ready | — |
| US-024 | 2 | ready | — |
| US-025 | 2 | ready | — |
| US-026 | 2 | ready | — |
