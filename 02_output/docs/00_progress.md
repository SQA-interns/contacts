# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: R /review INC-1
- Result: done
- Gate: DoD-I not passed (DoD-I-02: F-01, F-02 High; DoD-I-04: F-01, F-05, F-06), see `docs/04_review-INC-1.md`
- Notes for the next session: F-01 the Group tab bar and Group bar of the wireframes are missing; F-02 no link to the Tags page. Build one Group shell (header, name, Role badge, tabs, mobile menu) for all Group screens; add the missing screens WF-07, WF-08, WF-19 (F-05). Medium: F-03 `page=abc` on History endpoints answers 500, F-04 `/health` has an extra `groups` field (AC-018-09), F-06 date format in WF-13. The test stack now holds the seed and one test Contact (`<script>…`) in Acme.

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
| US-006 | 1 | done | passed in part (DoD-S-01: 4 backend tests wait for US-007, US-012, US-014, US-017; UI AC-006-04 timing, D-50) |
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
| US-020 | 2 | ready | — |
| US-021 | 2 | ready | — |
| US-022 | 2 | ready | — |
| US-023 | 2 | ready | — |
| US-024 | 2 | ready | — |
| US-025 | 2 | ready | — |
| US-026 | 2 | ready | — |
