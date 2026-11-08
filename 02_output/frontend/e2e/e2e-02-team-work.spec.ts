import { expect, test } from '@playwright/test';
import {
  api,
  createContact,
  createGroup,
  newUser,
  PASSWORD,
  paths,
  signIn,
  unique,
  verifyByLink,
} from '../acceptance/support/stack';

/**
 * E2E-02 Team work (e2e-scenarios.md): US-014, US-015, US-003, US-016, US-017.
 * Covers AC-014-01, AC-014-02, AC-003-05, AC-016-02, AC-015-02, AC-015-07, AC-017-01, DoD-P01.
 */
test('E2E-02 team work: Ana invites Boris as Editor, he edits, Ana sees the History and makes him Viewer', async ({
  browser,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const contact = await createContact(ana, acme, {
    firstName: 'Ana',
    lastName: 'Novak',
    company: 'Acme d.o.o.',
  });
  const borisEmail = unique('boris');
  const anaContext = await browser.newContext();
  const borisContext = await browser.newContext();
  const anaPage = await anaContext.newPage();
  const borisPage = await borisContext.newPage();

  // 1. Ana invites Boris as Editor
  await signIn(anaPage, ana);
  await anaPage.goto(paths.invitations(acme));
  await anaPage.getByLabel('E-pošta').fill(borisEmail);
  const role = anaPage.getByLabel('Vloga');
  if ((await role.evaluate((el) => el.tagName.toLowerCase())) === 'select')
    await role.selectOption({ label: 'Urednik' });
  else {
    await role.click();
    await anaPage.getByRole('option', { name: 'Urednik', exact: true }).click();
  }
  await anaPage.getByRole('button', { name: 'Ustvari povabilo' }).click();
  await expect(anaPage.getByRole('table', { name: 'Odprta povabila' })).toContainText(borisEmail);

  // 2. Boris registers, verifies and signs in: Acme is in his list with Role Editor
  await borisPage.goto('/register');
  await borisPage.getByLabel('Prikazno ime').fill('Boris Kos');
  await borisPage.getByLabel('E-pošta').fill(borisEmail);
  await borisPage.getByLabel('Geslo').fill(PASSWORD);
  await borisPage.getByRole('button', { name: 'Ustvari račun' }).click();
  await expect(
    borisPage.getByRole('heading', { level: 1, name: 'Potrdite svoj e-poštni naslov, da nadaljujete.' }),
  ).toBeVisible();
  await verifyByLink(borisEmail);
  await borisPage.reload();
  if (await borisPage.getByRole('heading', { level: 1, name: 'Prijava' }).isVisible()) {
    await borisPage.getByLabel('E-pošta').fill(borisEmail);
    await borisPage.getByLabel('Geslo').fill(PASSWORD);
    await borisPage.getByRole('button', { name: 'Prijava', exact: true }).click();
  }
  await expect(borisPage.getByRole('heading', { level: 1, name: 'Moje skupine' })).toBeVisible();
  await expect(borisPage.getByRole('heading', { name: 'Acme', exact: true })).toBeVisible();
  await expect(borisPage.getByText('Urednik').first()).toBeVisible();

  // 3. Boris changes the company of Ana Novak to Beta d.o.o.
  await borisPage.goto(paths.contact(acme, contact.id));
  await borisPage.getByRole('button', { name: 'Uredi' }).click();
  await borisPage.getByLabel('Podjetje', { exact: true }).fill('Beta d.o.o.');
  await borisPage.getByRole('button', { name: 'Shrani', exact: true }).click();
  await expect(borisPage.getByText('Beta d.o.o.').first()).toBeVisible();
  await expect(borisPage.getByText('Različica 2')).toBeVisible();

  // 4. Ana opens the History tab: one entry by Boris, company Acme d.o.o. → Beta d.o.o.
  await anaPage.goto(paths.contact(acme, contact.id));
  await anaPage.getByRole('navigation', { name: 'Zavihki kontakta' }).getByText('Zgodovina').click();
  const main = anaPage.getByRole('main');
  await expect(main.getByRole('heading', { name: 'Zgodovina' })).toBeVisible();
  await expect(main).toContainText('Urejeno');
  await expect(main).toContainText('Acme d.o.o.');
  await expect(main).toContainText('Beta d.o.o.');
  await expect(main.getByText('Boris Kos')).toHaveCount(1);

  // 5. Ana changes Boris to Viewer; his next attempt to edit is refused; the activity list shows the change
  await borisPage.goto(paths.contact(acme, contact.id));
  await borisPage.getByRole('button', { name: 'Uredi' }).click();
  await borisPage.getByLabel('Podjetje', { exact: true }).fill('Gama d.o.o.');
  await anaPage.goto(paths.members(acme));
  const borisRow = anaPage
    .getByRole('table', { name: 'Člani skupine' })
    .getByRole('row')
    .filter({ hasText: 'Boris Kos' });
  const select = borisRow.getByRole('combobox', { name: 'Vloga' });
  if ((await select.evaluate((el) => el.tagName.toLowerCase())) === 'select')
    await select.selectOption({ label: 'Ogledovalec' });
  else {
    await select.click();
    await anaPage.getByRole('option', { name: 'Ogledovalec', exact: true }).click();
  }
  await expect(anaPage.getByRole('status').filter({ hasText: 'Vloga je spremenjena.' })).toBeVisible();
  await borisPage.getByRole('button', { name: 'Shrani', exact: true }).click();
  await expect(borisPage.getByRole('alert').first()).toBeVisible();
  const stored = (await api(ana, 'GET', `/groups/${acme.id}/contacts/${contact.id}`)).body;
  expect(stored.company).toBe('Beta d.o.o.');
  await anaPage.goto(paths.activity(acme));
  const activity = anaPage.getByRole('table', { name: 'Dejavnost skupine' });
  await expect(activity.getByRole('row').filter({ hasText: 'Ogledovalec' }).first()).toContainText(
    'Ana Novak',
  );

  await anaContext.close();
  await borisContext.close();
});
