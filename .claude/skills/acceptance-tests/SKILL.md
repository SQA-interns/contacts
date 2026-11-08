---
name: acceptance-tests
description: Session S4, phase 3. Write and freeze the black-box acceptance tests from every story's acceptance criteria and edge cases, one end-to-end test per scenario, and the hidden held-out suite, against the contracts and the approved wireframes, before any production code exists.
disable-model-invocation: true
---

> For: agent · Owner: QA lead · Agent: read-only

## Session

- Id: S4, phase 3. Frame: generate.
- Reads: in every story only "Rules", "Acceptance criteria" and "Edge cases"; `project/01_requirements/e2e-scenarios.md`; `project/01_requirements/business-rules.md`; `docs/02_contracts/`; the specification sections the stories cite; `docs/wireframes/README.md` and the wireframe HTML of each screen (for labels, roles and headings); `project/00_setup/environments.md`; `general/testing/writing-tests.md`; `general/testing/measurement.md` ("Measurement constraints"); `general/testing/hidden-evaluation.md`; `general/rules/commits.md`.
- Writes: acceptance and e2e tests, the test harness, the hidden suite in `02_output/hidden-tests/`, `docs/03_test-strategy.md`, `docs/03_acceptance-manifest.sha256`, `docs/03_hidden-manifest.sha256`, `docs/00_progress.md`.
- Must not: read or write production source code. *Why:* tests written from the code test what the code does, not what the customer asked for.
- Exit (phase 3 gate): every AC and EX has a test; every `R` and `BR` has a hidden test; every e2e scenario has exactly one e2e test and there is no other; every test fails for a behavioural reason; the two manifests are committed last, alone, as the freeze commit.

## Procedure

1. Before any test: write the measurement exclusions (generated code, entry point, style and token files) with a reason each in `docs/03_test-strategy.md`.
2. Set up the harness per level (acceptance through the public interface; e2e through the real local stack, the seed data and the browser), using the local substitutes in `environments.md`.
3. For every `AC` and `EX`, write at least one test through the public interface that checks the observable outcome; for a rejection also check that nothing changed. ID in the test name; every expected value from the story, a business rule or a project-wide fact. UI tests find elements by the role, label or text the wireframe shows.
4. For every e2e scenario, exactly one end-to-end test; no other, none split or merged.
5. Run the suite: every test must fail because the behaviour is missing, not because of the build or harness. Record counts and one reason line per group in `docs/03_test-strategy.md`.
6. Self-check every assertion and helper: a correct implementation can pass it; it cannot pass or fail for the wrong reason; no assertion contradicts another. *Why:* a defective frozen test costs an approval.
7. Format and lint the tests now; after the freeze they cannot change.
8. Write the hidden suite as `general/testing/hidden-evaluation.md` describes, with the write tool only. Run it once: it must fail for behavioural reasons and write its JUnit report. Write `docs/03_hidden-manifest.sha256`. Record only the number of hidden tests per story, nowhere the cases.
9. Commit the visible tests story by story (`test:`), then the e2e tests; then write `docs/03_acceptance-manifest.sha256` and commit both manifests alone as the freeze commit.
10. Hand over (next: `/implement <first story of INC-1>`). The runner then moves the hidden suite out of the project.
