import { expect, test } from '@playwright/test';
import { api, createGroup, newUser, openAs } from '../acceptance/support/stack';
import { check } from './check';
import { VIEWPORTS } from './viewports';

/**
 * build-ui checks of WF-16 (My account, US-021) (UI-13, UI-14, UI-15, UI-19, UI-22): the details, the confirmation,
 * the busy button, the refusal for a Sole Owner with a Group and a Group in the Group Trash, the message after the
 * deletion, and the loading and error states, at the three widths. Needs the test stack.
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-16 details, confirmation, busy, deleted', async ({ page }) => {
      const cene = await newUser('Cene Zupan', 'cene');

      await openAs(page, cene, '/account');
      await expect(page.getByRole('heading', { level: 1, name: 'Moj račun' })).toBeVisible();
      await expect(page.getByText('Cene Zupan')).toBeVisible();
      await check(page, 'WF-16_account', 'details', viewport.name);

      await page.getByRole('button', { name: 'Izbriši račun' }).click();
      const dialog = page.getByRole('dialog', { name: 'Izbrišem račun?' });
      await expect(dialog).toContainText('Tega ni mogoče razveljaviti.');
      await check(page, 'WF-16_account', 'confirmation', viewport.name);

      await page.route('**/api/v1/me', async (route) => {
        if (route.request().method() !== 'DELETE') return route.fallback();
        await new Promise((resolve) => setTimeout(resolve, 1500));
        return route.fallback();
      });
      await dialog.getByRole('button', { name: 'Izbriši račun' }).click();
      await expect(dialog.getByRole('button', { name: 'Brisanje računa …' })).toBeDisabled();
      await check(page, 'WF-16_account', 'busy', viewport.name);

      await expect(page.getByRole('heading', { level: 1, name: 'Prijava' })).toBeVisible();
      await expect(page.getByText('Račun je izbrisan.')).toBeVisible();
      await check(page, 'WF-16_account', 'deleted', viewport.name);
    });

    test('WF-16 refused: Sole Owner of a Group and of a Group in the Group Trash', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      await createGroup(ana, 'Acme');
      const old = await createGroup(ana, 'Stara skupina');
      await api(ana, 'POST', `/groups/${old.id}/trash`, { name: 'Stara skupina' });

      await openAs(page, ana, '/account');
      await page.getByRole('button', { name: 'Izbriši račun' }).click();
      await page
        .getByRole('dialog', { name: 'Izbrišem račun?' })
        .getByRole('button', { name: 'Izbriši račun' })
        .click();

      const alert = page.getByRole('alert');
      await expect(alert).toContainText('Računa ni mogoče izbrisati, ker ste edini lastnik skupine.');
      await expect(alert).toContainText('v košu skupin, še 30 dni');
      await expect(alert.getByRole('link', { name: 'Člani Acme' })).toBeVisible();
      await expect(alert.getByRole('link', { name: 'Koš skupin' })).toBeVisible();
      await check(page, 'WF-16_account', 'refused', viewport.name);

      await alert.getByRole('button', { name: 'Izbriši skupino Acme' }).click();
      await page.getByLabel('Za potrditev vpišite ime skupine').fill('Acme');
      await page.getByRole('dialog').getByRole('button', { name: 'Izbriši skupino' }).click();
      await expect(alert.getByText('Acme', { exact: true })).toHaveCount(0);
      await expect(alert.getByText('Stara skupina')).toBeVisible();
    });

    test('WF-16 loading and error states', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      await page.route('**/api/v1/me', async (route) => {
        await new Promise((resolve) => setTimeout(resolve, 1500));
        await route.fulfill({ status: 500, json: { code: 'server.error', message: 'x' } });
      });

      await openAs(page, ana, '/account');
      await expect(page.getByRole('status')).toBeVisible();
      await check(page, 'WF-16_account', 'loading', viewport.name);
      await expect(page.getByRole('button', { name: 'Poskusi znova' })).toBeVisible();
      await check(page, 'WF-16_account', 'error', viewport.name);
    });
  });
}
