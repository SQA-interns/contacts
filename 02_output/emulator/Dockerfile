# Firebase Authentication emulator for development and tests only; never shipped (ADR-15, D-05).
# The Auth emulator is part of firebase-tools itself; no Java runtime is needed for it.
FROM node:22.20.0-bookworm-slim

RUN npm install -g --no-fund --no-audit firebase-tools@14.22.0 \
    && npm cache clean --force

WORKDIR /srv
COPY firebase.json .

ENV FIREBASE_TELEMETRY_COLLECTION_ENABLED=false \
    CI=true
EXPOSE 9099
USER node
CMD ["firebase", "emulators:start", "--only", "auth", "--project", "demo-contact-groups"]
