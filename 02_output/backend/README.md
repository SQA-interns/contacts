# Backend

> For: agent (writes), human (reads) · Written in: phase 2 skeleton, phase 7 final · Source: ES-06 · Agent: writes

Spring Boot 4.0.9 REST service of Contact Groups (Java 21, root package `si.contactgroups`). Design: `docs/02_specification.md`, `docs/02_adr/ADR-01..11`, contract `docs/02_contracts/openapi.yaml`.

## Prerequisites

JDK 21 (Temurin 21.0.8), Docker (Testcontainers starts PostgreSQL 17.6 and the Firebase Auth emulator for the integration tests). Maven is the wrapper `./mvnw` (Maven 3.9.11).

## Configuration

All values come from environment variables (ES-01); `docs/02_contracts/configuration.schema.json` is the list. Secrets are named in `01_input/01_project/00_setup/secrets.env.example`; the source of a value is `.env` (git-ignored) or the operator's environment.

| Variable | Needed | Default | Source |
|---|---|---|---|
| `DB_URL` | yes | none | JDBC address of PostgreSQL |
| `DB_MIGRATION_USER`, `DB_MIGRATION_PASSWORD` | yes | none | owner role for Flyway (Compose: `POSTGRES_USER`, `POSTGRES_PASSWORD`) |
| `DB_APP_USER`, `DB_APP_PASSWORD` | password yes | user `contactgroups_app` | restricted application role (D-29) |
| `FIREBASE_PROJECT_ID` | yes | none | Firebase project |
| `FIREBASE_AUTH_EMULATOR_HOST` | no | empty (real Firebase) | `host:port`; refused with profile `prod` |
| `CORS_ALLOWED_ORIGIN` | yes | none | origin of the frontend |
| `RATE_LIMIT_PER_MINUTE`, `MAX_REQUEST_SIZE`, `TRASH_PURGE_CRON`, `LOG_LEVEL`, `SERVER_PORT`, `REQUIRE_HTTPS` | no | 100, 1MB, hourly, INFO, 8080, false | see the schema |

## Commands (ES-05)

```bash
./mvnw -B -DskipTests package   # build: target/contact-groups-backend.jar
./mvnw -B verify                # test: unit tests (Surefire) and integration tests (Failsafe, needs Docker)
./mvnw -B verify -Pquality      # check: format (Spotless), Checkstyle, SpotBugs + find-sec-bugs, PMD, plus tests
./mvnw -B -Pquality spotless:apply   # fix formatting
./mvnw -B spring-boot:run       # run (set the variables above first)
```

## Troubleshooting

- `Failed to validate Maven distribution SHA-256` on macOS: the built-in `sha256sum` does not support `-c`. Put a GNU-compatible `sha256sum` first on `PATH` (for example `brew install coreutils`) or an executable shim `exec shasum -a 256 "$@"`.
- Integration tests need a running Docker daemon; the emulator test publishes port 9099 on localhost.
- The Admin SDK is configured without Firestore and Storage (D-12); do not make `FirebaseAuth` a Spring bean and do not call `listUsers` (ADR-04).
- `./mvnw verify -Pquality` ends with one red test, `EX-007-05` (F-08): the frozen test contradicts rule R-007-03 (D-68 i). It is a known limitation, not a broken checkout; the other 844 tests and every check pass. To see the checks without the tests: `./mvnw -B -Pquality spotless:check checkstyle:check spotbugs:check pmd:check`.
- The integration tests publish port 9099; stop any local stack that holds it (see the root README).

## Deploy

The backend is delivered as a container image, built from the Dockerfile in this folder (JRE 21, user 10001, health check on `/health`). The image contains no configuration or secret; it starts from the environment variables above, with profile `prod` (`REQUIRE_HTTPS=true`, `FIREBASE_AUTH_EMULATOR_HOST` refused).

```bash
docker build -t contact-groups-backend:1.0.0 .
```

Production needs one more file than the other environments: the Firebase service-account key (`GOOGLE_APPLICATION_CREDENTIALS`, role "Firebase Authentication Admin"), mounted read-only into the container by `../docker-compose.prod.yml`; without it the backend refuses to start (D-69). The database schema is created and migrated by Flyway on every start; migrations only move forward, so go back by restoring a backup (`../scripts/restore.sh`). Start, configuration names, backup and update: [`../docs/install-and-backup.md`](../docs/install-and-backup.md).
