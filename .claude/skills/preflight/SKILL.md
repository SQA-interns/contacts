---
name: preflight
description: Session S1, phase 0. Check that every platform, tool, browser, service, secret and dependency the run needs is present, resolvable and free of known Critical or High vulnerabilities, ask for whatever is missing, and freeze the inputs with a manifest.
disable-model-invocation: true
---

> For: agent · Owner: Team lead · Agent: read-only

## Session

- Id: S1, phase 0. Frame: measure.
- Reads: `project/00_setup/tech-stack.md`, `environments.md`, `secrets.env.example` (key names), `run-config.md` ("Pre-approved findings"); `general/rules/decisions.md`; `general/rules/secrets.md`; `general/quality/severity-scale.md`.
- Writes: `docs/00_preflight-report.md`, `docs/00_input-manifest.sha256`, `docs/decisions-log.md`, `docs/00_progress.md`, `out/logs/`.
- Must not: read requirements or design; install, upgrade or substitute anything.
- Exit (phase 0 gate): every check passes; every dependency resolves with no known Critical or High vulnerability; the input manifest is written.

## Procedure

1. For every `platforms` and `tooling` entry: run its `version_check` (or the tool's own version command) and confirm the listed version. A pin that runs but reports another version (for example an image tag) is a non-blocking decision; one that does not resolve or run is a failure.
2. For a component with a user interface: confirm the browser test runner starts its browser and saves a screenshot of a blank page (`out/logs/preflight-screenshot.png`). *Why:* `/wireframes` and every UI check depend on it, and its browsers are a separate download.
3. For every environment and service in `environments.md` needed locally: confirm it is running or reachable.
4. For every key of `secrets.env.example` not marked test-only or not needed: confirm `.env` gives it a non-empty value with `sed -n 's/^KEY=//p' .env | tr -d ' \r"'"'"'' | grep -q .` (reads a last line without newline, removes Windows line endings, treats quotes or spaces as empty; never displays the value).
5. For every `dependencies` entry: confirm the exact version resolves. A pin marked `[ASSUMPTION] verify in preflight` that does not resolve, or has a newer patch with a security fix, is replaced as `general/rules/decisions.md` allows (same minor: non-blocking; otherwise blocking).
6. Scan the dependency set with the `dependency-scan` tools, using each tool's `options`. Critical or High is a blocking decision unless "Pre-approved findings" covers it. An analyser that needs credentials `secrets.env.example` does not list: disable it, record a non-blocking decision, re-run; at least one analyser must complete.
7. Confirm the working tree is clean (apart from an untracked `.env`).
8. Re-run every failed check a second, different way; report only failures both confirm. Write each result with its method (never values) to `docs/00_preflight-report.md`.
9. If anything failed: ask for all of it in one message (what to install or start with its exact version; which key to add to `.env`; version or vulnerability decisions) and end the session. The next `/preflight` re-runs only the failed checks.
10. When everything passes, write `docs/00_input-manifest.sha256`: SHA-256 of every file under `01_input/`, `.claude/skills/` and `system/`, and of `AGENTS.md`, `CLAUDE.md`, `README.md`, `Run_Guide.md`; LF-normalised, `sha256sum` format.
11. Commit the report with the manifest (`docs:`); hand over (next: `/clarify`).
