# ADR-01: Backend internal structure

- Status: accepted (S3 /plan)
- Drivers: AR-01, AR-02, AR-06, AR-07, ES-11, ES-13

## Context

The backend is one Spring Boot service (`backend`, root package `si.contactgroups`). AR-01 demands three layers with one-way dependencies; AR-02 demands that every Group-scoped repository method takes the Group id. Several sessions will add code, so the structure must be fixed now.

## Options

1. Layer-first packages (`web`, `service`, `persistence`, ...), one sub-package per aggregate inside each layer.
2. Feature-first packages (`contact`, `group`, ...) with layers inside each feature.
3. Hexagonal (ports and adapters) with separate modules.

## Decision

Option 1. Packages under `si.contactgroups`:

| Package | Content | May depend on |
|---|---|---|
| `web` | REST controllers, request/response records, `ApiExceptionHandler`, `@RequiresAction` interceptor | `service`, `domain`, `security` (annotations only) |
| `service` | business rules, transactions, audit writing | `persistence`, `domain` |
| `persistence` | repositories (`JdbcClient`), row mappers | `domain` |
| `domain` | records, enums (`Role`, `Action`), validation helpers, `Normaliser` | nothing |
| `security` | token filter, `AuthenticatedUser`, rate limit, size and HTTPS filters | `service` (user lookup), `domain` |
| `config` | `@Configuration`, typed properties from environment | any |
| `job` | scheduled jobs (Trash purge) | `service` |

Controllers call services only; only repositories touch the database; no layer calls a layer above it (ArchUnit `layeredArchitecture`). Group-scoped repository methods have `UUID groupId` as their first parameter; repositories without a Group scope carry the marker `@GlobalScope` (users, the Group list of a User, the purge job) and are listed in the specification.

## Consequences

- Simple to test by layer; ArchUnit rules are short.
- A feature touches four packages; accepted because the product has one deployable and one team.
- New code that needs a layer shortcut must change this ADR first.
