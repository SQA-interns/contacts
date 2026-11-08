# Writing tests

> For: agent · Owner: QA lead · Agent: read-only

How tests are written and run. Thresholds and figures: `general/testing/measurement.md`.

## Test levels

| Level | Written in phase | Written from | Frozen | Location |
|---|---|---|---|---|
| Acceptance | 3 | story AC and edge cases + contracts | yes, from the freeze commit | path contains `acceptance` |
| End-to-end | 3 | `project/01_requirements/e2e-scenarios.md` (one test per scenario) | yes, from the freeze commit | path contains `e2e` |
| Unit / integration | 4 (test-first), 5 (survivors only) | story rules and the code being written | no | any other test path |
| Architecture | 2 (rules), 4 | `project/02_design/architecture.md` (`AR`) | no | path contains `architecture` |

## Constraints

- Do not test acceptance behaviour through internals; use the component's public interface (API, UI, CLI, messages, files). *Why:* the customer sees only the interface; tests on internals break on every refactoring and miss what users see.
- Do not depend on live external services in automated tests; use the local substitutes in `project/00_setup/environments.md` and list the real services in the release notes for manual testing. *Why:* live services make tests slow, flaky and costly, and may act on real data.
- Do not leave flaky, skipped or disabled tests; fix them or record a decision. *Why:* a test that may fail teaches everyone to ignore failures.
- Do not weaken an assertion to make an incorrect implementation pass. *Why:* the test would then certify the defect.
- Test names contain the `AC`, `EX` or `R` id they verify. Every expected value has a source in the inputs. *Why:* traceability, and a test with an invented value checks the agent's assumption, not the requirement.
- UI tests find elements by role, label or text, never by CSS class or layout, so a style change cannot break them. *Why:* the look changes after the tests are frozen (`/design`).

## Test-driven implementation (phase 4)

For every rule of a story: write a failing unit or integration test (red), write the least code that makes it pass (green), then refactor with the tests green (refactor). Production code changes only to make a failing test pass or during refactoring. Never add behaviour in the same step as a refactoring.

## Failing tests during implementation

Classify a failure before acting on it:

| Class | Action |
|---|---|
| Implementation defect | fix the code |
| Defect in a non-frozen test | fix the test and say so in the commit message |
| Frozen test appears wrong | blocking decision; do not touch the test |

## UI checks

For every component with a user interface:

- an automated accessibility check (axe or equivalent) on every screen and state; Critical and Serious violations are High findings;
- screenshots of every screen at the three widths (UI-22), saved under `out/logs/screens/` and compared with the approved wireframes (`docs/wireframes/`) and, after `/design`, with the UI baseline.

## Manifest

Both test manifests (`docs/03_acceptance-manifest.sha256`, and the hidden one) use `sha256sum` format with paths relative to `02_output/`, hashed after LF normalisation.
