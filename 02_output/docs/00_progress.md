# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-006
- Result: done
- Gate: DoD-S of US-006 passed in part: DoD-S-01 (9 of 13 frozen backend tests pass, 4 wait for US-007, US-012, US-014, US-017; frozen UI AC-006-04 races with the delete, AC-006-05 passes), see D-50
- Notes for the next session: Backend: `TagController` (`GET/PATCH/DELETE /groups/{g}/tags[/{t}]`, action `MANAGE_TAGS`), `TagService.listWithCounts/rename/delete`, `TagRepository.counts/find/rename/delete/contactIds`, `HistoryWriter.tagRenamed/tagDeleted`, `TagNotFoundException` (404), `TagNameTakenException` (409). Frontend: `features/tags/TagListPage`, `api/tags.ts`, route `groups/:groupId/tags`; screens checked, see `out/logs/screens/WF-09_tags/`. Quality (`-Pquality` spotless, checkstyle, spotbugs), prettier, eslint, tsc, 175 unit tests green; gitleaks clean. Re-check after US-007, US-009, US-012, US-014, US-017: `Us005TagAcceptanceIT` (11), `Us006TagListAcceptanceIT` (4), `us003-005-contact.spec.ts`. The group tab bar (Contacts, Tags, Trash, …) does not exist yet; the Tag list is reached by URL until US-007 builds the Group navigation.

## Next
- Command: /implement US-007
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
