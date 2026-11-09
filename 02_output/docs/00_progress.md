# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-005
- Result: done
- Gate: DoD-S of US-005 passed in part: DoD-S-01 (11 of 19 frozen backend tests wait for US-006, US-007, US-009, US-014; the UI tests for US-007), DoD-S-05 (no screenshots or accessibility scan), see D-49
- Notes for the next session: Backend: `TagService` (replace by diff, one History entry per Tag via `HistoryWriter.tagChanged`), `TagRepository` (`attach` creates or reuses the Tag ignoring case and links, `detach` removes the link only), `TagCheck` (R-005-02, 04), `TagChanges` (diff); `ContactService.create/update` take the names (null on edit keeps them) and merge their errors with BR-06 and the Attribute errors. Version snapshot includes Tags. Frontend: `TagInput` inside `ContactForm` (`onSubmit(values, attributes, tags)`), suggestions from `getTagNames` (`GET /groups/{g}/facets`, US-009 must return `tags`). The Tag list endpoint and renaming are US-006; Tags in the list US-007. Backend quality (`-Pquality`) clean; frontend prettier, eslint, tsc and 167 unit tests green; gitleaks clean on the new commits. Re-check after US-006, US-007, US-009, US-014: `Us005TagAcceptanceIT` (11 tests), the `us003-005-contact.spec.ts` tests.

## Next
- Command: /implement US-006
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | 1 | done | passed (DoD-S-07: two subjects over 72 characters) |
| US-002 | 1 | done | passed (DoD-S-01: EX-002-06/07 wait for US-020/US-014, D-42; DoD-S-07: one subject over 72 characters) |
| US-003 | 1 | done | passed in part (DoD-S-01: frozen tests wait for US-007, US-014, US-004, US-005; DoD-S-05 screens; DoD-S-07: size, D-46) |
| US-004 | 1 | done | passed in part (DoD-S-01: AC-004-04/07/08/09 wait for US-009, US-014; DoD-S-05 screens, D-48) |
| US-005 | 1 | done | passed in part (DoD-S-01: AC-005-01..08 frozen tests wait for US-006, US-007, US-009, US-014; DoD-S-05 screens, D-49) |
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
