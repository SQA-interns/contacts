#!/bin/sh
# Restores a dump made by backup.sh (NFR-06, DoD-P07, install and backup guide).
#   ./scripts/restore.sh <dump-file>              restore into a new database <name>_restore and
#                                                 print its table counts; production is not touched
#   ./scripts/restore.sh <dump-file> --replace    stop the backend, replace the production database
#                                                 with the dump, start the backend again
# Settings (environment variables, all optional): COMPOSE_FILE, ENV_FILE, COMPOSE_PROJECT as in
# backup.sh. --replace DELETES the current data of the production database: run it only after you
# have made a fresh backup, and never as a test.
set -eu
here="$(cd "$(dirname "$0")/.." && pwd)"
compose_file="${COMPOSE_FILE:-$here/docker-compose.prod.yml}"
env_file="${ENV_FILE:-$here/production.env}"
project="${COMPOSE_PROJECT:-contact-groups}"

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "usage: restore.sh <dump-file> [--replace]" >&2
  exit 2
fi
dump="$1"
mode="${2:-test}"
case "$mode" in test | --replace) ;; *) echo "unknown option: $mode" >&2; exit 2 ;; esac
if [ ! -s "$dump" ]; then
  echo "dump not found or empty: $dump" >&2
  exit 2
fi

dc() { docker compose -p "$project" -f "$compose_file" --env-file "$env_file" "$@"; }
# psql and pg_restore run inside the container with its own user and database name
inside() { dc exec -T postgres sh -c "$1"; }

# nothing is dropped before the file is known to be a readable dump
if ! dc exec -T postgres pg_restore --list <"$dump" >/dev/null 2>&1; then
  echo "not a readable dump of backup.sh: $dump; nothing was changed" >&2
  exit 2
fi

if [ "$mode" = "--replace" ]; then
  echo "stopping the backend and the frontend"
  dc stop backend frontend
  inside 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres -c "DROP DATABASE IF EXISTS \"$POSTGRES_DB\" WITH (FORCE)" -c "CREATE DATABASE \"$POSTGRES_DB\" OWNER \"$POSTGRES_USER\""'
  dc exec -T postgres sh -c 'pg_restore --exit-on-error --no-owner -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <"$dump"
  echo "starting the backend and the frontend"
  dc start backend frontend
  echo "restored into the production database"
  exit 0
fi

# test restore: into <database>_restore, replacing an earlier test copy
inside 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres -c "DROP DATABASE IF EXISTS \"${POSTGRES_DB}_restore\" WITH (FORCE)" -c "CREATE DATABASE \"${POSTGRES_DB}_restore\" OWNER \"$POSTGRES_USER\""'
dc exec -T postgres sh -c 'pg_restore --exit-on-error --no-owner -U "$POSTGRES_USER" -d "${POSTGRES_DB}_restore"' <"$dump"
echo "restored into the database <name>_restore; rows per table:"
dc exec -T postgres sh -c 'psql -At -U "$POSTGRES_USER" -d "${POSTGRES_DB}_restore"' <<'SQL'
SELECT table_name || ': ' || (xpath('/row/c/text()',
         query_to_xml(format('SELECT count(*) AS c FROM %I.%I', table_schema, table_name), false, true, '')))[1]::text
  FROM information_schema.tables
 WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
 ORDER BY table_name;
SQL
echo "compare with the production database, then remove the copy:"
echo "  docker compose -p $project -f $compose_file --env-file $env_file exec postgres sh -c 'psql -U \"\$POSTGRES_USER\" -d postgres -c \"DROP DATABASE \\\"\${POSTGRES_DB}_restore\\\"\"'"
