# Run configuration

> For: agent (reads), human (reviews) · Owner: Human operator · Agent: read-only

Settings of the run. How to fill each line: the table below. Start each session with the agent, model and effort of its row; never switch the model inside a session.

- Run label:
- Agent: claude
- Permission mode: default
- Design round: ask
- Hidden vault:

| Line | What to write |
|---|---|
| Run label | a short name for this run; use it as the run folder's name |
| Agent | the default agent of every session without its own in the table below: a name from "Agents" |
| Permission mode | for Claude Code: `default` (every command not allowed below is refused in a `/run` session and asked for in a manual one), `acceptEdits` or `auto` |
| Design round | `ask`: after the last increment, `/design` refines the final design with you; `skip`: `/design` applies the baseline final theme without questions |
| Hidden vault | where `system/run.mjs` keeps the hidden suite, outside the project folder; empty means `../<folder>.hidden-vault` |

## Sessions

| Session | Skill | Agent | Model | Effort | Max turns |
|---|---|---|---|---|---|
| G, T | `/grill-me`, `/to-spec` | | claude-opus-5-5 | high | |
| S1 | `/preflight` | | claude-sonnet-5-5 | medium | |
| S2 | `/clarify` | | claude-opus-5-5 | high | |
| S3 | `/plan` | | claude-opus-5-5 | high | |
| W | `/wireframes` | | claude-opus-5-5 | medium | |
| S4 | `/acceptance-tests` | | claude-opus-5-5 | medium | |
| S5 | `/implement` | | claude-opus-5-5 | medium | |
| R | `/review` | | claude-opus-5-5 | high | |
| D | `/design` | | claude-opus-5-5 | medium | |
| S6 | `/quality-gate` | | claude-sonnet-5-5 | medium | |
| S7 | `/verify` | | claude-opus-5-5 | high | |
| S8 | `/fix` | | claude-opus-5-5 | medium | |
| S9 | `/release` | | claude-sonnet-5-5 | medium | |

An empty Agent cell uses the default agent. Model and effort are passed to that agent as they are, so write values it understands. `Max turns` caps the model turns of one session (empty: no cap); a session that hits it ends as failed and is retried once with `resume`.

## Agents

How `system/run.mjs` starts a session for each agent. `Invocation`: `slash` passes the skill as `/name args` (agents that load `.claude/skills/`), `file` passes "follow `.claude/skills/<name>/SKILL.md` with argument …" (any agent). Placeholders in `Command`: `{prompt}`, `{model}`, `{effort}`, `{permission}`, `{allowed}` (the base tools and the allowed commands below, Claude Code only), `{limits}` (`--max-turns` from the session row, when set). `Invocation` `dollar` passes `$name args` for agents that start skills that way. Check each command against the agent's own documentation before the first run.

| Agent | Invocation | Command |
|---|---|---|
| claude | slash | `claude -p {prompt} --model {model} --effort {effort} --permission-mode {permission} --output-format json {limits} --allowedTools {allowed}` |
| codex | file | `codex exec {prompt} --model {model} --sandbox workspace-write -c model_reasoning_effort="{effort}"` |
| local | file | `codex exec --oss --local-provider ollama {prompt} --model {model} --sandbox workspace-write` |

The Codex reasoning-effort key (`model_reasoning_effort`) is not confirmed in the Codex documentation; remove the `-c` option if Codex rejects it. Codex finds the skills through `.agents/skills` (a link to `.claude/skills`); switch its invocation to `dollar` once your Codex version runs skills in `exec` mode. For another harness, add a row with its own command.

## Pre-approved findings

Findings the human already decided for this project, so the agent does not stop to ask. For an item below, record a **non-blocking** decision that cites this file, apply the action and continue. Apply an item only when the evidence matches the row exactly (same finding, same artifact); anything else is a normal decision (`general/rules/decisions.md`).

| ID | Finding | Evidence that it matches | Action |
|---|---|---|---|
| PA-01 | | | |

Not pre-approved: any Critical finding, any finding on a different artifact, and any change to a technology, version or service not listed above.

## Allowed commands

Commands Claude Code sessions may run without asking, in permission-rule syntax, one per line; `system/run.mjs` adds the base set (git, node, npm, npx, docker, file listing and hashing). Add the build, test and scan commands of the stack. `/to-spec` proposes them; the developer confirms. Codex and other agents use their own sandbox instead.

Proposed by `/to-spec` for the Java 21, Maven, Node, Playwright and Firebase emulator stack (listed in the review checklist). No shell, `curl`, `rm -rf` or `sudo`.

- `Bash(./mvnw *)`
- `Bash(mvn *)`
- `Bash(java *)`
- `Bash(npm run *)`
- `Bash(npx vitest *)`
- `Bash(npx playwright *)`
- `Bash(npx eslint *)`
- `Bash(npx prettier *)`
- `Bash(npx tsc *)`
- `Bash(npx stryker *)`
- `Bash(npx jscpd *)`
- `Bash(npx firebase *)`
- `Bash(npm audit *)`
- `Bash(docker compose *)`
- `Bash(gitleaks *)`
