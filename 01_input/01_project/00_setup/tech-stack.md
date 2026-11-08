# Tech stack

> For: agent (reads), human (reviews) · Owner: Architect · Agent: read-only

The single source of truth for every platform, dependency and tool version.
Build manifests and lock files in `02_output/` (e.g. `pom.xml`, `package.json`, `build.gradle`, `*.csproj`, `Podfile`) must match this file exactly; the phase 0 gate checks it. Resolvability and vulnerability results are recorded by the agent in `docs/00_preflight-report.md`.

## Rules

- Exact versions only: no ranges, wildcards, `latest` or floating tags.
- Changing any entry below is a blocking decision (`general/rules/decisions.md`). The human's approval in the decision record replaces the entry; nobody edits this file mid-run.
- A pin that resolves and runs but whose tool reports another version (for example an image tag) is a non-blocking decision; the listed pin stays authoritative. A pin that does not resolve or run is blocking.
- A dependency or tool not listed here may be added only with an exact version and a non-blocking decision record; it is scanned in phase 6 like any other.
- Every dependency's licence must be compatible with the project's licence.
- A `platforms` or `tooling` entry may carry `version_check` (a command whose output contains the version) and, for `tooling`, `options` (flags, analysers or rule sets to enable or disable, for example analysers that need credentials not listed in `secrets.env.example`). Preflight uses them.
- `tooling` must include at least one tool for each purpose required by `general/quality/` and `general/security/`: build, format, lint, type check (if the language has one), test, coverage, mutation, static analysis, dependency scan, secret scan; and for a component with a user interface: the UI stack of `general/design/ui-baseline.md` (UI-01), a browser test runner with screenshots, and an accessibility checker.
- A version proposed by `/to-spec` and not yet verified is marked `[ASSUMPTION] verify in preflight`; `/preflight` confirms or replaces it through a decision.

Project licence: Proprietary, all rights with the customer [ASSUMPTION: the interview does not name a licence; the customer receives the product]. Allowed dependency licences: Apache-2.0, MIT, BSD-2-Clause, BSD-3-Clause, ISC, 0BSD, BlueOak-1.0.0, CC0-1.0, PostgreSQL. Permitted only in `test` or `build` scope or as a runtime base image, and listed in the review checklist [ASSUMPTION]: EPL-1.0 and EPL-2.0 (JUnit; Logback is used under its EPL option), MPL-2.0 (axe-core, test only), GPL-2.0-with-classpath-exception (the Java runtime in the container image). Q-51 forbids copyleft code in the product.

Versions verified to resolve on: not yet. Every version in this file is the latest stable one known to `/to-spec` and is an [ASSUMPTION] to verify in preflight. Managed versions: the Spring Boot BOM manages Flyway, the PostgreSQL driver, JUnit and Mockito; the pins below must equal what the BOM resolves, or preflight records a decision. The PostgreSQL pin is "16 or newer" in Q-51; 17 is chosen. The npm packages are pinned without `^` or `~`.

## Platforms

```yaml
platforms:
  - { id: java, name: "Eclipse Temurin JDK", version: 21.0.8, source: https://adoptium.net/, version_check: "java -version" }  # [ASSUMPTION] verify in preflight
  - { id: node, name: "Node.js (LTS) with npm", version: 22.20.0, source: https://nodejs.org/, version_check: "node --version" }  # [ASSUMPTION] verify in preflight
  - { id: postgres, name: "PostgreSQL (runs as a container)", version: 17.6, source: https://www.postgresql.org/ }  # [ASSUMPTION] verify in preflight
  - { id: docker, name: "Docker Engine", version: 28.5.1, source: https://docs.docker.com/engine/, version_check: "docker --version" }  # [ASSUMPTION] verify in preflight
  - { id: compose, name: "Docker Compose", version: 2.40.2, source: https://docs.docker.com/compose/, version_check: "docker compose version" }  # [ASSUMPTION] verify in preflight
```

## Dependencies

One entry per artifact, written the way the ecosystem writes coordinates.

