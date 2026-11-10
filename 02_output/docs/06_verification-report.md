# Verification report

> For: agent (writes), human (reads) · Written in: phase 6 (`/verify`; `/fix` fills only the resolution column) · Source: `general/quality/*`, `general/security/*`, `general/design/ui-baseline.md`, `project/*` · Procedure: `skills/verify` · Agent: writes

One section per `/verify` run (`## Run 1`, `## Run 2`, …), each a self-check in a separate session, not an independent review.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|

## UI and accessibility

| Screen | Widths and modes | Baseline result | Accessibility violations |
|---|---|---|---|

## NFR

| NFR | Measured | Pass value | Result |
|---|---|---|---|

## Threats and controls

| Threat | Control | How checked | Result |
|---|---|---|---|

## CI and deployment

## Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|

## Traceability

| R / AC / EX | Tests | Commits |
|---|---|---|

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|

## Run 1

> **Self-check by the agent in a separate session (S7), not an independent review.** Commit under test: `3c49bdb` (code identical to `HEAD`; later commits only add documents). Result: **phase 6 gate not passed**: 8 High findings, one Critical-by-rule finding that the human lowered to Low (D-83, option 1), and several Medium ones are open. Logs: `out/logs/verify-*.log`. Ad hoc scripts (NFR measurement, traceability count) were written to a scratch folder outside the project, so the missing `perf/measure.mjs` (F-46) is not replaced by this run.

### Procedure result

| Step | Result |
|---|---|
| 1 run number | run 1; nothing stayed open for three runs |
| 2 manifests | input manifest 77 of 77 and acceptance manifest 59 of 59 hashes match (`verify-manifests.log`); the hidden manifest is checked by the runner (`06_hidden-evaluation.md`: "Manifest: ok"); this session may not read `hidden-tests/` |
| 3 suite, checks, hidden evaluation | see "Definition of Done"; hidden evaluation of `3c49bdb`: 214 tests, 209 pass, 5 fail (F-41..F-45); coverage and mutation not re-measured: no component changed since phase 5 |
| 4 runtime | test stack (Compose, emulator, empty database) and production stack (below) |
| 5 NFR | measured, see "NFR" |
| 6 UI | see "UI and accessibility" |
| 7 security | see "Threats and controls" |
| 8 logs, errors, secrets | logs and error bodies clean; `.env` scan: see F-49 and D-83 |
| 9 CI, deployment | see "CI and deployment" |
| 10 traceability | see "Traceability" |

### Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-R-01 full suite passes, hashes match | **Not met** | hashes match. Backend 808 tests (197 unit, 240 + 366 + 5 integration): 807 pass, `EX-007-05` fails (F-08, D-68). Frontend unit 585 of 585 pass. UI acceptance 88 of 95 pass (7 known failures, F-47). E2E scenarios: chromium 4 of 6, webkit 4 of 6 (E2E-01 and E2E-03 fail, F-40), firefox 0 of 6 (browser does not start, F-48). `verify-backend-quality.log`, `verify-ui-acceptance.log`, `verify-e2e-scenarios.log`, `verify-frontend-test.log` |
| DoD-R-02 format, lint, type check, static analysis | Met | Backend: Spotless, Checkstyle (0 violations), SpotBugs with find-sec-bugs (0 bug instances), PMD all pass (`verify-backend-static.log`; they are bound after Failsafe, so the failing frozen test stops `verify -Pquality` before them and they were run directly). Frontend: Prettier, ESLint (0 errors, 1 warning, F-54), `tsc -b`, i18n tests pass (`verify-frontend-check.log`) |
| DoD-R-03 quality gate | Met with a decision | backend 97.62 % line, 97.37 % mutation; frontend 97.77 % line, 82.43 % mutation (shortfall 7.57 points after three rounds, D-82, F-38 Medium). Not re-measured (no change since S6) |
| DoD-R-04 architecture constraints, no cycles | Met | `ArchitectureTest` 6, `LayerRulesTest` 2, `RepositoryScopeRuleTest` 2, `EndpointSecurityTest` 4: all pass |
| DoD-R-05 no open Critical or High | **Not met** | F-40..F-46 (High); F-49 was Critical by rule and is Low since D-83 |
| DoD-R-06 components run, core flows work | Partly | all components ran in the test stack and the production stack; E2E-02, -04, -05, -06 pass in chromium and webkit, E2E-01 and -03 do not (F-40) |
| DoD-R-07 every AC, EX, R traces to a test and a commit | Mostly met | 649 ids: AC 236 of 236 and EX 177 of 178 name a test; 196 of 235 R name one directly, the other 39 only through an AC or EX row that cites them; no test for `EX-019-03` (F-53). Every story has `feat` commits naming its R, AC or EX ids (`verify-traceability.log`) |
| DoD-R-08 final theme, three widths, no Critical/Serious accessibility violation | Partly | chromium: 79 of 79 light, 79 of 79 dark; webkit light: 73 of 79; firefox not run; WF-08 and WF-19 have no check (F-48, F-52) |
| DoD-R-09 READMEs from a clean checkout | Not yet checked | produced and proved by `/release` (its exit) |
| DoD-R-10 release notes | Not yet written | `/release` |
| DoD-R-11 decisions resolved or pending review; inputs unchanged | Met | 83 decisions; each has a resolution or is "pending review"; D-83 waits for the human; input manifest matches |
| DoD-R-12 commit history | Met in part | the freeze commit `fce75a1` adds only the two manifests; every story has a `feat` commit, but only US-019 names its `US` id in a `feat` subject (the others name R, AC or EX ids); 47 subjects are over 72 characters (F-53, carried F-22; history is not rewritten) |
| DoD-R-13 hidden suite passes | **Not met** | 209 of 214, manifest ok; F-41..F-45 |
| DoD-R-14 every NFR meets its pass value | **Not met** | see "NFR": NFR-01..04, 06 (by hand), 11, 12, 13 (npm) met; NFR-05, 07, 08, 09, 10, 15 not fully shown |
| DoD-R-15 CI pipeline passes | **Not met** | never ran remotely; the local run fails (F-51) |
| DoD-R-16 production files build and start; every threat has a checked control | Met | images built from `HEAD`, production stack healthy in 7 s; threat table below |
| DoD-P01 E2E-01, -02, -03, -04, -06 against the Compose stack | **Not met** | E2E-01 and E2E-03 fail (F-40) |
| DoD-P02 isolation test on every endpoint | Met | `IsolationIT` (all operations of the OpenAPI), `ContractParityIT`, `EndpointSecurityTest`, `AC-018-04` pass |
| DoD-P03 NFR-01..05 on the deployed system | Met for NFR-01..04 | measured by an ad hoc script on the test stack (same images, 10,000 Contacts); the specified script does not exist (F-46) |
| DoD-P04 four customer deliverables | **Not met** | install and backup guide, Slovene user guide and `docs/openapi.json` do not exist; the API serves `/api/v1/openapi.json` and `docs/02_contracts/openapi.yaml` exists (F-46) |
| DoD-P05 later features finished, E2E-05 passes | Met | E2E-05 passes in chromium and webkit |
| DoD-P06 no Critical or High open | **Not met** | see findings |
| DoD-P07 restore tested once | Partly | a dump restored into an empty database by hand: 13 tables, every row count equal (`verify-restore-test.log`); no scripts and no guide describe it (F-46) |

### UI and accessibility

Checks of `frontend/ui-checks/` (overflow `scrollWidth <= clientWidth`, axe with WCAG 2.2 AA tags, no Critical or Serious violation) at 375, 768 and 1440 px against the test stack: chromium light and dark (dark through `prefers-color-scheme`, which I confirmed changes the page background), webkit light. Firefox did not start. The structure against the approved wireframes was not compared again in this run (the reviews and `/design` did that); the screenshots were regenerated and the tracked copies restored.

| Screens (check file) | Widths and modes | Baseline result | Accessibility violations |
|---|---|---|---|
| WF-01..04 sign-in, register, reset, verify e-mail; WF-05 Group list; WF-15 Group trash; WF-14 delete dialog; WF-16 account | 3 widths; chromium light and dark; webkit light | pass, no overflow | none Critical or Serious |
| WF-06 Contact list, also with no last name | 3 widths; chromium light and dark; webkit light | chromium pass; webkit passed serially, 3 runs timed out under parallel load | none |
| WF-07 Contact tabs (Versions, restore, delete confirmation); WF-10 Trash; WF-09 Tags; WF-11 Members; WF-12 Invitations; WF-13 Activity; WF-17 Duplicates | 3 widths; chromium light and dark; webkit light | pass, no overflow | none |
| WF-18 Merge | 3 widths; chromium light and dark pass; webkit light **fails** | webkit tab crashes at `page.reload` when the Contact request answers 500 (F-48) | none seen before the crash |
| WF-08 New Contact, WF-19 Not found, WF-07 main page | not checked by any check file | no result | no result (F-52) |

### NFR

Measured on the test stack with the 10,000-Contact seed (`seed/generate-large.sql`) by a throw-away script; the 5-minute run used 19 signed-in Users plus the measuring account, in a Group with 20 Members (`verify-nfr-measure.log`, `verify-nfr-measure-under-load.log`, `verify-nfr-load.log`).

| NFR | Measured | Pass value | Result |
|---|---|---|---|
| NFR-01 list, search, filter | median 10 to 234 ms, p95 12 to 257 ms (list 106/150, search 234/257, filter 10/12 and 49/51); under load p95 at most 142 ms | median and p95 below 1 s | Met |
| NFR-02 open a Contact | 50 opens: median 3.5 ms, p95 4.8 ms | p95 below 500 ms | Met |
| NFR-03 export of 1,000 Contacts | 5 exports: 1.33 to 1.75 s (0.13 to 0.14 s under load) | each below 10 s | Met |
| NFR-04 20 concurrent Users, 5 minutes | 7,703 requests, all 200, 0 times 429, at most 82 requests per User in one minute, median 13 ms, p95 124 ms, max 473 ms | zero errors, zero 429, NFR-01 holds | Met |
| NFR-05 restart and health | `restart: unless-stopped` and a `HEALTHCHECK` on both images; no install guide | both present | Partly (F-46) |
| NFR-06 backup and restore | restore by hand: equal counts; `scripts/backup.sh` and `restore.sh` missing | counts equal, recorded | Partly (F-46) |
| NFR-07 E2E in three browsers | chromium 4 of 6, webkit 4 of 6, firefox does not start | all pass | Not met (F-40, F-48) |
| NFR-08 three widths, no overflow | chromium all pass, webkit 73 of 79, firefox not run | no horizontal scroll | Partly (F-48, F-52) |
| NFR-09 axe and keyboard walkthrough | axe: no Critical or Serious violation on the checked screens; no keyboard-only walkthrough was run | zero violations and walkthrough passes | Partly (F-50) |
| NFR-10 actions in E2E-01 | not measured: the inputs give no list of counted actions (`03_test-strategy.md`) and E2E-01 fails at step 4 | at most 12 | Not shown (F-50) |
| NFR-11 catalog | `i18n` tests pass (19); keys identical; US-019 acceptance covers both languages | identical sets | Met |
| NFR-12 production start | production stack from freshly built images healthy in 7 s (on a host with an existing volume, not a clean host) | healthy within 2 minutes | Met |
| NFR-13 licences | npm production tree: MIT 176, Apache-2.0 55, BSD-3-Clause 11, ISC 8, 0BSD 1, nothing else; the Maven licence report was not run | only allowed licences | Partly (F-50) |
| NFR-14 stale save stores nothing | stale-save tests (`AC-003-06`, `ContactEndpointsIT`, `Us003ContactAcceptanceIT`: 409 and nothing stored) pass; the planned `ConcurrentSaveIT` (T-75) does not exist, so two truly simultaneous saves were not tested | a stale save stores nothing | Partly (F-50) |
| NFR-15 release checklist files | `docs/openapi.json`, install and backup guide, user guide: none exists | three files | Not met (F-46) |

