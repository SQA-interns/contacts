# Preflight report

> For: agent (writes), human (reads) · Written in: phase 0 (`/preflight`) · Source: `project/00_setup/*` · Procedure: `skills/preflight` · Agent: writes

Session S1, 2026-11-08. Result: **gate not passed**. Nothing was installed, upgraded or substituted. No secret value was read. Logs: `out/logs/preflight-*`.

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Passed with a note: agent, permission mode, design round and the session table are filled; "Run label" is empty (the runner uses it as the run folder's name; set it before `system/run.mjs`); "Hidden vault" is empty (allowed: default location); PA-01 is an empty row (no pre-approved findings). |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | **Failed.** Java: not installed (`java -version`; second method: `/usr/libexec/java_home -V`, `/Library/Java`, SDKMAN, Homebrew all empty). Node: 26.11.1, pin 22.20.0 (no version manager found; D-04). gitleaks: not on `PATH` (D-04). Docker 29.8.2 vs 28.5.1, Compose 5.5.1 vs 2.40.2, npm 11.20.0 vs 10.9.3: run, newer, non-blocking (D-01). `./mvnw -v` and the `npx` checks cannot run: `02_output/` has no `pom.xml`, `package.json` or wrapper yet; the exact versions resolve (D-04 g). PostgreSQL 17.6: image tag resolves; no host install needed. Playwright browser start and blank-page screenshot (`out/logs/preflight-screenshot.png`): **not done**, no browsers are installed (`~/Library/Caches/ms-playwright` missing); the screenshot needs a download the human must start (D-04 d). |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Partly. Docker daemon answers (`docker info`, `docker ps`: 29.8.2, no containers). Maven Central, npm registry and Docker Hub reachable (HTTP 200). PostgreSQL and the Compose stack do not exist yet (`docker-compose.yml` is created in /implement). Firebase Authentication emulator: `firebase-tools` not installed and the emulator needs a JDK (D-04 a). Nothing listens on 5432 or 9099. |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | **Failed.** `.env` does not exist. Needed with a non-empty value: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `FIREBASE_PROJECT_ID`, `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN`. Test-only: `FIREBASE_AUTH_EMULATOR_HOST`. Not needed: `GOOGLE_APPLICATION_CREDENTIALS`. Method: file existence only (`ls`), no read. |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Passed. 93 distinct coordinates (all `dependencies`, all `tooling` with the coordinates behind the options) return HTTP 200 from Maven Central, the npm registry or Docker Hub; a non-existing version returns 404, so the check discriminates. npm: a lockfile-only install of all 50 packages succeeded (`--legacy-peer-deps`, peer compatibility not checked). Spring Boot 3.5.7 BOM equals the pins for Flyway 11.7.2, PostgreSQL driver 42.7.8, JUnit 5.12.2, Spring Security 6.5.6, Testcontainers 1.21.3. |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | **Failed.** npm audit: Critical 4, High 17 on the pins as listed (counts include transitive packages), see D-03. Maven (OSV lookup of direct coordinates, 36 packages): 2 direct pins with High, replaced within the same minor by D-02. The pinned Maven scanner `dependency-check-maven` did not run (no JDK), so transitive Maven dependencies are **not scanned**. Container images are not scanned in preflight. |
| Clean working tree on the starting commit | repository | **Failed.** `git status`: untracked `.agents/` and `.claude/`; there is no `.gitignore` (`.env` would not be ignored). D-04 f. |
| Input manifest written | `docs/00_input-manifest.sha256` | Not written (written only when every check passes; step 10). |

## Dependency results

Resolves: yes = HTTP 200 on the registry. Vulnerability column: highest severity for the pin itself, from `npm audit` (pins as listed) or an OSV lookup (Maven, direct coordinates only). Tooling entries show their `tooling:` id. Containers: not scanned.

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| org.springframework.boot:spring-boot-starter-web | 3.5.7 | yes | none known (OSV) |
| org.springframework.boot:spring-boot-starter-security | 3.5.7 | yes | none known (OSV) |
| org.springframework.boot:spring-boot-starter-validation | 3.5.7 | yes | none known (OSV) |
| org.springframework.boot:spring-boot-starter-jdbc | 3.5.7 | yes | none known (OSV) |
| org.springframework.boot:spring-boot-starter-actuator | 3.5.7 | yes | High (OSV: GHSA-8hfc-fq58-r658, GHSA-mgvc-8q2h-5pgc) see D-02 |
| org.postgresql:postgresql | 42.7.8 | yes | High (OSV: GHSA-98qh-xjc8-98pq, GHSA-j92g-9f8w-j867) see D-02 |
| org.flywaydb:flyway-core | 11.7.2 | yes | none known (OSV) |
| org.flywaydb:flyway-database-postgresql | 11.7.2 | yes | none known (OSV) |
| com.google.firebase:firebase-admin | 9.5.0 | yes | none known (OSV) |
| org.apache.poi:poi-ooxml | 5.4.1 | yes | none known (OSV) |
| org.springdoc:springdoc-openapi-starter-webmvc-ui | 2.8.13 | yes | none known (OSV) |
| com.bucket4j:bucket4j-core | 8.10.1 | yes | none known (OSV) |
| org.springframework.boot:spring-boot-starter-test | 3.5.7 | yes | none known (OSV) |
| org.springframework.security:spring-security-test | 6.5.6 | yes | none known (OSV) |
| org.junit.jupiter:junit-jupiter | 5.12.2 | yes | none known (OSV) |
| org.testcontainers:postgresql | 1.21.3 | yes | none known (OSV) |
| org.testcontainers:junit-jupiter | 1.21.3 | yes | none known (OSV) |
| org.springframework.boot:spring-boot-testcontainers | 3.5.7 | yes | none known (OSV) |
| com.tngtech.archunit:archunit-junit5 | 1.4.1 | yes | none known (OSV) |
| react | 19.2.0 | yes | none known |
| react-dom | 19.2.0 | yes | none known |
| react-router | 7.9.4 | yes | High see D-03 |
| @tanstack/react-query | 5.90.5 | yes | none known |
| firebase | 12.4.0 | yes | High see D-03 |
| i18next | 25.6.0 | yes | none known |
| react-i18next | 16.0.0 | yes | none known |
| react-hook-form | 7.65.0 | yes | none known |
| zod | 4.1.12 | yes | High see D-03 |
| @hookform/resolvers | 5.2.2 | yes | none known |
| tailwindcss | 4.1.15 | yes | none known |
| @tailwindcss/vite | 4.1.15 | yes | none known |
| lucide-react | 0.546.0 | yes | none known |
| @radix-ui/react-dialog | 1.1.15 | yes | none known |
| @radix-ui/react-dropdown-menu | 2.1.16 | yes | none known |
| @radix-ui/react-checkbox | 1.3.3 | yes | none known |
| @radix-ui/react-select | 2.2.6 | yes | none known |
| @radix-ui/react-tabs | 1.1.13 | yes | none known |
| @radix-ui/react-slot | 1.2.3 | yes | none known |
| class-variance-authority | 0.7.1 | yes | none known |
| clsx | 2.1.1 | yes | none known |
| tailwind-merge | 3.3.1 | yes | none known |
| sonner | 2.0.7 | yes | none known |
| typescript | 5.9.3 | yes | none known |
| vite | 7.1.10 | yes | High see D-03 |
| @vitejs/plugin-react | 5.0.4 | yes | none known |
| vitest | 3.2.4 | yes | Critical see D-03 |
| @vitest/coverage-v8 | 3.2.4 | yes | Critical see D-03 |
| @testing-library/react | 16.3.0 | yes | none known |
| @testing-library/user-event | 14.6.1 | yes | none known |
| @testing-library/jest-dom | 6.9.1 | yes | none known |
| jsdom | 27.0.0 | yes | none known |
| msw | 2.11.5 | yes | none known |
| @playwright/test | 1.56.1 | yes | none known |
| @axe-core/playwright | 4.10.2 | yes | none known |
| exceljs | 4.4.0 | yes | Medium |
| @types/react | 19.2.2 | yes | none known |
| @types/react-dom | 19.2.2 | yes | none known |
| postgres | 17.6 | yes | not scanned (image scan not part of preflight tools) |
| eclipse-temurin | 21.0.8_9-jre | yes | not scanned (image scan not part of preflight tools) |
| eclipse-temurin | 21.0.8_9-jdk | yes | not scanned (image scan not part of preflight tools) |
| node | 22.20.0-bookworm-slim | yes | not scanned (image scan not part of preflight tools) |
| nginx | 1.29.2-alpine | yes | not scanned (image scan not part of preflight tools) |
| org.apache.maven:apache-maven (tooling:maven) | 3.9.11 | yes | none known (OSV) |
| org.apache.maven.plugins:maven-compiler-plugin (tooling:maven-compiler-plugin) | 3.14.0 | yes | none known (OSV) |
| com.diffplug.spotless:spotless-maven-plugin (tooling:spotless-maven-plugin) | 2.46.1 | yes | none known (OSV) |
| org.apache.maven.plugins:maven-checkstyle-plugin (tooling:maven-checkstyle-plugin) | 3.6.0 | yes | none known (OSV) |
| org.apache.maven.plugins:maven-surefire-plugin (tooling:maven-surefire-plugin) | 3.5.4 | yes | none known (OSV) |
| org.apache.maven.plugins:maven-failsafe-plugin (tooling:maven-failsafe-plugin) | 3.5.4 | yes | none known (OSV) |
| org.jacoco:jacoco-maven-plugin (tooling:jacoco-maven-plugin) | 0.8.13 | yes | none known (OSV) |
| org.pitest:pitest-maven (tooling:pitest-maven) | 1.20.3 | yes | none known (OSV) |
| com.github.spotbugs:spotbugs-maven-plugin (tooling:spotbugs-maven-plugin) | 4.9.6.0 | yes | none known (OSV) |
| org.apache.maven.plugins:maven-pmd-plugin (tooling:maven-pmd-plugin) | 3.28.0 | yes | none known (OSV) |
| org.owasp:dependency-check-maven (tooling:dependency-check-maven) | 12.1.6 | yes | none known (OSV) |
| prettier (tooling:prettier) | 3.6.2 | yes | none known |
| eslint (tooling:eslint) | 9.37.0 | yes | none known |
| @stryker-mutator/core (tooling:stryker) | 9.2.0 | yes | High see D-03 |
| eslint-plugin-security (tooling:eslint-plugin-security) | 3.0.1 | yes | none known |
| npm (tooling:npm-audit) | 10.9.3 | yes | not scanned (host tool) |
| license-checker-rseidelsohn (tooling:license-checker-rseidelsohn) | 4.4.2 | yes | none known |
| shadcn (tooling:shadcn-cli) | 3.4.2 | yes | High see D-03 |
| firebase-tools (tooling:firebase-tools) | 14.22.0 | yes | High see D-03 |
| zricethezav/gitleaks (tooling:gitleaks) | 8.28.0 | yes | not scanned (image scan not part of preflight tools) |
| jscpd (tooling:jscpd) | 4.0.5 | yes | none known |
| com.google.googlejavaformat:google-java-format | 1.28.0 | yes | none known (OSV) |
| com.puppycrawl.tools:checkstyle | 11.0.1 | yes | none known (OSV) |
| org.pitest:pitest-junit5-plugin | 1.2.3 | yes | none known (OSV) |
| com.github.spotbugs:spotbugs | 4.9.6 | yes | none known (OSV) |
| com.h3xstream.findsecbugs:findsecbugs-plugin | 1.14.0 | yes | none known (OSV) |
| net.sourceforge.pmd:pmd-java | 7.17.0 | yes | none known (OSV) |
| @stryker-mutator/vitest-runner | 9.2.0 | yes | High see D-03 |
| typescript-eslint | 8.46.1 | yes | Medium |
| eslint-plugin-jsx-a11y | 6.10.2 | yes | none known |
| eslint-plugin-react-hooks | 5.2.0 | yes | none known |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-11-08 | D-03 (npm Critical/High: choose upgrade or lowering with evidence), D-04 (JDK 21.0.8, Node 22.20.0, gitleaks 8.28.0, Playwright browsers, `.env`, commit `.agents/` and `.claude/`, `.gitignore`, how to check Maven/npx tools) | | The next `/preflight` re-runs only the failed checks: platforms and tools, Playwright screenshot, secrets, the vulnerability check (after the human's D-03 answer), working tree. |
| 2026-11-08 | Recorded without a question: D-01 (newer Docker, Compose, npm), D-02 (Maven same-minor patches, pending review) | none needed | none |
