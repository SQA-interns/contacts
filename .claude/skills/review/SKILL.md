---
name: review
description: Session R, phase 4. Review one finished increment in a fresh context without changing anything - code against the specification and engineering standards, test-first evidence, screens against the approved wireframes, security against the baseline - and write findings for the next implement session.
argument-hint: "INC-n"
disable-model-invocation: true
---

> For: agent · Owner: QA lead · Agent: read-only

## Session

- Id: R, phase 4, once per increment. Frame: criticise. You did not write this code; read it as a reviewer would.
- Reads: the increment's stories; the specification sections they cite; `docs/wireframes/README.md` and the wireframe images of its screens; `git diff` and `git log` of the increment; `general/engineering-standards.md`; `general/security/security-baseline.md`; `general/quality/done-increment.md`; `general/quality/severity-scale.md`.
- Writes: `docs/04_review-<INC>.md`, `docs/00_progress.md`.
- Must not: change code or tests.
- Exit: every finding has a severity and a story; with none Critical or High open, the increment meets `DoD-I`.

## Procedure

1. Get the diff from the commit before the increment's first story to `HEAD`.
2. For every story check:
   - every rule `R` is implemented where the specification says and has a test that would fail without it;
   - every `feat` commit carries `Red:` lines (test-first evidence);
   - nothing is implemented that no rule asks for, and nothing listed under "Out of scope";
   - ES-01, ES-07, ES-10, ES-11, ES-13 and the architecture constraints;
   - security: input validated on the trusted side, authorisation on every non-public operation, output encoding, no secret or personal data in logs;
   - every value without a source is in the decision log.
3. Start the application with the seed data, exercise the increment's flows, and take screenshots of its screens at the three widths (UI-22); compare structure, layout per width and wording with the approved wireframes.
4. Write `docs/04_review-<INC>.md`: findings `F-nn` with severity, story, evidence (file and line, or screenshot) and what is wrong. No fixes.
5. Commit (`docs:`). Hand over: with a Critical or High finding, next is `/implement <first affected story>`; otherwise the first story of the next increment, or `/design` after the last increment. Medium and Low findings go to the story's next session or the release notes.
