import { expect, test } from '@playwright/test';
import { createContact, createGroup, newUser, openAs, paths } from '../acceptance/support/stack';
import { check } from './check';
import { VIEWPORTS } from './viewports';

/**
 * build-ui checks of WF-17 (UI-14, UI-19, UI-22): the list of Duplicate sets, "No duplicates found", the search
 * running and the error state, at the three widths. Needs the test stack, like `tag-screens.stack.spec.ts`.
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-17 Duplicates: list, empty, loading, error', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');
      for (const firstName of ['Ana', 'Anica', 'Anja']) {
        await createContact(ana, acme, { firstName, email: 'ana@acme.com', company: 'Acme d.o.o.' });
      }
      await createContact(ana, acme, {
        firstName: 'Boris',
        lastName: 'Kos',
        company: 'Beta d.o.o.',
        city: 'Celje',
      });
      await createContact(ana, acme, { firstName: 'boris', lastName: 'KOS', company: 'BETA d.o.o.' });
      await createContact(ana, acme, {
        firstName: 'Cene',
        lastName: 'Zupan',
        phone: '041 222 333',
      });
      await createContact(ana, acme, { firstName: 'Cene', lastName: 'Zupan', phone: '041-222-333' });

      await openAs(page, ana, paths.duplicates(acme));
      await expect(page.getByRole('heading', { level: 1, name: 'Podvojeni kontakti' })).toBeVisible();
      await expect(page.getByText('Zanesljivo')).toHaveCount(1);
      await expect(page.getByText('Verjetno')).toHaveCount(2);
      await check(page, 'WF-17_duplicates', 'list', viewport.name);

      await page.route('**/api/v1/groups/*/duplicates', (route) => route.fulfill({ json: [] }));
      await page.getByRole('button', { name: 'Poišči znova' }).click();
      await expect(page.getByRole('heading', { name: 'Ni podvojenih kontaktov' })).toBeVisible();
      await check(page, 'WF-17_duplicates', 'empty', viewport.name);
      await page.unroute('**/api/v1/groups/*/duplicates');

      let release: () => void = () => undefined;
      const gate = new Promise<void>((resolve) => (release = resolve));
      await page.route('**/api/v1/groups/*/duplicates', async (route) => {
        await gate;
        await route.fulfill({ json: [] });
      });
      await page.reload();
      await expect(page.getByText('Iskanje podvojenih kontaktov …')).toBeVisible();
      await check(page, 'WF-17_duplicates', 'loading', viewport.name);
      release();
      await expect(page.getByRole('heading', { name: 'Ni podvojenih kontaktov' })).toBeVisible();
      await page.unroute('**/api/v1/groups/*/duplicates');

      await page.route('**/api/v1/groups/*/duplicates', (route) =>
        route.fulfill({ status: 500, json: { code: 'x', message: 'x' } }),
      );
      await page.reload();
      await expect(page.getByRole('button', { name: 'Poskusi znova' })).toBeVisible();
      await check(page, 'WF-17_duplicates', 'error', viewport.name);
    });
  });
}
