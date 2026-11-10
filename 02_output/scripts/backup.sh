#!/bin/sh
# Daily database backup (NFR-06, DoD-P07, install and backup guide): one custom-format dump of the
# PostgreSQL database of the production Compose stack, kept 14 days.
#   ./scripts/backup.sh [backup-folder]
# Settings (environment variables, all optional):
#   COMPOSE_FILE    the Compose file of the stack        (default: docker-compose.prod.yml next to scripts/)
#   ENV_FILE        the file with the stack's values      (default: production.env next to scripts/)
#   COMPOSE_PROJECT the Compose project name              (default: contact-groups)
#   RETENTION_DAYS  dumps older than this many days go    (default: 14)
# The dump is written as <folder>/contact-groups-<UTC date and time>.dump with mode 0600. A failed
# dump leaves no file behind and the script exits with a non-zero status; old dumps are removed only
# after a new one succeeded, so a failing backup never empties the folder.
# Run it once a day, for example from cron:  15 2 * * *  /opt/contact-groups/scripts/backup.sh /var/backups/contact-groups
set -eu
here="$(cd "$(dirname "$0")/.." && pwd)"
compose_file="${COMPOSE_FILE:-$here/docker-compose.prod.yml}"
env_file="${ENV_FILE:-$here/production.env}"
project="${COMPOSE_PROJECT:-contact-groups}"
keep_days="${RETENTION_DAYS:-14}"
folder="${1:-$here/backups}"

case "$keep_days" in '' | *[!0-9]*) echo "RETENTION_DAYS must be a whole number" >&2; exit 2 ;; esac

mkdir -p "$folder"
umask 077
stamp="$(date -u +%Y-%m-%dT%H%M%SZ)"
target="$folder/contact-groups-$stamp.dump"
partial="$target.partial"

# The user and the database name come from the container's own environment, so no value is read here.
if ! docker compose -p "$project" -f "$compose_file" --env-file "$env_file" exec -T postgres \
  sh -c 'pg_dump --format=custom --no-owner -U "$POSTGRES_USER" -d "$POSTGRES_DB"' >"$partial"; then
  rm -f "$partial"
  echo "backup failed: no file written" >&2
  exit 1
fi
if [ ! -s "$partial" ]; then
  rm -f "$partial"
  echo "backup failed: the dump is empty" >&2
  exit 1
fi
mv "$partial" "$target"
echo "backup written: $target ($(wc -c <"$target" | tr -d ' ') bytes)"

# retention: remove dumps of this script's name pattern that are older than the retention period
find "$folder" -maxdepth 1 -type f -name 'contact-groups-*.dump' -mtime "+$((keep_days - 1))" -print -exec rm -f {} \; |
  sed 's/^/removed old backup: /'
