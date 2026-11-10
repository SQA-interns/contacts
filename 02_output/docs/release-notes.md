# Release notes

> For: agent (writes), human (reads) · Written in: phase 7 (`/release`) · Agent: writes

Version 1.0.0 of Contact Groups (Skupine kontaktov). State of the code: the commit of `/fix F-59` (`a629ad6`) plus the documents of phase 7. **The phase 6 gate was not passed** (`DoD-R-01`, `DoD-R-13`, `DoD-R-14`, `DoD-R-15` not met in the last verify run, no Critical or High finding open). The reasons are listed under "Known limitations"; the human accepted the run as it stands (D-86, D-91). Read the sections "Must be tested manually by a human" and "Decisions pending review" before the product goes to production.

## Delivered

All 26 stories are implemented, in two increments. How to start and operate the product: [`../README.md`](../README.md), [`install-and-backup.md`](install-and-backup.md) (operator), [`user-guide-sl.md`](user-guide-sl.md) (users, Slovene), [`openapi.json`](openapi.json) (interface).

**Increment 1 (MVP)**

| Story | What the user can do |
|---|---|
| US-001 | Register and sign in (e-mail with password, Google, verification and reset e-mails) |
| US-002 | Create a Group and see their Groups |
| US-003 | Create, view and edit a Contact |
| US-004 | Add and change Attributes of a Contact |
| US-005 | Tag a Contact |
| US-006 | Rename and delete a Tag |
| US-007 | Browse the contact list |
| US-008 | Search Contacts |
| US-009 | Filter Contacts |
| US-010 | Select Contacts in the list |
| US-011 | Export the Selection to Excel |
| US-012 | Delete a Contact and restore it from Trash |
| US-013 | Delete a Contact forever |
| US-014 | Invite a person to a Group |
| US-015 | Manage Members and Roles |
| US-016 | See the History of a Contact |
| US-017 | See the Group activity list |
| US-018 | Secured REST interface and Group isolation (404 for non-Members, 401, 403, rate limit, size limit, HTTPS only) |
| US-019 | Switch between Slovene and English |

**Increment 2**

| Story | What the user can do |
|---|---|
| US-020 | Delete a Group (Group Trash, restore, permanent deletion after 30 days) |
| US-021 | Delete their account |
| US-022 | View and compare earlier Versions of a Contact |
| US-023 | Restore an earlier Version of a Contact |
| US-024 | Find duplicate Contacts |
| US-025 | Mark a Duplicate set as "not a duplicate" |
| US-026 | Merge duplicate Contacts |

Evidence at the last verify run (run 4, a self-check by the agent, not an independent review; `06_verification-report.md`): backend 844 of 845 tests pass (the failing one is `EX-007-05`, below); frontend unit tests 597 of 597; UI acceptance 95 of 95; end-to-end scenarios E2E-01..06 pass in Chromium and WebKit; accessibility checks (axe, three widths, light and dark) show no Critical or Serious violation on any screen that ran; performance NFR-01..04 met on 10,000 Contacts with 20 concurrent Users; the production images build and start healthy in 18 seconds; a backup and restore were tested once. Held-out suite: 209 of 214 pass (`06_hidden-evaluation.md`).

## Known limitations

Open at release, none Critical or High.

