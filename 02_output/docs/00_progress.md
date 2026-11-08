# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S1 /preflight
- Result: blocked
- Gate: phase 0 gate not passed (3 Firebase values empty in `.env`; D-11 Critical and High findings left in Maven dependencies after the Boot 4.0.9 move; Firefox start not verified; input manifest not written)
- Notes for the next session: Details in `docs/00_preflight-report.md`, evidence in `out/logs/preflight-*`. Done at the human's request (D-09): JDK 21.0.8, Node 22.20.0 (npm 10.9.3), gitleaks 8.28.0, Playwright 1.56.1 browsers, `.gitignore`, `.env` with the PostgreSQL values, `.agents/` and `.claude/` committed. npm: D-05 versions audited, only `firebase` and `firebase-tools` findings remain (accepted in D-05). Maven: D-07 was answered by D-10 (Spring Boot 4.0.9, HttpClient5 5.6.5, HttpCore5 5.4.4); this cleared the Spring, Tomcat and Netty findings and also moved JUnit to 6.0.3 and Testcontainers to 2.0.5 with renamed artifacts (listed in D-10 for review). D-11 is blocking: 10 findings in Swagger UI, OpenTelemetry alpha and protobuf; the proposed variant C (exclude Firestore and Storage from `firebase-admin`, use springdoc `-api`) scans clean. The scan needs `-DassemblyAnalyzerEnabled=false -DnugetconfAnalyzerEnabled=false -DnuspecAnalyzerEnabled=false -DossindexAnalyzerEnabled=false` (D-08); the NVD data is cached in the session scratch folder only. `./mvnw -v` and `npx` checks are deferred to /implement (D-06 option 3). The input manifest is written only when every check passes. Do not edit `tech-stack.md`; the build manifests follow D-02, D-05, D-10 and the answer to D-11.

## Next
- Command: /preflight D-11: 1
- Waiting for: D-11 (human decision); the human adds `FIREBASE_PROJECT_ID`, `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN` to `.env`

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
