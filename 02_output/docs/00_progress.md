# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-007
- Result: done
- Gate: DoD-S of US-007 passed in part: DoD-S-01 (9 of 11 backend frozen tests pass: EX-007-05 contradicts R-007-03, AC-007-06 waits for US-012, AC-007-08 for US-014; UI AC-007-04, -05 wait for US-014), see D-51
- Notes for the next session: Backend: `GET /groups/{g}/contacts` (`ContactController.listContacts`, `SearchService.list`, `ContactRepository.count/page`, sort by normalised columns, migration V3 `city_norm`), `InvalidQueryException` (400). Frontend: `features/contacts/ContactListPage`, `ContactTable`, `api/contacts.listContacts`, route `groups/:groupId/contacts`; Contact tabs are now links `?tab=`. Screens in `out/logs/screens/WF-06_contact-list/`. `q` and filters are ignored until US-008/US-009. The Group tab bar still does not exist. Re-check after US-009, US-012, US-014, US-017: `Us005TagAcceptanceIT`, `Us006TagListAcceptanceIT`, `Us007ContactListAcceptanceIT`, `us003-005-contact.spec.ts`, `us007-009-list.spec.ts`.

## Next
- Command: /implement US-008
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
