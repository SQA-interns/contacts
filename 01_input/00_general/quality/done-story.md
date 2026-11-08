# Definition of Done: story (`DoD-S`)

> For: agent · Owner: QA lead · Agent: read-only

Checked by `/implement` at the end of every story. Entry condition: `general/quality/definition-of-ready.md`.

| ID | Criterion |
|---|---|
| DoD-S-01 | Every frozen acceptance test of the story passes; no previously passing test fails. |
| DoD-S-02 | Every rule of the story is covered by at least one unit or integration test written before the code; each `feat` commit carries its `Red:` lines (`general/rules/commits.md`). |
| DoD-S-03 | Build, format, lint, type check and static analysis are clean for the changed code; the secret scan finds nothing. |
| DoD-S-04 | The code was refactored after the tests passed (no duplication, names express intent, no dead code), with the tests still green. |
| DoD-S-05 | Every screen of the story matches its approved wireframe at the three widths (UI-22) in the wireframe theme, has its loading, empty, error and success states, and passes the automated accessibility check. |
| DoD-S-06 | Every value in code and tests has a source; every `[ASSUMPTION]` made is in the decision log. |
| DoD-S-07 | Commits follow `general/rules/commits.md` and name the story's IDs. |
