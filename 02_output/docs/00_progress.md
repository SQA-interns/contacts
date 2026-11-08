# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S1 /preflight
- Result: blocked
- Gate: phase 0 gate not passed (3 Firebase values empty in `.env`; input manifest not written; Firefox start unverified, optional)
- Notes for the next session: Details in `docs/00_preflight-report.md`, evidence in `out/logs/preflight-*`. Done at the human's request (D-09): JDK 21.0.8, Node 22.20.0 (npm 10.9.3), gitleaks 8.28.0, Playwright 1.56.1 browsers, `.gitignore`, `.env` with the PostgreSQL values, `.agents/` and `.claude/` committed. npm: D-05 versions audited, only `firebase` and `firebase-tools` findings remain (accepted in D-05). Maven: D-10 (Spring Boot 4.0.9, HttpClient5 5.6.5, HttpCore5 5.4.4; JUnit 6.0.3 and Testcontainers 2.0.5 with renamed artifacts follow the BOM) and D-12 (`firebase-admin` without Google Cloud Storage and Firestore, springdoc `-api`) were answered by the human; the resulting set scans clean. /implement must prove Firebase ID-token checking works without the two excluded libraries and check the JUnit 6 and Boot 4 compatibility of `archunit-junit5`, `pitest-junit5-plugin`, `bucket4j-core`, `poi-ooxml`. The scan needs `-DassemblyAnalyzerEnabled=false -DnugetconfAnalyzerEnabled=false -DnuspecAnalyzerEnabled=false -DossindexAnalyzerEnabled=false` (D-08); the NVD data is cached only in the session scratch folder (a new scan takes about 30 minutes). `./mvnw -v` and `npx` checks are deferred to /implement (D-06 option 3). The input manifest is written only when every check passes. Do not edit `tech-stack.md`; the build manifests follow D-02, D-05, D-10 and D-12.

## Next
- Command: /preflight
- Waiting for: the human adds `FIREBASE_PROJECT_ID`, `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN` to `.env` (no decision is open)

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
