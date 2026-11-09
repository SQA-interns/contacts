import { expect, test } from '@playwright/test';
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
 * build-ui checks of the tab Različice of WF-07 (UI-14, UI-19, UI-22): the list, an earlier Version, the comparison,
 * "no differences" and the error state, at the three widths. Needs the test stack, like `tag-screens.stack.spec.ts`.
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-07 Versions: list, open, compare, no differences, error', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const boris = await newUser('Boris Kos', 'boris');
      const acme = await createGroup(ana, 'Acme');
      await addMember(ana, acme, boris, 'EDITOR');
      const novak = await createContact(ana, acme, {
        firstName: 'Ana',
        lastName: 'Novak',
        company: 'Acme d.o.o.',
        country: 'SI',
        tags: ['gost'],
        attributes: [{ key: 'kompetenca', value: 'java' }],
      });
      await editContact(boris, acme, novak.id, { city: 'Celje' });
      await editContact(boris, acme, novak.id, {
        company: 'Beta d.o.o.',
        tags: ['vip'],
        attributes: [{ key: 'kompetenca', value: 'kotlin' }],
      });

      await openAs(page, ana, paths.contact(acme, novak.id));
      await page.getByRole('link', { name: 'Različice' }).click();
      await expect(page.getByRole('heading', { name: 'Različice' })).toBeVisible();
      await expect(page.getByText('trenutna').filter({ visible: true })).toBeVisible();
      await check(page, 'WF-07_versions', 'list', viewport.name);

      await page.getByRole('button', { name: 'Odpri' }).filter({ visible: true }).last().click();
      await expect(page.getByRole('heading', { name: 'Različica 1' })).toBeVisible();
      await check(page, 'WF-07_versions', 'open', viewport.name);
      await page.getByRole('button', { name: 'Nazaj na različice' }).click();

      await page
        .getByRole('button', { name: 'Primerjaj s trenutno' })
        .filter({ visible: true })
        .last()
        .click();
      await expect(page.getByRole('heading', { name: /Razlike: različica 1/ })).toBeVisible();
      await expect(page.getByText('(dodana)').filter({ visible: true })).toBeVisible();
      await check(page, 'WF-07_versions', 'compare', viewport.name);
      await page.getByRole('button', { name: 'Nazaj na različice' }).click();

      await page.route('**/versions/2/compare', (route) =>
        route.fulfill({ json: { number: 2, differences: [] } }),
      );
      await page
        .getByRole('button', { name: 'Primerjaj s trenutno' })
        .filter({ visible: true })
        .first()
        .click();
      await expect(page.getByText('Ni razlik.')).toBeVisible();
      await check(page, 'WF-07_versions', 'no-differences', viewport.name);

      await page.route('**/versions', (route) =>
        route.fulfill({ status: 500, json: { code: 'x', message: 'x' } }),
      );
      await page.goto(`${paths.contact(acme, novak.id)}?tab=versions`);
      await expect(page.getByRole('button', { name: 'Poskusi znova' })).toBeVisible();
      await check(page, 'WF-07_versions', 'error', viewport.name);
    });
  });
}
