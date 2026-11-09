import { expect, test } from '@playwright/test';
import { createContact, createGroup, newUser, openAs, paths } from '../acceptance/support/stack';
import { check } from './check';
import { VIEWPORTS } from './viewports';

/**
 * build-ui checks of the states of WF-06 for US-007 (UI-14, UI-19, UI-22): the list with its pager, the sorted
 * list, the empty state for an Editor, at the three widths. Needs the test stack.
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-06 list, sorted by company, empty', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');
      const empty = await createGroup(ana, 'Prazna');
      for (let i = 1; i <= 30; i++) {
        await createContact(ana, acme, {
          firstName: 'Ime',
          lastName: `Priimek${String(i).padStart(2, '0')}`,
          email: `oseba${i}@acme.si`,
          company: i % 2 ? 'Acme d.o.o.' : 'Beta',
          city: 'Ljubljana',
          country: 'SI',
          tags: ['gost', 'vip'],
        });
      }
      await openAs(page, ana, paths.contacts(acme));
      await expect(page.getByRole('heading', { level: 1, name: 'Kontakti' })).toBeVisible();
      await expect(page.getByText('Stran 1 od 2').first()).toBeVisible();
      await check(page, 'WF-06_contact-list', 'list', viewport.name);

      await page.getByLabel('Iskanje').fill('Priimek07');
      await expect(page.getByText('1 kontakt').first()).toBeVisible();
      await check(page, 'WF-06_contact-list', 'search', viewport.name);
      await page.getByLabel('Iskanje').fill('kompetenca');
      await expect(page.getByRole('heading', { name: 'Noben kontakt ne ustreza' })).toBeVisible();
      await check(page, 'WF-06_contact-list', 'no-match', viewport.name);
      await page.getByRole('button', { name: 'Počisti filtre' }).click();
      await expect(page.getByText('Stran 1 od 2').first()).toBeVisible();

      if (viewport.width >= 768) {
        await page.getByRole('button', { name: 'Podjetje' }).click();
        await expect(page.getByRole('columnheader', { name: 'Podjetje' })).toHaveAttribute(
          'aria-sort',
          'ascending',
        );
        await check(page, 'WF-06_contact-list', 'sorted', viewport.name);
      }

      await page.goto(paths.contacts(empty));
      await expect(page.getByRole('heading', { name: 'V skupini še ni kontaktov' })).toBeVisible();
      await check(page, 'WF-06_contact-list', 'empty', viewport.name);
    });
  });
}
