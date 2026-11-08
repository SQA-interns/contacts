# ADR-12: Frontend structure and routing

- Status: accepted (S3 /plan)
- Drivers: AR-03, AR-08, UI-01, NFR-10, ES-11

## Decision

React 19 + TypeScript strict + Vite. Folders under `src/`: `app/` (router, providers, layout), `api/` (the only place that calls `fetch`, adds the ID token, maps errors to `ApiError`), `auth/` (the only place that imports `firebase/*`), `i18n/`, `styles/`, `components/ui/` (shadcn), `components/` (shared), `features/<name>/` for `groups`, `contacts`, `tags`, `members`, `invitations`, `history`, `activity`, `trash`, `versions`, `duplicates`, `account` (pages, hooks, schemas). Routing: React Router declarative mode: `/sign-in`, `/register`, `/reset-password`, `/verify-email`, `/groups`, `/groups/:groupId/contacts`, `/groups/:groupId/contacts/:contactId/(details|history|versions)`, `/groups/:groupId/{tags,members,invitations,trash,activity,duplicates,merge}`, `/account`. TanStack Query holds server state; React Hook Form with zod validates forms (the server validates again, SB-01); the selection of the contact list lives in a small context (cleared on filter change). ESLint `no-restricted-imports` forbids `firebase/*` outside `auth/` and `fetch` outside `api/` (AR-03); `react/jsx-no-literals` forbids literal UI text (AR-08).

## Consequences

- Feature folders keep a story's UI together; shared pieces move to `components/` when used twice.
- No state library beyond TanStack Query and React context.
