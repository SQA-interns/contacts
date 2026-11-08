# Interview

> For: agent (reads), human (reviews) · Owner: Product owner · Written by: `/grill-me` · Agent: writes in intake only

Notes of the requirements interview. IDs never change once given. Source: `customer: <file>` (from `customer_requirement/`), `customer` (an answer the customer gave the developer) or `developer`. Status: `answered`, `deferred` (with reason) or `open`.

Language of the notes: English. Customer terms are kept in Slovene in quotes where the term matters.

## 1. Problem

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-01 | What hurts today, for whom, how do they cope now, what does it cost? | Mixed: some customers manage contacts in Excel sheets and e-mail lists, others use a competing CRM that is too complex or expensive. Cost (lost time, duplicate mailings, contacting people tagged `neObvescaj`) is not known. | customer | answered (cost unknown, see Open items) |
| Q-02 | Why a new product instead of an existing CRM? | Competing CRM systems are implicitly harder to use; the product's "konkurenčna prednost" (competitive advantage) is ease of use ("enostavnost uporabe"). Developer moved on without objecting to the topic 1 summary. | customer: customer-requirements.md | answered |

## 2. Product

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-03 | What do we build? | A multi-tenant ("večnajemniški") system for managing contact data of a project or a company (only the name is needed for the project or company). | customer: customer-requirements.md | answered |
| Q-04 | What does the delivery cover? | "Načrtovati, nadgraditi, implementirati in namestiti": design, upgrade, implement and deploy. | customer: customer-requirements.md | answered |
| Q-05 | What is the reference solution https://github.com/anzo26/SCM: only an example, or the base we "upgrade" (nadgraditi)? | Benchmark: its feature set is the minimum to match or beat; we write our own code and do not reuse or fork it. Brief calls it "Primer že izdelane rešitve". What SCM does and does not do is not yet known to the interview. | customer | answered |
| Q-06 | What does success look like (measurable)? | Customer set no measurable criteria. Proposal: (1) a new user signs in, creates a group and adds a first contact in under 2 minutes without help; (2) in a group of about 1,000 contacts, search and filter results appear in under 1 second; (3) selected contacts export to an `.xlsx` that opens in Excel with all columns filled; (4) a user never sees another group's contacts (tested, zero tolerance). | developer | open — needs customer |

## 3. Users and roles

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-07 | Who creates contact groups? | Users create groups of contacts themselves. | customer: customer-requirements.md | answered |
| Q-08 | Which roles exist and what may each do (group owner, member, read-only, system administrator)? | Brief names only "users"; authorisation is "lastna" (own, built by us). Chosen model, three roles per group: **Owner** (the creator; everything, including inviting and removing members, changing roles, deleting the group); **Editor** (create, edit, tag, merge and export contacts; no member management, cannot delete the group); **Viewer** (search, filter and view only; export not yet decided, see Q-26). | developer | answered (rights topic; customer to confirm) |
| Q-27 | Is there a system administrator who sees across groups? | No in-app administrator. Nobody sees or manages a group they are not a member of; operators work only with infrastructure and database. | developer | answered (rights topic; customer to confirm) |
| Q-26 | May a Viewer export contacts to Excel? | No. Only Owner and Editor may export. An Owner who wants someone to export makes them an Editor. | developer | answered (rights topic; customer to confirm) |
| Q-25 | How does a second person get into a group? | Invite by e-mail: the Owner enters an e-mail address and a role; the invited person sees the group after signing in through Firebase with that address (registering first if needed). No invite links, no admin-only adding. | developer | answered (rights topic; customer to confirm) |

## 4. Features (user goals, priority, MVP)

