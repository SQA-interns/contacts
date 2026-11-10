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

Measured in session S6 `/quality-gate` on 2026-11-09 and 2026-11-10. Scope of every figure: production code of the component without the measurement exclusions above. Backend line and branch figures are JaCoCo 0.8.13 counters over all classes (2,518 lines) except `ContactGroupsApplication`; frontend figures are Vitest 4.1.12 with `@vitest/coverage-v8` over `src/` (1,484 lines) without the exclusions.

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (raw / adjusted, scope) | Tools |
|---|---|---|---|---|---|---|
| backend | unit | 183 / 0 | 29.23 % (736 of 2,518) | 45.40 % | | JUnit 5, Surefire, JaCoCo (`jacoco.exec`) |
| backend | architecture | 14 / 0 | (in the unit run) | (in the unit run) | | ArchUnit |
| backend | integration | 240 / 0 | | | | Failsafe, Testcontainers |
| backend | acceptance | 369 / 2 (371 run) | | | | Failsafe, Testcontainers, Firebase Auth emulator |
| backend | non-unit (integration and acceptance) | 609 / 2 | 95.99 % (2,417 of 2,518) | 85.15 % | | JaCoCo (`jacoco-it.exec`) |
| backend | **all levels** | 806 / 2 | **97.62 %** (2,458 of 2,518) | 90.24 % | **97.37 % / 97.37 %**, 963 of 989 mutants killed (timed out counted as killed), equivalent 0, 969 reached (`scoreCovered` 99.38 %); scope: all of `src/main/java/si/contactgroups` except `ContactGroupsApplication`; tests: the full suite in five PIT runs (see "Phase 5 notes") | PIT 1.20.3, pitest-junit5-plugin 1.2.3, JaCoCo 0.8.13 (`jacoco-all.exec`, merge of the two) |
| frontend | unit | 585 / 0 | **97.77 %** (1,451 of 1,484) | 94.78 % | | Vitest, Testing Library, msw, `@vitest/coverage-v8` |
| frontend | non-unit (Playwright acceptance and end-to-end) | not re-run in S6 (S14: acceptance 88 passed, 7 known failures; E2E chromium 4 of 6) | not measured (the browser tests run against the built container images, which carry no coverage instrumentation) | not measured | | Playwright |
| frontend | **all levels** | 585 / 0 (unit only) | 97.77 % (unit only; the merged figure is at least this) | 94.78 % (unit only) | **82.43 % / 82.43 %**, 3,588 of 4,353 mutants killed (3,540 killed, 48 timed out), equivalent 0, 4,282 reached (`scoreCovered` 83.79 %); scope: all of `src/` without the measurement exclusions; tests: the full unit suite (`perTest` coverage analysis) | Stryker 9.6.1, `@stryker-mutator/vitest-runner` 9.6.1 |

The two failing backend acceptance tests are known and not caused by S6: `EX-007-05` (the frozen test contradicts R-007-03, D-51) and `AC-018-15` (security log; it failed in the Failsafe run of S6 and passed inside PIT). Both are listed in the progress file.

## Quality gate (phase 5)

| Component | Line threshold met | Mutation threshold met | Shortfall (points) | Survivor rounds | Tests at first mutation run | Decision |
|---|---|---|---|---|---|---|
| backend | yes, 97.62 % (>= 95 %) | yes, 97.37 % (>= 90 %) | none | 0 | 197 Surefire (183 unit, 14 architecture), 240 integration, 371 acceptance (1 of them failing and skipped by PIT) | D-81 (how PIT ran) |
| frontend | yes, 97.77 % (>= 95 %; unit tests alone) | **no**, 82.43 % (< 90 %) | mutation 7.57 points (330 more mutants to kill: 3,918 of 4,353 are needed, 3,588 are killed) | 3 (first full run 66.92 %, second full run after three rounds 82.43 %) | 376 unit | D-82; finding F-38 |

## Equivalent mutants (phase 5)

No survivor is counted as equivalent in either component: the backend meets the threshold on the raw score, and the frontend is a shortfall on the raw score. Both adjusted scores therefore equal the raw ones. Survivors that are plainly equivalent in kind (not counted, so not justified here): React Query `queryKey` string and array literals that the page uses consistently, and the empty-string default of `useParams()`.

