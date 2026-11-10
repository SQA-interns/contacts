# Retrospective

> For: agent (writes), human (reads) · Written in: phase 7 (`/release`) · Source: `docs/decisions-log.md`, `docs/04_review-*.md`, `docs/06_verification-report.md`, `docs/00_progress.md` · Agent: writes

## What went well

- **Everything was delivered.** All 26 stories in two increments (`00_progress.md`), with each story's definition of done checked and its open parts named.
- **Inputs stayed the contract.** The input manifest matched 77 of 77 and the acceptance manifest 59 of 59 hashes in every verify run; no session needed to edit `01_input/`, and answers of the human replaced input entries only through decisions (D-25..D-27, D-37, D-69, D-86, D-90).
- **Blocking topics were asked, not guessed.** The questions about money, rights and personal data (roles D-14, retention D-15, account deletion D-16) stopped the run and were answered in one message each (D-25, D-26, D-27).
- **Isolation and security held.** The isolation test of every endpoint (`IsolationIT`, `ContractParityIT`, `EndpointSecurityTest`), the threat model and the log and error probes passed in every run; no Critical or High finding is open.
- **Quality figures are high where they were measured.** Line coverage 97.71 % in both components, backend mutation 97.37 % (963 of 989), performance NFR-01..04 far inside their limits on 10,000 Contacts.
- **Reviews found real defects early.** `/review INC-2` found F-14 (production could not read or delete accounts, D-63, D-69, D-70) before any release, and the 95 UI acceptance tests ended green after the frozen-test defects were approved (D-87, D-90).
- **The held-out suite generalised.** 209 of 214 hidden cases pass although the implementer never saw them.

## What held the run back

<!-- Each item with its evidence: decision ids, fix loops, failed gates, repeated findings. -->

1. **The inputs contradicted each other about Firebase credentials.** `secrets.env.example` lists `GOOGLE_APPLICATION_CREDENTIALS` as "not needed" and ADR-04 initialises the Admin SDK without credentials, but US-021 deletes accounts and needs them. It showed only when US-021 was implemented (D-63 a), then cost a blocking decision (D-69), a code change, a compose change, a review finding (F-14) and a lowering of its severity (D-79); the real project check is still open.
2. **Requirements that cannot be checked as written.** NFR-10 ("at most 12 actions") has no counting rule: 6 to 7 clicks or 17 to 20 actions (D-89). NFR-13 ("no copyleft code") collides with the Spring Boot default, Logback, and two annotation APIs (D-88). Both went through three verify runs and stay open (F-50).
3. **The pins of the technology stack were stale at the start.** Phase 0 needed five blocking records (D-03, D-04, D-06, D-07, D-11) and a move to Spring Boot 4.0.9 and a reduced Firebase library set (D-02, D-05, D-10, D-12); 20 of 92 decisions are of class `block`.
4. **Frozen tests did not fit the plan or the design.** Many stories were closed "passed in part" because their frozen tests need endpoints of later stories (D-40, D-42, D-44, D-46..D-60, D-62..D-68); four frozen UI locators were wrong and needed the human's approval (D-87, D-90, F-47), and `EX-007-05` contradicts rule R-007-03 for the whole run (F-08, D-51). Five UI acceptance tests stayed red until that was settled (F-47).
5. **Five hidden-suite failures could not be acted on.** F-41..F-45 failed in all five hidden runs; the report names only the rule and a category ("other", once "invalid"), so the cause stayed unknown, the one fix loop that tried (loop 1, D-84) did not remove them, and the human accepted the risk (D-86).
6. **Checks that need the human's machine or a push were found late.** Firefox never started in the sandbox (F-48, D-09), CI never ran remotely (F-51), and the `images` job failed at the production Compose check (F-59) because `db4fb46` changed the compose file after the pipeline was written; it was found only in the fourth run. Three fix loops and four verify runs ended with the gate not passed (`06_verification-report.md`).
7. **Process rules were seen only after they were broken.** 50 of 433 commit subjects are over 72 characters and several commits over 400 lines (F-53, D-62, D-64, D-66, D-68); history is not rewritten, so the finding stays.
8. **The runner's numbers do not show the real effort.** "Sessions started: 0" in the table below because the sessions were started by hand, not by the runner; the evidence above comes from the decisions log and the reports.

## Improvements

| # | Improvement | For (project input or template rule / skill) | Evidence |
|---|---|---|---|
| 1 | Check the list of secrets and external services against the stories that use them (an Admin API call needs credentials) before the inputs are frozen, and say in `secrets.env.example` which stories need which key | project (`/grill-me`, `/to-spec`: `secrets.env.example`, `architecture.md`) and template (`/to-spec` checklist) | D-63 a, D-69, D-70, F-14, D-79 |
| 2 | Give every NFR a counting or measuring rule and check the chosen stack against the licence NFR in `/preflight` (list weak-copyleft libraries of the default stack and ask once) | project (`requirements`: NFR-10, NFR-13) and template (`/preflight`) | D-88, D-89, F-50 |
| 3 | Let `/acceptance-tests` run its frozen UI tests against the approved wireframes and check that every frozen test only needs stories that come before it in the plan, or mark such tests as "waits for" in the manifest at the start | template (`/acceptance-tests`, `/plan`) | D-40..D-68 (waiting tests), D-87, D-90, F-47, F-08 |
| 4 | Give the hidden evaluation a failure report that names what kind of check failed (status code, missing field, wrong value, order) and allow one targeted repeat with it, or set a fix budget after which the human decides sooner | template (`general/testing/hidden-evaluation.md`, `/verify`) | F-41..F-45 in five runs, D-86 |
| 5 | Run the CI steps locally (or in a throw-away runner) after every change of CI, Compose or Dockerfile inputs, and list at the start what the sandbox cannot run (Firefox, a push, real Firebase) so the human prepares it | template (`/implement`, `/verify`, `/preflight`) | F-48, F-51, F-59, D-09, D-91 |

## Numbers

Written by `system/run.mjs --summary`.

| Measure | Value |
|---|---|
| Sessions started | 0 |
| Sessions failed (exit code) | 0 |
| Sessions per skill | none |
| Cost reported by the agents (USD) | not reported |
| Decisions | 92 |
| Decisions by class (allow / open / block) | 9 / 63 / 20 |
| Approvals | 17 |
| Verification runs | 4 |
| Commits | 439 |
