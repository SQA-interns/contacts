# Requirements check

> For: agent (writes), human (reads) · Written in: phase 1 (`/clarify`) · Source: `project/01_requirements/*`, `general/requirements-standard.md`, `general/quality/definition-of-ready.md` · Agent: writes (results and gaps only; do not copy the requirements)

Session S2 `/clarify`, 2026-11-08. Phase 1 gate: **not passed** — three `Block` decisions (D-14, D-15, D-16) wait for the human; 11 of 26 stories are not ready because of them. Everything else is classified (D-17 to D-24) and does not block.

How this check was made: all 26 stories, `intent.md`, `business-rules.md`, `e2e-scenarios.md`, `security-requirements.md` and `quality-requirements.md` were read; IDs, rule-to-criterion coverage, `If … then` rules, e2e coverage and front matter were checked by script (scratch scripts, nothing kept in `out/logs/`). Limits: `interview.md` and `ui-design.md` are not on the reads list of `/clarify`, so Q ids and the fit of screens to the UI baseline were not checked (F-33, F-34).

## Run readiness (DoR-R)

| Item | Result | Note |
|---|---|---|
| DoR-R-01 | Met | `intent.md` has problem, users, core outcome, MVP, non-goals, principles (P-1 to P-5) and DoD-P01 to DoD-P07. |
| DoR-R-02 | Met with decisions | Project-wide facts exist for time zone, currency, tax, formats, languages, encoding, countries, phone, Trash period, page size, export limit. Four are `[ASSUMPTION]` (calendar-day zone, English time and number format, country list): D-20. |
| DoR-R-03 | **Not met** | Every `must` and MVP story must be ready; MVP stories US-012, US-013, US-014, US-015, US-016 wait for D-14 or D-15. |
| DoR-R-04 | Met | E2E-01 to E2E-06 each name stories and criteria; every cited AC exists; every MVP story (US-001 to US-019) is in at least one scenario (checked story by story). E2E-03 lists AC-018-04 but tests one endpoint: D-23. |
| DoR-R-05 | Met with decision | All NFR (01 to 15), DoD-P and principles say how they are checked. The SR table has no such column; D-24 records the check of each SR. |
| DoR-R-06 | Met | `docs/00_progress.md` and commit `4d4ddd9` record the phase 0 gate as passed. |

## Stories

Ready = all `DoR-S` items met, counting D-17 to D-24 as the recorded decisions (DoR-S-05, -07, -09, -10, -11 are met through them). "Not ready" = a `Block` topic is still open (DoR-S-07, -09). The rules → criteria map is in the appendix.

