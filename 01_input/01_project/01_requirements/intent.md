# Intent

> For: agent (reads), human (reviews) · Owner: Product owner · Agent: read-only

Why the product exists and what "done" means for the customer. Sources: interview `Q` ids.

## Problem statement

People who look after the contact data of a project or a company manage it today in Excel sheets and e-mail lists, or in a competing CRM system that is too complex or too expensive for them (Q-01, Q-02). In Excel, the same person appears several times, nobody knows who changed what, and a list for a mailing can include people who must not be notified (`neObvescaj`). The cost of this (lost time, duplicate mailings, contacting people tagged `neObvescaj`) is not known (Q-01, open).

## Product statement

A multi-tenant ("večnajemniški") web system, "Contact Groups" (working title, Slovene "Skupine kontaktov"; Q-53), in which every user creates Groups for a project or company and keeps Contacts in them, visible only to the Members of the Group (Q-03, Q-09). Contacts have predefined fields, free Attributes and Tags; every change is recorded; users search, filter and export selected Contacts to Excel. The backend is reached only through a secured REST interface, authentication is by Firebase and authorisation is the product's own (Q-19, Q-21, Q-24). Its competitive advantage over CRM systems is ease of use (Q-02).

## Users

| Role | Primary or secondary | Needs | Must not be able to |
|---|---|---|---|
| Owner | primary | Create Groups; do everything with Contacts; invite, change and remove Members; delete the Group; delete Contacts forever (Q-08) | Edit or delete History entries; see any Group they are not a Member of |
| Editor | primary | Create, edit, tag, delete, merge and export Contacts; see History (Q-08) | Manage Members; delete the Group; delete Contacts forever; see any Group they are not a Member of |
| Viewer | secondary | Search, filter and view Contacts (Q-08) | Change anything; export (Q-26); see History |
| Invited person | secondary | Join a Group after signing in with the invited, verified e-mail (Q-25) | Get access before the e-mail is verified |
| Operator (customer's staff) | secondary | Install, back up and monitor the system (Q-52) | See or manage a Group through the product; there is no in-app administrator (Q-27) |
| Other tool (reads the REST interface or an Excel file) | secondary | Use exported data and respect `neObvescaj` (Q-32) | Call the REST interface without a valid token (Q-39) |

The people described by Contacts are not users of the product; the product never contacts them (Q-33, Q-45).

## Core outcome

Users keep the Contacts of each project or company in one place that is simple to use, isolated from everyone else's data, traceable, and exportable to Excel. The measurable checks below are a developer proposal and not confirmed by the customer (OQ-P02):

1. a new user signs in, creates a Group and adds a first Contact in under 2 minutes without help (Q-06);
2. search and filter results appear in under 1 second in a Group of 10,000 Contacts (Q-50; Q-06 proposed about 1,000);
3. selected Contacts export to an `.xlsx` that opens in Excel with all columns filled (Q-06, Q-36);
4. a user never sees another Group's Contacts (tested, zero tolerance; Q-06, Q-55).

## MVP

The MVP is the first increment: US-001 to US-019 (all stories with `mvp: true`). It covers sign-in, Groups, Contacts with predefined fields, Attributes, Tags, list, search, filter, Selection, Excel export, Trash, Invitations and Roles, History, the secured REST interface and the language switch.

The second increment is US-020 to US-026: deleting a Group, deleting an account, Versions and their restore, duplicate search, "not a duplicate" and merge (Q-14, Q-15, Q-16; Q-55 item 5).

## Scope

In scope (Q-04: design, upgrade, implement and deploy):

- Sign-in with Firebase (US-001) and language switch (US-019).
- Groups (US-002, US-020), Members and Invitations (US-014, US-015), account deletion (US-021).
- Contacts: create, view, edit (US-003); Attributes (US-004); Tags (US-005, US-006).
- Overview, search, filter, Selection (US-007 to US-010); Excel export (US-011).
- Trash and permanent deletion (US-012, US-013).
- History and Group activity list (US-016, US-017); Versions (US-022, US-023).
- Duplicate search and merge (US-024, US-025, US-026).
- Secured REST interface and isolation (US-018).
- Deployment as container images with a Docker Compose setup, an install and backup guide, an OpenAPI description and a short user guide in Slovene (Q-52, Q-55).

## Non-goals (out of scope)

From Q-45 (the agent must not add any of these):

- Sending e-mail or any notification to contacts (Q-32, Q-33).
- Import from Excel or CSV; export to CSV (Q-36).
- API keys for other tools (Q-39).
- Typed Attributes (numbers, dates), saved filters, fuzzy duplicate matching.
- CRM activity features: tasks, deals, notes timeline, calendar, pipelines.
- Real-time collaboration, mobile app, offline use.
- Payments, billing, subscription plans.
- Contact data from external sources (social networks, address books).
- Renaming a Group (not in the interview; OQ-002-01).
- An in-app administrator (Q-27).
- Reusing code of the SCM example solution (Q-05, Q-51).

## Product principles

Each principle is checked as written (Q-45).

| ID | Principle | How it is checked |
|---|---|---|
| P-1 | A user never sees, changes or exports a Group they are not a Member of. | Isolation test: every endpoint with a user of another Group answers 404 and no data (US-018 AC-018-04); E2E-03. |
| P-2 | No data is lost silently: Trash, refused concurrent edits, undoable merge. | AC-012-03, AC-003-06, AC-026-10. |
| P-3 | The audit trail is complete and nobody can edit it. | AC-016-05; the History entries of every audited action (BR-03). |
| P-4 | The product never contacts a person on its own. | AC-014-11; no mail or messaging dependency in `tech-stack.md`. |
| P-5 | Simple wins: no feature adds a step to the common flows (create a Contact, search, export) if fewer steps give the same result. | Wireframe review and a step count of the three flows in E2E-01 and E2E-04. |

## Open questions

Product-level questions; story-level questions are in the story. Every question below is `OPEN`; the review (`/run` asks them) or the customer answers.

| ID | Question | Owner | Due | Answer |
|---|---|---|---|---|
| OQ-P01 | What does the current situation cost (lost time, duplicate mailings, contacting `neObvescaj` people)? (Q-01) | customer | review | OPEN |
| OQ-P02 | Which measurable success criteria does the customer accept? The four above are a developer proposal. (Q-06) | customer | review | OPEN |
| OQ-P03 | What does the SCM example solution (https://github.com/anzo26/SCM) do? The brief asks to match or beat it, but its features and screens are not known to this specification. Which of its screens does the customer like? (Q-05, Q-53) | customer | review | Developer, 2026-11-08 review: proceed without it; stays open for the customer. |
| OQ-P04 | Do the quality numbers hold: up to 100 users, 20 at once, Groups up to 10,000 Contacts, speeds, availability 99 %, backups? (Q-50, Q-35) | customer | review | OPEN |
| OQ-P05 | OPEN — needs customer (personal data). Who is the controller or processor, must data stay in the EU, which countries do the Contacts live in, are special categories of data (health, religion …) stored? The developer put legal matters outside the software. (Q-46) | customer | review | OPEN |
| OQ-P06 | Does a production Firebase project exist and who owns it? (Q-49) | customer | review | OPEN |
| OQ-P07 | Do the customer's time zone, languages and formats match Q-44 (Europe/Ljubljana, Slovene and English)? Which countries do users and Contacts live in? | customer | review | OPEN |
| OQ-P08 | Brand colour, logo, product name (working title "Contact Groups"). (Q-53) | customer | later | OPEN |
| OQ-P09 | Is Excel import wanted later? (Q-22) | customer | later | OPEN |
| OQ-P10 | Are the out-of-scope list and the acceptance checks (Q-45, Q-55) right? | customer | review | OPEN |

## Project Definition of Done

Project additions to the release Definition of Done (`general/quality/done-release.md`), checked in phase 6. Source: Q-55 (customer to confirm).

| ID | Criterion | How it is checked |
|---|---|---|
| DoD-P01 | Every `must` and MVP story works in the deployed Docker Compose setup, and the end-to-end scenarios E2E-01, E2E-02, E2E-03, E2E-04 and E2E-06 pass. | The five scenarios run against the Compose stack; the verification report lists them. |
| DoD-P02 | The isolation test passes on every endpoint with zero exceptions. | The isolation test of AC-018-04 and E2E-03 runs for each endpoint listed in the OpenAPI description. |
| DoD-P03 | The numbers of NFR-01 to NFR-05 are met on the deployed system, or the customer accepted others. | A runtime demonstration with a Group of 10,000 Contacts, recorded in the verification report. |
| DoD-P04 | The customer receives the container images, the install and backup guide, the OpenAPI description and a short user guide in Slovene. | The release checklist lists the four deliverables with their paths. |
| DoD-P05 | The later features (Versions, duplicate search, merge) are finished in the second increment and E2E-05 passes then. | E2E-05 runs against the Compose stack after the second increment. |
| DoD-P06 | No Critical or High finding is open. | The findings list in the verification report. |
| DoD-P07 | A database restore from a backup has been tested once before release (Q-50). | The restore test is described in the install and backup guide and recorded in the verification report. |
