# ADR-08: History entries

- Status: accepted (S3 /plan)
- Drivers: BR-03, P-3, R-016-01..08, R-013-06/08, R-021-03, D-18(e)(f), D-19(a)

## Decision

Table `history_entry`: `id`, `group_id`, `occurred_at` (UTC), `user_id` (no foreign key, survives account deletion), `user_name`, `user_email` (copied at the time), `action` (enum text from the BR-03 closed set, D-18e), `contact_id` (no foreign key; survives permanent deletion), `details` JSONB. `details` has a fixed shape per action (changes `{field: {old, new}}`, tag name, attribute key and values, merged Contact ids, export count, Roles). The service writes entries only through `HistoryWriter`, in the same transaction as the audited change, so a failed action leaves no entry and a successful one always has one. Permanent deletion of a Contact (job or Owner) replaces every value inside `details` of its entries with the text `removed` and writes one `CONTACT_DELETED_FOREVER` entry without personal data (user name `system` for the job). Account deletion sets `user_email` to NULL in the entries of that User (display of the name stays). Group-wide listing filters by user, action and calendar-day range in Europe/Ljubljana converted to UTC bounds.

## Consequences

- No history entry for Group deletion/restore or for the end of a Membership by account deletion (D-18e).
- `details` is personal data: exports and logs never print it.
