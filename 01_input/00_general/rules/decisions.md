# Decisions

> For: agent · Owner: Team lead · Agent: read-only

## Decision classes

Every choice the inputs do not settle falls into one class. Classify before acting.

| Class | When | What to do |
|---|---|---|
| **Allow** | The value, rule or limit is written in the inputs, or follows directly from them or from an approved decision. | Use it; cite its source (`US-001 R-02`, `business-rules.md` (project-wide facts)). |
| **Open** | An event, boundary, exception or other choice has more than one reasonable reading, and none of them is a `Block` topic. | Choose the most conservative reading, record a non-blocking decision marked "pending review", and continue. |
| **Block** | The choice has business consequences: price, payment, discount, rights or access, personal data, security, legal or compliance, cancellation or deletion, anything irreversible. | Never guess. Record a blocking decision, ask, and end the session if there is no answer. |

- A value used in code or tests that has no source in the inputs is an `[ASSUMPTION]`: write it in the decision log with its class. A generated test whose expected value has no source is not written.
- The intake marks unresolved items `OPEN` and unconfirmed values `[ASSUMPTION]`. Phase 1 turns every one of them into Allow (confirmed), a decision, or a blocker.

## Ask the human

Record a decision in `docs/decisions-log.md` (format below), ask, and end the session if the human does not answer in it.

**Blocking** (an answer is needed to go on):

- a `Block` topic is not settled by the inputs;
- anything checked in phase 0 is missing or wrong and only a human can fix it;
- a technology, version or service from `project/00_setup/tech-stack.md` does not work or needs changing;
- an input needs to change.

**Approval** (the human must accept a result): the wireframes; a change to a frozen test or its manifest; lowering or accepting a Critical or High finding; a fourth verify/fix round.

Ask all open questions of a session in one message, each with numbered options and a proposed default. *Why:* one round trip instead of many, and a default the human can accept with one word. While waiting, finish work that does not depend on the answer.

**Non-blocking**, record and continue: a finding listed in `project/00_setup/run-config.md` ("Pre-approved findings") whose evidence matches (cite its ID, apply its action, do not ask); a pinned tool that runs but reports another version; replacing a pin marked `[ASSUMPTION] verify in preflight` by the latest stable patch of the same minor version with no known Critical or High vulnerability (a different major or minor version, or a different technology, is blocking); a scanner analyser disabled for lack of credentials; dev-only tooling added under the `tech-stack.md` rule.

## Decision record

Append each decision to `docs/decisions-log.md` in this format:

```
## D-nn: <short title>
- Timestamp:
- Session: <id> <skill>
- Class: allow | open | block
- Type: blocking | non-blocking | approval
- Trigger: <what happened; cite IDs and files>
- Options: <numbered alternatives; mark the proposed default>
- Human response: <answer and time, or "none">
- Resolution: <chosen option> | pending review
- Overrides: <ID, only when the decision replaces a rule or input entry>
```

- Never delete or rewrite a record; add a follow-up record instead.
- The phase 7 gate fails while any record lacks a resolution.
