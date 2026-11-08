# Business rules

> For: agent (reads), human (reviews) · Owner: Product owner · Agent: read-only

Project-wide facts, rules shared by more than one story, the data model and the glossary. A rule that belongs to one story lives in that story. Format: `general/requirements-standard.md`.

## Project-wide facts

Facts that hold for the whole project, written once. Stories, rules and tests use these values and never repeat or override them.

| Fact | Value | Source |
|---|---|---|
| Time zone | Stored in UTC; shown in Europe/Ljubljana (CET/CEST) to every user. Calendar days (date filters, the date in an export file name) are days in Europe/Ljubljana. | Q-44 (customer to confirm); calendar days: [ASSUMPTION] same zone as the display, because no other zone exists |
| Currency | Not applicable (no prices, payments or invoices). | Q-44 |
| Tax (included in prices or not, rate) | Not applicable. | Q-44 |
| Date and time format | Slovene: date `d. M. yyyy` (for example `8. 11. 2026`), time `HH:mm` (24 hours). English: date `yyyy-MM-dd` (for example `2026-11-08`), time `HH:mm` [ASSUMPTION] (the interview gives no English time format; 24 hours like Slovene). | Q-44 |
| Number format | Slovene: decimal comma. English: decimal point [ASSUMPTION] (the interview gives only the Slovene rule). | Q-44 |
| Languages of the product | The UI is in Slovene (default) and English, with a switch in the header. API field names are English. | Q-44 |
| Text encoding | UTF-8 everywhere, including the API and the Excel export. | Q-44 |
| Units of measure | Not applicable. | Q-44 |
| Other: countries | A country is chosen from the ISO 3166-1 list of countries [ASSUMPTION] ("a standard country list" is all the interview says); stored as the two-letter code, shown by name in the active UI language. The countries of users and contacts are not known. | Q-30, Q-44 |
| Other: phone numbers | Stored as typed; checked for allowed characters and length only (BR-06). | Q-44 |
| Other: Trash period | 30 days (BR-04). | Q-38 |
| Other: list page size | 25 contacts per page. | Q-35 |
| Other: export limit | At most 10,000 contacts per export. | Q-36 |

## Shared rules

| ID | Rule (EARS) | Stories | Source |
|---|---|---|---|
| BR-01 | The system shall let a user read or change data of a Group only when the user is a Member of that Group; a request from a non-Member shall be answered as if the Group did not exist ("not found"), and a request from a Member whose Role is not allowed to do the action (BR-02) shall be refused as "forbidden". | US-002 to US-026 | Q-39, Q-45, Q-48 |
| BR-02 | The system shall allow each action only to the Roles marked for it in the permission matrix below. | US-002 to US-026 | Q-08, Q-26, Q-27, Q-37, Q-38 and the stories; Roles for actions the interview does not give are marked [ASSUMPTION] in the matrix |
| BR-03 | When an audited action (table below) is performed, the system shall append one History entry with the time (UTC), the acting user's name and e-mail, the action, the Contact (where the action concerns one) and, for an edit, the old and new value of every changed field; the system shall not allow any user, including the Owner, to change or delete a History entry. | US-004, US-005, US-006, US-011, US-012, US-013, US-014, US-015, US-016, US-017, US-023, US-026 | Q-37, Q-13 |
| BR-04 | While a Contact or Group is in Trash, the system shall hide it from lists, search, filters, export and duplicate search; while it has been in Trash for less than 30 days, the system shall let the permitted Role restore it; when it has been in Trash for 30 days, the system shall delete it permanently together with its personal data. | US-012, US-013, US-020, US-026 | Q-38 |
| BR-05 | Where the system compares texts, it shall compare tag names, attribute keys, Group names and e-mail addresses ignoring upper and lower case, and shall compare search text, names and company names for search and duplicate detection ignoring upper and lower case and accents (for example `Skerlj` equals `Škerlj`). | US-002, US-004, US-005, US-006, US-008, US-009, US-024 | Q-28, Q-31, Q-34, Q-35, Q-41 |
| BR-06 | When a Contact is saved, the system shall accept it only if at least one of first name, last name, e-mail and company is not empty after trimming spaces, e-mail (when present) has a valid format and is at most 255 characters, phone (when present) has only digits, spaces and `+ - ( ) . /` [ASSUMPTION] and at most 30 characters [ASSUMPTION], notes are at most 5,000 characters, every other text field is at most 255 characters, and country (when present) is on the country list; otherwise it shall store nothing. | US-003, US-023, US-026 | Q-30, Q-44 |
| BR-07 | When a Contact is saved (fields, tags or attributes changed through the Contact), the system shall raise its version number by 1 [ASSUMPTION: a change to tags or attributes counts as a save; renaming or deleting a Tag for the whole Group does not]; if the version number sent with the change is not the Contact's current one, then the system shall refuse the change with the message "This contact was changed by someone else", show the current Contact and store nothing. | US-003, US-004, US-005, US-023 | Q-38, Q-40 |

