# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 /implement US-003
- Result: done
- Gate: DoD-S of US-003 passed in part: DoD-S-01 (10 of 29 frozen backend tests pass, the rest and the UI tests wait for US-007, US-014, US-004, US-005), DoD-S-05 (no screenshots or accessibility scan, screens not reachable before US-007) and DoD-S-07 (two commits over the size limit) are open, see D-46
- Notes for the next session: Backend: `ContactController`, `ContactService`, `ContactValidator`, `ContactRepository`, `VersionRepository`, `Normaliser`, `CountryCodes`; create, get and edit with version check (409 `contact.changed_by_someone_else` with `current`), a `contact_version` row per save, no-change save keeps the version. `tags` and `attributes` are accepted and not stored (US-004/005 must replace the snapshot columns in `VersionRepository` and fill `ContactService.view`). `GET /groups/{g}/contacts` is US-007 (now 405). Frontend: `NewContactPage`, `ContactPage`, `ContactForm`, `ConflictBanner`, `useGroupRole`, routes `groups/:groupId/contacts/new` and `.../:contactId`. Backend quality (Spotless, Checkstyle, SpotBugs, PMD) and `verify` green; frontend prettier, eslint, tsc, 123 unit tests green; gitleaks clean. Re-check after US-007: `Us003ContactAcceptanceIT`, `Us018SecurityAcceptanceIT`, `us003-005-contact.spec.ts`, US-019 UI tests. Finding for US-018: `/health` also returns `groups` (AC-018-09).

## Next
- Command: /implement US-016
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | 1 | done | passed (DoD-S-07: two subjects over 72 characters) |
| US-002 | 1 | done | passed (DoD-S-01: EX-002-06/07 wait for US-020/US-014, D-42; DoD-S-07: one subject over 72 characters) |
| US-003 | 1 | done | passed in part (DoD-S-01: frozen tests wait for US-007, US-014, US-004, US-005; DoD-S-05 screens; DoD-S-07: size, D-46) |
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
| US-018 | 1 | done | passed (DoD-S-01: frozen tests wait for US-003, US-014 and the last story, D-44; DoD-S-07: one subject over 72 characters) |
| US-019 | 1 | done | passed in part (frozen UI tests wait for US-003, US-005, US-016) |
| US-020 | 2 | ready | — |
| US-021 | 2 | ready | — |
| US-022 | 2 | ready | — |
| US-023 | 2 | ready | — |
| US-024 | 2 | ready | — |
| US-025 | 2 | ready | — |
| US-026 | 2 | ready | — |
