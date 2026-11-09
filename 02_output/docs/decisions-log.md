# Decisions log

> For: agent (writes), human (reads) · Written in: every session · Format: `general/rules/decisions.md` ("Decision record") · Agent: appends only

## D-01: Host tools run but report newer versions than the pins
- Timestamp: 2026-11-08
- Session: S1 /preflight
- Class: allow
- Type: non-blocking
- Trigger: `docker --version` and `docker version` report 29.8.2 (pin 28.5.1); `docker compose version` reports 5.5.1 (pin 2.40.2); `npm --version` reports 11.20.0 (pin `npm-audit` 10.9.3). All three run and the daemon answers. `general/rules/decisions.md` ("Non-blocking").
- Options: 1. Keep the listed pins as authoritative and continue (proposed default). 2. Ask the human to install the pinned versions.
- Human response: none
- Resolution: 1 (the pins in `tech-stack.md` stay authoritative; the host tool is newer). Pending review.
- Overrides: none

## D-02: Replace Maven pins that have a High advisory by the latest patch of the same minor
- Timestamp: 2026-11-08
- Session: S1 /preflight
- Class: allow
- Type: non-blocking
- Trigger: OSV (supplementary lookup, not the pinned `dependency-check-maven`, which cannot run without a JDK) reports High advisories for `spring-boot-starter-actuator` 3.5.7 (GHSA-8hfc-fq58-r658, GHSA-mgvc-8q2h-5pgc; fixed in 3.5.12) and `org.postgresql:postgresql` 42.7.8 (GHSA-98qh-xjc8-98pq fixed in 42.7.11; GHSA-j92g-9f8w-j867 fixed in 42.7.12). Pins are marked `[ASSUMPTION] verify in preflight`. The latest same-minor patches resolve and OSV reports no advisory for them. Evidence: `out/logs/preflight-osv-maven.json`, `out/logs/preflight-resolve.json`.
- Options: 1. Replace within the same minor: every `org.springframework.boot:*` entry 3.5.7 to 3.5.16 (this also moves the BOM-managed `spring-security-test` 6.5.6 to 6.5.11 and `org.testcontainers:*` 1.21.3 to 1.21.4, as the 3.5.16 BOM resolves them); `org.postgresql:postgresql` 42.7.8 to 42.7.14 (above the 42.7.11 that the 3.5.16 BOM manages; the BOM value is a decision-covered exception, fixes the second advisory). Flyway 11.7.2 and JUnit 5.12.2 are unchanged. (proposed default). 2. Keep the pins and treat the findings as blocking.
- Human response: none
- Resolution: 1. Pending review. Not yet verified: `springdoc-openapi-starter-webmvc-ui` 2.8.13 with Boot 3.5.16 (the scaffold in /implement must confirm), and transitive Maven dependencies, which need `dependency-check-maven` after a JDK is installed (see D-04).
- Overrides: tech-stack.md entries listed in Options (version only)

## D-03: Critical and High findings in the npm pins
- Timestamp: 2026-11-08
- Session: S1 /preflight
- Class: block
- Type: blocking
- Trigger: `npm audit` on a lockfile-only resolution of all 50 npm pins (`out/logs/preflight-npm-audit.json`; with latest same-minor patches `...-patched.json`, runtime only `...-patched-runtime.json`). Same-minor patches do not remove any of the findings below, so a version change of a different minor or major, or written evidence to lower a finding, is needed (`general/quality/severity-scale.md`, `general/rules/decisions.md`). No row in "Pre-approved findings" matches.
  Runtime (browser bundle): `react-router` 7.9.4 High (GHSA-2w69-qvjg-hvjx and GHSA-9jcx-v3wj-wh4m, open redirect / XSS; fixed in 7.18.5); `zod` 4.1.12 High (GHSA-mrhx-c7xc-m3w5, memory exhaustion; fixed in 4.6.6, candidate 4.6.7); `firebase` 12.4.0 High, transitive `@grpc/grpc-js` <=1.13.5 and `@firebase/firestore` (still flagged in 12.19.0; only Authentication is used, Firestore is not).
  Build, test and tooling (not shipped): `vite` 7.1.10 High (path traversal, `server.fs.deny` bypass; fixed in 7.3.7); `vitest` 3.2.4 and `@vitest/coverage-v8` 3.2.4 Critical (GHSA-5xrq-8626-4rwp, UI server file read/execute; transitive `tinypool` Critical prototype pollution; fixed in 4.1.11, candidate 4.1.12); `@stryker-mutator/core` and `vitest-runner` 9.2.0 High (fixed in 9.6.1); `shadcn` 3.4.2 High (CLI run once to copy components; candidate 4.23.0); `firebase-tools` 14.22.0 Critical (`tar`) and High (`basic-ftp`, `minimatch`, `proxy-agent`) (test emulator only; still flagged in 14.27.0 and the 15.35.0 suggestion has not been audited).
  Candidate set audited (lockfile only, `--legacy-peer-deps`, peer compatibility not verified): `out/logs/preflight-npm-audit-candidate.json`: remaining High or Critical are only the `firebase` and `firebase-tools` families.
- Options: 1. Move to the candidate versions: `react-router` 7.18.5, `zod` 4.6.7, `vite` 7.3.7, `vitest` and `@vitest/coverage-v8` 4.1.12, `@stryker-mutator/core` and `@stryker-mutator/vitest-runner` 9.6.1, `shadcn` 4.23.0. For `firebase` (keep 12.4.0 or move to 12.19.0) and `firebase-tools` (keep 14.22.0): accept the remaining findings as lowered, on the evidence that Firestore, the gRPC client path, `tar`/FTP/proxy code and the emulator's network exposure are not reachable in the product or the test run (the emulator listens on localhost, runs without internet, never ships). Records the lowering as approved by you (proposed default). 2. As 1, but also evaluate `firebase-tools` 15.35.0 in a later /preflight instead of lowering. 3. Keep all pins and accept every finding as lowered (not recommended: `react-router` and `zod` are in the shipped bundle). 4. Other.
- Human response: none
- Resolution: pending (blocking)
- Overrides: tech-stack.md entries listed in the Options (version only), once answered

## D-04: Host prerequisites missing or wrong
- Timestamp: 2026-11-08
- Session: S1 /preflight
- Class: block
- Type: blocking
- Trigger: only a human can fix these; see `docs/00_preflight-report.md` for the checks. (a) No Java runtime (pin Temurin 21.0.8; also needed for the Firebase emulator and for `dependency-check-maven`, so the Maven vulnerability scan did not complete). (b) Node is 26.11.1 (pin 22.20.0 LTS). (c) `gitleaks` is not on `PATH` (pin 8.28.0). (d) No Playwright browsers are installed (`~/Library/Caches/ms-playwright` is missing), so the blank-page screenshot could not be taken. (e) `.env` does not exist; six keys need values. (f) The working tree has untracked `.agents/` and `.claude/` and there is no `.gitignore`, so `.env` is not ignored. (g) `./mvnw -v` and the `npx` version checks cannot run before /implement scaffolds `02_output/` (no `pom.xml`, `package.json` or wrapper yet).
- Options: 1. Install JDK 21.0.8 (Temurin), Node 22.20.0 (for example through `nvm` or `fnm`, so the host default may stay), gitleaks 8.28.0 (or `docker pull zricethezav/gitleaks:v8.28.0` and approve running it through Docker), then Playwright 1.56.1 browsers (for example `npx playwright@1.56.1 install chromium firefox webkit` under Node 22); copy `01_input/01_project/00_setup/secrets.env.example` to `.env` and fill the six keys; commit `.agents/` and `.claude/` and add a `.gitignore` with `.env` (proposed default). 2. For (b), approve Node 26.11.1 in this record instead of installing 22.20.0 (the pin was written against an LTS; Playwright 1.56.1 and Vite 7 support on Node 26 is unchecked). 3. For (g), preflight accepts "the exact distribution resolves on Maven Central and npm" as the check, and /implement runs `./mvnw -v` and the `npx` version checks in its first step (proposed default). Or: the human supplies Maven 3.9.11 on `PATH` for the check.
- Human response: none
- Resolution: pending (blocking)
- Overrides: none

