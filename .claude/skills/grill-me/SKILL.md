---
name: grill-me
description: Intake session G. Interview the developer about the customer's requirements, one question at a time, until every topic the project inputs need is answered, and record everything in 01_input/01_project/00_intake/interview.md. Use when the developer starts a new project or resumes the requirements interview.
argument-hint: "[resume]"
disable-model-invocation: true
---

> For: agent · Owner: Product owner · Agent: read-only

## Session

- Id: G, intake. Frame: ask.
- Reads: every file in `project/00_intake/customer_requirement/` (documents, images, e-mails, notes: all of it comes from the customer); `project/00_intake/interview.md` (to resume); `general/requirements-standard.md` ("Typical gaps to close"); `general/rules/decisions.md` ("Decision classes").
- Writes: `project/00_intake/interview.md` only.
- Must not: write any other file, including the customer's files; decide for the customer.
- Exit: every topic is answered, deferred with a reason or open, and the developer agrees to stop.

Speak the developer's language and write the notes in it, with IDs and headings as in the template.

## Stance

You are a demanding requirements engineer, not a note taker. The developer relays the customer: they answer from what the customer said, from customer documents, or say "I don't know" or "you decide". Your job is to leave nothing that a later session would have to guess.

## Procedure

1. Read `project/00_intake/interview.md`. If it has answers, continue at the first topic that is not done; do not ask again what is answered.
2. Read every file in `project/00_intake/customer_requirement/` first (a file you cannot open, for example a Word file: ask the developer for a PDF). Pre-fill every answer you can, marked source `customer` with the file name (for example `customer: offer.pdf`). Ask only what the material leaves open, and confirm what it states ambiguously. If the folder is empty, ask the developer to put the customer's material there, or to go on without it.
3. Go through the checklist below in order. For each topic:
   - Ask **one question at a time**. Give your recommended answer and why, and two or three alternatives when the choice is real.
   - Ask for concrete values and examples, never adjectives: "how many", "by when", "in which time zone", "what exactly happens when …", "show me an example".
   - Probe the typical gaps of `general/requirements-standard.md`: event and timestamp, exceptions and lifecycle, combinations of rules, limits, roles, data and retention.
   - When the customer uses two words for one thing, or one word for two things, ask which is meant and record the term once (ubiquitous language).
   - When an answer is vague, ask one follow-up that forces a choice. When an answer contradicts an earlier one, say so and resolve it.
   - Record the answer immediately in `interview.md` (see "Notes format"), before asking the next question, so nothing is lost if the session ends.
4. "You decide" on a `Block` topic: explain the consequence, propose a default, and record it as source `developer` with status `open — needs customer`. On any other topic record your recommendation as source `developer`.
5. After each topic, summarise it in three lines and ask whether it is right before moving on.
6. Stop when every topic is done, deferred with a reason or open, or when the developer says stop. Then list the open items and tell the developer the next command: `/to-spec`.

## Checklist (order, and the project file each topic feeds)

| # | Topic | Feeds |
|---|---|---|
| 1 | Problem: what hurts today, for whom, how they cope now, what it costs | `intent.md` |
| 2 | Product: what we build, the core outcome, what success looks like (measurable) | `intent.md` |
| 3 | Users and roles: primary, secondary, administrators; what each may do and must not do | `intent.md`, stories, `security-requirements.md` |
| 4 | Features as user goals; for each: role, goal, benefit, priority (must / should / could / won't), MVP or later | stories, `intent.md` |
| 5 | For each must and MVP feature: the main flow step by step, the rules, the exceptions, the boundaries, worked examples with real values | stories (rules, AC, edge cases) |
| 6 | Shared rules and data: entities, fields, required or optional, formats, uniqueness, lifecycle, shared terms | `business-rules.md` |
| 7 | Project-wide facts: time zone, currency, tax, date and number format, languages, encoding | `business-rules.md` (project-wide facts) |
| 8 | Out of scope and non-goals; principles the product must never break | `intent.md`, stories |
| 9 | Personal data: which items, why, how long, who sees them; consent; legal constraints | `security-requirements.md` |
| 10 | Security: authentication, authorisation, sensitive operations, abuse to prevent | `security-requirements.md` |
| 11 | Integrations and external services: which, what for, test or sandbox access | `environments.md`, `secrets.env.example` |
| 12 | Quality: users and load, response times, availability, devices and browsers, accessibility, localisation | `quality-requirements.md` |
| 13 | Technology: required or forbidden languages, frameworks, hosting, licence; existing systems | `tech-stack.md`, `architecture.md` |
| 14 | Environments and deployment: where it runs, who operates it, backups | `environments.md` |
| 15 | Design constraints only: brand colours and logo, tone, target devices, light or dark, reference products the customer likes | `ui-design.md` |
| 16 | End-to-end journeys that prove the product works | `e2e-scenarios.md` |
| 17 | Project done: what the customer must see to accept the product | `intent.md` (DoD-P) |

Design beyond topic 15 is not asked here: the product is built on `general/design/ui-baseline.md` and refined after it works.

## Notes format (`interview.md`)

One section per topic with this table; IDs never change once given.

| ID | Question | Answer | Source (`customer: <file>`, `customer` for an answer the customer gave you, `developer`) | Status (answered, deferred, open) |
|---|---|---|---|---|
| Q-01 | | | | |

Below the tables: "Contradictions resolved" and "Open items" (ID, why open, who must answer).
