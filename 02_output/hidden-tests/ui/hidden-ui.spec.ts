import { expect, test } from '@playwright/test';
import { accounts, bar, call, contact, group, join, lang, mail, person, register, rows, signIn } from './h';

/** Hidden UI cases of rules that only the screens show. */

const verify = 'Potrdite svoj e-poštni naslov, da nadaljujete.';

test('R-001-01 boundary a password of 9 characters creates the account', async ({ page }) => {
  const email = mail('devet');
  await register(page, 'Devet Znakov', email, 'Geslo1234');
  await expect(page.getByRole('heading', { level: 1, name: verify })).toBeVisible();
  expect(await accounts(email)).toBe(1);
});

test('R-001-03 other a verified sign-in on a second browser also opens the Group list', async ({ browser }) => {
  const u = await person('Mojca Kos', 'mojca');
  const ctx = await browser.newContext();
  await signIn(await ctx.newPage(), u);
  await ctx.close();
});

test('R-001-04 other the unverified User gets the verification page on a Group address too', async ({ page }) => {
  const owner = await person('Ana Novak', 'ana');
  const g = await group(owner, 'Acme');
  const u = await person('Neža Kos', 'neza', false);
  await signIn(page, u, false);
  await page.goto(`/groups/${g.id}/contacts`);
  await expect(page.getByRole('heading', { level: 1, name: verify })).toBeVisible();
});

test('R-001-05 invalid an address that is not registered shows the same single message', async ({ page }) => {
  await page.goto('/sign-in');
  await page.getByLabel('E-pošta').fill(mail('nihce'));
  await page.getByLabel('Geslo').fill('Karkoli123');
  await page.getByRole('button', { name: 'Prijava', exact: true }).click();
  await expect(page.getByRole('alert')).toHaveText('E-pošta ali geslo ni pravilno.');
});

test('R-001-06 boundary a password of 1 character creates no account', async ({ page }) => {
  const email = mail('ena');
  await register(page, 'Ena Crka', email, 'x');
  await expect(page.getByText('Geslo mora imeti vsaj 8 znakov.')).toBeVisible();
  expect(await accounts(email)).toBe(0);
});

test('R-001-07 other a registered address typed in upper case offers sign-in and reset', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  await register(page, 'Ana Druga', u.email.toUpperCase(), 'Geslo1234');
  await expect(page.getByRole('button', { name: 'Ponastavi geslo' })).toBeVisible();
  expect(await accounts(u.email)).toBe(1);
});

test('R-001-08 other two resets for registered addresses show the same confirmation', async ({ page }) => {
  const a = await person('A A', 'a');
  const texts: string[] = [];
  for (const email of [a.email, mail('drugi')]) {
    await page.goto('/reset-password');
    await page.getByLabel('E-pošta').fill(email);
    await page.getByRole('button', { name: 'Pošlji povezavo' }).click();
    texts.push((await page.getByRole('status').innerText()).trim());
  }
  expect(texts[0]).toBe(texts[1]);
});

test('R-001-09 other a reload on a Contact keeps the User signed in on the Contact', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  const c = await contact(u, g, { firstName: 'Mojca', lastName: 'Zupan' });
  await signIn(page, u);
  await page.goto(`/groups/${g.id}/contacts/${c}`);
  await page.reload();
  await expect(page.getByText('Zupan').first()).toBeVisible();
});

test('R-001-10 other after signing out a Contact address shows the sign-in page', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  const c = await contact(u, g, { firstName: 'Mojca', lastName: 'Zupan' });
  await signIn(page, u);
  await page.getByRole('button', { name: 'Uporabniški meni' }).click();
  await page.getByRole('menuitem', { name: 'Odjava' }).click();
  await page.goto(`/groups/${g.id}/contacts/${c}`);
  await expect(page.getByRole('heading', { level: 1, name: 'Prijava' })).toBeVisible();
  await expect(page.getByText('Zupan')).toHaveCount(0);
});

