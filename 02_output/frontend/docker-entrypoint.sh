#!/bin/sh
# Writes the runtime configuration and the Content-Security-Policy from environment variables (ADR-15, ES-01),
# then starts nginx. Nothing here is a secret: the Firebase web API key is an identifier of the project.
set -eu

js_escape() { printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'; }

api="${VITE_API_BASE_URL:-}"
emulator="${VITE_FIREBASE_AUTH_EMULATOR_URL:-}"

cat > /usr/share/nginx/html/config.js <<CONFIG
window.__CONFIG__ = {
  VITE_API_BASE_URL: "$(js_escape "$api")",
  VITE_FIREBASE_API_KEY: "$(js_escape "${VITE_FIREBASE_API_KEY:-}")",
  VITE_FIREBASE_AUTH_DOMAIN: "$(js_escape "${VITE_FIREBASE_AUTH_DOMAIN:-}")",
  VITE_FIREBASE_PROJECT_ID: "$(js_escape "${VITE_FIREBASE_PROJECT_ID:-}")",
  VITE_FIREBASE_AUTH_EMULATOR_URL: "$(js_escape "$emulator")"
};
CONFIG

# origin = scheme://host[:port] of the backend and of the emulator (only characters valid in a URL are kept)
origin() { printf '%s' "$1" | sed -E 's#^(https?://[A-Za-z0-9._:-]+).*#\1#' | grep -E '^https?://[A-Za-z0-9._:-]+$' || true; }
connect="'self' https://identitytoolkit.googleapis.com https://securetoken.googleapis.com $(origin "$api") $(origin "$emulator")"
frames="https://accounts.google.com https://*.firebaseapp.com"
printf 'add_header Content-Security-Policy "default-src '"'self'"'; script-src '"'self'"' https://apis.google.com; style-src '"'self'"' '"'unsafe-inline'"'; img-src '"'self'"' data: https://*.googleusercontent.com; connect-src %s; frame-src %s; frame-ancestors '"'none'"'; base-uri '"'self'"'; form-action '"'self'"'; object-src '"'none'"'" always;\n' "$connect" "$frames" > /tmp/csp.conf

exec "$@"
