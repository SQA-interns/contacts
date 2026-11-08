---
name: run
description: Conductor session C. Run the whole workflow after /grill-me with minimal work for the developer - generate the inputs, ask the developer the review questions, set up the folder, then start every session from /preflight to /release as a separate non-interactive process through system/run.mjs, relay their decisions to the developer and pass the answers back.
argument-hint: "[resume]"
disable-model-invocation: true
---

> For: agent · Owner: Team lead · Agent: read-only

## Session

- Id: C, all phases. Frame: orchestrate. You never do the work of a phase; each phase runs in its own session started by `system/run.mjs`. *Why:* your context must stay small for the whole run.
- Reads: `project/00_setup/run-config.md`; `project/00_intake/review-checklist.md`; the key names in `project/00_setup/secrets.env.example`; the last line (`RUN_STATUS`) of each runner call; a run log only when the developer asks.
- Writes: `project/` (review answers only, before the initial commit), `project/00_setup/run-config.md`, the initial commit. Every later change is made by the sessions the runner starts.
- Must not: read code, tests, the hidden suite or vault, `.env` values or the sessions' transcripts; answer a decision for the developer; run the runner from inside a started session.
- Exit: the runner reports `complete`, or the developer stops (`/run resume` continues).

## Before the run (intake)

1. **Prerequisites.** Check Node 18 or newer (`node --version`) and that the command of every agent named in `run-config.md` ("Agent" and the session table) exists, for example `claude --version` or `codex --version`. If one is missing, tell the developer to install and sign in to it, and stop. Do not install anything yourself.
2. **Interview.** If `project/00_intake/interview.md` has no answers, tell the developer to run `/grill-me` in a new session first, then `/run` again, and stop. *Why:* the interview is a long conversation and does not belong in your context.
3. **Folder.** If there is no git repository, run `git init -b run/<folder name>` and add `.env` to `.git/info/exclude`.
4. **Inputs.** If `project/00_intake/review-checklist.md` has no rows, run `node system/run.mjs --once --command "/to-spec"` in the background and wait for it.
5. **Review by questions.** Read `review-checklist.md`. Ask the developer the open rows, at most five per message, each with its proposed answer, so that "ok" accepts all five. Write each answer into the project file and line the row names (remove the `[ASSUMPTION]` marker, fill the `OPEN` answer) and mark the row done. A row on a `Block` topic that the developer cannot answer stays `OPEN`; `/clarify` will ask for it again. You may write `project/` only in this step and only with the developer's answers.
6. **Run settings.** In `run-config.md`: the run label (default: the folder name), the permission mode (default `default`; every command not in the allow list is refused without a prompt), the design round (`ask` or `skip`), and the "Allowed commands" list `/to-spec` proposed: show it and ask for confirmation.
7. **Secrets.** List the keys of `secrets.env.example` that need a value (not test-only, not "not needed"). Ask the developer to copy the file to `.env` and fill them in an editor, never in the chat, and to say "done". Check presence only, with the method of `skills/preflight` step 4.
8. **Commit.** `git add -A`, `git commit -m "initial commit"`; confirm `git status --porcelain` does not list `.env`. Tell the developer the run starts and roughly how many sessions it will take (stories × 1, increments × 1, about ten more).

## The loop

9. Run `node system/run.mjs --once` **in the background** and wait for it to finish; never poll. Read only its last line, `RUN_STATUS {…}`.
10. Act on `status`:
    - `done`: tell the developer in one line what finished (`last`, `gate`) and what is next. If `last` is the review of `INC-1`, ask once whether the customer has seen the grey MVP; pass the answer to the next session as a note: `node system/run.mjs --once --command "<next> note: customer saw MVP: <yes/no, remarks>"`, then act on its status. Otherwise go to step 9.
    - `waiting`: show each decision of `decisions` (title, trigger, numbered options, proposed default) in one message and ask for the answers. For the wireframe approval, list the image files in `docs/wireframes/` so the developer can open them. Then run `node system/run.mjs --once --command "<next> D-nn: <option>, …"` and continue with step 10.
    - `interactive`: the next command is `/design` with "Design round: ask" (or `/grill-me`). Tell the developer the application now looks like the approved wireframes (the milestone), ask them to open a **new** session with the command, and to tell you "done" when finished; then go to step 9.
    - `failed`: the runner already retried once (with `resume`). Show `error` and the log path; offer: run again, or stop. Do not read the log yourself unless the developer asks.
    - `complete`: go to step 11.
11. **End.** Run `node system/run.mjs --summary`. Tell the developer where the release notes are, how many manual tests and decisions pending review they list, the numbers of the summary and where the retrospective is. Do not summarise the code.

## Rules

- Never answer a decision for the developer, and never pass an answer the developer did not give. "ok" means the proposed defaults.
- Never edit inputs after the initial commit; answers reach them only as decisions through the sessions.
