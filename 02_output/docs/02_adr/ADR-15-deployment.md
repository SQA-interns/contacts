# ADR-15: Container images and Compose

- Status: accepted (S3 /plan)
- Drivers: AR-12, NFR-05, NFR-12, SB-11, ES-09, environments.md

## Decision

`backend/Dockerfile`: build stage `eclipse-temurin:21.0.8_9-jdk` runs `./mvnw -DskipTests package`; runtime stage `eclipse-temurin:21.0.8_9-jre`, non-root user, `HEALTHCHECK` on `/health`. `frontend/Dockerfile`: build stage `node:22.20.0-bookworm-slim` (`npm ci && npm run build`); runtime stage `nginx:1.29.2-alpine` run as the `nginx` user on port 8080 with writable temp paths, SPA fallback, security headers and a CSP that allows only the backend origin and the Firebase endpoints (`identitytoolkit.googleapis.com`, `securetoken.googleapis.com`, `apis.google.com`, `*.firebaseapp.com`). `VITE_*` values are build-time; to keep one image for every environment the image serves `/config.js` generated at container start from environment variables, and the app reads `window.__CONFIG__` first. `docker-compose.yml` (local, started with the repository's `.env`): `postgres:17.6` with a named volume and health check, `backend`, `frontend`, and, in the overlay `docker-compose.emulator.yml`, `emulator/Dockerfile` (`FROM node:22.20.0-bookworm-slim` with `firebase-tools` 14.22.0; the Authentication emulator is part of firebase-tools and needs no Java runtime; it starts with `--project $FIREBASE_PROJECT_ID`, the token audience; development and tests only, never shipped, D-05). The overlay also points the backend at `emulator:9099` and the browser at `http://localhost:9099`. `docker-compose.prod.yml`: images only, `restart: unless-stopped`, health checks, no emulator, all values from the environment file. TLS ends at the customer's reverse proxy.

## Consequences

- One image per component for all environments (AR-12).
- The base images are the tags pinned in `tech-stack.md`.
