# ADR-06: Case- and accent-insensitive comparison (BR-05)

- Status: accepted (S3 /plan)
- Drivers: BR-05, R-008-01, R-009-05, R-024-02, R-024-03, NFR-01, AR-04

## Context

Search, company filter, duplicate detection and unique names compare ignoring case, and for names, companies and search also ignoring accents (`Skerlj` = `Škerlj`). Groups hold up to 10,000 Contacts.

## Options

1. PostgreSQL `unaccent` extension with functional indexes.
2. Normalised copies of the text stored in `*_norm` columns, written by the service.
3. Filter in memory after loading the Group.

## Decision

Option 2. `Normaliser.normalise(String)` (package `domain`): trim, Unicode NFD, remove combining marks, map `đ`->`d`, `ß`->`ss`, lowercase with `Locale.ROOT`. The service writes `first_name_norm`, `last_name_norm`, `company_norm`, `email_norm` (lowercase only), `search_norm` (first name, last name, e-mail, company joined by a space) on every save. Search splits the normalised text into words and requires each word as `LIKE '%word%' ESCAPE '\'` against `search_norm`, with `%`, `_` and `\` escaped (R-008-06). Tag names, Attribute keys and Group names compare with `lower()` only (BR-05). Tests cover `Škerlj`, `Čosić`, `Đorđević`.

## Consequences

- No database extension; behaviour is unit-testable in Java.
- Writes must keep `*_norm` in step; a Flyway-free test compares every stored `*_norm` with `Normaliser`.
- A sequential scan inside one Group is enough for 10,000 rows (index on `group_id`); measured in NFR-01.
