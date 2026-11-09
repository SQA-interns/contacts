import { expect, test } from '@playwright/test';
import { addMember, createGroup, newUser, openAs, paths } from '../acceptance/support/stack';
import { check } from './check';
import { VIEWPORTS } from './viewports';

/**
 * build-ui checks of the states of WF-11 (Members; UI-13, UI-14, UI-15, UI-19, UI-22): Owner list, Viewer list,
 * Role changed, last Owner refused, remove confirmation, at the three widths. Needs the test stack.
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-11 owner list, role changed, last owner refused, remove confirmation, viewer list', async ({
      page,
      browser,
      baseURL,
    }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');
      await addMember(ana, acme, await newUser('Boris Kos', 'boris'), 'EDITOR');
      const viola = await newUser('Viola Vidmar', 'viola');
      await addMember(ana, acme, viola, 'VIEWER');

      await openAs(page, ana, paths.members(acme));
      await expect(page.getByRole('heading', { level: 1, name: 'Člani' })).toBeVisible();
      await expect(page.getByText('3 člani')).toBeVisible();
      await check(page, 'WF-11_members', 'owner-list', viewport.name);

      await page
        .getByRole('combobox', { name: 'Vloga' })
        .filter({ visible: true })
        .nth(1)
        .selectOption({ label: 'Ogledovalec' });
      await expect(page.getByRole('status').filter({ hasText: 'Vloga je spremenjena.' })).toBeVisible();
      await check(page, 'WF-11_members', 'role-changed', viewport.name);

      await page
        .getByRole('combobox', { name: 'Vloga' })
        .filter({ visible: true })
        .first()
        .selectOption({ label: 'Urednik' });
      await expect(page.getByText('Skupina potrebuje vsaj enega lastnika.')).toBeVisible();
      await check(page, 'WF-11_members', 'last-owner-refused', viewport.name);

      await page
        .getByRole('button', { name: 'Odstrani člana Viola Vidmar' })
        .filter({ visible: true })
        .click();
      await expect(page.getByRole('dialog', { name: 'Odstranim člana Viola Vidmar?' })).toBeVisible();
      await check(page, 'WF-11_members', 'remove-confirmation', viewport.name);
      await page.getByRole('button', { name: 'Odstrani člana', exact: true }).click();
      await expect(page.getByText('2 člana')).toBeVisible();

      await addMember(ana, acme, viola, 'VIEWER');
      const context = await browser.newContext({
        baseURL,
        viewport: { width: viewport.width, height: viewport.height },
      });
      const violaPage = await context.newPage();
      await openAs(violaPage, viola, paths.members(acme));
      await expect(violaPage.getByText('3 člani')).toBeVisible();
      await expect(violaPage.getByRole('combobox')).toHaveCount(0);
      await check(violaPage, 'WF-11_members', 'viewer-list', viewport.name);
      await context.close();
    });
  });
}
