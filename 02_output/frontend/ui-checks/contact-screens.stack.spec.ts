import { randomUUID } from 'node:crypto';
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
 * build-ui checks of the screens that had none (F-52, NFR-08, UI-19, UI-22): WF-08 New Contact (empty, rules,
 * suggestions), the main page of WF-07 (view, edit, stale save) and WF-19 Not found, at the three widths. Needs the
 * test stack, like `tag-screens.stack.spec.ts`.
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-08 New Contact: empty, rules, suggestions', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');
      await createContact(ana, acme, {
        firstName: 'Boris',
        tags: ['gospodar'],
        attributes: [{ key: 'kompetenca', value: 'java' }],
      });
      await openAs(page, ana, paths.newContact(acme));
      await expect(page.getByRole('heading', { level: 1, name: 'Nov kontakt' })).toBeVisible();
      await check(page, 'WF-08_new-contact', 'empty', viewport.name);

      await page.getByLabel('Telefon', { exact: true }).fill('040 123 456');
      await page.getByRole('button', { name: 'Shrani kontakt' }).click();
      await expect(
        page.getByText('Izpolnite vsaj ime, priimek, e-pošto ali podjetje.').first(),
      ).toBeVisible();
      await check(page, 'WF-08_new-contact', 'one-of-four-needed', viewport.name);

      await page.getByLabel('Ime', { exact: true }).fill('Ana');
      await page.getByLabel('E-pošta', { exact: true }).fill('ana@');
      await page.getByRole('button', { name: 'Shrani kontakt' }).click();
      await expect(page.getByText('E-poštni naslov ni veljaven.')).toBeVisible();
      await check(page, 'WF-08_new-contact', 'invalid-email', viewport.name);

      await page.getByLabel('Dodaj značko').fill('go');
      await expect(page.getByRole('option', { name: 'gospodar' })).toBeVisible();
      await page.getByRole('button', { name: 'Dodaj atribut' }).click();
      await page.getByLabel('Ključ', { exact: true }).fill('ko');
      await expect(page.getByRole('option', { name: 'kompetenca' })).toBeVisible();
      await check(page, 'WF-08_new-contact', 'suggestions', viewport.name);
    });

    test('WF-07 Contact: view, edit, stale save', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const boris = await newUser('Boris Kos', 'boris');
      const acme = await createGroup(ana, 'Acme');
      await addMember(ana, acme, boris, 'EDITOR');
      const novak = await createContact(ana, acme, {
        firstName: 'Ana',
        lastName: 'Novak',
        email: 'ana@acme.com',
        company: 'Acme d.o.o.',
        country: 'SI',
        tags: ['gost'],
        attributes: [{ key: 'kompetenca', value: 'java' }],
      });
      await openAs(page, ana, paths.contact(acme, novak.id));
      await expect(page.getByRole('heading', { level: 1 }).first()).toBeVisible();
      await expect(page.getByText('Acme d.o.o.').first()).toBeVisible();
      await check(page, 'WF-07_contact', 'view', viewport.name);

      await page.getByRole('button', { name: 'Uredi' }).click();
      await page.getByLabel('Podjetje', { exact: true }).fill('Beta d.o.o.');
      await expect(page.getByRole('button', { name: 'Shrani', exact: true })).toBeVisible();
      await check(page, 'WF-07_contact', 'edit', viewport.name);

      await editContact(boris, acme, novak.id, { city: 'Maribor' });
      await page.getByRole('button', { name: 'Shrani', exact: true }).click();
      await expect(
        page.getByRole('alert').filter({ hasText: 'Kontakt je spremenil nekdo drug.' }),
      ).toBeVisible();
      await check(page, 'WF-07_contact', 'stale-save', viewport.name);
    });

    test('WF-19 Not found: a Group of another User, an unknown page; WF-07 missing Contact', async ({
      page,
    }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const boris = await newUser('Boris Kos', 'boris');
      const acme = await createGroup(ana, 'Acme');
      const own = await createGroup(boris, 'Boris d.o.o.');

      await openAs(page, boris, paths.contacts(acme));
      await expect(page.getByRole('heading', { name: 'Strani ni mogoče najti' })).toBeVisible();
      await check(page, 'WF-19_not-found', 'group-of-another-user', viewport.name);

      // a Contact that does not exist in a Group of the User keeps the Group shell and says so (WF-07 error state)
      await page.goto(paths.contact(own, randomUUID()));
      await expect(page.getByRole('alert').filter({ hasText: 'Kontakta ni mogoče najti.' })).toBeVisible();
      await check(page, 'WF-07_contact', 'not-found', viewport.name);

      await page.goto('/ta-stran-ne-obstaja');
      await expect(page.getByRole('heading', { name: 'Strani ni mogoče najti' })).toBeVisible();
      await check(page, 'WF-19_not-found', 'unknown-page', viewport.name);
    });
  });
}
