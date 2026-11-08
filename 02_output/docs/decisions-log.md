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
