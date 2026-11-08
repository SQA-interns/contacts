# Specification

> For: agent (writes), human (reads) · Written in: phase 2 (`/plan`) · Source: `project/01_requirements/*`, `project/02_design/*`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `general/security/*`, `general/design/ui-baseline.md`, `docs/02_adr/` · Agent: writes

Session S3 `/plan`. Choices are in `docs/02_adr/ADR-01..16`; interfaces in `docs/02_contracts/`. Readings of open points are those of the decisions D-18 to D-27 (cited where used). Paths: REST base `/api/v1`; `{g}` = Group id.

## Components and modules

Two components (architecture.md): `backend` (`02_output/backend`, Spring Boot, root package `si.contactgroups`) and `frontend` (`02_output/frontend`, React). The database has no code of its own: Flyway migrations in the backend (AR-04).

### Backend modules (ADR-01)

| Package | Class (examples) | Responsibility |
|---|---|---|
| `web` | `MeController`, `GroupController`, `MemberController`, `InvitationController`, `ContactController`, `TrashController`, `TagController`, `FacetController`, `HistoryController`, `ActivityController`, `ExportController`, `VersionController`, `DuplicateController`, `MergeController`, `ApiExceptionHandler`, `GroupAccessInterceptor` | Map HTTP to service calls; request/response records; no rule. |
| `service` | `UserService`, `GroupService`, `MembershipService`, `InvitationService`, `ContactService`, `ContactValidator`, `TagService`, `AttributeService`, `SearchService`, `TrashService`, `ExportService`, `HistoryWriter`, `HistoryQueryService`, `VersionService`, `DuplicateService`, `MergeService`, `AccountService` | Business rules, transactions, audit writing (BR-03). |
| `persistence` | `UserRepository` (`@GlobalScope`), `GroupRepository`, `MemberRepository`, `InvitationRepository`, `ContactRepository`, `TagRepository`, `AttributeRepository`, `HistoryRepository`, `VersionRepository`, `DismissalRepository` | `JdbcClient` SQL with named parameters; every Group-scoped method takes `groupId` first (AR-02). |
| `domain` | `Role`, `Action`, `Permission`, `Normaliser`, `CountryCodes`, `ContactFields`, `HistoryAction`, records | Pure rules and types, no framework. |
| `security` | `FirebaseTokenFilter`, `AuthenticatedUser`, `RequestSizeFilter`, `HttpsFilter`, `RateLimitFilter`, `SecurityLogger`, `@RequiresAction`, `@PublicEndpoint` | ADR-04, ADR-05, ADR-11. |
| `config` | `AppProperties` (typed environment), `FirebaseConfig`, `SecurityConfig`, `ClockConfig`, `OpenApiConfig` | Configuration only (AR-07). |
| `job` | `TrashPurgeJob` | ADR-09. |

### Frontend modules (ADR-12)

`app/` (router, providers, shell), `api/` (`client.ts`, one module per resource, `ApiError`), `auth/` (Firebase wrapper, `AuthProvider`, `RequireVerified`), `i18n/` (`index.ts`, `locales/sl.json`, `locales/en.json`, `format.ts`), `styles/` (`tokens.css`, `themes/wireframe.css`, `themes/final.css`, `brand.css`), `brand.ts`, `components/ui/` (shadcn), `components/` (`ResponsiveTable`, `PageHeader`, `EmptyState`, `ErrorState`, `ConfirmDialog`, `FormField`, `LanguageSwitch`), `features/<name>/`.

## Interfaces and contracts

Every interface of `architecture.md` has a contract in `docs/02_contracts/` or, where the interface belongs to a third party or to the schema itself, the artefact named below. `tools/validate-contracts.mjs` parses each contract (log in `out/logs/contract-validate-all.log`).

| Interface (architecture.md) | Contract | Check |
|---|---|---|
| Application interface, frontend to backend (REST, JSON) | `openapi.yaml` (37 operations, one shared error schema, `x-required-action` on every Group operation) | OpenAPI validator; project rules (operationId, security, 401/404 responses); contract-parity test in `/implement` (T-11) |
| Sign-in, frontend to Firebase Authentication | the Firebase client SDK (third-party protocol); the product's side is the token it receives, described by `firebase-id-token.schema.json` | `FirebaseEmulatorIT`; Playwright E2E against the emulator |
| Token check, backend to Firebase | `firebase-id-token.schema.json` (claims read, claims ignored) | valid and invalid examples validated; `FirebaseEmulatorIT` |
| Persistence, backend to PostgreSQL | the Flyway migrations `V1__create_application_role.java` and `V2__schema.sql` (the schema is the contract, AR-04) and the data model above | `DatabaseRolesIT` |
| Excel export, backend to the user's browser | `xlsx-export.schema.json` (sheet count, header, language, file name) | valid and invalid examples validated; `exceljs` read-back in the export tests |
| User interface | screen inventory below, wireframes of `/wireframes` | review, Playwright at three widths |
| Configuration (environment variables) | `configuration.schema.json` | valid and invalid examples validated |

## Domain model (glossary → code)

Glossary terms and "Name in code" are those of `business-rules.md`; no synonym is used (ES-13). The Selection exists only in the browser and in the export request.

| Term | Module | Type | Table | API name |
|---|---|---|---|---|
| Group | `domain`, `service.GroupService` | `ContactGroup` (record) | `contact_group` | `groups` |
| Group Trash (Trash of a Group, D-22) | `service.GroupService` | `ContactGroup.deletedAt` | `contact_group.deleted_at` | `group-trash` |
| Contact | `domain`, `service.ContactService` | `Contact` | `contact` | `contacts` |
| Member (Membership, D-22) | `service.MembershipService` | `Member` | `member` | `members` |
| Role, Owner, Editor, Viewer | `domain` | `Role` enum `OWNER`, `EDITOR`, `VIEWER` | `member.role`, `invitation.role` | `role` |
| Tag | `service.TagService` | `Tag` | `tag`, `contact_tag` | `tags` |
| Attribute | `service.AttributeService` | `Attribute` | `attribute` | `attributes` |
| Trash (of a Contact) | `service.TrashService` | `Contact.deletedAt` | `contact.deleted_at` | `trash/contacts` |
| History, History entry | `service.HistoryWriter` | `HistoryEntry` | `history_entry` | `history` |
| Group activity list | `service.HistoryQueryService` | `GroupActivity` (view of `HistoryEntry`) | `history_entry` | `activity` |
| User | `service.UserService` | `User` | `app_user` | `me` |
| Invitation (Invited person = User with a pending Invitation, D-22) | `service.InvitationService` | `Invitation` | `invitation` | `invitations` |
| Version | `service.VersionService` | `ContactVersion` | `contact_version` | `versions` |
| Selection | frontend `features/contacts/selection`, `service.ExportService` | `selection` | — | `exports` body `selection` |
| Duplicate set | `service.DuplicateService` | `DuplicateSet` (computed, not stored) | — | `duplicates` |
| "not a duplicate" | `service.DuplicateService` | `DuplicateDismissal` | `duplicate_dismissal`, `duplicate_dismissal_contact` | `duplicates/dismissals` |
| Survivor | `service.MergeService` | `survivor` | — | `merges` body `survivorId` |
| Version number (of a Contact) | `service.ContactService` | `Contact.version` (= number of its highest Version, D-22) | `contact.version` | `version` |

