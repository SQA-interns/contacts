# Hidden evaluation

> For: agent · Owner: QA lead · Agent: read-only

Read only by `/acceptance-tests`, which writes the suite. Every other session knows only the rule in `AGENTS.md`.

Besides the visible frozen tests, `/acceptance-tests` writes a **hidden suite**: held-out cases that no implementing or checking agent ever sees, so an implementation can pass only by meeting the rules, not by fitting the visible examples. *Why:* an agent that sees every test can, without meaning to, write code that satisfies the tests instead of the requirement; green visible tests then prove only the agent's own assumptions.

- Content: for every rule `R` and shared rule `BR` at least one case the visible tests do not contain: other values than the acceptance-criteria examples, both sides of every boundary, combinations of rules, invalid input, other roles. Expected values come only from the inputs.
- Form: self-contained in `02_output/hidden-tests/` (its own configuration, using the components' public API or service interface), with `hidden.json` = `{"command": "<shell command run from the project root>", "junit": "<path of the JUnit XML report it writes>"}`. Each test name contains the rule id and one category: `boundary`, `combination`, `invalid`, `role` or `other` (for example `R-003-02 boundary above limit`).
- Frozen like the visible tests, with its own manifest `docs/03_hidden-manifest.sha256` (paths relative to `02_output/`).
- **The runner owns it, not an agent.** Right after `/acceptance-tests`, `system/run.mjs --vault` moves the suite out of the project into the hidden vault (`run-config.md`). Before every `/verify`, `system/run.mjs --hidden-eval` copies the project to a temporary folder, adds the suite, runs it there, checks the manifest and writes only the rule id and category of each failure to `docs/06_hidden-evaluation.md`. Before `/release`, `system/run.mjs --restore-hidden` puts it back so `/release` commits it. `/run` does these steps automatically.
- Not part of coverage or mutation: the quality gate measures the visible suite.