test('R-001-12 other a 401 on a Contact page returns to the sign-in page', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  await signIn(page, u);
  await page.route('**/api/v1/**', (r) => r.fulfill({ status: 401, contentType: 'application/json', body: '{"code":"auth.token_invalid","message":"x"}' }));
  await page.goto(`/groups/${g.id}/contacts`);
  await expect(page.getByRole('heading', { level: 1, name: 'Prijava' })).toBeVisible();
});

test('R-002-06 role an Owner whose only Group was deleted sees the empty state again', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  expect((await call(u, 'POST', `/groups/${g.id}/trash`, { name: 'Acme' })).status).toBe(204);
  await signIn(page, u);
  await expect(page.getByRole('heading', { name: 'Še nimate nobene skupine' })).toBeVisible();
});

test('R-003-03 combination with two errors both messages show and every entered value stays', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  await signIn(page, u);
  await page.goto(`/groups/${g.id}/contacts/new`);
  await page.getByLabel('Ime', { exact: true }).fill('Mojca');
  await page.getByLabel('E-pošta', { exact: true }).fill('mojca@');
  await page.getByLabel('Telefon', { exact: true }).fill('abc');
  await page.getByLabel('Kraj', { exact: true }).fill('Bled');
  await page.getByRole('button', { name: 'Shrani kontakt' }).click();
  await expect(page.getByText('E-poštni naslov ni veljaven.')).toBeVisible();
  await expect(page.getByText('Telefon lahko vsebuje številke, presledke in znake + - ( ) . /.')).toBeVisible();
  await expect(page.getByLabel('Kraj', { exact: true })).toHaveValue('Bled');
  expect((await call(u, 'GET', `/groups/${g.id}/contacts`)).body.total).toBe(0);
});

test('R-007-05 other a row on page 2 opens its Contact', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  for (let i = 1; i <= 26; i++) await contact(u, g, { lastName: `P${String(i).padStart(2, '0')}` });
  await signIn(page, u);
  await page.goto(`/groups/${g.id}/contacts`);
  await page.getByRole('navigation', { name: 'Strani' }).getByRole('button', { name: '2', exact: true }).click();
  await rows(page).first().getByText('P26').click();
  await expect(page).toHaveURL(/\/contacts\/[0-9a-f-]{36}$/);
});