## Data model and validation

Types and limits are those of the data model in `business-rules.md`; this section adds the physical design. Every table has `id uuid primary key default gen_random_uuid()`. Times are `timestamptz` stored in UTC (AR-10). Column names are `snake_case` of the field names. Foreign keys are named in the migrations.

| Table | Columns (beyond `id`) | Keys and constraints | Notes |
|---|---|---|---|
| `app_user` | `firebase_uid text(128)`, `email text(255)` lowercase, `display_name text(100)`, `created_at` | unique `firebase_uid`; unique `email` (D-18d: a second identifier with the same e-mail is refused); `display_name` 1..100 | R-001-11 |
| `contact_group` | `name text(100)`, `created_at`, `deleted_at` null | `name` 1..100 trimmed; Owner uniqueness checked in the service while holding a row lock on the creator's `app_user` (R-002-03, R-020-08) | |
| `member` | `group_id`, `user_id`, `role`, `joined_at` | primary key (`group_id`, `user_id`); `role` in (`OWNER`,`EDITOR`,`VIEWER`); last Owner rule in the service in one transaction (SR-10) | `group_id` cascade |
| `invitation` | `group_id`, `email text(255)` lowercase, `role`, `status`, `created_at`, `expires_at`, `created_by` | `expires_at = created_at + 14 days`; at most 20 `PENDING` per Group and unique pending (`group_id`, `email`) checked in the service (R-014-03/04); `status` in (`PENDING`,`ACCEPTED`,`CANCELLED`,`EXPIRED`) | expiry evaluated on read and by the purge job |
| `contact` | `group_id`, `version int` (start 1), 7 text columns (`first_name`, `last_name`, `company`, `job_title`, `street`, `postal_code`, `city`) 255, `email` 255 lowercase, `phone` 30, `country char(2)`, `notes` 5000, `first_name_norm`, `last_name_norm`, `company_norm`, `email_norm`, `search_norm` (first name, last name, e-mail, company, city), `created_at`, `updated_at`, `created_by`, `deleted_at` null | index (`group_id`, `deleted_at`, `last_name_norm`, `first_name_norm`); `group_id` never updated (trigger-free: no SQL updates it) | BR-06 in `ContactValidator`; ADR-06 |
| `tag` | `group_id`, `name text(50)`, `name_norm` | unique (`group_id`, `lower(name)`); pattern letters (any alphabet), digits, `-`, `_` (D-21) | |
| `contact_tag` | `contact_id`, `tag_id` | primary key pair; at most 50 per Contact (service); Contact and Tag same Group (service + composite check by query) | |
| `attribute` | `contact_id`, `key text(50)`, `value text(255)`, `value_norm` | unique (`contact_id`, `lower(key)`); at most 50 per Contact (service); value not empty (D-20) | |
| `history_entry` | `group_id`, `occurred_at`, `user_id`, `user_name`, `user_email` null, `action`, `contact_id` null, `details jsonb` | ADR-03, ADR-08; indexes (`group_id`, `occurred_at desc`), (`contact_id`, `occurred_at desc`) | no foreign key on `user_id`, `contact_id` |
| `contact_version` | `contact_id`, `number`, `created_at`, `user_id`, `snapshot jsonb` | unique (`contact_id`, `number`); cascade with `contact` | ADR-07, D-19i |
| `duplicate_dismissal` | `group_id`, `contact_key text`, `created_at`, `created_by` | unique (`group_id`, `contact_key`); `contact_key` = SHA-256 of the sorted Contact ids | R-025-02 |
| `duplicate_dismissal_contact` | `dismissal_id`, `contact_id` | primary key pair | R-025-03 (a bigger set is new) |

**Validation (SB-01, BR-06, server side only authoritative):** `ContactValidator.validate(ContactInput)` trims every text, lowercases e-mail, then checks in this order and returns all field errors at once: at least one of first name, last name, e-mail, company non-empty; e-mail format (simple RFC-5322 subset, max 255); phone characters digits, space and `+ - ( ) . /`, max 30; notes max 5,000; other text max 255; country in `CountryCodes` (`Locale.getISOCountries()`); Attributes: key 1..50, value 1..255, keys unique ignoring case, at most 50; Tags: valid name, at most 50. Field error codes: `validation.required_any`, `validation.email_invalid`, `validation.too_long`, `validation.phone_invalid`, `validation.country_invalid`, `validation.attribute_*`, `validation.tag_*`. Group name: 1..100 after trim. Invitation e-mail: BR-06 e-mail rule (D-19h). Request bodies reject unknown properties (R-018-04).

## Behaviour per story

Each entry names the endpoints (contract `openapi.yaml`), the module that implements each rule, and the readings of decisions that apply. Permission checks always come from `GroupAccessInterceptor` (ADR-05) with the `Action` named; "service" means the class in the module table. Order of stories = increment order (`docs/02_tasks.md`).

### Increment 1

**US-001 Register and sign in** (frontend `auth/`, `features/auth`; backend `FirebaseTokenFilter`, `UserService`). Registration, sign-in, Google, reset, verification e-mail resend are Firebase client SDK calls from `auth/` (R-001-01..08); the form validates length 8 and display name 1..100 before the call; Firebase errors map to the single message `auth.invalid_credentials` (R-001-05), the already-registered case offers Sign in and Reset password (R-001-07), reset shows the same confirmation either way (R-001-08). After registration the display name is saved as the Firebase profile name. Session persistence is Firebase's (R-001-09); sign-out clears it (R-001-10). Backend: `GET /me` returns the User, creating it on the first valid token (R-001-11); a second Firebase identifier with an existing e-mail gets 409 `user.email_taken` and no User (D-18d). Unverified e-mail: the shell shows `RequireVerified` (R-001-04) and the backend answers 403 `auth.email_not_verified` on every Group endpoint (R-018-06). Expired or invalid token: 401, the client signs out and shows the sign-in page (R-001-12).

