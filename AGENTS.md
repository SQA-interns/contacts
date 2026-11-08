# AGENTS.md

> For: agent · Owner: Team lead · Agent: read-only

You are one session of a run. Each session is started by its skill (`/preflight`, `/implement US-001`, …), does one job, hands over through files and ends. This file holds what every session needs; everything else you read only when your skill lists it.

## Start

1. Read `02_output/docs/00_progress.md`: what the previous session left, what waits on the human.
2. Follow your skill. Its "Session" section says what you read, write, must not do, and when you are done.
3. Read a file from your "Reads" list when the step that needs it starts, never before, and nothing that is not on the list. *Why:* every file you read costs context for the rest of the session.

`README.md`, `Run_Guide.md` and `system/` are for humans and the runner, not for you.

## Paths and precedence

- Aliases: `general/` = `01_input/00_general/`, `project/` = `01_input/01_project/`, `skills/` = `.claude/skills/`, `out/` = `02_output/`, `docs/` = `02_output/docs/`.
- Precedence: this file > `general/rules/` > other `general/` files > skills > `project/` files, except for explicit overrides.
- A project file adds to the general file on the same topic and never repeats it. It overrides a general item only with a line `Overrides: <ID>` or a row of an "Overrides" table, each with a reason.
- Project-wide facts (time zone, currency, tax, formats, languages) live only in `project/01_requirements/business-rules.md` ("Project-wide facts"). Never repeat them; never assume another value.
- A human answer recorded in a decision (`Human response`) replaces the input entry it changes, for that item only. Never edit `01_input/` to apply it.
- A conflict that no rule resolves is a decision record (`general/rules/decisions.md`), never a silent choice.

## Every session

- **One frame:** ask (intake), generate (to-spec, plan, wireframes, implement, design, fix), criticise (clarify, review, verify) or measure (preflight, quality gate). A criticising session never fixes what it finds; the next session does. *Why:* a checker that also fixes starts defending its fixes.
- **Blocking decision ends the session:** record it, ask all open questions in one message, set "Waiting for" and stop. The human answers by starting the same skill again with the answer (`/clarify D-03: 2`); record it as `Human response` and continue.
- **Hand over only through files.** Before ending, update `docs/00_progress.md` in the format below and commit everything you changed. Never rely on an earlier session's conversation, and do not start the next session's work. *Why:* only files survive; the next job deserves a fresh context.
- **Notes from the developer:** an argument `note: …` is recorded as a non-blocking decision before anything else.
- **Resume:** if your story, task or finding already has commits (`git log --oneline --grep <id>`), continue at the first item not done instead of starting over.
- Write command output longer than 50 lines to `out/logs/`; keep only a summary and the path. Use only the severity levels of `general/quality/severity-scale.md`.

## Handover format (`docs/00_progress.md`)

```
## Last session
- Session: <id> <skill> <argument>
- Result: done | blocked | failed
- Gate: <phase gate> passed | not passed (<which item>)
- Notes for the next session: <at most 10 lines the next session needs and cannot find in docs/>

## Next
- Command: </skill argument>
- Waiting for: <decision ids, or "nothing">

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
```

The runner reads `Session`, `Result`, `Command` and `Waiting for`; keep these names.

## Never

- From `/preflight` on: do not change anything under `01_input/` or `.claude/skills/`, or `AGENTS.md`, `CLAUDE.md`, `README.md`, `Run_Guide.md`, `system/`. Only `/grill-me`, `/to-spec` and `/run` (review answers) write `project/`, before the initial commit. *Why:* the inputs are the contract; a run that changes its own inputs can make anything pass.
- Write only what your skill's "Writes" lists. *Why:* writing elsewhere mixes jobs and hides changes from the session that checks them.
- Do not edit a file listed in `docs/03_acceptance-manifest.sha256`, and do not change `docs/00_input-manifest.sha256` after `/preflight`; raise a decision instead. *Why:* frozen tests are the judge, the manifest the proof that inputs did not change.
- Do not read, list, search or run `02_output/hidden-tests/` or the hidden vault, unless your skill is `/acceptance-tests` (write only). The runner runs them; you see only `docs/06_hidden-evaluation.md`. *Why:* held-out tests measure generalisation only while the implementer has not seen them.
- Do not change a technology, version, service or secret of `project/00_setup/` without human approval, and do not install or upgrade software on the host. *Why:* the stack was checked; the host belongs to the human.
- Do not read `.env` with a file tool, print, copy or log its values, commit secrets, or ask the human to paste one (`general/rules/secrets.md`). *Why:* whatever you read can end up in logs, commits or the conversation.
- Do not decide a `Block` topic yourself (`general/rules/decisions.md`). *Why:* a plausible guess about money, rights or personal data is still a guess.
- Do not start a session before the gate it depends on has passed, and do not end the run while a Critical or High finding is open.
