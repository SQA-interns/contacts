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
      await createContact(ana, acme, {
        firstName: 'Avstrijec',
        lastName: 'Zupan',
        company: 'Gama',
        country: 'AT',
      });
      await openAs(page, ana, paths.contacts(acme));
      await expect(page.getByRole('heading', { level: 1, name: 'Kontakti' })).toBeVisible();
      await expect(page.getByText('Stran 1 od 2').first()).toBeVisible();
      await check(page, 'WF-06_contact-list', 'list', viewport.name);

      // US-010: the Selection bar with two ticked rows, and with the whole page selected (the offer)
      const rowBoxes = page.getByRole('checkbox', { name: 'Izberi kontakt' });
      await rowBoxes.locator('visible=true').nth(0).check();
      await rowBoxes.locator('visible=true').nth(1).check();
      await expect(page.getByRole('region', { name: 'Izbira' })).toContainText('2 izbrana');
      await check(page, 'WF-06_contact-list', 'selection', viewport.name);
      await page.getByRole('checkbox', { name: 'Izberi vse na tej strani' }).locator('visible=true').check();
      await expect(page.getByRole('button', { name: 'Izberi vseh 31 zadetkov' })).toBeVisible();
      await check(page, 'WF-06_contact-list', 'selection-page', viewport.name);
      // US-011: the export refused as too large (R-011-05)
      await page.route('**/api/v1/groups/*/exports', (route) =>
        route.fulfill({
          status: 422,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'export.too_many', message: 'Too many' }),
        }),
      );
      await page.getByRole('button', { name: 'Izvozi v Excel' }).click();
      await expect(page.getByRole('alert').filter({ hasText: 'Zožite jo.' })).toBeVisible();
      await check(page, 'WF-06_contact-list', 'export-too-many', viewport.name);
      await page.unroute('**/api/v1/groups/*/exports');
      await page.getByRole('button', { name: 'Počisti izbiro' }).click();

      await page.getByLabel('Iskanje').fill('Priimek07');
      await expect(page.getByText('1 kontakt').first()).toBeVisible();
      await check(page, 'WF-06_contact-list', 'search', viewport.name);
      await page.getByLabel('Iskanje').fill('kompetenca');
      await expect(page.getByRole('heading', { name: 'Noben kontakt ne ustreza' })).toBeVisible();
      await check(page, 'WF-06_contact-list', 'no-match', viewport.name);
      await page.getByRole('button', { name: 'Počisti filtre' }).last().click();
      await expect(page.getByText('Stran 1 od 2').first()).toBeVisible();

      // US-009: the filter panel (always shown on desktop, behind "Filtri" below 1024 px)
      if (viewport.width < 1024) await page.getByRole('button', { name: 'Filtri' }).click();
      const filters = page.getByRole('complementary', { name: 'Filtri' });
      await filters.getByText('Izberite značke').first().click();
      await page.getByRole('option', { name: 'vip' }).click();
      await filters.getByLabel('Država').selectOption({ label: 'Slovenija' });
      await filters.getByLabel('Podjetje').selectOption({ label: 'Beta' });
      await expect(page.getByText('15 kontaktov').first()).toBeVisible();
      await check(page, 'WF-06_contact-list', 'filters', viewport.name);
      await filters.getByLabel('Država').selectOption({ label: 'Avstrija' });
      await expect(page.getByRole('heading', { name: 'Noben kontakt ne ustreza' })).toBeVisible();
      await check(page, 'WF-06_contact-list', 'filters-no-match', viewport.name);
      await page.getByRole('button', { name: 'Počisti filtre' }).last().click();
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
