# Tasks and increments

> For: agent (writes), human (reads) · Written in: phase 2 (`/plan`) · Source: stories, `docs/02_specification.md` · Agent: writes

Each task is a vertical slice that changes behaviour a test can observe. "Covers" lists the rules the task implements and the acceptance criteria (AC) and edge cases (EX) that become observable when the task is done (the last task that implements one of their rules carries them); every rule, AC and EX of every story is covered by exactly one task or more (generated and checked by a script, no gaps). Paths are relative to `02_output/`; `B` = `backend/src/main/java/si/contactgroups/`, `F` = `frontend/src/features/`. No story has more than six tasks, so one `/implement` session takes one story (`/implement US-018`); a session may be limited to a task range (`/implement US-018 T-07..T-09`).

## Increments

| Increment | Stories (in implement order) | Goal |
|---|---|---|
| INC-1 | US-001, US-002, US-018, US-019, US-003, US-016, US-004, US-005, US-006, US-007, US-008, US-009, US-010, US-011, US-012, US-013, US-014, US-015, US-017 | MVP: sign-in, Groups, Contacts with Attributes and Tags, list, search, filter, Selection, Excel export, Trash, Invitations and Roles, History, secured REST interface, language switch (`intent.md` MVP) |
| INC-2 | US-020, US-021, US-022, US-023, US-024, US-025, US-026 | Delete Group, delete account, Versions and restore, duplicate search, "not a duplicate", merge |

Order inside INC-1: a story comes after the stories it depends on (`depends_on` of the story files); `US-018` follows `US-002` because it needs the Membership data, and its isolation test (AC-018-04) runs per increment over the endpoints that exist, fully at the release (D-23). `US-016` precedes the stories that write History entries (`US-004`, `US-005`, `US-006`, `US-011` to `US-015`).

## Tasks

