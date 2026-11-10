# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S8 /fix (loop 3)
- Result: done
- Gate: phase 6 gate not passed (no High open; open Medium: F-41..F-45 accepted by D-86, F-48, F-50 (D-88 licences, D-89 NFR-10 rule), F-51)
- Notes for the next session: fixed and re-checked on a fresh test stack: F-57 `11d5b6e`, F-58 `ce9b26a` (NFR-04 1,792 requests, none failed), F-52 `1ca570f` (three screens, chromium light and dark, webkit), F-50 keyboard walkthrough `11ffe4b` (chromium and webkit), F-47 `AC-005-06` `ca16728`. UI acceptance 95 of 95, E2E-01..06 pass in chromium. The four other F-47 tests were defects of the frozen tests; the human approved the four locator changes (D-87, D-90) and they are committed, so `docs/03_acceptance-manifest.sha256` is out of date and the human must renew it before `/verify` step 2. Maven licence report: five dual or weak-copyleft libraries (D-88, blocking, the human decides); NFR-10 counting rule D-89. Not done: Firefox (F-48), CI (F-51, the human pushes), backend unchanged and not re-run, production stack not touched (it was not running), test stack removed. Unit tests 595 of 595 (+2 new). The next `/verify` is run 4 and must re-run E2E in three browsers, the hidden evaluation (runner) and `perf/measure.mjs` in full. Nothing pushed.

## Next
- Command: /verify
- Waiting for: D-88 (licences), the human renewing the acceptance manifest (D-90), D-68 d (F-19)

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | 1 | done | passed (DoD-S-07: two subjects over 72 characters; F-26 fixed, D-78, real sign-in shown in S12) |
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
| US-021 | 2 | done | passed in part (F-14 fixed in code, real-project check waits for the human, D-69, D-70; DoD-S-07: two new subjects over 72 characters; DoD-S-01: frozen tests of the full run that wait for US-022..US-026 and the known ones; DoD-S-05 screens; DoD-S-07: one subject over 72 characters, D-63) |
| US-022 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-023..US-026 and the known ones; DoD-S-05 screens; DoD-S-07: one commit over 400 lines, D-64) |
| US-023 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-024..US-026 and the known ones; DoD-S-05 screens, D-65) |
| US-024 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-025..US-026 and the known ones; DoD-S-05 screens; DoD-S-07: three subjects over 72 characters, one commit over 400 lines, D-66) |
| US-025 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-026 and the known ones; DoD-S-05 screens, D-67) |
| US-026 | 2 | done | passed in part (DoD-S-01: E2E-05 restore-status assertion waits for US-012 code, D-68 k; DoD-S-05 screens; DoD-S-07: one commit over 400 lines, D-68) |