**US-002 Create a Group, see my Groups** (`GroupService`, `MembershipService`). `POST /groups {name}`: trim, 1..100 (R-002-02), refuse equal name ignoring case among the User's Owner Groups not in Trash with 409 `group.name_taken` (R-002-03; EX-002-06: Groups in Trash do not count), create Group and Owner Member in one transaction. `GET /groups` first accepts matching pending Invitations (US-014 R-014-05, ADR below), then lists the User's Groups with Role, excluding Groups in Trash (R-002-05, R-020-03). No limit on count (R-002-04); same name by different Owners allowed (R-002-07). Empty state in the UI (R-002-06).

**US-018 Secured REST interface** (`security`, `web`, `config`). See ADR-04, 05, 10, 11 and "Security controls". Order per request: size (413) -> HTTPS -> CORS -> token (401) -> rate limit (429) -> verified e-mail (403) -> Membership (404) -> Role (403). Group from the path only; objects loaded with the Group id (R-018-04/05). `GET /health` public, body `{"status":"UP"}` only (R-018-07). OpenAPI published at `GET /api/v1/openapi.json` (R-018-08, springdoc `-api`, no UI page, D-12); the published description must equal the contract for implemented operations (contract-parity test). JSON only (R-018-15). Text is stored raw and the frontend renders it as text (React escaping, no `dangerouslySetInnerHTML`, lint rule) (R-018-14). AC-018-04 isolation test iterates all operations of the contract that have `{groupId}`; at the end of each increment it covers the operations that exist, fully at release (D-23c).

**US-019 Language switch** (frontend `i18n/`). ADR-14. Switch in the header; texts and formats change at once without losing form input (R-019-02: forms keep state, only labels re-render); user texts untranslated (R-019-06); API error codes translated (R-019-07).

**US-003 Create, view, edit a Contact** (`ContactService`, `ContactValidator`). `POST /groups/{g}/contacts` (EDIT_CONTACT) validates (BR-06, R-003-03 all field errors, 422), trims and lowercases, writes `*_norm`, version 1, History `CONTACT_CREATED` and Version 1 (R-016-01, R-022-01). `GET /groups/{g}/contacts/{c}` (READ_CONTACTS) returns fields, Tags, Attributes, version, `deletedAt` (a Contact in Trash is returned to Owner/Editor only through the Trash endpoints; here 404). `PUT /groups/{g}/contacts/{c}` with `version`: ADR-07; stale version -> 409 `contact.changed_by_someone_else` with `current` (R-003-07); no change -> 200 with the unchanged Contact, nothing stored (R-003-11); duplicate e-mail allowed (R-003-10); Viewer 403 (R-003-09).

**US-016 History of a Contact** (`HistoryWriter`, `HistoryQueryService`). Entries per ADR-08 and D-19a. `GET /groups/{g}/contacts/{c}/history` (READ_HISTORY, Owner/Editor) newest first, time in UTC in the API and Europe/Ljubljana in the UI (R-016-03); Viewer 403 (R-016-04). No endpoint changes or deletes an entry; `PUT/PATCH/DELETE` on `/history/**` do not exist and answer 405 (R-016-05; also database level, ADR-03). History stays visible after a restore (R-016-08).

**US-004 Attributes** (`AttributeService`; part of the Contact save). The `PUT`/`POST` Contact body carries `attributes: [{key,value}]`; the service diffs by lowercase key against stored Attributes and writes one History entry per added/changed/removed Attribute (R-004-06, D-19a), validates R-004-02..04 (422 with `fieldErrors` per pair), treats it as a save (R-004-07). Suggestions from `GET /groups/{g}/facets` keys (R-004-05). Value stored as text (R-004-09). Viewer 403.

**US-005 Tags on a Contact** (`TagService`; part of the Contact save). Body `tags: [name]`; unknown names create the Tag in the Group, names equal ignoring case reuse the existing spelling (R-005-01/03); invalid name -> 422 (R-005-02); more than 50 -> 422 (R-005-04); removal removes the link only (R-005-06); History per Tag change (R-005-08); suggestions from facets (R-005-07); `neObvescaj` is an ordinary Tag, always shown (R-005-09).

**US-006 Rename and delete a Tag** (`TagService`). `GET /groups/{g}/tags` (action `MANAGE_TAGS`, Owner/Editor) with Contact counts; `PATCH /tags/{t} {name}` renames (validity, uniqueness ignoring case, case-only change allowed, R-006-02..04); `DELETE /tags/{t}` removes links on all Contacts incl. Trash, deletes the Tag, no Contact deleted (R-006-05; the confirmation count comes from the list). One History entry per affected Contact with old and new name, no version change (R-006-07, D-19b). Viewer 403.

**US-007 Browse the contact list** (`SearchService`). `GET /groups/{g}/contacts?page&sort&dir&...` returns `{items, page, pageSize:25, total}` of Contacts not in Trash; default sort last name asc using `last_name_norm`, empty last names last, ties by `first_name_norm` (R-007-02); other columns sort on their `_norm` where text; page beyond the end -> empty `items` with correct `total` (R-007-08). List columns (story definition): last name, first name, e-mail, company, city, country, Tags. Empty state offers "Add contact" to Owner/Editor (R-007-06).

**US-008 Search** (`SearchService`). Query `q`: normalise (ADR-06), split on whitespace, every word must occur in at least one searched field (story definition: first name, last name, e-mail, company, city, Tag names, Attribute values; not phone, street, postal code, country, notes or Attribute keys, R-008-02): `contact.search_norm` (first name, last name, e-mail, company, city) or an `EXISTS` over `tag.name_norm` / `attribute.value_norm`, each as escaped `LIKE '%word%'`; empty/space-only = no search (R-008-05); results page 1 and total (R-008-03); empty result text and Clear filters (R-008-04). Injection strings are bound parameters (R-008-06, SR-04).

**US-009 Filter** (`SearchService`, `FacetService`). Query params `tags`, `tagMode=all|any`, `withoutTags`, `country`, `company`, `attrKey`, `attrValue`, `attrExists`; combined with `q` by AND (R-009-08); company compares `company_norm`; Attribute key/value compare lowercase (R-009-06/07). `GET /groups/{g}/facets` returns Tags, companies, countries and Attribute keys occurring in the Group (R-009-11). Page 1 on change is a client rule (R-009-09).

