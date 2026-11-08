# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-002
- Result: done
- Gate: DoD-S of US-002 passed, except DoD-S-01 in part (EX-002-06, EX-002-07 wait for US-020 and US-014 endpoints, D-42) and DoD-S-07 in part (subject of `5918897` is 77 characters; not rewritten, commits.md)
- Notes for the next session: `POST /groups` is built: `GroupService.createGroup` trims, checks 1..100 (`ValidationException` -> 422 `validation.failed` with field errors), locks the User row (`UserRepository.lock`) and refuses an equal name ignoring case among the User's Owner Groups not in Trash (`GroupNameTakenException` -> 409 `group.name_taken`); `MembershipService.addCreatorAsOwner` + `MemberRepository.insert(groupId, …)` store the Owner. Endpoint tests extend `support/WebTestSupport` (`newUser`, `as(user)`). Frontend: `features/groups/CreateGroupDialog`, opened from the page header and the empty state; UI checks share `ui-checks/check.ts`. `GET /groups` does not yet accept pending Invitations (US-014 R-014-05). Results: backend acceptance 20 of 366 (Us001 7/7, Us002 13/15), own-context 2 of 5; UI acceptance 26 of 95 (US-001 18/18, US-002 4/4, US-019 4); E2E-01 now stops at the Contacts page (US-003/US-007), `logs/s5-us002-e2e-01.log`. Open for review: D-40, D-41 ((c) before release), D-42, D-43.

## Next
- Command: /implement US-018
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
| US-018 | 1 | ready | — |
| US-019 | 1 | ready | — |
| US-020 | 2 | ready | — |
| US-021 | 2 | ready | — |
| US-022 | 2 | ready | — |
| US-023 | 2 | ready | — |
| US-024 | 2 | ready | — |
| US-025 | 2 | ready | — |
| US-026 | 2 | ready | — |
