# ADR-05: Authorisation in one place

- Status: accepted (S3 /plan)
- Drivers: BR-01, BR-02, SR-01, SR-03, AR-06, D-18(a)(c), R-018-02..06

## Context

Every Group endpoint needs the same checks in the same order. The permission matrix (BR-02) has about a dozen actions. A forgotten check on one endpoint breaks tenant isolation (P-1).

## Options

1. Check inside every service method.
2. A custom annotation on controller methods, evaluated by one interceptor.
3. Spring Security method security with SpEL.

## Decision

Option 2. `Action` is an enum (`READ_CONTACTS`, `EDIT_CONTACT`, `DELETE_CONTACT_FOREVER`, `MANAGE_MEMBERS`, ...) with its allowed Roles in one table that mirrors the BR-02 matrix. Each Group endpoint has `@RequiresAction(Action.X)`. `GroupAccessInterceptor` takes `groupId` from the path only (R-018-04) and applies, in this order (D-18c): token (401, filter) -> verified e-mail (403) -> Membership of a Group that is not in Trash (404, also for a Group in Trash, D-18a) -> Role (403). Objects named in the path (Contact, Tag, Invitation, Version) are loaded with the Group id and give 404 when they belong to another Group (R-018-05). The Owner-only Group Trash endpoints (`/group-trash`) have their own annotation and answer 404 to everyone who is not an Owner of that Group. A reflection test lists every mapped handler and fails when one has neither `@RequiresAction` nor `@PublicEndpoint` (only `/health`).

## Consequences

- The matrix lives in code once and is also published in the specification.
- Services trust the interceptor but still take `groupId` in every repository call (AR-02).
- A request body never names the Group; unknown body properties are rejected (R-018-04).
