# Definition of Done: release (`DoD-R`)

> For: agent · Owner: QA lead · Agent: read-only

Checked by `/verify` for the whole product, together with the project items `DoD-Pnn` in `project/01_requirements/intent.md`; evidence in `docs/06_verification-report.md`.

| ID | Criterion | Evidence |
|---|---|---|
| DoD-R-01 | The full test suite passes; frozen test hashes match the manifest | test report, hash check |
| DoD-R-02 | Format, lint, type check and static analysis report no errors | tool output |
| DoD-R-03 | The quality gate of `general/testing/measurement.md` is met for each component, or the shortfall after the third survivor round is recorded with a decision | `docs/03_test-strategy.md` |
| DoD-R-04 | Architecture constraints (`AR`) are checked automatically where possible; no dependency cycles | architecture test output |
| DoD-R-05 | No open Critical or High finding from reviews, the security review and scanners | `docs/04_review-*.md`, verification report |
| DoD-R-06 | Every component runs in its target environment and the core user flows work at runtime | runtime demonstration log |
| DoD-R-07 | Every AC, EX and R traces to at least one test and one commit | traceability table |
| DoD-R-08 | Every screen has the final theme, meets "Done for a screen" of the UI baseline at the three widths (UI-22), and has no Critical or Serious accessibility violation | screenshots, accessibility report |
| DoD-R-09 | Root and component READMEs (ES-06) work when followed from a clean checkout | clone log |
| DoD-R-10 | Release notes list everything a human must test manually | `docs/release-notes.md` |
| DoD-R-11 | Every decision has a resolution or is listed as pending review; inputs and protected files are unchanged | decisions log, `docs/00_input-manifest.sha256` check |
| DoD-R-12 | The history follows the commit rules (`general/rules/commits.md`): the phase 3 commits add no production code beyond the skeleton, the freeze commit adds only the two manifests, every story has a `feat` commit naming its `US` id | `git log` |
| DoD-R-13 | The hidden suite passes on the final commit; its hashes match `docs/03_hidden-manifest.sha256` | `docs/06_hidden-evaluation.md` (latest run) |
| DoD-R-14 | Every NFR has been checked as the specification defines and meets its pass value | measured values in the verification report |
| DoD-R-15 | The CI pipeline (ES-14) passes on the final commit | CI run or local run of the same pipeline |
| DoD-R-16 | The production deployment files build and start with production configuration; every threat of the threat model has a checked control | build and start log, threat table |
