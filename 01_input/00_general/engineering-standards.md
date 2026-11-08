# Engineering standards

> For: agent · Owner: Team lead · Agent: read-only

Apply to every component of every project.

| ID | Standard | Why |
|---|---|---|
| ES-01 | Every value that differs between environments comes from configuration; secrets have no default in code. | The same build runs in every environment; a secret in code leaks with every copy of it. |
| ES-02 | Secrets are never committed; `.env` is ignored by git; `project/00_setup/secrets.env.example` lists every key. | A committed secret stays in the history even after it is removed. |
| ES-03 | The repository has `.gitattributes` normalising text files to LF and `.gitignore` excluding build output, dependencies and local config. | Mixed line endings change hashes and diffs; build output in git hides real changes. |
| ES-04 | Dependency versions are exact and lock files are committed. | A floating version can change behaviour or bring a vulnerability without any change in the repository. |
| ES-05 | Each component offers one command each to build, test, check (format, lint, type check) and run; its README lists them. | One known command per task lets any person or session build and check the component without guessing. |
| ES-06 | Root README: purpose, components, quick start, documentation map. Component README: prerequisites, configuration (settings and secrets, with source), build, run, test, troubleshooting. | The product must be usable by someone who was not in the run. |
| ES-07 | Logs contain no secrets or personal data; errors shown to users contain no internal details. | Logs and error messages are read by people and tools that must not see secrets, personal data or internals. |
| ES-08 | Persistent data structures are versioned (migrations or equivalent). | Data outlives code; an unversioned schema change cannot be repeated or rolled back. |
| ES-09 | Deployable services expose health and readiness information. | Platforms restart or route around a service only if it reports its state. |
| ES-10 | Changes are limited to the task; do not refactor code outside the story being implemented. | Unrelated changes hide the change under review and break things nobody asked to touch. |
| ES-11 | Code is simple: it passes its tests, expresses intent through names, has no duplicated logic and the fewest elements needed; functions stay short and each module has one responsibility. | Simple code is cheaper to read, test, review and change, for people and agents alike. |
| ES-12 | Every user interface follows `general/design/ui-baseline.md` unless `project/02_design/ui-design.md` overrides a rule. | Without a baseline an agent builds unstyled, inconsistent screens, because nothing else asks for a design. |
| ES-13 | Code, APIs, database objects, tests and UI copy use the glossary terms of `project/01_requirements/business-rules.md` and their "Name in code"; no synonyms, no technical renames of domain concepts. | One word for one concept from the customer to the code keeps requirements, tests and code readable side by side. |
| ES-14 | The repository has a CI pipeline that runs every component's build, test and check commands (ES-05) and the security scanners on every push. | Checks that run only on one machine are skipped on the next; CI makes every change prove itself. |
