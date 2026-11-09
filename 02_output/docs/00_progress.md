# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S10 /review INC-2 (third look: the human's production stack with the key and a real sign-in; no code changed)
- Result: done
- Gate: DoD-I not met: new **F-26 (Critical)**: a real Firebase token is refused (401 `auth.token_invalid`) because the Admin SDK fails with `Error while fetching public key certificates: Not in GZIP format` (mixed google-http-client 1.46.3 / apache-v2 1.45.2 is the likely cause); F-14 (High) cannot be shown until F-26 is fixed; new Low F-25
- Notes for the next session: NEXT IS `/implement US-001` FOR F-26 (not `/review`): fix the verifier or the dependency set, add a test of the real key-fetch path (emulator tokens never run it) and a log line with the class of the cause. The reproduction needs no token: load the libraries of `app.jar` and verify a forged RS256 token (see the "Third look" table of `docs/04_review-INC-2.md`). After the fix the human signs in again at `https://localhost:8443` (rebuild both images with `docker build`, then `/review INC-2`). S10 only changed `docs/04_review-INC-2.md` and this file. The human runs `docker-compose.prod.yml` on this machine (`02_output/production.env`, git-excluded; key outside the repo). Plain HTTP answers 403 `https.required`, so a browser sign-in needs a TLS proxy or the header `X-Forwarded-Proto: https`; use `localhost` or add `127.0.0.1` to the Firebase authorised domains, and keep `CORS_ALLOWED_ORIGIN` equal to the browser origin. The text below is that of S9 and still holds. Notes of S9: the next command is `/review INC-2`, not `/implement US-021` as the skill's rule says, because the code of F-14 is done and there is nothing for `/implement` to change; the human may still ask for D-70 option 2 or the 503 of F-23 with `/implement US-021`. WAITS FOR THE HUMAN (D-70 b, c): create the Firebase service account (role "Firebase Authentication Admin"), put the key on the server readable by uid 10001, set `GOOGLE_APPLICATION_CREDENTIALS` in `production.env`, show a first sign-in (`GET /me` 200), `DELETE /me` and a refused token of a deleted account against the real project; the next review closes F-14 only with that. F-19 needs the human's answer (D-68 d). Low and for the release notes: F-20..F-24, F-06..F-09, F-11, F-12 (F-24: ADR-04, `secrets.env.example`, `configuration.schema.json` and README still say no service account is needed). Review evidence: `logs/review2-INC-2/`, `logs/screens/review2-INC-2/`. UI runs need `scripts/test-stack.sh up` (it is down now). Commit subjects: count the hash too, at most 72 characters.

## Next
- Command: /implement US-001
- Waiting for: D-68 d (F-19); D-70 c (sign-in, `DELETE /me`, refused token) after F-26 is fixed

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
| US-021 | 2 | done | passed in part (F-14 fixed in code, real-project check waits for the human, D-69, D-70; DoD-S-07: two new subjects over 72 characters; DoD-S-01: frozen tests of the full run that wait for US-022..US-026 and the known ones; DoD-S-05 screens; DoD-S-07: one subject over 72 characters, D-63) |
| US-022 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-023..US-026 and the known ones; DoD-S-05 screens; DoD-S-07: one commit over 400 lines, D-64) |
| US-023 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-024..US-026 and the known ones; DoD-S-05 screens, D-65) |
| US-024 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-025..US-026 and the known ones; DoD-S-05 screens; DoD-S-07: three subjects over 72 characters, one commit over 400 lines, D-66) |
| US-025 | 2 | done | passed in part (DoD-S-01: frozen tests of the full run that wait for US-026 and the known ones; DoD-S-05 screens, D-67) |
| US-026 | 2 | done | passed in part (DoD-S-01: E2E-05 restore-status assertion waits for US-012 code, D-68 k; DoD-S-05 screens; DoD-S-07: one commit over 400 lines, D-68) |
