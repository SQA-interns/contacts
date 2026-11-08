# Tech stack

> For: agent (reads), human (reviews) · Owner: Architect · Agent: read-only

The single source of truth for every platform, dependency and tool version.
Build manifests and lock files in `02_output/` (e.g. `pom.xml`, `package.json`, `build.gradle`, `*.csproj`, `Podfile`) must match this file exactly; the phase 0 gate checks it. Resolvability and vulnerability results are recorded by the agent in `docs/00_preflight-report.md`.

## Rules

- Exact versions only: no ranges, wildcards, `latest` or floating tags.
- Changing any entry below is a blocking decision (`general/rules/decisions.md`). The human's approval in the decision record replaces the entry; nobody edits this file mid-run.
- A pin that resolves and runs but whose tool reports another version (for example an image tag) is a non-blocking decision; the listed pin stays authoritative. A pin that does not resolve or run is blocking.
- A dependency or tool not listed here may be added only with an exact version and a non-blocking decision record; it is scanned in phase 6 like any other.
- Every dependency's licence must be compatible with the project's licence.
- A `platforms` or `tooling` entry may carry `version_check` (a command whose output contains the version) and, for `tooling`, `options` (flags, analysers or rule sets to enable or disable, for example analysers that need credentials not listed in `secrets.env.example`). Preflight uses them.
- `tooling` must include at least one tool for each purpose required by `general/quality/` and `general/security/`: build, format, lint, type check (if the language has one), test, coverage, mutation, static analysis, dependency scan, secret scan; and for a component with a user interface: the UI stack of `general/design/ui-baseline.md` (UI-01), a browser test runner with screenshots, and an accessibility checker.
- A version proposed by `/to-spec` and not yet verified is marked `[ASSUMPTION] verify in preflight`; `/preflight` confirms or replaces it through a decision.

Project licence: <licence>. Allowed dependency licences: <list>.

Versions verified to resolve on <YYYY-MM-DD>. <!-- notes on managed versions and CVE overrides -->

## Platforms

```yaml
platforms:
  # - { id: <id>, name: <name>, version: <exact version>, source: <url> }
```

## Dependencies

One entry per artifact, written the way the ecosystem writes coordinates.

```yaml
dependencies:
  # <component>, <scope> (<registry>: <url>)
  # - { id: <coordinates>, ecosystem: <maven | npm | container | …>, version: <exact version>, scope: <build | runtime | test>, component: <component | all>, source: <registry>, purpose: <purpose>, license: <SPDX id> }
```

## Tooling

```yaml
tooling:
  # <component>
  # - { id: <id>, ecosystem: <ecosystem>, version: <exact version>, purpose: <build | format | lint | type-check | test | coverage | mutation | static-analysis | dependency-scan | secret-scan | duplication | code-metrics>, component: <component | all>, source: <registry> }
```
