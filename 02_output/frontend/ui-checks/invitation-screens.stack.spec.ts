import { expect, test } from '@playwright/test';
import { api, createGroup, newUser, openAs, paths } from '../acceptance/support/stack';
import { check } from './check';
import { VIEWPORTS } from './viewports';

/**
 * build-ui checks of the states of WF-12 (Invitations; UI-13, UI-14, UI-15, UI-19, UI-22): empty, list, invalid
 * e-mail, already invited, created, cancel confirmation, at the three widths. Needs the test stack.
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-12 empty, invalid, created, already invited, list, cancel confirmation', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');

      await openAs(page, ana, paths.invitations(acme));
      await expect(page.getByRole('heading', { level: 1, name: 'Povabila' })).toBeVisible();
      await expect(page.getByText('Ni odprtih povabil')).toBeVisible();
      await check(page, 'WF-12_invitations', 'empty', viewport.name);

      await page.getByLabel(/E-pošta/).fill('boris@');
      await page.getByRole('button', { name: 'Ustvari povabilo' }).click();
      await expect(page.getByText('E-poštni naslov ni veljaven.')).toBeVisible();
      await check(page, 'WF-12_invitations', 'invalid-email', viewport.name);

      await page.getByLabel(/E-pošta/).fill('boris@acme.com');
      await page.getByRole('button', { name: 'Ustvari povabilo' }).click();
      await expect(page.getByText('Povabilo je ustvarjeno. Obvestite povabljeno osebo.')).toBeVisible();
      await expect(page.getByText('boris@acme.com').filter({ visible: true })).toBeVisible();
      await check(page, 'WF-12_invitations', 'created', viewport.name);

      await page.getByLabel(/E-pošta/).fill('Boris@Acme.com');
      await page.getByRole('button', { name: 'Ustvari povabilo' }).click();
      await expect(page.getByText('Ta oseba je že članica ali že ima odprto povabilo.')).toBeVisible();
      await check(page, 'WF-12_invitations', 'already-invited', viewport.name);

      await api(ana, 'POST', `/groups/${acme.id}/invitations`, { email: 'cene@acme.com', role: 'VIEWER' });
      await page.reload();
      await expect(page.getByText('Odprta povabila: 2 od 20')).toBeVisible();
      await check(page, 'WF-12_invitations', 'list', viewport.name);

      await page
        .getByRole('button', { name: 'Prekliči povabilo za boris@acme.com' })
        .filter({ visible: true })
        .click();
      const dialog = page.getByRole('dialog', { name: 'Prekličem povabilo?' });
      await expect(dialog).toContainText('boris@acme.com');
      await check(page, 'WF-12_invitations', 'cancel-confirmation', viewport.name);
      await dialog.getByRole('button', { name: 'Prekliči povabilo' }).click();
      await expect(page.getByText('Odprta povabila: 1 od 20')).toBeVisible();
    });
  });
}
