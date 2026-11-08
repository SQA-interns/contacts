import { expect, test } from '@playwright/test';
import { createGroup, newUser, signIn } from '../acceptance/support/stack';
import { check } from './check';
import { VIEWPORTS } from './viewports';

/**
 * build-ui checks of the US-002 states of WF-05 (UI-13, UI-15, UI-19, UI-22): the create dialog, its field errors
 * (R-002-02, R-002-03) and the confirmation after creating, at the three widths. Needs the test stack, like
 * `account-screens.stack.spec.ts`.
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-05 create dialog: open, errors, created', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      await createGroup(ana, 'Acme');
      await signIn(page, ana);
      await expect(page.getByRole('heading', { level: 2, name: 'Acme' })).toBeVisible();

      await page.getByRole('button', { name: 'Ustvari skupino' }).click();
      const dialog = page.getByRole('dialog', { name: 'Nova skupina' });
      await expect(dialog).toBeVisible();
      await check(page, 'WF-05_group-list', 'create-dialog', viewport.name);

      await dialog.getByRole('button', { name: 'Ustvari', exact: true }).click();
      await expect(dialog.getByText('Ime mora imeti od 1 do 100 znakov.')).toBeVisible();
      await check(page, 'WF-05_group-list', 'create-dialog-empty-name', viewport.name);

      await dialog.getByLabel('Ime skupine').fill('ACME');
      await dialog.getByRole('button', { name: 'Ustvari', exact: true }).click();
      await expect(dialog.getByText('Skupino s tem imenom že imate.')).toBeVisible();
      await check(page, 'WF-05_group-list', 'create-dialog-name-taken', viewport.name);

      await dialog.getByLabel('Ime skupine').fill('Šola ČŽ');
      await dialog.getByRole('button', { name: 'Ustvari', exact: true }).click();
      await expect(page.getByText('Skupina je ustvarjena.')).toBeVisible();
      await expect(page.getByRole('heading', { level: 2, name: 'Šola ČŽ' })).toBeVisible();
      await check(page, 'WF-05_group-list', 'created', viewport.name);
    });
  });
}
