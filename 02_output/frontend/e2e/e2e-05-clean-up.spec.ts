import { expect, test } from '@playwright/test';
import {
  addMember,
  api,
  createContact,
  createGroup,
  newUser,
  paths,
  signIn,
} from '../acceptance/support/stack';

/**
 * E2E-05 Clean-up, second increment (e2e-scenarios.md): US-024, US-026, US-012.
 * Covers AC-024-01, AC-026-04, AC-026-07, AC-026-10, DoD-P05. Whether the restore also reverts the Survivor
 * (OQ-026-02) is not checked; only that the Contact is back.
 */
test('E2E-05 clean-up: one certain set, merge field by field, restore the merged-away Contact', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const editor = await newUser('Eva Horvat', 'eva');
  await addMember(ana, acme, editor, 'EDITOR');

  // 1. two Contacts with the same e-mail exist (A is the older)
  const a = await createContact(ana, acme, {
    firstName: 'Mojca',
    lastName: 'Zupan',
    email: 'mojca@zupan.si',
    company: 'Zupan d.o.o.',
  });
  const b = await createContact(ana, acme, {
    firstName: 'Mojca',
    lastName: 'Zupan',
    email: 'MOJCA@zupan.si',
    phone: '041 222 333',
  });

  // 2. the duplicate search shows one certain set
  await signIn(page, editor);
  await page.goto(paths.duplicates(acme));
  await expect(page.getByText('Zanesljivo')).toHaveCount(1);
  await expect(page.getByText('Verjetno')).toHaveCount(0);
  await expect(page.getByText(/2 kontakta imata e-pošto/)).toBeVisible();

  // 3. merge field by field on the Merge screen and confirm
  await page.getByRole('button', { name: /Združi izbrane \(2\)/ }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Združi kontakte' })).toBeVisible();
  await page.getByRole('radio', { name: '041 222 333' }).check();
  await page.getByRole('radio', { name: 'Zupan d.o.o.' }).check();
  await page.getByRole('button', { name: 'Združi', exact: true }).click();
  await page
    .getByRole('dialog', { name: 'Združim 2 kontakta?' })
    .getByRole('button', { name: 'Združi', exact: true })
    .click();
  await expect(page.getByRole('status').filter({ hasText: 'Kontakti so združeni.' })).toBeVisible();

  // the Survivor shows the merged data and its History shows the merge
  await page.goto(paths.contact(acme, a.id));
  await expect(page.getByText('041 222 333')).toBeVisible();
  await expect(page.getByText('Zupan d.o.o.')).toBeVisible();
  await page.getByRole('navigation', { name: 'Zavihki kontakta' }).getByText('Zgodovina').click();
  await expect(page.getByRole('main')).toContainText('Združeno');

  // 4. the merged-away Contact is in Trash; restoring it brings it back to the list
  await page.goto(paths.trash(acme));
  const trashed = page
    .getByRole('table', { name: 'Kontakti v košu' })
    .getByRole('row')
    .filter({ hasText: 'Zupan' });
  await expect(trashed).toHaveCount(1);
  await trashed.getByRole('button', { name: 'Obnovi' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Kontakt je obnovljen.' })).toBeVisible();
  await page.goto(paths.contacts(acme));
  await expect(
    page.getByRole('table', { name: 'Kontakti skupine' }).getByRole('row').filter({ hasText: 'Zupan' }),
  ).toHaveCount(2);
  const list = (await api(ana, 'GET', `/groups/${acme.id}/contacts`)).body.items.map((c: any) => c.id);
  expect(list).toEqual(expect.arrayContaining([a.id, b.id]));
});
