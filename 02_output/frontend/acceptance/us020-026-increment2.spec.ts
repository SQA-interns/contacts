import { expect, test, type Page } from '@playwright/test';
import {
  accountsWith,
  api,
  createContact,
  createGroup,
  newUser,
  openAs,
  paths,
  signIn,
  type Group,
  type User,
} from './support/stack';

/** Increment 2 screens: US-020 (WF-14), US-021 (WF-16), US-024 (WF-17), US-026 (WF-18). */

async function openDeleteDialog(page: Page) {
  await page.getByRole('button', { name: 'Možnosti skupine' }).first().click();
  await page.getByRole('menuitem', { name: 'Izbriši skupino' }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByRole('heading')).toContainText('Izbrišem skupino');
  return dialog;
}

test('AC-020-01 typing Acme enables the delete; Acme leaves the Group list', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  await signIn(page, ana);

  const dialog = await openDeleteDialog(page);
  await dialog.getByLabel('Za potrditev vpišite ime skupine').fill('Acme');
  await expect(dialog.getByRole('button', { name: 'Izbriši skupino' })).toBeEnabled();
  await dialog.getByRole('button', { name: 'Izbriši skupino' }).click();

  await expect(page.getByRole('heading', { name: 'Acme', exact: true })).toHaveCount(0);
  const trash = (await api(ana, 'GET', '/group-trash')).body;
  expect(trash.map((g: any) => [g.id, g.daysLeft])).toEqual([[acme.id, 30]]);
});

test('AC-020-02 typing acme keeps the delete button disabled; the Group stays', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  await signIn(page, ana);

  const dialog = await openDeleteDialog(page);
  await dialog.getByLabel('Za potrditev vpišite ime skupine').fill('acme');

  await expect(dialog.getByRole('button', { name: 'Izbriši skupino' })).toBeDisabled();
  const groups = (await api(ana, 'GET', '/groups')).body;
  expect(groups.map((g: any) => g.id)).toContain(acme.id);
});

test('AC-021-02 the Sole Owner of Acme sees that the account cannot be deleted, with Acme listed', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  await createGroup(ana, 'Acme');
  await openAs(page, ana, '/account');

  await page.getByRole('button', { name: 'Izbriši račun' }).click();
  await page
    .getByRole('dialog', { name: 'Izbrišem račun?' })
    .getByRole('button', { name: 'Izbriši račun' })
    .click();

  const alert = page
    .getByRole('alert')
    .filter({ hasText: 'Računa ni mogoče izbrisati, ker ste edini lastnik skupine.' });
  await expect(alert).toBeVisible();
  await expect(page.getByText('Acme').first()).toBeVisible();
  expect(await accountsWith(ana.email)).toBe(1);
});

test('EX-021-03 cancelling the account deletion changes nothing', async ({ page }) => {
  const cene = await newUser('Cene Zupan', 'cene');
  await openAs(page, cene, '/account');

  await page.getByRole('button', { name: 'Izbriši račun' }).click();
  const dialog = page.getByRole('dialog', { name: 'Izbrišem račun?' });
  await expect(dialog).toContainText('Tega ni mogoče razveljaviti.');
  await dialog.getByRole('button', { name: 'Prekliči' }).click();

  await expect(page.getByRole('dialog')).toHaveCount(0);
  expect(await accountsWith(cene.email)).toBe(1);
  expect((await api(cene, 'GET', '/me')).status).toBe(200);
});

test('AC-024-01 three Contacts sharing ana@acme.com are one certain set with the reason', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  for (const [firstName, email] of [
    ['Ana', 'ana@acme.com'],
    ['Anica', 'ANA@acme.com'],
    ['Anja', 'Ana@Acme.com'],
  ]) {
    await createContact(ana, acme, { firstName, email });
  }

  await openAs(page, ana, paths.duplicates(acme));

  await expect(page.getByText('Zanesljivo')).toBeVisible();
  await expect(page.getByText(/3 kontakti imajo e-pošto/)).toBeVisible();
  await expect(page.getByText('ana@acme.com').first()).toBeVisible();
});

test('AC-024-10 without duplicates "No duplicates found" is shown', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  await createContact(ana, acme, { firstName: 'Ana', lastName: 'Novak', email: 'ana@acme.com' });

  await openAs(page, ana, paths.duplicates(acme));

  await expect(page.getByRole('heading', { name: 'Ni podvojenih kontaktov' })).toBeVisible();
});

async function pair(
  a: Record<string, unknown>,
  b: Record<string, unknown>,
): Promise<{ ana: User; acme: Group; idA: string; idB: string }> {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const idA = (await createContact(ana, acme, { email: 'skupna@acme.com', ...a })).id;
  const idB = (await createContact(ana, acme, { email: 'skupna@acme.com', ...b })).id;
  return { ana, acme, idA, idB };
}

