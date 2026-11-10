# Frontend

> For: agent (writes), human (reads) · Written in: phase 2 skeleton, phase 7 final · Source: ES-06 · Agent: writes

React 19 + TypeScript single-page application of Contact Groups, built with Vite 7, styled with Tailwind CSS 4 tokens and shadcn/ui components, texts in Slovene (default) and English. Design: `docs/02_specification.md` ("User interface"), `docs/02_adr/ADR-12..14`.

## Prerequisites

Node 22.20.0 with npm 10.9.3. `.npmrc` sets `legacy-peer-deps=true` (D-33). For the browser tests: `npx playwright install chromium firefox webkit` (Playwright 1.56.1).

## Configuration

| Variable                                                                         | Needed                                | Source                                                         |
| -------------------------------------------------------------------------------- | ------------------------------------- | -------------------------------------------------------------- |
| `VITE_API_BASE_URL`                                                              | yes (default `http://localhost:8080`) | address of the backend as the browser sees it                  |
| `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN`, `VITE_FIREBASE_PROJECT_ID` | yes                                   | Firebase web app (an identifier of the project)                |
| `VITE_FIREBASE_AUTH_EMULATOR_URL`                                                | no                                    | for example `http://localhost:9099`; empty means real Firebase |

For `npm run dev` put them in `frontend/.env.local` (git-ignored) or the environment. In the container they are read at start from the environment and served as `/config.js` (`window.__CONFIG__`, ADR-15), so one image serves every environment.

## Commands (ES-05)

```bash
npm ci                # install exactly the locked versions
npm run build         # build: type check and production bundle in dist/
npm test              # test: Vitest (jsdom, msw, Testing Library)
npm run check         # check: Prettier, ESLint (incl. AR-03 and AR-08 rules), tsc, catalog test
npm run dev           # run: Vite dev server on http://localhost:5173
npm run e2e           # browser tests (Playwright, 3 browsers, 375/768/1440 px, axe); builds and previews first
```

## Structure

`src/app` router and shell · `src/api` the only code that calls the backend · `src/auth` the only code that imports Firebase · `src/i18n` catalogs and formats · `src/styles` tokens, `themes/wireframe.css` (active), `themes/final.css`, `brand.css` · `src/brand.ts` product name and logo · `src/components/ui` shadcn components (adapted to the pinned Radix packages) · `src/features/<name>` one folder per feature.

## Troubleshooting

- The page is blank: open the console; without `VITE_FIREBASE_*` the shell still renders (signed out), so a blank page means a build error.
- Firefox may fail to start inside restricted sandboxes (seen in the agent sandbox); run `--project=chromium --project=webkit` there.
- `npm install` fails with `edgesOut`: keep `.npmrc` (D-33).
- `npm run e2e` is the only command that starts a browser for all three engines; Firefox does not start on every machine (F-48, see the release notes).

## Deploy

The frontend is delivered as a container image (static files behind nginx, user `nginx`, health check `/healthz`) built from the Dockerfile in this folder. The image is the same for every environment: at start the entrypoint writes `/config.js` from the `VITE_*` variables above, so nothing environment-specific is baked in.

```bash
docker build -t contact-groups-frontend:1.0.0 .
```

Production starts it together with the backend and PostgreSQL through `../docker-compose.prod.yml` (the page is on `127.0.0.1:8081`, behind the customer's TLS proxy). Configuration names, start and checks: [`../docs/install-and-backup.md`](../docs/install-and-backup.md).