| Task | Story | Covers (R, AC, EX) | Expected files | Depends on |
|---|---|---|---|---|
| T-01 | US-001 | R-001-11, R-001-12, AC-001-11, SR-02, SR-03 | `backend/src/main/java/si/contactgroups/web/MeController`, `backend/src/main/java/si/contactgroups/service/UserService`, `backend/src/main/java/si/contactgroups/persistence/UserRepository` | — |
| T-02 | US-001 | R-001-02..03, R-001-05, R-001-09..10, R-001-12, AC-001-02..03, AC-001-05, AC-001-09..10, EX-001-07 | `frontend/src/features/auth/SignInPage`, `frontend/src/features/auth/useSignIn`, `frontend/src/auth/` | T-01 |
| T-03 | US-001 | R-001-01, R-001-04, R-001-06..07, AC-001-01, AC-001-04, AC-001-06..07, EX-001-01..06, EX-001-09 | `frontend/src/features/auth/RegisterPage`, `frontend/src/features/auth/VerifyEmailPage` | T-02 |
| T-04 | US-001 | R-001-08, AC-001-08, EX-001-08 | `frontend/src/features/auth/ResetPasswordPage` | T-02 |
| T-05 | US-002 | R-002-01..05, R-002-07, AC-002-01..06, AC-002-08, EX-002-01..07 | `backend/src/main/java/si/contactgroups/web/GroupController`, `backend/src/main/java/si/contactgroups/service/GroupService`, `backend/src/main/java/si/contactgroups/service/MembershipService`, `backend/src/main/java/si/contactgroups/persistence/GroupRepository`, `backend/src/main/java/si/contactgroups/persistence/MemberRepository` | T-01 |
| T-06 | US-002 | R-002-02..03, R-002-05..06, AC-002-07 | `frontend/src/features/groups/GroupListPage`, `frontend/src/features/groups/CreateGroupDialog` | T-05, T-03 |
| T-07 | US-018 | R-018-01..03, R-018-06..07, AC-018-01..05, AC-018-08..09, EX-018-01, EX-018-05, EX-018-07..08, SR-01, SR-02, SR-03, AR-06 | `backend/src/main/java/si/contactgroups/web/GroupAccessInterceptor`, `backend/src/main/java/si/contactgroups/service/GroupAccessService`, `backend/src/main/java/si/contactgroups/persistence/MemberRepository` | T-05 |
| T-08 | US-018 | R-018-04, R-018-05, AC-018-06..07, AR-02 | `backend/src/main/java/si/contactgroups/web/*Controller (path variables)`, `backend/src/main/java/si/contactgroups/persistence/* (groupId first)` | T-07 |
| T-09 | US-018 | R-018-09, R-018-11, R-018-14..15, AC-018-11, AC-018-13, AC-018-16..17, EX-018-02, EX-018-04, EX-018-06, SR-05, SR-12 | `backend/src/main/java/si/contactgroups/web/ApiExceptionHandler`, `backend/src/main/java/si/contactgroups/security/RequestSizeFilter` | T-07 |
| T-10 | US-018 | R-018-10, R-018-12..13, R-018-16, AC-018-12, AC-018-14..15, EX-018-03, EX-018-09, SR-07, SR-08, SB-04, SB-06, SB-10 | `backend/src/main/java/si/contactgroups/security/RateLimitFilter`, `backend/src/main/java/si/contactgroups/security/HttpsFilter`, `backend/src/main/java/si/contactgroups/security/SecurityLogger`, `backend/src/main/java/si/contactgroups/config/SecurityConfig` | T-07 |
| T-11 | US-018 | R-018-08, AC-018-10, SR-01, DoD-P02 | `backend/src/main/java/si/contactgroups/config/OpenApiConfig`, `backend/src/test/**/IsolationIT`, `backend/src/test/**/ContractParityIT` | T-08, T-09, T-10 |
| T-12 | US-019 | R-019-01..03, R-019-06, AC-019-01..03, AC-019-06, EX-019-01..02 | `frontend/src/features/../i18n/*`, `frontend/src/components/LanguageSwitch` | T-01 |
| T-13 | US-019 | R-019-04, R-019-05, R-019-07, AC-019-04..05, AC-019-07, EX-019-03 | `frontend/src/features/../i18n/format`, `frontend/src/api/errors` | T-12 |
| T-14 | US-003 | R-003-01..05, R-003-10, AC-003-01..04, AC-003-09, EX-003-01..10, BR-05, BR-06 | `backend/src/main/java/si/contactgroups/web/ContactController`, `backend/src/main/java/si/contactgroups/service/ContactService`, `backend/src/main/java/si/contactgroups/service/ContactValidator`, `backend/src/main/java/si/contactgroups/domain/Normaliser`, `backend/src/main/java/si/contactgroups/domain/CountryCodes`, `backend/src/main/java/si/contactgroups/persistence/ContactRepository` | T-07, T-05 |
| T-15 | US-003 | R-003-06..09, R-003-11, AC-003-05..08, AC-003-10, EX-003-11..13, BR-07, NFR-14 | `backend/src/main/java/si/contactgroups/service/ContactService`, `backend/src/main/java/si/contactgroups/persistence/ContactRepository`, `backend/src/main/java/si/contactgroups/persistence/VersionRepository` | T-14 |
| T-16 | US-003 | R-003-01..05 | `frontend/src/features/contacts/ContactForm`, `frontend/src/features/contacts/NewContactPage` | T-14, T-06 |
| T-17 | US-003 | R-003-06..09, R-003-11 | `frontend/src/features/contacts/ContactPage`, `frontend/src/features/contacts/ConflictBanner` | T-15, T-16 |
| T-18 | US-016 | R-016-01..02, R-016-05..07, AC-016-02, AC-016-05..06, AC-016-08..09, EX-016-01..03, EX-016-07, BR-03, AR-09, P-3 | `backend/src/main/java/si/contactgroups/service/HistoryWriter`, `backend/src/main/java/si/contactgroups/persistence/HistoryRepository`, `backend/src/main/java/si/contactgroups/web/ContactController (405 on History writes)` | T-15 |
| T-19 | US-016 | R-016-03, R-016-04, R-016-08, AC-016-01, AC-016-03..04, AC-016-07, EX-016-04..06 | `backend/src/main/java/si/contactgroups/web/HistoryController`, `frontend/src/features/history/ContactHistoryTab` | T-18 |
| T-20 | US-004 | R-004-01..04, R-004-06..09, AC-004-01..03, AC-004-05..07, AC-004-09, EX-004-01..09 | `backend/src/main/java/si/contactgroups/service/AttributeService`, `backend/src/main/java/si/contactgroups/persistence/AttributeRepository` | T-15, T-18 |
| T-21 | US-004 | R-004-02..05, AC-004-04, AC-004-08 | `frontend/src/features/contacts/AttributeEditor` | T-20, T-17 |
| T-22 | US-005 | R-005-01..06, R-005-08, R-005-10, AC-005-01..05, AC-005-08, EX-005-01..08 | `backend/src/main/java/si/contactgroups/service/TagService`, `backend/src/main/java/si/contactgroups/persistence/TagRepository` | T-15, T-18 |
| T-23 | US-005 | R-005-02, R-005-04, R-005-07, R-005-09, AC-005-06..07 | `frontend/src/features/contacts/TagInput` | T-22, T-17 |
| T-24 | US-006 | R-006-01..08, AC-006-01..07, EX-006-01..06 | `backend/src/main/java/si/contactgroups/web/TagController`, `backend/src/main/java/si/contactgroups/service/TagService` | T-22 |
| T-25 | US-006 | R-006-01, R-006-03, R-006-06, R-006-08 | `frontend/src/features/tags/TagListPage` | T-24, T-06 |
| T-26 | US-007 | R-007-01..04, R-007-07..08, AC-007-01..03, AC-007-06, AC-007-08, EX-007-01..07, NFR-01 | `backend/src/main/java/si/contactgroups/service/SearchService`, `backend/src/main/java/si/contactgroups/persistence/ContactRepository` | T-14 |
| T-27 | US-007 | R-007-01..06, AC-007-04..05, AC-007-07 | `frontend/src/features/contacts/ContactListPage`, `frontend/src/features/contacts/ContactTable` | T-26, T-06 |
| T-28 | US-008 | R-008-01..03, R-008-05..07, AC-008-01..04, AC-008-06..09, EX-008-01..07, SR-04, BR-05 | `backend/src/main/java/si/contactgroups/service/SearchService`, `backend/src/main/java/si/contactgroups/persistence/ContactRepository` | T-26 |
| T-29 | US-008 | R-008-03, R-008-04, R-008-05, AC-008-05 | `frontend/src/features/contacts/SearchBox` | T-28, T-27 |
| T-30 | US-009 | R-009-01..08, R-009-11, AC-009-01..08, AC-009-11, EX-009-01..07, BR-05 | `backend/src/main/java/si/contactgroups/service/SearchService`, `backend/src/main/java/si/contactgroups/web/FacetController` | T-28 |
| T-31 | US-009 | R-009-01..07, R-009-09..10, R-009-12, AC-009-09..10, AC-009-12 | `frontend/src/features/contacts/FilterPanel` | T-30, T-29 |
| T-32 | US-010 | R-010-01, R-010-03..04, R-010-07..10, AC-010-01..02, AC-010-05..06, AC-010-08..09, EX-010-03..04 | `frontend/src/features/contacts/selection/*` | T-27 |
| T-33 | US-010 | R-010-02, R-010-05, R-010-06, AC-010-03..04, AC-010-07, EX-010-01..02, EX-010-05 | `frontend/src/features/contacts/selection/*` | T-32, T-31 |
| T-34 | US-011 | R-011-02..04, R-011-06, R-011-09..10, AC-011-02..04, AC-011-08, AC-011-10..11, EX-011-01, EX-011-03..10, SR-06 | `backend/src/main/java/si/contactgroups/service/ExportService`, `backend/src/main/java/si/contactgroups/domain/ExportColumns`, `backend/src/main/resources/export_sl.properties`, `backend/src/main/resources/export_en.properties` | T-28 |
| T-35 | US-011 | R-011-05, R-011-07, R-011-08, AC-011-05..07, AC-011-09, EX-011-02 | `backend/src/main/java/si/contactgroups/web/ExportController` | T-34, T-18 |
| T-36 | US-011 | R-011-01, R-011-05, AC-011-01 | `frontend/src/features/contacts/ExportButton` | T-35, T-33 |
| T-37 | US-012 | R-012-02, R-012-04..07, R-012-09, AC-012-03..05, AC-012-07, EX-012-01..02, EX-012-04..06 | `backend/src/main/java/si/contactgroups/service/TrashService`, `backend/src/main/java/si/contactgroups/web/TrashController` | T-18 |
| T-38 | US-012 | R-012-03, R-012-08, AC-012-02, AC-012-06, AC-012-08, EX-012-07 | `backend/src/main/java/si/contactgroups/web/TrashController` | T-37 |
| T-39 | US-012 | R-012-01, R-012-03..04, R-012-08, AC-012-01, EX-012-03 | `frontend/src/features/trash/TrashPage`, `frontend/src/features/trash/DeleteContactDialog` | T-38, T-17 |
| T-40 | US-013 | R-013-01, R-013-03, R-013-05..08, AC-013-01, AC-013-03, AC-013-05..08, EX-013-01, EX-013-04, EX-013-06, AR-09 | `backend/src/main/java/si/contactgroups/service/TrashService`, `backend/src/main/java/si/contactgroups/service/HistoryWriter` | T-37 |
| T-41 | US-013 | R-013-04, AC-013-04, EX-013-02..03, AR-13, BR-04 | `backend/src/main/java/si/contactgroups/job/TrashPurgeJob` | T-40 |
| T-42 | US-013 | R-013-02, R-013-03, AC-013-02 | `frontend/src/features/trash/DeleteForeverDialog` | T-40, T-39 |
| T-43 | US-014 | R-014-01..04, R-014-07, R-014-09..12, AC-014-01, AC-014-04..07, AC-014-09..11, EX-014-01, EX-014-05..06, EX-014-08, SR-11 | `backend/src/main/java/si/contactgroups/service/InvitationService`, `backend/src/main/java/si/contactgroups/web/InvitationController`, `backend/src/main/java/si/contactgroups/persistence/InvitationRepository` | T-05, T-18 |
| T-44 | US-014 | R-014-05..06, R-014-08, R-014-11, AC-014-02..03, AC-014-08, EX-014-02..04, EX-014-07, EX-014-09, SR-11 | `backend/src/main/java/si/contactgroups/service/InvitationService`, `backend/src/main/java/si/contactgroups/service/GroupService` | T-43 |
| T-45 | US-014 | R-014-01..04, R-014-07, R-014-09..10 | `frontend/src/features/invitations/InvitationsPage` | T-44, T-06 |
| T-46 | US-015 | R-015-01..08, AC-015-01..09, EX-015-01..05, SR-10 | `backend/src/main/java/si/contactgroups/service/MembershipService`, `backend/src/main/java/si/contactgroups/web/MemberController` | T-44 |
| T-47 | US-015 | R-015-01, R-015-04, R-015-05 | `frontend/src/features/members/MembersPage` | T-46, T-06 |
| T-48 | US-017 | R-017-01..06, R-017-08..09, AC-017-01..06, AC-017-08..09, EX-017-01..05 | `backend/src/main/java/si/contactgroups/service/HistoryQueryService`, `backend/src/main/java/si/contactgroups/web/ActivityController` | T-18 |
| T-49 | US-017 | R-017-01..07, AC-017-07 | `frontend/src/features/activity/ActivityPage` | T-48, T-06 |
| T-50 | US-020 | R-020-01..04, R-020-06..08, AC-020-01..03, AC-020-05..07, EX-020-01..04, EX-020-06..07, D-18 | `backend/src/main/java/si/contactgroups/service/GroupService`, `backend/src/main/java/si/contactgroups/web/GroupController` | T-15 |
| T-51 | US-020 | R-020-05, AC-020-04, EX-020-05, AR-13 | `backend/src/main/java/si/contactgroups/job/TrashPurgeJob` | T-41, T-59 |
| T-52 | US-020 | R-020-01..02, R-020-04, R-020-07..08 | `frontend/src/features/groups/DeleteGroupDialog`, `frontend/src/features/groups/GroupTrashPage` | T-59, T-06 |
| T-53 | US-021 | R-021-01..05, AC-021-02..07, EX-021-01..02, EX-021-04, D-27 | `backend/src/main/java/si/contactgroups/service/AccountService`, `backend/src/main/java/si/contactgroups/web/MeController` | T-59 |
| T-54 | US-021 | R-021-02, R-021-06, AC-021-01, EX-021-03 | `frontend/src/features/account/AccountPage` | T-62, T-06 |
| T-55 | US-022 | R-022-01..07, AC-022-01..07, EX-022-01..05, D-19 | `backend/src/main/java/si/contactgroups/service/VersionService`, `backend/src/main/java/si/contactgroups/web/VersionController` | T-15, T-18 |
| T-56 | US-022 | R-022-02..04, R-022-06 | `frontend/src/features/versions/VersionsTab` | T-64, T-17 |
| T-57 | US-023 | R-023-01..06, AC-023-01..07, EX-023-01..04 | `backend/src/main/java/si/contactgroups/service/VersionService` | T-64 |
| T-58 | US-023 | R-023-03, R-023-04, R-023-06 | `frontend/src/features/versions/RestoreVersionButton` | T-66, T-65 |
| T-59 | US-024 | R-024-01..08, R-024-10, AC-024-01..08, AC-024-10..11, EX-024-01..06, D-19, EX-013-05 | `backend/src/main/java/si/contactgroups/service/DuplicateService`, `backend/src/main/java/si/contactgroups/web/DuplicateController` | T-14 |
| T-60 | US-024 | R-024-09, AC-024-09 | `backend/src/main/java/si/contactgroups/web/DuplicateController` | T-68 |
| T-61 | US-024 | R-024-01, R-024-05, R-024-10 | `frontend/src/features/duplicates/DuplicatesPage` | T-68, T-06 |
| T-62 | US-025 | R-025-01..05, AC-025-01..05, EX-025-01..04 | `backend/src/main/java/si/contactgroups/service/DuplicateService`, `backend/src/main/java/si/contactgroups/persistence/DismissalRepository` | T-68 |
| T-63 | US-025 | R-025-01, R-025-04 | `frontend/src/features/duplicates/DismissButton` | T-71, T-70 |
| T-64 | US-026 | R-026-01, R-026-02, AC-026-01, EX-026-02..03 | `frontend/src/features/duplicates/MergePage`, `frontend/src/features/duplicates/preselect.ts` | T-70, T-15 |
| T-65 | US-026 | R-026-03, R-026-04, AC-026-02..03, EX-026-04..05, EX-026-08 | `frontend/src/features/duplicates/MergePage`, `backend/src/main/java/si/contactgroups/service/MergeService` | T-73 |
| T-66 | US-026 | R-026-05..06, R-026-08..09, R-026-12, AC-026-04..06, AC-026-08..09, AC-026-11, EX-026-06..07, EX-026-09, D-19 | `backend/src/main/java/si/contactgroups/service/MergeService`, `backend/src/main/java/si/contactgroups/web/MergeController` | T-74, T-37 |
| T-67 | US-026 | R-026-07, AC-026-07, EX-026-01 | `backend/src/main/java/si/contactgroups/service/MergeService`, `backend/src/main/java/si/contactgroups/service/HistoryQueryService` | T-75, T-18 |
| T-68 | US-026 | R-026-10, R-026-11, AC-026-10, AC-026-12 | `frontend/src/features/duplicates/MergePage`, `frontend/e2e/e2e-05.spec.ts` | T-76, T-39 |