| Story | Ready | Failing DoR-S items | Rules → criteria | Findings (decision ids) |
|---|---|---|---|---|
| US-001 | yes | none | 12 rules, 11 AC, 9 EX; all rules covered | F-12 (D-18), F-25 (D-21), F-33 |
| US-002 | yes | none | 7, 8, 7 | F-25 (D-21) |
| US-003 | yes | none | 11, 10, 13 | F-25 (D-21), F-29 (D-23) |
| US-004 | yes | none | 9, 9, 9 | F-15 (D-19), F-25, F-29, F-30 |
| US-005 | yes | none | 10, 8, 8 | F-15 (D-19), F-25, F-29 |
| US-006 | yes | none | 8, 7, 6 | F-16 (D-19), F-24 (D-20), F-29, F-37 |
| US-007 | yes | none | 8, 8, 7 | F-25 (D-21), F-30 |
| US-008 | yes | none | 7, 9, 7 | F-29, F-30 |
| US-009 | yes | none | 12, 12, 7 | F-25 (D-21), F-29 |
| US-010 | yes | none | 10, 9, 5 | F-25 (D-21) |
| US-011 | yes | none | 10, 11, 10 | F-19, F-21 (D-19), F-29 |
| US-012 | **no** | DoR-S-07, -09 (R-012-03, R-012-08: Editor sees Trash) | 9, 8, 7 | F-01 (D-14), F-30 |
| US-013 | **no** | DoR-S-07, -09 (R-013-04 delay, R-013-06) | 8, 8, 6 | F-02 (D-15), F-29 |
| US-014 | **no** | DoR-S-07, -09 (R-014-01 Owner Role in an Invitation) | 12, 11, 9 | F-01 (D-14), F-10 (D-18), F-22 (D-19), F-29 |
| US-015 | **no** | DoR-S-07, -09 (R-015-01 Viewers see the Member list) | 8, 9, 5 | F-01 (D-14), F-29 |
| US-016 | **no** | DoR-S-07, -09 (R-016-07 retention) | 8, 9, 7 | F-02 (D-15), F-15 (D-19), F-29 |
| US-017 | yes | none | 9, 9, 5 | F-13 (D-18), F-29 |
| US-018 | yes | DoR-S-02 reading only (size) | 16, 17, 9 | F-09, F-11 (D-18), F-20 (D-19), F-31, F-32 (D-23) |
| US-019 | yes | none | 7, 7, 3 | F-25 (D-21) |
| US-020 | **no** | DoR-S-07, -09 (R-020-05 delay; interplay with US-021) | 8, 7, 7 | F-02 (D-15), F-03 (D-16), F-09, F-10 (D-18) |
| US-021 | **no** | DoR-S-07, -09, -10 (R-021-02, EX-021-04; no screen named) | 6, 7, 4 | F-03 (D-16), F-13, F-14 (D-18), F-33 |
| US-022 | **no** | DoR-S-07, -09 (R-022-06 Viewer refused) | 7, 7, 5 | F-01 (D-14), F-16, F-23 (D-19), F-37 |
| US-023 | **no** | DoR-S-07, -09 (R-023-06 Viewer refused) | 6, 7, 4 | F-01 (D-14), F-29 |
| US-024 | **no** | DoR-S-07, -09 (R-024-09), -10 (screen not named) | 10, 11, 6 | F-01 (D-14), F-17 (D-19), F-33 |
| US-025 | **no** | DoR-S-07, -09 (R-025-04 Viewer refused) | 5, 5, 4 | F-01 (D-14), F-29 |
| US-026 | yes | none | 12, 12, 9 | F-18 (D-19), F-31 (D-23) |

Ready: 15 (US-001 to US-011, US-017, US-018, US-019, US-026). Not ready: 11 (US-012 to US-016, US-020 to US-025).

## Findings

Class: Allow (the inputs settle it), Open (non-blocking decision with the most conservative reading), Block (blocking decision with numbered options and a default).