test('R-007-06 role an Owner sees "Add contact" in the empty state', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  await signIn(page, u);
  await page.goto(`/groups/${g.id}/contacts`);
  await expect(page.getByRole('heading', { name: 'V skupini še ni kontaktov' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Dodaj kontakt' }).or(page.getByRole('link', { name: 'Dodaj kontakt' })).first()).toBeVisible();
});

test('R-008-04 other "Clear filters" after a search without matches shows the list again', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  await contact(u, g, { lastName: 'Zupan' });
  await signIn(page, u);
  await page.goto(`/groups/${g.id}/contacts`);
  await page.getByLabel('Iskanje').fill('nicesar');
  await expect(page.getByRole('heading', { name: 'Noben kontakt ne ustreza' })).toBeVisible();
  await page.getByRole('button', { name: 'Počisti filtre' }).last().click();
  await expect(rows(page)).toHaveCount(1);
});

async function editorWith(n: number) {
  const owner = await person('Ana Novak', 'ana');
  const g = await group(owner, 'Acme');
  const ed = await person('Boris Kos', 'boris');
  await join(owner, g, ed, 'EDITOR');
  for (let i = 1; i <= n; i++) await contact(owner, g, { lastName: `P${String(i).padStart(2, '0')}`, country: i <= 5 ? 'AT' : 'SI' });
  return { owner, g, ed };
}

test('R-010-01 role an Owner sees a checkbox on every row', async ({ page }) => {
  const { owner, g } = await editorWith(4);
  await signIn(page, owner);
  await page.goto(`/groups/${g.id}/contacts`);
  await expect(rows(page).getByRole('checkbox', { name: 'Izberi kontakt' })).toHaveCount(4);
});

test('R-010-02 role a Viewer has no "select all on this page"', async ({ page }) => {
  const { owner, g } = await editorWith(4);
  const v = await person('Viola Vidmar', 'viola');
  await join(owner, g, v, 'VIEWER');
  await signIn(page, v);
  await page.goto(`/groups/${g.id}/contacts`);
  await expect(rows(page)).toHaveCount(4);
  await expect(page.getByRole('checkbox', { name: 'Izberi vse na tej strani' })).toHaveCount(0);
});

test('R-010-03 other ticking 3 rows shows "3 selected"', async ({ page }) => {
  const { ed, g } = await editorWith(5);
  await signIn(page, ed);
  await page.goto(`/groups/${g.id}/contacts`);
  for (let i = 0; i < 3; i++) await rows(page).nth(i).getByRole('checkbox').check();
  await expect(bar(page)).toContainText('3 izbrani');
});

test('R-010-04 other "select all on this page" of 10 shows "10 selected" and no offer', async ({ page }) => {
  const { ed, g } = await editorWith(10);
  await signIn(page, ed);
  await page.goto(`/groups/${g.id}/contacts`);
  await page.getByRole('checkbox', { name: 'Izberi vse na tej strani' }).check();
  await expect(bar(page)).toContainText('10 izbranih');
  await expect(bar(page).getByRole('button', { name: /Izberi vseh/ })).toHaveCount(0);
});

test('R-010-05 boundary with 50 results the offer reads "select all 50 results"', async ({ page }) => {
  const { ed, g } = await editorWith(50);
  await signIn(page, ed);
  await page.goto(`/groups/${g.id}/contacts`);
  await page.getByRole('checkbox', { name: 'Izberi vse na tej strani' }).check();
  await expect(bar(page).getByRole('button', { name: 'Izberi vseh 50 zadetkov' })).toBeVisible();
});

test('R-010-06 other "select all 30 results" shows "30 selected"', async ({ page }) => {
  const { ed, g } = await editorWith(30);
  await signIn(page, ed);
  await page.goto(`/groups/${g.id}/contacts`);
  await page.getByRole('checkbox', { name: 'Izberi vse na tej strani' }).check();
  await bar(page).getByRole('button', { name: 'Izberi vseh 30 zadetkov' }).click();
  await expect(bar(page)).toContainText('30 izbranih');
});

test('R-010-07 combination a page change and then a sort change keep "3 selected"', async ({ page }) => {
  const { ed, g } = await editorWith(30);
  await signIn(page, ed);
  await page.goto(`/groups/${g.id}/contacts`);
  for (let i = 0; i < 3; i++) await rows(page).nth(i).getByRole('checkbox').check();
  await page.getByRole('navigation', { name: 'Strani' }).getByRole('button', { name: '2', exact: true }).click();
  await page.getByRole('columnheader', { name: 'Priimek' }).click();
  await expect(bar(page)).toContainText('3 izbrani');
});

test('R-010-08 other a country filter change clears the Selection', async ({ page }) => {
  const { ed, g } = await editorWith(8);
  await signIn(page, ed);
  await page.goto(`/groups/${g.id}/contacts`);
  await rows(page).nth(0).getByRole('checkbox').check();
  const country = page.getByLabel('Država', { exact: true });
  if ((await country.evaluate((e) => e.tagName.toLowerCase())) === 'select') await country.selectOption({ label: 'Avstrija' });
  else {
    await country.click();
    await page.getByRole('option', { name: 'Avstrija', exact: true }).click();
  }
  await expect(rows(page)).toHaveCount(5);
  await expect(bar(page)).toContainText('0 izbranih');
});

test('R-010-09 other unticking one row after "select all on this page" of 10 shows "9 selected"', async ({ page }) => {
  const { ed, g } = await editorWith(10);
  await signIn(page, ed);
  await page.goto(`/groups/${g.id}/contacts`);
  await page.getByRole('checkbox', { name: 'Izberi vse na tej strani' }).check();
  await rows(page).nth(4).getByRole('checkbox').uncheck();
  await expect(bar(page)).toContainText('9 izbranih');
});

test('R-010-10 other selecting and clearing changes no version', async ({ page }) => {
  const { ed, g, owner } = await editorWith(3);
  const before = (await call(owner, 'GET', `/groups/${g.id}/contacts`)).body.items.map((c: any) => c.version);
  await signIn(page, ed);
  await page.goto(`/groups/${g.id}/contacts`);
  await page.getByRole('checkbox', { name: 'Izberi vse na tej strani' }).check();
  await bar(page).getByRole('button', { name: 'Počisti izbiro' }).click();
  const after = (await call(owner, 'GET', `/groups/${g.id}/contacts`)).body.items.map((c: any) => c.version);
  expect(after).toEqual(before);
});

test('R-011-01 combination after clearing a Selection the Export button is disabled again', async ({ page }) => {
  const { ed, g } = await editorWith(3);
  await signIn(page, ed);
  await page.goto(`/groups/${g.id}/contacts`);
  await rows(page).nth(0).getByRole('checkbox').check();
  await expect(page.getByRole('button', { name: 'Izvozi v Excel' })).toBeEnabled();
  await rows(page).nth(0).getByRole('checkbox').uncheck();
  await expect(page.getByRole('button', { name: 'Izvozi v Excel' })).toBeDisabled();
});

test('R-012-01 other the delete confirmation says the Contact stays 30 days in Trash', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  const c = await contact(u, g, { lastName: 'Zupan' });
  await signIn(page, u);
  await page.goto(`/groups/${g.id}/contacts/${c}`);
  await page.getByRole('button', { name: 'Izbriši' }).click();
  await expect(page.getByRole('dialog', { name: 'Premaknem kontakt v koš?' })).toContainText('30 dni');
});