**US-010 Select Contacts** (frontend `features/contacts/selection`). Selection is client state of ids; Viewer sees no checkboxes (R-010-02, role from `GET /groups` response); "select all N results" stores the filter, not ids (the export request then sends the filter; the backend recomputes the ids, so a Selection of 10,000 does not travel as ids); cleared on any search/filter change (R-010-08) and kept across page and sort changes (R-010-07).

**US-011 Export to Excel** (`ExportService`, Apache POI). `POST /groups/{g}/exports {selection:{contactIds[]}|{filter:{...}}, lang}` (EXPORT, Owner/Editor) -> `.xlsx` bytes with `Content-Disposition` `<Group name>_<YYYY-MM-DD>.xlsx` (day in Europe/Ljubljana, forbidden file-name characters `\/:*?"<>|` and control characters replaced by `_`, R-011-04). More than 10,000 -> 422 `export.too_many` with no file, no History (R-011-05). Contacts in Trash are skipped (R-011-02). Columns per `xlsx-export.schema.json`: 11 predefined fields, `Tags` (names comma-separated sorted ignoring case and accents, D-19g), then one column per Attribute key in the exported Contacts sorted the same way (R-011-03). Every cell is written with POI `setCellValue(String)` (cell type STRING, never `setCellFormula`), so a value starting with `=`, `+`, `-` or `@` stays the exact text as typed, including a phone `+386 40 123 456` (R-011-06, AC-011-04, EX-011-03, SR-06). Header texts and country names from `export_<lang>.properties` (R-011-09, D-19e). History `CONTACTS_EXPORTED` with the count (R-011-07); Viewer 403 and no entry (R-011-08).

**US-012 Delete and restore from Trash** (`TrashService`). `POST /groups/{g}/contacts/{c}/trash` (DELETE_CONTACT; confirmation is a UI dialog) sets `deleted_at`, History `CONTACT_DELETED`; already in Trash -> 200, nothing stored (R-012-09). `GET /groups/{g}/trash/contacts` (Owner/Editor) with days left (30 - age), "deleted by" from the History entry (D-18f). `POST .../trash/contacts/{c}/restore` clears `deleted_at`, keeps content and version, History `CONTACT_RESTORED`; if 30 days or more, 404 (R-012-05). Edit of a Contact in Trash -> 409 `contact.in_trash` and nothing stored (R-012-06). Viewer 403 (R-012-08).

**US-013 Delete forever** (`TrashService`, `TrashPurgeJob`). `DELETE /groups/{g}/trash/contacts/{c}` (DELETE_CONTACT_FOREVER, Owner only) deletes now, History values replaced by `removed`, one `CONTACT_DELETED_FOREVER` entry without personal data (R-013-06/08, ADR-08). The job does the same after 30 days (ADR-09, R-013-04). Tags stay (R-013-07). Afterwards 404 (R-013-05).

**US-014 Invite a person** (`InvitationService`). `POST /groups/{g}/invitations {email, role}` (MANAGE_MEMBERS, Owner only): BR-06 e-mail rule and max 255 (D-19h), lowercase, not a Member and no pending Invitation, at most 20 pending (R-014-02..04, 409/422), expiry 14 days, History `INVITATION_CREATED`. `GET /groups/{g}/invitations` lists pending (R-014-09). `POST .../invitations/{i}/cancel` -> `CANCELLED`, History. Acceptance runs inside `GET /groups` for a User with a verified e-mail: pending, unexpired Invitations of that e-mail (and Group not in Trash, D-18b) become Memberships with the invited Role, status `ACCEPTED`, History `MEMBER_JOINED` (R-014-05/06/08). No e-mail is sent by the product (R-014-12). Editor/Viewer 403 (R-014-10).

**US-015 Members and Roles** (`MembershipService`). `GET /groups/{g}/members` (all Members, R-015-01); `PATCH /groups/{g}/members/{userId} {role}` and `DELETE` (Owner only); both inside one transaction that locks the Group's Owner rows and refuses the loss of the last Owner with 409 `group.last_owner` (R-015-04, SR-10); Role read from the database each request so the change applies at once (R-015-02/03); removed Member's next request is 404; Contacts and History they created stay with the name (R-015-07); History `ROLE_CHANGED`, `MEMBER_REMOVED`. No operator access exists (R-015-08).

**US-017 Group activity list** (`HistoryQueryService`). `GET /groups/{g}/activity?userId&action&from&to&page` (READ_HISTORY): AND of filters, `from`/`to` are calendar days in Europe/Ljubljana both included, converted to UTC bounds (R-017-04); `from > to` -> 422 `activity.range_invalid` and no list (R-017-06); `userId` filter options come from the entries' users.

### Increment 2

**US-020 Delete a Group** (`GroupService`). `POST /groups/{g}/trash {name}` (DELETE_GROUP, Owner): the typed name must equal the Group name after trimming, case-sensitive, else 422 `group.name_mismatch`, nothing stored (R-020-01/02); sets `deleted_at`. From then every Group-scoped endpoint answers 404 for every caller (D-18a, R-020-03); the Group leaves `GET /groups`. `GET /group-trash` lists the caller's Groups in Trash where the caller is Owner, with days left (R-020-04); `POST /group-trash/{g}/restore` (Owner of that Group, else 404) restores within 30 days, refuses with 409 `group.name_taken` when the caller already owns a Group not in Trash with an equal name (R-020-07/08). Pending Invitations do not give access to a Group in Trash (D-18b). The job deletes after 30 days with all children (R-020-05, ADR-03/09). Editor/Viewer 403 on the delete (R-020-06).

**US-021 Delete my account** (`AccountService`). `DELETE /me` (any signed-in User, verified): if the User is the only Owner of a Group, including a Group in the Group Trash (D-27 A1), answer 409 `account.sole_owner` with `groups: [{id,name,inTrash,daysLeft}]` and delete nothing (R-021-02). Otherwise in one transaction: remove Memberships (no History entry, D-18e), set `user_email = NULL` in the User's History entries (name stays, R-021-03), keep Contacts (R-021-04), delete the `app_user` row, then delete the Firebase account through the Admin SDK (`FirebaseAuth.deleteUser`; a failure rolls the database back). Pending Invitations to the address stay pending (D-27 B1). A later registration with the same e-mail is a new User without Memberships (R-021-05). Confirmation dialog states irreversibility (R-021-06).

