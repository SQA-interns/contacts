import { expect, test } from '@playwright/test';
import { addMember, api, createContact, createGroup, newUser, openAs, paths, signIn } from './support/stack';

/** US-012/013 (WF-07, WF-10), US-014 (WF-04), US-015 (WF-11). */

test('AC-012-01 Delete asks for confirmation and the Contact is still listed', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const boris = await newUser('Boris Kos', 'boris');
  await addMember(ana, acme, boris, 'EDITOR');
  const c = await createContact(ana, acme, { firstName: 'Ana', lastName: 'Novak' });
  await openAs(page, boris, paths.contact(acme, c.id));

  await page.getByRole('button', { name: 'Izbriši' }).click();

  await expect(page.getByRole('dialog', { name: 'Premaknem kontakt v koš?' })).toBeVisible();
  expect((await api(ana, 'GET', `/groups/${acme.id}/contacts`)).body.total).toBe(1);
});

test('EX-012-03 cancelling the delete confirmation changes nothing', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const c = await createContact(ana, acme, { firstName: 'Ana', lastName: 'Novak' });
  await openAs(page, ana, paths.contact(acme, c.id));

  await page.getByRole('button', { name: 'Izbriši' }).click();
  await page
    .getByRole('dialog', { name: 'Premaknem kontakt v koš?' })
    .getByRole('button', { name: 'Prekliči' })
    .click();

  await expect(page.getByRole('dialog')).toHaveCount(0);
  expect((await api(ana, 'GET', `/groups/${acme.id}/contacts`)).body.total).toBe(1);
  expect((await api(ana, 'GET', `/groups/${acme.id}/trash/contacts`)).body).toHaveLength(0);
});

test('AC-013-02 cancelling "Delete forever" keeps the Contact in Trash', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const c = await createContact(ana, acme, { firstName: 'Mojca', lastName: 'Zupan' });
  expect((await api(ana, 'POST', `/groups/${acme.id}/contacts/${c.id}/trash`)).status).toBe(204);
  await openAs(page, ana, paths.trash(acme));

  await page
    .getByRole('row')
    .filter({ hasText: 'Zupan' })
    .getByRole('button', { name: 'Izbriši za vedno' })
    .click();
  const dialog = page.getByRole('dialog', { name: 'Trajno izbrišem kontakt?' });
  await expect(dialog).toContainText('Tega ni mogoče razveljaviti.');
  await dialog.getByRole('button', { name: 'Prekliči' }).click();

  await expect(page.getByRole('row').filter({ hasText: 'Zupan' })).toBeVisible();
  const trash = (await api(ana, 'GET', `/groups/${acme.id}/trash/contacts`)).body;
  expect(trash.map((t: any) => t.id)).toEqual([c.id]);
});

test('AC-014-03 Boris, invited but unverified, sees "Verify your e-mail" and not Acme', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const boris = await newUser('Boris Kos', 'boris', false);
  expect(
    (await api(ana, 'POST', `/groups/${acme.id}/invitations`, { email: boris.email, role: 'EDITOR' })).status,
  ).toBe(201);

  await signIn(page, boris, false);

  await expect(
    page.getByRole('heading', { level: 1, name: 'Potrdite svoj e-poštni naslov, da nadaljujete.' }),
  ).toBeVisible();
  await expect(page.getByText('Acme')).toHaveCount(0);
});

test('AC-015-01 Viola sees three Members with names, e-mails and Roles and no change controls', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const boris = await newUser('Boris Kos', 'boris');
  const viola = await newUser('Viola Vidmar', 'viola');
  await addMember(ana, acme, boris, 'EDITOR');
  await addMember(ana, acme, viola, 'VIEWER');

  await openAs(page, viola, paths.members(acme));

  const table = page.getByRole('table', { name: 'Člani skupine' });
  for (const [name, email, role] of [
    ['Ana Novak', ana.email, 'Lastnik'],
    ['Boris Kos', boris.email, 'Urednik'],
    ['Viola Vidmar', viola.email, 'Ogledovalec'],
  ]) {
    const row = table.getByRole('row').filter({ hasText: name });
    await expect(row).toContainText(email);
    await expect(row).toContainText(role);
  }
  await expect(page.getByRole('button', { name: 'Odstrani' })).toHaveCount(0);
  await expect(page.getByRole('combobox', { name: 'Vloga' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Povabi osebo' })).toHaveCount(0);
});

test('AC-015-04 the only Owner trying to become Editor sees "A group needs at least one owner"', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  await openAs(page, ana, paths.members(acme));

  const row = page
    .getByRole('table', { name: 'Člani skupine' })
    .getByRole('row')
    .filter({ hasText: 'Ana Novak' });
  const role = row.getByRole('combobox', { name: 'Vloga' });
  const tag = await role.evaluate((el) => el.tagName.toLowerCase());
  if (tag === 'select') {
    await role.selectOption({ label: 'Urednik' });
  } else {
    await role.click();
    await page.getByRole('option', { name: 'Urednik', exact: true }).click();
  }

  await expect(
    page.getByRole('alert').filter({ hasText: 'Skupina potrebuje vsaj enega lastnika.' }),
  ).toBeVisible();
  const groups = (await api(ana, 'GET', '/groups')).body;
  expect(groups.find((g: any) => g.id === acme.id).role).toBe('OWNER');
});
