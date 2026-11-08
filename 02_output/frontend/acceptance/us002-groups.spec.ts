import { expect, test, type Page } from '@playwright/test';
import { api, createGroup, newUser, signIn } from './support/stack';

/** US-002 Create a Group and see my Groups: screen WF-05. */

async function openCreateDialog(page: Page) {
  await page.getByRole('button', { name: 'Ustvari skupino' }).first().click();
  const dialog = page.getByRole('dialog', { name: 'Nova skupina' });
  await expect(dialog).toBeVisible();
  return dialog;
}

test('AC-002-07 a User with no Group sees the empty state with "Create group"', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');

  await signIn(page, ana);

  await expect(page.getByRole('heading', { name: 'Še nimate nobene skupine' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Ustvari skupino' }).first()).toBeVisible();
});

test('AC-002-01 creating Acme shows it in the Group list with Role Owner', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  await signIn(page, ana);

  const dialog = await openCreateDialog(page);
  await dialog.getByLabel('Ime skupine').fill('Acme');
  await dialog.getByRole('button', { name: 'Ustvari', exact: true }).click();

  await page.goto('/groups');
  const card = page.getByRole('heading', { name: 'Acme', exact: true });
  await expect(card).toBeVisible();
  await expect(page.getByText('Lastnik').first()).toBeVisible();
  const groups = (await api(ana, 'GET', '/groups')).body;
  expect(groups.map((g: any) => [g.name, g.role])).toEqual([['Acme', 'OWNER']]);
});

test('AC-002-03 creating ACME next to Acme shows "You already have a group with this name"', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  await createGroup(ana, 'Acme');
  await signIn(page, ana);

  const dialog = await openCreateDialog(page);
  await dialog.getByLabel('Ime skupine').fill('ACME');
  await dialog.getByRole('button', { name: 'Ustvari', exact: true }).click();

  await expect(dialog.getByText('Skupino s tem imenom že imate.')).toBeVisible();
  expect((await api(ana, 'GET', '/groups')).body).toHaveLength(1);
});

test('AC-002-05 an empty name shows the rule next to the field and creates nothing', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  await signIn(page, ana);

  const dialog = await openCreateDialog(page);
  await dialog.getByLabel('Ime skupine').fill('');
  await dialog.getByRole('button', { name: 'Ustvari', exact: true }).click();

  await expect(dialog.getByText('Ime mora imeti od 1 do 100 znakov.')).toBeVisible();
  expect((await api(ana, 'GET', '/groups')).body).toHaveLength(0);
});