## D-05: Answer to D-03 (npm Critical and High findings)
- Timestamp: 2026-11-08
- Session: S1 /preflight (re-run with the human's answer)
- Class: block
- Type: approval
- Trigger: follow-up to D-03; D-03 itself is not rewritten.
- Options: as D-03; the proposed default (option 1) was chosen.
- Human response: "Accept defaults" (2026-11-08, in chat) = option 1 of D-03.
- Resolution: Option 1. The version replacements apply to the build manifests: `react-router` 7.18.5, `zod` 4.6.7, `vite` 7.3.7, `vitest` 4.1.12, `@vitest/coverage-v8` 4.1.12, `@stryker-mutator/core` 9.6.1, `@stryker-mutator/vitest-runner` 9.6.1, `shadcn` 4.23.0 (all resolve; checked by a lockfile-only install and audit, see `out/logs/preflight-npm-audit-candidate.json`). `firebase` stays at 12.4.0 (the option allowed 12.4.0 or 12.19.0; both carry the same finding, so the smaller change was chosen; tell the human if 12.19.0 is wanted). The remaining `firebase` (transitive `@grpc/grpc-js`, `@firebase/firestore`) and `firebase-tools` 14.22.0 (`tar`, `basic-ftp`, `minimatch`, `proxy-agent`) findings are accepted as lowered by the human, on the stated evidence: Firestore and gRPC are not used (Authentication only), and `firebase-tools` runs only as the local test emulator on localhost without internet and is never shipped. Open for later phases: peer compatibility of vitest 4 with msw, jsdom and the Stryker vitest runner is unchecked (/implement verifies it, and any conflict is a new decision).
- Overrides: tech-stack.md entries listed in Resolution (version only)

## D-06: Answer to D-04 (host prerequisites)
- Timestamp: 2026-11-08
- Session: S1 /preflight (re-run with the human's answer)
- Class: block
- Type: blocking
- Trigger: follow-up to D-04; re-check on 2026-11-08 found nothing changed on the host: no JDK, Node 26.11.1, no gitleaks (binary or image), no Playwright browsers, no `.env`, no `.gitignore`, untracked `.agents/` and `.claude/`.
- Options: as D-04; the proposed defaults were chosen (option 1 for the installs and files, option 3 for the Maven and `npx` checks).
- Human response: "Accept defaults" (2026-11-08, in chat) = option 1 and option 3 of D-04. This approves the plan; it does not do the installs, which only the human can do.
- Resolution: Pending the human's actions. Decided: Node 22.20.0 is required (Node 26.11.1 is not approved); preflight accepts "the exact Maven and npm distributions resolve" for `./mvnw -v` and the `npx` checks, and /implement runs those checks in its first step. Still to do by the human: install JDK Temurin 21.0.8; install Node 22.20.0 (for example through `nvm` or `fnm`) and make it the Node used when `/preflight` runs; install gitleaks 8.28.0 (or pull `zricethezav/gitleaks:v8.28.0`); install the Playwright 1.56.1 browsers; create `.env` from `secrets.env.example` and fill the six keys; commit `.agents/` and `.claude/`; add a `.gitignore` containing `.env`.
- Overrides: none

## D-07: Critical and High findings in transitive Maven dependencies
- Timestamp: 2026-11-08
- Session: S1 /preflight
- Class: block
- Type: blocking
- Trigger: `dependency-check-maven` 12.1.6 (NVD data without an API key, D-08) on the 19 Maven pins with D-02 applied (Spring Boot 3.5.16, pgjdbc 42.7.14, Spring Security 6.5.11, Testcontainers 1.21.4) and the Boot 3.5.16 BOM: 129 libraries, 25 Critical, 47 High, 81 Medium, 6 Low (`out/logs/preflight-dependency-check-report.json`). By library: Tomcat 10.1.55 (8 Critical, 6 High; fixed in 10.1.58, 10.1.61 exists), Netty 4.1.135 (2 Critical, 15 High; fixed in 4.1.137, 4.1.140 exists), HttpClient5 5.5.2 (1 Critical) and HttpCore5 5.3.6 (2 High) (fixed in 5.6.4 and 5.4.2+; 5.6.5 and 5.4.4 exist), Spring Framework 6.2.19 `spring-core` and `spring-web` (6 Critical, 6 High each), Spring Security 6.5.11 `core` and `web` (1 Critical, 2 High each), OpenTelemetry alpha 1.35/1.37 (5 High) and protobuf-java 3.25.5 (1 High) (both through `firebase-admin`), Swagger UI 5.28.1 bundle (2 High, DOMPurify, through springdoc). Matching is by CPE and can include false positives; per `severity-scale.md` the tool's levels are used as reported.
  Test of overrides (explicit `dependencyManagement` versions, `out/logs/preflight-dependency-check-report-overrides.json`): Tomcat 10.1.61, `netty-bom` 4.1.140.Final, HttpClient5 5.6.5 and HttpCore5 5.4.4 remove all their findings. What remains: Spring Framework and Spring Security (the 6.x lines have no fixed release on Maven Central: the newest are 6.2.19 and 6.5.11, the advisories name 6.2.20 and 6.5.12; fixed releases exist in the 7.x line: Spring Framework 7.0.9 and 7.0.10, Spring Security 7.0.8, with Spring Boot 4.0.9 and springdoc 3.2.0), OpenTelemetry, protobuf and Swagger UI.
  No row in "Pre-approved findings" matches. I did not analyse which of the Spring CVEs are reachable in this product, so I cannot offer written evidence to lower them.
- Options: 1. Apply the four overrides above and move the backend to Spring Boot 4.0.9 (Spring Framework 7, Spring Security 7, springdoc 3.2.0; a different major version, so your approval replaces the Spring Boot, Spring Security, springdoc and Boot-starter pins); the next /preflight re-resolves and re-scans the new set, including the Firebase and Swagger findings, and anything left becomes a new decision (proposed default). 2. Apply the four overrides, stay on Spring Boot 3.5.16 and accept the Spring Framework and Spring Security findings as lowered; the run would continue with 12 Critical and High findings per library in the shipped web layer and needs per-CVE evidence from you. 3. Apply the four overrides, stay on 3.5.16 and wait for Spring Framework 6.2.20 and Spring Security 6.5.12 (not yet published; the date is unknown); the run stays blocked until then. 4. Other.
- Human response: none
- Resolution: pending (blocking)
- Overrides: tech-stack.md Maven entries named in the Options, once answered

## D-08: Dependency-scan analysers disabled
- Timestamp: 2026-11-08
- Session: S1 /preflight
- Class: allow
- Type: non-blocking
- Trigger: `dependency-check-maven` stopped with "dotnet 8.0 runtime or sdk" missing. Disabled: `assemblyAnalyzerEnabled`, `nugetconfAnalyzerEnabled`, `nuspecAnalyzerEnabled` (.NET only; the project has no .NET code) and `ossindexAnalyzerEnabled` (needs credentials that `secrets.env.example` does not list). The NVD analyser ran without an API key (slow, about 30 minutes for the first download, then cached). At least one analyser completed. `general/rules/decisions.md` ("a scanner analyser disabled for lack of credentials").
- Options: 1. Continue with these analysers disabled (proposed default). 2. Provide an NVD API key to speed up later runs (not required).
- Human response: none
- Resolution: 1. Pending review.
- Overrides: none

## D-09: Host prerequisites installed at the human's request (follow-up to D-06)
- Timestamp: 2026-11-08
- Session: S1 /preflight
- Class: allow
- Type: approval
- Trigger: the human wrote "go ahead do these" after D-06. `AGENTS.md` ("Never") forbids installing software on the host without the human; this record is that approval, limited to the items of D-06. Done, each download checked against the publisher's SHA-256: Temurin JDK 21.0.8+9 in `~/Library/Java/JavaVirtualMachines` (found by `java_home`); Node 22.20.0 (npm 10.9.3) in `~/.local/node-22.20.0` with links `node`, `npm`, `npx` in `~/.local/bin`, which comes first on `PATH` and so replaces Node 26.11.1 for new shells (remove those three links to undo); gitleaks 8.28.0 in `~/.local/bin`; Playwright 1.56.1 browsers (Chromium 141, Firefox, WebKit 26) in `~/Library/Caches/ms-playwright`. Maven 3.9.11 and the Playwright package live only in the session scratch folder, not on the host. `.gitignore` (`.env`), `.env` with the three PostgreSQL values (generated, never displayed) and the commit of `.agents/` and `.claude/` were done.
- Options: 1. Keep as installed (proposed default). 2. Undo.
- Human response: "go ahead do these" (2026-11-08, in chat).
- Resolution: 1. Still open from D-06: `FIREBASE_PROJECT_ID`, `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN` in `.env` (only the human has these values); Firefox crashed on launch inside the session's sandbox ("Operation not permitted"), so only Chromium and WebKit are verified.
- Overrides: none

## D-10: Answer to D-07 (Maven findings): move the backend to Spring Boot 4.0.9
- Timestamp: 2026-11-08
- Session: S1 /preflight (re-run with the human's answer)
- Class: block
- Type: approval
- Trigger: follow-up to D-07; D-07 itself is not rewritten.
- Options: as D-07; option 1 was chosen.
- Human response: "Yes, Boot 4.0.9" (2026-11-08, in chat) = option 1 of D-07.
- Resolution: Option 1. Effective Maven set for the build manifests (the Boot 4.0.9 BOM resolves them; `tech-stack.md` is not edited): all `org.springframework.boot:*` 4.0.9, with `spring-boot-starter-webmvc` in place of `spring-boot-starter-web` (the old name is deprecated in 4.0.9 in its favour); Spring Framework 7.0.10; Spring Security and `spring-security-test` 7.0.8; Tomcat 11.0.27; Netty 4.2.19.Final; springdoc 3.2.0; PostgreSQL driver 42.7.14. Overrides above the BOM, both needed by D-07 evidence: HttpClient5 5.6.5 and HttpCore5 5.4.4 (the BOM has 5.5.2 and 5.3.6, which the scan flags). Consequences of the Boot 4 BOM that the human did not list by name, so please review: Flyway 11.7.2 becomes 11.14.1 (`flyway-core`, `flyway-database-postgresql`); JUnit 5.12.2 becomes JUnit Jupiter 6.0.3 (a major change); Mockito 5.17.0 becomes 5.20.0; Testcontainers 1.21.3 becomes 2.0.5 and its artifacts are renamed (`org.testcontainers:testcontainers-postgresql` and `testcontainers-junit-jupiter` replace `postgresql` and `junit-jupiter`); Jackson 3 and Hibernate Validator 9 arrive. Not verified here and left to /implement (a conflict becomes a new decision): `archunit-junit5` 1.4.1 and `pitest-junit5-plugin` 1.2.3 with JUnit 6, `bucket4j-core` 8.10.1 and `poi-ooxml` 5.4.1 with Boot 4, `firebase-admin` 9.5.0 with Jackson 3. Evidence: the set resolves with Maven 3.9.11 on JDK 21.0.8 (`out/logs/preflight-mvn-resolve-boot4-*.log`); `dependency-check-maven` on it finds no Spring, Tomcat or Netty finding (`out/logs/preflight-dependency-check-report-boot4-9.5.0.json`). It leaves the findings of D-11.
- Overrides: tech-stack.md Maven entries named in Resolution (version and, for Testcontainers and the web starter, artifact name)

## D-11: Remaining Critical and High findings after the move to Boot 4.0.9
- Timestamp: 2026-11-08
- Session: S1 /preflight
- Class: block
- Type: blocking
- Trigger: the scan of D-10's set with the listed `firebase-admin` 9.5.0 and `springdoc-openapi-starter-webmvc-ui` 3.2.0 (`out/logs/preflight-dependency-check-report-boot4-9.5.0.json`): 2 Critical and 8 High in four libraries. (1) Swagger UI bundle 5.34.0 (2 Critical, 2 High; the DOMPurify script in the bundle; CVE-2026-116803, CVE-2026-116804). (2) `opentelemetry-extension-incubator` 1.35.0-alpha (2 High) and `opentelemetry-gcp-resources` 1.37.0-alpha (3 High; CVE-2026-39883, CVE-2026-24051, CVE-2026-29181). (3) `protobuf-java` 3.25.5 (1 High, CVE-2026-0994). Libraries (2) and (3) come only through `firebase-admin` -> `google-cloud-storage` and `google-cloud-firestore`, which this product does not use (it only verifies ID tokens, AR-06). The newest `opentelemetry-extension-incubator` on Central is 1.36.0-alpha, below the advisory's fixed version, so a version change cannot clear it; the match is probably by name, not a real exposure, but that is my reading and not evidence. With `firebase-admin` 9.11.0 the same findings remain and two more High appear (`protobuf` 0.13.0, `protobuf-java` 4.33.6).
  Tested variant C (`out/logs/preflight-dependency-check-report-boot4-C.json`): `firebase-admin` 9.5.0 with `google-cloud-storage` and `google-cloud-firestore` excluded in the Maven dependency, and `springdoc-openapi-starter-webmvc-api` 3.2.0 (OpenAPI description only, without Swagger UI) instead of the `-ui` starter. It resolves and the scan reports no vulnerability at all (85 libraries). Not verified: that Firebase token checking works without the two libraries (/implement must show it with a test against the emulator), and whether any story needs the Swagger UI page (R-018-08 says "OpenAPI description"; I did not read the requirements).
- Options: 1. Use variant C: exclusions on `firebase-admin`, `springdoc-openapi-starter-webmvc-api` instead of the `-ui` starter; /implement shows by test that sign-in checking works without the two libraries and adds the Swagger UI later only through a new decision (proposed default). 2. Keep `firebase-admin` and the `-ui` starter and accept the 10 findings as lowered, on your written evidence (no Firestore, Storage or UI page in use). 3. Mix: exclusions only (fixes 5 High) and keep the UI starter with its 4 findings accepted. 4. Other.
- Human response: none
- Resolution: pending (blocking)
- Overrides: tech-stack.md `firebase-admin` and `springdoc` entries, once answered

## D-12: Answer to D-11: variant C
- Timestamp: 2026-11-08
- Session: S1 /preflight (re-run with the human's answer)
- Class: block
- Type: approval
- Trigger: follow-up to D-11; D-11 itself is not rewritten.
- Options: as D-11; option 1 (variant C) was chosen.
- Human response: "Yes, variant C" (2026-11-08, in chat) = option 1 of D-11.
- Resolution: Option 1. `com.google.firebase:firebase-admin` 9.5.0 is used with `com.google.cloud:google-cloud-storage` and `com.google.cloud:google-cloud-firestore` excluded; `org.springdoc:springdoc-openapi-starter-webmvc-ui` is replaced by `org.springdoc:springdoc-openapi-starter-webmvc-api` 3.2.0 (OpenAPI description, no Swagger UI page). The scan of this set (D-10 set plus these two changes) reports no vulnerability in 85 libraries (`out/logs/preflight-dependency-check-report-boot4-C.json`); it resolves with Maven 3.9.11 on JDK 21.0.8. Left to /implement: show by a test against the Firebase emulator that ID-token checking works without the two excluded libraries (a failure is a new decision); add a Swagger UI page only through a new decision. The Maven vulnerability check is now met; the phase 0 gate still waits on the Firebase values in `.env`.
- Overrides: tech-stack.md `firebase-admin` (exclusions) and `springdoc` (artifact `-ui` to `-api`) entries

## D-13: Close the blocking records of the phase 0 run
- Timestamp: 2026-11-08
- Session: S1 /preflight (re-run)
- Class: allow
- Type: non-blocking
- Trigger: records D-03, D-04, D-06, D-07 and D-11 end with "pending (blocking)" and are not rewritten. Their answers and outcomes are in follow-up records; this record states the resolution of each so no record lacks one.
- Options: none.
- Human response: see the records named below.
- Resolution: D-03 resolved by D-05 (option 1). D-04 resolved: the installs by D-09, the Maven and `npx` checks by D-06 option 3, the `.env` values, the `.gitignore` and the committed `.agents/` and `.claude/` by the 2026-11-08 re-run (all six keys non-empty, `.env` ignored, clean tree). D-06 resolved by D-09 and the same re-run. D-07 resolved by D-10 (option 1). D-11 resolved by D-12 (option 1). Still "pending review" (non-blocking, nothing waits on them): D-01, D-02, D-08.
- Overrides: none

## D-14: Roles for actions the interview does not assign
- Timestamp: 2026-11-08
- Session: S2 /clarify
- Class: block
- Type: blocking
- Trigger: `business-rules.md` permission matrix rows marked `*` and the story rules that depend on them: Editor may see Trash (US-012 R-012-03, R-012-08); Owner and Editor may see and restore Versions, Viewer refused (US-022 R-022-06, US-023 R-023-06); Owner and Editor may run the duplicate search and mark a set "not a duplicate", Viewer refused (US-024 R-024-09, US-025 R-025-04); every Member including a Viewer may see the Member list with display names and e-mail addresses (US-015 R-015-01); an Owner may invite with the Role Owner (US-014 R-014-01). The interview gives the Roles (Q-08) but none of these actions. The developer answer to OQ-015-01 ("yes, as written") is about the three Roles and the rule "no in-app administrator", not about these rows.
- Options: 1. As written (proposed default): Owner and Editor see Trash, Versions and run the duplicate search and "not a duplicate"; Viewers do none of these; all Members, Viewers too, see the Member list; an Owner may invite with any of the three Roles. 2. As 1, but the Member list only for Owners and Editors (Viewers see no other person's e-mail address). 3. As 1, but an Invitation may carry only Editor or Viewer (an Owner is made by changing the Role of a Member). 4. Both 2 and 3. 5. Other.
- Human response: none
- Resolution: pending (blocking)
- Overrides: none (the matrix rows stay as written unless an option other than 1 is chosen)

## D-15: Deletion timing and retention not given by the interview
- Timestamp: 2026-11-08
- Session: S2 /clarify
- Class: block
- Type: blocking
- Trigger: personal-data retention and deletion with no source in the interview. (a) US-013 R-013-04 [ASSUMPTION]: a Contact is deleted permanently within 24 hours after its 30 days in Trash; US-020 R-020-05 and BR-04 give no delay for a Group; one clean-up job is assumed for both. (b) US-016 R-016-07 [ASSUMPTION]: History entries are kept as long as the Group exists. (c) `security-requirements.md` ("Personal data") [ASSUMPTION]: server log kept 30 days, set by the operator. (d) US-013 R-013-06 [ASSUMPTION]: the History entry for permanent deletion holds no personal data of the Contact. The developer answers (OQ-012-01, OQ-013-02, OQ-020-02) settle only the 30 days.
- Options: 1. Accept (a) to (d) as written: clean-up at most 24 hours late for Contacts and Groups, History kept while the Group exists, logs 30 days, no personal data in the deletion entry (proposed default). 2. As 1, but the clean-up runs so that nothing stays more than 1 hour after day 30. 3. As 1, but another log retention (state the days). 4. Other.
- Human response: none
- Resolution: pending (blocking)
- Overrides: none

## D-16: Account deletion when a Group is in Group Trash; Invitations after account deletion
- Timestamp: 2026-11-08
- Session: S2 /clarify
- Class: block
- Type: blocking
- Trigger: US-021 R-021-02 refuses account deletion for a "Sole Owner of at least one Group" but does not say whether a Group in the Group Trash (US-020, 30 days, no earlier permanent deletion) counts. R-021-01 removes all Memberships; the data model and SR-10 require every Group to have an Owner. Without an answer the account cannot be deleted for up to 30 days, or a Group in Trash is left without an Owner. US-021 EX-021-04 [ASSUMPTION]: pending Invitations to the deleted account's e-mail address stay pending, so a person who registers again with that address and verifies it joins the Group (access).
- Options: A1. A Group in the Group Trash counts: deletion is refused, the Group is listed with the days left and the instruction to restore it and hand it over, or wait for its permanent deletion (proposed default). A2. It does not count: the account is deleted and the Group stays in Trash without an Owner until day 30 (nobody can restore it). A3. The confirmation names such a Group and deletes it permanently with the account. B1. Pending Invitations to the address stay pending (proposed default). B2. They are cancelled when the account is deleted. Other: describe.
- Human response: none
- Resolution: pending (blocking)
- Overrides: none

## D-17: Developer answers recorded in the inputs are used; customer confirmation stays for the review
- Timestamp: 2026-11-08
- Session: S2 /clarify
- Class: allow
- Type: non-blocking
- Trigger: these open questions are marked OPEN "needs customer" (rights, personal data, deletion, consent, destructive action) but carry a developer answer from the 2026-11-08 review in the story file: OQ-001-01, OQ-002-01, OQ-003-01, OQ-005-01, OQ-012-01, OQ-013-01, OQ-013-02, OQ-014-01, OQ-014-02, OQ-015-01, OQ-018-01, OQ-020-01, OQ-020-02, OQ-021-01, OQ-025-01, OQ-026-01, OQ-026-02, OQ-P03. The inputs settle them (`general/rules/decisions.md`, class Allow: written in the inputs). Product questions OQ-P01, OQ-P02, OQ-P04 to OQ-P10 have no answer; none changes behaviour: OQ-P05 (controller, EU, special data) is handled outside the software (Q-46, `security-requirements.md`), OQ-P06 is a deployment value read from the environment (SR-09, SR-13), OQ-P02 and OQ-P04 are the developer's proposals that DoD-P03 already allows the customer to replace, OQ-P01, OQ-P08 and OQ-P09 have no effect on the stories.
- Options: 1. Use the developer answers as the written rule; the customer confirms at the review (`/run` asks, `intent.md` "Open questions") and a different customer answer becomes a new decision (proposed default). 2. Treat each as blocking until the customer answers.
- Human response: the developer answers quoted above, in the input files (2026-11-08 review).
- Resolution: 1. Pending review by the customer; nothing waits on it.
- Overrides: none

## D-18: Lifecycle and access-order gaps between stories
- Timestamp: 2026-11-08
- Session: S2 /clarify
- Class: open
- Type: non-blocking
- Trigger: places where stories or rules read together leave more than one behaviour, none a `Block` topic beyond D-14 to D-16. (a) US-018 R-018-02 and EX-018-08 answer 404 for a Group in Trash, but US-020 R-020-04 and R-020-07 let an Owner list and restore it. (b) US-014 R-014-05 joins a person when the Group list opens; a Group in the Group Trash is not listed. (c) US-018 R-018-01, R-018-02, R-018-03, R-018-06 do not say in which order the checks apply to one request. (d) US-001 and `security-requirements.md` name e-mail with password and Google, but not what happens when both belong to one e-mail address (the User e-mail is unique, data model). (e) BR-03 lists the audited actions as a closed set; deleting or restoring a Group, and the end of a Membership by account deletion (US-021), are not in it. (f) US-012 shows "the user who deleted", US-022 R-022-02 shows the user of a Version, and US-021 R-021-03 only covers History entries.
- Options: 1. (a) Group-scoped endpoints answer 404 for every caller while the Group is in Trash; the Group Trash list and restore are separate Owner-only endpoints that answer 404 to everyone else. (b) A Group in Trash gives no access through an Invitation; the Invitation stays pending until it expires or the Group is restored. (c) Order: token (401), verified e-mail (403), Membership (404), Role (403). (d) One account per e-mail address (Firebase default); if a sign-in arrives for an e-mail address held by another Firebase identifier, the system refuses and creates no second User. (e) No History entry for these; the closed set of BR-03 stays. (f) The user shown is the name; "deleted by" comes from the History entry of the deletion; after account deletion only the name is shown (proposed default for all). 2. Ask the human per item.
- Human response: none
- Resolution: 1. Pending review.
- Overrides: none

## D-19: Behaviour and data gaps inside stories
- Timestamp: 2026-11-08
- Session: S2 /clarify
- Class: open
- Type: non-blocking
- Trigger: readings that change observable behaviour but no business outcome. (a) A single save that changes fields, Tags and Attributes: US-016 R-016-02, US-004 R-004-06 and US-005 R-005-08 each say "a History entry". (b) US-022 defines the current state as the highest Version, but a Tag rename or delete changes Tags without a new version number (US-006 R-006-07, BR-07). (c) US-024 R-024-03: Contacts A and B share a company, B and C share a phone, A and C neither: one set or two. (d) US-026 R-026-02 "choose any other value of that field": values only from the merged Contacts, or typed; EX-026-09 can then arise only through a direct request. (e) US-011 R-011-09 uses "the active UI language" but an export is a REST request. (f) US-018 R-018-10: fixed or sliding minute, and whether 4xx answers count; NFR-04 sends list, search, edit and export from 20 Users. (g) US-011 R-011-03 "alphabetical order" of Tags and of Attribute columns. (h) US-014 R-014-02 does not name the e-mail format rule or length. (i) `business-rules.md` data model gives ContactVersion no time and no user, which US-022 R-022-01 requires.
- Options: 1. (a) One entry per audited action of BR-03 inside the same save (one "Contact edited" entry for fields, one per Tag change, one per Attribute change), all with the same time and user. (b) The comparison uses the live Contact (its fields, Tags and Attributes now) as "current"; stored Versions keep the names they had. (c) One set per group of Contacts with the same names, linked through company or phone (connected Contacts form one set); a Contact is in one set (R-024-08 extended). (d) The chosen value must be one of the values of the merged Contacts; any other value is refused. (e) The export request carries the language (`sl` or `en`); Slovene when absent. (f) Fixed window of one minute per User, every authenticated request counts, 429 from the 101st; the front end debounces typing and the load script of NFR-04 stays at or below 100 requests per User per minute. (g) Alphabetical ignoring upper and lower case and accents (as US-007 R-007-03). (h) As BR-06: valid format, at most 255 characters. (i) ContactVersion also stores time and user (R-022-01 is explicit). (proposed default for all) 2. Ask the human per item.
- Human response: none
- Resolution: 1. Pending review.
- Overrides: none

## D-20: Assumptions in project-wide facts, business rules, e2e scenarios and priorities
- Timestamp: 2026-11-08
- Session: S2 /clarify
- Class: open
- Type: non-blocking
- Trigger: every `[ASSUMPTION]` in `business-rules.md`, `e2e-scenarios.md` and the priority and MVP lines of stories needs a decision (DoR-S-09). Project-wide facts: calendar days in Europe/Ljubljana; English time `HH:mm` and decimal point; ISO 3166-1 country list, stored as two letters. BR-06: phone characters `+ - ( ) . /` with spaces and digits, at most 30 characters. BR-07: a change of Tags or Attributes through the Contact counts as a save, a Tag rename or delete for the Group does not. Data model: Attribute value not empty. Glossary: User, Invitation, Version, Selection, Duplicate set, Survivor, Group activity list. Permission matrix and audited actions marked `*` other than the rows of D-14: "Contact deleted forever", "Tag renamed or deleted for the Group", "Invitation created or cancelled", "Contact version restored" (each only adds an audit entry). E2E steps marked `[ASSUMPTION]` (E2E-01 to E2E-06) are test steps and add no behaviour. Priority and MVP lines marked `[ASSUMPTION]` (US-001, US-012 to US-015, US-019 to US-021): the MVP split of `intent.md` agrees with the `mvp` flags of all 26 stories. NFR and DoD-P numbers (NFR-01 to NFR-09, DoD-P03) are the developer's proposals (OQ-P02, OQ-P04).
- Options: 1. Use each as written (proposed default); the customer confirms at the review. 2. Ask the human per item.
- Human response: none
- Resolution: 1. Pending review.
- Overrides: none

## D-21: Story-level assumptions without business consequence
- Timestamp: 2026-11-08
- Session: S2 /clarify
- Class: open
- Type: non-blocking
- Trigger: `[ASSUMPTION]` markers inside story rules and edge cases that only fix a screen, format or search detail, with the conservative reading in the text itself: US-001 R-001-01 (a display name is asked), R-001-02 (Google name, else e-mail), R-001-05 (one message for both errors), R-001-08 (same confirmation); US-002 EX-002-06 (Groups in Trash do not count); US-003 R-003-05 (trimming), R-003-10 (duplicate e-mail allowed), R-003-11 (no-change save); US-004 R-004-02 and EX-004-04 (empty value refused); US-005 Tag name letters of any alphabet, R-005-06 (Tag stays when unused), R-005-07 (suggestions); US-006 Tag list screen, R-006-05 (also Contacts in Trash); US-007 list columns, R-007-02, R-007-03, R-007-08; US-008 R-008-01; US-009 R-009-05 and R-009-06 (one at a time); US-010 R-010-02, R-010-08, EX-010-03; US-011 R-011-03, R-011-04, R-011-09; US-017 25 per page, EX-017-03; US-019 R-019-03, EX-019-03; US-023 R-023-05 (a deleted Tag is created again); US-024 phone comparison, R-024-08; US-025 R-025-03, undo not offered; US-026 EX-026-03, EX-026-04, EX-026-09; US-013 R-013-06 (see D-15).
- Options: 1. Use each as written (proposed default). 2. Ask the human per item.
- Human response: none
- Resolution: 1. Pending review.
- Overrides: none

## D-22: Glossary gaps and synonyms
- Timestamp: 2026-11-08
- Session: S2 /clarify
- Class: open
- Type: non-blocking
- Trigger: the glossary is "the one vocabulary" (`requirements-standard.md`), but stories use terms that are not in it or name one thing twice. Used in more than one story and defined only inside one: Group list (US-002; used in US-001, US-014, US-020), Contact list (US-007; used in US-008 to US-011), Verified e-mail and Display name (US-001; used in US-014, US-015, US-018), Predefined fields (US-003; used in US-011, e2e), Search text (US-008; used in US-009), Member list (US-015), History tab (US-016), Trash view (US-012). Not defined anywhere: Visitor (US-001), Operator (US-015 R-015-08; `intent.md` Users), Membership (US-021 R-021-01, a second word for "Member"), Group Trash (US-020; the glossary calls it Trash), Invited person (`intent.md`). "Version number" (the Contact's counter, BR-07) and "Version" (a stored state) are both used.
- Options: 1. Keep the story definitions as the meaning everywhere; read Membership as Member of a Group, Group Trash as the Trash of a Group, Visitor as a person who is not signed in, Operator as in `intent.md`, Invited person as a User with a pending Invitation; the version number of a Contact equals the number of its highest Version. Code and tests use the glossary names and no new synonym (proposed default). 2. Add the terms to the glossary through a human change of `business-rules.md` (an input change, so a new decision).
- Human response: none
- Resolution: 1. Pending review.
- Overrides: none

## D-23: Readings of the Definition of Ready and form findings
- Timestamp: 2026-11-08
- Session: S2 /clarify
- Class: open
- Type: non-blocking
- Trigger: (a) DoR-S-05 asks for an edge-case row for every `If … then` rule, while `requirements-standard.md` ("Acceptance criteria") asks the acceptance criteria to cover every `If … then` rule and every boundary in the edge-case table. 21 stories have If-rules with no edge-case row (listed in `docs/01_requirements-check.md`); each is covered by an acceptance criterion that states the refusal and what stays unchanged, except R-004-02, R-007-08, R-008-05 and R-012-05, which only edge-case rows cover. All boundaries have rows. (b) US-018 (16 rules, estimate 5) and US-026 (12 rules, estimate 8) are the largest stories. (c) AC-018-04 and DoD-P02 test every endpoint, which only exists when the last endpoint does; E2E-03 lists AC-018-04 but tests one endpoint. (d) US-001, US-014 (Invitations screen), US-021 (where account deletion is offered) and US-024 (duplicate search screen) do not name every screen (DoR-S-10); `ui-design.md` is not on the reads list of `/clarify`. (e) Front matter `rules:` lacks BR-07 in US-006 and BR-04 in US-022 and lists BR-01 in 17 stories whose text does not repeat it; `business-rules.md` "Stories" columns omit US-007 and US-014 in BR-05 and US-022, US-026 in BR-07. (f) Cosmetic: R-018-15 and R-018-16 and AC-018-17 are out of numeric order; EX-013-06 stands before EX-013-05; E2E-01 step 2 gives no display name; EX-026-09 describes a case the Merge screen cannot produce; `P-4` says "a person" while `intent.md` says the product never contacts the people described by Contacts (Firebase's verification and reset e-mails in US-001 are started by the user). (g) Q ids cited by stories were not checked: `interview.md` is not on the reads list.
- Options: 1. (a) An acceptance criterion with the refusal and "nothing stored or changed" counts as the row; the four rules above keep their edge-case rows as their test oracle. (b) Both stay one story each; `/plan` splits them into tasks. (c) AC-018-04 runs over every endpoint that exists at the end of each increment and fully at the release (DoD-P02); E2E-03 keeps one endpoint. (d) `/wireframes` draws the missing screens and checks all screens against `ui-design.md`. (e) and (f) are read as written, with no effect. (g) The Q ids are plausible (Q-01 to Q-55) and are checked in a later session that reads the interview (proposed default for all). 2. Fail DoR-S-05 for those stories and ask the human to change the inputs.
- Human response: none
- Resolution: 1. Pending review. If the human prefers 2, say so in the answer to this session.
- Overrides: none

## D-24: How each security requirement is checked (DoR-R-05)
- Timestamp: 2026-11-08
- Session: S2 /clarify
- Class: open
- Type: non-blocking
- Trigger: the table of `security-requirements.md` has no "how it is checked" column. NFR-01 to NFR-15 and DoD-P01 to DoD-P07 have one. SR-03 and SR-09 are cited by no acceptance criterion.
- Options: 1. Checks (proposed default): SR-01 AC-018-04, E2E-03, DoD-P02; SR-02 AC-018-01, AC-018-02, AC-018-08; SR-03 a test where the token carries a role claim or none and the database Role differs, plus AC-015-02 and AC-015-03; SR-04 the injection test named in SR-04 and EX-008-03; SR-05 AC-018-16, EX-003-13, EX-004-08; SR-06 AC-011-04, EX-011-03; SR-07 AC-018-15; SR-08 AC-018-12, AC-018-13, AC-018-14; SR-09 gitleaks scan and `.env` untracked (`general/rules/secrets.md`); SR-10 AC-015-04, AC-015-05, EX-015-01; SR-11 AC-014-03, AC-014-05, AC-014-08, EX-014-02; SR-12 AC-018-11; SR-13 tests run offline against the emulator with no real Firebase credential. 2. Ask the customer's security officer.
- Human response: none
- Resolution: 1. Pending review.
- Overrides: none

## D-25: Answer to D-14 (Roles for actions the interview does not assign)
- Timestamp: 2026-11-08
- Session: S3 /clarify (re-run with the human's answer)
- Class: block
- Type: approval
- Trigger: follow-up to D-14; D-14 itself is not rewritten.
- Options: as D-14; option 1 (as written) was the proposed default.
- Human response: "Accept defaults" (2026-11-08, in chat) = option 1 of D-14.
- Resolution: Option 1. Owner and Editor see Trash, Versions and run the duplicate search and "not a duplicate"; Viewers do none of these; all Members, Viewers too, see the Member list; an Owner may invite with any of the three Roles. The `*` rows of the permission matrix (BR-02) and US-012 R-012-03, R-012-08, US-014 R-014-01, US-015 R-015-01, US-022 R-022-06, US-023 R-023-06, US-024 R-024-09, US-025 R-025-04 stand as written.
- Overrides: none

## D-26: Answer to D-15 (deletion timing and retention)
- Timestamp: 2026-11-08
- Session: S3 /clarify (re-run with the human's answer)
- Class: block
- Type: approval
- Trigger: follow-up to D-15; D-15 itself is not rewritten.
- Options: as D-15; option 1 was the proposed default.
- Human response: "Accept defaults" (2026-11-08, in chat) = option 1 of D-15.
- Resolution: Option 1. The clean-up deletes Contacts and Groups at most 24 hours after day 30 (US-013 R-013-04, US-020 R-020-05, BR-04); History is kept as long as the Group exists (US-016 R-016-07); server log retention is 30 days, set by the operator; the History entry for a permanent deletion holds no personal data of the Contact (US-013 R-013-06).
- Overrides: none

## D-27: Answer to D-16 (account deletion with a Group in Group Trash; Invitations)
- Timestamp: 2026-11-08
- Session: S3 /clarify (re-run with the human's answer)
- Class: block
- Type: approval
- Trigger: follow-up to D-16; D-16 itself is not rewritten.
- Options: as D-16; A1 and B1 were the proposed defaults.
- Human response: "Accept defaults" (2026-11-08, in chat) = A1 and B1 of D-16.
- Resolution: A1: a Group in the Group Trash counts as a Group the User solely owns (US-021 R-021-02); account deletion is refused and lists the Group with the days left and the instruction to restore it and hand it over, or wait for its permanent deletion. B1: pending Invitations to the deleted account's e-mail address stay pending (US-021 EX-021-04).
- Overrides: none

## D-28: Close the blocking records of the phase 1 run
- Timestamp: 2026-11-08
- Session: S3 /clarify
- Class: allow
- Type: non-blocking
- Trigger: D-14, D-15 and D-16 end with "pending (blocking)" and are not rewritten, so that no record lacks a resolution.
- Options: none.
- Human response: see D-25, D-26, D-27.
- Resolution: D-14 resolved by D-25 (option 1). D-15 resolved by D-26 (option 1). D-16 resolved by D-27 (A1, B1). Still "pending review" (non-blocking, nothing waits on them): D-01, D-02, D-08, D-17 to D-24.
- Overrides: none

## D-29: Configuration keys the design needs that `environments.md` does not list
- Timestamp: 2026-11-08
- Session: S3 /plan
- Class: open
- Type: non-blocking
- Trigger: ADR-03 (two database roles, AR-09), ADR-11 (HTTPS only behind a TLS proxy, R-018-12) and ADR-15 (one image for every environment, AR-12). `environments.md` and `secrets.env.example` are inputs and cannot be edited from phase 2 on (`AGENTS.md`, "Never"). `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` exist.
- Options: 1. Add these keys, all read from the environment (proposed default): `DB_MIGRATION_USER`, `DB_MIGRATION_PASSWORD` (Compose maps them from `POSTGRES_USER`, `POSTGRES_PASSWORD`; used by Flyway only); `DB_APP_USER` (default `contactgroups_app`), `DB_APP_PASSWORD` (the restricted application role created by migration V1; Compose defaults it to the value of `POSTGRES_PASSWORD` when it is unset, so no new secret is needed to start, and the operator should set a different one in production); `REQUIRE_HTTPS` (default `false`; `true` in `docker-compose.prod.yml`); `SPRING_PROFILES_ACTIVE` (`prod` in production: the backend refuses to start with `FIREBASE_AUTH_EMULATOR_HOST` set); the frontend container reads `VITE_API_BASE_URL`, `VITE_FIREBASE_*` at start and serves them as `/config.js`. 2. Keep one database user (rejected: AR-09 cannot be shown at the database level). 3. Ask the human to extend `secrets.env.example` first.
- Human response: none
- Resolution: 1. Pending review. `DB_APP_PASSWORD` is the only new secret; the human may add it to `secrets.env.example` and `.env` (an input change, not made by the agent).
- Overrides: none

## D-30: Development-only tools added to `tech-stack.md`
- Timestamp: 2026-11-08
- Session: S3 /plan
- Class: allow
- Type: non-blocking
- Trigger: `tech-stack.md` rule "a dependency or tool not listed here may be added only with an exact version and a non-blocking decision record". Needed: contract validation (phase 2 gate), no floating Maven plugin versions (ES-04: Maven has no lock file), a Maven licence report (NFR-13), a CI syntax check (ES-14).
- Options: 1. Add (proposed default), none ships in an image: `@seriousme/openapi-schema-validator` 2.11.0 (MIT), `ajv` 8.20.0 (MIT), `ajv-formats` 3.0.1 (MIT), `yaml` 2.9.1 (ISC), all in `02_output/tools` (`npm audit`: 0 vulnerabilities); `org.apache.maven.plugins:maven-enforcer-plugin` 3.6.1 (Apache-2.0, bans dynamic versions and checks upper bounds); `org.codehaus.mojo:license-maven-plugin` 2.7.1 (Apache-2.0); container `rhysd/actionlint` 1.7.7 (MIT). 2. Write the checks by hand without these tools.
- Human response: none
- Resolution: 1. Pending review. Each is scanned by the scanners of phase 6 like any other.
- Overrides: none

## D-31: Open readings fixed by the specification
- Timestamp: 2026-11-08
- Session: S3 /plan
- Class: open
- Type: non-blocking
- Trigger: points the inputs leave to the design, each fixed in `docs/02_specification.md` with the conservative reading. (a) "Select all N results" sends the filter, not N ids; the server recomputes the ids (a Selection above 10,000 is refused, R-011-05). (b) The Group trash and its restore are Owner-only endpoints `/group-trash` (D-18a). (c) Invitations are accepted when the Group list is read (R-014-05), so `GET /groups` has this write effect. (d) Contacts in Trash are returned only by the Trash endpoints; the Contact endpoint answers 404. (e) The Merge preselection is computed in the browser; the server validates every chosen value against the merged Contacts (D-19d). (f) A merged-away Contact's History is linked to the Survivor through `details.mergedContactIds`. (g) The password-length rule is a client rule, because Firebase owns passwords (R-001-06). (h) The published OpenAPI description is `GET /api/v1/openapi.json` without a UI page (D-12).
- Options: 1. Use each as written (proposed default). 2. Ask the human per item.
- Human response: none
- Resolution: 1. Pending review.
- Overrides: none

## D-32: Backend dependency set changes needed by Spring Boot 4 and by variant C (D-12)
- Timestamp: 2026-11-08
- Session: S3 /plan
- Class: open
- Type: non-blocking
- Trigger: building and testing the backend skeleton on Boot 4.0.9 with Maven 3.9.11 and JDK 21.0.8. (a) Boot 4 moved the Flyway auto-configuration into `spring-boot-starter-flyway` (same group and version as every other Boot starter); the tech-stack entries `flyway-core` and `flyway-database-postgresql` alone start no migration. (b) `spring-boot-starter-webmvc-test` (test scope) holds `@AutoConfigureMockMvc` in Boot 4. (c) With `google-cloud-storage` and `google-cloud-firestore` excluded (D-12), `FirebaseOptions` fails with `NoClassDefFoundError: com.google.api.client.json.jackson2.JacksonFactory`; adding `com.google.http-client:google-http-client-jackson2` 1.46.3 (Apache-2.0, same version as `google-http-client`, depends on `jackson-core`) fixes it, proven by `FirebaseEmulatorIT` (a token issued by the Auth emulator is verified). (d) The `FirebaseAuth` class references `com.google.api.gax.paging.Page` (excluded `gax`); it is therefore not a Spring bean (ADR-04). (e) `maven-site-plugin` 3.12.1 is pinned (the default-lifecycle plugin that `requirePluginVersions` flagged). (f) The Maven Wrapper 3.3.2 check of `distributionSha256Sum` fails on this macOS because the system `sha256sum` rejects `-c`; the sum is correct (the Central SHA-512 and the SHA-256 were both checked). The work in this session used a `sha256sum` shim in the session scratch folder; nothing was changed on the host. Linux CI uses GNU `sha256sum`.
- Options: 1. Keep (a), (b), (c), (e) as dependencies and pins; `dependency-check` of the final set is run in CI and in S3 (the result is in `out/logs/backend-dependency-check.log`); if it reports a Critical or High finding for (c), remove (c), keep `firebase-admin` with its default libraries and ask the human (D-11 options) (proposed default). 2. Ask the human first and stop. 3. Re-include the two excluded libraries (re-opens D-11).
- Human response: none
- Resolution: 1. Pending review and pending the scan result.
- Overrides: tech-stack.md Maven entries (additions only)

## D-33: Frontend dependency set changes needed by the skeleton
- Timestamp: 2026-11-08
- Session: S3 /plan
- Class: open
- Type: non-blocking
- Trigger: installing and building the frontend with the D-05 versions on Node 22.20.0 / npm 10.9.3. (a) `@testing-library/react` 16.3.0 needs the peer `@testing-library/dom`; added `@testing-library/dom` 10.4.1 (MIT, test). (b) ESLint 9 flat config needs `@eslint/js` 9.37.0 (MIT) and `globals` 16.4.0 (MIT); `@types/node` 22.18.8 (MIT) types the Vite, Playwright and test files (all dev only). (c) npm 10.9.3 crashes while resolving the peer set of this list ("Cannot read properties of null (reading 'edgesOut')"; D-05's audit also used `--legacy-peer-deps`). `frontend/.npmrc` sets `legacy-peer-deps=true`; every peer is installed explicitly in `package.json`; `npm ls` shows no unmet peer (one transitive `picomatch` 2.3.2 against `fdir`'s `^3 || ^4` is reported as "invalid"; build, tests and the browser smoke test run without a problem). (d) The `shadcn` 4.23.0 CLI generates components that import the umbrella package `radix-ui` and `next-themes`; those packages are not in `tech-stack.md`, so the components were adapted by hand to the pinned `@radix-ui/react-*` packages and `Label` is a plain `<label>`. (e) The Lucide, Radix, Tailwind and Vitest 4 / msw / jsdom / Stryker versions install together; Vitest 4.1.12 runs the tests with msw 2.11.5 and jsdom 27.0.0. `@stryker-mutator/*` 9.6.1 installs; its run is configured in phase 5. (f) `npm audit`: runtime packages show only the `firebase` family (accepted in D-05); with dev packages 2 low and 17 moderate, no Critical or High besides the same family.
- Options: 1. Keep as built (proposed default). 2. Replace `legacy-peer-deps` by a different npm version (host change, needs the human). 3. Ask the human before any addition.
- Human response: none
- Resolution: 1. Pending review.
- Overrides: tech-stack.md npm entries (additions only)

## D-34: Scanner results of the skeleton (gitleaks, licence check)
- Timestamp: 2026-11-08
- Session: S3 /plan
- Class: open
- Type: non-blocking
- Trigger: (a) gitleaks 8.28.0 over the history reported 2 `generic-api-key` findings in `out/logs/backend-verify.log` and `backend-check.log` (commit d6ac788, line 118): the line `Using generated security password: <uuid>`. Spring Boot creates a default in-memory user with a random password when the application has no `UserDetailsService`, and logs it at start; here the user is never used (stateless bearer tokens). Fixed in code: `SecurityConfig.noLocalUsers()` declares a `UserDetailsService` without users (test `ApplicationIT.thereIsNoLocalUserAndNoGeneratedPassword`); later logs contain no such line (checked). The two committed lines stay in history (no rewrite, `general/rules/commits.md`) and are accepted by fingerprint in `.gitleaksignore`. (b) `license-checker-rseidelsohn` on the frontend production tree: only `lightningcss` (MPL-2.0), the CSS transformer inside Tailwind and Vite, is outside the allowed list; `tech-stack.md` permits MPL-2.0 in build scope, and it is not part of the bundle or of the runtime image (nginx serves static files). The CI licence step excludes `lightningcss*` and the project's own `UNLICENSED` entry and fails on anything else. (c) `npm audit`: `scripts/npm-audit-gate.mjs` fails on any High or Critical package other than the four accepted in D-05.
- Options: 1. Accept as described (proposed default). 2. Rewrite history to remove the two log lines (not allowed by the commit rules). 3. Ask the human.
- Human response: none
- Resolution: 1. Pending review.
- Overrides: none

## D-35: Result of the backend dependency scan (closes the open condition of D-32)
- Timestamp: 2026-11-08
- Session: S3 /plan
- Class: allow
- Type: non-blocking
- Trigger: `dependency-check-maven` 12.1.6 (`dependency-check:check`, fails on CVSS 7.0 or higher, NVD data without an API key, analysers as in D-08) on the final backend dependency set: 86 libraries, 0 vulnerabilities of any severity, build success (`out/logs/backend-dependency-check.log`). `google-http-client-jackson2` 1.46.3 added in D-32 is in the scanned set.
- Options: none.
- Human response: none
- Resolution: D-32 option 1 stands (the added library is kept). The Maven vulnerability check of the skeleton is met. D-32 itself is not rewritten.
- Overrides: none

## D-36: Approve wireframes
- Timestamp: 2026-11-08
- Session: W /wireframes
- Class: open
- Type: approval
- Trigger: `/wireframes` drew every screen of the inventory in `docs/02_specification.md` ("User interface", SC-01..SC-19) as WF-01..WF-19 (`docs/wireframes/README.md`), each with its states (UI-14) at the three widths (UI-22). Images: `docs/wireframes/WF-nn_<slug>_mobile.png`, `…_tablet.png`, `…_desktop.png` for WF-01_sign-in, WF-02_register, WF-03_reset-password, WF-04_verify-email, WF-05_group-list, WF-06_contact-list, WF-07_contact, WF-08_new-contact, WF-09_tags, WF-10_trash, WF-11_members, WF-12_invitations, WF-13_group-activity, WF-14_delete-group, WF-15_group-trash, WF-16_account, WF-17_duplicates, WF-18_merge, WF-19_not-found (57 files). Choices made where the inputs leave room, each changeable with option 3: UI text drawn in Slovene, the default language (R-019-01), with "Ogledovalec" for Viewer, "Urednik" for Editor, "Lastnik" for Owner; Group navigation as a tab bar on desktop and tablet and a menu button on mobile (the spec allows tab bar or side bar); filters as a side panel on desktop, behind a "Filtri" button below 1024 px; tablet hides Kraj and Država in the contact list and Podjetje and Izbrisano in Trash; Group deletion is reached from the card menu of WF-05 and "Možnosti skupine" in the Group bar; WF-17 asks no confirmation for "Ni podvojeno" (no story asks for one); WF-18 confirms the merge in a dialog (R-026-05). No global search in the header, although UI-27 lists one: the spec's navigation has none and no story needs it.
- Options: 1 approved with the customer (default); 2 approved by the developer only; 3 change (what, per `WF-nn`, for example `D-36: 3 WF-06 move filters above the list`)
- Human response: none
- Resolution: pending review
- Overrides: none

## D-37: Answer to D-36 (wireframes approved with the customer)
- Timestamp: 2026-11-08
- Session: W /wireframes (re-run with the human's answer)
- Class: open
- Type: approval
- Trigger: follow-up to D-36; D-36 itself is not rewritten.
- Options: as D-36; the proposed default (option 1) was chosen.
- Human response: "D-36: 1 (with customer)" (2026-11-08, in chat) = option 1 of D-36.
- Resolution: Option 1. WF-01..WF-19 (`docs/wireframes/`, commit `2994e88`) are approved with the customer, including the choices listed in D-36. They bind `/acceptance-tests`, `/implement` and `/design`; a later change to structure is a decision, not a styling choice. The phase 2 gate is passed.
- Overrides: none

## D-38: Readings of the test harness (phase 3)
- Timestamp: 2026-11-08
- Session: S4 /acceptance-tests
- Class: open
- Type: non-blocking
- Trigger: the frozen tests need ways to set up time, bulk data and limits that the inputs do not spell out (`general/testing/writing-tests.md`, `environments.md`, ADR-09, ADR-16).
- Options: 1 (default) the readings below; 2 leave the time-, bulk- and limit-dependent AC/EX without tests.
- Human response: none
- Resolution: pending review. Option 1: (a) time: the backend acceptance tests replace the `java.time.Clock` bean with a movable clock (ADR-09); the product must take every time (History, Trash, Invitations, Versions, export date) from that bean. (b) clean-up: `TRASH_PURGE_CRON` is `* * * * * *` in the acceptance context, so BR-04 runs within a test; nothing calls internals. (c) bulk fixtures of 10,000+ Contacts and one Version with content of an earlier rule (AC-023-04) are written with SQL through the schema (the persistence contract, AR-04), as `seed/generate-large.sql` does; purge results (AC-013-04, AC-020-04) are checked in the database. (d) `RATE_LIMIT_PER_MINUTE` is raised in every context except `Us018RateLimitOwnContextAcceptanceIT` (default 100); the Compose test overlay `docker-compose.test.yml` raises it for the browser tests. (e) tests with their own environment run in a JVM of their own (failsafe execution `acceptance-own-context`), because the product registers one named `FirebaseApp` per JVM. (f) `frontend/e2e/smoke.spec.ts` (skeleton UI check) moved to `frontend/ui-checks/` so that `e2e/` holds only E2E-01..06; `npm run e2e` still runs the UI checks for CI. (g) where a screen must show a time or a server answer that the stack cannot produce on demand (AC-016-03, EX-016-04, AC-019-04, AC-001-11, AC-011-05, EX-026-03), the browser test serves that answer in the format of the contract.
- Overrides: none

## D-39: EX-019-03 has no screen that shows a decimal number
- Timestamp: 2026-11-08
- Session: S4 /acceptance-tests
- Class: open
- Type: non-blocking
- Trigger: EX-019-03 (`1234,5` / `1234.5`) has no observable surface: no screen of WF-01..WF-19 and no field of the contract shows a non-integer number (counts, days left, version numbers are integers; phone numbers are stored as typed, BR-06). A test with an invented surface would check an assumption, not the requirement (`general/rules/decisions.md`).
- Options: 1 (default) no frozen acceptance test for EX-019-03; R-019-04 is covered by the date and time tests (AC-019-04, AC-016-03, EX-016-04); `/implement` covers the number format of the i18n module with a unit test; 2 add a screen element that shows a decimal number (input change, needs the human).
- Human response: none
- Resolution: pending review (option 1). This is the only AC/EX without a frozen test.
- Overrides: none

## D-40: US-001 frozen tests need the Group list, which the plan gives to US-002
- Timestamp: 2026-11-08
- Session: S5 /implement US-001
- Class: open
- Type: non-blocking
- Trigger: DoD-S-01 asks every frozen test of US-001 to pass, but `Us001AccountAcceptanceIT` (AC-001-02, AC-001-03, AC-001-04, EX-001-09) calls `GET /groups` and expects 200 for a verified User and 403 `auth.email_not_verified` for `GET` and `POST /groups` otherwise, and `frontend/acceptance/us001-account.spec.ts` (AC-001-02, 04, 09, 10, EX-001-09) waits for the heading "Moje skupine" of the Group list. `docs/02_tasks.md` puts `GroupController` in T-05 and `GroupListPage` in T-06 (US-002), and the verified-e-mail check in T-07 (US-018, R-018-06); the story's own rule R-001-04 ("open no Group") is the same check.
- Options: 1 (default) US-001 builds the smallest slice its tests need and leaves the rest to its tasks: the verified-e-mail check (403 `auth.email_not_verified` on every path under `/groups` and `/group-trash`, in the filter order of US-018 after the token check), `GET /groups` reading the Groups of the User with Role (no Invitation acceptance, no create), and a Group list page with its heading, list, loading, empty and error states (no create dialog). US-002 (T-05, T-06), US-014 (R-014-05) and US-018 (T-07) extend these files. 2 leave the nine frozen tests red until US-002 and mark US-001 done with DoD-S-01 open.
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none

## D-41: Readings of US-001 made while implementing
- Timestamp: 2026-11-08
- Session: S5 /implement US-001
- Class: open
- Type: non-blocking
- Trigger: details the story and the specification leave open: (a) R-001-01 says "a display name of 1 to 100 characters" without saying whether spaces count; (b) the Group list slice of D-40 needs an order, and US-002 gives none; (c) R-001-02 takes the Google profile name, but the data model allows at most 100 characters and a Google name can be longer; (d) WF-01 draws the Google button with a "G" mark, which would be a brand logo or a text outside the message catalog.
- Options: 1 (default) (a) the name is trimmed and must then have 1 to 100 characters, so a name of only spaces is refused like an empty one (EX-001-04), and the trimmed name is saved; (b) by name ignoring case, until US-002 fixes an order; (c) left as it is for now: a Google name over 100 characters makes the first request fail with 500 `server.error`; to be decided with the human (cut it to 100 characters, or use the e-mail as for a missing name); (d) a generic sign-in icon instead of the letter. 2 ask the human per item.
- Human response: none
- Resolution: pending review (option 1); (c) needs an answer before the release.
- Overrides: none

## D-42: Two frozen tests of US-002 set up their data through endpoints of later stories
- Timestamp: 2026-11-08
- Session: S5 /implement US-002
- Class: open
- Type: non-blocking
- Trigger: DoD-S-01 asks every frozen test of US-002 to pass. `Us002GroupAcceptanceIT` EX-002-06 moves a Group to the Group Trash through `POST /groups/{groupId}/trash` (US-020, increment 2, `x-required-action: DELETE_GROUP`, needs `GroupAccessInterceptor` of US-018 T-07) and EX-002-07 makes Ana an Editor through an Invitation (`POST /groups/{groupId}/invitations`, US-014 R-014-05). Both fail in their setup with 404 `route.not_found` before they reach the US-002 behaviour. The behaviour itself (Groups in Trash and Groups the User does not own do not count for R-002-03) is built in T-05 and covered by `GroupEndpointsIT` (`r_002_03_a_group_in_trash_does_not_count`, `r_002_03_a_group_of_which_the_user_is_not_owner_does_not_count`), which set up the data directly in the database.
- Options: 1 (default) leave the two frozen tests red until US-014 (EX-002-07) and US-020 (EX-002-06) build the endpoints; mark US-002 done with DoD-S-01 open for these two, and let `/implement US-014` and `/implement US-020` confirm them. 2 build slices of the Invitation and Group Trash endpoints now, as D-40 did for the Group list; rejected as default because Group Trash belongs to increment 2 and both need the access check of US-018, which comes after US-002 in the plan.
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none

## D-43: Readings of US-002 made while implementing
- Timestamp: 2026-11-08
- Session: S5 /implement US-002
- Class: open
- Type: non-blocking
- Trigger: details the story, the specification and WF-05 leave open: (a) WF-05 draws a contact count on each Group tile, but the contract schema `GroupSummary` has no such field; (b) WF-05 draws "Koš skupin" and an Owner card menu, which belong to US-020; (c) texts the wireframe does not give: the busy label of the submit button and the English texts of the dialog; (d) R-002-02 counts characters, while the browser check counts UTF-16 code units, so a name of 100 characters outside the Basic Multilingual Plane (for example emoji) is refused in the browser and by the server (`String.length()`), although the column would hold it; (e) the order of the Group list (D-41 b) is not given by US-002 either; (f) a missing or `null` name is treated as an empty name (422 `validation.name_invalid`), not as a malformed request.
- Options: 1 (default) (a) no count on the tile until a story and the contract add one; (b) left to US-020; (c) [ASSUMPTION] "Ustvarjanje …" / "Creating …" while the request runs, English "New group", "Group name", "Up to 100 characters.", "Create", "Group created." (the Slovene texts are those of WF-05); (d) kept: the stricter reading refuses only names the customer is unlikely to use; (e) kept: by name ignoring case; (f) as written. 2 ask the human per item.
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none

## D-44: Frozen tests of US-018 set up their data through endpoints of later stories
- Timestamp: 2026-11-09
- Session: S5 /implement US-018
- Class: open
- Type: non-blocking
- Trigger: DoD-S-01 asks every frozen test of US-018 to pass. (a) `Us018SecurityAcceptanceIT` creates a Contact in its `@BeforeEach` (`POST /groups/{groupId}/contacts`, US-003), so all its 22 tests fail in the set-up with 404 `route.not_found` before they reach US-018 behaviour; AC-018-05 to AC-018-07, AC-018-13 and EX-018-04 also need Contact, export and Trash endpoints (US-003, US-011, US-012). (b) `Us018RateLimitOwnContextAcceptanceIT.ac_018_15_security_log` creates a Contact and an Invitation (US-003, US-014 `POST /groups/{groupId}/invitations`). (c) AC-018-10 compares the published description with every operation of the contract, which holds only when the last story is done (the specification says the contract-parity test covers the operations that exist, D-23c). Behaviour of US-018 that these tests check is covered meanwhile by own tests: `GroupAccessIT`, `IsolationIT`, `RequestLimitsIT`, `SecurityChainIT`, `ContractParityIT`, `RequestSizeFilterTest`, `RateLimitFilterTest`, `HttpsFilterTest`, `SecurityLoggerTest`, `ApiExceptionHandlerTest`. The own-context tests AC-018-12, AC-018-14 (HTTPS), EX-018-03 and EX-018-09 pass.
- Options: 1 (default) leave these frozen tests red; mark US-018 done with DoD-S-01 open for them; `/implement US-003` confirms `Us018SecurityAcceptanceIT` (except AC-018-10, AC-018-04 in full, which wait for the last story of the increments), `/implement US-014` confirms AC-018-15, and R-018-05 / AC-018-06..07 get their own Contact tests there. 2 build slices of the Contact, Invitation and export endpoints now; rejected: they belong to other stories and would be written without their own rules.
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none

## D-45: Readings of US-018 made while implementing
- Timestamp: 2026-11-09
- Session: S5 /implement US-018
- Class: open
- Type: non-blocking
- Trigger: points the story and ADR-05, ADR-10, ADR-11 leave open. (a) R-018-04 says "ignore or reject" a Group in the body; chosen: reject every unknown property with 400 `request.unknown_property` (ADR-05, ADR-10) through `fail-on-unknown-properties` in `application.yml`; this applies to every request body of the interface. (b) A body without `Content-Length` is counted while it is read and fails with 413 `request.too_large` (ADR-11). (c) [ASSUMPTION] `Retry-After` of the 429 answer is the whole seconds until the bucket refills, at least 1 (ADR-11 names the header, not the value). (d) [ASSUMPTION] The rate-limit buckets are kept in memory for every User who sent a request and are not removed; one bucket is small and the product runs one instance (ADR-11), so no eviction was built. (e) [ASSUMPTION] The security log line is `security: answered <status> to <method> <path>` for 401, 403, 404 and 429, written by `SecurityLogger` around the whole chain; the path is logged without query string; the 401 line of `FirebaseTokenFilter` was removed so that each event has one line. (f) HTTPS is judged by the scheme after the forwarded-header filter (`server.forward-headers-strategy: framework`), which reads `X-Forwarded-Proto`; `/health` is excluded (ADR-11). (g) Test-only endpoints (`GroupPathProbe`, profile `group-path-probe`, hidden from the OpenAPI description) stand in for Group endpoints until US-003 and later stories exist. (h) R-018-14 is protected in the frontend by a lint rule against `dangerouslySetInnerHTML` (no new package: `no-restricted-syntax`); the check that a Contact named `<script>` is listed and opened as text (AC-018-16) waits for the Contact screens of US-003 and US-007. (i) One commit subject is 73 characters (`feat: publish the OpenAPI description of the contract (R-018-08, DoD-P02)`); history is not rewritten (commits.md), DoD-S-07 in part.
- Options: 1 (default) as written. 2 ask the human per item.
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none

## D-46: Readings of US-003 made while implementing, and frozen tests that wait for later stories
- Timestamp: 2026-11-09
- Session: S5 /implement US-003
- Class: open
- Type: non-blocking
- Trigger: points the story, ADR-06, ADR-07 and the contract leave open, and DoD-S items not met. (a) `ContactInput` carries `tags` and `attributes` (contract), but US-004 and US-005 store them; until then they are accepted and not stored, and a Contact is answered with empty `tags` and `attributes` (AC-003-08 and the Tag and Attribute parts of the frozen tests wait for them). (b) [ASSUMPTION] An edit without `version` is answered with 400 `request.malformed` (the contract lists 400 for the edit and `version` is required there), not with a field error. (c) [ASSUMPTION] The e-mail format check is linear (one `@`, a local part, a domain of at least two non-empty labels, no white space), because the first regular expression was flagged as ReDoS by SpotBugs; ADR text says "simple RFC-5322 subset". (d) [ASSUMPTION] When none of first name, last name, e-mail and company is given, `validation.required_any` is reported on each of the four fields, so the form shows the message next to each (WF-07 field-error state). (e) Every save, including the first, stores a `contact_version` row (snapshot written in SQL with `jsonb_build_object`; Tags and Attributes are `[]` until US-004/US-005 replace this); History entries are written by US-016 (T-18). (f) The Contact screens have no Group shell (group bar, tabs) and no link target `/groups/:g/contacts` yet: the list is US-007. The Role of the caller comes from the Group list query (`useGroupRole`). (g) The country list in the form is the 249 ISO codes of `Locale.getISOCountries()`, names by `Intl.DisplayNames`. (h) DoD-S-01: 19 of 29 tests of `Us003ContactAcceptanceIT` fail in their set-up or last line on endpoints of later stories: `GET /groups/{g}/contacts` for `total()` (US-007, answers 405 now), adding a Member (US-014), Tags and Attributes (US-004, US-005); the 10 that do not need them pass. The UI tests of `us003-005-contact.spec.ts` were not run: they need the contact list (US-007), Members (US-014) and Tags/Attributes. Before the list exists the behaviour is covered by `ContactEndpointsIT` (12 tests), `ContactValidatorTest`, `NormaliserTest`, `CountryCodesTest` and the 13 screen tests. `Us018SecurityAcceptanceIT` (D-44): 7 of 21 pass now; the rest wait for the list (405 instead of 404 before the Group check), Members, Trash and Group deletion; `ac_018_09` fails for another reason: `/health` answers `{"groups", "status"}` instead of `status` only (US-018 code, not changed here; for `/fix` or review). (i) DoD-S-05: screenshots at three widths and the automated accessibility scan were not made, because the screens cannot be reached from the Group list until US-007 (no list, no group shell). (j) DoD-S-07: the commit `feat: create, read and edit a Contact with versions` has 13 files and 757 lines and the commit `feat: new Contact screen ...` 8 files and 727 lines (over 15 files / 400 lines is not met by the second: lines): endpoint, service and repositories (and the form with its page) cannot be tested apart.
- Options: 1 (default) as written. 2 ask the human per item.
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none

## D-47: Readings of US-016 made while implementing, and frozen tests that wait for later stories
- Timestamp: 2026-11-09
- Session: S5 /implement US-016
- Class: open
- Type: non-blocking
- Trigger: points the story, ADR-08 and the contract leave open, and DoD-S items not met. (a) `details` of `CONTACT_CREATED` is `{values: {field: text}}` (fields with a value only) and of `CONTACT_EDITED` is `{changes: {field: {old, new}}}` with `null` for an empty value (ADR-08 names the shape only in general). (b) [ASSUMPTION] A User without a display name is recorded under the e-mail address; names are cut at 100 characters (column limit). (c) `PUT`, `PATCH` and `DELETE` on `/history/{id}` have no controller method, so the contract stays free of them (`ContractParityIT`); `ApiExceptionHandler` turns the resulting "no route" of such a request into 405 `request.method_not_allowed` (R-016-05). The collection path answers 405 natively. (d) The History of a Contact in Trash is readable (R-016-08); after permanent deletion the Contact is gone and the answer is 404 (R-013-05). (e) Tabs "Podrobnosti" and "Zgodovina" are on the Contact screen for Owner and Editor; a Viewer sees no tabs and the History is never requested. The tab "Različice" comes with US-022. (f) DoD-S-01: the 15 frozen `Us016HistoryAcceptanceIT` tests fail in their set-up (`member(...)` needs Invitations, US-014); AC-016-07 also needs Trash (US-012); `us016-019-history-language.spec.ts` was not run, the Contact screen is not reachable from a list before US-007. Re-check them after US-014, US-012 and US-007. The same 200 frozen tests failed before and after this session; no previously passing test fails. (g) DoD-S-05: no screenshots or accessibility scan of the History tab, same reason as (f). (h) DoD-S-07: the backend `feat` commit has about 590 changed lines (limit 400); the entry model, writer, reader and the 405 rule are one behaviour covered by one test class and were not split.
- Options: 1 (default) as written. 2 ask the human per item.
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none

## D-48: Readings of US-004 made while implementing, and frozen tests that wait for later stories
- Timestamp: 2026-11-09
- Session: S5 /implement US-004
- Class: open
- Type: non-blocking
- Trigger: points the story, the specification and the contract leave open, and DoD-S items not met. (a) `attributes` in the Contact body: absent (`null`) keeps the stored pairs on an edit and means none on create; `[]` removes all (the form always sends the list). (b) Keys and values are trimmed before the checks; field errors are named `attributes[i].key`, `attributes[i].value` and `attributes` (limit), all returned together with the Contact field errors (422 `validation.failed`). (c) `details` of the History entries: `ATTRIBUTE_ADDED` `{key, new}`, `ATTRIBUTE_CHANGED` `{key, old, new}`, `ATTRIBUTE_REMOVED` `{key, old}`; one entry per pair, written in the transaction of the save, after `CONTACT_EDITED`. A save that changes only Attributes raises the version and writes a Version (R-004-07). (d) Pairs are listed ordered by key ignoring case. The Version snapshot now carries the Attributes (it carried `[]`). (e) Key suggestions read `GET /groups/{g}/facets` (`attributeKeys`), which US-009 builds; until then the request fails and the form shows no suggestions (the query runs only when a pair exists). (f) `ContactInput` keeps accepting `tags` without storing them (D-46, US-005). (g) DoD-S-01: 16 of 20 frozen `Us004AttributeAcceptanceIT` tests pass; AC-004-07 and AC-004-09 wait for Members (US-014), AC-004-04 and AC-004-08 for the facets endpoint (US-009). The frozen UI tests were not run (no contact list before US-007). Other frozen classes show the same counts as before (US-003 19 of 29, US-016 15 of 15, US-018 14 of 21 fail). (h) DoD-S-05: no screenshots or accessibility scan of the Attribute editor, the Contact screens cannot be reached before US-007. (i) DoD-S-07: the backend `feat` commit has 385 changed lines in 8 files (within limits); gitleaks reports 2 findings in `02_output/logs/backend-verify.log` and `backend-check.log` of commit d6ac788 (log files, not from this session, not changed here; for review).
- Options: 1 (default) as written. 2 ask the human per item.
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none

## D-49: Readings of US-005 made while implementing, and frozen tests that wait for later stories
- Timestamp: 2026-11-09
- Session: S5 /implement US-005
- Class: open
- Type: non-blocking
- Trigger: points the story, the specification and the contract leave open, and DoD-S items not met. (a) `tags` in the Contact body: absent (`null`) keeps the stored Tags on an edit and means none on create; `[]` removes all (the form always sends the list). (b) Names are trimmed and a name typed twice (ignoring case) counts once before the checks; field errors are named `tags[i]` (`validation.tag_invalid`) and `tags` (`validation.tag_limit`, more than 50 distinct names), all returned together with the other field errors (422 `validation.failed`). A letter is any Unicode letter (`\p{L}`), as the Definitions `[ASSUMPTION]` allows. (c) A typed name equal to an existing Tag of the Group ignoring case uses that Tag and its spelling (`INSERT … ON CONFLICT (group_id, lower(name)) DO NOTHING`, then link); a Tag is created only when the Group has none. Removing a Tag deletes only the `contact_tag` row (R-005-06). (d) `details` of `TAG_ADDED` and `TAG_REMOVED` is `{name}` with the Group's spelling; one entry per Tag, written in the transaction of the save, after the Attribute entries. A save that changes only Tags raises the version and writes a Version (R-005-08). The Version snapshot now carries the Tags (it carried `[]`). (e) Tags are listed ordered by name ignoring case. (f) The Tag suggestions read `GET /groups/{g}/facets` (`tags`), which US-009 builds; the request runs only while text is typed, until then it fails and the form shows no suggestions. The form also checks the name with the same pattern before it adds the Tag, to show the rule at once (R-005-02); the server judges again. (g) DoD-S-01: 8 of 19 frozen `Us005TagAcceptanceIT` tests pass; the other 11 need `GET /groups/{g}/tags` (US-006), the Contact list (US-007), Members (US-014) or the facets (US-009) and fail with `route.not_found`; they exercise the code written here (checked with own `TagEndpointsIT`, 9 tests, all green). Other frozen classes show the same counts as before (US-003 19 of 29, US-016 15 of 15, US-018 14 of 21 fail). The frozen UI tests were not run (no contact list before US-007). (h) DoD-S-05: no screenshots or accessibility scan of the Tag input, the Contact screens cannot be reached before US-007. (i) R-005-09 (Tags in the contact list) waits for US-007; the Contact screen already shows every Tag. (j) The unit test of `NewContactPage` that counted 11 fields now counts 12: the 11 fields and the Tag input.
- Options: 1 (default) as written. 2 ask the human per item.
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none

## D-50: Readings of US-006 made while implementing, and frozen tests that wait or race
- Timestamp: 2026-11-09
- Session: S5 /implement US-006
- Class: open
- Type: non-blocking
- Trigger: points the story leaves open, a frozen test that seems wrong, and DoD-S items not met. (a) Routes `GET /groups/{g}/tags`, `PATCH` and `DELETE /groups/{g}/tags/{t}` as in the contract; `TagCount` = `{id, name, contactCount}`, counted over all Contacts including Trash, ordered by name ignoring case. A tag id that is not a UUID or not of this Group is 404 `tag.not_found` (BR-01). (b) Rename: the name is trimmed and checked with the Tag name rule of US-005 (422, field `name`, `validation.tag_invalid`); another Tag with the name ignoring case is 409 `tag.name_taken`; the same spelling returns 200 and writes nothing (EX-006-05); a case-only change is stored (R-006-04). (c) History: one `TAG_RENAMED` entry per carrying Contact with `details` `{old, new, name}`, one `TAG_DELETED` entry with `{name}`; no version change, no Version (R-006-07, BR-07). A Tag in Trash only gets entries too (its Contact carries it). (d) Delete: links and the Tag are removed in one transaction; Contacts are untouched (R-006-05); a restored Contact has no Tag (EX-006-04). (e) UI: `TagListPage` at `/groups/:g/tags`; a Viewer sees a notice and no request is sent (R-006-08); the count in the confirmation uses the four Slovene plural forms (`tags.contactCount.*`). (f) DoD-S-01: 9 of 13 frozen `Us006TagListAcceptanceIT` tests pass; AC-006-04, AC-006-07, EX-006-03, EX-006-04 need the Contact list (US-007), Members (US-014), Activity (US-017) and Trash (US-012) and fail with `route.not_found` or 405; the same rules are checked by own `TagListEndpointsIT` (3 tests, green). (g) Frozen UI test AC-006-04 fails: while the confirmation is open Radix marks the page `aria-hidden`, so `getByRole('row')…toHaveCount(0)` is true at once and the test reads the Tag list before the `DELETE` (204, checked in the browser) has finished. The feature works (list empty afterwards); a frozen test that waits for the closing of the dialog would pass. AC-006-05 passes. Approval needed to change the test. (h) DoD-S-05 done for WF-09 (list, rename, refusals, confirmation, empty at 375, 768 and 1440 px, no Critical or Serious axe finding, no overflow) in Chromium and WebKit; Firefox does not start in this sandbox.
- Options: 1 (default) as written, test unchanged. 2 change the frozen test to wait for the dialog to close.
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none


## D-51: Readings of US-007 made while implementing, and a frozen test that seems wrong
- Timestamp: 2026-11-09
- Session: S5 /implement US-007
- Class: open
- Type: approval
- Trigger: a frozen test that contradicts its rule, points the story leaves open, and DoD-S items not met. (a) Frozen `Us007ContactListAcceptanceIT.ex_007_05_sort_ignores_accents` and EX-007-05 expect `Cerar`, `Čeh`, `Cvetko`. Comparing "ignoring case and accents" (R-007-03, BR-05) gives `ceh` < `cerar` < `cvetko`, i.e. `Čeh`, `Cerar`, `Cvetko`; no accent-insensitive order puts `Čeh` between `Cerar` and `Cvetko`, and Slovene collation (Č after C) gives `Cerar`, `Cvetko`, `Čeh`. The code follows the rule (normalised text, BR-05); the test fails. Approval needed to change the expected order in the story and the test. (b) `GET /groups/{g}/contacts` takes `page`, `sort`, `dir`; `q` and the filters of US-008/US-009 are ignored until those stories. A page below 1 or not a number, an unknown `sort` or a `dir` other than `asc`/`desc` is 400 `request.malformed`. (c) Empty values sort last in both directions; ties by last name, first name, id. `tags` sorts by the first Tag name; `country` by its code. (d) Migration `V3__contact_city_norm.sql` adds `city_norm` (the sort key of the city column, BR-05; existing rows get `lower(city)`); `DatabaseRolesIT` now expects 3 migrations. (e) Screen: `ContactListPage` at `/groups/:g/contacts` (page, sort and direction in the query string); a column header is a button, `aria-sort` marks the order; tablet hides city and country (WF-06); mobile shows cards. The Group tab bar, search, filters and Selection (US-008..US-010) are not built. (f) The Contact tabs are links to `?tab=history`, because the frozen AC-007-07 looks for a link or tab named "Podrobnosti". (g) DoD-S-01: 9 of 11 `Us007ContactListAcceptanceIT` pass backend-side; fails: EX-007-05 (a), AC-007-06 (Trash, US-012), AC-007-08 (Members, US-014). UI: AC-007-01, -03, -07, EX-007-07 pass; AC-007-04, -05 wait for Members (US-014). (h) DoD-S-05 done for WF-06 (list, sorted, empty at 375, 768 and 1440 px, no Critical or Serious axe finding, no overflow) in Chromium and WebKit. (i) DoD-S-07: the first frontend commit has no test of its own (the table is exercised by the page test of the next commit); split to stay under 400 lines.
- Options: 1 (default) as written, test unchanged. 2 change EX-007-05 and the test to `Čeh`, `Cerar`, `Cvetko`.
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none


## D-52: Readings of US-008 made while implementing, and a frozen test that waits
- Timestamp: 2026-11-09
- Session: S5 /implement US-008
- Class: open
- Type: non-blocking
- Trigger: points the story leaves open and DoD-S items not met. (a) `GET /groups/{g}/contacts?q=` splits the text at whitespace after `Normaliser`, and each word must be contained (`LIKE '%word%' ESCAPE '\'`) in `contact.search_norm`, in a `tag.name_norm` or in an `attribute.value_norm` (ADR-06); `%`, `_` and `\` are escaped and every pattern is a bound parameter, so `'; DROP TABLE` and `<script>` are plain text (R-008-06). `q` of spaces or empty is no search (R-008-05). The count and the page use the same condition; the page number is as sent, so the UI sends page 1 on a new text (R-008-03). (b) Screen: `SearchBox` (label "Iskanje" / "Search", `role="search"`) in `ContactListPage`; the text is kept in `?q=` and sent 300 ms after the last key [ASSUMPTION: 300 ms, no source in the inputs; a UI timing only]. A new text sets `page=1`. With `q` set and no result the page shows "No contacts match" and "Clear filters", which removes `q`; filters join it in US-009. A Group without Contacts and without `q` still shows the empty state of US-007. (c) DoD-S-01: 15 of 16 `Us008SearchAcceptanceIT` pass; AC-008-07 needs the Trash endpoint (US-012) and fails with `route.not_found`; the Trash condition is `deleted_at IS NULL`, shared with US-007. UI: AC-008-05, AC-008-09, EX-008-04 pass; AC-009-09..12 and AC-007-04/05 in the same file wait for US-009 and US-014. (d) DoD-S-05 done for WF-06 (list with search text, "No contacts match" at 375, 768 and 1440 px, no Critical or Serious axe finding, no overflow) in Chromium and WebKit; Firefox does not start in this sandbox. (e) The repository-wide secret scan reports 2 findings outside the changed code (backend/src, frontend/src and ui-checks scan clean); not looked into here.
- Options: 1 (default) as written. 2 another delay for the search box.
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none


## D-53: Readings of US-009 made while implementing, and a frozen test that waits
- Timestamp: 2026-11-09
- Session: S5 /implement US-009
- Class: open
- Type: non-blocking
- Trigger: points the story leaves open and DoD-S items not met. (a) `GET /groups/{g}/contacts` takes `tags` (repeated), `tagMode` (`all` default, `any`), `withoutTags`, `country` (two capital letters), `company`, `attrKey`, `attrValue`, `attrExists`; other values are 400. All conditions and the search words join with AND in one SQL condition shared by count and page (R-009-08); every value is a bound parameter. Tag names, company and Attribute value compare on the normalised text (BR-05); the Attribute key compares with `lower()` [ASSUMPTION: accent-insensitive equality for the value, as BR-05 says]. `attrKey` without `attrValue` or `attrExists=true` is no filter; with both, "exists" wins [ASSUMPTION: the contract leaves it open]. (b) `GET /groups/{g}/facets` offers the Tags, companies, countries and Attribute keys of the Contacts not in Trash of the Group, sorted without case and accents [ASSUMPTION: a Tag no Contact carries is not offered]. (c) Screen: `FilterPanel` (`aside` "Filtri") left of the list on desktop, behind a "Filtri (n)" button below 1024 px; filters live in the address so page 1 follows each change (R-009-09). Tags use a listbox (`MultiSelect`); country, company and Attribute key are native selects. The panel's "Clear filters" is the first such button and clears filters and search text (R-009-10), also a search text still waiting for its 300 ms. (d) DoD-S-01: 16 of 17 `Us009FilterAcceptanceIT` pass; EX-009-06 needs the Trash endpoint (US-012). UI: AC-009-09..12 pass; AC-007-04/05 wait for US-014. (e) DoD-S-05 done for WF-06 filters and filters-no-match at 375, 768 and 1440 px, no Critical or Serious axe finding, in Chromium and WebKit; Firefox does not start in this sandbox. (f) `-Pquality` PMD, Checkstyle and SpotBugs are clean on the changed code.
- Options: 1 (default) as written. 2 offer every Tag of the Group in the filter.
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none


## D-54: Readings of US-010 made while implementing, and frozen tests that wait
- Timestamp: 2026-11-09
- Session: S5 /implement US-010
- Class: open
- Type: non-blocking
- Trigger: points the story leaves open and DoD-S items not met. (a) The Selection is built in the browser only (`selection/useSelection`, `SelectionBar`): either a set of ticked Contact ids, or "all results" with the set of Contacts unticked afterwards, so the count is N minus the unticked ones and a Contact can still be unticked after "select all N results" [ASSUMPTION: the story leaves unticking after "select all" open]. It is cleared when the search text or any filter changes, also back to an earlier text (R-010-08, EX-010-04); page and sort order do not clear it. The export (US-011) will have to send the filter and the unticked ids, or the ids, to the backend; no endpoint was changed here. (b) "Select all N results" is offered while the page is fully selected and N is larger than the number of rows on the page (EX-010-01, EX-010-02). (c) Slovene count forms: `1 izbran`, `2 izbrana`, `3 izbrani`, other `izbranih`; "Počisti izbiro" shows from 1 selected on; the bar is shown to an Owner or Editor only. (d) Below 768 px the cards have a checkbox each and a "Izberi vse na tej strani" box above the list. (e) The Export button of the bar (AC-011-01, AC-011-05) belongs to US-011 (T-36) and is not built; the frozen AC-011-01 and AC-011-05 fail until then. (f) DoD-S-01: all 13 frozen AC-010 and EX-010 tests need `POST /groups/{g}/invitations` for the Editor and Viewer set-up (US-014) and fail with `route.not_found` before the screen is reached; they could not be run. They are covered by 10 page tests and 5 hook tests (207 unit tests pass), and the screens were checked as an Owner. Re-check `us010-011-selection.spec.ts` after US-014 and US-011. (g) DoD-S-05 done for WF-06 `selection` and `selection-page` at 375, 768 and 1440 px, no Critical or Serious axe finding, no overflow, in Chromium; WebKit and Firefox not run in this session.
- Options: 1 (default) as written. 2 a Selection of all results that holds no unticked Contacts (unticking then ends "all results").
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none

## D-55: Readings of US-011 made while implementing, and frozen tests that wait
- Timestamp: 2026-11-09
- Session: S5 /implement US-011
- Class: open
- Type: non-blocking
- Trigger: points the story and the contract leave open, and DoD-S items not met. (a) "Select all N results" with some Contacts unticked afterwards (D-54): the contract `ExportRequest` names only `contactIds` or a `filter`, so the Export button lists the results page by page (`GET .../contacts`, 25 per page) and sends the ids without the unticked ones [ASSUMPTION]; with no unticked Contact it sends the filter. A Selection of about 10,000 with unticked Contacts therefore costs about 400 list requests. (b) The column of an Attribute key that appears in several spellings (`Kompetenca`, `kompetenca`) shows the smallest spelling in natural order, `Kompetenca` before `kompetenca` [ASSUMPTION]; only case merges keys, so `sola` and `Šola` stay two columns; the columns are ordered by the key without case and accents, ties by natural order. (c) A Contact's Tag cell is sorted without case and accents. (d) The History entry `CONTACTS_EXPORTED` has no Contact id and the details `{"count": n}`, n being the exported rows after Trash was left out (R-011-07, AC-011-09). (e) The file is made by Apache POI `XSSFWorkbook` in memory (10,000 rows of about 23 cells); every cell is a string cell (SR-06). (f) The `lang` of the request defaults to `sl` as in the contract; any other value is 400. (g) A body that names both or neither of `contactIds` and `filter` is 400 `request.malformed`. (h) Frozen `Us011ExportAcceptanceIT`: 14 of 21 pass; the 7 that fail call routes of later stories (members US-014 `addMember`, Trash US-012 `moveToTrash`, activity US-017 `activity`) and answer `route.not_found`; the rules they test (Viewer 403 with no entry, History entry with user, time and count, left-out deleted Contact, Selection of 10,001 with no entry) are covered meanwhile by `ExportEndpointsIT` and `ExportServiceTest`. The frozen UI tests of `us010-011-selection.spec.ts` need US-014 (Members). (i) Slip, no effect: a `find` over `02_output` listed three file names under `02_output/hidden-tests/` (`H07ListsExportHiddenIT`); no file there was opened or used.
- Options: 1 (default) as written. 2 Add an optional `excludeIds` to the filter selection in the contract (one request for any Selection; needs a contract change and the approval of the human).
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none

## D-56: Readings of US-012 made while implementing, and frozen tests that wait
- Timestamp: 2026-11-09
- Session: S5 /implement US-012
- Class: open
- Type: non-blocking
- Trigger: points the story and the contract leave open, and DoD-S items not met. (a) The move to Trash is one `UPDATE ... WHERE deleted_at IS NULL`; of two requests at once only one changes the row and writes the History entry, the other answers 204 and records nothing (R-012-09, EX-012-04). A delete of a Contact that does not exist in the Group is 404 `contact.not_found`. (b) Days left = whole days left, a started day counting as one (30 right after the deletion, 1 at 29 days 23 hours, 0 from 30 days on) [ASSUMPTION: the story gives "30 days left" only for the moment of deletion]. The Trash view lists only Contacts younger than 30 days, so a Contact the purge (US-013) has not yet deleted is neither listed nor restorable (R-012-05, EX-012-02) [ASSUMPTION]. (c) A restore of a Contact that is not in Trash or is 30 days or more in Trash is 404 `contact.not_found`. The restore keeps `updated_at` and the version number and answers the Contact. (d) A save of a Contact in Trash is 409 `contact.in_trash`, also when the Contact went to Trash between the read and the update (EX-012-05); the edit form shows the message of the catalog. (e) "Deleted by" is the user name of the latest `CONTACT_DELETED` entry of the Contact, `null` when none is found (D-18f). (f) The History details of `CONTACT_DELETED` and `CONTACT_RESTORED` are `{}`. (g) Screens: `TrashPage` (WF-10) with Restore for Owner and Editor; a Viewer sees a refusal and no request is sent; Delete (WF-07) with the confirmation "Premaknem kontakt v koš?". The Trash page is reached by a "Koš" button next to "Nov kontakt" on the Contact list, since the group navigation of the wireframe is not built yet [ASSUMPTION]. "Izbriši za vedno" belongs to US-013 and is not built. (h) DoD-S-01: all 13 frozen `Us012TrashAcceptanceIT` tests fail in their set-up with `route.not_found` (`addMember` needs `POST /groups/{g}/invitations`, US-014); the rules are covered meanwhile by `TrashEndpointsIT` (8 tests), `TrashPeriodTest` and 8 UI unit tests. UI: EX-012-03 passes; AC-012-01 waits for US-014. A date test in `TrashEndpointsIT` counts hours, not days, because a day is 25 hours across the change of summer time. (i) DoD-S-05 done for WF-10 `delete-confirmation`, `list`, `restored`, `empty` at 375, 768 and 1440 px, no Critical or Serious axe finding, no overflow, in Chromium. (j) The acceptance and web integration tests cannot run in one Maven invocation (one `FirebaseApp` per JVM) and the acceptance ones need port 9099: the test stack's emulator was stopped for the run.
- Options: 1 (default) as written. 2 Show Contacts of 30 days or more with 0 days left until the purge runs.
- Human response: none
- Resolution: pending review (option 1).
- Overrides: none
