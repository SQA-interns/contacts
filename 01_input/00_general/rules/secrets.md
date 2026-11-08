# Secrets (`.env`)

> For: agent · Owner: Security officer · Agent: read-only

You never need to see a secret value; the tools that run need it, not you.

- Do not read `.env` with a file tool, and do not print, copy or log its values: no `cat`, `echo`, `env`, `printenv` or `docker compose config` on it, in commands, files, logs, commit messages or the conversation.
- Pass values to tools without showing them: `docker compose --env-file .env`, or `export NAME="$(sed -n 's/^NAME=//p' .env | tr -d '\r')"` inside the command that needs it.
- Check presence and emptiness only, as in `skills/preflight`.
- Before saving output from a command that may print an environment, filter it so no value is written.
- The phase 6 secret-leak check (`skills/verify`) must find nothing.
