# Definition of Ready

> For: agent · Owner: Product owner · Agent: read-only

When a story, and the run as a whole, may enter planning. Checked in phase 1; the result per story is in `docs/01_requirements-check.md`. Acceptance criteria belong to one story; the Definition of Done (`done-story.md`, `done-increment.md`, `done-release.md`) is the exit; this file is the entry.

## Story (`DoR-S`)

| ID | Criterion |
|---|---|
| DoR-S-01 | The story has the structure of `general/requirements-standard.md`, with a role, a goal and a benefit, and a Purpose. |
| DoR-S-02 | The story meets INVEST; it is small enough for one implement session. |
| DoR-S-03 | Every rule is one EARS statement that meets the ISO 29148 properties. |
| DoR-S-04 | It has at least one acceptance criterion; every criterion is Given / When / Then with concrete values and cites its rules. |
| DoR-S-05 | Every `If … then` rule and every boundary has a row in the edge-case table. |
| DoR-S-06 | "Out of scope" is filled. |
| DoR-S-07 | No `OPEN` item on a `Block` topic; every other `OPEN` item has a proposed default recorded as a decision. |
| DoR-S-08 | Every ID the story cites exists; every dependency is a story that exists. |
| DoR-S-09 | Every `[ASSUMPTION]` is either confirmed by the human or recorded as a decision with its class. |
| DoR-S-10 | A story with UI names its screens; each screen can follow `general/design/ui-baseline.md` or `project/02_design/ui-design.md`. |
| DoR-S-11 | The story uses only glossary terms; every domain term it needs is in the glossary. |

## Run (`DoR-R`)

| ID | Criterion |
|---|---|
| DoR-R-01 | `intent.md` states the problem, the users, the core outcome, the MVP, the non-goals, the principles and the project done criteria (`DoD-P`). |
| DoR-R-02 | `business-rules.md` ("Project-wide facts") gives every project-wide fact the stories rely on. |
| DoR-R-03 | Every `must` story and every MVP story is ready (`DoR-S`). |
| DoR-R-04 | Every e2e scenario names the stories and criteria it covers, and every MVP story is in at least one scenario. |
| DoR-R-05 | Every NFR, SR and DoD-P item says how it is checked. |
| DoR-R-06 | The phase 0 gate has passed. |
