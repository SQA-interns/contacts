import { expect, test, type Page } from '@playwright/test';
import {
  addMember,
  choose,
  contactRows,
  createContact,
  createGroup,
  newUser,
  openAs,
  paths,
  type Group,
  type User,
} from './support/stack';

/** US-007 Browse, US-008 Search, US-009 Filter on the contact list (WF-06), desktop width. */

async function acmeWith(
  count: number,
  extra: (i: number) => Record<string, unknown> = () => ({}),
): Promise<{ ana: User; acme: Group }> {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  for (let i = 1; i <= count; i++) {
    await createContact(ana, acme, {
      firstName: 'Ime',
      lastName: `Priimek${String(i).padStart(2, '0')}`,
      ...extra(i),
    });
  }
  return { ana, acme };
}

function search(page: Page) {
  return page.getByLabel('Iskanje');
}

function pager(page: Page) {
  return page.getByRole('navigation', { name: 'Strani' });
}

test('AC-007-01 Acme with 60 Contacts shows 25 sorted by last name and "60 contacts"', async ({ page }) => {
  const { ana, acme } = await acmeWith(60);

  await openAs(page, ana, paths.contacts(acme));

  await expect(page.getByText('60 kontaktov').first()).toBeVisible();
  await expect(contactRows(page)).toHaveCount(25);
  await expect(contactRows(page).first()).toContainText('Priimek01');
  await expect(contactRows(page).last()).toContainText('Priimek25');
});

test('AC-007-03 choosing the header Company twice sorts ascending, then descending', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  for (const company of ['Beta', 'Cezar', 'Acme'])
    await createContact(ana, acme, { firstName: company, company });
  await openAs(page, ana, paths.contacts(acme));
  await expect(contactRows(page)).toHaveCount(3);

  await page.getByRole('columnheader', { name: 'Podjetje' }).click();
  await expect(contactRows(page).first()).toContainText('Acme');
  await expect(contactRows(page).last()).toContainText('Cezar');

  await page.getByRole('columnheader', { name: 'Podjetje' }).click();
  await expect(contactRows(page).first()).toContainText('Cezar');
  await expect(contactRows(page).last()).toContainText('Acme');
});

test('AC-007-04 an empty Acme shows an Editor the empty state with "Add contact"', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const boris = await newUser('Boris Kos', 'boris');
  await addMember(ana, acme, boris, 'EDITOR');

  await openAs(page, boris, paths.contacts(acme));

  await expect(page.getByRole('heading', { name: 'V skupini še ni kontaktov' })).toBeVisible();
  await expect(page.getByText('Kontakt je oseba ali organizacija, ki jo hranite v skupini.')).toBeVisible();
  await expect(
    page
      .getByRole('button', { name: 'Dodaj kontakt' })
      .or(page.getByRole('link', { name: 'Dodaj kontakt' }))
      .first(),
  ).toBeVisible();
});

test('AC-007-05 an empty Acme shows a Viewer the empty state without "Add contact"', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const viola = await newUser('Viola Vidmar', 'viola');
  await addMember(ana, acme, viola, 'VIEWER');

  await openAs(page, viola, paths.contacts(acme));

  await expect(page.getByRole('heading', { name: 'V skupini še ni kontaktov' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Dodaj kontakt' })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Dodaj kontakt' })).toHaveCount(0);
});

test('AC-007-07 choosing the row of Ana Novak opens the Contact', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const c = await createContact(ana, acme, { firstName: 'Ana', lastName: 'Novak' });
  await openAs(page, ana, paths.contacts(acme));

  await contactRows(page).filter({ hasText: 'Novak' }).getByText('Novak').click();

  await expect(page).toHaveURL((url) => url.pathname.endsWith(`/contacts/${c.id}`));
  await expect(
    page
      .getByRole('link', { name: 'Podrobnosti' })
      .or(page.getByRole('tab', { name: 'Podrobnosti' }))
      .first(),
  ).toBeVisible();
});

test('EX-007-07 a very long company name stays available in full in the list', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const company = `Zelo dolgo ime podjetja ${'in še daljše '.repeat(12)}d.o.o.`.trim();
  await createContact(ana, acme, { firstName: 'Ana', lastName: 'Novak', company });

  await openAs(page, ana, paths.contacts(acme));

  const row = contactRows(page).filter({ hasText: 'Novak' });
  const cellText = await row.innerText();
  const titled = await row.locator(`[title="${company}"]`).count();
  expect(cellText.includes(company) || titled > 0).toBe(true);
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
  );
  expect(overflow).toBeLessThanOrEqual(0);
});

