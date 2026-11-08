---
name: to-spec
description: Intake session T. Turn the interview notes into every project input file of the template (intent, context, one file per user story with EARS rules, acceptance criteria and edge cases, business rules, scope, e2e scenarios, design, setup), marking every unconfirmed value, and write a review checklist for the developer. Use after /grill-me.
disable-model-invocation: true
---

> For: agent · Owner: Product owner · Agent: read-only

## Session

- Id: T, intake. Frame: generate.
- Reads: `project/00_intake/interview.md`; the templates in `project/01_requirements/`, `project/02_design/`, `project/00_setup/`; `general/requirements-standard.md`; `general/quality/definition-of-ready.md`; `general/design/ui-baseline.md` ("Stack" only).
- Writes: every file in `project/` except the interview; `project/00_intake/review-checklist.md`.
- Must not: add behaviour the interview does not give without marking it `[ASSUMPTION]`; resolve an `OPEN` item.
- Exit: every project file is filled, and the review checklist lists every `[ASSUMPTION]` and `OPEN` item with its file and ID.

Write the project files in English; keep customer terms and quoted text in their language.

## Rules

- **Source every statement.** Each story lists its `sources` (Q ids); each rule, value and limit comes from an answer. A value the interview does not give is written with `[ASSUMPTION]` and a one-line reason; an unanswered question becomes `OPEN` in the story's (or `intent.md`'s) open questions. Never resolve an `OPEN` item yourself.
- **Answers from the developer** (source `developer`) are written as given but listed in the review checklist; on a `Block` topic they stay `OPEN — needs customer`.
- **One fact, one place** (`general/requirements-standard.md`, "Where each kind of requirement lives"). Project-wide facts go to `business-rules.md` (project-wide facts) only.
- **Fill the templates as they are**: keep headings, tables, IDs and front matter; replace every `<…>` placeholder; delete the `<!-- … -->` comments; write "None." in a section that does not apply.

## Procedure

1. Read the interview and list its topics, answered and open.
2. `intent.md`: problem, product, users, core outcome, MVP, scope and non-goals, principles, product-level open questions, project done criteria `DoD-P` with how each is checked.
3. Stories: one file per user goal, `stories/US-001.md` onwards, copied from the `US-001.md` template. For each:
   - story sentence, purpose, definitions;
   - rules in EARS, one per statement;
   - acceptance criteria in Given / When / Then with the interview's example values; cover the main flow, every `If … then` rule and every boundary;
   - edge-case table for boundaries, errors and lifecycle;
   - out of scope; open questions;
   - front matter: priority from the interview, `mvp`, `estimate` in points (1, 2, 3, 5, 8; split anything larger than 8 by the patterns in the standard), `depends_on`, `rules`, `sources`. Progress is tracked in `docs/00_progress.md`, never in the story.
   Check INVEST and the ISO 29148 properties for every story and rule; split or rewrite until they hold.
4. `business-rules.md`: every project-wide fact (`[ASSUMPTION]` where none was given, for example the customer's local time zone), shared rules in EARS, the data model (entities, fields, formats, constraints, lifecycle), and the glossary as the ubiquitous language: one name per concept, its meaning, its name in code, the synonyms not to use. Then rewrite every story to use only glossary terms.
5. `e2e-scenarios.md`: one scenario per main journey (from topic 16 and every MVP story), each naming its stories and criteria.
6. `02_design/`:
   - `architecture.md`: components, constraints and interfaces implied by the stories and topic 13;
   - `security-requirements.md`: level, roles, personal data with purpose and retention, `SR` items;
   - `quality-requirements.md`: NFRs with how each is checked; thresholds `>= 95 %` line coverage and `>= 90 %` adjusted mutation score unless the interview gives others; mutation scope = all production code of each component;
   - `ui-design.md`: only what topic 15 gave (brand colour, logo, tone, devices, modes, references), each as an override of a `UI-nn` rule; otherwise "None. The UI baseline applies.".
7. `00_setup/`:
   - `tech-stack.md`: the stack from topic 13, or a mainstream default for the architecture, including the UI stack of `UI-01`, an accessibility checker and a browser test runner; each version the latest stable you know, marked `[ASSUMPTION] verify in preflight`; tooling for every required purpose;
   - `environments.md`, `secrets.env.example` (names only);
   - `run-config.md`: leave the run label empty; leave "Pre-approved findings" empty unless the interview gives decided findings; keep the default model and effort per session; fill "Allowed commands" with the build, test, check, scan and run commands of the stack (for example `Bash(./mvnw *)`, `Bash(python3 -m pytest *)`), never a shell, `curl`, `rm -rf` or `sudo`; list them in the review checklist.
8. Self-check against `DoR-S` and `DoR-R` and fix what you can; what you cannot fix becomes an item in the checklist.
9. Write `review-checklist.md`: a table of every `[ASSUMPTION]`, every `OPEN` item and every developer-sourced answer, with file, ID, the question for the developer and a proposed answer; then the list of stories with priority, MVP and estimate. Tell the developer the next step is the review (with `/run` it asks the questions).
