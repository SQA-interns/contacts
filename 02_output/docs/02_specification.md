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
| `contact` | `group_id`, `version int` (start 1), 7 text columns (`first_name`, `last_name`, `company`, `job_title`, `street`, `postal_code`, `city`) 255, `email` 255 lowercase, `phone` 30, `country char(2)`, `notes` 5000, `first_name_norm`, `last_name_norm`, `company_norm`, `email_norm`, `search_norm`, `created_at`, `updated_at`, `created_by`, `deleted_at` null | index (`group_id`, `deleted_at`, `last_name_norm`, `first_name_norm`); `group_id` never updated (trigger-free: no SQL updates it) | BR-06 in `ContactValidator`; ADR-06 |
| `tag` | `group_id`, `name text(50)` | unique (`group_id`, `lower(name)`); pattern letters (any alphabet), digits, `-`, `_` (D-21) | |
| `contact_tag` | `contact_id`, `tag_id` | primary key pair; at most 50 per Contact (service); Contact and Tag same Group (service + composite check by query) | |
| `attribute` | `contact_id`, `key text(50)`, `value text(255)` | unique (`contact_id`, `lower(key)`); at most 50 per Contact (service); value not empty (D-20) | |
| `history_entry` | `group_id`, `occurred_at`, `user_id`, `user_name`, `user_email` null, `action`, `contact_id` null, `details jsonb` | ADR-03, ADR-08; indexes (`group_id`, `occurred_at desc`), (`contact_id`, `occurred_at desc`) | no foreign key on `user_id`, `contact_id` |
| `contact_version` | `contact_id`, `number`, `created_at`, `user_id`, `snapshot jsonb` | unique (`contact_id`, `number`); cascade with `contact` | ADR-07, D-19i |
| `duplicate_dismissal` | `group_id`, `contact_key text`, `created_at`, `created_by` | unique (`group_id`, `contact_key`); `contact_key` = SHA-256 of the sorted Contact ids | R-025-02 |
| `duplicate_dismissal_contact` | `dismissal_id`, `contact_id` | primary key pair | R-025-03 (a bigger set is new) |

**Validation (SB-01, BR-06, server side only authoritative):** `ContactValidator.validate(ContactInput)` trims every text, lowercases e-mail, then checks in this order and returns all field errors at once: at least one of first name, last name, e-mail, company non-empty; e-mail format (simple RFC-5322 subset, max 255); phone characters digits, space and `+ - ( ) . /`, max 30; notes max 5,000; other text max 255; country in `CountryCodes` (`Locale.getISOCountries()`); Attributes: key 1..50, value 1..255, keys unique ignoring case, at most 50; Tags: valid name, at most 50. Field error codes: `validation.required_any`, `validation.email_invalid`, `validation.too_long`, `validation.phone_invalid`, `validation.country_invalid`, `validation.attribute_*`, `validation.tag_*`. Group name: 1..100 after trim. Invitation e-mail: BR-06 e-mail rule (D-19h). Request bodies reject unknown properties (R-018-04).

<!-- slice 2: behaviour INC-1 -->
<!-- slice 3: behaviour INC-2, error model, security controls -->
<!-- slice 4: threat model, NFR checks, user interface -->
<!-- slice 5: traceability -->