### Permission matrix (BR-02)

O = Owner, E = Editor, V = Viewer. Rows marked * are Roles chosen by `/to-spec` [ASSUMPTION] because the interview does not give them.

| Action | O | E | V |
|---|---|---|---|
| See the Group, its Contacts, Tags and Attributes; search, filter, sort | yes | yes | yes |
| Create or edit a Contact; add, change or remove its Attributes and Tags | yes | yes | no |
| Rename or delete a Tag of the Group | yes | yes | no |
| Delete a Contact (to Trash); see Trash*; restore from Trash | yes | yes | no |
| Delete a Contact forever | yes | no | no |
| Export Contacts | yes | yes | no |
| See History of a Contact and the Group activity list | yes | yes | no |
| See earlier Versions of a Contact*; restore a Version* | yes | yes | no |
| Find duplicates*; mark a Duplicate set as "not a duplicate"*; merge Contacts | yes | yes | no |
| Invite a person; cancel an Invitation; change a Member's Role; remove a Member | yes | no | no |
| See the list of Members* | yes | yes | yes |
| Delete the Group | yes | no | no |

### Audited actions (BR-03)

| Action | Recorded details | Story |
|---|---|---|
| Contact created | the new values | US-016 |
| Contact edited | old and new value of each changed field | US-016 |
| Contact deleted (to Trash) | the Contact | US-012 |
| Contact deleted forever* | the action and the time, no personal data of the Contact | US-013 |
| Contact restored from Trash | the Contact (Q-54 journey 6 needs it; Q-37 does not list it) | US-012 |
| Tag added to or removed from a Contact | Contact, Tag | US-005 |
| Tag renamed or deleted for the Group* | one entry per affected Contact, old and new name | US-006 |
| Attribute added to, changed on or removed from a Contact | Contact, key, old and new value | US-004 |
| Contacts merged | the merged Contacts and the chosen values | US-026 |
| Contacts exported | number of Contacts | US-011 |
| Member joined, Role changed, Member removed | the Member and Roles | US-014, US-015 |
| Invitation created or cancelled* | e-mail and Role | US-014 |
| Contact version restored* | Contact, restored version number | US-023 |

## Data model

Types: `uuid`, `text(n)` = text of at most n characters, `timestamp` = UTC date and time. Every table has an internal `uuid` primary key (`id`) not repeated below.

| Entity | Field | Type and format | Required | Constraints | Personal data |
|---|---|---|---|---|---|
| User | firebaseUid | text(128) | yes | unique | yes (identifier) |
| User | email | text(255), lowercase | yes | valid e-mail; unique | yes |
| User | displayName | text(100) | yes | 1 to 100 characters | yes |
| User | createdAt | timestamp | yes | | no |
| Group | name | text(100), trimmed | yes | 1 to 100 characters; unique per Owner ignoring case among Groups not in Trash (US-002) | no |
| Group | createdAt | timestamp | yes | | no |
| Group | deletedAt | timestamp | no | set when the Group is in Trash | no |
| Member | group, user | references | yes | unique pair (group, user) | no |
| Member | role | `OWNER`, `EDITOR`, `VIEWER` | yes | every Group has at least one Owner | no |
| Invitation | group | reference | yes | | no |
| Invitation | email | text(255), lowercase | yes | valid e-mail | yes |
| Invitation | role | `OWNER`, `EDITOR`, `VIEWER` | yes | | no |
| Invitation | createdAt, expiresAt | timestamp | yes | expiresAt = createdAt + 14 days | no |
| Invitation | status | `PENDING`, `ACCEPTED`, `CANCELLED`, `EXPIRED` | yes | at most 20 `PENDING` per Group | no |
| Contact | group | reference | yes | never changes after creation | no |
| Contact | version | integer | yes | starts at 1; BR-07 | no |
| Contact | firstName, lastName, company, jobTitle, street, postalCode, city | text(255), trimmed | no | BR-06 (at least one of firstName, lastName, email, company) | yes |
| Contact | email | text(255), lowercase | no | valid e-mail when present | yes |
| Contact | phone | text(30) | no | BR-06 | yes |
| Contact | country | ISO 3166-1 alpha-2 code | no | on the country list | yes |
| Contact | notes | text(5000) | no | | yes |
| Contact | createdAt, updatedAt | timestamp | yes | | no |
| Contact | deletedAt | timestamp | no | set when the Contact is in Trash | no |
| Tag | group | reference | yes | | no |
| Tag | name | text(50) | yes | 1 to 50 characters: letters, digits, `-`, `_`; unique per Group ignoring case | no |
| ContactTag | contact, tag | references | yes | unique pair; at most 50 per Contact; Contact and Tag are in the same Group | no |
| Attribute | contact | reference | yes | at most 50 per Contact | no |
| Attribute | key | text(50) | yes | 1 to 50 characters; unique per Contact ignoring case | no |
| Attribute | value | text(255) | yes | 1 to 255 characters [ASSUMPTION: not empty] | yes (may contain personal data) |
| HistoryEntry | group | reference | yes | append-only | no |
| HistoryEntry | occurredAt | timestamp | yes | | no |
| HistoryEntry | userName, userEmail | text, copied at the time of the action | yes | the name stays when the User is deleted; the e-mail is removed (US-021) | yes |
| HistoryEntry | action | one of the audited actions | yes | | no |
| HistoryEntry | contact | reference | no | empty for actions that do not concern one Contact | no |
| HistoryEntry | details | structured text | no | old and new values, merged Contacts, export count | yes |
| ContactVersion | contact, number | reference, integer | yes | unique pair; the saved fields, Tags and Attributes of that version | yes |
| DuplicateDismissal | group, contacts | reference, set of references | yes | a Duplicate set marked "not a duplicate" | no |