### NFR and operations tasks

Every `NFR` check of `docs/02_specification.md` has a task (increment shown in the title).

| Task | Story | Covers | Expected files | Depends on |
|---|---|---|---|---|
| T-69 | INC-1 | NFR-01, NFR-02, NFR-03, NFR-04 | `perf/measure.mjs`, `seed/generate-large.sql` | T-36, T-32 |
| T-70 | INC-1 | NFR-06, DoD-P07 | `scripts/backup.sh`, `scripts/restore.sh` | — |
| T-71 | INC-1 | NFR-05, NFR-12 | `docker-compose.prod.yml` | — |
| T-72 | INC-1 | NFR-07, NFR-08, NFR-09 | `frontend/e2e/*` | — |
| T-73 | INC-1 | NFR-10, NFR-11 | `frontend/e2e/*` | — |
| T-74 | INC-1 | NFR-13, SB-08 | `.github/workflows/ci.yml` | — |
| T-75 | INC-1 | NFR-14, BR-07 | `backend/src/test/**/ConcurrentSaveIT` | T-15 |
| T-76 | INC-1 | NFR-15, DoD-P04 | `docs/openapi.json`, `docs/install-and-backup.md`, `docs/user-guide-sl.md` | — |
| T-77 | INC-1 | SR-09, SR-13, DoD-P06 | `.github/workflows/ci.yml` | — |
| T-78 | INC-2 | DoD-P02, DoD-P05, SR-01 | `backend/src/test/**/IsolationIT`, `frontend/e2e/*` | T-11, T-77 |

