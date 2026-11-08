import { defineConfig, devices } from '@playwright/test';

/**
 * UI acceptance tests (phase 3, frozen): through the screens of the wireframes, against the test stack
 * (`../scripts/test-stack.sh up`: frontend, backend, PostgreSQL, Firebase Auth emulator). Data is set up through
 * the REST interface and the emulator; every test uses its own e-mail addresses, so tests do not share data.
 */
export default defineConfig({
  testDir: './acceptance',
  outputDir: './test-results/acceptance',
  fullyParallel: true,
  workers: 4,
  forbidOnly: !!process.env.CI,
  retries: 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [['list'], ['junit', { outputFile: 'test-results/acceptance-junit.xml' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    locale: 'sl-SI',
    timezoneId: 'Europe/Ljubljana',
    viewport: { width: 1440, height: 900 },
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } } },
  ],
});
