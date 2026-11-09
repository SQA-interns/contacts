import { expect, test } from '@playwright/test';
import { api, createContact, createGroup, newUser, openAs, paths } from '../acceptance/support/stack';
import { check } from './check';
import { VIEWPORTS } from './viewports';

/**
 * build-ui checks of the states of WF-10 (Trash) and the delete confirmation of WF-07 (UI-13, UI-14, UI-15, UI-19,
 * UI-22): the confirmation, the list, the restore message and the empty state, at the three widths. Needs the test
 * stack, like `tag-screens.stack.spec.ts`.
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-07 delete confirmation, WF-10 list, restored, empty', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');
      const novak = await createContact(ana, acme, {
        firstName: 'Ana',
        lastName: 'Novak',
        company: 'Acme d.o.o.',
        tags: ['udelezenec2025'],
      });
      await createContact(ana, acme, { firstName: 'Mojca', lastName: 'Zupan', company: 'Zupan d.o.o.' });

      await openAs(page, ana, paths.contact(acme, novak.id));
      await page.getByRole('button', { name: 'Izbriši' }).click();
      const dialog = page.getByRole('dialog', { name: 'Premaknem kontakt v koš?' });
      await expect(dialog).toBeVisible();
      await check(page, 'WF-10_trash', 'delete-confirmation', viewport.name);
      await dialog.getByRole('button', { name: 'Premakni v koš' }).click();
      await expect(page.getByText('Kontakt je v košu.')).toBeVisible();

      await page.goto(paths.trash(acme));
      await expect(page.getByRole('heading', { level: 1, name: 'Koš' })).toBeVisible();
      await expect(page.getByText('30 dni').filter({ visible: true })).toBeVisible();
      await check(page, 'WF-10_trash', 'list', viewport.name);

      await page.getByRole('button', { name: 'Obnovi Ana Novak' }).filter({ visible: true }).click();
      await expect(page.getByText('Kontakt je obnovljen.')).toBeVisible();
      await check(page, 'WF-10_trash', 'restored', viewport.name);
      await expect(page.getByText('Koš je prazen')).toBeVisible();
      expect((await api(ana, 'GET', `/groups/${acme.id}/contacts`)).body.total).toBe(2);
      await check(page, 'WF-10_trash', 'empty', viewport.name);
    });
  });
}
