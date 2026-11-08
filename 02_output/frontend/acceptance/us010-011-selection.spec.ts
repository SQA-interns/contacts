import { expect, test, type Page } from '@playwright/test';
import {
  addMember,
  api,
  contactRows,
  createContact,
  createGroup,
  newUser,
  openAs,
  paths,
  selectionBar,
  type Group,
  type User,
} from './support/stack';

/** US-010 Select Contacts and US-011 (Export button) on the contact list (WF-06), Editor Boris. */

async function borisWith(count: number): Promise<{ ana: User; boris: User; acme: Group }> {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const boris = await newUser('Boris Kos', 'boris');
  await addMember(ana, acme, boris, 'EDITOR');
  for (let i = 1; i <= count; i++) {
    await createContact(ana, acme, {
      firstName: 'Ime',
      lastName: `Priimek${String(i).padStart(2, '0')}`,
      city: i <= 12 ? 'Kranj' : 'Celje',
    });
  }
  return { ana, boris, acme };
}

function rowBox(page: Page, n: number) {
  return contactRows(page).nth(n).getByRole('checkbox', { name: 'Izberi kontakt' });
}

function pageBox(page: Page) {
  return page.getByRole('checkbox', { name: 'Izberi vse na tej strani' });
}

function pager(page: Page) {
  return page.getByRole('navigation', { name: 'Strani' });
}

test('AC-010-01 ticking 2 rows shows "2 selected"', async ({ page }) => {
  const { boris, acme } = await borisWith(5);
  await openAs(page, boris, paths.contacts(acme));

  await rowBox(page, 0).check();
  await rowBox(page, 1).check();

  await expect(selectionBar(page)).toContainText('2 izbrana');
});

test('AC-010-02 the 2 ticked rows stay ticked after going to page 2 and back', async ({ page }) => {
  const { boris, acme } = await borisWith(30);
  await openAs(page, boris, paths.contacts(acme));
  await rowBox(page, 0).check();
  await rowBox(page, 1).check();

  await pager(page).getByRole('button', { name: '2', exact: true }).click();
  await expect(contactRows(page).first()).toContainText('Priimek26');
  await pager(page).getByRole('button', { name: '1', exact: true }).click();

  await expect(rowBox(page, 0)).toBeChecked();
  await expect(rowBox(page, 1)).toBeChecked();
  await expect(selectionBar(page)).toContainText('2 izbrana');
});

test('AC-010-03 "select all on this page" of 60 shows "25 selected" and offers "select all 60 results"', async ({
  page,
}) => {
  const { boris, acme } = await borisWith(60);
  await openAs(page, boris, paths.contacts(acme));

  await pageBox(page).check();

  await expect(selectionBar(page)).toContainText('25 izbranih');
  await expect(selectionBar(page).getByRole('button', { name: 'Izberi vseh 60 zadetkov' })).toBeVisible();
});

test('AC-010-04 "select all 60 results" shows "60 selected"', async ({ page }) => {
  const { boris, acme } = await borisWith(60);
  await openAs(page, boris, paths.contacts(acme));
  await pageBox(page).check();

  await selectionBar(page).getByRole('button', { name: 'Izberi vseh 60 zadetkov' }).click();

  await expect(selectionBar(page)).toContainText('60 izbranih');
});

test('AC-010-05 changing the search to 12 matches clears a Selection of 60', async ({ page }) => {
  const { boris, acme } = await borisWith(60);
  await openAs(page, boris, paths.contacts(acme));
  await pageBox(page).check();
  await selectionBar(page).getByRole('button', { name: 'Izberi vseh 60 zadetkov' }).click();
  await expect(selectionBar(page)).toContainText('60 izbranih');

  await page.getByLabel('Iskanje').fill('Kranj');

  await expect(page.getByText('12 kontaktov').first()).toBeVisible();
  await expect(selectionBar(page)).toContainText('0 izbranih');
  await expect(rowBox(page, 0)).not.toBeChecked();
});

test('AC-010-06 unticking one of 2 rows shows "1 selected"', async ({ page }) => {
  const { boris, acme } = await borisWith(5);
  await openAs(page, boris, paths.contacts(acme));
  await rowBox(page, 0).check();
  await rowBox(page, 1).check();

  await rowBox(page, 1).uncheck();

  await expect(selectionBar(page)).toContainText(/\b1 izbran\b/);
});

test('AC-010-07 a Viewer sees no checkbox and no selection control', async ({ page }) => {
  const { ana, acme } = await borisWith(5);
  const viola = await newUser('Viola Vidmar', 'viola');
  await addMember(ana, acme, viola, 'VIEWER');

  await openAs(page, viola, paths.contacts(acme));

  await expect(contactRows(page)).toHaveCount(5);
  await expect(page.getByRole('table', { name: 'Kontakti skupine' }).getByRole('checkbox')).toHaveCount(0);
  await expect(selectionBar(page)).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Izvozi v Excel' })).toHaveCount(0);
});

