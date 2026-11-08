---
name: quality-gate
description: Session S6, phase 5. Measure line coverage and mutation score per component over the defined scope, run at most three targeted survivor rounds, and record the gate result or the shortfall.
disable-model-invocation: true
---

> For: agent · Owner: QA lead · Agent: read-only

## Session

- Id: S6, phase 5. Frame: measure.
- Reads: `general/testing/measurement.md`; `project/02_design/quality-requirements.md` ("Thresholds", "Mutation scope"); `docs/03_test-strategy.md` ("Measurement exclusions"); the production code and tests of the area being measured.
- Writes: unit and integration tests, tool configuration, `docs/03_test-strategy.md`, `docs/00_progress.md`.
- Must not: change production code, except to fix a defect a new test proves (a finding with its own `fix:` commit).
- Exit (phase 5 gate): the quality gate is met per component, or the shortfall after the third survivor round is recorded with a decision.

## Procedure

1. Configure coverage and mutation tools exactly to the scope and exclusions; commit the configuration alone (`build:`).
2. Measure coverage per component (`unit`, `nonUnit`, `all`). Below the line threshold, add tests area by area (validation, security, persistence, business rules first), measuring after each area.
3. Check once that the mutation tool works: a mutant in plainly covered code must be killed. A score far below line coverage means mutants are not activated: stop and raise a decision; record nothing.
4. Run mutation testing **once** per component on the full scope with the full suite, in the background; read only the summary lines.
5. Below the mutation threshold: up to **three** survivor rounds on the code that matters most, re-running only the mutated classes, then one confirming full run. Justify every equivalent mutant.
6. Stop each component at the gate; record a shortfall after the third round as a non-blocking decision and a finding.
7. Fill the phase 5 sections of `docs/03_test-strategy.md`. Commit; hand over (next: `/verify`).
