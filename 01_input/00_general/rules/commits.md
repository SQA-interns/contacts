# Commits

> For: agent · Owner: Team lead · Agent: read-only

Commit as a human developer would: small, finished steps, never half a project. Each point below is a check, not advice.

- One commit = one logical change that can be reverted alone. Commit it when its checks pass, **before** starting the next change. *Why:* a change that cannot be reverted alone cannot be reviewed or undone alone.
- Size: at most 15 files and 400 changed lines, not counting logs, lock files and generated files. Check with `git diff --cached --shortstat` before every commit and split when over. *Why:* small commits can be reviewed; large ones are approved unread.
- One category per commit: production code together with the unit tests that drove it (`feat`, `fix`, `refactor`), acceptance or e2e tests (`test`), documents (`docs`), or build and dependency files (`build`, `chore`). Never add a document change to a code, test or build commit. *Why:* a reviewer reads each kind of change differently; mixing them hides one in the other.
- A `feat` commit made test-first carries in its body one line per new test: `Red: <test name> failed: <reason>`. *Why:* it is the evidence that the test came before the code (DoD-S-02).
- Each fix (`F-nn`) is its own commit.
- The build passes at every commit and no previously passing test fails. *Why:* any commit may be the one someone checks out or reverts to. Frozen acceptance and end-to-end tests of behaviour not yet built may fail.
- Message: `<type>: <summary> (IDs)`; type `feat`, `test`, `fix`, `refactor`, `docs`, `build` or `chore`; imperative; the **whole subject line including the IDs is at most 72 characters**. Name at most three IDs or one range (`AC-003-01..03`) in the subject; put the rest in the message body.
- Working tree clean at the end of every session.
- Do not squash, rebase or rewrite history. *Why:* the history is the evidence of how the result was reached; the manifests and traceability point into it.

## Commit units per phase

| Phase | One commit per |
|---|---|
| 0 | preflight report with the input manifest |
| 1 | requirements check |
| 2 | specification; each contract; each ADR; repository files (ES-03); each component build file (pinned dependencies); each component skeleton; task list; wireframes |
| 3 | story's acceptance tests; e2e tests; then the freeze commit (the two manifests only) |
| 4 | test-driven step of one task: a unit test with the code that makes it pass (`feat`), or a refactor with no behaviour change (`refactor`), each at most 15 files and 400 lines; each accepted design change (`refactor`); each review report alone |
| 5 | group of tests for one area (survivor round), at most 400 lines; tool configuration in its own `build` commit; the gate report alone |
| 6 | fix of one finding, with its re-run evidence; each verification report alone |
| 7 | each README, release notes, retrospective |