All are named by the customer; priority and MVP are not stated for any of them.

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-09 | Contact groups | Users create a group of contacts for a project or company; contacts are visible only inside their group. | customer: customer-requirements.md; priority and MVP: developer | answered — must, MVP |
| Q-10 | Predefined contact fields | Predefined to a degree: name, surname, address, country, email, company "ipd." (etc.). Full list not stated by the customer; fixed in Q-29. | customer: customer-requirements.md; priority and MVP: developer | answered (list in Q-29) — must, MVP |
| Q-11 | Custom attributes | Free key-value definition per contact, example `kompetenca=spletnoProgramiranje`. | customer: customer-requirements.md; priority and MVP: developer | answered — should, MVP |
| Q-12 | Tags | Free tags on contacts, examples `neObvescaj`, `udelezenec2025`; they act as contact categories. | customer: customer-requirements.md; priority and MVP: developer | answered — must, MVP |
| Q-13 | Audit trail ("revizijska sled") | All changes to contacts are tracked. | customer: customer-requirements.md; priority and MVP: developer | answered — should, MVP |
| Q-14 | Versioning | Contact changes are "primerno verzionirati" (versioned appropriately). Meaning fixed in Q-40. | customer: customer-requirements.md; priority and MVP: developer | answered (Q-40) — should, later |
| Q-15 | Duplicate search | Find duplicate contacts. Matching rule fixed in Q-41. | customer: customer-requirements.md; priority and MVP: developer | answered (Q-41) — should, later |
| Q-16 | Merge | Merge duplicates ("združevanje ipd."). Rules fixed in Q-42. | customer: customer-requirements.md; priority and MVP: developer | answered (Q-42) — should, later |
| Q-17 | Overview, search, filtering | Friendly overview, search and filtering of contacts. | customer: customer-requirements.md; priority and MVP: developer | answered — must, MVP |
| Q-18 | Excel export | Export of selected ("označenih") contacts to MS Excel format. | customer: customer-requirements.md; priority and MVP: developer | answered — must, MVP |
| Q-19 | REST interface | Backend is accessed through a suitably secured REST interface. | customer: customer-requirements.md; priority and MVP: developer | answered — must, MVP |

