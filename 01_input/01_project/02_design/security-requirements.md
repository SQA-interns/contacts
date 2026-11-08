# Security requirements

> For: agent (reads), human (reviews) · Owner: Security officer · Agent: read-only

Adds to `general/security/security-baseline.md` (SB-01 … SB-14); do not repeat those here.

## Verification standard

| Component | Standard | Level | Reason if above the default |
|---|---|---|---|
| backend | OWASP ASVS 5.0 | Level 2 [ASSUMPTION] | The system stores personal data of many people and separates the data of different tenants (Groups); one leak breaks the main promise (principle P-1). Level 2 is the usual level for this. |
| frontend | OWASP ASVS 5.0 | Level 2 [ASSUMPTION] | Same reason; the frontend shows the data and holds the sign-in session. |

The interview gives no level. Level 2 fits a multi-tenant system with personal data and no regulated special data; a Level 3 would need the answer to OQ-P05 (special categories of data).

## Authentication and authorization

- **Authentication:** Firebase, with e-mail and password, and "Sign in with Google"; no other providers and no multi-factor authentication in the MVP. A verified e-mail is required before any Group opens. Passwords follow Firebase's defaults (at least 8 characters). The server checks the ID token on every request except the health check (Q-47, Q-39).
- **Authorisation:** the product's own, per Group, with three Roles (Owner, Editor, Viewer) as in the permission matrix of `business-rules.md` (BR-02). A non-Member gets "not found"; a Member with too low a Role gets "forbidden" (BR-01).
- **No in-app administrator:** nobody sees a Group of which they are not a Member; operators work only with the infrastructure and the database (Q-27).
- **Users and roles:** see `intent.md` (Users).

## Personal data

| Data item | Purpose | Retention | Shown/exported to |
|---|---|---|---|
| Contact data (name, e-mail, phone, company, job title, address, country, notes, Attributes, Tags) | Keep the contacts of a project or company | Until the Contact is deleted; then 30 days in Trash; then deleted permanently (BR-04). Backups hold it for up to 14 more days (Q-50). Legal retention is open (OQ-012-01). | Members of the Group, according to Role; Owners and Editors may export it to Excel (US-011) |
| User account (Firebase identifier, e-mail, display name) | Sign in, show who did what | While the account exists; deleted with the account (US-021) | The User; other Members of shared Groups see name and e-mail (Member list, History) |
| History entries (user name and e-mail, old and new values of Contact fields) | Audit trail (Q-37) | As long as the Group exists; the name stays after the account is deleted (Q-47). After a Contact is deleted forever the values in its entries read "removed" (US-013); after the account is deleted only the name stays (US-021). Both answers are the developer's and wait for the customer (OQ-013-01, OQ-021-01). | Owners and Editors of the Group |
| Invitations (e-mail address) | Add a person to a Group | 14 days, then expired; removed with the Group | Owners of the Group |
| Server log (request time, path, status, User identifier) | Find abuse and errors (Q-48) | [ASSUMPTION] 30 days, set by the operator | Operators; contains no Contact data and no tokens (SR-07) |
| Database backups | Recover from loss | 14 days (Q-50) | Operators |

Legal matters (controller or processor role, data-processing agreement, privacy notice, consent) are handled by the customer outside the software (Q-46); the answers to OQ-P05 are open.

## Project requirements

| ID | Requirement |
|---|---|
| SR-01 | Group isolation: every endpoint that has a Group in its path answers 404 and no data to a User who is not a Member. An automated test covers every endpoint listed in the OpenAPI description, with a User of another Group (Q-48). |
| SR-02 | Every endpoint except the health check answers 401 without a valid Firebase ID token, and 403 to a User with an unverified e-mail (US-018). |
| SR-03 | The Role of a User is read from the database on every request and never from the request (token claims, body or headers). |
| SR-04 | All database access is parameterised (AR-05); a test sends injection strings such as `'; DROP TABLE contact; --` in every text field and finds no effect. |
| SR-05 | All user-entered text is shown escaped; a Contact named `<script>alert(1)</script>` appears as plain text (US-018 R-018-14). |
| SR-06 | Cell values of the Excel export that start with `=`, `+`, `-` or `@` are written as text (US-011 R-011-06). |
| SR-07 | The server logs failed token checks, 401, 403 and 404 responses and rate-limit hits, and never Contact data, tokens or passwords (US-018 R-018-13). |
| SR-08 | Rate limit of about 100 requests per User per minute, maximum request size 1 MB, HTTPS only, CORS limited to the origin of the front end (US-018). |
| SR-09 | Firebase and database credentials exist only in environment variables, in the developer's `.env`, never in code, the repository, logs or chat (Q-48; `general/rules/secrets.md`). |
| SR-10 | The last Owner of a Group cannot be removed or demoted; a Group always has an Owner (US-015 R-015-04). |
| SR-11 | At most 20 pending Invitations per Group, each expiring after 14 days; an unverified e-mail never gives access through an Invitation (US-014). |
| SR-12 | Error responses contain a machine-readable code and a short message, and never a stack trace or SQL (US-018 R-018-09). |
| SR-13 | Automated tests use the Firebase Local Emulator Suite; no real account or real Firebase project is used by tests (Q-49). |

## Overrides

None.