test('AC-008-05 searching kompetenca (a key only) shows "No contacts match" and "Clear filters"', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  await createContact(ana, acme, {
    firstName: 'Ana',
    lastName: 'Novak',
    attributes: [{ key: 'kompetenca', value: 'java' }],
  });
  await openAs(page, ana, paths.contacts(acme));

  await search(page).fill('kompetenca');

  await expect(page.getByRole('heading', { name: 'Noben kontakt ne ustreza' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Počisti filtre' }).last()).toBeVisible();
});

test('AC-008-09 typing a search on page 3 shows page 1 of 2 and "30 contacts"', async ({ page }) => {
  const { ana, acme } = await acmeWith(60, (i) => ({ city: i % 2 === 0 ? 'Sodovci' : 'Lihovci' }));
  await openAs(page, ana, paths.contacts(acme));
  await pager(page).getByRole('button', { name: '3', exact: true }).click();
  await expect(pager(page)).toContainText('Stran 3 od 3');

  await search(page).fill('Sodovci');

  await expect(pager(page)).toContainText('Stran 1 od 2');
  await expect(page.getByText('30 kontaktov').first()).toBeVisible();
});

test('EX-008-04 the search text <script> finds nothing and is shown as text', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  await createContact(ana, acme, { firstName: 'Ana', lastName: 'Novak' });
  let dialogs = 0;
  page.on('dialog', async (d) => {
    dialogs++;
    await d.dismiss();
  });
  await openAs(page, ana, paths.contacts(acme));

  await search(page).fill('<script>');

  await expect(page.getByRole('heading', { name: 'Noben kontakt ne ustreza' })).toBeVisible();
  await expect(search(page)).toHaveValue('<script>');
  expect(dialogs).toBe(0);
});

test('AC-009-09 changing a filter on page 2 shows page 1 and the new count', async ({ page }) => {
  const { ana, acme } = await acmeWith(40, (i) => ({ country: i <= 30 ? 'SI' : 'AT' }));
  await openAs(page, ana, paths.contacts(acme));
  await pager(page).getByRole('button', { name: '2', exact: true }).click();
  await expect(pager(page)).toContainText('Stran 2 od 2');

  await choose(page, 'Država', 'Slovenija');

  await expect(pager(page)).toContainText('Stran 1 od 2');
  await expect(page.getByText('30 kontaktov').first()).toBeVisible();
});

test('AC-009-10 "Clear filters" shows the full list again', async ({ page }) => {
  const { ana, acme } = await acmeWith(4, (i) => ({ country: i <= 1 ? 'SI' : 'AT' }));
  await openAs(page, ana, paths.contacts(acme));
  await choose(page, 'Država', 'Slovenija');
  await search(page).fill('Priimek01');
  await expect(contactRows(page)).toHaveCount(1);

  await page.getByRole('button', { name: 'Počisti filtre' }).first().click();

  await expect(contactRows(page)).toHaveCount(4);
  await expect(search(page)).toHaveValue('');
  await expect(page.getByText('4 kontakti').first()).toBeVisible();
});

test('AC-009-11 the Tag filter of Acme does not offer gost of Beta', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  await createContact(ana, acme, { firstName: 'Ana', tags: ['udelezenec2025'] });
  const beta = await createGroup(ana, 'Beta');
  await createContact(ana, beta, { firstName: 'Cene', tags: ['gost'] });
  await openAs(page, ana, paths.contacts(acme));

  const filters = page.getByRole('complementary', { name: 'Filtri' });
  await filters.getByText('Izberite značke').first().click();

  await expect(page.getByRole('option', { name: 'udelezenec2025' }).first()).toBeVisible();
  await expect(page.getByRole('option', { name: 'gost', exact: true })).toHaveCount(0);
});

test('AC-009-12 a filter that matches nothing shows "No contacts match" and "Clear filters"', async ({
  page,
}) => {
  const { ana, acme } = await acmeWith(3, () => ({ country: 'SI' }));
  await createContact(ana, acme, { firstName: 'Avstrijec', country: 'AT' });
  await openAs(page, ana, paths.contacts(acme));
  await search(page).fill('Priimek01');

  await choose(page, 'Država', 'Avstrija');

  await expect(page.getByRole('heading', { name: 'Noben kontakt ne ustreza' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Počisti filtre' }).last()).toBeVisible();
});
