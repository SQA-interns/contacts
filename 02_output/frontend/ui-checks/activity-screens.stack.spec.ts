import { expect, test } from '@playwright/test';
import {
  addMember,
  choose,
  createContact,
  createGroup,
  fillDate,
  newUser,
  openAs,
  paths,
} from '../acceptance/support/stack';
import { check } from './check';
import { VIEWPORTS } from './viewports';

/**
 * build-ui checks of the states of WF-13 (Group activity; UI-13, UI-14, UI-19, UI-22): list, empty result, start
 * after end, Viewer refusal, at the three widths. Needs the test stack.
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-13 list, filtered, no match, range message, viewer refused', async ({
      page,
      browser,
      baseURL,
    }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');
      const boris = await newUser('Boris Kos', 'boris');
      await addMember(ana, acme, boris, 'EDITOR');
      const viola = await newUser('Viola Vidmar', 'viola');
      await addMember(ana, acme, viola, 'VIEWER');
      await createContact(ana, acme, { firstName: 'Ana', lastName: 'Novak' });
      await createContact(boris, acme, { firstName: 'Cene', lastName: 'Zupan' });

      await openAs(page, ana, paths.activity(acme));
      await expect(page.getByRole('heading', { level: 1, name: 'Dejavnost skupine' })).toBeVisible();
      await expect(
        page
          .getByRole('table', { name: 'Dejavnost skupine' })
          .or(page.getByRole('list', { name: 'Dejavnost skupine' }))
          .first(),
      ).toBeVisible();
      await check(page, 'WF-13_group-activity', 'list', viewport.name);

      await choose(page, 'Uporabnik', 'Boris Kos');
      await choose(page, 'Dejanje', 'Kontakt ustvarjen');
      await expect(
        page
          .getByText(/Boris Kos · /)
          .filter({ visible: true })
          .first(),
      ).toBeVisible();
      await check(page, 'WF-13_group-activity', 'filtered', viewport.name);

      await choose(page, 'Dejanje', 'Kontakti izvoženi');
      await expect(page.getByRole('heading', { name: 'Nobena dejavnost ne ustreza' })).toBeVisible();
      await check(page, 'WF-13_group-activity', 'empty', viewport.name);

      await page.getByRole('button', { name: 'Počisti filtre' }).first().click();
      await fillDate(page.getByLabel('Od', { exact: true }), '2026-11-10');
      await fillDate(page.getByLabel('Do', { exact: true }), '2026-11-01');
      await expect(page.getByText('Začetni datum je po končnem.')).toBeVisible();
      await check(page, 'WF-13_group-activity', 'range-invalid', viewport.name);

      const context = await browser.newContext({
        baseURL,
        viewport: { width: viewport.width, height: viewport.height },
      });
      const violaPage = await context.newPage();
      await openAs(violaPage, viola, paths.activity(acme));
      await expect(violaPage.getByRole('alert')).toContainText('nimate pravice');
      await check(violaPage, 'WF-13_group-activity', 'viewer-refused', viewport.name);
      await context.close();
    });
  });
}