**US-022 Versions** (`VersionService`). Every Contact save writes a Version (ADR-07). `GET /groups/{g}/contacts/{c}/versions` (READ_VERSIONS, Owner/Editor) newest first with number, time, user; `GET .../versions/{n}`; `GET .../versions/{n}/compare` compares Version n with the live Contact (fields, Tags, Attributes that differ, both values; D-19b). Viewer 403 (R-022-06). Versions are never changed (R-022-05) and are deleted with the Contact (R-022-07).

**US-023 Restore a Version** (`VersionService` calling `ContactService.save`). `POST .../versions/{n}/restore {version}` (RESTORE_VERSION): the content of Version n is saved as a new Version with the usual validation (BR-06, R-023-03 -> 422), stale `version` -> 409 as BR-07 (R-023-04), missing Tags are created again (R-023-05, D-21), History `VERSION_RESTORED` with the restored number and changed fields (R-023-02). Viewer 403.

**US-024 Find duplicates** (`DuplicateService`, read only). `GET /groups/{g}/duplicates` (FIND_DUPLICATES, Owner/Editor) loads the Group's Contacts not in Trash and builds sets in memory: certain = same `email_norm` (R-024-02); probable = same first and last name (both non-empty, normalised) and the same company (non-empty) or the same phone (digits only, non-empty) (R-024-03); connected Contacts form one set (D-19c); a Contact in a certain set appears only there (R-024-08); Contacts with no e-mail, no name pair and no company are skipped (R-024-04); sets that equal a dismissal are hidden (R-025-02). Each set carries mark and reason text code with parameters (R-024-05); empty result text (R-024-10). Nothing is changed (R-024-06); other Groups are never read (R-024-07).

**US-025 Not a duplicate** (`DuplicateService`). `POST /groups/{g}/duplicates/dismissals {contactIds}` (DISMISS_DUPLICATE) stores the key and Contacts; a set that has any additional Contact is shown again (R-025-03). No Contact changes (R-025-05); undo not offered (D-21).

**US-026 Merge** (`MergeService`). `POST /groups/{g}/merges {survivorId, contactIds, fields, attributes, tags?, versions}` (MERGE_CONTACTS): at least two distinct Contacts of this Group (R-026-09, else 422 `merge.invalid_selection`); survivor = the oldest by `created_at` unless sent otherwise but it must be one of them; every chosen value must equal a value of one of the merged Contacts, else 422 (D-19d); Tags are the union (R-026-03); Attributes the union with a chosen value for keys that clash (R-026-04); the result is validated by BR-06 (R-026-06); each Contact's sent `version` must be current and none in Trash, else 409 as BR-07 and nothing merged (R-026-08). In one transaction: survivor saved as a normal save (new version, Version, edit History entries), merged-away Contacts moved to Trash (R-026-05/11), one `CONTACTS_MERGED` entry listing the merged Contacts and chosen values; the History of a merged-away Contact is shown in the Survivor's History through `details.mergedContactIds` (R-026-07). Preselection (first non-empty value of the Survivor, then of the next oldest, R-026-02) is computed by the frontend from the Contacts it received and is only a default; there is no automatic merge endpoint (R-026-10). Viewer 403 (R-026-12).

## Error model

Format and status mapping: ADR-10. Codes are fixed here and appear in the OpenAPI error schema and in both message catalogs (NFR-11).

| Status | Codes |
|---|---|
| 400 | `request.malformed`, `request.unknown_property` |
| 405 | `request.method_not_allowed` (also the answer to PUT, PATCH, DELETE on History, R-016-05) |
| 415 | `request.unsupported_media_type` |
| 401 | `auth.token_missing`, `auth.token_invalid` |
| 403 | `auth.email_not_verified`, `auth.role_forbidden`, `https.required` |
| 404 | `group.not_found`, `contact.not_found`, `tag.not_found`, `invitation.not_found`, `member.not_found`, `version.not_found`, `duplicate.not_found`, `route.not_found` |
| 409 | `contact.changed_by_someone_else` (with `current`), `contact.in_trash`, `group.name_taken`, `group.last_owner`, `user.email_taken`, `invitation.exists`, `invitation.limit`, `account.sole_owner`, `tag.name_taken` |
| 413 | `request.too_large` |
| 422 | `validation.failed` with `fieldErrors[]` (`validation.required_any`, `validation.email_invalid`, `validation.too_long`, `validation.phone_invalid`, `validation.country_invalid`, `validation.attribute_key_invalid`, `validation.attribute_value_invalid`, `validation.attribute_duplicate_key`, `validation.attribute_limit`, `validation.tag_invalid`, `validation.tag_limit`, `validation.name_invalid`), `export.too_many`, `activity.range_invalid`, `group.name_mismatch`, `merge.invalid_selection`, `merge.invalid_value` |
| 429 | `rate.limited` |
| 500 | `server.error` (no detail) |

Not found is used for a missing object, an object of another Group (R-018-05), a Group the caller is not a Member of, a Group in Trash, and a Contact past 30 days in Trash; the response never tells these apart (BR-01). The password-length rule (`validation.password_short`) exists only in the client, because Firebase owns passwords.

## Security controls

Level: OWASP ASVS 5.0 Level 2 for both components (security-requirements.md). Controls of `security-baseline.md` and the project requirements, with the module that provides them and the check:

| ID | Control in this design | Where | Check |
|---|---|---|---|
| SB-01 | Server validation of every input (`ContactValidator`, Bean Validation on request records, unknown properties rejected) | `web`, `service`, `domain` | validation tests of US-003/004/005; frontend rules are convenience only |
| SB-02 | `FirebaseTokenFilter` + `GroupAccessInterceptor`; handler-annotation reflection test | `security`, `web` | AR-06 test; AC-018-01..03 |
| SB-03 | No passwords stored (Firebase holds them); secrets from environment | `config` | gitleaks, `application.yml` placeholder test (AR-07) |
| SB-04 | TLS at the reverse proxy, `HttpsFilter` with `REQUIRE_HTTPS`, HSTS set by the proxy and by nginx for `https` | `security`, nginx | AC-018-12; install guide |
| SB-05 | Parameterised SQL only; React escapes text; JSON responses | `persistence`, `frontend` | find-sec-bugs; injection test (SR-04); AC-018-14 |
| SB-06 | Rate limit per User; the only unauthenticated endpoint is `/health` | `RateLimitFilter` | AC-018-10 |
| SB-07 | `ApiExceptionHandler`, `SecurityLogger`; logs without bodies, tokens, query strings | `web`, `security` | AC-018-09, AC-018-13 |
| SB-08 | `dependency-check-maven`, `npm audit` in CI | CI | scanner logs |
| SB-09 | SpotBugs + find-sec-bugs, `eslint-plugin-security`, gitleaks | CI | scanner logs |
| SB-10 | API headers (ADR-11); nginx CSP/frame/nosniff/referrer (ADR-15) | filter, nginx | header test on both |
| SB-11 | Non-root containers, no secrets in images | Dockerfiles | `docker run --user` check, image scan |
| SB-12 | Only fields the requirements name are collected (data model) | data model | review |
| SB-13 | Purpose and retention per item: security-requirements.md "Personal data"; purge job and account deletion enforce them | `job`, `AccountService` | AC-013-04, AC-021-* |
| SB-14 | No consent is collected in the product; there is no preselected consent box | UI | review |
| SR-01 | Isolation by interceptor + repositories with `groupId`; isolation test over the contract | `web`, `persistence` | AC-018-04, E2E-03 (D-24) |
| SR-02 | Filter order of US-018 | `security` | AC-018-01, 02, 08 |
| SR-03 | Role read from `member` on every request; token claims ignored | `GroupAccessInterceptor` | test with a token carrying a `role` claim; AC-015-02/03 |
| SR-04 | Named parameters; injection test sends `'; DROP TABLE contact; --` in every text field | `persistence` | SR-04 test, EX-008-03 |
| SR-05 | No HTML rendering of user text | frontend | AC-018-16, EX-003-13 |
| SR-06 | POI string cells | `ExportService` | AC-011-04, EX-011-03 |
| SR-07 | `SecurityLogger` | `security` | AC-018-15 |
| SR-08 | Rate limit, size filter, HTTPS filter, CORS | `security` | AC-018-12..14 |
| SR-09 | Environment variables only; `.env` untracked; `secrets.env.example` lists names | repo | gitleaks scan |
| SR-10 | Transactional last-Owner rule | `MembershipService` | AC-015-04/05, EX-015-01 |
| SR-11 | Invitation limit, expiry, verified e-mail | `InvitationService` | AC-014-03/05/08, EX-014-02 |
| SR-12 | Error format without internals | `ApiExceptionHandler` | AC-018-11 |
| SR-13 | Emulator only in tests and local development | test config | tests run offline |

### Permission table

`Action` enum of ADR-05; it mirrors the matrix of BR-02 (with the Roles of D-25). O = Owner, E = Editor, V = Viewer.

| Action | Used by | O | E | V |
|---|---|---|---|---|
| `READ_CONTACTS` (also facets, search, filter, Group view) | US-002, US-003, US-007..US-010 | yes | yes | yes |
| `EDIT_CONTACT` (create, edit, Attributes, Tags of a Contact) | US-003..US-005 | yes | yes | no |
| `MANAGE_TAGS` (Tag list, rename, delete) | US-006 | yes | yes | no |
| `DELETE_CONTACT`, `READ_TRASH`, `RESTORE_CONTACT` | US-012 | yes | yes | no |
| `DELETE_CONTACT_FOREVER` | US-013 | yes | no | no |
| `EXPORT` | US-011 | yes | yes | no |
| `READ_HISTORY` (Contact History, Group activity list) | US-016, US-017 | yes | yes | no |
| `READ_VERSIONS`, `RESTORE_VERSION` | US-022, US-023 | yes | yes | no |
| `FIND_DUPLICATES`, `DISMISS_DUPLICATE`, `MERGE_CONTACTS` | US-024..US-026 | yes | yes | no |
| `MANAGE_MEMBERS` (invite, cancel Invitation, change Role, remove) | US-014, US-015 | yes | no | no |
| `READ_MEMBERS` | US-015 | yes | yes | yes |
| `DELETE_GROUP` | US-020 | yes | no | no |

Endpoints without a Group in the path need no Action: `GET /me`, `DELETE /me`, `GET /groups`, `POST /groups`, and the Owner-only `GET /group-trash` and `POST /group-trash/{g}/restore` (annotation `@OwnerOfGroupInTrash`).

## Threat model

STRIDE per interface (S spoofing, T tampering, R repudiation, I information disclosure, D denial of service, E elevation of privilege) and per personal-data item. "New" controls are added by this specification; `[ASSUMPTION]` marks a residual risk accepted until the customer decides.

