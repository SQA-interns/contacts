# ADR-07: Optimistic concurrency and Versions

- Status: accepted (S3 /plan)
- Drivers: BR-07, NFR-14, P-2, R-003-06/07, R-022-01, D-19(a)(b)(i), R-023-01

## Context

Two users may edit one Contact. A save with an old version number must store nothing and show the current Contact. Every save also makes a stored Version (US-022).

## Decision

The Contact row has `version integer`. Save = one transaction: `UPDATE contact SET ..., version = version + 1 WHERE id = :id AND group_id = :groupId AND version = :sent AND deleted_at IS NULL`; zero rows -> 409 `contact.changed_by_someone_else` with the current Contact in the body, nothing stored. In the same transaction the service replaces the Contact's Tags and Attributes (one save covers fields, Tags and Attributes, BR-07, R-004-07, R-005-08), inserts one `contact_version` row (`contact_id`, `number`, `created_at`, `user_id`, `snapshot` JSONB with fields, Tag names, Attributes) and the History entries of D-19a: one `CONTACT_EDITED` entry for changed fields, one entry per Tag change and one per Attribute change, all with the same time and user. A save that changes nothing stores nothing (R-003-11). A Tag rename or delete for the Group changes no version number (BR-07, D-19b); the Versions comparison uses the live Contact as "current". Restoring a Version is a normal save with the old content (R-023-01).

## Consequences

- No pessimistic locks; clients retry with the new version.
- `contact_version` grows with every save; deleted with the Contact (R-022-07).
- The snapshot is JSONB, so old Versions stay readable if columns are added later.
