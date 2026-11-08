# ADR-02: Persistence with Spring JDBC and Flyway

- Status: accepted (S3 /plan)
- Drivers: AR-02, AR-04, AR-05, ES-08, NFR-01, NFR-14

## Context

PostgreSQL 17 holds all data. `tech-stack.md` lists `spring-boot-starter-jdbc`, Flyway and the PostgreSQL driver, and no JPA or ORM. SQL must be parameterised (AR-05) and the schema changes only by migrations (AR-04).

## Options

1. Spring `JdbcClient` with named parameters and hand-written SQL, `RowMapper` per entity.
2. Spring Data JPA/Hibernate (not in the stack; would need a new pinned dependency and a scan).
3. jOOQ (new dependency, code generation).

## Decision

Option 1. Repositories are classes in `persistence` that use `JdbcClient` only with named parameters; a SQL string is never built from user input (dynamic list queries assemble only fixed fragments chosen by code and bind every value). Schema comes from `src/main/resources/db/migration/V<n>__<slug>.sql`; Hibernate-style schema creation does not exist. Tests use Testcontainers with the same migrations (no H2). `spring.flyway.clean-disabled=true`.

## Consequences

- Full control of the SQL needed for filters, sorting and normalised search (ADR-06); no lazy-loading surprises.
- More hand-written mapping code; kept short by small record types.
- SpotBugs with find-sec-bugs checks AR-05; ArchUnit forbids `Statement` and string-built `JdbcTemplate.execute`.
