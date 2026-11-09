import { expect, test } from '@playwright/test';
import { createContact, createGroup, newUser, openAs, paths } from '../acceptance/support/stack';
import { check } from './check';
import { VIEWPORTS } from './viewports';

/**
 * F-13 (US-007, WF-06): a Contact without a last name still has a named link in the name column, so axe finds no
 * `link-name` violation at any of the three widths. Needs the test stack.
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-06 list with Contacts that have no last name', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');
      await createContact(ana, acme, { firstName: 'Mojca' });
      await createContact(ana, acme, { email: 'brez@priimka.si' });
      await openAs(page, ana, paths.contacts(acme));
      await expect(page.getByRole('heading', { level: 1, name: 'Kontakti' })).toBeVisible();

      const links = page.getByRole('link', { name: /^(Mojca|brez@priimka\.si)$/ }).locator('visible=true');
      await expect(links).toHaveCount(2);
      for (const link of await links.all()) await expect(link).not.toBeEmpty();
      await check(page, 'WF-06_contact-list', 'no-last-name', viewport.name);
    });
  });
}