```yaml
dependencies:
  # backend, runtime and test (maven: https://repo.maven.apache.org/maven2), then frontend (npm: https://registry.npmjs.org), then container images (docker hub: https://hub.docker.com)
  - { id: org.springframework.boot:spring-boot-starter-web, ecosystem: maven, version: 3.5.7, scope: runtime, component: backend, source: https://repo.maven.apache.org/maven2, purpose: REST interface, license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: org.springframework.boot:spring-boot-starter-security, ecosystem: maven, version: 3.5.7, scope: runtime, component: backend, source: https://repo.maven.apache.org/maven2, purpose: authentication filter chain, 401/403, license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: org.springframework.boot:spring-boot-starter-validation, ecosystem: maven, version: 3.5.7, scope: runtime, component: backend, source: https://repo.maven.apache.org/maven2, purpose: request validation, license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: org.springframework.boot:spring-boot-starter-jdbc, ecosystem: maven, version: 3.5.7, scope: runtime, component: backend, source: https://repo.maven.apache.org/maven2, purpose: parameterised database access (AR-05), license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: org.springframework.boot:spring-boot-starter-actuator, ecosystem: maven, version: 3.5.7, scope: runtime, component: backend, source: https://repo.maven.apache.org/maven2, purpose: health check, license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: org.postgresql:postgresql, ecosystem: maven, version: 42.7.8, scope: runtime, component: backend, source: https://repo.maven.apache.org/maven2, purpose: PostgreSQL JDBC driver, license: BSD-2-Clause }  # [ASSUMPTION] verify in preflight
  - { id: org.flywaydb:flyway-core, ecosystem: maven, version: 11.7.2, scope: runtime, component: backend, source: https://repo.maven.apache.org/maven2, purpose: schema migrations (AR-04), license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: org.flywaydb:flyway-database-postgresql, ecosystem: maven, version: 11.7.2, scope: runtime, component: backend, source: https://repo.maven.apache.org/maven2, purpose: Flyway PostgreSQL support, license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: com.google.firebase:firebase-admin, ecosystem: maven, version: 9.5.0, scope: runtime, component: backend, source: https://repo.maven.apache.org/maven2, purpose: verify Firebase ID tokens (AR-06), license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: org.apache.poi:poi-ooxml, ecosystem: maven, version: 5.4.1, scope: runtime, component: backend, source: https://repo.maven.apache.org/maven2, purpose: Excel export (US-011), license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: org.springdoc:springdoc-openapi-starter-webmvc-ui, ecosystem: maven, version: 2.8.13, scope: runtime, component: backend, source: https://repo.maven.apache.org/maven2, purpose: OpenAPI description (R-018-08), license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: com.bucket4j:bucket4j-core, ecosystem: maven, version: 8.10.1, scope: runtime, component: backend, source: https://repo.maven.apache.org/maven2, purpose: rate limit per User (R-018-10), license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: org.springframework.boot:spring-boot-starter-test, ecosystem: maven, version: 3.5.7, scope: test, component: backend, source: https://repo.maven.apache.org/maven2, purpose: JUnit, Mockito, AssertJ, Spring test support, license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: org.springframework.security:spring-security-test, ecosystem: maven, version: 6.5.6, scope: test, component: backend, source: https://repo.maven.apache.org/maven2, purpose: security tests, license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: org.junit.jupiter:junit-jupiter, ecosystem: maven, version: 5.12.2, scope: test, component: backend, source: https://repo.maven.apache.org/maven2, purpose: JUnit test engine (managed by Spring Boot), license: EPL-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: org.testcontainers:postgresql, ecosystem: maven, version: 1.21.3, scope: test, component: backend, source: https://repo.maven.apache.org/maven2, purpose: throwaway PostgreSQL for tests, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: org.testcontainers:junit-jupiter, ecosystem: maven, version: 1.21.3, scope: test, component: backend, source: https://repo.maven.apache.org/maven2, purpose: Testcontainers JUnit 5 support, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: org.springframework.boot:spring-boot-testcontainers, ecosystem: maven, version: 3.5.7, scope: test, component: backend, source: https://repo.maven.apache.org/maven2, purpose: Testcontainers integration, license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: com.tngtech.archunit:archunit-junit5, ecosystem: maven, version: 1.4.1, scope: test, component: backend, source: https://repo.maven.apache.org/maven2, purpose: architecture tests (AR-01, AR-02), license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: react, ecosystem: npm, version: 19.2.0, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: UI library, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: react-dom, ecosystem: npm, version: 19.2.0, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: UI library, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: react-router, ecosystem: npm, version: 7.9.4, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: routing, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @tanstack/react-query, ecosystem: npm, version: 5.90.5, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: server state and caching, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: firebase, ecosystem: npm, version: 12.4.0, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: Firebase Authentication client (Q-47), license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: i18next, ecosystem: npm, version: 25.6.0, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: message catalogs (UI-30), license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: react-i18next, ecosystem: npm, version: 16.0.0, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: React bindings for i18next, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: react-hook-form, ecosystem: npm, version: 7.65.0, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: forms, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: zod, ecosystem: npm, version: 4.1.12, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: form validation, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @hookform/resolvers, ecosystem: npm, version: 5.2.2, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: form validation bridge, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: tailwindcss, ecosystem: npm, version: 4.1.15, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: styling system (UI-01), license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @tailwindcss/vite, ecosystem: npm, version: 4.1.15, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: Tailwind Vite plugin, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: lucide-react, ecosystem: npm, version: 0.546.0, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: icons (UI-01), license: ISC }  # [ASSUMPTION] verify in preflight
  - { id: @radix-ui/react-dialog, ecosystem: npm, version: 1.1.15, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: accessible primitives for shadcn/ui, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @radix-ui/react-dropdown-menu, ecosystem: npm, version: 2.1.16, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: accessible primitives for shadcn/ui, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @radix-ui/react-checkbox, ecosystem: npm, version: 1.3.3, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: accessible primitives for shadcn/ui, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @radix-ui/react-select, ecosystem: npm, version: 2.2.6, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: accessible primitives for shadcn/ui, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @radix-ui/react-tabs, ecosystem: npm, version: 1.1.13, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: accessible primitives for shadcn/ui, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @radix-ui/react-slot, ecosystem: npm, version: 1.2.3, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: accessible primitives for shadcn/ui, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: class-variance-authority, ecosystem: npm, version: 0.7.1, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: component variants (shadcn/ui), license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: clsx, ecosystem: npm, version: 2.1.1, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: class names, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: tailwind-merge, ecosystem: npm, version: 3.3.1, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: class merging (shadcn/ui), license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: sonner, ecosystem: npm, version: 2.0.7, scope: runtime, component: frontend, source: https://registry.npmjs.org, purpose: toasts (UI-15), license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: typescript, ecosystem: npm, version: 5.9.3, scope: build, component: frontend, source: https://registry.npmjs.org, purpose: TypeScript compiler, license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: vite, ecosystem: npm, version: 7.1.10, scope: build, component: frontend, source: https://registry.npmjs.org, purpose: build and dev server, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @vitejs/plugin-react, ecosystem: npm, version: 5.0.4, scope: build, component: frontend, source: https://registry.npmjs.org, purpose: Vite React plugin, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: vitest, ecosystem: npm, version: 3.2.4, scope: test, component: frontend, source: https://registry.npmjs.org, purpose: unit and component tests, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @vitest/coverage-v8, ecosystem: npm, version: 3.2.4, scope: test, component: frontend, source: https://registry.npmjs.org, purpose: coverage, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @testing-library/react, ecosystem: npm, version: 16.3.0, scope: test, component: frontend, source: https://registry.npmjs.org, purpose: component tests, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @testing-library/user-event, ecosystem: npm, version: 14.6.1, scope: test, component: frontend, source: https://registry.npmjs.org, purpose: user input in tests, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @testing-library/jest-dom, ecosystem: npm, version: 6.9.1, scope: test, component: frontend, source: https://registry.npmjs.org, purpose: DOM assertions, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: jsdom, ecosystem: npm, version: 27.0.0, scope: test, component: frontend, source: https://registry.npmjs.org, purpose: DOM for unit tests, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: msw, ecosystem: npm, version: 2.11.5, scope: test, component: frontend, source: https://registry.npmjs.org, purpose: API mocks in unit tests, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @playwright/test, ecosystem: npm, version: 1.56.1, scope: test, component: frontend, source: https://registry.npmjs.org, purpose: browser test runner with screenshots, license: Apache-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: @axe-core/playwright, ecosystem: npm, version: 4.10.2, scope: test, component: frontend, source: https://registry.npmjs.org, purpose: accessibility checker, license: MPL-2.0 }  # [ASSUMPTION] verify in preflight
  - { id: exceljs, ecosystem: npm, version: 4.4.0, scope: test, component: frontend, source: https://registry.npmjs.org, purpose: read the exported .xlsx in tests, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @types/react, ecosystem: npm, version: 19.2.2, scope: build, component: frontend, source: https://registry.npmjs.org, purpose: types, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: @types/react-dom, ecosystem: npm, version: 19.2.2, scope: build, component: frontend, source: https://registry.npmjs.org, purpose: types, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: postgres, ecosystem: container, version: 17.6, scope: runtime, component: all, source: https://hub.docker.com, purpose: database, license: PostgreSQL }  # [ASSUMPTION] verify in preflight
  - { id: eclipse-temurin, ecosystem: container, version: 21.0.8_9-jre, scope: runtime, component: backend, source: https://hub.docker.com, purpose: runtime base image of the backend, license: GPL-2.0-with-classpath-exception }  # [ASSUMPTION] verify in preflight
  - { id: eclipse-temurin, ecosystem: container, version: 21.0.8_9-jdk, scope: build, component: backend, source: https://hub.docker.com, purpose: build image of the backend, license: GPL-2.0-with-classpath-exception }  # [ASSUMPTION] verify in preflight
  - { id: node, ecosystem: container, version: 22.20.0-bookworm-slim, scope: build, component: frontend, source: https://hub.docker.com, purpose: build image of the frontend, license: MIT }  # [ASSUMPTION] verify in preflight
  - { id: nginx, ecosystem: container, version: 1.29.2-alpine, scope: runtime, component: frontend, source: https://hub.docker.com, purpose: serves the built frontend, license: BSD-2-Clause }  # [ASSUMPTION] verify in preflight
```

