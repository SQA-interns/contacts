# Project template

> For: human · Owner: Team lead

A reusable structure that takes a project from the customer's requirements to a working, tested, secure and deployable solution with one coding agent, in a sequence of short sessions (spec-driven development). You interview the customer with `/grill-me`, start `/run`, and then only answer questions. How to run it: `Run_Guide.md`.

## Who each part is for

| Part | For | Content |
|---|---|---|
| `README.md`, `Run_Guide.md` | human | what the template is and how to run it; agents never read them |
| `AGENTS.md` (+ `CLAUDE.md`, which loads it in Claude Code) | agent | what every session needs: paths, precedence, session rules, handover, prohibitions |
| `.claude/skills/` (`.agents/skills` links to it for Codex) | agent | one skill per session: its "Session" block (reads, writes, must not, exit) and its procedure |
| `01_input/00_general/` | agent | rules read only by the sessions that list them: `rules/`, `testing/`, `quality/`, `security/`, `design/`, standards |
| `01_input/01_project/` | agent reads, human reviews | the project: intake notes, setup, requirements, design |
| `02_output/` | agent writes, human reads | documents, code, logs; `docs/wireframes/` for the approved wireframes |
| `system/run.mjs` | system | the runner: starts sessions, keeps and runs the hidden tests, `--summary`, `--lint-template`; nobody reads it during a run |

Every Markdown file states its audience in the line `> For: …`. A session reads `AGENTS.md`, `docs/00_progress.md`, its skill and only the files its skill lists. *Why:* less reading means less cost, a smaller context and fewer chances to follow the wrong rule.

## Flow

| Phase | Session | Skill | Ends when |
|---|---|---|---|
| — | C | `/run` (optional conductor) | the runner reports `complete` |
| Intake | G, T | `/grill-me`, `/to-spec`, then your review | inputs reviewed and committed |
| 0 Preflight | S1 | `/preflight` | tools, secrets, dependencies ok; inputs frozen |
| 1 Clarify | S2 | `/clarify` | every story meets the Definition of Ready |
| 2 Plan | S3, W | `/plan`, `/wireframes` | specification, contracts, ADRs, skeletons, CI, tasks; wireframes approved |
| 3 Acceptance tests | S4 | `/acceptance-tests` | visible and hidden tests frozen |
| 4 Implement | S5, R, D | `/implement US-nnn` per story, `/review INC-n` per increment, then `/design` | every story done; the grey app matches the wireframes (milestone); final design applied |
| 5 Quality gate | S6 | `/quality-gate` | coverage and mutation thresholds met |
| 6 Verify | S7, S8 | `/verify`, `/fix` (at most three rounds) | release Definition of Done holds |
| 7 Release | S9 | `/release` | READMEs work, release notes and retrospective written |

Entry and exit at every level: `01_input/00_general/quality/definition-of-ready.md`, `done-story.md`, `done-increment.md`, `done-release.md`. Design stages: `01_input/00_general/design/ui-baseline.md`.

## Agents

The rules, inputs, documents and skills are plain Markdown and work with any coding agent. Each session can use its own agent, model, effort and turn limit (`01_input/01_project/00_setup/run-config.md`, "Sessions" and "Agents").

| Part | Claude Code | Other agents (Codex, a local model, another harness) |
|---|---|---|
| Instructions | `CLAUDE.md` loads `AGENTS.md` | read `AGENTS.md` |
| Starting a skill | `/name args` | "follow `.claude/skills/<name>/SKILL.md` …", or `$name` |
| Permissions | the allowed commands in `run-config.md` | the agent's own sandbox |

The hidden evaluation is run by `system/run.mjs` itself, so it does not depend on what an agent can be forbidden to read.

## 1. Filled in by the intake, reviewed by you

Every file in `01_input/01_project/`: `00_intake/` (`customer_requirement/` with everything the customer gave you, interview, review checklist), `00_setup/` (run configuration, secrets list, tech stack, environments), `01_requirements/` (intent, stories, business rules, end-to-end scenarios), `02_design/` (architecture, security, quality, UI design). What goes in each is stated in the file itself.

## 2. Template files (change only when revising the template)

`README.md`, `Run_Guide.md`, `AGENTS.md`, `CLAUDE.md`, `system/`, `.claude/skills/`, `.agents/`, and everything in `01_input/00_general/`.

## 3. Written during the run

- `02_output/`: documents in `docs/`, logs in `logs/`, all source code and tests, the hidden suite (`01_input/00_general/testing/hidden-evaluation.md`).
- Repository files at the root: `.gitattributes`, `.gitignore`, the CI pipeline, deployment files.

Any change to sections 1 and 2 after `/preflight` is detected by `docs/00_input-manifest.sha256`.

## Rules for editing this template

- **One audience per file**, stated in `> For: …`. Human explanation goes only in `README.md` and `Run_Guide.md`; agent rules and procedures never explain things to humans. *Why:* mixed files cost every reader the part meant for the other.
- **Read only what you need.** Put what every session needs in `AGENTS.md` (loaded once); everything else in small files that only the sessions needing them list. A skill's "Reads" names exact files, never whole folders.
- **At most 200 lines per document.** Split a file when it grows past that or covers two topics.
- **One fact, one place.** State each rule, value or list once and refer to it ("the three widths (UI-22)"). *Why:* copies drift apart.
- **No conflicts.** Before adding a rule, search for the topic and change the existing rule instead of adding a second one.
- Constraints go in `01_input/00_general/`, procedures and what a session reads and writes in its skill, versions only in `tech-stack.md`.
- Before committing a change to the template, run `node system/run.mjs --lint-template` (lengths, headers, references, unique IDs, skills without a run-config row).