## 5. Main flows, rules, exceptions, examples

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-31 | What are tags and how do they behave? | Tags belong to one group and are invisible to others. Free text, 1 to 50 characters, letters, digits, `-` and `_`, no spaces, compared ignoring case. A contact carries up to 50 tags; a tag may be on many contacts. Typing a new tag creates it (no separate creation screen). Rename changes it on all contacts; delete removes it from all contacts but deletes no contact; both are Owner and Editor actions. Filtering supports "has all of these tags" and "has any of these tags". Tag `neObvescaj` has a special meaning: the developer says it "guarantees that the group with that tag doesn't send notifications" (resolved in Q-32). | developer; neObvescaj meaning: customer (relayed by developer) | answered |
| Q-42 | How does merging duplicates work? | Owner and Editor, within one group only. The user picks two or more contacts from a duplicate set; a side-by-side screen shows them field by field, and for each field the user chooses the value to keep (default: first non-empty value of the oldest contact). Tags are combined. Attributes are combined; if the same key has different values the user chooses. The user must confirm. The oldest contact survives with the chosen values (so its history stays); the others go to Trash (Q-38) and can be restored for 30 days, which undoes the merge. One audit entry lists the merged contacts and chosen values; the merged contacts' history is linked to the survivor. Not allowed: merging contacts of different groups, merging a contact with itself. Automatic merge without choice is not offered. | developer | answered (destructive topic; customer to confirm) |
| Q-41 | How are duplicate contacts found? | Within one group only, never across groups. Certain duplicate: same e-mail, ignoring case. Probable duplicate: same first and last name (ignoring case and accents) plus the same company or the same phone number. Contacts without e-mail, name and company are never matched. Result: a list of duplicate sets (for example "3 contacts share ana@acme.com"), each marked certain or probable. Finding only reports and never changes data. The user can start a merge (Q-42) or mark a set as "not a duplicate", after which it is not shown again. Fuzzy matching is a later improvement. | developer | answered (no customer example of a duplicate) |
| Q-40 | What does "versioning" of contacts mean? | Each save creates a new numbered version of the contact (v1, v2, v3…). A user (Owner or Editor) can open an earlier version, compare it with the current one and restore it; a restore creates a new version, never rewrites history. The audit trail (Q-37) says who changed what; versions let the user go back. Priority "should, later" (Q-14); restore in the first version was not asked for. | developer | answered |
| Q-38 | What is the contact lifecycle: delete, restore, concurrent edits? | Delete moves a contact to the group's Trash (hidden from list, search and export); Owner and Editor may delete. Restore from Trash within 30 days by Owner and Editor; after 30 days the system deletes it permanently and removes its personal data. "Delete forever" before 30 days: Owner only, after confirmation. Deleting a group: Owner only, the group name must be typed to confirm, the group goes to its own Trash for 30 days, then is deleted permanently. Concurrent edits: if two Editors edit the same contact, the second save is refused with "this contact was changed by someone else" and the new version is shown; nothing is silently overwritten. The 30 days is the developer's proposal; no legal retention period was given. | developer | answered (deletion topic; 30 days and legal retention need customer) |
| Q-37 | What does the audit trail record and who sees it? | Logged: create, edit and delete of a contact; adding or removing tags and attributes; merge; exports (Q-36); member and role changes in the group. Each entry: time, user (name and e-mail), action, contact, and for edits the old and new value of each changed field. Time stored in UTC, shown in the user's local time (topic 7 to confirm). Entries are append-only: no user, including the Owner, can edit or delete them. Seen by Owner and Editor, not by Viewer. Shown as a "History" tab on each contact and a group-wide activity list filterable by user, action and date. Lives as long as the group; handling of personal data after a contact is deleted is asked in topic 9. | developer | answered (retention of deleted contacts open, topic 9) |
| Q-36 | How does the Excel export work? | Exports only the ticked contacts; with nothing ticked the Export button is disabled ("select all N results" covers exporting everything). File `.xlsx`, one sheet, header row, one row per contact. Columns: the 11 predefined fields (Q-29), `Tags` (comma-separated), then one column per attribute key used in the selection. File name `<group name>_<YYYY-MM-DD>.xlsx`. At most 10,000 contacts per export; above that the user must narrow the selection. Cell values starting with `=`, `+`, `-` or `@` are written as plain text (formula injection). Every export is logged in the audit trail (who, when, how many contacts). Only Owner and Editor may export (Q-26). Import from Excel is not in the MVP. | developer | answered |
| Q-35 | How do search and filtering work in the contact list? | One search box: partial-word match, ignoring case and accents (`Skerlj` finds `Škerlj`), over first name, last name, e-mail, company, city, tag names and attribute values. Filters combined with AND: tag (all or any), country, company, attribute key equals value; filters and search work together. List: 25 per page, default sort by last name, sortable columns, a checkbox per row. Selection: "select all on this page" and "select all N results of the current filter"; it survives paging and is cleared when the filters change. Empty result shows "No contacts match" and a "Clear filters" button. Performance target: results in under 1 second for a group of up to 10,000 contacts (developer proposal; customer gave no group size, see Open items). | developer | answered (group size and speed open) |
| Q-34 | How do custom key-value attributes work? | Per contact, up to 50 pairs. Key 1 to 50 characters, value up to 255; both text (typed values out of scope for the MVP). A contact cannot have the same key twice, ignoring case. The form suggests keys already used in the group (autocomplete) but never forces them. Filters: "key equals value" and "key exists". Attributes are part of the audit trail and of Excel export (one column per key used in the selection). | developer | answered |
| Q-32 | Does the product send notifications, and what does `neObvescaj` guarantee? | The product sends no notifications to contacts (no mailing feature). `neObvescaj` is a marker that tells other tools using the REST API or Excel export not to notify the contact. The product's duty: make the tag visible, filterable, and let users exclude it (a "without tag" filter) when selecting contacts for export. Sending notifications is out of scope. Option 1 of the question; the developer's earlier wording ("doesn't send notifications") is read as "others must not notify". | developer | answered (consent-related; customer to confirm) |
| Q-28 | What does creating a contact group need, and what are the name rules? | A group has a name and nothing else (brief: "samo naziv je dovolj"). The name is required, trimmed of leading and trailing spaces, 1 to 100 characters. Names are unique per Owner, ignoring case (one Owner cannot have "Acme" and "ACME"; different Owners may each have "Acme"). No limit on the number of groups per user in the MVP. | developer | answered |

