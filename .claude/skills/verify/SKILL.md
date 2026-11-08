---
name: verify
description: Session S7, phase 6. Verify the finished system in a fresh context with evidence and without changing anything - release Definition of Done, manifests, hidden evaluation, NFRs, threat model, security scanners, secret leaks, runtime flows, UI and accessibility, CI, deployment, traceability - and write findings for /fix.
disable-model-invocation: true
---

> For: agent · Owner: QA lead · Agent: read-only

## Session

- Id: S7, phase 6, until clean (at most three rounds with `/fix`). Frame: criticise.
- Reads: `general/quality/done-release.md`; `DoD-P` in `project/01_requirements/intent.md`; the stories' rules and AC; `project/02_design/quality-requirements.md` (NFR) and `security-requirements.md`; `general/security/security-baseline.md`; `general/quality/severity-scale.md`; `general/rules/secrets.md`; `docs/02_specification.md` (threat model, NFR checks, traceability); `docs/wireframes/README.md`; the latest run in `docs/06_hidden-evaluation.md`; `docs/decisions-log.md`; production code and tests as each check needs them.
- Writes: a new section `## Run n` in `docs/06_verification-report.md`, `docs/00_progress.md`, `out/logs/`; it commits `docs/06_hidden-evaluation.md` written by the runner.
- Must not: change code, tests or configuration.
- Exit (phase 6 gate, when no finding is open): every `DoD-R` and `DoD-P` item has evidence; the hidden suite passes; manifests match; no secret leaked.

## Procedure

1. If this is run 4 or later, or a finding has stayed open for three runs: record an approval decision with options (accept as a known limitation; show the human the failing hidden test; change the requirement) and end the session. *Why:* the loop must end with a person, not continue forever.
2. Recompute every hash of the input, acceptance and hidden manifests. A mismatch is Critical.
3. Run the full suite and every check command (ES-05). Read the latest hidden evaluation: if its commit is not `HEAD`, stop and ask for `node system/run.mjs --hidden-eval` (automatic with `/run`). Each failure is a High finding "`R-nnn-nn` fails a held-out case (category)". Re-measure coverage and mutation only for a component changed since phase 5, once.
4. Runtime: start every component in its target configuration with the seed data; exercise the core flows; record what was run and seen.
5. NFR: run the check of every `NFR` from the specification and record the measured value against its pass value.
6. UI: every screen at the three widths (UI-22), light and dark, against the approved wireframes and the baseline; the accessibility check on every screen.
7. Security: for every `SB`, `SR` and every threat of the threat model, record where it is controlled and how it was checked. Run every security scanner of `tooling`; archive raw output. Lowering a Critical or High needs evidence and an approval decision.
8. Check logs and error responses for personal data, secrets and internals; check that no `.env` value leaked, listing file names only (any name listed is Critical):
   - files: `git ls-files -co --exclude-standard -z | xargs -0 grep -lF -f <(sed -n 's/^[A-Z_]*=//p' .env | tr -d '\r "'"'"'' | awk 'length>=6')`
   - commits: `git log --all --format=%B | grep -cF -f <(sed -n 's/^[A-Z_]*=//p' .env | tr -d '\r "'"'"'' | awk 'length>=6')` (must print `0`)
9. CI and deployment: run the CI pipeline locally if a runner is pinned, else confirm its last result; build and start the production deployment files with production configuration.
10. Traceability `R` / `AC` / `EX` → tests → commits; evidence or a finding for every `DoD-R` and `DoD-P`; every decision has a resolution or is pending review.
11. State at the top of the section that it is a self-check in a separate session. Commit (`docs:`); hand over (next: `/fix` with an open finding, otherwise `/release`).
