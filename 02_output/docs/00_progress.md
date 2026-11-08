# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S1 /preflight
- Result: done
- Gate: phase 0 gate passed
- Notes for the next session: Report `docs/00_preflight-report.md`, input manifest `docs/00_input-manifest.sha256` (77 files), decisions D-01 to D-13 in `docs/decisions-log.md`. `tech-stack.md` was not edited: the effective versions are those of D-02, D-05, D-10 and D-12, which replace the entries it lists (Maven: Spring Boot 4.0.9 with the web starter renamed `spring-boot-starter-webmvc`, HttpClient5 5.6.5, HttpCore5 5.4.4, `firebase-admin` 9.5.0 without Google Cloud Storage and Firestore, springdoc `-api` 3.2.0, JUnit 6.0.3, Testcontainers 2.0.5 with renamed artifacts, Flyway 11.14.1 plus `spring-boot-starter-flyway`; npm: `react-router` 7.18.5, `zod` 4.6.7, `vite` 7.3.7, `vitest` and `@vitest/coverage-v8` 4.1.12, Stryker 9.6.1, `shadcn` 4.23.0). Accepted findings: `firebase` and `firebase-tools` in npm (D-05). Host: Node 22.20.0 and gitleaks in `~/.local/bin`, JDK 21.0.8 in `~/Library/Java/JavaVirtualMachines`, Playwright 1.56.1 browsers (Firefox start unverified). For /implement: prove Firebase ID-token checking works without the excluded libraries, check JUnit 6 and Boot 4 compatibility of `archunit-junit5`, `pitest-junit5-plugin`, `bucket4j-core`, `poi-ooxml`, `./mvnw -v` and the `npx` version checks (D-06). Records D-01, D-02 and D-08 are "pending review", nothing waits on them.

## Next
- Command: /clarify
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
