import { expect, test, type Page } from '@playwright/test';
import {
  accountsWith,
  createAccount,
  newUser,
  sentEmails,
  setVerified,
  signIn,
  unique,
} from './support/stack';

/** US-001 Register and sign in: screens WF-01 to WF-04 (Slovene UI, the default language). */

const verifyHeading = 'Potrdite svoj e-poštni naslov, da nadaljujete.';

async function register(page: Page, displayName: string, email: string, password: string) {
  await page.goto('/register');
  await page.getByLabel('Prikazno ime').fill(displayName);
  await page.getByLabel('E-pošta').fill(email);
  await page.getByLabel('Geslo').fill(password);
  await page.getByRole('button', { name: 'Ustvari račun' }).click();
}

test('AC-001-01 registering Ana Novak sends the verification e-mail and asks to verify; no Group opens', async ({
  page,
}) => {
  const email = unique('ana');

  await register(page, 'Ana Novak', email, 'Geslo1234');

  await expect(page.getByRole('heading', { level: 1, name: verifyHeading })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Moje skupine' })).toHaveCount(0);
  expect(await accountsWith(email)).toBe(1);
  await expect
    .poll(async () => (await sentEmails()).some((m) => m.email === email && m.requestType === 'VERIFY_EMAIL'))
    .toBe(true);
});

test('AC-001-02 a verified sign-in opens the Group list', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');

  await signIn(page, ana);

  await expect(page.getByRole('heading', { level: 1, name: 'Moje skupine' })).toBeVisible();
});

test('AC-001-04 an unverified User sees "Verify your e-mail to continue" and the resend button, no Group', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana', false);

  await signIn(page, ana, false);

  await expect(page.getByRole('heading', { level: 1, name: verifyHeading })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Pošlji znova' })).toBeVisible();
  await page.goto('/groups');
  await expect(page.getByRole('heading', { level: 1, name: verifyHeading })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Moje skupine' })).toHaveCount(0);
});

test('AC-001-05 a wrong password shows "E-mail or password is wrong" and nobody is signed in', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana');

  await page.goto('/sign-in');
  await page.getByLabel('E-pošta').fill(ana.email);
  await page.getByLabel('Geslo').fill('NapacnoGeslo1');
  await page.getByRole('button', { name: 'Prijava', exact: true }).click();

  await expect(page.getByRole('alert')).toHaveText('E-pošta ali geslo ni pravilno.');
  await page.goto('/groups');
  await expect(page.getByRole('heading', { level: 1, name: 'Prijava' })).toBeVisible();
});

test('AC-001-06 the password Abc1234 (7 characters) creates no account; the rule is shown at the field', async ({
  page,
}) => {
  const email = unique('new');

  await register(page, 'Nova Oseba', email, 'Abc1234');

  await expect(page.getByText('Geslo mora imeti vsaj 8 znakov.')).toBeVisible();
  expect(await accountsWith(email)).toBe(0);
});

test('AC-001-07 registering an address that exists offers Sign in and Reset password; no second account', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana');

  await register(page, 'Ana Druga', ana.email, 'Geslo1234');

  await expect(page.getByRole('alert')).toContainText('Ta e-poštni naslov je že registriran.');
  await expect(page.getByRole('button', { name: 'Prijava' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Ponastavi geslo' })).toBeVisible();
  expect(await accountsWith(ana.email)).toBe(1);
});

async function askReset(page: Page, email: string): Promise<string> {
  await page.goto('/reset-password');
  await page.getByLabel('E-pošta').fill(email);
  await page.getByRole('button', { name: 'Pošlji povezavo' }).click();
  const status = page.getByRole('status');
  await expect(status).toContainText('Preverite svojo e-pošto.');
  return (await status.innerText()).trim();
}

test('AC-001-08 the reset confirmation is the same for nobody@acme.com and a registered address', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  const nobody = unique('nobody');

  const forNobody = await askReset(page, nobody);
  const forAna = await askReset(page, ana.email);

  expect(forNobody).toBe(forAna);
  await expect
    .poll(async () =>
      (await sentEmails()).some((m) => m.email === ana.email && m.requestType === 'PASSWORD_RESET'),
    )
    .toBe(true);
  expect((await sentEmails()).some((m) => m.email === nobody)).toBe(false);
});

test('AC-001-09 after a reload Ana is still signed in', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  await signIn(page, ana);

  await page.reload();

  await expect(page.getByRole('heading', { level: 1, name: 'Moje skupine' })).toBeVisible();
});

