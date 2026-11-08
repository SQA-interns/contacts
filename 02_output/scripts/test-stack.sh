#!/bin/sh
# Starts or removes the stack the browser tests run against (environments.md, "test"): PostgreSQL, backend,
# frontend and the Firebase Auth emulator, with an empty database and throwaway test values (no .env is read).
#   ./02_output/scripts/test-stack.sh up      remove any earlier test stack, then build and start a fresh one
#   ./02_output/scripts/test-stack.sh down    remove the test stack and its data
# Then: cd 02_output/frontend && npm run acceptance   (UI acceptance tests)
#       cd 02_output/frontend && npm run e2e:scenarios (E2E-01..06)
set -eu
here="$(cd "$(dirname "$0")/.." && pwd)"

# test-only values; the database lives only as long as the test stack
POSTGRES_DB=contactgroups_test
POSTGRES_USER=contactgroups_owner
POSTGRES_PASSWORD="test-$(od -An -N12 -tx1 /dev/urandom | tr -d ' \n')"
DB_APP_PASSWORD="test-$(od -An -N12 -tx1 /dev/urandom | tr -d ' \n')"
FIREBASE_PROJECT_ID=demo-contact-groups
VITE_FIREBASE_API_KEY=fake-api-key
VITE_FIREBASE_AUTH_DOMAIN=localhost
export POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD DB_APP_PASSWORD FIREBASE_PROJECT_ID VITE_FIREBASE_API_KEY VITE_FIREBASE_AUTH_DOMAIN

dc() {
  docker compose -p contact-groups-test \
    -f "$here/docker-compose.yml" -f "$here/docker-compose.emulator.yml" -f "$here/docker-compose.test.yml" "$@"
}

wait_for() {
  i=0
  until curl -fsS -o /dev/null "$1"; do
    i=$((i + 1))
    if [ "$i" -gt 120 ]; then echo "not ready: $1" >&2; exit 1; fi
    sleep 2
  done
}

case "${1:-}" in
  up)
    dc down -v --remove-orphans >/dev/null 2>&1 || true
    dc up -d --build
    wait_for http://localhost:8080/health
    wait_for http://localhost:5173/
    wait_for http://localhost:9099/
    echo "test stack ready: frontend http://localhost:5173, backend http://localhost:8080, emulator localhost:9099"
    ;;
  down)
    dc down -v --remove-orphans
    ;;
  *)
    echo "usage: $0 up|down" >&2
    exit 2
    ;;
esac
