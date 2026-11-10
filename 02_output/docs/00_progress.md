# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S11 /verify (run 4, ended at step 1)
- Result: blocked
- Gate: phase 6 gate not passed (not run: run 4 needs the human's approval, D-91)
- Notes for the next session: step 1 of `/verify` stops at run 4; no check was run and `docs/06_verification-report.md` has no Run 4 section. D-91 asks: 1 one last `/verify` round, 2 accept the open Medium findings (F-48, F-50, F-51 and the fixes waiting for confirmation) and go to `/release`, 3 change a requirement. Before a run 4 the human runs `node system/run.mjs --hidden-eval` (the last evaluation is of `12e38c7`, `HEAD` is later). The acceptance manifest was renewed (`78ef42d`); the earlier notes still hold: F-47, F-52, F-57, F-58 fixed and unconfirmed, D-88 licences and D-89 NFR-10 rule open, Firefox (F-48) and CI (F-51) need the human. Nothing pushed.

## Next
- Command: /verify D-91: <1|2|3> (with 2: /release)
- Waiting for: D-91 (fourth round), D-88 (licences), D-68 d (F-19)

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
