# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-026
- Result: blocked
- Gate: DoD-S not passed for US-026 (DoD-S-01: the frozen `Us026MergeAcceptanceIT`, the frozen UI tests of US-026 and `e2e-05` not run; DoD-S-05: WF-18 screenshots missing; DoD-S-07: one commit of about 1,000 lines)
- Notes for the next session: D-68 records the readings and what could not run. Done and committed: `MergeValues`, `MergeService`, `MergeController` (`POST /groups/{g}/merges`), the survivor History with the merged-away entries (`HistoryRepository.contactAndMergedIds`), `ContactRepository.moveToTrashAtVersion`; UI: pick boxes and "Združi izbrane (n)" on `DuplicatesPage`, `MergePage`, `MergeForm`, `MergeTable`, `preselect.ts`, `api/duplicates.ts` `mergeContacts`. Own tests pass: `MergeValuesTest`, `MergeServiceIT`, `MergeEndpointsIT`, frontend `preselect.test.ts`, `DuplicatesPage.test.tsx`, `MergePage.test.tsx`; the full backend build without acceptance tests has 418 of 419 passing (the failure is `FirebaseEmulatorIT`, port 9099 taken by `contact-groups-test-emulator-1`), the frontend check and vitest pass (one US-010 selection test failed once under load and passes alone). Gitleaks clean. NOT RUN: stopping `contact-groups-test-emulator-1` for the acceptance IT was refused in the session, and the test stack was not rebuilt, so the frozen backend IT, the frozen UI tests (`acceptance/us020-026-increment2.spec.ts`, `e2e/e2e-05-clean-up.spec.ts`) and `ui-checks/merge-screens.stack.spec.ts` (written, linted, uncommitted logs in `logs/screens/WF-18_merge`) have not run. Then run `/review INC-2` after fixing what they find. Open from the review as before: F-03, F-04, F-10, F-13 and Low F-06, F-07, F-11, F-12; HUMAN, BEFORE PRODUCTION: Firebase Admin credentials (D-63 a); known failing frozen UI tests of INC-1: AC-003-07, AC-004-04, AC-004-08, AC-005-06 (D-62 h). Spotless: `./mvnw -B -Pquality spotless:apply`. Commit subjects: count the hash too, at most 72 characters.

## Next
- Command: /implement US-026
- Waiting for: human: free port 9099 (stop `contact-groups-test-emulator-1`) for the backend acceptance run and let `scripts/test-stack.sh up` rebuild the test stack (D-68 i)

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | 1 | done | passed (DoD-S-07: two subjects over 72 characters) |
| US-002 | 1 | done | passed (DoD-S-01: EX-002-06/07 wait for US-020/US-014, D-42; DoD-S-07: one subject over 72 characters) |
| US-003 | 1 | done | passed in part (DoD-S-01: frozen tests wait for US-007, US-014, US-004, US-005; DoD-S-05 screens; DoD-S-07: size, D-46) |
| US-004 | 1 | done | passed in part (DoD-S-01: AC-004-04/07/08/09 wait for US-009, US-014; DoD-S-05 screens, D-48) |
| US-005 | 1 | done | passed in part (DoD-S-01: AC-005-01..08 frozen tests wait for US-006, US-007, US-009, US-014; DoD-S-05 screens, D-49) |
| US-006 | 1 | done | passed in part (DoD-S-01: UI AC-006-04 timing, D-50; Group navigation F-01, F-02 fixed, D-61) |
| US-007 | 1 | done | passed in part (DoD-S-01: EX-007-05 frozen test contradicts R-007-03, others wait for US-012, US-014; DoD-S-05 screens, D-51) |
| US-008 | 1 | done | passed in part (DoD-S-01: AC-008-07 waits for US-012; DoD-S-05 screens, D-52) |
| US-009 | 1 | done | passed in part (DoD-S-01: EX-009-06 waits for US-012; DoD-S-05 screens, D-53) |
| US-010 | 1 | done | passed in part (DoD-S-01: 13 frozen tests wait for US-014, US-011; DoD-S-05 screens, D-54) |
| US-011 | 1 | done | passed in part (DoD-S-01: 7 backend and the UI frozen tests wait for US-012, US-014, US-017; DoD-S-05 screens, D-55) |
| US-012 | 1 | done | passed in part (DoD-S-01: 13 backend and UI AC-012-01 frozen tests wait for US-014; DoD-S-05 screens, D-56) |
| US-013 | 1 | done | passed in part (DoD-S-01: 6 backend and UI AC-013-02 frozen tests wait for US-014, US-017, US-020, US-026; DoD-S-05 screens, D-57) |
| US-014 | 1 | done | passed in part (DoD-S-01: 2 backend and the UI AC-014-03 frozen tests wait for US-015, US-017 and D-58 item g; DoD-S-05 screens, D-58) |
| US-015 | 1 | done | passed in part (DoD-S-01: 2 backend and `e2e-02` frozen tests wait for US-017; DoD-S-05 screens, D-59) |
| US-016 | 1 | done | passed in part (DoD-S-01: frozen tests wait for US-014, US-012, US-007; DoD-S-05 screens; DoD-S-07: size, D-47) |
| US-017 | 1 | done | passed in part (DoD-S-01: UI AC-014-03 waits for D-58 item g; DoD-S-05 screens, D-60) |
| US-018 | 1 | done | passed (DoD-S-01: frozen tests wait for US-003, US-014 and the last story, D-44; DoD-S-07: one subject over 72 characters) |
| US-019 | 1 | done | passed in part (frozen UI tests wait for US-003, US-005, US-016) |
| US-020 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-021..US-026 and the known ones; DoD-S-05 screens; DoD-S-07: three subjects over 72 characters, D-62) |
| US-021 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-022..US-026 and the known ones; DoD-S-05 screens; DoD-S-07: one subject over 72 characters, D-63) |
| US-022 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-023..US-026 and the known ones; DoD-S-05 screens; DoD-S-07: one commit over 400 lines, D-64) |
| US-023 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-024..US-026 and the known ones; DoD-S-05 screens, D-65) |
| US-024 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-025..US-026 and the known ones; DoD-S-05 screens; DoD-S-07: three subjects over 72 characters, one commit over 400 lines, D-66) |
| US-025 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-026 and the known ones; DoD-S-05 screens, D-67) |
| US-026 | 2 | in progress | not passed (frozen tests and WF-18 screens not run, D-68) |
