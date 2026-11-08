# Architecture

> For: agent (reads), human (reviews) · Owner: Architect · Agent: read-only

Source: Q-24, Q-51, Q-52, Q-39. Everything below follows from the stack the customer required (Spring Boot, React, PostgreSQL, Firebase, Excel) and the deployment answer of Q-52; where the interview gives no detail, the item is marked as an assumption.

## Components

Each component gets its own folder and README in `02_output/`. Versions come only from `tech-stack.md`; reference its ids, never repeat a version here.

```yaml
components:
  - name: backend
    type: api
    folder: 02_output/backend
    platform: java
    build: maven
    bootstrap: Maven project with the Maven Wrapper, written by hand from the Spring Initializr layout; root package `si.contactgroups` [ASSUMPTION]
    commands:          # ES-05; filled in phase 2 if left empty
      build: ./mvnw -B -DskipTests package
      test: ./mvnw -B verify
      check: ./mvnw -B verify -Pquality
      run: ./mvnw -B spring-boot:run
    depends_on: []
    deploys_as: container image
  - name: frontend
    type: web-app
    folder: 02_output/frontend
    platform: node
    build: vite
    bootstrap: Vite React TypeScript project written from the `react-ts` template layout; Tailwind CSS and shadcn/ui configured as UI-01 asks
    commands:
      build: npm run build
      test: npm test
      check: npm run check
      run: npm run dev
    depends_on: [backend]
    deploys_as: container image (static files behind a web server)
```

The local stack is started with `02_output/docker-compose.yml`: PostgreSQL, the backend, the frontend and (for development and tests) the Firebase Local Emulator Suite. The database is not a component with its own code: its schema lives in the backend as Flyway migrations (AR-04).

## Constraints

Checkable rules the code must respect (layering, allowed dependencies, patterns). Checked automatically in phase 6 where the stack allows (DoD-04).

| ID | Constraint |
|---|---|
| AR-01 | The backend has the layers web (REST controllers), service (business rules) and persistence (repositories). Controllers call services only; only repositories touch the database; no layer calls a layer above it. An architecture test checks the package dependencies. |
| AR-02 | Every repository method that reads or writes Group data takes the Group id as a parameter and filters by it; the Group id comes from the request path only (US-018 R-018-04). A test lists all repository methods and fails when one of them lacks the Group id. |
| AR-03 | The frontend reaches data only through the REST interface of the backend and Firebase Authentication; it holds no business rule that the backend does not also enforce. |
| AR-04 | The database schema changes only through Flyway migrations in the backend; no schema is created by the application at start-up. |
| AR-05 | All database access uses parameterised statements; no SQL is built by joining user input. A static-analysis rule checks it. |
| AR-06 | The backend checks every Firebase ID token with the Firebase Admin SDK and builds authorisation (BR-01, BR-02) itself in one place that every endpoint passes. A test checks that no endpoint except the health check is reachable without it. |
| AR-07 | All configuration comes from environment variables (`environments.md`); no value, host or secret is written in code or in a committed file. |
| AR-08 | All user-visible texts of the frontend come from the message catalogs of both languages, never from code (UI-30); error messages of the backend are machine-readable codes that the frontend translates. |
| AR-09 | The History is append-only at the code and the database level: no repository method updates or deletes a History entry, and the database user of the application has no `UPDATE` or `DELETE` right on that table, except that the permanent-deletion job (BR-04, US-013 R-013-08) may replace the values in the `details` of entries of a deleted Contact and US-021 may clear the e-mail of a deleted User [ASSUMPTION]. |
| AR-10 | Times are stored in UTC and converted to Europe/Ljubljana only for display and for calendar-day filters (business-rules, project-wide facts). |
| AR-11 | Every dependency has a permissive open-source licence (`tech-stack.md`); no code of the SCM example solution is used. |
| AR-12 | The backend and the frontend are delivered as container images that start from environment variables alone, with no dependency on one cloud provider (Q-52). |
| AR-13 | The permanent deletion of Contacts and Groups from Trash (BR-04) runs as a scheduled job inside the backend [ASSUMPTION], not as a manual step of the operator. |

## Interfaces

Every interface between components or with external systems is defined as a contract in `docs/02_contracts/` in phase 2.

| Interface | Between | Style (e.g. REST, gRPC, events, file, UI) |
|---|---|---|
| Application interface | frontend → backend | REST over HTTPS, JSON, described by OpenAPI; every endpoint except the health check requires a Firebase ID token (Q-39) |
| Sign-in | frontend → Firebase Authentication | Firebase client SDK: e-mail with password, Google, verification and reset e-mails (Q-47) |
| Token check | backend → Firebase | Firebase Admin SDK verifies the ID token with Firebase's public keys |
| Persistence | backend → PostgreSQL | JDBC with Flyway migrations (Q-51) |
| Excel export | backend → user's browser | `.xlsx` file produced with Apache POI and sent as a download (Q-36) |
| User interface | user → frontend | Responsive web UI in Slovene and English (US-019) |
