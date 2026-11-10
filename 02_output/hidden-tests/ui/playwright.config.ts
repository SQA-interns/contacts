import { defineConfig, devices } from '@playwright/test';

/** Hidden UI cases against the test stack (`scripts/test-stack.sh up`); run by `hidden-tests/run.sh`. */
export default defineConfig({
  testDir: '.',
  outputDir: '../target/ui-results',
  fullyParallel: true,
  workers: 4,
  retries: 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [['junit', { outputFile: '../target/ui-junit.xml' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    locale: 'sl-SI',
    timezoneId: 'Europe/Ljubljana',
    viewport: { width: 1440, height: 900 },
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } } }],
});
