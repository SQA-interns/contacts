# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S1 /preflight
- Result: blocked
- Gate: phase 0 gate not passed (3 Firebase values empty in `.env`; D-07 Critical and High findings in Maven dependencies; Firefox start not verified; input manifest not written)
- Notes for the next session: Details in `docs/00_preflight-report.md`, evidence in `out/logs/preflight-*`. Done at the human's request (D-09): JDK 21.0.8, Node 22.20.0 (npm 10.9.3), gitleaks 8.28.0, Playwright 1.56.1 browsers, `.gitignore`, `.env` with the PostgreSQL values, `.agents/` and `.claude/` committed. npm: D-05 versions audited, only `firebase` and `firebase-tools` findings remain (accepted in D-05). Maven: D-07 is blocking (Spring Framework 6.2.19 and Spring Security 6.5.11 have no fixed 6.x release; Tomcat 10.1.61, Netty 4.1.140.Final, HttpClient5 5.6.5 and HttpCore5 5.4.4 overrides clear their findings). The scan needs `-DassemblyAnalyzerEnabled=false -DnugetconfAnalyzerEnabled=false -DnuspecAnalyzerEnabled=false -DossindexAnalyzerEnabled=false` (D-08); the NVD data is cached in the session scratch folder only. `./mvnw -v` and `npx` checks are deferred to /implement (D-06 option 3). The input manifest is written only when every check passes. Do not edit `tech-stack.md`; the build manifests follow D-02, D-05 and the answer to D-07.

## Next
- Command: /preflight D-07: 1
- Waiting for: D-07 (human decision); the human adds `FIREBASE_PROJECT_ID`, `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN` to `.env`

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
