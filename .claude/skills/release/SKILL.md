---
name: release
description: Session S9, phase 7. Write the root and component READMEs with build, run and deploy instructions, the release notes with manual tests and pending decisions, prove the READMEs work from a clean checkout, commit the hidden suite, and write the retrospective with the run's numbers and improvements.
disable-model-invocation: true
---

> For: agent · Owner: Team lead · Agent: read-only

## Session

- Id: S9, phase 7. Frame: generate.
- Reads: `general/engineering-standards.md` (ES-05, ES-06); `project/02_design/architecture.md`; `project/00_setup/environments.md`; `project/02_design/security-requirements.md` (retention); `docs/decisions-log.md`; the last section of `docs/06_verification-report.md`.
- Writes: `out/README.md`, one README per component, `docs/release-notes.md`, `docs/07_retrospective.md`, the hidden suite commit, `docs/00_progress.md`.
- Must not: change production code or tests.
- Exit (phase 7 gate): every README works from a clean checkout; the release notes list the manual tests and every decision pending review; the retrospective is written.

## Procedure

1. Write `out/README.md` and one README per component (ES-06) with the ES-05 commands and a "Deploy" section for the deployment files.
2. Follow every README from a clean clone in a temporary folder; fix the README until it works; log in `out/logs/`.
3. `docs/release-notes.md`: delivered stories by increment; known limitations (accepted findings, shortfalls); manual tests (real external services, items without automated evidence); decisions pending review; data-retention duties; how to deploy.
4. The runner has put the hidden suite back into `02_output/hidden-tests/`: remove it from `.gitignore` and commit it (`test:`); its hashes must match `docs/03_hidden-manifest.sha256`.
5. `docs/07_retrospective.md`: the "Numbers" section is written by `node system/run.mjs --summary` (run it if it is empty); then what went well, what held the run back (with evidence), and at most five improvements, each marked for the project or the template.
6. Commit each document; hand over (result: run complete).
