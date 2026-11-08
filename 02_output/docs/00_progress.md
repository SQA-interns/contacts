# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S1 /preflight
- Result: blocked
- Gate: phase 0 gate not passed (platforms and tools: no JDK, Node 26 not 22.20.0, no gitleaks, no Playwright browsers; no `.env`; npm Critical/High findings; untracked `.agents/` and `.claude/`; input manifest not written)
- Notes for the next session: Full results are in `docs/00_preflight-report.md`, evidence in `out/logs/preflight-*`. All 93 pinned versions resolve and the Boot 3.5.7 BOM matches the pins. Non-blocking, recorded: D-01 (Docker, Compose, npm newer than pins), D-02 (Maven pins to 3.5.16 and pgjdbc 42.7.14, pending review; `tech-stack.md` is not edited). Blocking (D-03 now answered by D-05; first listing kept): D-03 (npm: `react-router`, `zod`, `firebase` runtime; `vite`, `vitest`, `@stryker-mutator/*`, `shadcn`, `firebase-tools` tooling) and D-04 (host prerequisites, secrets, git state). The Maven transitive scan (`dependency-check-maven`) and the Playwright screenshot could not run and must be done next time, after the JDK, Node 22 and the browsers exist. Re-run only the failed checks. D-05 approved the npm version changes: audit them again with the real tools once Node 22 exists. Do not edit `tech-stack.md`; the build manifests follow D-02 and D-05. The input manifest is written only when every check passes.

## Next
- Command: /preflight
- Waiting for: D-06 (the human installs JDK 21.0.8, Node 22.20.0, gitleaks 8.28.0, Playwright browsers; creates `.env`; commits `.agents/` and `.claude/`; adds `.gitignore` with `.env`)

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