| Threat (STRIDE) | Interface or data | Control (SB, SR or new) |
|---|---|---|
| S: forged, stolen or replayed ID token | REST interface | Admin SDK checks signature, expiry, audience; `FirebaseTokenFilter` (SB-02, SR-02); tokens are short-lived (Firebase) |
| S: emulator accepted in production | Token check | New: the backend refuses to start with profile `prod` when `FIREBASE_AUTH_EMULATOR_HOST` is set; `docker-compose.prod.yml` does not define it |
| T: Group named in body or query to reach another tenant | REST interface | Group from path only, unknown properties rejected, repositories filter by `groupId` (SR-01, R-018-04, AR-02) |
| T: ID of an object of another Group in a path | REST interface | Object loaded with the Group id, 404 otherwise (R-018-05) |
| R: user denies a change | History, logs | History with user, time, action, old/new values, append-only at code and database level (BR-03, ADR-03); `SecurityLogger` (SR-07) |
| I: existence of a Group or Contact revealed by 403 vs 404 | REST interface | Fixed check order, one 404 for "missing" and "not a Member" (D-18c, BR-01) |
| I: stack trace, SQL, token in an error or log | REST interface, logs | `ApiExceptionHandler`, `SecurityLogger` never log bodies, query strings, tokens (SB-07, SR-12) |
| D: request flood from one User | REST interface | 100 requests per User per minute, 1 MB bodies (SR-08, ADR-11) |
| D: huge export or search | Export, search | 10,000 Contacts per export (R-011-05), pagination 25, bounded queries |
| E: Editor/Viewer performs an Owner action | REST interface | One permission table, `@RequiresAction`, Role read from the database (SR-03, ADR-05) |
| E: last Owner removed, Group without owner | Members | Transactional rule (SR-10) |
| S: credential stuffing, account enumeration | Firebase sign-in | Firebase's protections; one error message and one reset confirmation (R-001-05, R-001-08) |
| E: use before e-mail verification | Firebase sign-in, Invitations | 403 on every Group endpoint; Invitation joins only with a verified e-mail (R-018-06, SR-11) |
| T: SQL injection through any text field | Database | Named parameters only, find-sec-bugs, injection test (SR-04, AR-05) |
| T: application role alters History or schema | Database | Restricted role, column-level grants (ADR-03); test proves it |
| I: personal data in backups kept longer than stated | Database backups | 14-day retention stated in the install guide (SB-13); `[ASSUMPTION]` restores of an old backup re-create purged data |
| T: formula injection in Excel | Export | String cells only (SR-06) |
| I: export contains another Group's Contacts | Export | `contactIds` resolved with `groupId`; filter selection runs the Group query (SR-01) |
| T/I: stored XSS through Contact, Tag, Group names | User interface | React escaping, no raw HTML sink (lint rule), CSP `script-src 'self'` (SR-05, SB-10) |
| E: clickjacking | User interface | `X-Frame-Options: DENY`, CSP `frame-ancestors 'none'` on nginx and API |
| I: token theft by XSS from Firebase storage | User interface | CSP and escaping reduce XSS; the SDK keeps the session in IndexedDB; `[ASSUMPTION]` accepted for Level 2 |
| T: CSRF | User interface | Bearer token in a header, no cookie session, CORS limited to one origin (R-018-16) |
| I: open redirect after sign-in | User interface | Redirect target is an in-app route only; `react-router` 7.18.5 (D-05) |
| Data item: Contact data (names, e-mail, phone, address, notes, Attributes) | Database, API, export | Group isolation, Roles; Trash 30 days then purge (BR-04); `removed` replacement in History (R-013-08); minimisation: only the 11 fields (SB-12) |
| Data item: User account (UID, e-mail, name) | Database, Firebase | Deleted with the account (US-021); other Members see name and e-mail only within shared Groups |
| Data item: History `details` | Database, API | Visible to Owner/Editor only; values replaced when the Contact is deleted forever; e-mail cleared when the account is deleted (R-021-03); retained while the Group exists (D-26) |
| Data item: Invitation e-mail | Database | Visible to Owners; 14-day expiry; removed with the Group (D-27 B1: stays pending after account deletion) |
| Data item: server log | Log files | No Contact data, no tokens; 30 days retention by the operator (D-26) |

## NFR checks

Checked in phase 6 by runtime demonstration unless stated; scripts are written by the tasks named in `docs/02_tasks.md`.

| NFR | How it is checked | Pass value |
|---|---|---|
| NFR-01 | `02_output/perf/measure.mjs` (Node 22 `fetch`, no new dependency) on the Compose stack with the 10,000-Contact seed (`seed/generate-large.sql`): 50 requests each for list, search, filter; median and p95 recorded | median and p95 below 1 s |
| NFR-02 | Same script, 50 opens of a Contact | below 500 ms (p95) |
| NFR-03 | Same script, 5 exports of 1,000 Contacts | each below 10 s |
| NFR-04 | Same script, 20 concurrent Users (list, search, edit, export) for 5 minutes, each below 100 requests per minute | zero errors and zero 429; NFR-01 still holds |
| NFR-05 | Review of `docker-compose.prod.yml` (health checks, `restart: unless-stopped`) and the install guide | both present |
| NFR-06 | `scripts/backup.sh` and `scripts/restore.sh`; one restore into an empty database, row counts compared | counts equal; recorded (DoD-P07) |
| NFR-07 | Playwright runs E2E-01..06 in chromium, firefox, webkit; Edge covered by chromium [ASSUMPTION] | all pass |
| NFR-08 | Playwright screenshots `mobile` 375, `tablet` 768, `desktop` 1440 for every screen; overflow check `scrollWidth <= clientWidth` | no horizontal scroll, no overlap |
| NFR-09 | `@axe-core/playwright` with WCAG 2.2 AA tags on every screen; keyboard-only walkthrough of sign in, list, create/edit Contact, export | zero Critical/Serious violations; walkthrough passes |
| NFR-10 | E2E-01 counts user actions from first page to first Contact | at most 12 [ASSUMPTION] |
| NFR-11 | Catalog test: `sl.json` and `en.json` have identical key sets, every backend error code has a key; screenshots in both languages | identical sets |
| NFR-12 | `docker compose -f docker-compose.prod.yml up` from the built images on a clean Docker host | all services healthy within 2 minutes |
| NFR-13 | `license-checker-rseidelsohn` for npm and the Maven license report, compared with the allowed list of `tech-stack.md` | no other licence |
| NFR-14 | AC-003-06, EX-003-* validation tests, concurrent-save integration test | a stale save stores nothing |
| NFR-15 | Release checklist: `docs/openapi.json` export, install and backup guide, Slovene user guide | three files exist (DoD-P04) |

## User interface

Stack and tokens: ADR-13; language: ADR-14; structure: ADR-12. All screens follow `ui-baseline.md` (UI-01..30) with the two overrides of `ui-design.md` (UI-08 desktop first, UI-03 brand in one place). Every screen exists at the three widths (UI-22) and has loading, empty, error and success states (UI-14). `/wireframes` draws each screen as `WF-nn`; the screen ids below (`SC-nn`) are the inventory it starts from.