## 6. Shared rules and data

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-43 | What is the one name for what a user creates to hold contacts, and the related terms? | **Group** (Slovene: skupina; code `ContactGroup`). Not to use: project, company, tenant, organisation, workspace (a project or company is only what the user names the group). Related: **Contact** (kontakt) a person or organisation record inside a group; **Member** a user with a role in a group; **Tag** (značka); **Attribute** (atribut, a key-value pair); **Trash** (koš) for deleted items; **History** (zgodovina) for the audit trail; roles **Owner**, **Editor**, **Viewer**. | developer | answered |
| Q-30 | What must a contact have to be saved, and what is validated? | At least one of first name, last name, e-mail or company is required; a contact with all four empty is rejected and nothing is stored. Nothing else is required. E-mail, when present, must have a valid format; one e-mail and one phone per contact; e-mail stored in lowercase. Text fields at most 255 characters, notes at most 5,000. Country is chosen from a standard country list, not typed freely. | developer | answered (customer to confirm; multiple e-mails or phones not asked for) |
| Q-29 | Which predefined fields does a contact have? | 11 fields: first name, last name, e-mail, phone, company, job title, street and house number, postal code, city, country, notes (free text). Phone, job title and notes are not in the brief (it ends with "ipd."). Address is split into four parts so it can be filtered by city or country. | developer | answered (customer to confirm the additions) |

## 7. Project-wide facts

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-44 | Time zone, languages, formats, currency, tax, encoding? | Time: stored in UTC, shown in Europe/Ljubljana (CET/CEST) for every user (also settles the display of Q-37). Languages: UI in Slovene (default) and English with a switch in the header; API field names in English. Formats: Slovene `d. M. yyyy`, `HH:mm` (24 hours), decimal comma; English `yyyy-MM-dd`. Currency and tax: not applicable (no prices, payments or invoices). Encoding: UTF-8 everywhere including API and Excel export. Phone numbers stored as typed (character and length check), not forced into one format. The countries of users and contacts were not given by the customer. | developer | answered (customer to confirm) |

## 8. Out of scope, non-goals, principles

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-45 | What is out of scope and which principles must never break? | Out of scope (the agent must not add): sending e-mail or any notification to contacts (Q-32, Q-33); import from Excel or CSV, export to CSV (Q-36); API keys for other tools (Q-39); typed attributes (numbers, dates), saved filters, fuzzy duplicate matching; usual CRM activity features (tasks, deals, notes timeline, calendar, pipelines); real-time collaboration, mobile app, offline use; payments, billing, subscription plans; contact data from external sources (social networks, address books). Principles: (1) a user never sees, changes or exports a group they are not a member of; (2) no data is lost silently (Trash, refused concurrent edits, undoable merge); (3) the audit trail is complete and nobody can edit it; (4) the product never contacts a person on its own; (5) simple wins: no feature may add a step to the common flows (create a contact, search, export) if fewer steps give the same result. | developer | answered (customer to confirm) |
| Q-33 | Does the product send notifications or e-mails to contacts? | No. Sending is out of scope (Q-32). | developer | answered (customer to confirm) |

## 9. Personal data

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-46 | Who is responsible for the contact data and what legal constraints apply? | Developer chose option 3: legal matters (controller or processor role, data-processing agreement, privacy notice, consent) are handled by the customer outside the software. The product adds no legal features beyond what is already specified: isolation (Q-39, Q-45), History (Q-37), Trash and permanent deletion after 30 days (Q-38). Not answered: countries of the contacts, whether data must stay in the EU, whether special categories of data (health, religion and so on) are stored. The interviewer advised against recording this without a legal answer, because it is a personal-data topic. | developer | open — needs customer |

## 10. Security

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-20 | Isolation of data | Data of different users and their projects must be isolated. | customer: customer-requirements.md | answered |
| Q-48 | How are abuse and attacks handled beyond sign-in? | Every endpoint is tested with a user from another group and must return 404 and no data. All database access is parameterised; text shown in the UI is escaped (a contact named `<script>…` appears as plain text). Excel formula injection blocked (Q-36). Invitations: at most 20 pending per group, expire after 14 days, the Owner can cancel them. The last Owner of a group cannot be removed or demoted; a group always has an Owner. The server logs failed sign-ins, 401/403/404 spikes and rate-limit hits, never contact data or tokens. Firebase and database credentials live only in environment variables, never in code or the repository. No extra lock after failed sign-ins beyond Firebase's own protection. | developer | answered |
| Q-47 | How do users sign in with Firebase and what is required? | Methods: e-mail with password, and "Sign in with Google"; no other providers, no multi-factor authentication in the MVP. A verified e-mail is required before any group can be opened (invitations match on e-mail, Q-25). Passwords: Firebase defaults, at least 8 characters; reset through Firebase's e-mail flow. Sessions: the front end keeps the Firebase session; the server checks the token on every request (Q-39). Account deletion: a user may delete their own account; groups where they are the only Owner must first be handed to another member or deleted; their name stays in the History. Company single sign-on later: not asked of the customer. | developer | answered (customer to confirm) |
| Q-39 | What does the secured REST API promise? | Every endpoint except the health check requires a valid Firebase ID token (`Authorization: Bearer`); missing or invalid gives 401 and no data. Authorisation is checked on every request: a non-member gets 404 (existence of other groups not revealed), a member with too low a role gets 403. The group is always in the request path (`/groups/{id}/contacts/…`) and the server filters by it; a group is never taken from the request body. JSON with a published OpenAPI description; the same API serves the UI and is the only way to the data. One error format with a machine-readable code, no stack traces. Rate limit per user (about 100 requests per minute), 1 MB maximum request size, HTTPS only, CORS limited to our own front-end origin. API keys for other tools are not in the MVP (later story if wanted). | developer | answered (customer to confirm; other tools calling the API not known) |
| Q-21 | Authentication and authorisation | Authentication: Firebase. Authorisation: own (built in the product). | customer: customer-requirements.md | answered |