async function openMerge(page: Page, user: User, group: Group) {
  await openAs(page, user, paths.duplicates(group));
  await page.getByRole('button', { name: /Združi izbrane \(2\)/ }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Združi kontakte' })).toBeVisible();
}

test('AC-026-01 the merge screen shows both; first name is preselected from A, phone from B', async ({
  page,
}) => {
  const { ana, acme } = await pair({ firstName: 'Ana' }, { firstName: 'ana', phone: '040 123 456' });

  await openMerge(page, ana, acme);

  await expect(page.getByRole('radio', { name: 'Ana', exact: true })).toBeChecked();
  await expect(page.getByRole('radio', { name: 'ana', exact: true })).not.toBeChecked();
  await expect(page.getByRole('radio', { name: '040 123 456' })).toBeChecked();
});

test('AC-026-03 two values of kompetenca need a choice before the merge', async ({ page }) => {
  const { ana, acme, idA, idB } = await pair(
    { firstName: 'Ana', attributes: [{ key: 'kompetenca', value: 'java' }] },
    { firstName: 'Anica', attributes: [{ key: 'kompetenca', value: 'spletnoProgramiranje' }] },
  );
  await openMerge(page, ana, acme);

  await expect(page.getByRole('radio', { name: 'java' })).not.toBeChecked();
  await expect(page.getByRole('radio', { name: 'spletnoProgramiranje' })).not.toBeChecked();
  await page.getByRole('button', { name: 'Združi', exact: true }).click();

  await expect(page.getByText('Izberite vrednost.')).toBeVisible();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  expect((await api(ana, 'GET', `/groups/${acme.id}/contacts/${idB}`)).status).toBe(200);
  expect((await api(ana, 'GET', `/groups/${acme.id}/contacts/${idA}`)).body.version).toBe(1);
});

test('AC-026-05 and AC-026-12 "Združi" asks for confirmation; without it nothing is merged', async ({
  page,
}) => {
  const { ana, acme, idA, idB } = await pair({ firstName: 'Ana' }, { firstName: 'Anica' });
  await openMerge(page, ana, acme);

  await page.getByRole('button', { name: 'Združi', exact: true }).click();
  const dialog = page.getByRole('dialog', { name: 'Združim 2 kontakta?' });
  await expect(dialog).toBeVisible();
  await dialog.getByRole('button', { name: 'Prekliči' }).click();

  await expect(page.getByRole('dialog')).toHaveCount(0);
  expect((await api(ana, 'GET', `/groups/${acme.id}/contacts/${idA}`)).body.version).toBe(1);
  expect((await api(ana, 'GET', `/groups/${acme.id}/contacts/${idB}`)).status).toBe(200);
  expect((await api(ana, 'GET', `/groups/${acme.id}/trash/contacts`)).body).toHaveLength(0);
});

test('EX-026-02 a value all Contacts share is preselected and needs no choice', async ({ page }) => {
  const { ana, acme } = await pair(
    { firstName: 'Ana', company: 'Acme' },
    { firstName: 'Anica', company: 'Acme' },
  );
  await openMerge(page, ana, acme);

  const companyRow = page.getByRole('row').filter({ has: page.getByText('Podjetje', { exact: true }) });
  await expect(companyRow).toContainText('Acme');
  await page.getByRole('button', { name: 'Združi', exact: true }).click();

  await expect(page.getByRole('dialog', { name: 'Združim 2 kontakta?' })).toBeVisible();
  await expect(page.getByText('Izberite vrednost.')).toHaveCount(0);
});

test('EX-026-03 with equal creation times the Contact with the smaller id is the Survivor', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const created = '2026-11-06T10:00:00Z';
  const small = '00000000-0000-4000-8000-00000000000a';
  const large = '00000000-0000-4000-8000-00000000000b';
  const item = (id: string, firstName: string) => ({
    id,
    firstName,
    lastName: 'Novak',
    email: 'skupna@acme.com',
    company: null,
    city: null,
    country: null,
    tags: [],
    version: 1,
    createdAt: created,
  });
  const full = (id: string, firstName: string) => ({
    ...item(id, firstName),
    phone: null,
    jobTitle: null,
    street: null,
    postalCode: null,
    notes: null,
    attributes: [],
    updatedAt: created,
  });
  await page.route('**/api/v1/groups/*/duplicates', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          key: 'k',
          mark: 'certain',
          reasonCode: 'duplicate.same_email',
          reasonParams: { email: 'skupna@acme.com', count: '2' },
          contacts: [item(large, 'Velika'), item(small, 'Mala')],
        },
      ]),
    }),
  );
  for (const [id, name] of [
    [small, 'Mala'],
    [large, 'Velika'],
  ] as const) {
    await page.route(`**/api/v1/groups/*/contacts/${id}`, (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(full(id, name)) }),
    );
  }

  await openMerge(page, ana, acme);

  await expect(page.getByRole('columnheader', { name: /Ohranjeni kontakt/ })).toContainText('Mala');
});