| Component | Class and method | Mutation | Why it cannot change behaviour |
|---|---|---|---|

## Phase 5 notes

- **Configuration** (commits `b2d39c2`, `0009f91`, each `build:` alone): JaCoCo excludes only the application entry point and writes a merged report (`target/jacoco-all.exec`, `target/site/jacoco-all`) with a 95 % line check; PIT is limited to `si.contactgroups.*` without `ContactGroupsApplication*`, threshold 90, `skipFailingTests`; Vitest coverage uses exactly the exclusions of the table above; Stryker mutates `src/` without tests, `src/test`, `src/main.tsx`, `src/components/ui` and `src/brand.ts` (the style and locale files are not TypeScript), `coverageAnalysis: perTest`, no `break` threshold (the adjusted score is computed by hand). No threshold, scope or exclusion was changed to improve a number.
- **PIT could not run the whole backend suite in one JVM** (D-81). The Spring contexts of the other integration tests (`ApplicationContextSupport`) and of the acceptance tests each register the one named `FirebaseApp` the product allows per JVM, the two acceptance classes `Us018*OwnContext*` need a JVM of their own, and the Firebase Auth emulator uses the fixed port 9099. PIT's baseline run therefore failed all 371 acceptance tests. The full suite was run in five PIT runs with disjoint test sets (unit `*Test` without architecture; integration `*IT` outside `acceptance/`; acceptance without the two own-context classes; `Us018HttpsOwnContextAcceptanceIT`; `Us018RateLimitOwnContextAcceptanceIT`), each over all of `targetClasses`, one thread, `FIREBASE_AUTH_EMULATOR_HOST=localhost:9099` exported in the shell. The five `mutations.xml` files were merged by mutant (class, method, line, mutator, index, description): a mutant is killed if any run killed or timed it out. Run results before merging: unit 358 killed, integration 708, acceptance 925, HTTPS class 61, rate-limit class 209, of 989 mutants. Timeout constant 240 s, factor 3, so that the start of a Spring context is not counted as a timeout.
- **The frontend nonUnit coverage was not measured** (D-81 item b): the Playwright tests run against the production build in containers, and instrumenting them would be a different tool and build.
- **Frontend survivor rounds** (tests only, no production code changed): round 1 `src/api`, `config`, `filters`, `rules`, merge request, plural and date helpers (the 641 mutants of these files reached 96.1 %, 616 killed, in the check run); round 2 the activity, contact table, invitation form, multi-select, tags, trash and versions screens (the 1,060 mutants of these files reached 83.1 % in the check run); round 3 the members, invitations, group trash, filter panel, contact list and contact form screens. 209 new tests in 18 new test files and one extended; the unit suite grew from 376 to 585 tests and the line coverage from 95.28 % to 97.77 %. The remaining survivors are in the screens (class names and other texts a test does not read, defaults of `useParams`, query keys), above all `ActivityPage`, `ContactPage`, `ContactListPage`, `TagListPage`, `MergeForm`, `MergeTable`, `app/routes.tsx`, `RegisterPage`, `AccountPage` and `CreateGroupDialog`.
- **Observation while writing tests (F-39):** `TagListPage` asks for the Tags before the Group list has told it the Role (`enabled: role !== 'VIEWER'` is true while the Role is unknown), so a Viewer's browser makes one request that the backend refuses. Nothing is shown to the Viewer. Not fixed in S6 (production code).
- **Commit size:** the three frontend test commits of the survivor rounds (`ed6a1a7`, `94632cf`, `5860c10`) hold 597, 1,670 and 1,340 added lines, over the 400 lines of `commits.md` for a survivor round; history is not rewritten (D-82 item c).
- **Logs:** `out/logs/quality-*.log` (backend verify, PIT partitions A1, A2, B, C1, C2, Stryker runs, frontend unit with coverage). The PIT and Stryker reports are in `backend/target/pit-reports/` and `frontend/reports/mutation/` and are not committed.
