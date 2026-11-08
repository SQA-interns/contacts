# Test strategy (as executed)

> For: agent (writes), human (reads) · Written in: phase 3 (`/acceptance-tests`), phase 5 (`/quality-gate`) · Source: `general/testing/writing-tests.md`, `general/testing/measurement.md`, `project/02_design/quality-requirements.md` · Agent: writes

## Test levels and harness

## Measurement exclusions

<!-- Before any test is written: production code left out of coverage and mutation, one row per path with the reason (generated code, application entry point, style and token files, …). Rules: `general/testing/measurement.md`. Not extended later without a decision record. -->

Decided in phase 3 (session S4 `/acceptance-tests`) before any test was written. Paths are relative to the component folder. Test code (`src/test/**`, `**/*.test.ts(x)`, `src/test/**`, `acceptance/`, `e2e/`, `ui-checks/`) never counts (`general/testing/measurement.md`) and is not listed.

| Path | Reason |
|---|---|
| backend `src/main/java/si/contactgroups/ContactGroupsApplication.java` | Application entry point (`main` only). |
| frontend `src/main.tsx` | Application entry point (mounts the React tree). |
| frontend `src/components/ui/**` | Generated code: shadcn components copied by the `shadcn` generator (ADR-13); already excluded from ESLint for the same reason. |
| frontend `src/styles/**/*.css` (`tokens.css`, `index.css`, `brand.css`, `themes/wireframe.css`, `themes/final.css`) | Style sheets and design-token files: presentation, no behaviour. |
| frontend `src/brand.ts` | Brand token file (product name, logo placeholder, brand colour; UI-03 override in `ui-design.md`): constants only, no logic. |
| frontend `src/i18n/locales/*.json`, `public/**` | Static assets (message catalogs, runtime `config.js` placeholder); their key parity is checked by the catalog test (NFR-11), not by coverage. |

Everything else under `backend/src/main/java` and `frontend/src` is measured. Logic must not be moved into these files (`general/testing/measurement.md`).

## Hidden suite

<!-- Only the run command and the number of hidden tests per story; never the cases. -->

| Story | Hidden tests |
|---|---|

Run command:

## First run of the frozen tests (before any implementation)

| Story | Tests | Failed | Reason (one line per group) |
|---|---|---|---|

## Test measures (phase 5)

<!-- Level rows: tests passed / failed. unit = unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all-levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (raw / adjusted, scope) | Tools |
|---|---|---|---|---|---|---|

## Quality gate (phase 5)

| Component | Line threshold met | Mutation threshold met | Shortfall (points) | Survivor rounds | Tests at first mutation run | Decision |
|---|---|---|---|---|---|---|

## Equivalent mutants (phase 5)

| Component | Class and method | Mutation | Why it cannot change behaviour |
|---|---|---|---|
