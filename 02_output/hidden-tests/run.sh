#!/bin/sh
# Runs the hidden evaluation suite from the project root and writes one JUnit report:
#   02_output/hidden-tests/target/hidden-junit.xml
# 1. backend cases: this folder's Maven project (compiles ../backend/src/main) with Testcontainers and its own
#    Firebase Auth emulator on port 9099, so the test stack is removed first (no other stack may use port 9099)
# 2. UI cases: Playwright of the frontend against a fresh test stack, which is removed afterwards
# The exit code is 0 when every case passed.
set -u
root="$(cd "$(dirname "$0")/../.." && pwd)"
here="$root/02_output/hidden-tests"
stack="$root/02_output/scripts/test-stack.sh"
rm -rf "$here/target/failsafe-reports" "$here/target/ui-junit.xml" "$here/target/hidden-junit.xml"
mkdir -p "$here/target"
status=0

"$stack" down >/dev/null 2>&1 || true
(cd "$root/02_output/backend" && ./mvnw -B -q -f "$here/pom.xml" verify) || status=1

"$stack" up || status=1
if [ ! -d "$root/02_output/frontend/node_modules" ]; then (cd "$root/02_output/frontend" && npm ci); fi
(cd "$root/02_output/frontend" && NODE_PATH="$root/02_output/frontend/node_modules" npx playwright test -c "$here/ui/playwright.config.ts") || status=1
"$stack" down || true

node "$here/merge-junit.mjs" "$here/target/hidden-junit.xml" "$here/target/failsafe-reports" "$here/target/ui-junit.xml" || status=1
exit $status