## 11. Integrations and external services

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-49 | Which Firebase project is used for development and tests, and who holds the credentials? | Automated tests use the Firebase Local Emulator Suite (no internet, no real accounts). One real development Firebase project, created by the developer, for manual checks. Credentials live only in the developer's `.env`, never in the repository or in chat. The production Firebase project: whether it exists and who owns it was not answered. | developer | answered (production project open) |
| Q-22 | Which integrations? | Firebase (authentication); Excel (export, called "Integracija: Excel"). Whether Excel also means import is not stated; import is not in the MVP (Q-36). | customer: customer-requirements.md; MVP decision: developer | open (import later?) |

## 12. Quality

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-50 | Load, speed, availability, backups, devices, accessibility? | Load: up to 100 registered users, 20 at the same time, groups of up to 10,000 contacts. Speed (measured on the deployed system with normal data): list, search and filter under 1 second; opening a contact under 500 ms; export of 1,000 contacts under 10 seconds. Availability: 99% per month, no on-call duty, planned maintenance announced. Backups: database backed up daily, kept 14 days, a restore tested once before release. Browsers: current and previous version of Chrome, Firefox, Safari, Edge; responsive UI down to a narrow phone window, no native app. Accessibility: WCAG 2.1 level AA for the main flows (sign in, list, create or edit a contact, export). Ease of use: a new user needs no manual; every screen has a clear empty state and a clear error message. | developer | answered (customer gave no numbers; to confirm) |
| Q-23 | Quality emphasis | Ease of use is the stated differentiator. No numbers from the customer; developer proposals are in Q-50 and Q-44. | customer: customer-requirements.md; numbers: developer | answered (Q-50, Q-44; customer to confirm) |

## 13. Technology

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-51 | Which versions and supporting tools? | Backend: Java 21 (LTS), Spring Boot 3.x current stable, Maven. Frontend: React with TypeScript, built with Vite. Database: PostgreSQL 16 or newer, schema changes through Flyway migrations. Excel: Apache POI on the server. Tests: JUnit and Testcontainers (backend), Vitest and Playwright (frontend). Licences: permissive open source only (Apache, MIT, BSD), no copyleft code in the product. Forbidden: reusing code from the SCM example (Q-05). Exact versions are checked on the host by a later session. | developer | answered (developer proposal) |
| Q-24 | Required stack | Backend: Spring Boot. Frontend: React (the brief says "Resct", read as a typo for React). Data: PostgreSQL. Integration: Excel. Authentication: Firebase. Authorisation: own. | customer: customer-requirements.md | answered |

## 14. Environments and deployment

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-52 | Where does it run, who operates it, how is it deployed? | Development: the team's internal server or a local Docker setup. Production: the customer decides and operates it (cloud or own infrastructure); the product must therefore be deployable in either. Deliverable: container images (backend, frontend), a Docker Compose setup with PostgreSQL, an install and backup guide; all configuration through environment variables, nothing tied to one provider. Three environments: development (local), test (automated tests with the Firebase emulator and a throwaway database), production (customer). Nobody on call. Budget, domain and the production host are the customer's concern and not known. | developer | answered |