| ID | Story or file | Property or DoR item | What is missing | Consequence of a wrong reading | Class | Decision |
|---|---|---|---|---|---|---|
| F-01 | BR-02 matrix `*` rows; US-012 R-012-03/-08, US-014 R-014-01, US-015 R-015-01, US-022 R-022-06, US-023 R-023-06, US-024 R-024-09, US-025 R-025-04 | DoR-S-07, -09; correct | Roles for actions the interview does not assign (Trash, Versions, duplicates, Member list, Owner Role in an Invitation). | Editors or Viewers see or do what the customer reserves: Viewers read colleagues' e-mails, an invited Owner can delete the Group. | Block | D-14 |
| F-02 | US-013 R-013-04/-06, US-020 R-020-05, US-016 R-016-07, security log retention | DoR-S-07, -09; complete | Clean-up delay after day 30, History retention, log retention, content of the deletion entry. | Personal data stays longer than the customer expects. | Block | D-15 |
| F-03 | US-021 R-021-02, EX-021-04 × US-020 | complete; consistent | Whether a Group in Group Trash makes the User a Sole Owner; what pending Invitations do after account deletion. | The account cannot be deleted for 30 days, or a Group sits in Trash without an Owner, or a re-registered person gets access. | Block | D-16 |
| F-04 | 18 `OPEN — needs customer` questions with a developer answer | DoR-S-07 | Customer confirmation of rights, personal data, deletion, consent, destructive-action answers. | A different customer answer changes the story later. | Allow | D-17 |
| F-05 | `intent.md` OQ-P01, P02, P04 to P10 | DoR-R-01 | Customer answers; none changes a story. OQ-P05 (legal) is handled outside the software (Q-46). | NFR numbers or Level 3 security may change after the review. | Allow | D-17 |
| F-06 | `intent.md` P-4 | unambiguous | "A person" vs Firebase's verification and reset e-mails (US-001). | None: those are started by the user; P-4 concerns the people described by Contacts. | Allow | D-23 (f) |
| F-07 | US-013 R-013-06 vs BR-03 | consistent | "user (or system)" where BR-03 says name and e-mail of the acting user. | None: the story rule is the specific one; the clean-up has no user. | Allow | D-23 (f) |
| F-08 | `business-rules.md` "Stories" columns, story front matter | DoR-S-08 | BR-05 omits US-007, US-014; BR-07 omits US-022, US-026; US-006 lacks BR-07; US-022 lacks BR-04; 17 stories list BR-01 without repeating it. | None: every cited ID exists and each rule names its BR. | Allow | D-23 (e) |
| F-09 | US-018 R-018-02/EX-018-08 vs US-020 R-020-04/-07 | consistent | A Group in Trash is "not found" for everyone, yet Owners list and restore it. | Owners cannot restore, or non-Members see a trashed Group. | Open | D-18 (a) |
| F-10 | US-014 R-014-05 × US-020 | complete | Invitation to a Group in Trash. | A person joins a Group the Owners deleted. | Open | D-18 (b) |
| F-11 | US-018 R-018-01/-02/-03/-06 | unambiguous | Order of the checks on one request. | Wrong status code (403 vs 404) can reveal that a Group exists. | Open | D-18 (c) |
| F-12 | US-001, `security-requirements.md` | complete | Password and Google accounts with one e-mail address (User e-mail is unique). | Two Users for one person, or a failed first sign-in. | Open | D-18 (d) |
| F-13 | BR-03 audited set, US-020, US-021 | complete | Group delete and restore, and the end of a Membership by account deletion, are not audited. | A gap in the audit trail (P-3). | Open | D-18 (e) |
| F-14 | US-012, US-021 R-021-03, US-022 R-022-02 | complete | Which identity is shown for "deleted by" and for a Version's user, and what remains after account deletion. | An e-mail address survives an account deletion. | Open | D-18 (f) |
| F-15 | US-016 R-016-02, US-004 R-004-06, US-005 R-005-08 | unambiguous | How many History entries one save makes when fields, Tags and Attributes change together. | Tests and the History tab disagree. | Open | D-19 (a) |
| F-16 | US-022 definition of "current", US-006 R-006-07 | consistent | A Tag rename or delete changes a Contact without a new version number. | The comparison shows false or no differences. | Open | D-19 (b) |
| F-17 | US-024 R-024-03 | unambiguous | Sets when A~B (company) and B~C (phone) but A≁C. | Two overlapping sets or one; merges differ. | Open | D-19 (c) |
| F-18 | US-026 R-026-02, EX-026-09 | unambiguous; verifiable | Whether the chosen value must come from the merged Contacts; EX-026-09 cannot occur on the Merge screen. | A typed value bypasses the user's choice. | Open | D-19 (d) |
| F-19 | US-011 R-011-09 | complete | How the export request names the language. | Headers in the wrong language. | Open | D-19 (e) |
| F-20 | US-018 R-018-10, NFR-04 | unambiguous; feasible | Fixed or sliding minute; whether 4xx count; the 20-User load script and search-as-you-type against 100 requests per minute. | Normal use is answered 429. | Open | D-19 (f) |
| F-21 | US-011 R-011-03 | unambiguous | "Alphabetical" order of Tags and Attribute columns. | Different column order in tests. | Open | D-19 (g) |
| F-22 | US-014 R-014-02 | complete | E-mail format and length rule of an Invitation. | A 300-character address is stored. | Open | D-19 (h) |
| F-23 | `business-rules.md` data model, US-022 R-022-01 | consistent | ContactVersion has no time and user. | Versions tab cannot show them. | Allow (R-022-01 is explicit) | D-19 (i) |
| F-24 | `business-rules.md`, glossary, priorities | DoR-S-09 | 17 `[ASSUMPTION]` markers (formats, phone, version rule, countries, glossary entries, `*` audit rows). | A format or limit differs from the customer's. | Open | D-20 |
| F-25 | US-001 to US-026 | DoR-S-09 | About 60 story-level `[ASSUMPTION]` markers without business consequence. | Small UI or search differences. | Open | D-21 |
| F-26 | `e2e-scenarios.md` | DoR-S-09 | Five `[ASSUMPTION]` steps. | None: they are test steps. | Open | D-20 |
| F-27 | `quality-requirements.md` NFR-01 to NFR-09, DoD-P03 | correct | Numbers are the developer's proposals (OQ-P02, OQ-P04). | Customer wants other numbers. | Open | D-20 |
| F-28 | glossary vs stories | DoR-S-11 | Terms used in several stories but defined in one; Visitor, Operator, Membership, Group Trash, Invited person not in the glossary; Version vs version number. | Two meanings in code and tests. | Open | D-22 |
| F-29 | 21 stories (see D-23) | DoR-S-05 | `If … then` rules with no edge-case row (US-001: R-001-05, R-001-07, R-001-12; US-003: R-003-09, R-003-11; US-004: R-004-08; US-005: R-005-10; US-006: R-006-08; US-008: R-008-04; US-009: R-009-12; US-011: R-011-08; US-012: R-012-08; US-013: R-013-03; US-014: R-014-02, R-014-10; US-015: R-015-05; US-016: R-016-04, R-016-05; US-017: R-017-06 to R-017-08; US-018: R-018-03, R-018-05; US-020: R-020-02, R-020-06, R-020-08; US-022: R-022-06; US-023: R-023-03, R-023-04, R-023-06; US-024: R-024-09, R-024-10; US-025: R-025-03, R-025-04; US-026: R-026-12). | Strict reading fails 21 stories with no behaviour change; an acceptance criterion covers each. | Open | D-23 (a) |
| F-30 | US-004 R-004-02, US-007 R-007-08, US-008 R-008-05, US-012 R-012-05 | DoR-S-04; standard "Acceptance criteria" | `If … then` rules covered only by edge-case rows, no acceptance criterion. | None for tests; the standard asks for a criterion. | Open | D-23 (a) |
| F-31 | US-018, US-026 | DoR-S-02 (small) | 16 and 12 rules, estimates 5 and 8; largest stories (increment 2 third = 9.7, so within the limit). | A session may not finish one story. | Open | D-23 (b) |
| F-32 | US-018 AC-018-04, DoD-P02, E2E-03 | verifiable | The every-endpoint test exists only when all endpoints do; E2E-03 lists AC-018-04 for one endpoint. | US-018 cannot reach DoD-S early. | Open | D-23 (c) |
| F-33 | US-001, US-014, US-021, US-024 | DoR-S-10 | Registration and reset pages, the Invitations screen, where account deletion is offered, the duplicate search screen are not named; fit to `ui-design.md` not checked (not on the reads list). | Missing screens found late. | Open | D-23 (d) |
| F-34 | `security-requirements.md` | DoR-R-05 | SR-01 to SR-13 have no "how checked"; SR-03 and SR-09 have no acceptance criterion. | An SR without a test. | Open | D-24 |
| F-35 | all `sources:` Q ids | DoR-S-08 | Q ids not checked against `interview.md` (not on the reads list); all lie in Q-01 to Q-55. | A story cites a question that does not exist. | Open | D-23 (g) |
| F-36 | US-018 R-018-15/-16 order, EX-013-05/-06 order, E2E-01 step 2, EX-026-09 wording | conforming | Cosmetic. | None. | Allow | D-23 (f) |
| F-37 | US-006 R-006-05/-07, US-022 | consistent | A Tag deleted after a Version was saved is created again by a restore (US-023 R-023-05), possibly beside a renamed Tag. | Duplicate-looking Tags. | Open | D-19 (b), D-21 |

