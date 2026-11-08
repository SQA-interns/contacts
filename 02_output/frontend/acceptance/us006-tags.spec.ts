import { expect, test } from '@playwright/test';
import {
  api,
  createContact,
  createGroup,
  newUser,
  openAs,
  paths,
  type Group,
  type User,
} from './support/stack';

/** US-006 Rename and delete a Tag: screen WF-09. */

async function gostOnFive(): Promise<{ ana: User; acme: Group }> {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  for (let i = 1; i <= 5; i++) await createContact(ana, acme, { firstName: `Oseba${i}`, tags: ['gost'] });
  return { ana, acme };
}

async function gostCount(ana: User, acme: Group): Promise<number | undefined> {
  const tags = (await api(ana, 'GET', `/groups/${acme.id}/tags`)).body as any[];
  return tags.find((t) => t.name === 'gost')?.contactCount;
}

test('AC-006-04 the deletion asks with "5 contacts"; after confirming the Tag is gone, the Contacts stay', async ({
  page,
}) => {
  const { ana, acme } = await gostOnFive();
  await openAs(page, ana, paths.tags(acme));

  await page.getByRole('row').filter({ hasText: 'gost' }).getByRole('button', { name: 'Izbriši' }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog).toContainText('5 kontaktov');
  await dialog.getByRole('button', { name: 'Izbriši značko' }).click();

  await expect(page.getByRole('row').filter({ hasText: 'gost' })).toHaveCount(0);
  expect(await gostCount(ana, acme)).toBeUndefined();
  expect((await api(ana, 'GET', `/groups/${acme.id}/contacts`)).body.total).toBe(5);
});

test('AC-006-05 cancelling the deletion changes nothing', async ({ page }) => {
  const { ana, acme } = await gostOnFive();
  await openAs(page, ana, paths.tags(acme));

  await page.getByRole('row').filter({ hasText: 'gost' }).getByRole('button', { name: 'Izbriši' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Prekliči' }).click();

  await expect(page.getByRole('dialog')).toHaveCount(0);
  await expect(page.getByRole('row').filter({ hasText: 'gost' })).toContainText('5');
  expect(await gostCount(ana, acme)).toBe(5);
});
