# ADR-09: Scheduled Trash purge

- Status: accepted (S3 /plan)
- Drivers: BR-04, AR-10, AR-13, R-013-04, R-020-05, D-26

## Decision

`TrashPurgeJob` (package `job`) runs on `TRASH_PURGE_CRON` (default hourly) inside the backend using Spring `@Scheduled` and an injected `Clock`. It selects Contacts and Groups with `deleted_at <= now - 30 days`, and purges each in its own transaction (one failure does not stop the others): Contact -> History values replaced by `removed` (ADR-08), Versions and Contact rows deleted, final entry written; Group -> row deleted, children cascade (ADR-03). With an hourly run an item is deleted at most about one hour after day 30, inside the "at most 24 hours" of D-26. In a deployment with several backend instances the job takes a PostgreSQL advisory lock so it runs once. The job logs counts only.

## Consequences

- Tests set the Clock and call `purgeOnce()`; no sleeping.
- Backups keep purged data up to 14 days (security-requirements, personal data).
