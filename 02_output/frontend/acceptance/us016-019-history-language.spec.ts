import { expect, test, type Page } from '@playwright/test';
import {
  addMember,
  choose,
  contactRows,
  createContact,
  createGroup,
  editContact,
  fillDate,
  newUser,
  openAs,
  paths,
  signIn,
  type Group,
  type User,
} from './support/stack';

/**
 * US-016 (History tab, WF-07), US-017 (WF-13), US-018 (plain text), US-019 (language switch). Times shown in
 * Europe/Ljubljana with the Slovene format `d. M. yyyy HH:mm` and the English format `yyyy-MM-dd HH:mm`
 * (business-rules.md, project-wide facts). Where a test needs entries at given UTC times, the History answer
 * is served by the test in the format of the contract (HistoryPage).
 */

async function acmeWithContact(fields: Record<string, unknown> = { firstName: 'Ana', lastName: 'Novak' }) {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const c = await createContact(ana, acme, fields);
  return { ana, acme, c };
}

async function serveHistory(page: Page, contactId: string, times: string[]) {
  const items = times.map((occurredAt, i) => ({
    id: `00000000-0000-4000-8000-0000000000${String(10 + i)}`,
    occurredAt,
    userName: 'Ana Novak',
    userEmail: 'ana@acme.com',
    action: 'CONTACT_EDITED',
    contactId,
    details: {},
  }));
  await page.route('**/api/v1/groups/*/contacts/*/history**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ items, page: 1, pageSize: 25, total: items.length }),
    }),
  );
}

async function openHistoryTab(page: Page, user: User, group: Group, contactId: string) {
  await openAs(page, user, paths.contact(group, contactId));
  await page.getByRole('navigation', { name: 'Zavihki kontakta' }).getByText('Zgodovina').click();
}

async function switchTo(page: Page, lang: 'SL' | 'EN') {
  await page
    .getByRole('group', { name: /Jezik|Language/ })
    .getByRole('button', { name: lang, exact: true })
    .first()
    .click();
  await expect(page.locator('html')).toHaveAttribute('lang', lang.toLowerCase());
}

test('AC-016-03 entries of 2026-01-15 10:00 and 2026-07-01 10:00 UTC read 15. 1. 2026 11:00 and 1. 7. 2026 12:00', async ({
  page,
}) => {
  const { ana, acme, c } = await acmeWithContact();
  await serveHistory(page, c.id, ['2026-07-01T10:00:00Z', '2026-01-15T10:00:00Z']);

  await openHistoryTab(page, ana, acme, c.id);

  await expect(page.getByText('1. 7. 2026 12:00')).toBeVisible();
  await expect(page.getByText('15. 1. 2026 11:00')).toBeVisible();
});

test('EX-016-04 2026-03-29 00:30 UTC reads 01:30 (CET) and 01:30 UTC reads 03:30 (CEST)', async ({
  page,
}) => {
  const { ana, acme, c } = await acmeWithContact();
  await serveHistory(page, c.id, ['2026-03-29T01:30:00Z', '2026-03-29T00:30:00Z']);

  await openHistoryTab(page, ana, acme, c.id);

  await expect(page.getByText('29. 3. 2026 03:30')).toBeVisible();
  await expect(page.getByText('29. 3. 2026 01:30')).toBeVisible();
});

test('AC-017-06 a start date after the end date shows the message next to the dates and no list', async ({
  page,
}) => {
  const { ana, acme } = await acmeWithContact();
  await openAs(page, ana, paths.activity(acme));
  await expect(page.getByRole('table', { name: 'Dejavnost skupine' })).toBeVisible();

  await fillDate(page.getByLabel('Od', { exact: true }), '2026-11-10');
  await fillDate(page.getByLabel('Do', { exact: true }), '2026-11-01');

  await expect(page.getByText('Začetni datum je po končnem.')).toBeVisible();
  await expect(page.getByRole('table', { name: 'Dejavnost skupine' })).toHaveCount(0);
});

