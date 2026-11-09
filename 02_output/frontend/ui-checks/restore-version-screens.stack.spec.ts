import { expect, test, type Page } from '@playwright/test';
import {
  addMember,
  createContact,
  createGroup,
  editContact,
  newUser,
  openAs,
  paths,
} from '../acceptance/support/stack';
import { check } from './check';
import { VIEWPORTS } from './viewports';

/**
 * build-ui checks of the restore of a Version in WF-07 (UI-14, UI-15, UI-19, UI-22): the confirmation, the busy
 * state, a refusal for an invalid Version (R-023-03), the restored list, and the refusal for a stale number
 * (R-023-04), at the three widths. Needs the test stack, like `version-screens.stack.spec.ts`.
 */
const restoreOfVersionOne = (page: Page) =>
  page.getByRole('button', { name: 'Obnovi', exact: true }).filter({ visible: true }).last();

for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-07 restore: confirm, busy, refused, restored, stale', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const boris = await newUser('Boris Kos', 'boris');
      const acme = await createGroup(ana, 'Acme');
      await addMember(ana, acme, boris, 'EDITOR');
      const novak = await createContact(ana, acme, {
        firstName: 'Ana',
        lastName: 'Novak',
        company: 'Acme d.o.o.',
        tags: ['gost'],
      });
      await editContact(boris, acme, novak.id, { company: 'Beta d.o.o.' });
      await editContact(boris, acme, novak.id, { company: 'Cezar d.o.o.' });

      await openAs(page, ana, paths.contact(acme, novak.id));
      await page.getByRole('link', { name: 'Različice' }).click();
      await expect(page.getByText('trenutna').filter({ visible: true })).toBeVisible();
      await check(page, 'WF-07_versions', 'list', viewport.name);

      await restoreOfVersionOne(page).click();
      const dialog = page.getByRole('dialog', { name: 'Obnovim različico 1?' });
      await expect(dialog).toContainText('shranjena kot nova različica 4');
      await check(page, 'WF-07_versions', 'restore-confirm', viewport.name);

      await page.route('**/versions/1/restore', (route) =>
        route.fulfill({
          status: 422,
          json: {
            code: 'validation.failed',
            message: 'x',
            fieldErrors: [{ field: 'email', code: 'validation.email_invalid' }],
          },
        }),
      );
      await dialog.getByRole('button', { name: 'Obnovi', exact: true }).click();
      await expect(page.getByRole('alert')).toContainText('E-poštni naslov ni veljaven.');
      await check(page, 'WF-07_versions', 'restore-refused-invalid', viewport.name);
      await page.unroute('**/versions/1/restore');

      let release: () => void = () => undefined;
      const gate = new Promise<void>((resolve) => (release = resolve));
      await page.route('**/versions/1/restore', async (route) => {
        await gate;
        await route.continue();
      });
      await restoreOfVersionOne(page).click();
      await dialog.getByRole('button', { name: 'Obnovi', exact: true }).click();
      await expect(dialog.getByRole('button', { name: 'Obnavljanje …' })).toBeDisabled();
      await check(page, 'WF-07_versions', 'restore-busy', viewport.name);
      release();
      await expect(dialog).toBeHidden();
      await expect(page.getByText('v4').filter({ visible: true }).first()).toBeVisible();
      await expect(page.getByRole('alert')).toHaveCount(0);
      await check(page, 'WF-07_versions', 'restore-done', viewport.name);
      await page.unroute('**/versions/1/restore');

      await editContact(boris, acme, novak.id, { city: 'Celje' });
      await restoreOfVersionOne(page).click();
      await dialog.getByRole('button', { name: 'Obnovi', exact: true }).click();
      await expect(page.getByRole('alert')).toContainText('Kontakt je spremenil nekdo drug.');
      await expect(page.getByRole('alert')).toContainText('Nič ni bilo obnovljeno.');
      await check(page, 'WF-07_versions', 'restore-refused-stale', viewport.name);
    });
  });
}
