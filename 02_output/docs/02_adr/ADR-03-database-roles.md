# ADR-03: Database roles and an append-only History

- Status: accepted (S3 /plan)
- Drivers: AR-09, AR-04, SR-03, BR-03, R-013-08, R-016-05, R-020-05, R-021-03

## Context

The History must be append-only "at the code and the database level" (AR-09). Exceptions: the permanent-deletion job replaces values in `details` of entries of a deleted Contact (R-013-08), US-021 clears the e-mail of a deleted User (R-021-03), and a permanently deleted Group takes its History with it (R-020-05).

## Options

1. One database user for migrations and application; rely on code discipline.
2. Two roles: an owner role for Flyway and a restricted application role with column-level grants.
3. Database triggers that reject UPDATE and DELETE on `history_entry`.

## Decision

Option 2 plus a check in tests. `DB_MIGRATION_USER` (owner, used only by Flyway at start-up) creates all objects. `DB_USER` (the application) gets `SELECT, INSERT, UPDATE, DELETE` on ordinary tables, but on `history_entry` only `SELECT, INSERT` and `UPDATE (details, user_email)`; no `DELETE`, no `CREATE` on the schema. `history_entry.group_id` references `contact_group` with `ON DELETE CASCADE`: referential-integrity actions run with the table owner's rights, so deleting a Group removes its History without a `DELETE` grant. `contact_version` and all child tables cascade the same way. A Testcontainers test connects as the application role and shows: UPDATE of `action` fails, DELETE fails, deleting a Group still removes its entries, `CREATE TABLE` fails.

## Consequences

- Two sets of credentials in configuration (new keys, decision D-29).
- Flyway must run as the owner; the application pool uses the restricted role.
- The "removed" replacement can touch only `details` and `user_email`; a code error cannot rewrite time, action or user name.
