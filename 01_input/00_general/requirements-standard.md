# Requirements standard

> For: agent · Owner: Product owner · Agent: read-only

How every requirement in `project/01_requirements/` is written, so that a human and an agent read it the same way. Based on ISO/IEC/IEEE 29148 (requirement quality), SWEBOK v4 (traceability, validation), EARS (rule syntax), user stories with 3C and INVEST, and Given/When/Then acceptance criteria (ATDD/BDD). The goal is not more text, but fewer silent decisions.

## Ubiquitous language

The glossary in `business-rules.md` is the one vocabulary of the project (domain-driven design). Every story, rule, criterion, the specification, the code, the API, the database, the tests and the UI copy use its terms, with the same meaning, everywhere.

- Each term has one name, a meaning, the name it takes in code, and the synonyms not to use.
- A story uses glossary terms, never a synonym; a term used in two stories means the same in both.
- A new domain term found later is added through a decision, not invented in code.

## Where each kind of requirement lives

| Kind | File | Written as |
|---|---|---|
| Why the product exists, users, MVP, scope and non-goals, principles, product open questions, project done criteria | `intent.md` | prose, lists, tables |
| One user need | `stories/US-nnn.md`, one file per story | the story structure below |
| Facts that hold for the whole project (time zone, currency, tax, formats, language), a rule shared by several stories, the data model, the glossary | `business-rules.md` | one facts table, EARS, tables |
| User journeys through the whole system | `e2e-scenarios.md` | table |
| Quality, security, architecture, UI | `project/02_design/*` | tables |

Never write the same fact in two places; refer to its ID instead.

## Story structure (`stories/US-nnn.md`)

Front matter, then these sections in this order. A section that does not apply says "None."

| Section | Content | Main author |
|---|---|---|
| front matter | `id`, `title`, `priority` (must, should, could, won't), `mvp` (true/false), `estimate` (points), `depends_on` (story ids), `rules` (BR ids), `sources` (Q ids) | to-spec |
| Story | "As a <role>, I want <goal>, so that <benefit>." | customer, via interview |
| Purpose | Why this story exists and what goes wrong without it, in the customer's terms. | customer, via interview |
| Definitions | Terms specific to this story (shared terms stay in the glossary). | both |
| Rules | `R-nnn-nn`, one EARS statement each. | to-spec |
| Acceptance criteria | `AC-nnn-nn`, Given / When / Then, each citing the rules it confirms. | to-spec, confirmed by customer |
| Edge cases | `EX-nnn-nn`, a table input → expected result: the test oracle for boundaries, errors and lifecycle. | both |
| Out of scope | What this story does not do and the agent must not add. | to-spec |
| Open questions | `OQ-nnn-nn`, with owner, due date and answer. | customer, via interview |

## EARS patterns for rules

| Pattern | Form |
|---|---|
| Ubiquitous | The system shall <response>. |
| Event-driven | When <trigger>, the system shall <response>. |
| State-driven | While <state>, the system shall <response>. |
| Unwanted behaviour | If <unwanted condition>, then the system shall <response>. |
| Optional feature | Where <feature is included>, the system shall <response>. |
| Complex | While <state>, when <trigger>, the system shall <response>. |

One statement = one testable rule = one row of traceability. If the trigger, condition or response cannot be written without guessing, the rule is `OPEN`.

## Acceptance criteria

- One observable behaviour each, Given / When / Then, with concrete values.
- Observable through a public interface (UI, API, CLI, messages, files), never through internals.
- A rejection states what must not happen as well (nothing stored, nothing sent).
- Cover the main flow, every `If … then` rule and every boundary in the edge-case table.

## Quality of each requirement (ISO/IEC/IEEE 29148)

Every rule and acceptance criterion is: **appropriate** (right level of abstraction, no design decision), **singular** (one requirement), **unambiguous** (one reading), **verifiable** (a test can be written), **necessary**, **complete**, **correct** (matches the customer's need), **conforming** (follows this standard) and **feasible**.

## Quality of each story (INVEST)

**Independent**, **negotiable** (states intent, not implementation), **valuable** to a user, **estimable**, **small** (fits one implement session; at most a third of an increment) and **testable**. A story is a vertical slice: it works through every layer it needs.

## Splitting a story that is too large

Split by: workflow steps (by goal), operations (create, read, update, delete), business-rule variations, data variations, interfaces, simple then complex, main effort then additions, deferred quality attributes, or a spike when the implementation is unknown. Every resulting story must still meet INVEST.

## Typical gaps to close

Clarify only what changes system behaviour or a business outcome when read differently:

- Events and time: which event and which timestamp decides, in which time zone, until when.
- Exceptions and lifecycle: cancel, repeat, failed validation, concurrent submissions at a boundary, changes after the fact.
- Business rules: tax, prices and discounts and whether they combine, limits and quotas, roles that may do what.
- Data: required fields, formats, retention, who sees what.

## Traceability

`Q` (interview) → `US` → `R` → `AC` / `EX` → test → commit. Every test names the `AC`, `EX` or `R` it checks; every value in a test has a source in a story, a business rule or `business-rules.md` (project-wide facts).
