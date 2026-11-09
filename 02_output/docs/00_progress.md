# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-004
- Result: done
- Gate: DoD-S of US-004 passed in part: DoD-S-01 (4 of 20 frozen backend tests wait for US-014 and US-009, the UI tests for US-007), DoD-S-05 (no screenshots or accessibility scan), see D-48
- Notes for the next session: Backend: `AttributeService` (replace by diff, one History entry per pair via `HistoryWriter.attributeChanged`), `AttributeRepository`, `AttributeCheck` (R-004-02..04), `AttributeChanges` (diff); `ContactService.create/update` take the pairs (null on edit keeps them) and merge their errors with BR-06. Version snapshot includes Attributes. Frontend: `AttributeEditor` inside `ContactForm` (props `groupId`; `onSubmit(values, attributes)`), suggestions from `getAttributeKeys` (`GET /groups/{g}/facets`, US-009 must return `attributeKeys`). Tags are still not stored (US-005). Backend quality (`-Pquality` spotless, checkstyle, PMD, SpotBugs) clean; frontend prettier, eslint, tsc and 145 unit tests green. Re-check after US-014, US-009, US-007: `Us004AttributeAcceptanceIT` (ac_004_04, 07, 08, 09), the `us003-005-contact.spec.ts` tests. gitleaks flags two old log files, see D-48 (i).

## Next
- Command: /implement US-004
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | 1 | done | passed (DoD-S-07: two subjects over 72 characters) |
| US-002 | 1 | done | passed (DoD-S-01: EX-002-06/07 wait for US-020/US-014, D-42; DoD-S-07: one subject over 72 characters) |
| US-003 | 1 | done | passed in part (DoD-S-01: frozen tests wait for US-007, US-014, US-004, US-005; DoD-S-05 screens; DoD-S-07: size, D-46) |
| US-004 | 1 | done | passed in part (DoD-S-01: AC-004-04/07/08/09 wait for US-009, US-014; DoD-S-05 screens, D-48) |
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