## Appendix: story → rules → criteria

Each rule with the acceptance criteria (AC) and edge cases (EX) that cite it; numbers only, same story. "—" means none (checked: no rule is without an AC or EX).

- **US-001**: 01→AC01,EX01,EX03,EX04,EX05,EX06; 02→AC03,EX07; 03→AC02,AC03,EX09; 04→AC01,AC04,EX09; 05→AC05; 06→AC06,EX02; 07→AC07; 08→AC08,EX08; 09→AC09; 10→AC10; 11→AC02,EX05; 12→AC11
- **US-002**: 01→AC01,AC02,EX01,EX04; 02→AC05,EX01,EX02,EX03; 03→AC03,EX05,EX06,EX07; 04→AC08; 05→AC01,AC04,AC06; 06→AC07; 07→AC04,EX07
- **US-003**: 01→AC01,EX01,EX02,EX10,EX13; 02→AC01; 03→AC02,AC03,EX04,EX05,EX06,EX07,EX08,EX09; 04→AC04; 05→EX03; 06→AC05; 07→AC06,EX11; 08→AC08,EX12,EX13; 09→AC07; 10→AC09; 11→AC10
- **US-004**: 01→AC01,EX06,EX08; 02→EX01,EX02,EX03,EX04; 03→AC02,EX07; 04→AC03,EX05; 05→AC04,AC08; 06→AC05,AC06; 07→AC01,AC09; 08→AC07; 09→EX09
- **US-005**: 01→AC01,EX06; 02→AC03,EX01,EX02,EX03,EX04; 03→AC02,EX05; 04→AC04,EX07; 05→AC06; 06→AC05; 07→AC06; 08→AC01,AC05,EX08; 09→AC07; 10→AC08
- **US-006**: 01→AC01; 02→AC01,EX05,EX06; 03→AC02,EX01,EX02; 04→AC03; 05→AC04,EX03,EX04; 06→AC04,AC05; 07→AC06; 08→AC07
- **US-007**: 01→AC01,EX01,EX02,EX07; 02→AC01,EX03,EX04; 03→AC03,EX05; 04→AC02; 05→AC07; 06→AC04,AC05; 07→AC06,AC08; 08→EX06
- **US-008**: 01→AC01,AC02,AC03,AC04,EX05,EX06,EX07; 02→AC05,AC06; 03→AC09; 04→AC05; 05→EX01; 06→EX02,EX03,EX04; 07→AC07,AC08
- **US-009**: 01→AC01,AC03,EX01,EX06; 02→AC02; 03→AC03; 04→AC04,EX05; 05→AC05,EX07; 06→AC06,EX04; 07→AC07,EX03; 08→AC03,AC08,EX02; 09→AC09; 10→AC10; 11→AC11; 12→AC12
- **US-010**: 01→AC01; 02→AC07; 03→AC01; 04→AC03; 05→AC03,EX01,EX02; 06→AC04,EX05; 07→AC02,AC09; 08→AC05,EX03,EX04; 09→AC06,AC08; 10→AC08
- **US-011**: 01→AC01; 02→AC02,AC08,AC09,EX01,EX09; 03→AC02,AC03,EX04,EX05,EX08; 04→AC02,EX06,EX10; 05→AC05,EX02; 06→AC04,EX03; 07→AC07,AC09; 08→AC06; 09→AC10,EX07; 10→AC11
- **US-012**: 01→AC01,EX03; 02→AC02; 03→AC02,AC08,EX07; 04→AC03,EX01,EX06; 05→EX02; 06→AC05,EX05; 07→AC04; 08→AC06; 09→AC07,EX04
- **US-013**: 01→AC01,EX01; 02→AC02; 03→AC03; 04→AC04,EX02,EX03; 05→AC01,AC05,EX04; 06→AC06; 07→AC07; 08→AC08,EX06
- **US-014**: 01→AC01,EX06; 02→AC04; 03→AC05,EX01,EX02; 04→AC06,EX05,EX08; 05→AC02,EX07,EX09; 06→AC03,EX07; 07→AC07; 08→AC08,EX02,EX03,EX04; 09→AC01; 10→AC09; 11→AC10; 12→AC11
- **US-015**: 01→AC01; 02→AC02,EX02; 03→AC03,EX03,EX04,EX05; 04→AC04,AC05,EX01; 05→AC01,AC06; 06→AC07; 07→AC08; 08→AC09
- **US-016**: 01→AC01; 02→AC02,AC08,EX01,EX02,EX03,EX07; 03→AC01,AC03,EX04,EX05,EX06; 04→AC04; 05→AC05; 06→AC06; 07→AC09; 08→AC07
- **US-017**: 01→AC01,EX04; 02→AC02,EX03; 03→AC03; 04→AC04,EX01,EX02,EX05; 05→AC05; 06→AC06; 07→AC07; 08→AC08; 09→AC09
- **US-018**: 01→AC01,AC02,EX07; 02→AC03,AC04,EX01,EX02,EX05,EX08; 03→AC05; 04→AC06; 05→AC07; 06→AC08; 07→AC09; 08→AC10; 09→AC11,EX02,EX06; 10→AC12,EX03,EX09; 11→AC13,EX04; 12→AC14; 13→AC15; 14→AC16; 15→AC17; 16→AC14
- **US-019**: 01→AC01,EX01; 02→AC02,EX02; 03→AC03,EX01; 04→AC04,EX03; 05→AC05; 06→AC06; 07→AC07
- **US-020**: 01→AC01,EX01,EX02; 02→AC02; 03→AC03,EX04; 04→AC01,EX03; 05→AC04,EX05; 06→AC05; 07→AC06,EX05,EX06,EX07; 08→AC07
- **US-021**: 01→AC01,AC03,EX02; 02→AC02,EX01; 03→AC04; 04→AC05; 05→AC06,AC07,EX04; 06→AC01,EX03
- **US-022**: 01→AC01,EX05; 02→AC01,EX04; 03→AC02,EX03; 04→AC03,AC04,EX01,EX02; 05→AC06; 06→AC05; 07→AC07
- **US-023**: 01→AC01,EX01,EX02,EX03; 02→AC02; 03→AC03,AC04; 04→AC05; 05→AC06,EX04; 06→AC07
- **US-024**: 01→AC07,EX05; 02→AC01,EX03; 03→AC02,AC03,AC04,EX01,EX02,EX04,EX06; 04→AC05; 05→AC01; 06→AC08; 07→AC06; 08→AC11; 09→AC09; 10→AC10
- **US-025**: 01→AC01,EX03,EX04; 02→AC02,EX01,EX02; 03→AC03; 04→AC04; 05→AC05
- **US-026**: 01→AC01; 02→AC01,EX02,EX03; 03→AC02,EX04; 04→AC03,EX05,EX08; 05→AC04,AC05,EX01; 06→AC06,EX09; 07→AC07,EX01; 08→AC08,EX07; 09→AC09,EX06; 10→AC12; 11→AC10; 12→AC11

Story → business rules (BR) as listed in the front matter of the stories and the "Stories" column of `business-rules.md` (differences: F-08): BR-01 and BR-02 apply to US-002 to US-026 (isolation and Roles); BR-03 to US-004, US-005, US-006, US-011 to US-017, US-023, US-026; BR-04 to US-007, US-008, US-009, US-012, US-013, US-020, US-022, US-024, US-026; BR-05 to US-002, US-004 to US-009, US-014, US-024; BR-06 to US-003, US-023, US-026; BR-07 to US-003 to US-006, US-022, US-023, US-026. Story → e2e: see `e2e-scenarios.md`; US-020, US-021, US-022, US-023 and US-025 are in no scenario (second increment, not required by DoR-R-04); US-024 and US-026 are in E2E-05.
