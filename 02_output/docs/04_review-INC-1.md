# Review of INC-1

> For: agent (writes), human (reads) · Written in: phase 4 (`/review`) · Frame: criticise · No fixes here.

Range: `1035623^..HEAD` (first code commit to `58d0952`), stories US-001..US-019 of INC-1.

## How it was checked

| Check | Method | Coverage |
|---|---|---|
| Test-first evidence | every `feat` commit body searched for `Red:` lines | all 81 `feat` commits |
| Authorisation, validation, errors, headers | live calls to the running test stack (seeded users Ana Owner, Boris Editor, Cene Viewer; Group Acme) | Activity, Contacts, Tags, Groups, health |
| Output encoding, logs | grep for `dangerouslySetInnerHTML`, `console.*`, log statements | whole frontend and backend |
| Wireframe comparison | WF-13 desktop wireframe against the implementer's screenshot; list of screenshot folders against the wireframe inventory | not every screen was opened one by one |
| Rule-to-test mapping | sampled for US-017 and the security stories, not for all 19 stories | partial |

Not done: I did not take my own screenshots at the three widths; I read the screenshots under `logs/screens/` made by the implementing sessions. I did not read every test of every story. Both limits are named in F-08.

## Findings

| ID | Severity | Story | Finding |
|---|---|---|---|
| F-01 | High | US-006, US-010..US-017 (all Group screens) | The Group tab bar of the wireframes (Contacts, Tags, Trash, Duplicates, Activity, Members, Invitations) and the Group bar (Group name, Role badge, "Možnosti skupine") do not exist. `app/Layout.tsx` is a header, `main` and footer only. The links to Trash, Members, Invitations and Activity are ad hoc buttons on the Contact list (`features/contacts/ContactListPage.tsx:70-85`). **There is no link to the Tags page (`/groups/:g/tags`) anywhere in the UI**; it is reachable only by typing the address. Moving between Group screens needs the Contact list each time. The structure differs from every Group wireframe (`docs/wireframes/README.md` lines 9, 11); DoD-I-04 is not met. |
| F-02 | High | US-006 | Consequence of F-01: the Tag list (US-006, WF-09) cannot be reached by a User; rename and delete of Tags are unusable without knowing the URL. |
| F-03 | Medium | US-016, US-017 | A malformed `page` value on the History endpoints answers 500 `server.error` instead of 400 `request.malformed`: `GET /groups/{g}/activity?page=abc` and `GET /groups/{g}/contacts/{c}/history?page=abc`. The Contact list handles it (400, "Page must be a whole number of at least 1"). `ApiExceptionHandler` has no handler for argument type mismatch. Breaks R-018-09 intent (a client error shown as a server error) and puts a client mistake into the error log. Evidence: live calls, `web/ActivityController.java:30`, `web/ApiExceptionHandler.java`. |
| F-04 | Medium | US-018 | `GET /health` answers `{"groups":["liveness","readiness"],"status":"UP"}`; AC-018-09 asks for the status and nothing else. Already noted in D-44 as "for `/fix` or review"; still open. Frozen test `ac_018_09` fails for it. |
| F-05 | Medium | US-003, US-004 | No screenshots of WF-07 (Contact) and WF-08 (new Contact) exist in `logs/screens/`, nor of WF-19 (not found, US-018/US-019). The decisions of US-003 (D-46 item i) deferred them until the list existed; no later session made them. DoD-S-05 and DoD-I-04 are open for these screens. |
| F-06 | Medium | US-017 | In the WF-13 screenshot the date fields show the browser's US format (`11/09/2026`); the wireframe shows `d. M. llll` and R-019-04 asks for Slovene date formats. Time and date cells in the table are formatted correctly. Also the "Počisti filtre" button sits below the filters as a button, the wireframe has a text link; layout otherwise matches. Evidence: `logs/screens/WF-13_group-activity/list-desktop.png`. |
| F-07 | Low | US-017 | Wireframe WF-13 shows page-number buttons (1, 2, 3, 4); the page has only "Prejšnja"/"Naslednja" and "Stran 1 od N". No rule asks for numbers. |
| F-08 | Low | all | DoD-S-01 is "passed in part" for 17 of 19 stories because frozen tests wait for US-020 and US-026 (INC-2) or D-58 item g. These cannot be closed in INC-1; the release gate must re-run them. Test stack data was changed by this review: the seed was loaded and one Contact (`<script>alert(1)</script>`, id `2b157190-…`) was created in Group Acme through the API to test output handling; the page output is encoded (no `dangerouslySetInnerHTML` outside the lint-rule test). |
| F-09 | Low | scaffolding | Eleven `feat` commits carry no `Red:` line (`1035623`, `24625fa`, `6e89e65`, `d96a78a`, `713802d`, `7c0600c`, `a0a8b38`, `7d0d579`, `1ede227`, `524243c`, `79a2159`). Ten are skeleton, configuration or catalog files with no test of their own; `1ede227` changes `ApiExceptionHandler` and adds an IT, so it is test-first without the line; `79a2159` says its test is in the next commit. Commit hygiene only. |

## Checks without a finding

- Authorisation (SB-02, SR-01): every operation tried without a token answers 401; a non-member gets 404 `group.not_found`; a Viewer gets 403 on the Activity list and on creating a Contact (R-017-08); a Contact id of Acme asked through Group Beta is 404; malformed Group id is 404.
- Validation on the trusted side (SB-01): bad `action`, `userId`, date and inverted range give 400/422 with the catalog message; a quote in a date is rejected, not executed. Queries use named parameters; the string concatenation found in repositories joins constant fragments only.
- Headers (SB-10): API sends CSP `default-src 'none'`, `X-Frame-Options: DENY`, `nosniff`, no-store; nginx sends a CSP and the same set.
- Logs (ES-07, SB-07): `SecurityLogger` writes status, method and path, no query, body or token; no `console.*` in production frontend code; errors show catalog messages, no stack.
- US-017 against its rules: R-017-01..09 are implemented in `ActivityController`, `HistoryQueryService`, `ActivityRange`, `ActivityFilter`, `ActivityPage`; nothing outside the story was found; both `feat` commits of the story carry `Red:` lines.
- Out of scope: no Group deletion, Versions, duplicates or merge code was found in INC-1 (routes list).

## Definition of Done: increment

| DoD-I | Result | Evidence |
|---|---|---|
| DoD-I-01 | Not met in full | every story is "passed" or "passed in part"; open parts are listed in F-05, F-08 |
| DoD-I-02 | **Not met** | F-01, F-02 High |
| DoD-I-03 | Partly | the stack runs, the Activity, Contacts, Groups and security flows work at runtime; the Tag page is not reachable (F-02) |
| DoD-I-04 | **Not met** | F-01, F-05, F-06 |

## Handover

Two High findings are open: next is `/implement US-006`, whose Group navigation (F-01, F-02) is the first task; the Group shell is shared by all Group screens, so that session builds it once and `/review` runs again afterwards. F-03 and F-04 belong to the next session on US-016 and US-018 or to `/fix`.
