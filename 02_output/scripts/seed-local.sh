#!/bin/sh
# Loads the local seed (fake users in the Auth emulator, fake Groups and Contacts) with one command.
#   ./02_output/scripts/seed-local.sh            small seed
#   ./02_output/scripts/seed-local.sh --large    also 10,000 Contacts for the performance checks
# Needs the stack running with the emulator overlay (see 02_output/README.md). Values of .env are never printed.
set -eu
root="$(cd "$(dirname "$0")/../.." && pwd)"
dc() { docker compose -f "$root/02_output/docker-compose.yml" -f "$root/02_output/docker-compose.emulator.yml" --env-file "$root/.env" "$@"; }
psql_owner() { dc exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -q -U "$POSTGRES_USER" -d "$POSTGRES_DB"'; }

FIREBASE_PROJECT_ID="$(sed -n 's/^FIREBASE_PROJECT_ID=//p' "$root/.env" | tr -d '\r')"
export FIREBASE_PROJECT_ID

node "$root/02_output/seed/seed-auth.mjs" http://localhost:9099
psql_owner < "$root/02_output/seed/seed.sql"
if [ "${1:-}" = "--large" ]; then psql_owner < "$root/02_output/seed/generate-large.sql"; fi
echo "seed loaded"
