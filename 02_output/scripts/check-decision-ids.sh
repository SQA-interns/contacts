#!/usr/bin/env bash
# Fails when docs/decisions-log.md has two entries with the same id (F-18).
set -euo pipefail
log="$(dirname "$0")/../docs/decisions-log.md"
dups="$(grep -oE '^## D-[0-9]+' "$log" | sort | uniq -d)"
if [ -n "$dups" ]; then
  echo "Duplicate decision ids in decisions-log.md:" >&2
  echo "$dups" >&2
  exit 1
fi
echo "decision ids unique"
