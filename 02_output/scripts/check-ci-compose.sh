#!/usr/bin/env bash
# Replays the "Compose files are valid" step of .github/workflows/ci.yml: only the
# variables of that step's `env` are set, then the three Compose checks run (F-59).
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
vars="$(awk '
  /name: Compose files are valid/ { step = 1; next }
  step && /^ *env:/ { inenv = 1; next }
  step && inenv && /^ *run:/ { exit }
  step && inenv && /^ *[A-Z_]+: / { sub(/^ */, ""); sub(/: /, "="); print }
' "$root/.github/workflows/ci.yml")"
[ -n "$vars" ] || { echo "no env found for the Compose step of ci.yml" >&2; exit 2; }
cd "$root"
env -i PATH="$PATH" HOME="$HOME" $vars bash -c '
  set -e
  docker compose -f 02_output/docker-compose.yml config -q
  docker compose -f 02_output/docker-compose.yml -f 02_output/docker-compose.emulator.yml config -q
  docker compose -f 02_output/docker-compose.prod.yml config -q
'
echo "compose files valid with the variables of ci.yml"