test('AC-017-07 filters that match nothing show "No activity matches" and a clear button', async ({
  page,
}) => {
  const { ana, acme } = await acmeWithContact();
  await openAs(page, ana, paths.activity(acme));

  await choose(page, 'Dejanje', 'Kontakti izvoženi');

  await expect(page.getByRole('heading', { name: 'Nobena dejavnost ne ustreza' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Počisti filtre' }).last()).toBeVisible();
});

test('AC-018-16 the last name <script>alert(1)</script> is listed and opened as typed; no script runs', async ({
  page,
}) => {
  const { ana, acme, c } = await acmeWithContact({ firstName: 'Ana', lastName: '<script>alert(1)</script>' });
  let dialogs = 0;
  page.on('dialog', async (d) => {
    dialogs++;
    await d.dismiss();
  });

  await openAs(page, ana, paths.contacts(acme));
  await expect(contactRows(page).first()).toContainText('<script>alert(1)</script>');
  await page.goto(paths.contact(acme, c.id));
  await expect(page.getByText('<script>alert(1)</script>').first()).toBeVisible();

  expect(dialogs).toBe(0);
});

test.describe('in a browser whose language is English', () => {
  test.use({ locale: 'en-US' });

  test('AC-019-01 the product opens in Slovene', async ({ page }) => {
    await page.goto('/sign-in');

    await expect(page.getByRole('heading', { level: 1, name: 'Prijava' })).toBeVisible();
    await expect(page.locator('html')).toHaveAttribute('lang', 'sl');
  });
});

test('AC-019-02 switching to English keeps the half-filled Contact form', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  await openAs(page, ana, paths.newContact(acme));
  await page.getByLabel('Ime', { exact: true }).fill('Ana');
  await page.getByLabel('Priimek', { exact: true }).fill('Novak');

  await switchTo(page, 'EN');

  await expect(page.getByLabel('Priimek', { exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Shrani kontakt' })).toHaveCount(0);
  const values = await page
    .locator('input')
    .evaluateAll((inputs) => inputs.map((i) => (i as HTMLInputElement).value));
  expect(values).toContain('Ana');
  expect(values).toContain('Novak');
});

test('AC-019-03 the language chosen last is used when the product is opened again', async ({
  page,
  browser,
}) => {
  await page.goto('/sign-in');
  await switchTo(page, 'EN');
  const state = await page.context().storageState();
  await page.context().close();

  const reopened = await browser.newContext({ storageState: state, locale: 'sl-SI' });
  const again = await reopened.newPage();
  await again.goto('/sign-in');

  await expect(again.locator('html')).toHaveAttribute('lang', 'en');
  await expect(again.getByRole('heading', { level: 1, name: 'Prijava' })).toHaveCount(0);
  await reopened.close();
});

test('AC-019-04 the entry of 2026-11-08 10:00 UTC reads 8. 11. 2026 11:00, then 2026-11-08 11:00', async ({
  page,
}) => {
  const { ana, acme, c } = await acmeWithContact();
  await serveHistory(page, c.id, ['2026-11-08T10:00:00Z']);
  await openHistoryTab(page, ana, acme, c.id);
  await expect(page.getByText('8. 11. 2026 11:00')).toBeVisible();

  await switchTo(page, 'EN');

  await expect(page.getByText('2026-11-08 11:00')).toBeVisible();
  await expect(page.getByText('8. 11. 2026 11:00')).toHaveCount(0);
});

test('AC-019-05 the country SI reads Slovenija, then Slovenia', async ({ page }) => {
  const { ana, acme, c } = await acmeWithContact({ firstName: 'Ana', country: 'SI' });
  await openAs(page, ana, paths.contact(acme, c.id));
  await expect(page.getByText('Slovenija', { exact: true })).toBeVisible();

  await switchTo(page, 'EN');

  await expect(page.getByText('Slovenia', { exact: true })).toBeVisible();
  await expect(page.getByText('Slovenija', { exact: true })).toHaveCount(0);
});

test('AC-019-06 the Tag neObvescaj and the Group Šola stay unchanged when the language is switched', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  const sola = await createGroup(ana, 'Šola');
  await createContact(ana, sola, { firstName: 'Ana', lastName: 'Novak', tags: ['neObvescaj'] });
  await openAs(page, ana, paths.contacts(sola));
  await expect(contactRows(page).first()).toContainText('neObvescaj');

  await switchTo(page, 'EN');

  await expect(page.getByRole('table', { name: 'Group contacts' }).getByRole('row').nth(1)).toContainText(
    'neObvescaj',
  );
  await expect(page.getByText('Šola').first()).toBeVisible();
});

test('AC-019-07 the error "changed by someone else" is shown in Slovene, then in English', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const boris = await newUser('Boris Kos', 'boris');
  await addMember(ana, acme, boris, 'EDITOR');
  const c = await createContact(ana, acme, { firstName: 'Ana', lastName: 'Novak' });
  await openAs(page, ana, paths.contact(acme, c.id));
  await page.getByRole('button', { name: 'Uredi' }).click();
  await page.getByLabel('Podjetje', { exact: true }).fill('Beta');
  await editContact(boris, acme, c.id, { city: 'Maribor' });
  await page.getByRole('button', { name: 'Shrani', exact: true }).click();
  await expect(page.getByRole('alert').filter({ hasText: 'Kontakt je spremenil nekdo drug.' })).toBeVisible();

  await switchTo(page, 'EN');

  await expect(page.getByRole('alert').filter({ hasText: /changed by someone else/i })).toBeVisible();
  await expect(page.getByText('Kontakt je spremenil nekdo drug.')).toHaveCount(0);
});

test('EX-019-01 with browser storage blocked Slovene is used at every visit and nothing fails', async ({
  page,
}) => {
  await page.addInitScript(() => {
    const blocked = () => {
      throw new DOMException('blocked', 'SecurityError');
    };
    Object.defineProperty(window, 'localStorage', { get: blocked, configurable: true });
    Object.defineProperty(window, 'sessionStorage', { get: blocked, configurable: true });
  });
  await page.goto('/sign-in');
  await expect(page.getByRole('heading', { level: 1, name: 'Prijava' })).toBeVisible();
  await switchTo(page, 'EN');

  await page.reload();

  await expect(page.getByRole('heading', { level: 1, name: 'Prijava' })).toBeVisible();
  await expect(page.locator('html')).toHaveAttribute('lang', 'sl');
  await expect(page.getByText('Nekaj je šlo narobe. Poskusite znova.')).toHaveCount(0);
});

test('EX-019-02 a result that arrives after the switch is shown in the new language', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  await createGroup(ana, 'Acme');
  await signIn(page, ana);
  await page.route('**/api/v1/groups', async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 2500));
    await route.continue();
  });

  await page.reload();
  await switchTo(page, 'EN');

  await expect(page.getByRole('heading', { name: 'Acme' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Moje skupine' })).toHaveCount(0);
  await expect(page.getByText('Lastnik')).toHaveCount(0);
});
