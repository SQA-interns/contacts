# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-010
- Result: done
- Gate: DoD-S of US-010 passed in part: DoD-S-01 (the 13 frozen tests need invitations, US-014; own unit and page tests pass), see D-54
- Notes for the next session: Frontend only: `features/contacts/selection/` (`useSelection` hook with ids or "all results" minus unticked, `SelectionBar`); `ContactTable` takes an optional `selection`. The bar's Export button is US-011 (T-36): it must send the filter plus the unticked ids, or the ids (`useSelection` has `allResults`, `isSelected`, `count`). Re-check after US-014, US-011: `acceptance/us010-011-selection.spec.ts`. Screens `selection`, `selection-page` in `out/logs/screens/WF-06_contact-list/`. The test stack is up (`scripts/test-stack.sh down` to stop).

## Next
- Command: /implement US-011
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | 1 | done | passed (DoD-S-07: two subjects over 72 characters) |
| US-002 | 1 | done | passed (DoD-S-01: EX-002-06/07 wait for US-020/US-014, D-42; DoD-S-07: one subject over 72 characters) |
| US-003 | 1 | done | passed in part (DoD-S-01: frozen tests wait for US-007, US-014, US-004, US-005; DoD-S-05 screens; DoD-S-07: size, D-46) |
| US-004 | 1 | done | passed in part (DoD-S-01: AC-004-04/07/08/09 wait for US-009, US-014; DoD-S-05 screens, D-48) |
| US-005 | 1 | done | passed in part (DoD-S-01: AC-005-01..08 frozen tests wait for US-006, US-007, US-009, US-014; DoD-S-05 screens, D-49) |
| US-006 | 1 | done | passed in part (DoD-S-01: 4 backend tests wait for US-007, US-012, US-014, US-017; UI AC-006-04 timing, D-50) |
| US-007 | 1 | done | passed in part (DoD-S-01: EX-007-05 frozen test contradicts R-007-03, others wait for US-012, US-014; DoD-S-05 screens, D-51) |
| US-008 | 1 | done | passed in part (DoD-S-01: AC-008-07 waits for US-012; DoD-S-05 screens, D-52) |
| US-009 | 1 | done | passed in part (DoD-S-01: EX-009-06 waits for US-012; DoD-S-05 screens, D-53) |
| US-010 | 1 | done | passed in part (DoD-S-01: 13 frozen tests wait for US-014, US-011; DoD-S-05 screens, D-54) |
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
