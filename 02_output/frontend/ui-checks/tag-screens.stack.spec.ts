import { expect, test } from '@playwright/test';
import { createContact, createGroup, newUser, openAs, paths } from '../acceptance/support/stack';
import { check } from './check';
import { VIEWPORTS } from './viewports';

/**
 * build-ui checks of the states of WF-09 (UI-13, UI-14, UI-15, UI-19, UI-22): the list, the rename dialog with its
 * refusals (R-006-03), the delete confirmation (R-006-06) and the empty state, at the three widths. Needs the test
 * stack, like `group-screens.stack.spec.ts`.
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-09 list, rename dialog, refusals, delete confirmation, empty', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');
      await createContact(ana, acme, { firstName: 'Oseba', tags: ['gost', 'udelezenec2025'] });
      await openAs(page, ana, paths.tags(acme));
      await expect(page.getByRole('heading', { level: 1, name: 'Značke' })).toBeVisible();
      await check(page, 'WF-09_tags', 'list', viewport.name);

      await page.getByRole('button', { name: 'Preimenuj značko gost' }).filter({ visible: true }).click();
      const dialog = page.getByRole('dialog', { name: 'Preimenuj značko' });
      await expect(dialog).toBeVisible();
      await check(page, 'WF-09_tags', 'rename-dialog', viewport.name);

      await dialog.getByLabel(/Novo ime/).fill('UDELEZENEC2025');
      await dialog.getByRole('button', { name: 'Preimenuj', exact: true }).click();
      await expect(dialog.getByText('Značka s tem imenom že obstaja.')).toBeVisible();
      await check(page, 'WF-09_tags', 'rename-taken', viewport.name);

      await dialog.getByLabel(/Novo ime/).fill('nov gost');
      await dialog.getByRole('button', { name: 'Preimenuj', exact: true }).click();
      await expect(dialog.getByText(/Ime značke lahko vsebuje/)).toBeVisible();
      await check(page, 'WF-09_tags', 'rename-invalid', viewport.name);
      await dialog.getByRole('button', { name: 'Prekliči' }).click();

      await page.getByRole('button', { name: 'Izbriši značko gost' }).filter({ visible: true }).click();
      await expect(page.getByRole('dialog')).toContainText('1 kontakt');
      await check(page, 'WF-09_tags', 'delete-confirmation', viewport.name);
      await page.getByRole('dialog').getByRole('button', { name: 'Izbriši značko', exact: true }).click();
      await expect(page.getByText('Značka je izbrisana.')).toBeVisible();

      await page
        .getByRole('button', { name: 'Izbriši značko udelezenec2025' })
        .filter({ visible: true })
        .click();
      await page.getByRole('dialog').getByRole('button', { name: 'Izbriši značko', exact: true }).click();
      await expect(page.getByText('V skupini še ni značk')).toBeVisible();
      await check(page, 'WF-09_tags', 'empty', viewport.name);
    });
  });
}