| Id | Severity | What | Why it stays | Decision |
|---|---|---|---|---|
| F-41..F-45 | Medium | Five held-out cases fail: `R-003-02` (invalid input), `R-006-07`, `R-007-03`, `R-017-02`, `R-018-07`. The cause is unknown and unreproduced; the same five failed in every hidden run. The behaviour may be wrong in a case the visible tests do not cover. | Accepted risk by the human, not a finding that the behaviour is correct | D-86 |
| F-48 | Medium | Firefox does not start in the agent's sandbox, so the E2E scenarios and UI checks never ran in Firefox; the WF-18 Merge screen crashes the WebKit tab on `page.reload` with a mocked 500 error (Chromium passes). | Needs the human's machine | D-91, D-92 |
| F-51 | Medium | The CI pipeline (`.github/workflows/ci.yml`) never ran remotely: nothing was pushed. Every job was run locally. The `images` job failed locally at the production Compose check (F-59); fixed in `a629ad6` and replayed by `scripts/check-ci-compose.sh`, but the fix was not verified again. | The human pushes `main` | D-91, D-92 |
| F-50 | Medium | NFR-10 (at most 12 actions in E2E-01): 6 to 7 clicks, or 17 to 20 actions when typing and each field count; not met under the stricter counting, met under the click counting. NFR-13 (licences): see the table below. | Waits for the human | D-88, D-89 |
| F-38, F-39 | Medium, Low | Frontend mutation score is below the 90 % threshold (shortfall 7.57 points after three survivor rounds; the later `useSuggestions.tsx` scores 56.67 % on its own). Backend: 963 of 989 mutants killed, 97.37 %. Line coverage 97.71 % in both components. | Accepted as a shortfall | D-81, D-82 |
| F-08 | Low | `EX-007-05` (frozen test) contradicts rule R-007-03; the backend `verify` ends with this one red test. | The frozen test is not changed by the run | D-68 i |
| F-14 | Medium (lowered from High) | Sign-in of a new person and account deletion were implemented and tested against the Auth emulator only; nothing was shown against the real Firebase project. | Needs the production credentials of the customer | D-69, D-70, D-79 |
| F-19, F-20..F-25 | Low | Details of the Survivor of a Merge that changes nothing (F-19) and Low findings of the reviews | Wait for the human or accepted | D-68 d |
| F-49 | Low | Two non-secret values of `.env` match a scanner pattern | Accepted | D-83 |
| F-53 | Low | 50 of 433 commit subjects are longer than 72 characters; history is not rewritten | Accepted | |

Licences of the backend (NFR-13, D-88, unresolved): the npm production tree is permissive only (MIT, Apache-2.0, BSD, ISC, 0BSD). Of 138 Maven runtime dependencies, five are not purely permissive; each is used unmodified as a library and is offered under a weak-copyleft licence of its own choice, none is GPL without the exception:

| Library | Licence |
|---|---|
| `ch.qos.logback:logback-classic` and `logback-core` 1.5.38 | EPL-2.0 or LGPL-2.1 |
| `jakarta.annotation:jakarta.annotation-api` | EPL-2.0 or GPL-2.0 with the Classpath Exception |
| `javax.annotation:javax.annotation-api` | CDDL-1.1 or GPL-2.0 with the Classpath Exception |

NFR-13 asks for no copyleft code; whether this meets it is the customer's legal choice. The alternatives are replacing Logback by Log4j2 (a change of the technology stack) or rewording NFR-13 (D-88).

Other things to know:
- The Google profile name of a new person is not shortened: a Google name longer than 100 characters makes the first request fail with `500 server.error` (D-41 c). It needs an answer before the release.
- Server errors that Firebase causes (for example `getUser` failing) answer 500, not 503 (D-70, option 2 not built).

## Must be tested manually by a human

| # | Test | Why no automated evidence | Source |
|---|---|---|---|
| 1 | Real **Google sign-in**, and the real **verification and password-reset e-mails** of Firebase | The emulator cannot send e-mails or talk to Google | `environments.md` |
| 2 | Open an exported `.xlsx` in real **Microsoft Excel** once (Q-06, check 3): columns, text cells, no formula executed | Tests read the file back with a library only | `environments.md` |
| 3 | **Production Firebase project:** create the service account ("Firebase Authentication Admin"), start production, then show (a) a first sign-in of a new person (`GET /me` answers 200), (b) `DELETE /me` deletes the account, (c) a token of a deleted account is refused | Nothing in the run talks to a real Firebase project | D-63 a, D-70 b, c |
| 4 | **Restore test on the customer's server** (`scripts/restore.sh`, row counts equal) and one daily `backup.sh` run | A restore was tested once on a development machine (run 2) | DoD-P07, `install-and-backup.md` |
| 5 | **Firefox** (and WebKit for the Merge screen): `npm run e2e` and `npm run e2e:scenarios` with `--project=firefox` and `--project=webkit` on a machine where the browsers start | F-48 | D-91 |
| 6 | **Push `main`** and see the CI pipeline pass, in particular the `images` job after F-59 and the dependency scan | The pipeline never ran remotely | F-51 |
| 7 | **Read the guides**: a person follows `install-and-backup.md` on a clean server and `user-guide-sl.md` in the product | Their content was never tested by a person | DoD-P04 |
| 8 | **Look at the screens** against the wireframes (WF-01..WF-19) in the final theme, in particular the Attribute and Tag suggestion list changed in F-47 | The last runs compared by axe and layout checks, not by eye | run 4 |
| 9 | **Behind the customer's TLS proxy**: the page and `/api/` work, plain HTTP from outside answers 403, `CORS_ALLOWED_ORIGIN` equals the page address | Production was started and probed on `localhost` only; no sign-in flow was run on it | run 4 |
| 10 | The five held-out failures (F-41..F-45): try the rules `R-003-02`, `R-006-07`, `R-007-03`, `R-017-02`, `R-018-07` by hand with unusual input | Unknown cause | D-86 |

