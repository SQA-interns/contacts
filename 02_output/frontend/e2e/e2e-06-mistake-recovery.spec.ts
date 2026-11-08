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
 * E2E-06 Mistake recovery (e2e-scenarios.md): US-012, US-013, US-016.
 * Covers AC-012-02, AC-012-03, AC-012-04, AC-013-01, DoD-P01.
 */
test('E2E-06 mistake recovery: delete and restore Ana Novak, then delete another Contact forever', async ({
  browser,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const editor = await newUser('Eva Horvat', 'eva');
  await addMember(ana, acme, editor, 'EDITOR');
  const anaNovak = await createContact(ana, acme, {
    firstName: 'Ana',
    lastName: 'Novak',
    tags: ['udelezenec2025'],
    attributes: [{ key: 'kompetenca', value: 'spletnoProgramiranje' }],
  });
  const other = await createContact(ana, acme, { firstName: 'Cene', lastName: 'Zupan' });
  const editorContext = await browser.newContext();
  const ownerContext = await browser.newContext();
  const page = await editorContext.newPage();
  const owner = await ownerContext.newPage();
  const rows = (p: typeof page) => p.getByRole('table', { name: 'Kontakti skupine' }).getByRole('row');

  // 1. the Editor deletes Ana Novak; the Contact leaves the list
  await signIn(page, editor);
  await page.goto(paths.contact(acme, anaNovak.id));
  await page.getByRole('button', { name: 'Izbriši' }).click();
  await page
    .getByRole('dialog', { name: 'Premaknem kontakt v koš?' })
    .getByRole('button', { name: 'Premakni v koš' })
    .click();
  await expect(page.getByRole('status').filter({ hasText: 'Kontakt je v košu.' })).toBeVisible();
  await page.goto(paths.contacts(acme));
  await expect(rows(page).filter({ hasText: 'Zupan' })).toHaveCount(1);
  await expect(rows(page).filter({ hasText: 'Novak' })).toHaveCount(0);

  // 2. she finds it in the Trash view and restores it; it is back with its Tag and Attribute
  await page.goto(paths.trash(acme));
  const inTrash = page
    .getByRole('table', { name: 'Kontakti v košu' })
    .getByRole('row')
    .filter({ hasText: 'Novak' });
  await expect(inTrash).toContainText('30 dni');
  await inTrash.getByRole('button', { name: 'Obnovi' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Kontakt je obnovljen.' })).toBeVisible();
  await page.goto(paths.contacts(acme));
  await expect(rows(page).filter({ hasText: 'Novak' })).toContainText('udelezenec2025');
  await page.goto(paths.contact(acme, anaNovak.id));
  await expect(page.getByText('udelezenec2025').first()).toBeVisible();
  await expect(page.getByText('spletnoProgramiranje').first()).toBeVisible();

  // 3. the History tab shows the delete and the restore
  await page.getByRole('navigation', { name: 'Zavihki kontakta' }).getByText('Zgodovina').click();
  await expect(page.getByRole('main')).toContainText('Premaknjeno v koš');
  await expect(page.getByRole('main')).toContainText('Obnovljeno iz koša');

  // 4. the Owner moves another Contact to Trash and deletes it forever with confirmation
  await signIn(owner, ana);
  await owner.goto(paths.contact(acme, other.id));
  await owner.getByRole('button', { name: 'Izbriši' }).click();
  await owner
    .getByRole('dialog', { name: 'Premaknem kontakt v koš?' })
    .getByRole('button', { name: 'Premakni v koš' })
    .click();
  await expect(owner.getByRole('status').filter({ hasText: 'Kontakt je v košu.' })).toBeVisible();
  await owner.goto(paths.trash(acme));
  await owner
    .getByRole('row')
    .filter({ hasText: 'Zupan' })
    .getByRole('button', { name: 'Izbriši za vedno' })
    .click();
  await owner
    .getByRole('dialog', { name: 'Trajno izbrišem kontakt?' })
    .getByRole('button', { name: 'Izbriši za vedno' })
    .click();
  await expect(owner.getByRole('row').filter({ hasText: 'Zupan' })).toHaveCount(0);
  expect((await api(ana, 'GET', `/groups/${acme.id}/contacts/${other.id}`)).status).toBe(404);

  await editorContext.close();
  await ownerContext.close();
});
