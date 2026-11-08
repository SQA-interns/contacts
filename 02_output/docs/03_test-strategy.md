# Test strategy (as executed)

> For: agent (writes), human (reads) · Written in: phase 3 (`/acceptance-tests`), phase 5 (`/quality-gate`) · Source: `general/testing/writing-tests.md`, `general/testing/measurement.md`, `project/02_design/quality-requirements.md` · Agent: writes

## Test levels and harness

Written in phase 3 (session S4 `/acceptance-tests`) from the stories' Rules, Acceptance criteria and Edge cases, `business-rules.md`, `e2e-scenarios.md`, the contracts and the wireframes; no production code was read. Readings of the harness: D-38. Frozen from the freeze commit (`docs/03_acceptance-manifest.sha256`).

| Level | Where | Interface | Harness | Run |
|---|---|---|---|---|
| Acceptance, backend | `backend/src/test/java/si/contactgroups/acceptance/` (`Us0nn…AcceptanceIT`, support in `support/`) | REST interface of `openapi.yaml` over HTTP | Spring Boot on a random port, Testcontainers PostgreSQL, Firebase Auth emulator on port 9099; empty database and no accounts before every test; movable `Clock` bean; clean-up job every second; xlsx read back with Apache POI; JUnit names carry the AC/EX id | `./mvnw -B verify` (failsafe executions `acceptance` and `acceptance-own-context`; `-DskipAcceptance=true` leaves them out) |
| Acceptance, UI | `frontend/acceptance/` (`playwright.acceptance.config.ts`, support in `acceptance/support/stack.ts`) | the screens of WF-01..WF-19, by role, label and text | Playwright (chromium, 1440 px, `sl-SI`, Europe/Ljubljana) against the test stack; set-up through the REST interface and the emulator; unique addresses per test | `./02_output/scripts/test-stack.sh up`, then `npm run acceptance` in `frontend/` |
| End-to-end | `frontend/e2e/` (E2E-01..E2E-06, one test each; `playwright.e2e.config.ts`) | the browser on the real local stack | the test stack (Compose with emulator overlay and `docker-compose.test.yml`, empty database, throwaway values; no `.env` is read) in chromium, firefox and webkit (NFR-07) | `npm run e2e:scenarios` in `frontend/` |
| UI checks (phase 2 skeleton) | `frontend/ui-checks/` | `vite preview` | Playwright at three widths with axe | `npm run e2e` (CI) |

NFR-10 (number of user actions of E2E-01) is measured in the phase 6 runtime demonstration, not asserted by E2E-01, because the inputs give no fixed list of the counted actions.

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
| US-001 | 15 |
| US-002 | 8 |
| US-003 | 12 |
| US-004 | 9 |
| US-005 | 10 |
| US-006 | 8 |
| US-007 | 8 |
| US-008 | 7 |
| US-009 | 12 |
| US-010 | 10 |
| US-011 | 10 |
| US-012 | 10 |
| US-013 | 9 |
| US-014 | 12 |
| US-015 | 8 |
| US-016 | 8 |
| US-017 | 9 |
| US-018 | 16 |
| US-019 | 7 |
| US-020 | 8 |
| US-021 | 7 |
| US-022 | 7 |
| US-023 | 6 |
| US-024 | 10 |
| US-025 | 5 |
| US-026 | 15 |
| BR-01..BR-07 (shared rules) | 7 |
| Total | 253 |

Every rule `R` and every shared rule `BR` has at least one hidden case. Run command (from the project root): `sh 02_output/hidden-tests/run.sh`; JUnit report `02_output/hidden-tests/target/hidden-junit.xml` (`hidden-tests/hidden.json`). Manifest: `docs/03_hidden-manifest.sha256`.

## First run of the frozen tests (before any implementation)

| Story | Tests | Failed | Reason (one line per group) |
|---|---|---|---|
| US-001 | 25 (7 backend, 18 UI) | 25 | backend: `/me`, `/groups` missing (404 route.not_found); UI: register, verify and sign-in screens missing |
| US-002 | 19 (15 backend, 4 UI) | 19 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-003 | 35 (29 backend, 6 UI) | 35 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-004 | 23 (20 backend, 3 UI) | 23 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-005 | 22 (19 backend, 3 UI) | 22 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-006 | 15 (13 backend, 2 UI) | 15 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-007 | 17 (11 backend, 6 UI) | 17 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-008 | 19 (16 backend, 3 UI) | 19 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-009 | 21 (17 backend, 4 UI) | 21 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-010 | 14 (1 backend, 13 UI) | 14 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-011 | 22 (20 backend, 2 UI) | 22 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-012 | 15 (13 backend, 2 UI) | 15 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-013 | 14 (13 backend, 1 UI) | 14 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-014 | 21 (20 backend, 1 UI) | 21 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-015 | 16 (14 backend, 2 UI) | 16 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-016 | 17 (15 backend, 2 UI) | 17 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-017 | 16 (14 backend, 2 UI) | 16 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-018 | 27 (26 backend, 1 UI) | 27 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-019 | 9 (0 backend, 9 UI) | 8 | UI: language-dependent screens missing; AC-019-03 passes (the skeleton already remembers the language, phase 2) |
| US-020 | 16 (14 backend, 2 UI) | 16 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-021 | 12 (10 backend, 2 UI) | 12 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-022 | 12 (12 backend, 0 UI) | 12 | backend: endpoint missing (404 route.not_found) |
| US-023 | 11 (11 backend, 0 UI) | 11 | backend: endpoint missing (404 route.not_found) |
| US-024 | 19 (17 backend, 2 UI) | 19 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| US-025 | 9 (9 backend, 0 UI) | 9 | backend: endpoint missing (404 route.not_found) |
| US-026 | 20 (15 backend, 5 UI) | 20 | backend: endpoint missing (404 route.not_found); UI: set-up through the missing REST interface fails, screens missing |
| E2E-01..06 | 6 (chromium) | 6 | E2E-01: register screen missing; E2E-02..06: set-up through the missing REST interface (404) |
| Total | 472 | 471 | every failure is a missing endpoint or screen; no build or harness failure |

Run on 2026-11-08 against the phase 2 skeleton: backend `./mvnw -B verify` (failsafe `acceptance` and `acceptance-own-context`, executions of parameterised tests counted one by one; the 19 earlier integration tests still pass); UI `npm run acceptance` and E2E `npm run e2e:scenarios -- --project=chromium` against `scripts/test-stack.sh up`. Hidden suite: 253 cases, 252 failed for the same reasons (missing endpoints, missing screens, missing HTTPS filter), 1 passed (language memory of the skeleton). Not covered by a frozen test: EX-019-03 (D-39).

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
