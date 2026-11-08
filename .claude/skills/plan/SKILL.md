---
name: plan
description: Session S3, phase 2. Turn the ready requirements into a specification with threat model, architecture decision records, interface contracts, component skeletons that build, CI pipeline, deployment files, seed data, architecture tests and a task list that splits the stories into increments, MVP first.
disable-model-invocation: true
---

> For: agent · Owner: Architect · Agent: read-only

## Session

- Id: S3, phase 2. Frame: generate.
- Reads: `docs/01_requirements-check.md` and the resolved decisions in `docs/decisions-log.md` first; then `project/01_requirements/intent.md`, `business-rules.md`, every story; `project/02_design/architecture.md`, `security-requirements.md`, `quality-requirements.md`, `ui-design.md`; `project/00_setup/tech-stack.md`, `environments.md`; `general/security/security-baseline.md`; `general/engineering-standards.md`; `general/design/ui-baseline.md`; `general/rules/commits.md`.
- Writes: `docs/02_specification.md`, `docs/02_contracts/`, `docs/02_adr/`, `docs/02_tasks.md`, repository files (ES-03), CI pipeline (ES-14), component build files, skeletons, deployment files, seed data, architecture tests, `docs/00_progress.md`.
- Must not: write production logic beyond the skeleton.
- Exit: the phase 2 gate holds except the wireframes (they follow in `/wireframes`): every `R`, `AC`, `SR`, `SB` and `NFR` maps to a section of the specification; every interface has a contract that validates; every significant choice has an ADR; every skeleton builds and passes its check command; the CI pipeline runs; every story is in exactly one increment.

## Procedure

1. **ADRs.** For every significant choice (internal architecture per component, persistence, authentication, configuration of variable data, UI structure and routing, error handling, anything a later session could choose differently) write `docs/02_adr/ADR-nn-<slug>.md` in MADR form: context, options, decision, consequences, the requirement IDs that drove it. Prefer the simplest option that meets the requirements.
2. **Specification** (`docs/02_specification.md`):
   - modules and responsibilities per component; data model with validation; behaviour per story (which module implements which rule); error model;
   - domain model: each glossary term with its module, type, table and API name (ES-13);
   - security: controls mapped to `SB` and `SR`, and a **threat model** (STRIDE per interface and per personal-data item), each threat linked to a control or recorded as an `[ASSUMPTION]`;
   - user interface: screen inventory per story, navigation, tokens and components of the baseline with the `ui-design.md` overrides;
   - **NFR checks**: for every `NFR`, the test, script or measurement that checks it and its pass value;
   - traceability: every `R`, `AC`, `EX`, `BR`, `SR`, `SB`, `NFR` → section.
3. **Contracts** (`docs/02_contracts/`): one per interface (for example OpenAPI for REST, JSON Schema for files and configuration). Validate each with a parser; output in `out/logs/`.
4. **Skeleton.** Repository files (ES-03), with `02_output/hidden-tests/` in `.gitignore`. Per component: build file with exactly the pinned versions and its lock file; source skeleton with the folder structure of the specification, configuration (ES-01), health endpoint (ES-09) for a service; for a UI component the token files with the wireframe theme active and the final theme ready (UI-03), the responsive base layout (UI-22..25), the component library, and a message catalog when there is more than one language (UI-30). Fill `commands` (ES-05) and confirm build, test and check pass. Architecture tests for every `AR` a tool can check.
5. **CI** (ES-14): a pipeline (GitHub Actions unless `tech-stack.md` names another) that runs every component's build, test and check commands and the scanners; run it locally once if a runner is pinned, otherwise validate its syntax.
6. **Deployment and data:** the deployment files `architecture.md` names under `deploys_as` (for example a Dockerfile per service and a production compose file), built once; seed data for the local environment with no real personal data, loaded by one command.
7. **Tasks** (`docs/02_tasks.md`): split each story into vertical tasks `T-nn` (each changes behaviour a test can observe) with the rules and criteria it covers and the files it touches; add a task for every NFR check. A story with more than six tasks is split into task groups, one `/implement` session each (`/implement US-003 T-05..T-08`). Group stories into increments `INC-1` (all MVP stories, in dependency order), `INC-2`, … by priority.
8. Commit in the units of `general/rules/commits.md`; hand over with the stories table (next: `/wireframes` when there is a user interface, otherwise `/acceptance-tests`).