## Decisions pending review

Non-blocking records with the resolution "pending review": the agent chose the default option and continued; the human should read them and say if an option 2 is preferred. All are in [`decisions-log.md`](decisions-log.md).

**Waiting for an answer of the human** (they change the product or its acceptance):

| Decision | Question |
|---|---|
| D-88 | Accept the five weak-copyleft libraries, replace Logback, or reword NFR-13? |
| D-89 | Which counting rule for NFR-10? (the report states both) |
| D-63 a, D-70 b, c | Supply the Firebase service account for production and show the real sign-in and account deletion |
| D-41 c | What to do with a Google name over 100 characters (cut it, or use the e-mail) |
| D-68 d (F-19) | Version of the Survivor of a Merge that changes nothing: keep the number (as built) or always raise it |
| D-87 | The four frozen-test locators were changed with the human's approval (D-90); the record stays pending review |
| D-92 | The outcome of run 4 and the known limitations above |

**Pending review, nothing waits on them:** D-01, D-02, D-08 (host tool versions, version replacements, disabled scan analysers); D-17 to D-24 (developer answers and readings of the requirements); D-28 to D-34 (design assumptions, configuration keys, development tools, dependency changes); D-36 (wireframes, approved by D-37); D-38 to D-68, D-70 to D-78 (readings of each story made while implementing, and the fixes of the first review); D-81, D-82, D-84, D-85 (how mutation was measured, the fix loops).

Decided by the human: D-05, D-06, D-09, D-10, D-12, D-25, D-26, D-27 (blocking questions of phases 0 and 1), D-37, D-69, D-79, D-80, D-83, D-86, D-90, D-91.

## Data retention duties

The operator and the customer, not the software, keep these promises (`security-requirements.md`, D-26).

| Data | Retention | Who does it |
|---|---|---|
| Contact data | Until the Contact is deleted; then 30 days in Trash; the scheduled clean-up deletes it for good at most 24 hours after day 30 (`TRASH_PURGE_CRON`, hourly by default) | the backend |
| Groups in Group Trash | the same 30 days, then deleted with all their Contacts | the backend |
| **Database backups** | **14 days.** A Contact or an account deleted for good stays in the backups until they expire; copies of the backups elsewhere must not be kept longer | the operator (`scripts/backup.sh` removes dumps older than 14 days, `RETENTION_DAYS`) |
| User account | While it exists; deleted with the account (US-021), also in Firebase | the backend (needs the service account) |
| History | As long as the Group exists; after a Contact is deleted for good its values read "removed", after an account is deleted only the name stays. Both are the developer's answers and wait for the customer (OQ-013-01, OQ-021-01) | the backend |
| Invitations | 14 days, then expired; removed with the Group | the backend |
| **Server log** | **30 days** [ASSUMPTION], set by the operator; contains request time, path, status and User identifier, no Contact data and no tokens | the operator (Docker log rotation) |

Legal matters (controller or processor role, data-processing agreement, privacy notice, consent) are the customer's, outside the software (Q-46); OQ-P05 about special categories of data is open.

## How to deploy

Summary in [`../README.md`](../README.md) ("Deploy"); the full guide for the operator is [`install-and-backup.md`](install-and-backup.md).

1. Build the two images (`docker build -t contact-groups-backend:1.0.0 backend`, `docker build -t contact-groups-frontend:1.0.0 frontend` in `02_output`).
2. Create `production.env` (names in section 3 of the guide) and the Firebase service-account file; put a TLS reverse proxy in front of ports 8081 (page) and 8080 (`/api/`).
3. `docker compose -f docker-compose.prod.yml --env-file production.env up -d`, then `curl -fsS http://127.0.0.1:8080/health`.
4. Schedule `scripts/backup.sh` daily, run the restore test once, then the manual tests 1 to 4 and 9 above.
