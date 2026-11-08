# Architecture

> For: agent (reads), human (reviews) · Owner: Architect · Agent: read-only

## Components

Each component gets its own folder and README in `02_output/`. Versions come only from `tech-stack.md`; reference its ids, never repeat a version here.

```yaml
components:
  - name: <component>
    type: <api | web-app | cli | library | …>
    folder: 02_output/<component>
    platform: <platform id from tech-stack.md>
    build: <tooling id from tech-stack.md>
    bootstrap: <how the project is created (generator or hand-written manifest), root package or module>
    commands:          # ES-05; filled in phase 2 if left empty
      build:
      test:
      check:
      run:
    depends_on: []
    deploys_as: <container image, package, …>
```

<!-- How the local stack is started (for example `02_output/docker-compose.yml`). -->

## Constraints

Checkable rules the code must respect (layering, allowed dependencies, patterns). Checked automatically in phase 6 where the stack allows (DoD-04).

| ID | Constraint |
|---|---|
| AR-01 | |

## Interfaces

Every interface between components or with external systems is defined as a contract in `docs/02_contracts/` in phase 2.

| Interface | Between | Style (e.g. REST, gRPC, events, file, UI) |
|---|---|---|
| | | |
