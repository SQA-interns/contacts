---
name: fix
description: Session S8, phase 6. Fix the open findings of the last verification, one finding per commit with its re-run evidence, without touching frozen tests or anything that is not a finding.
disable-model-invocation: true
---

> For: agent · Owner: Team lead · Agent: read-only

## Session

- Id: S8, phase 6, after each `/verify` with open findings. Frame: generate.
- Reads: the open findings in the last section of `docs/06_verification-report.md`; the code, specification sections and rules they cite; `general/testing/writing-tests.md`; `general/rules/commits.md`; for a UI finding `skills/build-ui/SKILL.md`.
- Writes: production code, non-frozen tests, the resolution column of `docs/06_verification-report.md`, `docs/decisions-log.md`, `docs/00_progress.md`.
- Must not: change frozen tests; fix anything that is not a finding; look for the hidden tests.
- Exit: every open finding has a fix commit or a decision.

## Procedure

1. Critical and High first. For each finding: a failing test that shows it (red), the fix (green), the full suite and the check that found it. Commit each fix alone (`fix: … (F-nn)`, with its `Red:` line).
2. A hidden-test finding gives only the rule and a case category: re-read the rule, write your own visible test for that category with values from the inputs, and make the rule hold for every such case, not only your test.
3. A finding that needs a change to a frozen test, the specification or an input is a decision; record it and go on.
4. Medium and Low: fixed, accepted with a reason (non-blocking decision), or an open item for the release notes.
5. Fill only the resolution column; commit; hand over (next: `/verify`).
