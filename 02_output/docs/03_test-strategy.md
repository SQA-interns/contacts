# Test strategy (as executed)

> For: agent (writes), human (reads) · Written in: phase 3 (`/acceptance-tests`), phase 5 (`/quality-gate`) · Source: `general/testing/writing-tests.md`, `general/testing/measurement.md`, `project/02_design/quality-requirements.md` · Agent: writes

## Test levels and harness

## Measurement exclusions

<!-- Before any test is written: production code left out of coverage and mutation, one row per path with the reason (generated code, application entry point, style and token files, …). Rules: `general/testing/measurement.md`. Not extended later without a decision record. -->

| Path | Reason |
|---|---|

## Hidden suite

<!-- Only the run command and the number of hidden tests per story; never the cases. -->

| Story | Hidden tests |
|---|---|

Run command:

## First run of the frozen tests (before any implementation)

| Story | Tests | Failed | Reason (one line per group) |
|---|---|---|---|

## Test measures (phase 5)

<!-- Level rows: tests passed / failed. unit = unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all-levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (raw / adjusted, scope) | Tools |
|---|---|---|---|---|---|---|

## Quality gate (phase 5)

| Component | Line threshold met | Mutation threshold met | Shortfall (points) | Survivor rounds | Tests at first mutation run | Decision |
|---|---|---|---|---|---|---|

## Equivalent mutants (phase 5)

| Component | Class and method | Mutation | Why it cannot change behaviour |
|---|---|---|---|
