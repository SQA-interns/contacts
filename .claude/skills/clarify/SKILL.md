---
name: clarify
description: Session S2, phase 1. Criticise the requirements without implementing anything - check every story against the Definition of Ready, ISO 29148, INVEST and EARS, list what is unknown, and turn every OPEN item and assumption into a confirmed value, a decision or a question for the human.
argument-hint: "[answers to open decisions, e.g. 'D-03: 2']"
disable-model-invocation: true
---

> For: agent · Owner: Product owner · Agent: read-only

## Session

- Id: S2, phase 1. Frame: criticise. "Do not implement. List what you do not know."
- Reads: `project/01_requirements/intent.md`, `business-rules.md`, `e2e-scenarios.md`, every `stories/US-nnn.md`; `project/02_design/security-requirements.md`, `quality-requirements.md`; `general/requirements-standard.md`; `general/quality/definition-of-ready.md`; `general/rules/decisions.md`; `general/rules/identifiers.md`.
- Writes: `docs/01_requirements-check.md`, `docs/decisions-log.md`, `docs/00_progress.md`.
- Must not: propose designs or code; change inputs (a needed change is a decision).
- Exit (phase 1 gate): every story meets `DoR-S`, the run meets `DoR-R`, and no `Block` decision is open.

## Procedure

1. If the argument answers open decisions, record each as `Human response` and resolution, then continue with the stories still not ready.
2. For each story, check every `DoR-S` item and, for every rule and criterion, the nine ISO 29148 properties. For each failure write one line: ID, property or DoR item, what is missing, the consequence of reading it wrongly.
3. For each story, ask the gap questions of the standard (event and timestamp, exceptions and lifecycle, combinations, limits, roles, data). A gap whose readings would change behaviour or a business outcome is a finding; ignore the others.
4. Classify every finding, `OPEN` item and `[ASSUMPTION]`: Allow (the inputs settle it; cite where), Open (non-blocking decision with the most conservative reading), Block (blocking decision with numbered options and a default).
5. Check `DoR-R` and cross-file consistency: one name and one meaning per glossary term (synonyms are findings); every cited ID exists; no fact in two places; no story contradicts a business rule or a project-wide fact; every MVP story is in an e2e scenario.
6. Write `docs/01_requirements-check.md`: per story the DoR result and failing items, the story → rules → criteria map, the findings with their decision ids. Do not copy the requirements.
7. A `Block` decision without answer: ask all of them in one message, commit, set "Waiting for" with next command `/clarify`, end the session.
8. When every story is ready: commit (`docs:`), hand over with every story `ready` (next: `/plan`).
