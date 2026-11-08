# ADR-16: Test and check layout

- Status: accepted (S3 /plan)
- Drivers: ES-05, ES-14, DoD-04, `general/testing/*`, quality-requirements thresholds

## Decision

Backend: unit tests `*Test` run by Surefire; integration tests `*IT` (Testcontainers, emulator) by Failsafe in `verify`; architecture tests are `*Test` under `si.contactgroups.architecture`. `./mvnw -B verify` runs both; `-Pquality` adds Spotless check, Checkstyle, SpotBugs with find-sec-bugs, PMD, `maven-enforcer-plugin` rules and the JaCoCo report. PIT and the JaCoCo thresholds are configured in phase 5 (`/quality-gate`) in their own `build` commit; the skeleton only declares the plugin versions. Frontend: `npm test` = Vitest (jsdom, msw, Testing Library); `npm run check` = Prettier check, ESLint, `tsc --noEmit`, catalog parity test; `npm run build` = `tsc -b && vite build`; Playwright (`npm run e2e`) runs against the Compose stack with chromium, firefox and webkit at 375, 768 and 1440 px, and axe for WCAG 2.2 AA. Acceptance and E2E tests are written by `/acceptance-tests`, not in this phase. Contract validation runs in a small `tools/` package (`npm run validate-contracts`).

## Consequences

- The skeleton is green without business tests; the quality gate adds coverage and mutation thresholds later.
