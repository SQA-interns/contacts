# Quality requirements

> For: agent (reads), human (reviews) · Owner: QA lead · Agent: read-only

Adds to `general/quality/` (definition of done, test strategy, severity scale); do not repeat those here. Project done criteria (`DoD-P`) are in `project/01_requirements/intent.md`.

## Non-functional requirements

| ID | Category (performance, availability, accessibility, localisation, usability, …) | Requirement | How it is checked |
|---|---|---|---|
| NFR-01 | | | |

## Thresholds

These thresholds are the **quality gate** of the project, per component, over the scope and with the tests defined in `general/testing/measurement.md`. The agent works until each component meets them, then stops adding tests for that component (rules and stop rule: "Quality gate" in `general/testing/measurement.md`). Keep the format `>= <number> %`.

| Measure | Threshold |
|---|---|
| Line coverage (all levels together) | >= <number> % |
| Mutation score (adjusted) | >= <number> % |
| Branch coverage | record only |

## Mutation scope

The paths or packages whose code is mutated (default: all production code of the component). Selection rule: validation, security, persistence and business-rule code. Rules: `general/testing/measurement.md`.

| Component | Paths or packages | Exclusions and reason |
|---|---|---|
| <component> | | |

## Overrides

None.
