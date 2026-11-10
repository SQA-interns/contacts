import { expect, test, type Locator, type Page } from '@playwright/test';
import { createContact, createGroup, newUser, paths, signIn } from '../acceptance/support/stack';
import { VIEWPORTS } from './viewports';

/**
 * Keyboard-only walkthrough of the four main flows (NFR-09, WCAG 2.1.1; F-50): sign in, open a Contact from the
 * list, create and edit a Contact, export. Inside a flow nothing is clicked or filled by the test: it presses Tab
 * until the control it needs has focus, then types, presses Enter or Space. Needs the test stack.
 */
async function tabTo(page: Page, target: Locator, limit = 150) {
  const handle = await target.elementHandle({ timeout: 10_000 });
  if (!handle) throw new Error('control not found');
  // Safari and WebKit skip links, buttons and check boxes on Tab unless Option is held (their keyboard default)
  const key = page.context().browser()?.browserType().name() === 'webkit' ? 'Alt+Tab' : 'Tab';
  for (let presses = 0; presses <= limit; presses++) {
    if (await handle.evaluate((el) => el === document.activeElement)) return;
    await page.keyboard.press(key);
  }
  throw new Error(`the control cannot be reached with Tab: ${target}`);
}

/** Tabs to a field, types into it (the whole content replaced). */
async function typeInto(page: Page, field: Locator, text: string) {
  await tabTo(page, field);
  await page.keyboard.press('ControlOrMeta+a');
  await page.keyboard.type(text);
}

for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('flow 1 sign in', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      await page.goto('/sign-in');
      await typeInto(page, page.getByLabel('E-pošta'), ana.email);
      await typeInto(page, page.getByLabel('Geslo'), 'Geslo1234');
      await tabTo(page, page.getByRole('button', { name: 'Prijava', exact: true }));
      await page.keyboard.press('Enter');
      await expect(page.getByRole('heading', { level: 1, name: 'Moje skupine' })).toBeVisible();
    });

    test('flow 2 list: open the Group, then a Contact', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');
      await createContact(ana, acme, { firstName: 'Ana', lastName: 'Novak', company: 'Acme d.o.o.' });
      await signIn(page, ana);

      await tabTo(page, page.getByRole('link', { name: /Acme/ }).first());
      await page.keyboard.press('Enter');
      await expect(page.getByRole('heading', { level: 1, name: 'Kontakti' })).toBeVisible();

      await typeInto(page, page.getByLabel('Iskanje'), 'novak');
      await expect(page.getByRole('link', { name: /Novak/ }).filter({ visible: true })).toHaveCount(1);
      await tabTo(page, page.getByRole('link', { name: 'Novak' }).filter({ visible: true }).first());
      await page.keyboard.press('Enter');
      await expect(page.getByText('Različica 1')).toBeVisible();
    });

    test('flow 3 create and edit a Contact', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');
      await signIn(page, ana);
      await page.goto(paths.newContact(acme));
      await expect(page.getByRole('heading', { level: 1, name: 'Nov kontakt' })).toBeVisible();

      await typeInto(page, page.getByLabel('Ime', { exact: true }), 'Ana');
      await typeInto(page, page.getByLabel('Priimek', { exact: true }), 'Novak');
      await typeInto(page, page.getByLabel('Podjetje', { exact: true }), 'Acme d.o.o.');
      await typeInto(page, page.getByLabel('Dodaj značko'), 'gost');
      await page.keyboard.press('Enter');
      await tabTo(page, page.getByRole('button', { name: 'Dodaj atribut' }));
      await page.keyboard.press('Space');
      await typeInto(page, page.getByLabel('Ključ', { exact: true }).last(), 'kompetenca');
      await typeInto(page, page.getByLabel('Vrednost', { exact: true }).last(), 'java');
      await tabTo(page, page.getByRole('button', { name: 'Shrani kontakt' }));
      await page.keyboard.press('Enter');
      await expect(page.getByText('Različica 1')).toBeVisible();

      await tabTo(page, page.getByRole('button', { name: 'Uredi' }));
      await page.keyboard.press('Enter');
      await typeInto(page, page.getByLabel('Podjetje', { exact: true }), 'Beta d.o.o.');
      await tabTo(page, page.getByRole('button', { name: 'Shrani', exact: true }));
      await page.keyboard.press('Enter');
      await expect(page.getByText('Beta d.o.o.').first()).toBeVisible();
      await expect(page.getByText('Različica 2')).toBeVisible();
    });

    test('flow 4 export the selected Contacts', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');
      await createContact(ana, acme, { firstName: 'Ana', lastName: 'Novak' });
      await createContact(ana, acme, { firstName: 'Boris', lastName: 'Kos' });
      await signIn(page, ana);
      await page.goto(paths.contacts(acme));
      await expect(page.getByRole('heading', { level: 1, name: 'Kontakti' })).toBeVisible();

      await tabTo(
        page,
        page.getByRole('checkbox', { name: 'Izberi vse na tej strani' }).filter({ visible: true }),
      );
      await page.keyboard.press('Space');
      await tabTo(page, page.getByRole('button', { name: 'Izvozi v Excel' }));
      const download = page.waitForEvent('download');
      await page.keyboard.press('Enter');
      expect((await download).suggestedFilename()).toMatch(/^Acme_.*\.xlsx$/);
    });
  });
}