test('AC-001-10 after signing out the sign-in page is shown and the Group list needs a sign-in', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  await signIn(page, ana);

  await page.getByRole('button', { name: 'Uporabniški meni' }).click();
  await page.getByRole('menuitem', { name: 'Odjava' }).click();

  await expect(page.getByRole('heading', { level: 1, name: 'Prijava' })).toBeVisible();
  await page.goto('/groups');
  await expect(page.getByRole('heading', { level: 1, name: 'Prijava' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Moje skupine' })).toHaveCount(0);
});

test('AC-001-11 an expired token (401 from the server) returns Ana to the sign-in page', async ({ page }) => {
  const ana = await newUser('Ana Novak', 'ana');
  await signIn(page, ana);
  await page.route('**/api/v1/**', (route) =>
    route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ code: 'auth.token_invalid', message: 'Invalid token' }),
    }),
  );

  await page.reload();

  await expect(page.getByRole('heading', { level: 1, name: 'Prijava' })).toBeVisible();
  await expect(page.getByText('Seja je potekla. Prijavite se znova.')).toBeVisible();
});

test('EX-001-01 a password of exactly 8 characters creates the account', async ({ page }) => {
  const email = unique('osem');

  await register(page, 'Osem Znakov', email, 'Geslo123');

  await expect(page.getByRole('heading', { level: 1, name: verifyHeading })).toBeVisible();
  expect(await accountsWith(email)).toBe(1);
});

test('EX-001-02 a password of 7 characters is refused', async ({ page }) => {
  const email = unique('sedem');

  await register(page, 'Sedem Znakov', email, 'Geslo12');

  await expect(page.getByText('Geslo mora imeti vsaj 8 znakov.')).toBeVisible();
  expect(await accountsWith(email)).toBe(0);
});

test('EX-001-03 a display name of 100 characters creates the account', async ({ page }) => {
  const email = unique('dolgo');

  await register(page, 'A'.repeat(100), email, 'Geslo1234');

  await expect(page.getByRole('heading', { level: 1, name: verifyHeading })).toBeVisible();
  expect(await accountsWith(email)).toBe(1);
});

for (const [name, displayName] of [
  ['101 characters', 'A'.repeat(101)],
  ['empty', ''],
] as const) {
  test(`EX-001-04 a display name that is ${name} creates no account; the message is at the field`, async ({
    page,
  }) => {
    const email = unique('ime');

    await register(page, displayName, email, 'Geslo1234');

    await expect(page.getByText('Ime mora imeti od 1 do 100 znakov.')).toBeVisible();
    expect(await accountsWith(email)).toBe(0);
  });
}

test('EX-001-06 the e-mail ana@ creates no account; the message is at the field', async ({ page }) => {
  await register(page, 'Ana Novak', 'ana@', 'Geslo1234');

  await expect(page.getByText('E-poštni naslov ni veljaven.')).toBeVisible();
  await expect(page.getByRole('heading', { level: 1, name: 'Registracija' })).toBeVisible();
  expect(await accountsWith('ana@')).toBe(0);
});

test('EX-001-08 a reset for an unregistered address shows the confirmation and reveals nothing', async ({
  page,
}) => {
  const nobody = unique('nikogar');

  await askReset(page, nobody);

  await expect(
    page.getByText('Če je naslov registriran, smo nanj poslali povezavo za ponastavitev gesla.'),
  ).toBeVisible();
  await expect(page.getByRole('alert')).toHaveCount(0);
});

test('EX-001-09 after verifying the e-mail and reloading, the Group list opens', async ({ page }) => {
  const email = unique('ana');
  const uid = await createAccount(email, 'Ana Novak', false);
  await signIn(page, { email, displayName: 'Ana Novak', uid, token: '' }, false);
  await expect(page.getByRole('heading', { level: 1, name: verifyHeading })).toBeVisible();

  await setVerified(uid, true);
  await page.reload();

  await expect(page.getByRole('heading', { level: 1, name: 'Moje skupine' })).toBeVisible();
});
