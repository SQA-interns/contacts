# Identifiers

> For: agent · Owner: Team lead · Agent: read-only

| Prefix | Meaning | Defined in |
|---|---|---|
| `Q-nn` | Interview item | `project/00_intake/interview.md` |
| `US-nnn` | User story | `project/01_requirements/stories/US-nnn.md` |
| `R-nnn-nn` / `AC-nnn-nn` / `EX-nnn-nn` / `OQ-nnn-nn` | Story rule (EARS) / acceptance criterion / edge case / story open question | the story file |
| `BR-nn` / `OQ-nn` | Cross-story business rule / product open question | `project/01_requirements/business-rules.md` / `intent.md` |
| `E2E-nn` | End-to-end scenario | `project/01_requirements/e2e-scenarios.md` |
| `AR-nn` | Architecture constraint | `project/02_design/architecture.md` |
| `SB-nn` / `SR-nn` | Security baseline / project security requirement | `general/security/` / `project/02_design/security-requirements.md` |
| `ES-nn` | Engineering standard | `general/engineering-standards.md` |
| `UI-nn` | UI baseline rule | `general/design/ui-baseline.md` |
| `NFR-nn` | Non-functional requirement | `project/02_design/quality-requirements.md` |
| `DoR-S-nn` / `DoR-R-nn` | Definition of Ready, story / run | `general/quality/definition-of-ready.md` |
| `DoD-S-nn` / `DoD-I-nn` / `DoD-R-nn` / `DoD-Pnn` | Definition of Done, story / increment / release / project | `general/quality/done-*.md` / `project/01_requirements/intent.md` |
| `ADR-nn` / `T-nn` / `INC-n` | Architecture decision / task / increment | `docs/02_adr/` / `docs/02_tasks.md` |
| `PA-nn` | Pre-approved finding | `project/00_setup/run-config.md` ("Pre-approved findings") |
| `F-nn` / `D-nn` | Finding / decision | `docs/04_review-*.md`, `docs/06_verification-report.md` / `docs/decisions-log.md` |

Tests, commits and findings reference these IDs. A test name carries the `AC`, `EX` or `R` id it checks.