### Threats and controls

Backend security and isolation tests pass in this run (`SecurityChainIT`, `IsolationIT`, `GroupAccessIT`, `RateLimitFilterTest`, `RequestSizeFilterTest`, `HttpsFilterTest`, `SecurityLoggerTest`, `ApiExceptionHandlerTest`, `DatabaseRolesIT`, `StartupGuardTest`, `FirebaseTokenFilterTest`, `ExportServiceTest`, `Us018SecurityAcceptanceIT`). Controls of SB-01..14 and SR-01..13 are those of the specification table; the checks below are the ones I ran or read in this session. Live probes on the test stack: `verify-error-and-headers.log`, `verify-log-scan.log`.

| Threat | Control | How checked | Result |
|---|---|---|---|
| S: forged, stolen or replayed token | `FirebaseTokenFilter`, Admin SDK (SB-02, SR-02) | `FirebaseTokenFilterTest`, `Us018SecurityAcceptanceIT`; live: no token 401 `auth.token_missing`, garbage token 401 `auth.token_invalid` | Controlled |
| S: emulator accepted in production | startup guard (SR-13) | `StartupGuardTest` (8); production stack runs with profile `prod` and no emulator host | Controlled |
| T: Group in body or query to reach another tenant; object of another Group in a path | Group from path only, `groupId` in repositories (SR-01, AR-02) | `IsolationIT`, `RepositoryScopeRuleTest`; live: unknown Group 404 `group.not_found`, unknown body property 400 `request.unknown_property` | Controlled |
| R: user denies a change | append-only History, `SecurityLogger` (SR-07) | `DatabaseRolesIT` (8), `SecurityLoggerTest`; the application role cannot alter History | Controlled |
| I: existence revealed by 403 vs 404 | one 404 for "missing" and "not a Member" | live probe above; `GroupAccessIT` | Controlled |
| I: stack trace, SQL, token, personal data in errors or logs | `ApiExceptionHandler`, `SecurityLogger` (SB-07, SR-12) | live: 400, 401, 404, 413 bodies carry only `code` and `message`; 57 backend log lines scanned for tokens, e-mail addresses, names, passwords, stack frames: 0 hits | Controlled |
| D: request flood; huge export or search | rate limit 100 per User per minute, 1 MB bodies, 10,000 per export | `RateLimitFilterTest`, `RequestLimitsIT`; live: 2 MB body 413 `request.too_large`; 7,703 requests from 19 Users at up to 82 per minute raised no 429 | Controlled |
| E: Editor or Viewer performs an Owner action; last Owner removed | permission table, Role from the database (SR-03, SR-10) | `EndpointSecurityTest`, `GroupAccessIT`, acceptance tests of US-015 | Controlled |
| E: use before e-mail verification | 403 on Group endpoints (R-018-06, SR-11) | `Us014InvitationAcceptanceIT`, `Us018SecurityAcceptanceIT`; UI `AC-014-03` fails on `getByText('Acme')` (substring match, D-58 g), not a data leak | Controlled (F-47 covers the failing UI test) |
| S: credential stuffing, enumeration | Firebase, one error message | not testable offline; real sign-in shown by S12 (D-78) | Carried by the human's manual test |
| T: SQL injection | named parameters, find-sec-bugs (SR-04) | SpotBugs 0 instances; live: `q='; DROP TABLE contact; --` answers 200 with an empty list; `SearchServiceTest` | Controlled |
| T: formula injection in Excel | string cells only (SR-06) | `ExportServiceTest`, `Us011ExportAcceptanceIT`; 5 exports of 1,000 Contacts returned valid files (200) | Controlled |
| I: export contains another Group's Contacts | `groupId` on the selection (SR-01) | `ExportEndpointsIT`, `IsolationIT` | Controlled |
| T/I: stored XSS | React escaping, CSP (SR-05, SB-10) | ESLint security plugin 0 errors; live CSP on nginx `script-src 'self' https://apis.google.com`, `style-src 'self' 'unsafe-inline'` (a known weakness for styles only); `AC-018-16` | Controlled, residual risk for styles |
| E: clickjacking | `X-Frame-Options: DENY`, `frame-ancestors 'none'` | live headers on the API (`/health`) and nginx (`/`) | Controlled |
| I: token theft by XSS | CSP and escaping | residual risk accepted for Level 2 in the specification | Accepted |
| T: CSRF | Bearer header, one CORS origin | live: a request from an unknown origin gets no `Access-Control-Allow-Origin` | Controlled |
| I: open redirect | in-app routes only; `react-router` 7.18.5 | no sign-in redirect parameter exists (`redirect` does not occur in `frontend/src`); `npm audit` gate; no dedicated test | Controlled by absence |
| T: application role alters schema | restricted role, column grants | `DatabaseRolesIT` | Controlled |
| I: personal data in backups kept longer | 14-day retention in the install guide | the install guide does not exist (F-46) | **Not shown** |
| Data items: Contact data, account, History details, Invitation e-mail, server log | isolation, Trash purge, account deletion, redaction | `Us013DeleteForeverAcceptanceIT`, `Us021DeleteAccountAcceptanceIT`, `HistoryRedactorTest`, `GroupPurgeServiceIT`; log scan clean | Controlled |
| SB-03, SR-09 no secrets in the repository or images | `.env` untracked; names only in `secrets.env.example` | gitleaks 8.28.0: no leaks in 390 commits; images and logs hold no password, API key, user name or auth domain; two non-secret `.env` values match the prescribed scan (F-49, D-83) | Controlled; F-49 waits for the human |
| SB-08, SB-09 scanners | `dependency-check-maven`, `npm audit`, SpotBugs, `eslint-plugin-security`, gitleaks | backend dependency scan: build success, no CVSS 7.0 or higher (NVD data of 2026-11-08, `-DautoUpdate=false`); `npm audit` frontend: 4 High, all accepted in D-05 (human approval), tools 0; SpotBugs 0; ESLint security 0 errors | Controlled |
| SB-11 non-root containers, no secrets in images | `USER` in the Dockerfiles | backend 10001, frontend `nginx`, postgres image is the official one (runs as root at start); no image scanner is listed in `tooling`, so no image scan | Controlled, no image scan |
| SB-04 TLS | `HttpsFilter` with `REQUIRE_HTTPS` | `HttpsFilterTest`; production stack answers 403 on plain HTTP; the customer's proxy ends TLS | Controlled |
| SB-14 no consent box | review | the word consent (Slovene `soglasj-`, English) occurs in neither language catalog | Controlled |

### CI and deployment

- No CI runner is pinned (no `act`) and `origin/main` is 212 commits behind, so the pipeline never ran remotely. I ran every job locally (`.github/workflows/ci.yml`):
  - backend build and `verify -Pquality`: fails at `EX-007-05` (frozen test, F-08); the quality plugins pass when run directly;
  - frontend `npm run build`, `npm test`, `npm run check`: pass; browser tests (`npm run e2e`): chromium 79 of 79, webkit 73 of 79, firefox does not start;
  - contracts (`validate-contracts`: 37 operations) and traceability (684 story ids, 7 BR, 13 SR, 14 SB, 15 NFR all mapped): pass; decision ids unique;
  - scanners: gitleaks no leaks, `npm audit` gate (frontend 4 High accepted by D-05, tools 0), licence check, `jscpd` 59 clones, 1.79 % (limit 3 %): pass; backend dependency scan: pass;
  - images: backend 554 MB, frontend 83 MB, emulator 742 MB build; the three Compose files are valid with dummy values.
  Result: the local pipeline does not pass (F-51).
- Production deployment: `docker compose -f docker-compose.prod.yml --env-file production.env up -d` with the three images built from `HEAD`: all services healthy 7 s after the start; `/health` returns `{"status":"UP"}`; frontend 200; the backend answers 403 on plain HTTP (`REQUIRE_HTTPS`); both application containers have `restart: unless-stopped` and run as non-root; Flyway validated 3 migrations; the log shows no error. The human's TLS proxy answered 200 again. Logs: `verify-prod-start.log`.

### Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| backend, frontend, emulator, PostgreSQL | test stack (`scripts/test-stack.sh up`, empty database, throw-away values) | UI acceptance suite (95 tests), E2E-01..06 in three browsers, UI checks, the seed, 10,000 Contacts, 20 Users for 5 minutes, error and header probes, a dump and restore | UI acceptance 88 of 95; E2E 8 of 18; everything else as above |
| backend, frontend, PostgreSQL | production stack with the human's production configuration and real Firebase project | start, `/health`, frontend, plain-HTTP refusal, TLS proxy answer | Healthy; no sign-in flow was run by this session (S12 showed it for the earlier build) |

The test stack was taken down afterwards. The production application containers were stopped for this run with the human's consent (the two ports collide) and started again from the new images.

### Traceability

Checked by `check-traceability.mjs` (every id mapped to the specification) and a count of ids referenced in test files (`verify-traceability.log`).

| R / AC / EX | Tests | Commits |
|---|---|---|
| 649 defined ids of 26 stories | AC 236 of 236, EX 177 of 178, R 196 of 235 directly and the other 39 through the AC or EX that cites them; no test for `EX-019-03` | every story has `feat` commits naming its ids; the freeze commit is `fce75a1` |

### Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-40 | High | E2E-01, E2E-03 (`verify-e2e-scenarios.log`), DoD-P01 | After "Ustvari" in the create-Group dialog the app stays on `/groups`; both scenarios expect the new Group to open as an empty Contact list (step 4 of E2E-01, an `[ASSUMPTION]`; AC-002-01 only asks that it appears in the list). Chromium and webkit fail at the same heading (`Kontakti`, level 1). | fixed in `1174061` (the dialog opens the new Group); E2E re-run waits for `/verify` (port 8080 is held by the production stack), D-84 a |
| F-41 | High | hidden evaluation | `R-003-02` fails a held-out case (invalid) | fixed in `c6ba1e6` (a number or truth value in a text field is 400); held-out case not visible, D-84 b |
| F-42 | High | hidden evaluation | `R-006-07` fails a held-out case (other) | probed, no defect: 6 visible tests pass (`d1a9ec4`); not shown fixed, next hidden evaluation decides, D-84 b |
| F-43 | High | hidden evaluation | `R-007-03` fails a held-out case (other); the frozen `EX-007-05` on the same rule also fails (F-08) | probed, no defect: 6 visible tests pass (`ecc793a`); frozen `EX-007-05` still contradicts the rule (D-68 i, D-84 c); not shown fixed |
| F-44 | High | hidden evaluation | `R-017-02` fails a held-out case (other) | probed, no defect in the REST interface: 5 visible tests pass (`ecc793a`); Activity screen cannot offer a removed user, D-84 d; not shown fixed |
| F-45 | High | hidden evaluation | `R-018-07` fails a held-out case (other). My own probe: `GET /health` answers `{"status":"UP"}` with content type `application/vnd.spring-boot.actuator.v3+json`; I cannot see the case, so this is an observation, not a cause. | fixed in `d5c20f0` (`/health` answers `application/json`); held-out case not visible, D-84 b |
| F-46 | High | T-69, T-70, T-76 of `02_tasks.md`; DoD-P04, DoD-P07; NFR-05, 06, 15 | The files of three planned tasks do not exist: `perf/measure.mjs`, `scripts/backup.sh`, `scripts/restore.sh`, `docs/install-and-backup.md`, `docs/user-guide-sl.md`, `docs/openapi.json`. `/release` writes only READMEs, release notes and the retrospective, so nothing would create them. NFR-01..04 and the restore were checked by hand here; the customer deliverables are missing. | fixed: scripts `9c5f1d1`, `perf/measure.mjs` `37aee07` (live run waits for `/verify`), `docs/openapi.json` `40ba2ff`, install guide `cad24a1`, user guide `d55bc99`; D-84 e |
| F-47 | Medium | UI acceptance (7 failing frozen tests, carried as F-08 Low) | Five of the seven failing tests have no recorded cause: `AC-003-07` (strict mode: `getByText('Novak')` matches 2 elements), `AC-004-04` and `AC-004-08` (`getByLabel('Ključ')` not found after adding an Attribute), `AC-005-06` (option `gospodar` not shown), `AC-006-04` (the row disappears, but the API still counts 5 Contacts on the Tag right after). `AC-014-03` and `AC-019-06` have decisions (D-58 g, D-61 g). Each needs to be classed as a product defect or a frozen-test defect; `AC-006-04` may be a timing or a real defect. | open: needs the test stack to class each failure (D-84 f) |
| F-48 | Medium | NFR-07, NFR-08 | Firefox cannot start in this environment (`browserType.launch` timeout of 180 s, graphics crash), so no Firefox E2E or UI result exists. In WebKit the Merge screen crashes the page on `page.reload` with a mocked 500 at all three widths (reproducible serially); three Contact-list checks time out under parallel load and pass serially. Cause not found. | open item for the release notes: Firefox does not start here; the WebKit crash cause not found (D-84 f) |
| F-49 | Low (Critical by rule, lowered by the human, D-83) | step 8, `.env` scan | The scan lists files and 14 commit messages. Two matching values are not secrets (a database name, a public Firebase project id); the four secret-class values appear nowhere (D-83). | accepted, D-83 option 1 |
| F-50 | Medium | NFR-09, NFR-10, NFR-13, NFR-14 | No keyboard-only walkthrough was run; NFR-10 has no counting rule and E2E-01 fails at step 4; the Maven licence report was not run; `ConcurrentSaveIT` (T-75) does not exist. | partly fixed: `ConcurrentSaveIT` written and passing (`3f36c9c`); keyboard walkthrough, NFR-10 counting rule and Maven licence report open (D-84 f) |
| F-51 | Medium | DoD-R-15, ES-14 | The CI pipeline has never run: `origin/main` is 212 commits behind and no runner is pinned. The local run of every job fails at the frozen backend test and has no Firefox. | open item for the release notes: the human pushes `main` so CI can run (D-84 f) |
| F-52 | Medium | DoD-R-08 | WF-08 (New Contact), WF-19 (Not found) and the main page of WF-07 have no UI check (three widths, axe, overflow) in `frontend/ui-checks/`. | open: the three UI checks need the test stack to be run (D-84 f) |
| F-53 | Low | DoD-R-07, DoD-R-12 | `EX-019-03` (1234.5 in Slovene and English) is named by no test; `feat` subjects name the `US` id only for US-019; 47 subjects are over 72 characters (carried F-22). | partly fixed: `EX-019-03` test and English number format `49df64f`; the long subjects stay (history is not rewritten) |
| F-54 | Low | `npm run check` | ESLint warning `security/detect-non-literal-regexp` in `ui-checks/group-shell-screens.stack.spec.ts:42` (a test file). | fixed in `e5c966f` |

