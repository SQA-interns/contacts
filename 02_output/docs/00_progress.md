# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-001
- Result: done
- Gate: DoD-S of US-001 passed, except DoD-S-07 in part (two commit subjects are 76 characters, over 72: `eb3d93a`, `1aaa78c`; not rewritten, commits.md)
- Notes for the next session: US-001 built the read-only slice of the Group list that its frozen tests need (D-40): `GroupController` GET only, `GroupService.listGroups`, `GroupRepository.findGroupsOfUser`, `features/groups/GroupListPage` without the create dialog, and the 403 `auth.email_not_verified` filter `security/EmailVerifiedFilter` for everything under `/groups` and `/group-trash` (US-018 T-07 places it in its filter order). US-002 extends these files. `FirebaseTokenFilter` now finds or creates the User (ADR-04); the principal is `AuthenticatedUser(userId, email, emailVerified)`. Spring integration tests outside `acceptance/` must extend `support/ApplicationContextSupport` (one FirebaseApp per JVM). Frontend: `AuthProvider` takes an `AuthClient` (fake in `src/test/fakeAuth.ts`, whole app in tests via `src/test/renderApp.tsx`); shared `FormField`, `Notice`. UI checks with axe and screenshots run on the stack: `E2E_BASE_URL=http://localhost:5173 npx playwright test ui-checks --project=chromium` (`*.stack.spec.ts` need the stack). Results: backend acceptance 8 of 366 pass (Us001 7 of 7), own-context 2 of 5; UI acceptance 21 of 95 (US-001 18 of 18, US-019 3); E2E-01 now stops at step 4 "Ustvari skupino" (US-002), `logs/s5-e2e-01.log`. D-41 (c): a Google name over 100 characters fails with 500, needs a human answer before the release. D-40, D-41 pending review.

## Next
- Command: /implement US-002
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | 1 | done | passed (DoD-S-07: two subjects over 72 characters) |
| US-002 | 1 | ready | — |
| US-003 | 1 | ready | — |
| US-004 | 1 | ready | — |
| US-005 | 1 | ready | — |
| US-006 | 1 | ready | — |
| US-007 | 1 | ready | — |
| US-008 | 1 | ready | — |
| US-009 | 1 | ready | — |
| US-010 | 1 | ready | — |
| US-011 | 1 | ready | — |
| US-012 | 1 | ready | — |
| US-013 | 1 | ready | — |
| US-014 | 1 | ready | — |
| US-015 | 1 | ready | — |
| US-016 | 1 | ready | — |
| US-017 | 1 | ready | — |
| US-018 | 1 | ready | — |
| US-019 | 1 | ready | — |
| US-020 | 2 | ready | — |
| US-021 | 2 | ready | — |
| US-022 | 2 | ready | — |
| US-023 | 2 | ready | — |
| US-024 | 2 | ready | — |
| US-025 | 2 | ready | — |
| US-026 | 2 | ready | — |