| Screen | Route | Stories | Notes |
|---|---|---|---|
| SC-01 Sign in | `/sign-in` | US-001, US-019 | e-mail + password, Google button, links to register and reset; one error message (R-001-05) |
| SC-02 Register | `/register` | US-001 | e-mail, display name, password with rule hint |
| SC-03 Reset password | `/reset-password` | US-001 | same confirmation for any address |
| SC-04 Verify e-mail | `/verify-email` | US-001 | message "Verify your e-mail to continue", resend button, sign out |
| SC-05 Group list | `/groups` | US-002, US-014, US-020 | cards with Role; empty state "Create group"; create dialog; link to Group Trash for Owners |
| SC-06 Contact list | `/groups/:g/contacts` | US-007..US-011, US-010, US-019 | search box, filter panel (Tags all/any, Without-Tags, country, company, Attribute), sortable columns, 25 per page, Selection bar, Export button, empty and no-match states |
| SC-07 Contact | `/groups/:g/contacts/:c` | US-003, US-004, US-005, US-012, US-016, US-022, US-023 | tabs Details (view/edit form with Attributes and Tags), History, Versions; conflict banner with current Contact |
| SC-08 New Contact | `/groups/:g/contacts/new` | US-003, US-004, US-005 | same form as SC-07 |
| SC-09 Tag list | `/groups/:g/tags` | US-006 | counts, rename, delete with confirmation |
| SC-10 Trash | `/groups/:g/trash` | US-012, US-013 | days left, Restore, Delete forever (Owner) |
| SC-11 Members | `/groups/:g/members` | US-015 | list; Owner changes Role, removes |
| SC-12 Invitations | `/groups/:g/invitations` | US-014 | create, pending list, cancel (screen added by D-23d) |
| SC-13 Group activity | `/groups/:g/activity` | US-017 | filters user, action, dates; message states |
| SC-14 Delete Group | dialog in SC-05/SC-06 | US-020 | type the Group name to confirm |
| SC-15 Group Trash | `/group-trash` | US-020 | Owner-only list with days left, Restore |
| SC-16 Account | `/account` | US-021 | delete account dialog; Sole-Owner Groups listed (D-23d) |
| SC-17 Duplicates | `/groups/:g/duplicates` | US-024, US-025 | sets with mark and reason, pick Contacts, "not a duplicate", start merge (screen added by D-23d) |
| SC-18 Merge | `/groups/:g/merge` | US-026 | side-by-side fields, choice per field and Attribute, confirm |
| SC-19 Not found / error | any | US-018 | friendly message, no detail |

Navigation: header with product name (`brand.ts`), Group switcher (link to SC-05), language switch and user menu (Account, Sign out); inside a Group a tab bar or side bar (desktop) / menu button (mobile) with Contacts, Tags, Trash, Duplicates, Activity, Members, Invitations; items the Role may not use are not shown, and the server still enforces (SB-02). One primary action per page (UI-10). Tokens: the semantic set of UI-04 in `styles/themes/{wireframe,final}.css`; type scale 12/14/16/20/24/32; 4 px spacing grid; radii 8/12. Components: button (primary, secondary, ghost, busy), input, label, form field with inline error, select, checkbox, dialog and confirm dialog, dropdown menu, tabs, `ResponsiveTable` (cards below 768 px), toast (sonner), skeleton, alert, empty and error state, language switch, pagination.

<!-- slice 5: traceability -->

## Traceability

Every requirement ID of the inputs maps to a section of this document (rows are generated and checked by `scripts/check-traceability.mjs`; a story row lists its rules, acceptance criteria and edge cases, implemented in the story entry of "Behaviour per story").

| R / AC / EX / BR / SR / SB / NFR | Section |
|---|---|
| R-001-01..12, AC-001-01..11, EX-001-01..09 | Behaviour per story: US-001 |
| R-002-01..07, AC-002-01..08, EX-002-01..07 | Behaviour per story: US-002 |
| R-003-01..11, AC-003-01..10, EX-003-01..13 | Behaviour per story: US-003 |
| R-004-01..09, AC-004-01..09, EX-004-01..09 | Behaviour per story: US-004 |
| R-005-01..10, AC-005-01..08, EX-005-01..08 | Behaviour per story: US-005 |
| R-006-01..08, AC-006-01..07, EX-006-01..06 | Behaviour per story: US-006 |
| R-007-01..08, AC-007-01..08, EX-007-01..07 | Behaviour per story: US-007 |
| R-008-01..07, AC-008-01..09, EX-008-01..07 | Behaviour per story: US-008 |
| R-009-01..12, AC-009-01..12, EX-009-01..07 | Behaviour per story: US-009 |
| R-010-01..10, AC-010-01..09, EX-010-01..05 | Behaviour per story: US-010 |
| R-011-01..10, AC-011-01..11, EX-011-01..10 | Behaviour per story: US-011 |
| R-012-01..09, AC-012-01..08, EX-012-01..07 | Behaviour per story: US-012 |
| R-013-01..08, AC-013-01..08, EX-013-01..06 | Behaviour per story: US-013 |
| R-014-01..12, AC-014-01..11, EX-014-01..09 | Behaviour per story: US-014 |
| R-015-01..08, AC-015-01..09, EX-015-01..05 | Behaviour per story: US-015 |
| R-016-01..08, AC-016-01..09, EX-016-01..07 | Behaviour per story: US-016 |
| R-017-01..09, AC-017-01..09, EX-017-01..05 | Behaviour per story: US-017 |
| R-018-01..16, AC-018-01..17, EX-018-01..09 | Behaviour per story: US-018 |
| R-019-01..07, AC-019-01..07, EX-019-01..03 | Behaviour per story: US-019 |
| R-020-01..08, AC-020-01..07, EX-020-01..07 | Behaviour per story: US-020 |
| R-021-01..06, AC-021-01..07, EX-021-01..04 | Behaviour per story: US-021 |
| R-022-01..07, AC-022-01..07, EX-022-01..05 | Behaviour per story: US-022 |
| R-023-01..06, AC-023-01..07, EX-023-01..04 | Behaviour per story: US-023 |
| R-024-01..10, AC-024-01..11, EX-024-01..06 | Behaviour per story: US-024 |
| R-025-01..05, AC-025-01..05, EX-025-01..04 | Behaviour per story: US-025 |
| R-026-01..12, AC-026-01..12, EX-026-01..09 | Behaviour per story: US-026 |
| BR-01 | Security controls (SR-01); ADR-05; Behaviour per story: US-018 |
| BR-02 | Permission table; ADR-05; Behaviour per story |
| BR-03 | ADR-08; Behaviour per story: US-016, US-011 to US-017 |
| BR-04 | ADR-09; Behaviour per story: US-012, US-013, US-020 |
| BR-05 | ADR-06; Data model and validation |
| BR-06 | Data model and validation; Behaviour per story: US-003 |
| BR-07 | ADR-07; Behaviour per story: US-003 |
| SR-01, SR-02, SR-03, SR-04, SR-05, SR-06, SR-07, SR-08, SR-09, SR-10, SR-11, SR-12, SR-13 | Security controls (table rows SR-01..SR-13); Threat model |
| SB-01, SB-02, SB-03, SB-04, SB-05, SB-06, SB-07, SB-08, SB-09, SB-10, SB-11, SB-12, SB-13, SB-14 | Security controls (table rows SB-01..SB-14); Threat model |
| NFR-01, NFR-02, NFR-03, NFR-04, NFR-05, NFR-06, NFR-07, NFR-08, NFR-09, NFR-10, NFR-11, NFR-12, NFR-13, NFR-14, NFR-15 | NFR checks (one row each) |