### Lifecycles

- **Contact:** active → in Trash (delete) → active (restore, within 30 days) or permanently deleted (delete forever by the Owner, or 30 days in Trash). A Contact in Trash cannot be edited.
- **Group:** active → in Trash (delete by the Owner) → permanently deleted after 30 days. An Owner can restore a Group within 30 days (US-020).
- **Invitation:** `PENDING` → `ACCEPTED` (the invited person signs in with the verified e-mail), `CANCELLED` (by an Owner) or `EXPIRED` (14 days).
- **Member:** joins through an Invitation or by creating the Group; leaves when an Owner removes the Member or the User deletes the account.

## Glossary

The ubiquitous language of the project (`general/requirements-standard.md`): every requirement, the code, the API, the tests and the UI use these terms. Terms marked [ASSUMPTION] were added by `/to-spec`; the others are from Q-43.

| Term | Meaning | Name in code | Do not use |
|---|---|---|---|
| Group | What a user creates to hold Contacts; has only a name. A project or company is only what the user calls the Group. (Slovene: skupina) | `ContactGroup` | project, company, tenant, organisation, workspace |
| Contact | A person or organisation record inside one Group. (kontakt) | `Contact` | customer, lead, person, record |
| Member | A User who has a Role in a Group. | `Member` | participant, collaborator |
| Role | What a Member may do in a Group: Owner, Editor or Viewer (BR-02). | `Role` | permission level, access level |
| Owner | The Role that may do everything in a Group, including managing Members and deleting the Group. The creator starts as Owner. | `OWNER` | admin, manager |
| Editor | The Role that may create, edit, tag, delete, merge and export Contacts. | `EDITOR` | contributor, writer |
| Viewer | The Role that may only search, filter and view. | `VIEWER` | reader, guest, read-only user |
| Tag | A free label on Contacts of one Group; acts as a category. (značka) | `Tag` | label, category, group (for tags) |
| Attribute | A free key-value pair on one Contact. (atribut) | `Attribute` | custom field, property, metadata |
| Trash | Where a deleted Contact or Group stays for 30 days before permanent deletion. (koš) | `Trash`, `deletedAt` | recycle bin, archive |
| History | The audit trail of a Group: append-only entries of audited actions. (zgodovina) | `History`, `HistoryEntry` | audit log, activity log (except "Group activity list" for the Group-wide view) |
| User | A signed-in person with a verified e-mail. [ASSUMPTION] | `User` | account holder, customer |
| Invitation | An Owner's offer of a Role in a Group to an e-mail address. [ASSUMPTION] | `Invitation` | invite link, request |
| Version | A numbered state of a Contact (v1, v2, …); each save makes a new one. [ASSUMPTION: Q-40 describes it, Q-43 does not name it] | `ContactVersion` | revision, snapshot |
| Selection | The Contacts ticked in the contact list for export. [ASSUMPTION] | `selection` | basket, cart |
| Duplicate set | Two or more Contacts of one Group that match as duplicates, certain or probable. [ASSUMPTION] | `DuplicateSet` | cluster, conflict |
| Survivor | The Contact that remains after a merge (the oldest of the merged Contacts). [ASSUMPTION] | `survivor` | target, master |
| Group activity list | The Group-wide, filterable view of History. [ASSUMPTION] | `GroupActivity` | feed, timeline |