## 15. Design constraints

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-53 | Brand, tone, devices, reference products? | The customer gave none. First version uses the neutral default: calm style with one accent colour, product name as text, working title "Contact Groups" (Slovene "Skupine kontaktov"), friendly short wording, light theme with dark mode following the system, desktop first and responsive down to a phone. Brand colours, logo and name come later from the customer's feedback; the design must therefore keep colours, name and logo in one place (tokens) so they can be replaced. Which SCM screens the customer likes is unknown. | developer | answered (customer feedback expected later) |

## 16. End-to-end journeys

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-54 | Which journeys must work end to end? | (1) First use: register with e-mail, verify, sign in, create group "Acme", add a contact with tag `udelezenec2025` and attribute `kompetenca=spletnoProgramiranje`, find it with the search box. (2) Team work: Owner invites a colleague as Editor, the colleague edits a contact, the Owner sees the change in the History with old and new values. (3) Isolation: user A creates a group with contacts; user B (not a member) requests its contacts through the API and gets 404 and no data, and the group is absent from B's screen. (4) Export: an Editor filters by tag `udelezenec2025`, excludes `neObvescaj`, selects all results, exports to Excel; the file has the right rows, columns and tags; the export is in the History. (5) Clean-up (later features): two contacts with the same e-mail are found, merged field by field, the survivor shows the merged data, restoring the other from Trash undoes the merge. (6) Mistake recovery: an Editor deletes a contact, finds it in Trash, restores it; the History shows both steps. Journeys 5 depends on later features (Q-15, Q-16). | developer | answered |

## 17. Project done

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-55 | What must the customer see to accept the product? | (1) All must and MVP features work in the deployed Compose setup (Q-52) and journeys 1, 2, 3, 4 and 6 of Q-54 pass. (2) The isolation test passes on every endpoint with zero exceptions (Q-48). (3) The numbers of Q-50 are met on the deployed system, or the customer accepted others. (4) The customer receives the container images, the install and backup guide, the OpenAPI description and a short user guide in Slovene. (5) The later features (versioning, duplicate search, merge) are finished in a second increment and journey 5 passes then. (6) No open Critical or High finding. | developer | answered (customer to confirm) |

## Contradictions resolved

| IDs | Contradiction | Resolution |
|---|---|---|

## Open items

| ID | Why open | Who must answer |
|---|---|---|
| Q-01 | Cost of the current situation is unknown. | customer |
| Q-06 | No measurable success criteria from the customer; the four checks are a developer proposal. | customer |
| Q-53 | Brand colours, logo, product name and liked SCM screens: expected from the customer later. | customer |
| Q-50 | All quality numbers (users, load, speed, availability, backups) are the developer's proposal; the customer gave no numbers or deadline. | customer |
| Q-49 | Production Firebase project: does it exist and who owns it? | customer |
| Q-46 | Personal data: legal roles, EU data residency, countries of contacts and special categories are unanswered; the developer put legal matters outside the software. | customer |
| Q-44 | Time zone, UI languages and the countries of users and contacts were not given by the customer; the values are the developer's proposal. | customer |
| Q-42 | The merge rules (field-by-field choice, undo through Trash) are the developer's choice and concern destructive operations. | customer |
| Q-38 | The 30-day Trash period and any legal retention or erasure deadline are not known. | customer |
| Q-35 | Typical group size and speed target are not known (proposal: 10,000 contacts, results under 1 second). | customer |
| Q-08 | The role model (Owner, Editor, Viewer) is not in the brief; chosen by the developer, concerns rights and access. | customer |
| Q-25 | Invite-by-e-mail membership is not in the brief; chosen by the developer, concerns rights and access. | customer |
| Q-26, Q-27 | Viewer cannot export and no in-app administrator: chosen by the developer, concern rights and access. | customer |
| Q-22 | Whether the customer also wants Excel import (not in the MVP, Q-36). | customer |
| Q-29, Q-30 | Fields added beyond the brief (phone, job title, notes) and one e-mail and one phone per contact. | customer |
| Q-32, Q-33 | `neObvescaj` read as a marker for other tools; no notifications sent by the product (consent-related). | customer |
| Q-39 | Whether other tools will call the API directly (decides if API keys are wanted early). | customer |
| Q-45, Q-55 | Out-of-scope list and acceptance checks are the developer's proposal. | customer |
| Q-47 | Sign-in methods (e-mail and Google) and whether company single sign-on is wanted later. | customer |
