import { expect, test } from '@playwright/test';
import { PASSWORD, unique, verifyByLink } from '../acceptance/support/stack';

/**
 * E2E-01 First use (e2e-scenarios.md): US-001, US-002, US-003, US-004, US-005, US-007, US-008, US-019.
 * Covers AC-001-01, AC-001-02, AC-002-01, AC-002-07, AC-003-01, AC-004-01, AC-005-01, AC-007-04, AC-008-04,
 * AC-019-02, DoD-P01.
 */
test('E2E-01 first use: register, verify, create Acme, add Ana Novak, find her by spletno', async ({
  page,
}) => {
  const email = unique('ana');

  // 1. the UI is Slovene; switch to English and back
  await page.goto('/');
  await expect(page.locator('html')).toHaveAttribute('lang', 'sl');
  await page.getByRole('button', { name: 'EN', exact: true }).first().click();
  await expect(page.locator('html')).toHaveAttribute('lang', 'en');
  await page.getByRole('button', { name: 'SL', exact: true }).first().click();
  await expect(page.locator('html')).toHaveAttribute('lang', 'sl');

  // 2. register; the page asks to verify the e-mail
  await page.goto('/register');
  await page.getByLabel('Prikazno ime').fill('Ana Novak');
  await page.getByLabel('E-pošta').fill(email);
  await page.getByLabel('Geslo').fill(PASSWORD);
  await page.getByRole('button', { name: 'Ustvari račun' }).click();
  await expect(
    page.getByRole('heading', { level: 1, name: 'Potrdite svoj e-poštni naslov, da nadaljujete.' }),
  ).toBeVisible();

  // 3. verify in the emulator and sign in: the empty Group list offers "Create group"
  await verifyByLink(email);
  await page.reload();
  if (await page.getByRole('heading', { level: 1, name: 'Prijava' }).isVisible()) {
    await page.getByLabel('E-pošta').fill(email);
    await page.getByLabel('Geslo').fill(PASSWORD);
    await page.getByRole('button', { name: 'Prijava', exact: true }).click();
  }
  await expect(page.getByRole('heading', { level: 1, name: 'Moje skupine' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Še nimate nobene skupine' })).toBeVisible();

  // 4. create Acme: it opens as an empty contact list
  await page.getByRole('button', { name: 'Ustvari skupino' }).first().click();
  const dialog = page.getByRole('dialog', { name: 'Nova skupina' });
  await dialog.getByLabel('Ime skupine').fill('Acme');
  await dialog.getByRole('button', { name: 'Ustvari', exact: true }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Kontakti' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'V skupini še ni kontaktov' })).toBeVisible();

  // 5. add Ana Novak, Acme d.o.o., Tag udelezenec2025, Attribute kompetenca=spletnoProgramiranje
  await page
    .getByRole('button', { name: 'Dodaj kontakt' })
    .or(page.getByRole('link', { name: 'Dodaj kontakt' }))
    .first()
    .click();
  await expect(page.getByRole('heading', { level: 1, name: 'Nov kontakt' })).toBeVisible();
  await page.getByLabel('Ime', { exact: true }).fill('Ana');
  await page.getByLabel('Priimek', { exact: true }).fill('Novak');
  await page.getByLabel('Podjetje', { exact: true }).fill('Acme d.o.o.');
  await page.getByLabel('Dodaj značko').fill('udelezenec2025');
  await page.getByLabel('Dodaj značko').press('Enter');
  await page.getByRole('button', { name: 'Dodaj atribut' }).click();
  await page.getByLabel('Ključ', { exact: true }).last().fill('kompetenca');
  await page.getByLabel('Vrednost', { exact: true }).last().fill('spletnoProgramiranje');
  await page.getByRole('button', { name: 'Shrani kontakt' }).click();
  await expect(page.getByText('Različica 1')).toBeVisible();
  await expect(page.getByText('udelezenec2025').first()).toBeVisible();

  // 6. searching spletno finds Ana Novak with the Tag
  await page
    .getByRole('link', { name: /Kontakti/ })
    .first()
    .click();
  await page.getByLabel('Iskanje').fill('spletno');
  const row = page
    .getByRole('table', { name: 'Kontakti skupine' })
    .getByRole('row')
    .filter({ hasText: 'Novak' });
  await expect(row).toHaveCount(1);
  await expect(row).toContainText('Ana');
  await expect(row).toContainText('udelezenec2025');
  await expect(page.getByText('1 kontakt').first()).toBeVisible();
});