test('AC-010-08 "Clear selection" of 60 shows "0 selected" and changes no Contact', async ({ page }) => {
  const { boris, acme } = await borisWith(60);
  const before = (await api(boris, 'GET', `/groups/${acme.id}/contacts`)).body.items;
  await openAs(page, boris, paths.contacts(acme));
  await pageBox(page).check();
  await selectionBar(page).getByRole('button', { name: 'Izberi vseh 60 zadetkov' }).click();

  await selectionBar(page).getByRole('button', { name: 'Počisti izbiro' }).click();

  await expect(selectionBar(page)).toContainText('0 izbranih');
  const after = (await api(boris, 'GET', `/groups/${acme.id}/contacts`)).body.items;
  expect(after.map((c: any) => [c.id, c.version])).toEqual(before.map((c: any) => [c.id, c.version]));
});

test('AC-010-09 changing the sort order keeps a Selection of 60', async ({ page }) => {
  const { boris, acme } = await borisWith(60);
  await openAs(page, boris, paths.contacts(acme));
  await pageBox(page).check();
  await selectionBar(page).getByRole('button', { name: 'Izberi vseh 60 zadetkov' }).click();

  await page.getByRole('columnheader', { name: 'Priimek' }).click();

  await expect(selectionBar(page)).toContainText('60 izbranih');
});

test('EX-010-01 with exactly 25 results the whole page selected offers no "select all N results"', async ({
  page,
}) => {
  const { boris, acme } = await borisWith(25);
  await openAs(page, boris, paths.contacts(acme));

  await pageBox(page).check();

  await expect(selectionBar(page)).toContainText('25 izbranih');
  await expect(selectionBar(page).getByRole('button', { name: /Izberi vseh/ })).toHaveCount(0);
});

test('EX-010-02 with 26 results the whole page selected offers "select all 26 results"', async ({ page }) => {
  const { boris, acme } = await borisWith(26);
  await openAs(page, boris, paths.contacts(acme));

  await pageBox(page).check();

  await expect(selectionBar(page).getByRole('button', { name: 'Izberi vseh 26 zadetkov' })).toBeVisible();
});

test('EX-010-03 after a reload the Selection is empty', async ({ page }) => {
  const { boris, acme } = await borisWith(5);
  await openAs(page, boris, paths.contacts(acme));
  await rowBox(page, 0).check();
  await expect(selectionBar(page)).toContainText(/\b1 izbran\b/);

  await page.reload();

  await expect(contactRows(page)).toHaveCount(5);
  await expect(rowBox(page, 0)).not.toBeChecked();
  await expect(selectionBar(page)).toContainText('0 izbranih');
});

test('EX-010-04 changing the search back to the earlier text leaves the Selection cleared', async ({
  page,
}) => {
  const { boris, acme } = await borisWith(30);
  await openAs(page, boris, paths.contacts(acme));
  await page.getByLabel('Iskanje').fill('Kranj');
  await expect(page.getByText('12 kontaktov').first()).toBeVisible();
  await rowBox(page, 0).check();
  await rowBox(page, 1).check();

  await page.getByLabel('Iskanje').fill('Celje');
  await expect(page.getByText('18 kontaktov').first()).toBeVisible();
  await page.getByLabel('Iskanje').fill('Kranj');

  await expect(page.getByText('12 kontaktov').first()).toBeVisible();
  await expect(selectionBar(page)).toContainText('0 izbranih');
  await expect(rowBox(page, 0)).not.toBeChecked();
});

test('AC-011-01 with nothing selected the Export button is disabled', async ({ page }) => {
  const { boris, acme } = await borisWith(3);

  await openAs(page, boris, paths.contacts(acme));

  await expect(contactRows(page)).toHaveCount(3);
  await expect(page.getByRole('button', { name: 'Izvozi v Excel' })).toBeDisabled();
});

test('AC-011-05 an export of more than 10,000 shows the message to narrow the Selection', async ({
  page,
}) => {
  const { boris, acme } = await borisWith(3);
  await page.route('**/api/v1/groups/*/exports', (route) =>
    route.fulfill({
      status: 422,
      contentType: 'application/json',
      body: JSON.stringify({ code: 'export.too_many', message: 'Too many' }),
    }),
  );
  await openAs(page, boris, paths.contacts(acme));
  await pageBox(page).check();

  await page.getByRole('button', { name: 'Izvozi v Excel' }).click();

  await expect(
    page.getByRole('alert').filter({ hasText: 'Izbira ima več kot 10.000 kontaktov. Zožite jo.' }),
  ).toBeVisible();
});