## Stories per increment (check)

| Story | Increment | Tasks |
|---|---|---|
| US-001 | 1 | T-01..T-04 (4) |
| US-002 | 1 | T-05..T-06 (2) |
| US-018 | 1 | T-07..T-11 (5) |
| US-019 | 1 | T-12..T-13 (2) |
| US-003 | 1 | T-14..T-17 (4) |
| US-016 | 1 | T-18..T-19 (2) |
| US-004 | 1 | T-20..T-21 (2) |
| US-005 | 1 | T-22..T-23 (2) |
| US-006 | 1 | T-24..T-25 (2) |
| US-007 | 1 | T-26..T-27 (2) |
| US-008 | 1 | T-28..T-29 (2) |
| US-009 | 1 | T-30..T-31 (2) |
| US-010 | 1 | T-32..T-33 (2) |
| US-011 | 1 | T-34..T-36 (3) |
| US-012 | 1 | T-37..T-39 (3) |
| US-013 | 1 | T-40..T-42 (3) |
| US-014 | 1 | T-43..T-45 (3) |
| US-015 | 1 | T-46..T-47 (2) |
| US-017 | 1 | T-48..T-49 (2) |
| US-020 | 2 | T-50..T-52 (3) |
| US-021 | 2 | T-53..T-54 (2) |
| US-022 | 2 | T-55..T-56 (2) |
| US-023 | 2 | T-57..T-58 (2) |
| US-024 | 2 | T-59..T-61 (3) |
| US-025 | 2 | T-62..T-63 (2) |
| US-026 | 2 | T-64..T-68 (5) |
