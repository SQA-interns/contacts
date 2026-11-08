# ADR-04: Authentication with Firebase ID tokens

- Status: accepted (S3 /plan)
- Drivers: AR-06, SR-02, SR-03, SR-13, R-001-11, R-001-12, R-018-01, R-018-06, D-12

## Context

Firebase Authentication identifies people (Q-21); authorisation is the product's own. The backend checks every ID token with the Admin SDK (AR-06). Tests must run offline against the Firebase Local Emulator Suite (SR-13). D-12 removes `google-cloud-storage` and `google-cloud-firestore` from `firebase-admin`; a test must prove that token checking still works.

## Options

1. Spring Security OAuth2 resource server with Firebase's JWKS.
2. A servlet filter that calls `FirebaseAuth.verifyIdToken` and builds the principal.
3. Session cookies issued by the backend.

## Decision

Option 2. `FirebaseTokenFilter` (package `security`) reads `Authorization: Bearer`, verifies the token (`checkRevoked=false`), and on success loads the `User` by Firebase UID or creates it (R-001-11: UID, lowercase e-mail, display name from the token `name`, else the e-mail). The principal `AuthenticatedUser` carries `userId`, `email`, `emailVerified` taken from the token, and never a Role (SR-03). Missing, invalid or expired token: 401 with the error format. `FirebaseApp` is initialised with the project id only (no service account, `GOOGLE_APPLICATION_CREDENTIALS` is not needed); when `FIREBASE_AUTH_EMULATOR_HOST` is set the SDK talks to the emulator and accepts its unsigned tokens. Unverified e-mail: the filter lets `GET /me` through and every Group endpoint answers 403 (ADR-05).

## Consequences

- No Spring OAuth2 resource-server dependency.
- Role is always read from the database on every request.
- A short-lived cache of verified tokens is not added; the Admin SDK caches the public keys.
- `FirebaseAuth` is not a Spring bean (the verifier creates it from the `FirebaseApp`): with `google-cloud-*` excluded (D-12) its class references `com.google.api.gax.paging.Page`, which Spring's bean introspection would try to load. `google-http-client-jackson2` is added for `FirebaseOptions` (D-32). Anything that calls `listUsers` would need `gax` and a new decision.
