import { defineConfig, devices } from '@playwright/test';

/**
 * UI checks of the skeleton (NFR-07, NFR-08, NFR-09) against `vite preview`. The UI acceptance tests and the
 * E2E scenarios have their own configurations (`playwright.acceptance.config.ts`, `playwright.e2e.config.ts`)
 * and run against the test stack of `scripts/test-stack.sh`.
 */
const baseURL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173';

export default defineConfig({
  testDir: './ui-checks',
  outputDir: './test-results',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: { baseURL, trace: 'retain-on-failure' },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
  ],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        env: {
          VITE_FIREBASE_API_KEY: 'fake-api-key',
          VITE_FIREBASE_AUTH_DOMAIN: 'localhost',
          VITE_FIREBASE_PROJECT_ID: 'demo-contact-groups',
          VITE_FIREBASE_AUTH_EMULATOR_URL: 'http://127.0.0.1:9099',
        },
        command: 'npm run build && npm run preview -- --host 127.0.0.1',
        url: baseURL,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
});
