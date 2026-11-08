# Measurement

> For: agent · Owner: QA lead · Agent: read-only

What test counts, coverage, mutation score and code size mean, and the quality gate.

Record every figure in `docs/03_test-strategy.md` (and `docs/06_verification-report.md` when re-measured) with its scope, level and tool. The procedure that reaches the quality gate is `skills/quality-gate`. The project selects mutation paths and exclusions in `project/02_design/quality-requirements.md` ("Mutation scope"); this file never names them.

## Measurement constraints

- Do not change a tool's scope, filters, exclusions or thresholds to improve a number. A change is a decision record. *Why:* a number reached by moving the target says nothing about the code.
- Do not report a figure without its scope, level and tool. *Why:* 95 % of what? Without scope a figure cannot be compared or trusted.
- Production code is the source a component ships. It excludes tests, generated code, the application entry point and anything listed under "Measurement exclusions" in `docs/03_test-strategy.md` with a reason. Decide the exclusions in phase 3, before any test is written; do not extend them later. *Why:* exclusions chosen after seeing the results are a way to hide untested code.
- Presentation is not logic: style sheets, design-token files and static assets are outside coverage and mutation. Keep styling in those files (or in style modules listed as exclusions in phase 3); keep validation, state, data flow and conditional rendering in measured code. Do not move logic into an excluded file. *Why:* styles have no behaviour to test, so measuring them only punishes good design; logic must stay measured.
- Test code never counts towards coverage or mutation. 

## Test counts

Per component and level (`unit`, `integration`, `acceptance`, `architecture`, `e2e`), passed and failed, from the runner's own report.

## Coverage

Line and branch, per component, as three figures: `unit` (unit tests only), `nonUnit` (integration, acceptance and end-to-end tests that exercise the component) and `all` (one merged report). The headline figure is `all`.

## Mutation score

- **Scope:** all production code of the component, narrowed only to the paths in `project/02_design/quality-requirements.md` "Mutation scope" (default: all production code). Do not narrow it by running only some tests or by excluding packages that are not listed there.
- **Tests:** the component's full automated suite, at every level it has. If that is infeasible, record the tests actually run and the reason in a decision record.
- **Record:** `score` (raw) = killed (timed-out counts as killed) ÷ all mutants the tool generated; a mutant no test reaches counts as survived. `scoreAdjusted` = (killed + equivalent) ÷ total, where `equivalent` is the number of survivors justified as equivalent. `scoreCovered` = killed ÷ mutants a test reaches. Also killed, equivalent, total, covered, scope, tests, tool.
- Do not remove equivalent mutants by hand. List every survivor you count as equivalent in `docs/03_test-strategy.md` with the class and method, the mutation and the reason it cannot change behaviour; a survivor without that entry is not equivalent. Always record the raw score next to the adjusted score.

## Code size

`cloc` (version from `tech-stack.md`): production = non-test source and resources, test = test sources including end-to-end tests. Generated code and lock files are excluded.

## Quality gate

The thresholds in `project/02_design/quality-requirements.md` ("Thresholds"), per component, over the scope and with the tests defined above.

- **Gate:** line coverage (`all`) at or above the line threshold, and `scoreAdjusted` at or above the mutation threshold. Branch coverage is recorded only.
- **Stop at the gate:** once a component meets both thresholds, add no further test and start no further mutation run for it.
- **Shortfall:** after the third survivor round a component still below a threshold is recorded as a shortfall (which threshold, by how many points) in a non-blocking decision and as a finding, and the run continues. Do not change the scope, the tests run or the exclusions to reach a threshold.