test('R-013-02 other the delete-forever confirmation says it cannot be undone', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  const c = await contact(u, g, { lastName: 'Zupan' });
  expect((await call(u, 'POST', `/groups/${g.id}/contacts/${c}/trash`)).status).toBe(204);
  await signIn(page, u);
  await page.goto(`/groups/${g.id}/trash`);
  await page.getByRole('row').filter({ hasText: 'Zupan' }).getByRole('button', { name: 'Izbriši za vedno' }).click();
  await expect(page.getByRole('dialog', { name: 'Trajno izbrišem kontakt?' })).toContainText('Tega ni mogoče razveljaviti.');
});

test.describe('German browser', () => {
  test.use({ locale: 'de-DE' });
  test('R-019-01 other a browser in German opens the product in Slovene', async ({ page }) => {
    await page.goto('/sign-in');
    await expect(page.locator('html')).toHaveAttribute('lang', 'sl');
    await expect(page.getByRole('heading', { level: 1, name: 'Prijava' })).toBeVisible();
  });
});

test('R-019-02 combination switching to English and back keeps the typed values', async ({ page }) => {
  await page.goto('/register');
  await page.getByLabel('Prikazno ime').fill('Mojca Zupan');
  await lang(page, 'EN');
  await lang(page, 'SL');
  await expect(page.getByLabel('Prikazno ime')).toHaveValue('Mojca Zupan');
});

test('R-019-03 other Slovene chosen again after English is remembered', async ({ page, browser }) => {
  await page.goto('/sign-in');
  await lang(page, 'EN');
  await lang(page, 'SL');
  const state = await page.context().storageState();
  const ctx = await browser.newContext({ storageState: state, locale: 'en-US' });
  const p = await ctx.newPage();
  await p.goto('/sign-in');
  await expect(p.locator('html')).toHaveAttribute('lang', 'sl');
  await ctx.close();
});

test('R-019-04 other 2026-02-03 08:05 UTC reads 3. 2. 2026 09:05 and 2026-02-03 09:05', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  const c = await contact(u, g, { lastName: 'Zupan' });
  await page.route('**/api/v1/groups/*/contacts/*/history**', (r) =>
    r.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ items: [{ id: '00000000-0000-4000-8000-000000000099', occurredAt: '2026-02-03T08:05:00Z', userName: 'Ana Novak', userEmail: u.email, action: 'CONTACT_CREATED', contactId: c, details: {} }], page: 1, pageSize: 25, total: 1 }),
    }),
  );
  await signIn(page, u);
  await page.goto(`/groups/${g.id}/contacts/${c}`);
  await page.getByRole('navigation', { name: 'Zavihki kontakta' }).getByText('Zgodovina').click();
  await expect(page.getByText('3. 2. 2026 09:05')).toBeVisible();
  await lang(page, 'EN');
  await expect(page.getByText('2026-02-03 09:05')).toBeVisible();
});

