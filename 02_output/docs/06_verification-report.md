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

> **Self-check by the agent in a separate session (S9), not an independent review.** Commit under test: `d453f13`. Result: **stopped at step 3, nothing re-verified**. Logs: `out/logs/verify-run2-manifests.log`.

| Step | Result |
|---|---|
| 1 run number | run 2; no finding has stayed open for three runs |
| 2 manifests | input manifest 77 of 77 and acceptance manifest 59 of 59 hashes match; the hidden manifest is checked by the runner |
| 3 hidden evaluation | **stopped**: the latest run in `06_hidden-evaluation.md` is of commit `3c49bdb`, `HEAD` is `d453f13`. It still shows F-41..F-45 as failing, so it says nothing about the loop 1 fixes. The full suite, E2E, UI and NFR checks were not run, because F-40 (E2E) and the NFR script need the test stack on port 8080, which the human's production stack holds. |
| 4..10 | not run in this session |

**Needed from the human:** run `node system/run.mjs --hidden-eval` (automatic with `/run`). The test stack needs port 8080: stop the production stack for the run with consent and start it again afterwards. Then start `/verify` again; it is still run 2 (no run section was completed).
