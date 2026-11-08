import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end scenarios E2E-01..06 (`e2e-scenarios.md`, one test each, frozen) against the test stack
 * (`../scripts/test-stack.sh up`) in chromium, firefox and webkit (NFR-07).
 */
export default defineConfig({
  testDir: './e2e',
  outputDir: './test-results/e2e',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: 0,
  timeout: 180_000,
  expect: { timeout: 15_000 },
  reporter: [['list'], ['junit', { outputFile: 'test-results/e2e-junit.xml' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    timezoneId: 'Europe/Ljubljana',
    acceptDownloads: true,
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'], viewport: { width: 1440, height: 900 } } },
    { name: 'webkit', use: { ...devices['Desktop Safari'], viewport: { width: 1440, height: 900 } } },
  ],
});