test('R-019-05 other the country AT reads Avstrija, then Austria', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  const c = await contact(u, g, { lastName: 'Zupan', country: 'AT' });
  await signIn(page, u);
  await page.goto(`/groups/${g.id}/contacts/${c}`);
  await expect(page.getByText('Avstrija', { exact: true })).toBeVisible();
  await lang(page, 'EN');
  await expect(page.getByText('Austria', { exact: true })).toBeVisible();
});

test('R-019-06 other the Attribute value Šifra and the company Čuk d.o.o. are not translated', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  const c = await contact(u, g, { lastName: 'Zupan', company: 'Čuk d.o.o.', attributes: [{ key: 'koda', value: 'Šifra' }] });
  await signIn(page, u);
  await page.goto(`/groups/${g.id}/contacts/${c}`);
  await lang(page, 'EN');
  await expect(page.getByText('Čuk d.o.o.').first()).toBeVisible();
  await expect(page.getByText('Šifra').first()).toBeVisible();
});

test('R-019-07 other "You already have a group with this name" is shown in English after the switch', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  await group(u, 'Acme');
  await signIn(page, u);
  await lang(page, 'EN');
  await page.getByRole('button', { name: /create group/i }).first().click();
  await page.getByRole('dialog').getByRole('textbox').fill('acme');
  await page.getByRole('dialog').getByRole('button').last().click();
  await expect(page.getByRole('dialog').getByText(/already have a group/i)).toBeVisible();
});

test('R-021-06 other the account deletion asks for confirmation that it cannot be undone', async ({ page }) => {
  const u = await person('Cene Zupan', 'cene');
  await signIn(page, u);
  await page.goto('/account');
  await page.getByRole('button', { name: 'Izbriši račun' }).click();
  await expect(page.getByRole('dialog', { name: 'Izbrišem račun?' })).toContainText('Tega ni mogoče razveljaviti.');
  expect(await accounts(u.email)).toBe(1);
});

test('R-026-01 other the Merge screen shows the Survivor and Contact 2 side by side', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  await contact(u, g, { firstName: 'Prva', email: 'x@hidden.test' });
  await contact(u, g, { firstName: 'Druga', email: 'x@hidden.test' });
  await signIn(page, u);
  await page.goto(`/groups/${g.id}/duplicates`);
  await page.getByRole('button', { name: /Združi izbrane \(2\)/ }).click();
  await expect(page.getByRole('columnheader', { name: /Ohranjeni kontakt/ })).toContainText('Prva');
  await expect(page.getByRole('columnheader', { name: /Kontakt 2/ })).toContainText('Druga');
});

test('R-026-02 other an empty value of the Survivor preselects the value of the next oldest', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  await contact(u, g, { firstName: 'Prva', email: 'x@hidden.test' });
  await contact(u, g, { firstName: 'Druga', email: 'x@hidden.test', city: 'Bovec' });
  await signIn(page, u);
  await page.goto(`/groups/${g.id}/duplicates`);
  await page.getByRole('button', { name: /Združi izbrane \(2\)/ }).click();
  await expect(page.getByRole('radio', { name: 'Bovec' })).toBeChecked();
  await expect(page.getByRole('radio', { name: 'Prva', exact: true })).toBeChecked();
});

test('R-026-10 other with one Contact picked the merge is not offered', async ({ page }) => {
  const u = await person('Ana Novak', 'ana');
  const g = await group(u, 'Acme');
  await contact(u, g, { firstName: 'Prva', email: 'x@hidden.test' });
  await contact(u, g, { firstName: 'Druga', email: 'x@hidden.test' });
  await signIn(page, u);
  await page.goto(`/groups/${g.id}/duplicates`);
  await page.getByRole('checkbox', { name: 'Izberi kontakt' }).first().uncheck();
  await expect(page.getByText('Za združitev izberite vsaj dva kontakta.')).toBeVisible();
  expect((await call(u, 'GET', `/groups/${g.id}/contacts`)).body.total).toBe(2);
});
