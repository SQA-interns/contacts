import { expect, test } from '@playwright/test';
import { newUser, signIn, unique } from '../acceptance/support/stack';
import { check } from './check';
import { VIEWPORTS } from './viewports';

/**
 * build-ui checks of the US-001 screens WF-01 to WF-05 (UI-14, UI-19, UI-22): every state that the stack can
 * show, at the three widths, with no horizontal scroll and no Critical or Serious axe violation; screenshots go to
 * `02_output/logs/screens/<screen>/`. Needs the test stack (`scripts/test-stack.sh up`, E2E_BASE_URL set); the
 * default UI-check run leaves `*.stack.spec.ts` out (playwright.config.ts).
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-01 sign-in: form and wrong credentials', async ({ page }) => {
      await page.goto('/sign-in');
      await expect(page.getByRole('heading', { level: 1, name: 'Prijava' })).toBeVisible();
      await check(page, 'WF-01_sign-in', 'success', viewport.name);
      await page.getByLabel('E-pošta').fill(unique('nikogar'));
      await page.getByLabel('Geslo').fill('NapacnoGeslo1');
      await page.getByRole('button', { name: 'Prijava', exact: true }).click();
      await expect(page.getByRole('alert')).toBeVisible();
      await check(page, 'WF-01_sign-in', 'error', viewport.name);
    });

    test('WF-02 register: form and field errors', async ({ page }) => {
      await page.goto('/register');
      await expect(page.getByRole('heading', { level: 1, name: 'Registracija' })).toBeVisible();
      await check(page, 'WF-02_register', 'success', viewport.name);
      await page.getByLabel('Geslo').fill('Abc1234');
      await page.getByRole('button', { name: 'Ustvari račun' }).click();
      await expect(page.getByText('Geslo mora imeti vsaj 8 znakov.')).toBeVisible();
      await check(page, 'WF-02_register', 'field-errors', viewport.name);
    });

    test('WF-03 reset password: form and confirmation', async ({ page }) => {
      await page.goto('/reset-password');
      await expect(page.getByRole('heading', { level: 1, name: 'Ponastavitev gesla' })).toBeVisible();
      await check(page, 'WF-03_reset-password', 'success', viewport.name);
      await page.getByLabel('E-pošta').fill(unique('nikogar'));
      await page.getByRole('button', { name: 'Pošlji povezavo' }).click();
      await expect(page.getByRole('status')).toBeVisible();
      await check(page, 'WF-03_reset-password', 'confirmation', viewport.name);
    });

    test('WF-04 verify e-mail: message, resend', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana', false);
      await signIn(page, ana, false);
      await expect(page.getByRole('button', { name: 'Pošlji znova' })).toBeVisible();
      await check(page, 'WF-04_verify-email', 'success', viewport.name);
      await page.getByRole('button', { name: 'Pošlji znova' }).click();
      await expect(page.getByRole('status')).toBeVisible();
      await check(page, 'WF-04_verify-email', 'resent', viewport.name);
    });

    test('WF-05 Group list: empty, list and error', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      await signIn(page, ana);
      await expect(page.getByRole('heading', { name: 'Še nimate nobene skupine' })).toBeVisible();
      await check(page, 'WF-05_group-list', 'empty', viewport.name);
      const groups = [
        {
          id: '00000000-0000-4000-8000-000000000001',
          name: 'Acme',
          role: 'OWNER',
          createdAt: '2026-11-08T08:00:00Z',
        },
        {
          id: '00000000-0000-4000-8000-000000000002',
          name: 'Klub',
          role: 'EDITOR',
          createdAt: '2026-11-08T08:00:00Z',
        },
        {
          id: '00000000-0000-4000-8000-000000000003',
          name: 'Projekt',
          role: 'VIEWER',
          createdAt: '2026-11-08T08:00:00Z',
        },
      ];
      await page.route('**/api/v1/groups', (route) => route.fulfill({ json: groups }));
      await page.reload();
      await expect(page.getByRole('heading', { level: 2, name: 'Acme' })).toBeVisible();
      await check(page, 'WF-05_group-list', 'list', viewport.name);
      await page.route('**/api/v1/groups', (route) =>
        route.fulfill({ status: 500, json: { code: 'server.error', message: 'Unexpected error' } }),
      );
      await page.reload();
      await expect(page.getByRole('button', { name: 'Poskusi znova' })).toBeVisible();
      await check(page, 'WF-05_group-list', 'error', viewport.name);
    });
  });
}
