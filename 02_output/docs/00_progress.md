# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-021 D-69: 1 (review finding F-14)
- Result: done
- Gate: DoD-I-02 not met until F-14 is shown against the real Firebase project (code fixed, D-70); no new Critical or High
- Notes for the next session: F-14 is fixed in code (D-69 option 1, D-70): `FirebaseCredentials`, `StartupGuard` (prod refuses to start without the service-account file), `docker-compose.prod.yml` mount. WAITS FOR THE HUMAN (D-70 b, c): create the service account, put the key on the server (readable by uid 10001), set `GOOGLE_APPLICATION_CREDENTIALS` in `production.env`, and show a first sign-in and `DELETE /me` against the real project; /review must not close F-14 without that. Inputs, ADR-04, the contract and README still say "no service account" (D-70 d). Full backend run: 2 known failures (`ex_007_05`, `ac_018_09`); PMD reports 3 findings in other stories' code (`ContactRepository:94`, `:234`, `MergeService:98`): fix them in the sessions on US-025/US-026. Still open from `docs/04_review-INC-2.md`: F-16 (US-025, next), F-15 (US-026, after it), F-17 and F-18 (`/fix`), carried F-03, F-04, F-13; then `/review INC-2`. Spotless: `./mvnw -B -Pquality spotless:apply`; backend runs need port 9099 free; UI runs need `scripts/test-stack.sh up`. Commit subjects: count the hash too, at most 72 characters.

## Next
- Command: /implement US-025
- Waiting for: nothing

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
| US-021 | 2 | done | passed in part (F-14 fixed in code, real-project check waits for the human, D-69, D-70; DoD-S-07: three subjects over 72 characters; DoD-S-01: frozen tests of the full run that wait for US-022..US-026 and the known ones; DoD-S-05 screens; DoD-S-07: one subject over 72 characters, D-63) |
| US-022 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-023..US-026 and the known ones; DoD-S-05 screens; DoD-S-07: one commit over 400 lines, D-64) |
| US-023 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-024..US-026 and the known ones; DoD-S-05 screens, D-65) |
| US-024 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-025..US-026 and the known ones; DoD-S-05 screens; DoD-S-07: three subjects over 72 characters, one commit over 400 lines, D-66) |
| US-025 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-026 and the known ones; DoD-S-05 screens, D-67) |
| US-026 | 2 | done | passed in part (DoD-S-01: E2E-05 restore-status assertion waits for US-012 code, D-68 k; DoD-S-05 screens; DoD-S-07: one commit over 400 lines, D-68) |