Carried and not re-opened by this run: F-08 (Low, see F-47), F-14 (Medium, D-79: replay of a deleted account's token to be checked at the first deployment), F-19 (Low, waits for the human, D-68 d), F-20..F-25 (Low), F-38 (Medium, D-82) and F-39 (Low).

### Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-40..F-46, F-50, F-53, F-54 (D-84) | 10 `fix` commits and 3 `test` commits, `1174061`, `d5c20f0`..`3f36c9c` | not yet: `/verify` run 2 re-runs the E2E scenarios, the hidden evaluation and the NFR script |

### Human to do

1. D-83 answered (option 1).
2. D-68 d (F-19): the version of a Survivor that changes nothing.
3. Run Firefox (and the real Excel check of Q-06, real Google sign-in) on a machine where Firefox starts; this environment cannot.
4. Push `main` so CI can run (F-51); I did not push.

## Run 2

> **Self-check by the agent in a separate session (S9), not an independent review.** Commit under test: `a7d0a77` (code identical to `d453f13`, the commit of the loop 1 fixes; later commits only add documents). Result: **phase 6 gate not passed**: the five held-out failures are unchanged, `E2E-01` and `E2E-03` still fail (now for two other reasons), and the loop 1 fix of F-40 broke a UI check. Logs: `out/logs/verify2-*.log`. The first attempt of this run stopped at step 3 because the hidden evaluation was not of `HEAD`; the runner has since evaluated `a7d0a77` (runs 2 and 3 of `06_hidden-evaluation.md`).

### Procedure result

| Step | Result |
|---|---|
| 1 run number | run 2; no finding has stayed open for three runs |
| 2 manifests | input manifest 77 of 77 and acceptance manifest 59 of 59 hashes match (`verify-run2-manifests.log`); hidden manifest "ok" in the runner's evaluation |
| 3 suite, checks, hidden evaluation | see "Definition of Done"; hidden evaluation of `a7d0a77` (runs 2 and 3, same result): 214 tests, 209 pass, 5 fail, the same five as before (F-41..F-45). Coverage and mutation not re-measured: no component changed since phase 5 except the loop 1 fixes; backend line coverage 97.71 % (JaCoCo of this run) |
| 4 runtime | test stack (Compose, emulator, empty database, then the 10,000-Contact seed) and production stack |
| 5 NFR | measured, see "NFR" |
| 6 UI | see "UI and accessibility" |
| 7 security | see "Threats and controls" |
| 8 logs, errors, secrets | live probes and a scan of 8,640 backend log lines: no stack frame, token, password or e-mail address; `.env` scan: only the two non-secret values of F-49 (below) |
| 9 CI, deployment | see "CI and deployment" |
| 10 traceability | see "Traceability" |

### Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-R-01 full suite passes, hashes match | **Not met** | hashes match. Backend 845 tests (197 unit, 648 integration, counted from the fresh reports): 844 pass, `EX-007-05` fails (F-08, D-68 i). Frontend unit 587 of 587 pass. UI acceptance 88 of 95 (the same 7 failures, F-47). E2E scenarios: chromium 4 of 6, webkit 4 of 6 (E2E-01, E2E-03 fail: F-55, F-56), firefox 0 of 6 (F-48). `verify2-backend-quality.log`, `verify2-ui-acceptance.log`, `verify2-e2e-scenarios.log`, `verify2-frontend-test.log` |
| DoD-R-02 format, lint, type check, static analysis | Met | Spotless, Checkstyle 0, SpotBugs with find-sec-bugs 0, PMD 0; Prettier, ESLint, `tsc -b`, i18n tests pass, no ESLint warning left (`npm run check` exit 0; F-54 fixed) |
| DoD-R-03 quality gate | Met with a decision | as in run 1 (D-82, F-38); not re-measured |
| DoD-R-04 architecture constraints, no cycles | Met | architecture tests pass in the backend run |
| DoD-R-05 no open Critical or High | **Not met** | F-40..F-46 as listed below, F-55, F-56 |
| DoD-R-06 components run, core flows work | Partly | all components start; E2E-02, -04, -05, -06 pass, E2E-01 and -03 do not |
| DoD-R-07 every AC, EX, R traces to a test and a commit | Met in the main | AC 236 of 236, EX 178 of 178 name a test (`EX-019-03` now has one, F-53); R 181 of 235 are named directly (counting rule of this run; run 1 counted 196), the rest through the AC or EX that cites them (`verify2-traceability.log`) |
| DoD-R-08 final theme, three widths, no Critical/Serious accessibility violation | Partly | chromium light 76 of 79 and dark 76 of 79, webkit light 73 of 79, firefox not run; no accessibility violation on any screen that ran; WF-08, WF-19 and the main page of WF-07 still have no check (F-52) |
| DoD-R-09 READMEs from a clean checkout | Not yet checked | `/release` |
| DoD-R-10 release notes | Not yet written | `/release` |
| DoD-R-11 decisions resolved or pending review; inputs unchanged | Met | 84 decisions, each with a resolution or "pending review"; input manifest matches |
| DoD-R-12 commit history | Met in part | as in run 1: 47 subjects over 72 characters, history not rewritten (F-53) |
| DoD-R-13 hidden suite passes | **Not met** | 209 of 214 at `a7d0a77`; F-41..F-45 |
| DoD-R-14 every NFR meets its pass value | **Not met** | see "NFR" |
| DoD-R-15 CI pipeline passes | **Not met** | never ran remotely; local run: see "CI and deployment" (F-51) |
| DoD-R-16 production files build and start; every threat has a checked control | Met | images rebuilt from `HEAD`, production stack healthy in 20 s |
| DoD-P01 E2E-01, -02, -03, -04, -06 against the Compose stack | **Not met** | E2E-01 (F-55), E2E-03 (F-56) |
| DoD-P02 isolation test on every endpoint | Met | `IsolationIT`, `ContractParityIT`, `EndpointSecurityTest`, `AC-018-04` pass in the backend run |
| DoD-P03 NFR-01..05 on the deployed system | Partly | NFR-01..03 pass with the specified script; NFR-04 passes only with a one-line change to the script (F-58) |
| DoD-P04 four customer deliverables | Met in the main | `docs/install-and-backup.md` (130 lines), `docs/user-guide-sl.md` (115 lines), `docs/openapi.json` (OpenAPI 3.1, 37 operations, same as the contract), backup and restore scripts. Content of the guides was not tested by a person |
| DoD-P05 later features finished, E2E-05 passes | Met | E2E-05 passes in chromium and webkit |
| DoD-P06 no Critical or High open | **Not met** | see findings |
| DoD-P07 restore tested once | Met | `scripts/backup.sh` wrote a 0600 dump of the test stack's database; `scripts/restore.sh` restored it into a new database with 13 tables and row counts equal to the source (11,198 Contacts); `verify2-backup-restore.log` |

### UI and accessibility

`frontend/ui-checks/` (overflow, axe WCAG 2.2 AA, no Critical or Serious violation) at 375, 768 and 1440 px against the test stack: chromium light and dark (dark through `colorScheme` in a scratch configuration), webkit light. Firefox does not start (F-48). The structure against the approved wireframes was not compared again in this run.

| Screens | Widths and modes | Baseline result | Accessibility violations |
|---|---|---|---|
| all screens with a check file except those below | 3 widths; chromium light 76 of 79 and dark 76 of 79 pass; webkit light 73 of 79 | pass, no overflow | none Critical or Serious |
| WF-05 create-Group dialog "created" state | chromium light, dark and webkit: **fails** | after "Ustvari" the app opens the new Group, the check expects the Group card in the list (F-57) | none seen |
| WF-18 Merge | chromium pass; webkit **fails** | tab crash at `page.reload` with a mocked 500, as in run 1 (F-48) | none seen before the crash |
| WF-08, WF-19, WF-07 main page | no check file | no result | no result (F-52) |

### NFR

Measured with the specified script `perf/measure.mjs` on the test stack with the 10,000-Contact seed (`verify2-nfr-measure.log`, `verify2-nfr04.log`).

| NFR | Measured | Pass value | Result |
|---|---|---|---|
| NFR-01 list, search, filter | median 13 to 209 ms, p95 20 to 212 ms | median and p95 below 1 s | Met |
| NFR-02 open a Contact | median 11 ms, p95 13 ms | p95 below 500 ms | Met |
| NFR-03 export of 1,000 Contacts | 155 to 564 ms | each below 10 s | Met |
| NFR-04 20 concurrent Users, 5 minutes | script as delivered: 8,111 of 9,000 requests answered 404, because the invited Users never became Members (F-58). Same script with one line changed (`GET /groups` after `/me`): 8,962 requests, 0 not 200, 0 times 429, reads median 14 ms, p95 47 ms, max 365 ms | zero errors, zero 429, reads below 1 s | Met only with the changed copy |
| NFR-05 restart and health | `restart: unless-stopped` and `HEALTHCHECK` on both images, install guide exists | both present | Met |
| NFR-06 backup and restore | scripts run, row counts equal | counts equal | Met |
| NFR-07 E2E in three browsers | chromium 4 of 6, webkit 4 of 6, firefox does not start | all pass | Not met (F-55, F-56, F-48) |
| NFR-08 three widths, no overflow | chromium 76 of 79 (the 3 failures are the F-57 check, not an overflow), webkit 73 of 79, firefox not run | no horizontal scroll | Partly (F-48, F-52, F-57) |
| NFR-09 axe and keyboard walkthrough | axe clean on every screen that ran; no keyboard-only walkthrough | zero violations and walkthrough passes | Partly (F-50) |
| NFR-10 actions in E2E-01 | not measured: E2E-01 fails at step 5 and there is no counting rule | at most 12 | Not shown (F-50) |
| NFR-11 catalog | i18n tests pass (20) | identical sets | Met |
| NFR-12 production start | healthy 20 s after `up` from freshly built images (existing volume) | within 2 minutes | Met |
| NFR-13 licences | npm production tree: MIT 176, Apache-2.0 55, BSD-3-Clause 11, ISC 8, 0BSD 1; Maven licence report not run | only allowed licences | Partly (F-50) |
| NFR-14 stale save stores nothing | `ConcurrentSaveIT` (T-75) exists and passes in the backend run, with the stale-save tests | a stale save stores nothing | Met |
| NFR-15 release checklist files | `docs/openapi.json`, install and backup guide, user guide exist | three files | Met |

### Threats and controls

Unchanged from run 1 except where stated; the backend security and isolation tests all pass again in this run (the only backend failure is `EX-007-05`). Live probes on the test stack (`verify2-error-and-headers.log`): no token 401 `auth.token_missing`; garbage token 401 `auth.token_invalid`; 2 MB body 413-class answer `request.too_large`; API headers `X-Frame-Options: DENY`, `frame-ancestors 'none'`, `nosniff`, `no-referrer`; frontend CSP and the same headers; a request from a foreign origin gets 403 and no `Access-Control-Allow-Origin`; `/health` answers `application/json` (F-45's observation fixed, the held-out case still fails); Boris, no Member of Acme, gets no Acme data on its URL (F-56 is about the screen, not about data). Scanners: gitleaks 8.28.0 no leaks in 407 commits; `npm audit` gate: frontend 4 High all accepted in D-05, tools 0; `jscpd` 63 clones, 1.85 % (limit 3 %); backend OWASP dependency-check not re-run (no dependency changed since run 1). `.env` scan: file scan and commit scan match only `POSTGRES_DB` (the database name, 15 commit messages) and `FIREBASE_PROJECT_ID` (public, 2 files); the other four values of 6 or more characters appear nowhere; Low by D-83 (F-49).

| Threat group | Result |
|---|---|
| S, T, R, I, D, E rows and data items of run 1 | Controlled as in run 1 (tests pass again) |
| I: personal data in backups kept longer | Controlled: the install and backup guide describes the 14-day retention and `backup.sh` removes older dumps only after a new one succeeded |
| SB-03, SR-09 no secrets | Controlled; F-49 Low (D-83) |
| SB-11 non-root | backend 10001, frontend `nginx` |

### CI and deployment

- No runner is pinned and `origin/main` was not pushed, so the pipeline never ran remotely. Every job run locally: backend `verify -Pquality` fails only at `EX-007-05`; frontend build, test, check pass; browser tests fail as above; contracts (37 operations) and traceability (all ids mapped) pass; gitleaks, `npm audit` gate, licences, `jscpd` pass. Result: not passing (F-51).
- Production: `contact-groups-backend:1.0.0` and `contact-groups-frontend:1.0.0` rebuilt from `HEAD`; `docker compose -p contact-groups -f docker-compose.prod.yml --env-file production.env up -d`: three services healthy within 20 s; `/health` `{"status":"UP"}`, `application/json`; frontend 200; plain HTTP 403 (`REQUIRE_HTTPS`); both application containers non-root with `restart: unless-stopped`; Flyway ran; no error in the log. The human had stopped the production stack and its TLS proxy for this run; both were started again (`docker start contact-groups-tlsproxy`). Logs: `verify2-prod-start.log`, `verify2-image-*.log`.
- Note for the next run: the backend integration tests start their own emulator on port 9099, so `mvnw verify` cannot run while the test stack is up (the first attempt, `verify2-backend-quality-attempt1-port-clash.log`, failed 406 tests for that reason and is not counted).

### Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| backend, frontend, emulator, PostgreSQL | test stack (`scripts/test-stack.sh up`, empty database, throw-away values; then `seed/`, 10,000 Contacts) | UI acceptance (95), E2E-01..06 in three browsers, UI checks, `perf/measure.mjs`, error and header probes, backup and restore scripts | UI acceptance 88 of 95; E2E 8 of 18; the rest as above |
| backend, frontend, PostgreSQL, TLS proxy | production stack, the human's production configuration | start, `/health`, frontend, plain-HTTP refusal | Healthy; no sign-in flow run |

The test stack was removed afterwards (`scripts/test-stack.sh down`).

### Traceability

| R / AC / EX | Tests | Commits |
|---|---|---|
| 649 defined ids | AC 236 of 236, EX 178 of 178, R 181 of 235 directly | unchanged from run 1; loop 1 commits `1174061`, `d5c20f0`..`3f36c9c` name F-ids |

### Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-40 | High | E2E-01, E2E-03 | The first cause is fixed (the new Group opens). Both scenarios now fail further on: see F-55 and F-56. | open until E2E-01 and E2E-03 pass |
| F-41 | High | hidden evaluation | `R-003-02` fails a held-out case (invalid) at `a7d0a77`, after the loop 1 change | open; fix not shown |
| F-42 | High | hidden evaluation | `R-006-07` fails a held-out case (other) | open |
| F-43 | High | hidden evaluation | `R-007-03` fails a held-out case (other); frozen `EX-007-05` also fails (F-08) | open |
| F-44 | High | hidden evaluation | `R-017-02` fails a held-out case (other) | open |
| F-45 | High | hidden evaluation | `R-018-07` fails a held-out case (other) after `/health` became `application/json` | open; cause still unknown |
| F-46 | High | T-69, T-70, T-76 | **Closed in substance**: guides, `openapi.json`, backup and restore scripts exist and work; `perf/measure.mjs` has a defect (F-58) | closed except F-58 |
| F-47 | Medium | UI acceptance | Seven failing frozen tests, unchanged. `AC-004-04`, `AC-004-08` fixed (F-55); five remain. `AC-003-07`: two links per list row carry the surname, so the strict locator matches twice; `AC-005-06` (option `gospodar` not offered after typing `go`; the Tag field uses a native datalist like the Attribute keys did, same likely cause) and `AC-006-04` (the API still lists the Tag right after the row disappeared) not classed. | open |
| F-48 | Medium | NFR-07, NFR-08 | Firefox does not start; WebKit Merge screen crashes on `page.reload` with a mocked 500; unchanged | open item for the release notes |
| F-49 | Low (D-83) | `.env` scan | Same two non-secret values; 15 commit messages now | accepted, D-83 |
| F-50 | Medium | NFR-09, NFR-10, NFR-13 | `ConcurrentSaveIT` exists and passes (NFR-14 met). Keyboard walkthrough, NFR-10 counting rule and Maven licence report still open. | open |
| F-51 | Medium | DoD-R-15 | CI never ran; the human pushes `main` | open item for the release notes |
| F-52 | Medium | DoD-R-08 | WF-08, WF-19, WF-07 main page have no UI check | open |
| F-53 | Low | DoD-R-07, DoD-R-12 | `EX-019-03` fixed; 47 long subjects stay | accepted, history is not rewritten |
| F-54 | Low | `npm run check` | fixed, no ESLint warning | closed |
| F-55 | High | E2E-01 step 5 (`verify2-e2e-scenarios.log`, chromium and webkit), `AC-004-04`, `AC-004-08`, DoD-P01 | The Attribute fields are labelled with a number, `Ključ 1` and `Vrednost 1` (`AttributeEditor.tsx`, key `contacts.attributeKey` = "Ključ {{n}}"). The approved wireframes WF-07 and WF-08 show `Ključ` and `Vrednost`, and the frozen tests look for the exact labels `Ključ` and `Vrednost` of the last row. `getByLabel('Ključ', { exact: true })` finds nothing, the test times out. | fixed, two causes. (1) `7394819`: the fields are labelled `Ključ` and `Vrednost`, each row is a group `Atribut n`. (2) `04b29a9`: the key suggestions were a native datalist, never visible, but WF-08 and `AC-004-04/08` expect a visible list; now a combobox with a listbox. Unit tests 592 of 592. Against a fresh test stack: `AC-004-04`, `AC-004-08` pass, E2E-01 passes in chromium and webkit (`f55b-acceptance.log`, `f55b-e2e01.log`). Closed once `/verify` confirms |
| F-56 | High | E2E-03 step 2, DoD-P01, AC-002-06 | A Member-less User who opens the contact-list URL of another Group sees the Group shell with the list headings and a generic error ("Nekaj je šlo narobe", "Poskusi znova") instead of the Not found page "Strani ni mogoče najti"; the scenario expects the page. No Acme data is shown (probe of this run), so isolation holds, but the screen is wrong. Hidden behind F-40 in run 1. | fixed in `592cbf9`: `GroupShell` shows `NotFoundPage` when the fresh Group list holds no Group with the id of the URL (every Group screen, BR-01); a stale list is not trusted. Unit tests 593 of 593 (`f56-red.log`, `f56-green.log`). E2E-03 not re-run (D-85). Closed once `/verify` confirms |
| F-57 | Medium | `ui-checks/group-screens.stack.spec.ts:38`, NFR-08 | WF-05 "created" check fails in chromium (light, dark) and webkit at all widths: after the loop 1 change of F-40 the dialog opens the new Group, the check still waits for the Group's card in the list. A regression of the fix; the check is not frozen. | open |
| F-58 | Medium | `perf/measure.mjs`, T-69, DoD-P03, NFR-04 | The script invites 19 Users and joins them with `GET /me`, but the contract accepts Invitations on `GET /groups` (R-014-05). The Users stay outside the Group, 8,111 of 9,000 requests are 404, the script exits 1. With `GET /groups` added, NFR-04 passes (8,962 requests, none failed). | open |

Carried and not re-opened: F-08 (Low, see F-47), F-14 (Medium, D-79), F-19 (Low, waits for the human, D-68 d), F-20..F-25 (Low), F-38 (Medium, D-82), F-39 (Low).

### Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-40..F-46, F-50, F-53, F-54 (D-84) | 10 `fix` commits and 3 `test` commits | run 2: F-46, F-54, `EX-019-03`, NFR-14 and the restore confirmed; F-40, F-41..F-45 not confirmed; two new findings from the fixes (F-57, F-58) |

### Human to do

1. D-68 d (F-19): the version of a Survivor that changes nothing.
2. Run Firefox, the real Excel check (Q-06) and real Google sign-in on a machine where they work.
3. Push `main` so CI can run (F-51); I did not push.
4. The five held-out failures cannot be seen by `/fix`: if two more loops do not clear them, run 4 asks you (accept as a known limitation, see the failing test, or change the requirement).

## Run 3

> **Self-check by the agent in a separate session (S10), not an independent review.** Commit under test: `12e38c7` (code identical to `0e7f518`, the commit of the F-56 fix; later commits only add documents). Result: **phase 6 gate not passed** because of the open Medium findings below; the five held-out failures F-41..F-45 are unchanged, and after the run the human accepted them as a known limitation (D-86), so no High is open. F-40, F-55 and F-56 are confirmed fixed (E2E-01 and E2E-03 pass in chromium and webkit). Logs: `out/logs/verify3-*.log`. The first attempt of this run stopped at step 3 because the hidden evaluation was not of `HEAD` (commit `12e38c7`); the human then ran `node system/run.mjs --hidden-eval` (run 4 of `06_hidden-evaluation.md`, commit `12e38c7`), and the human allowed stopping the production stack for the browser checks.

### Procedure result

| Step | Result |
|---|---|
| 1 run number | run 3. F-41..F-45 were open in runs 1, 2 and 3, so they stayed open for three runs and the approval decision was asked: the human accepted them as a known limitation (D-86) |
| 2 manifests | input manifest 77 of 77 and acceptance manifest 59 of 59 hashes match (`verify-run3-manifests.log`); hidden manifest "ok" in the runner's evaluation |
| 3 suite, checks, hidden evaluation | see "Definition of Done". Hidden evaluation of `12e38c7`: 214 tests, 209 pass, 5 fail, the same five (F-41..F-45). Coverage and mutation not re-measured: no backend file changed since phase 5 except the loop 1 fixes, the frontend changed only in `AttributeEditor`, `GroupShell` and the two catalogs; combined JaCoCo line coverage 97.71 % (report of the latest backend run) |
| 4 runtime | test stack (Compose, emulator, empty database, then `seed/` and the 10,000-Contact seed) and production stack |
| 5 NFR | measured, see "NFR" |
| 6 UI | see "UI and accessibility" |
| 7 security | see "Threats and controls" |
| 8 logs, errors, secrets | live probes and a scan of 1,812 backend log lines: no stack frame, token, password or e-mail address; `.env` scan: only the two non-secret values of F-49 (below) |
| 9 CI, deployment | see "CI and deployment" |
| 10 traceability | see "Traceability" |

### Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-R-01 full suite passes, hashes match | **Not met** | hashes match. Backend 845 tests (197 unit, 243 + 400 + 5 integration, from the failsafe and surefire summaries): 844 pass, `EX-007-05` fails (F-08, D-68 i). Frontend unit 593 of 593. UI acceptance 90 of 95: `AC-004-04` and `AC-004-08` now pass; the same five fail (F-47). E2E scenarios: chromium 6 of 6, webkit 6 of 6, firefox 0 of 6 (does not start, F-48). `verify3-backend-quality.log`, `verify3-frontend-test.log`, `verify3-ui-acceptance.log`, `verify3-e2e-scenarios.log` |
| DoD-R-02 format, lint, type check, static analysis | Met | `spotless:check`, Checkstyle 0 violations, SpotBugs with find-sec-bugs 0 bugs, PMD pass (`verify3-static-analysis.log`; Maven stops `verify -Pquality` at the failing `EX-007-05` before these plugins, so they ran as separate goals); Prettier, ESLint, `tsc -b`, i18n tests pass (`npm run check` exit 0) |
| DoD-R-03 quality gate | Met with a decision | as in run 1 (D-82, F-38); not re-measured |
| DoD-R-04 architecture constraints, no cycles | Met | architecture tests pass in the backend run |
| DoD-R-05 no open Critical or High | Met after D-86 | F-41..F-45 lowered to Medium by D-86 (human approval); F-40, F-46, F-55, F-56 closed |
| DoD-R-06 components run, core flows work | Met | all components start; E2E-01..06 pass in chromium and webkit |
| DoD-R-07 every AC, EX, R traces to a test and a commit | Met in the main | AC 236 of 236, EX 178 of 178 name a test; R 198 of 235 are named directly (looser counting rule than run 2's 181, `verify3-traceability.log`), the rest through the AC or EX that cites them; `check-traceability.mjs`: all 684 story ids, 7 BR, 13 SR, 14 SB, 15 NFR mapped |
| DoD-R-08 final theme, three widths, no Critical/Serious accessibility violation | Partly | chromium light 76 of 79 and dark 76 of 79, webkit light 73 of 79, firefox not run; no accessibility violation on any screen that ran; WF-08, WF-19 and the main page of WF-07 still have no check (F-52); the structure against the approved wireframes was not compared again |
| DoD-R-09 READMEs from a clean checkout | Not yet checked | `/release` |
| DoD-R-10 release notes | Not yet written | `/release` |
| DoD-R-11 decisions resolved or pending review; inputs unchanged | Met | 85 decisions, each with a resolution or "pending review"; input manifest matches; `check-decision-ids.sh`: ids unique |
| DoD-R-12 commit history | Met in part | 47 subjects over 72 characters of 417 commits, history not rewritten (F-53) |
| DoD-R-13 hidden suite passes | **Not met**, accepted for five cases (D-86) | 209 of 214 at `12e38c7`; the five failing cases are the accepted known limitation |
| DoD-R-14 every NFR meets its pass value | **Not met** | see "NFR" |
| DoD-R-15 CI pipeline passes | **Not met** | never ran remotely; local run: see "CI and deployment" (F-51) |
| DoD-R-16 production files build and start; every threat has a checked control | Met | images rebuilt from `HEAD`, production stack healthy in 17 s |
| DoD-P01 E2E-01, -02, -03, -04, -06 against the Compose stack | Met in chromium and webkit | all pass; Firefox does not start (F-48) |
| DoD-P02 isolation test on every endpoint | Met | `IsolationIT`, `ContractParityIT`, `EndpointSecurityTest`, `AC-018-04` pass; E2E-03 passes |
| DoD-P03 NFR-01..05 on the deployed system | Partly | NFR-01..03 pass with the specified script; NFR-04 passes only with a one-line change to the script (F-58) |
| DoD-P04 four customer deliverables | Met in the main | as in run 2; content of the guides was not tested by a person |
| DoD-P05 later features finished, E2E-05 passes | Met | E2E-05 passes in chromium and webkit |
| DoD-P06 no Critical or High open | Met after D-86 | none open |
| DoD-P07 restore tested once | Met | run 2 (`verify2-backup-restore.log`); `scripts/` and the backend are unchanged since, so not repeated |

### UI and accessibility

`frontend/ui-checks/` (overflow, axe WCAG 2.2 AA, no Critical or Serious violation) at 375, 768 and 1440 px against the test stack: chromium light, chromium dark (through `colorScheme` in a scratch configuration outside the project), webkit light. Firefox does not start (F-48).

| Screens | Widths and modes | Baseline result | Accessibility violations |
|---|---|---|---|
| all screens with a check file except those below | 3 widths; chromium light 76 of 79 and dark 76 of 79 pass; webkit light 73 of 79 | pass, no overflow | none Critical or Serious |
| WF-05 create-Group dialog "created" state | chromium light, dark and webkit: **fails** | the check expects the Group card in the list, the app opens the new Group (F-57, unchanged) | none seen |
| WF-18 Merge | chromium pass; webkit **fails** | tab crash at `page.reload` with a mocked 500 (F-48, unchanged) | none seen before the crash |
| WF-08, WF-19, WF-07 main page | no check file | no result | no result (F-52) |

### NFR

Measured on the test stack with the 10,000-Contact seed (`verify3-nfr-measure.log`, `verify3-nfr04-delivered.log`).

| NFR | Measured | Pass value | Result |
|---|---|---|---|
| NFR-01 list, search, filter | median 6 to 10 ms, p95 7 to 40 ms | median and p95 below 1 s | Met |
| NFR-02 open a Contact | median 4 ms, p95 12 ms | p95 below 500 ms | Met |
| NFR-03 export of 1,000 Contacts | 78 to 212 ms | each below 10 s | Met |
| NFR-04 20 concurrent Users, 5 minutes | script as delivered (1 minute, enough to show the defect): 1,787 requests, 1,652 not 200 (404), because the invited Users never become Members (F-58). Same script with one line changed (`GET /groups` after `/me`, copy outside the project): 8,964 requests, 0 not 200, 0 times 429, reads median 14 ms, p95 39 ms, max 142 ms | zero errors, zero 429, reads below 1 s | Met only with the changed copy |
| NFR-05 restart and health | `restart: unless-stopped` on both application containers, `HEALTHCHECK` healthy, install guide exists | both present | Met |
| NFR-06 backup and restore | as run 2, scripts unchanged | counts equal | Met |
| NFR-07 E2E in three browsers | chromium 6 of 6, webkit 6 of 6, firefox does not start | all pass | Partly (F-48) |
| NFR-08 three widths, no overflow | chromium 76 of 79 (the 3 failures are the F-57 check, not an overflow), webkit 73 of 79, firefox not run | no horizontal scroll | Partly (F-48, F-52, F-57) |
| NFR-09 axe and keyboard walkthrough | axe clean on every screen that ran; no keyboard-only walkthrough | zero violations and walkthrough passes | Partly (F-50) |
| NFR-10 actions in E2E-01 | not measured: E2E-01 passes, but there is no counting rule | at most 12 | Not shown (F-50) |
| NFR-11 catalog | i18n tests pass | identical sets | Met |
| NFR-12 production start | healthy 17 s after `up` from freshly built images (existing volume) | within 2 minutes | Met |
| NFR-13 licences | npm production tree: MIT 176, Apache-2.0 55, BSD-3-Clause 11, ISC 8, 0BSD 1; Maven licence report not run | only allowed licences | Partly (F-50) |
| NFR-14 stale save stores nothing | `ConcurrentSaveIT` and the stale-save tests pass in the backend run | a stale save stores nothing | Met |
| NFR-15 release checklist files | `docs/openapi.json`, install and backup guide, user guide exist | three files | Met |

### Threats and controls

Unchanged from run 2: the backend and the security design did not change; the backend security and isolation tests all pass again (the only backend failure is `EX-007-05`). The only code change since run 2 is the F-56 `GroupShell` fix (the screen shows Not found; isolation was never in doubt) and the F-55 `AttributeEditor` change (labels and a listbox). Live probes on the test stack (`verify3-error-and-headers.log`): no token 401 `auth.token_missing`; garbage token 401 `auth.token_invalid`; a 2.2 MB body gets `request.too_large`; API headers `X-Frame-Options: DENY`, `frame-ancestors 'none'`, `nosniff`, `no-referrer`; frontend CSP and the same headers; a request from a foreign origin gets 403 and no `Access-Control-Allow-Origin`; `/health` answers `application/json` `{"status":"UP"}` (the held-out case F-45 still fails). Scanners: gitleaks 8.28.0 no leaks in 417 commits; `npm audit` gate: frontend 4 High all accepted in D-05, tools 0; `jscpd` 63 clones, 1.84 % (limit 3 %); backend OWASP dependency-check not re-run (`pom.xml` and the npm manifests unchanged since run 1); licences pass. `.env` scan (`verify3-env-leak.log`): file scan and commit scan match only `POSTGRES_DB` (the database name, 8 characters, also a common word; 16 commit messages) and `FIREBASE_PROJECT_ID` (public, 2 files); the other four values of 6 or more characters appear nowhere; Low by D-83 (F-49).

| Threat group | Result |
|---|---|
| S, T, R, I, D, E rows and data items of run 1 | Controlled as in runs 1 and 2 (tests pass again) |
| I: personal data in backups kept longer | Controlled as in run 2 (scripts unchanged) |
| SB-03, SR-09 no secrets | Controlled; F-49 Low (D-83) |
| SB-11 non-root | backend 10001, frontend 101 (`nginx`), checked in the running production containers |

### CI and deployment

- No runner is pinned and `origin/main` was not pushed, so the pipeline never ran remotely. Every job run locally: backend build and `verify -Pquality` fail only at `EX-007-05`; frontend build, test, check pass; browser tests fail as above (UI acceptance 5, Firefox, WebKit Merge, WF-05); contracts and traceability pass (`verify3-contracts.log`, `verify3-traceability-script.log`); gitleaks, `npm audit` gate, licences, `jscpd` pass. Result: not passing (F-51).
- Production: `contact-groups-backend:1.0.0` and `contact-groups-frontend:1.0.0` rebuilt from `HEAD` (`docker build` as in the install guide); `docker compose -p contact-groups -f docker-compose.prod.yml --env-file production.env up -d`: backend healthy 17 s after the start; `/health` `{"status":"UP"}` as `application/json`; frontend 200; plain HTTP to the API 403; backend user 10001, frontend nginx; `restart: unless-stopped` on both; no error in the backend log; Flyway ran. The human's production stack held port 8080 for the whole check; with the human's consent in this session its four containers were stopped (volume untouched), the test stack ran, and the production stack was started again from the new images, the TLS proxy with `docker start contact-groups-tlsproxy`. No sign-in flow was run against production. Logs: `verify3-prod-start.log`, `verify3-prod-checks.log`, `verify3-image-*.log`, `verify3-test-stack-up.log`, `verify3-test-stack-down.log`.
- Note for the next run: the backend integration tests publish port 9099, so `mvnw verify` cannot run while the test stack is up (this run ran it first, with the stack down).

### Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| backend, frontend, emulator, PostgreSQL | test stack (`scripts/test-stack.sh up`, empty database, throw-away values; then `seed/`, 10,000 Contacts) | UI acceptance (95), E2E-01..06 in three browsers, UI checks, `perf/measure.mjs`, error and header probes | UI acceptance 90 of 95; E2E 12 of 18 (all of chromium and webkit); the rest as above |
| backend, frontend, PostgreSQL, TLS proxy | production stack, the human's production configuration | start, `/health`, frontend, plain-HTTP refusal, user and restart checks | Healthy; no sign-in flow run |

The test stack was removed afterwards (`scripts/test-stack.sh down`).

### Traceability

| R / AC / EX | Tests | Commits |
|---|---|---|
| 649 defined ids | AC 236 of 236, EX 178 of 178, R 198 of 235 directly (looser rule than run 2) | unchanged from run 2; loop 2 commits `7394819`, `04b29a9` (F-55), `592cbf9` (F-56) name F-ids |

### Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-40 | High | E2E-01, E2E-03 | New Group opens its Contact list. | closed: E2E-01 and E2E-03 pass in chromium and webkit (`verify3-e2e-scenarios.log`) |
| F-41 | Medium (D-86) | hidden evaluation | `R-003-02` fails a held-out case (invalid) at `12e38c7` | accepted as a known limitation, D-86 (lowered from High on the human's approval; the cause is unknown); listed in the release notes |
| F-42 | Medium (D-86) | hidden evaluation | `R-006-07` fails a held-out case (other) | accepted as a known limitation, D-86 (lowered from High on the human's approval; the cause is unknown); listed in the release notes |
| F-43 | Medium (D-86) | hidden evaluation | `R-007-03` fails a held-out case (other); frozen `EX-007-05` also fails (F-08) | accepted as a known limitation, D-86 (lowered from High on the human's approval; the cause is unknown); listed in the release notes |
| F-44 | Medium (D-86) | hidden evaluation | `R-017-02` fails a held-out case (other) | accepted as a known limitation, D-86 (lowered from High on the human's approval; the cause is unknown); listed in the release notes |
| F-45 | Medium (D-86) | hidden evaluation | `R-018-07` fails a held-out case (other) | accepted as a known limitation, D-86 (lowered from High on the human's approval; the cause is unknown); listed in the release notes |
| F-46 | High | T-69, T-70, T-76 | closed in substance in run 2; `perf/measure.mjs` defect is F-58 | closed except F-58 |
| F-47 | Medium | UI acceptance | Five failing frozen tests: `AC-003-07`, `AC-005-06`, `AC-006-04`, `AC-014-03`, `AC-019-06` (`AC-004-04`, `AC-004-08` pass since F-55) | fixed: `AC-005-06` in `ca16728` (the Tag field is a combobox with a visible list); the four frozen-test defects were corrected with the human's approval (D-87, D-90), UI acceptance 95 of 95 (`fix3-ui-acceptance2.log`); the acceptance manifest must be renewed by the human; closed once `/verify` confirms |
| F-48 | Medium | NFR-07, NFR-08 | Firefox does not start; WebKit Merge screen crashes on `page.reload` with a mocked 500; unchanged | open item for the release notes |
| F-49 | Low (D-83) | `.env` scan | Same two non-secret values; 16 commit messages now | accepted, D-83 |
| F-50 | Medium | NFR-09, NFR-10, NFR-13 | Keyboard walkthrough, NFR-10 counting rule and Maven licence report still open | partly fixed: keyboard-only walkthrough of the four flows passes at three widths in chromium and webkit (`keyboard-walkthrough.stack.spec.ts`, `fix3-keyboard-*.log`); Maven licence report run: five dual or weak-copyleft libraries, D-88 waits for the human (`fix3-maven-third-party.txt`); NFR-10 counting rule open, 6 to 7 clicks or 17 to 20 actions depending on the reading, D-89 |
| F-51 | Medium | DoD-R-15 | CI never ran; the human pushes `main` | open item for the release notes |
| F-52 | Medium | DoD-R-08 | WF-08, WF-19, WF-07 main page have no UI check | fixed in `1ca570f`: `contact-screens.stack.spec.ts` checks WF-08, the main page of WF-07 and WF-19 at three widths with axe and overflow; chromium light 9 of 9, dark pass, webkit 9 of 9 (`fix3-f52-*.log`); closed once `/verify` confirms |
| F-53 | Low | DoD-R-07, DoD-R-12 | 47 long subjects stay | accepted, history is not rewritten |
| F-54 | Low | `npm run check` | fixed, no ESLint warning | closed |
| F-55 | High | E2E-01 step 5, `AC-004-04`, `AC-004-08`, DoD-P01 | fixed in `7394819` and `04b29a9` | closed: `AC-004-04`, `AC-004-08` pass; E2E-01 passes in chromium and webkit |
| F-56 | High | E2E-03 step 2, DoD-P01 | fixed in `592cbf9` | closed: E2E-03 passes in chromium and webkit |
| F-57 | Medium | `ui-checks/group-screens.stack.spec.ts:38`, NFR-08 | WF-05 "created" check fails in chromium (light, dark) and webkit at all widths: the dialog opens the new Group, the check waits for the Group's card in the list; the check is not frozen | fixed in `11d5b6e`: the check expects the opened new Group (Contact list heading and Group name); chromium 3 of 3 (`fix3-f57-green.log`), dark pass; closed once `/verify` confirms |
| F-58 | Medium | `perf/measure.mjs`, T-69, DoD-P03, NFR-04 | The script joins the 19 invited Users with `GET /me`, but the contract accepts Invitations on `GET /groups` (R-014-05): 1,652 of 1,787 requests are 404 in a 1-minute run. With `GET /groups` added, NFR-04 passes | fixed in `ce9b26a`: the script lists the Groups after `/me` and stops unless every User is a Member; `--only nfr04 --minutes 1` on the test stack: 21 Members, 1,792 requests, 0 not 200, 0 times 429, reads p95 48 ms (`fix3-nfr04.log`); closed once `/verify` confirms |

Carried and not re-opened: F-08 (Low, see F-47), F-14 (Medium, D-79), F-19 (Low, waits for the human, D-68 d), F-20..F-25 (Low), F-38 (Medium, D-82), F-39 (Low).

### Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-40..F-46, F-50, F-53, F-54 (D-84) | 10 `fix` commits and 3 `test` commits | run 2: F-46, F-54, `EX-019-03`, NFR-14 and the restore confirmed; F-41..F-45 not; F-57, F-58 new |
| 2 | F-55, F-56 (S8) | `7394819`, `04b29a9`, `592cbf9` and their documents | run 3: F-55, F-56 and F-40 confirmed; F-41..F-45 not |

### Human to do

1. F-41..F-45: decided (D-86, known limitation). If you later want them fixed, look at the five cases in the hidden vault and describe them; `/fix` cannot see them.
2. D-68 d (F-19): the version of a Survivor that changes nothing.
3. Run Firefox, the real Excel check (Q-06) and real Google sign-in on a machine where they work.
4. Push `main` so CI can run (F-51); I did not push.
5. Your production stack was stopped for the length of this check and is running again from images built from `HEAD`; please check that your own sign-in still works.

## Run 4

> **Self-check by the agent in a separate session (S12), not an independent review.** Commit under test: `f56ae0e` (`HEAD` when the runner produced the hidden evaluation; `68c90b7` only adds that evaluation). Run 4 was approved by the human as the last round (D-91, option 1). Result: **phase 6 gate not passed**: no Critical or High is open, but `DoD-R-01`, `DoD-R-13`, `DoD-R-14` and `DoD-R-15` are not met (open Medium findings below, accepted as known limitations by D-86 and, for the rest, D-91 and D-92). F-47, F-52, F-57 and F-58 are confirmed fixed; one new Medium finding, F-59 (CI), was found. Logs: `out/logs/verify4-*.log`. Nothing was pushed.

### Procedure result

| Step | Result |
|---|---|
| 1 run number | run 4, approved by D-91 (option 1) |
| 2 manifests | input manifest 77 of 77 and acceptance manifest 59 of 59 hashes match (`verify4-manifests.log`); hidden manifest "ok" in the runner's evaluation (the vault was not read) |
| 3 suite, checks, hidden evaluation | see "Definition of Done". Hidden evaluation of `f56ae0e` = `HEAD` (run 5 of `06_hidden-evaluation.md`): 214 tests, 209 pass, 5 fail, the same five (F-41..F-45). Backend not changed since run 3, so JaCoCo not re-measured (97.71 % lines in run 3). Frontend changed (`useSuggestions.tsx` new, `AttributeEditor.tsx`, `TagInput.tsx`): coverage re-measured once, 97.71 % lines (1,494 of 1,529), 94.84 % branches (`verify4-frontend-coverage.log`); Stryker once on those three files (`verify4-stryker-changed.log`): `useSuggestions.tsx` 56.67 %, `AttributeEditor.tsx` 89.36 %, `TagInput.tsx` 89.19 %, together 75.36 % (159 killed, 47 survived, 5 not covered) |
| 4 runtime | test stack (Compose, emulator, empty database, then `seed/` and the 10,000-Contact seed); sign-in as a seed User and the group list checked in the browser pane; production stack started from freshly built images |
| 5 NFR | measured, see "NFR" |
| 6 UI | see "UI and accessibility" |
| 7 security | see "Threats and controls" |
| 8 logs, errors, secrets | live probes; 104 test-stack and 149 production backend log lines: no stack frame, token, password word or e-mail address; `.env` scan: only the two non-secret values of F-49 (below) |
| 9 CI, deployment | see "CI and deployment" |
| 10 traceability | see "Traceability" |

### Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-R-01 full suite passes, hashes match | **Not met** | hashes match. Backend 845 tests (197 unit, 243 + 400 + 5 integration): 844 pass, `EX-007-05` fails (F-08, D-68 i). Frontend unit 597 of 597. UI acceptance **95 of 95** (F-47 fixed). E2E scenarios: chromium 6 of 6, webkit 6 of 6, firefox 0 of 6 (does not start, F-48). `verify4-backend-quality.log`, `verify4-frontend-test.log`, `verify4-ui-acceptance.log`, `verify4-e2e-scenarios.log` |
| DoD-R-02 format, lint, type check, static analysis | Met | `spotless:check`, Checkstyle 0 violations, SpotBugs 0 bugs, PMD pass (`verify4-static-analysis.log`, run as separate goals because Maven stops `verify -Pquality` at `EX-007-05`); Prettier, ESLint, `tsc -b`, i18n tests pass (`verify4-frontend-check.log`); frontend build passes |
| DoD-R-03 quality gate | Met with a decision | line coverage of both components above the gate; frontend mutation below 90 % (F-38, D-82); the new `useSuggestions.tsx` (56.67 %) adds survivors, the full figure was not re-measured (about one hour) |
| DoD-R-04 architecture constraints, no cycles | Met | architecture tests pass in the backend run |
| DoD-R-05 no open Critical or High | Met after D-86 | none open |
| DoD-R-06 components run, core flows work | Met | all components start; E2E-01..06 pass in chromium and webkit |
| DoD-R-07 every AC, EX, R traces to a test and a commit | Met in the main | see "Traceability" |
| DoD-R-08 final theme, three widths, no Critical/Serious accessibility violation | Partly | chromium light 100 of 100 and dark 100 of 100, webkit light 97 of 100 (WF-18 Merge crashes, F-48); no accessibility violation on any screen that ran; Firefox not run; WF-08, WF-19 and WF-07 main page now have checks (F-52 fixed); the structure against the approved wireframes was not compared again |
| DoD-R-09 READMEs from a clean checkout | Not yet checked | `/release` |
| DoD-R-10 release notes | Not yet written | `/release` |
| DoD-R-11 decisions resolved or pending review; inputs unchanged | Met | 91 decisions; the eight old "pending (blocking)" records (D-03, D-04, D-06, D-07, D-11, D-14, D-15, D-16) are answered by follow-up records, D-88 waits for the human, D-89 and D-82 pending review; input manifest matches; `check-decision-ids.sh`: ids unique |
| DoD-R-12 commit history | Met in part | 50 subjects over 72 characters of 433 commits, history not rewritten (F-53) |
| DoD-R-13 hidden suite passes | **Not met**, accepted for five cases (D-86) | 209 of 214 at `HEAD`; the five failing cases are the accepted known limitation |
| DoD-R-14 every NFR meets its pass value | **Not met** | see "NFR" (NFR-07, NFR-08 Firefox; NFR-10; NFR-13) |
| DoD-R-15 CI pipeline passes | **Not met** | never ran remotely; locally the `images` job also fails at the production Compose check (F-59); see "CI and deployment" |
| DoD-R-16 production files build and start; every threat has a checked control | Met | images rebuilt from `HEAD`, production stack healthy 18 s after `up` |
| DoD-P01 E2E-01, -02, -03, -04, -06 against the Compose stack | Met in chromium and webkit | all pass; Firefox does not start (F-48) |
| DoD-P02 isolation test on every endpoint | Met | `IsolationIT`, `ContractParityIT`, `EndpointSecurityTest`, `AC-018-04` pass; E2E-03 passes |
| DoD-P03 NFR-01..05 on the deployed system | Met | NFR-01..04 pass with the scripts as delivered (F-58 fixed), NFR-05 present |
| DoD-P04 four customer deliverables | Met in the main | as in run 2; content of the guides was not tested by a person |
| DoD-P05 later features finished, E2E-05 passes | Met | E2E-05 passes in chromium and webkit |
| DoD-P06 no Critical or High open | Met after D-86 | none open |
| DoD-P07 restore tested once | Met | run 2 (`verify2-backup-restore.log`); `scripts/` and the backend are unchanged since, so not repeated |

### UI and accessibility

`frontend/ui-checks/` (overflow, axe WCAG 2.2 AA, no Critical or Serious violation; 100 tests now: 79 of run 3, 9 for F-52, 12 for the keyboard walkthrough) at 375, 768 and 1440 px against the test stack. Chromium dark through `colorScheme` in a scratch configuration outside the project. Firefox does not start (F-48).

| Screens | Widths and modes | Baseline result | Accessibility violations |
|---|---|---|---|
| all screens with a check file, including WF-05 "created", WF-08, WF-19 and the main page of WF-07, and the keyboard-only walkthrough of the four main flows | 3 widths; chromium light 100 of 100, chromium dark 100 of 100, webkit light 97 of 100 | pass, no overflow | none Critical or Serious |
| WF-18 Merge | chromium pass; webkit **fails** at all three widths | tab crash at `page.reload` with a mocked 500 (F-48, unchanged) | none seen before the crash |
| WF-06 list, desktop, webkit | timed out (30 s) once in the full parallel run, passes alone (`verify4-ui-checks-webkit-wf06-rerun.log`) | load timeout of five parallel WebKit workers, not a defect | none seen |

The Attribute/Tag suggestion list that changed in F-47 was not compared against the wireframe by eye in this run; `AC-005-06`, the axe checks and the keyboard walkthrough pass.

### NFR

Measured on the test stack with the 10,000-Contact seed, scripts as delivered (`verify4-nfr-measure.log`).

| NFR | Measured | Pass value | Result |
|---|---|---|---|
| NFR-01 list, search, filter | median 12 to 55 ms, p95 17 to 78 ms | median and p95 below 1 s | Met |
| NFR-02 open a Contact | median 7 ms, p95 14 ms | p95 below 500 ms | Met |
| NFR-03 export of 1,000 Contacts | 173 to 362 ms | each below 10 s | Met |
| NFR-04 20 concurrent Users, 5 minutes | 21 Members; 8,986 requests, 0 not 200, 0 times 429, reads median 19 ms, p95 54 ms, max 283 ms | zero errors, zero 429, reads below 1 s | Met (F-58 fixed) |
| NFR-05 restart and health | `restart: unless-stopped` on both application containers, `HEALTHCHECK` healthy, install guide exists | both present | Met |
| NFR-06 backup and restore | as run 2, scripts unchanged | counts equal | Met |
| NFR-07 E2E in three browsers | chromium 6 of 6, webkit 6 of 6, firefox does not start | all pass | Partly (F-48) |
| NFR-08 three widths, no overflow | chromium light and dark 100 of 100, webkit 97 of 100 (the 3 failures are the WF-18 crash), firefox not run | no horizontal scroll | Partly (F-48) |
| NFR-09 axe and keyboard walkthrough | axe clean on every screen that ran; keyboard-only walkthrough of the four flows passes at three widths in chromium and webkit | zero violations and walkthrough passes | Met in chromium and webkit (Firefox not run) |
| NFR-10 actions in E2E-01 | 6 to 7 clicks (reading A) or 17 to 20 actions (reading B); under the conservative reading of D-89 option 2: 17 | at most 12 | Not met under reading B, met under reading A (D-89 pending review) |
| NFR-11 catalog | i18n tests pass | identical sets | Met |
| NFR-12 production start | healthy 18 s after `up` from freshly built images | within 2 minutes | Met |
| NFR-13 licences | npm production tree: MIT 176, Apache-2.0 55, BSD-3-Clause 11, ISC 8, 0BSD 1 (`verify4-licences.log`); Maven report of fix loop 3: five dual or weak-copyleft libraries, D-88 waits for the human | only allowed licences | Partly (D-88) |
| NFR-14 stale save stores nothing | `ConcurrentSaveIT` and the stale-save tests pass in the backend run | a stale save stores nothing | Met |
| NFR-15 release checklist files | `docs/openapi.json`, install and backup guide, user guide exist | three files | Met |

### Threats and controls

Unchanged from runs 2 and 3: the backend, the security design and the Compose files did not change; every backend security and isolation test passes again (the only backend failure is `EX-007-05`). Live probes on the test stack (`verify4-error-and-headers.log`): no token 401; garbage token 401 `auth.token_invalid`; a 2.3 MB body gets `request.too_large`; API headers `X-Frame-Options: DENY`, `frame-ancestors 'none'`, `nosniff`, `no-referrer`; frontend CSP and the same headers; a request from a foreign origin gets 403; `/health` answers `application/json` `{"status":"UP"}` (the held-out case F-45 still fails). Scanners: gitleaks 8.28.0 no leaks in 433 commits (`verify4-gitleaks.log`); `npm audit` gate: frontend 4 High all accepted in D-05, tools 0; `jscpd` 63 clones, 1.84 % (limit 3 %); OWASP dependency-check re-run with NVD data updated that day (3,683 records): build success, no CVSS 7.0 or higher (`verify4-dependency-check.log`); licences pass. `.env` scan (`verify4-env-leak.log`): `POSTGRES_DB` (an ordinary 8-character word) matches 370 files (the code, documents and logs use the word) and 16 commit messages, `FIREBASE_PROJECT_ID` (public) matches the same 2 files as before (`docs/04_review-INC-2.md`, `docs/decisions-log.md`); the other four values of 6 or more characters appear nowhere; Low by D-83 (F-49).

| Threat group | Result |
|---|---|
| S, T, R, I, D, E rows and data items of run 1 | Controlled as in runs 1 to 3 (tests pass again) |
| I: personal data in backups kept longer | Controlled as in run 2 (scripts unchanged) |
| SB-03, SR-09 no secrets | Controlled; F-49 Low (D-83) |
| SB-08, SB-09 scanners | gitleaks, `npm audit`, SpotBugs with find-sec-bugs, dependency-check: pass |
| SB-11 non-root | backend 10001, frontend `nginx`, checked in the running production containers |

### CI and deployment

- No runner is pinned and `origin/main` was not pushed, so the pipeline never ran remotely. Every job run locally: backend build and `verify -Pquality` fail only at `EX-007-05`; frontend build, test, check pass; browser tests: Firefox, WebKit Merge fail (F-48); contracts and traceability pass (`verify4-contracts.log`, `verify4-traceability-script.log`); gitleaks, `npm audit` gate, licences, `jscpd`, dependency-check pass; the `images` job: both images build, the development and emulator Compose files validate, but `docker compose -f 02_output/docker-compose.prod.yml config -q` **fails** with the dummy values of `ci.yml` because `GOOGLE_APPLICATION_CREDENTIALS` is required (since the D-69 change `db4fb46`) and the workflow does not set it; with a dummy path it passes (F-59, `verify4-compose-config.log`). Result: not passing.
- Production: `contact-groups-backend:1.0.0` and `contact-groups-frontend:1.0.0` rebuilt from `HEAD`; `docker compose -p contact-groups -f docker-compose.prod.yml --env-file production.env up -d`: backend healthy 18 s after the start; `/health` `{"status":"UP"}` as `application/json`; frontend 200; plain HTTP to the API 403; backend user 10001, frontend `nginx`; `restart: unless-stopped` on both; no error in the backend log; Flyway ran. The human's production containers were stopped when this session began (they had exited two hours earlier); they were recreated from the new images with the volume untouched and stopped again afterwards, so the state is as found (the TLS proxy was not touched). No sign-in flow was run against production. Logs: `verify4-prod-start.log`, `verify4-prod-checks.log`, `verify4-prod-backend.log`, `verify4-image-*.log`.
- The backend integration tests publish port 9099, so `mvnw verify` ran first with the test stack down.

### Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| backend, frontend, emulator, PostgreSQL | test stack (`scripts/test-stack.sh up`, empty database, throw-away values; then `seed/`, 10,000 Contacts) | UI acceptance (95), E2E-01..06 in three browsers, UI checks (3 widths, light, dark), `perf/measure.mjs`, error and header probes, sign-in as a seed User and the Group list in the browser pane (Acme, Large Co) | UI acceptance 95 of 95; E2E 12 of 18 (all of chromium and webkit); the rest as above |
| backend, frontend, PostgreSQL | production stack, the human's production configuration, the human's volume | start, `/health`, frontend, plain-HTTP refusal, user and restart checks | Healthy; no sign-in flow run |

The test stack was removed afterwards (`scripts/test-stack.sh down`).

### Traceability

| R / AC / EX | Tests | Commits |
|---|---|---|
| 649 defined ids | AC 236 of 236, EX 178 of 178 named in a test; R 177 of 235 named directly (whole-id match in test files, `verify4-traceability.log`: stricter than run 3's 198, close to run 2's 181), the rest through the AC or EX that cites them; `check-traceability.mjs`: all 684 story ids, 7 BR, 13 SR, 14 SB, 15 NFR mapped | loop 3 commits name their F-ids and ids (`ca16728`, `bcbb525`, `1ca570f`, `11ffe4b`, `11d5b6e`, `ce9b26a`, `aaec851`) |

### Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-38 | Medium | quality gate | Frontend mutation score below 90 %; the new `useSuggestions.tsx` (extracted in F-47) scores 56.67 % (35 survivors) | open, D-82; known limitation for the release notes |
| F-41..F-45 | Medium (D-86) | hidden evaluation | `R-003-02`, `R-006-07`, `R-007-03`, `R-017-02`, `R-018-07` fail a held-out case at `HEAD`, as in runs 1 to 4 | accepted as a known limitation, D-86 |
| F-47 | Medium | UI acceptance | five frozen UI tests | **closed**: UI acceptance 95 of 95 |
| F-48 | Medium | NFR-07, NFR-08 | Firefox does not start (launch times out after 180 s for every test); WebKit Merge screen crashes on `page.reload` with a mocked 500 | open, needs the human's machine; known limitation for the release notes |
| F-49 | Low (D-83) | `.env` scan | the same two non-secret values | accepted, D-83 |
| F-50 | Medium | NFR-09, NFR-10, NFR-13 | NFR-09 keyboard walkthrough passes (closed); NFR-10 counting rule (D-89) and NFR-13 weak-copyleft libraries (D-88) remain | open for NFR-10 and NFR-13; D-88 waits for the human |
| F-51 | Medium | DoD-R-15 | CI never ran; the human pushes `main` | open, known limitation for the release notes |
| F-52 | Medium | DoD-R-08 | WF-08, WF-19, WF-07 main page had no UI check | **closed**: checks pass in chromium light, dark and webkit |
| F-53 | Low | DoD-R-07, DoD-R-12 | 50 long subjects stay (433 commits) | accepted, history is not rewritten |
| F-57 | Medium | `group-screens.stack.spec.ts:38`, NFR-08 | WF-05 "created" check | **closed**: passes in chromium light, dark and webkit |
| F-58 | Medium | `perf/measure.mjs`, NFR-04 | script did not join the invited Users | **closed**: NFR-04 passes with the delivered script, 5 minutes, 0 errors |
| F-59 | Medium | `.github/workflows/ci.yml`, DoD-R-15 | the `images` job runs `docker compose -f docker-compose.prod.yml config -q` without `GOOGLE_APPLICATION_CREDENTIALS`, which the file requires since `db4fb46`; the job fails on every push | open: `/fix` adds a dummy path to the job's `env` (one line in `ci.yml`) |

Closed in earlier runs and not re-opened: F-40, F-46, F-54, F-55, F-56. Carried: F-08 (Low, `EX-007-05`), F-14 (Medium, D-79), F-19 (Low, waits for the human, D-68 d), F-20..F-25 (Low), F-39 (Low).

### Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-40..F-46, F-50, F-53, F-54 (D-84) | 10 `fix` commits and 3 `test` commits | run 2: F-46, F-54, `EX-019-03`, NFR-14 and the restore confirmed; F-41..F-45 not; F-57, F-58 new |
| 2 | F-55, F-56 (S8) | `7394819`, `04b29a9`, `592cbf9` and their documents | run 3: F-55, F-56 and F-40 confirmed; F-41..F-45 not |
| 3 | F-47, F-50 (NFR-09), F-52, F-57, F-58 (S8) | `ca16728`, `9077b49`, `bcbb525`, `1ca570f`, `11ffe4b`, `11d5b6e`, `ce9b26a`, `aaec851` | run 4: F-47, F-52, F-57, F-58 and NFR-09 confirmed; F-59 new |

### Human to do

1. D-88 (licences of five backend libraries) and D-89 (NFR-10 reading): answer or leave them as known limitations; D-68 d (F-19): the version of a Survivor that changes nothing.
2. Run Firefox (F-48), the real Excel check (Q-06) and real Google sign-in on a machine where they work.
3. Push `main` so CI can run (F-51); I did not push. F-59 should be fixed first, otherwise the `images` job fails.
4. Your production containers were stopped when this session began; I recreated them from the new images (volume untouched) and stopped them again. Start them as usual; please check your own sign-in.
