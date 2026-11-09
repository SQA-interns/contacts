import { expect, test } from '@playwright/test';
import { addMember, createContact, createGroup, newUser, openAs, paths } from '../acceptance/support/stack';
import { check } from './check';
import { VIEWPORTS } from './viewports';

/**
 * build-ui checks of the Group bar and Group navigation of WF-06..WF-13 (UI-22, UI-23; review F-01, F-02): an Owner
 * moves from the Contact list to the Tag list without typing an address, at the three widths; a Viewer sees the
 * shorter navigation. Needs the test stack.
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('Owner reaches the Tag list from the Contact list, Viewer sees fewer tabs', async ({
      page,
      browser,
      baseURL,
    }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');
      await createContact(ana, acme, { firstName: 'Oseba', lastName: 'Primer', tags: ['gost'] });
      const viola = await newUser('Viola Vidmar', 'viola');
      await addMember(ana, acme, viola, 'VIEWER');

      await openAs(page, ana, paths.contacts(acme));
      await expect(page.getByRole('heading', { level: 1, name: 'Kontakti' })).toBeVisible();
      await expect(page.getByText('Acme', { exact: true })).toBeVisible();
      await expect(page.getByText('Lastnik', { exact: true })).toBeVisible();
      await check(page, 'WF-06_contact-list', 'group-shell', viewport.name);

      if (viewport.width < 768) {
        await page.getByRole('button', { name: /Meni skupine/ }).click();
        await page.getByRole('menuitem', { name: 'Značke' }).click();
      } else {
        await page
          .getByRole('navigation', { name: 'Navigacija skupine' })
          .getByRole('link', { name: 'Značke' })
          .click();
      }
      await expect(page.getByRole('heading', { level: 1, name: 'Značke' })).toBeVisible();
      await expect(page).toHaveURL(new RegExp(`/groups/${acme.id}/tags$`));
      await check(page, 'WF-09_tags', 'group-shell', viewport.name);

      const context = await browser.newContext({
        viewport: { width: viewport.width, height: viewport.height },
        baseURL,
      });
      const viewer = await context.newPage();
      await openAs(viewer, viola, paths.contacts(acme));
      await expect(viewer.getByText('Ogledovalec', { exact: true })).toBeVisible();
      if (viewport.width >= 768) {
        const tabs = viewer.getByRole('navigation', { name: 'Navigacija skupine' });
        await expect(tabs.getByRole('link')).toHaveText(['Kontakti', 'Člani']);
      }
      await check(viewer, 'WF-06_contact-list', 'group-shell-viewer', viewport.name);
      await context.close();
    });
  });
}