## Tooling

```yaml
tooling:
  # backend
  - { id: maven, ecosystem: maven, version: 3.9.11, purpose: build, component: backend, source: https://repo.maven.apache.org/maven2, version_check: "./mvnw -v" }  # [ASSUMPTION] verify in preflight
  - { id: maven-compiler-plugin, ecosystem: maven, version: 3.14.0, purpose: type-check, component: backend, source: https://repo.maven.apache.org/maven2 }  # [ASSUMPTION] verify in preflight
  - { id: spotless-maven-plugin, ecosystem: maven, version: 2.46.1, purpose: format, component: backend, source: https://repo.maven.apache.org/maven2, options: "google-java-format 1.28.0" }  # [ASSUMPTION] verify in preflight
  - { id: maven-checkstyle-plugin, ecosystem: maven, version: 3.6.0, purpose: lint, component: backend, source: https://repo.maven.apache.org/maven2, options: "checkstyle 11.0.1" }  # [ASSUMPTION] verify in preflight
  - { id: maven-surefire-plugin, ecosystem: maven, version: 3.5.4, purpose: test, component: backend, source: https://repo.maven.apache.org/maven2 }  # [ASSUMPTION] verify in preflight
  - { id: maven-failsafe-plugin, ecosystem: maven, version: 3.5.4, purpose: test, component: backend, source: https://repo.maven.apache.org/maven2 }  # [ASSUMPTION] verify in preflight
  - { id: jacoco-maven-plugin, ecosystem: maven, version: 0.8.13, purpose: coverage, component: backend, source: https://repo.maven.apache.org/maven2 }  # [ASSUMPTION] verify in preflight
  - { id: pitest-maven, ecosystem: maven, version: 1.20.3, purpose: mutation, component: backend, source: https://repo.maven.apache.org/maven2, options: "pitest-junit5-plugin 1.2.3" }  # [ASSUMPTION] verify in preflight
  - { id: spotbugs-maven-plugin, ecosystem: maven, version: 4.9.6.0, purpose: static-analysis, component: backend, source: https://repo.maven.apache.org/maven2, options: "spotbugs 4.9.6 with findsecbugs-plugin 1.14.0" }  # [ASSUMPTION] verify in preflight
  - { id: maven-pmd-plugin, ecosystem: maven, version: 3.28.0, purpose: code-metrics, component: backend, source: https://repo.maven.apache.org/maven2, options: "PMD 7.17.0" }  # [ASSUMPTION] verify in preflight
  - { id: dependency-check-maven, ecosystem: maven, version: 12.1.6, purpose: dependency-scan, component: backend, source: https://repo.maven.apache.org/maven2, options: "disable analysers that need an API key; use the offline NVD data mirror or OSS Index without credentials" }  # [ASSUMPTION] verify in preflight
  # frontend
  - { id: vite, ecosystem: npm, version: 7.1.10, purpose: build, component: frontend, source: https://registry.npmjs.org, version_check: "npx vite --version" }  # [ASSUMPTION] verify in preflight
  - { id: typescript-tsc, ecosystem: npm, version: 5.9.3, purpose: type-check, component: frontend, source: https://registry.npmjs.org, version_check: "npx tsc --version" }  # [ASSUMPTION] verify in preflight
  - { id: prettier, ecosystem: npm, version: 3.6.2, purpose: format, component: frontend, source: https://registry.npmjs.org }  # [ASSUMPTION] verify in preflight
  - { id: eslint, ecosystem: npm, version: 9.37.0, purpose: lint, component: frontend, source: https://registry.npmjs.org, options: "typescript-eslint 8.46.1, eslint-plugin-jsx-a11y 6.10.2, eslint-plugin-react-hooks 5.2.0" }  # [ASSUMPTION] verify in preflight
  - { id: vitest-runner, ecosystem: npm, version: 3.2.4, purpose: test, component: frontend, source: https://registry.npmjs.org }  # [ASSUMPTION] verify in preflight
  - { id: vitest-coverage-v8, ecosystem: npm, version: 3.2.4, purpose: coverage, component: frontend, source: https://registry.npmjs.org }  # [ASSUMPTION] verify in preflight
  - { id: stryker, ecosystem: npm, version: 9.2.0, purpose: mutation, component: frontend, source: https://registry.npmjs.org, options: "@stryker-mutator/core 9.2.0 with @stryker-mutator/vitest-runner 9.2.0" }  # [ASSUMPTION] verify in preflight
  - { id: eslint-plugin-security, ecosystem: npm, version: 3.0.1, purpose: static-analysis, component: frontend, source: https://registry.npmjs.org }  # [ASSUMPTION] verify in preflight
  - { id: npm-audit, ecosystem: npm, version: 10.9.3, purpose: dependency-scan, component: frontend, source: https://registry.npmjs.org, version_check: "npm --version" }  # [ASSUMPTION] verify in preflight
  - { id: license-checker-rseidelsohn, ecosystem: npm, version: 4.4.2, purpose: code-metrics, component: frontend, source: https://registry.npmjs.org, options: "licence report for NFR-13" }  # [ASSUMPTION] verify in preflight
  - { id: tailwindcss-ui-stack, ecosystem: npm, version: 4.1.15, purpose: build, component: frontend, source: https://registry.npmjs.org, options: "UI-01 token styling system: Tailwind CSS with CSS-variable tokens" }  # [ASSUMPTION] verify in preflight
  - { id: shadcn-cli, ecosystem: npm, version: 3.4.2, purpose: build, component: frontend, source: https://registry.npmjs.org, options: "UI-01 component library: shadcn/ui components copied into the repository, Radix primitives, Lucide icons" }  # [ASSUMPTION] verify in preflight
  - { id: playwright-test-runner, ecosystem: npm, version: 1.56.1, purpose: test, component: all, source: https://registry.npmjs.org, options: "browser test runner with screenshots at 375, 768 and 1440 px; chromium, firefox, webkit" }  # [ASSUMPTION] verify in preflight
  - { id: axe-core-playwright, ecosystem: npm, version: 4.10.2, purpose: lint, component: frontend, source: https://registry.npmjs.org, options: "accessibility checker, WCAG 2.2 AA rule tags" }  # [ASSUMPTION] verify in preflight
  - { id: firebase-tools, ecosystem: npm, version: 14.22.0, purpose: test, component: all, source: https://registry.npmjs.org, options: "Local Emulator Suite, Authentication emulator only" }  # [ASSUMPTION] verify in preflight
  # all components
  - { id: gitleaks, ecosystem: container, version: 8.28.0, purpose: secret-scan, component: all, source: https://hub.docker.com/r/zricethezav/gitleaks, version_check: "gitleaks version" }  # [ASSUMPTION] verify in preflight
  - { id: jscpd, ecosystem: npm, version: 4.0.5, purpose: duplication, component: all, source: https://registry.npmjs.org }  # [ASSUMPTION] verify in preflight
```
