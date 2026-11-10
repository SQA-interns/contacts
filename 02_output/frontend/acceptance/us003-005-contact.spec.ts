import { expect, test, type Page } from '@playwright/test';
import {
  addMember,
  api,
  createContact,
  createGroup,
  editContact,
  getContact,
  newUser,
  openAs,
  paths,
  type Group,
  type User,
} from './support/stack';

/** US-003, US-004, US-005 on the screens WF-07 (Contact) and WF-08 (New Contact). */

async function owned(): Promise<{ ana: User; acme: Group }> {
  const ana = await newUser('Ana Novak', 'ana');
  return { ana, acme: await createGroup(ana, 'Acme') };
}

async function total(user: User, group: Group): Promise<number> {
  return (await api(user, 'GET', `/groups/${group.id}/contacts`)).body.total;
}

function field(page: Page, label: string) {
  return page.getByLabel(label, { exact: true });
}

test('AC-003-02 with only a phone the form says one of the four is needed and keeps the phone', async ({
  page,
}) => {
  const { ana, acme } = await owned();
  await openAs(page, ana, paths.newContact(acme));

  await field(page, 'Telefon').fill('040 123 456');
  await page.getByRole('button', { name: 'Shrani kontakt' }).click();

  await expect(page.getByText('Izpolnite vsaj ime, priimek, e-pošto ali podjetje.').first()).toBeVisible();
  await expect(field(page, 'Telefon')).toHaveValue('040 123 456');
  expect(await total(ana, acme)).toBe(0);
});

test('AC-003-03 the e-mail ana@ shows a message next to the e-mail field and stores nothing', async ({
  page,
}) => {
  const { ana, acme } = await owned();
  await openAs(page, ana, paths.newContact(acme));

  await field(page, 'Ime').fill('Ana');
  await field(page, 'E-pošta').fill('ana@');
  await page.getByRole('button', { name: 'Shrani kontakt' }).click();

  await expect(page.getByText('E-poštni naslov ni veljaven.')).toBeVisible();
  await expect(field(page, 'Ime')).toHaveValue('Ana');
  expect(await total(ana, acme)).toBe(0);
});

test('AC-003-06 a stale save shows "This contact was changed by someone else" and the current Contact', async ({
  page,
}) => {
  const { ana, acme } = await owned();
  const boris = await newUser('Boris Kos', 'boris');
  await addMember(ana, acme, boris, 'EDITOR');
  const c = await createContact(ana, acme, { firstName: 'Ana', lastName: 'Novak', company: 'Acme d.o.o.' });
  await openAs(page, ana, paths.contact(acme, c.id));
  await page.getByRole('button', { name: 'Uredi' }).click();
  await field(page, 'Podjetje').fill('Beta d.o.o.');

  await editContact(boris, acme, c.id, { city: 'Maribor' });
  await page.getByRole('button', { name: 'Shrani', exact: true }).click();

  await expect(page.getByRole('alert').filter({ hasText: 'Kontakt je spremenil nekdo drug.' })).toBeVisible();
  await expect(page.getByText('Maribor')).toBeVisible();
  const stored = await getContact(ana, acme, c.id);
  expect(stored.company).toBe('Acme d.o.o.');
  expect(stored.version).toBe(2);
});

test('AC-003-07 a Viewer is offered no "Add contact" action', async ({ page }) => {
  const { ana, acme } = await owned();
  const viola = await newUser('Viola Vidmar', 'viola');
  await addMember(ana, acme, viola, 'VIEWER');
  await createContact(ana, acme, { firstName: 'Ana', lastName: 'Novak' });

  await openAs(page, viola, paths.contacts(acme));

  await expect(page.getByRole('heading', { level: 1, name: 'Kontakti' })).toBeVisible();
  await expect(page.getByRole('table').getByText('Novak')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Dodaj kontakt' })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Dodaj kontakt' })).toHaveCount(0);
});

test('AC-003-08 the Contact shows all 11 predefined fields, the Tag and the Attribute', async ({ page }) => {
  const { ana, acme } = await owned();
  const c = await createContact(ana, acme, {
    firstName: 'Ana',
    lastName: 'Novak',
    email: 'ana@acme.com',
    phone: '040 123 456',
    company: 'Acme d.o.o.',
    jobTitle: 'Vodja',
    street: 'Slovenska cesta 1',
    postalCode: '1000',
    city: 'Ljubljana',
    country: 'SI',
    notes: 'Udeleženka',
    tags: ['udelezenec2025'],
    attributes: [{ key: 'kompetenca', value: 'spletnoProgramiranje' }],
  });

  await openAs(page, ana, paths.contact(acme, c.id));

  for (const label of [
    'Ime',
    'Priimek',
    'E-pošta',
    'Telefon',
    'Podjetje',
    'Delovno mesto',
    'Ulica in hišna številka',
    'Poštna številka',
    'Kraj',
    'Država',
    'Opombe',
  ]) {
    await expect(page.getByText(label, { exact: true }).first()).toBeVisible();
  }
  for (const value of [
    'Novak',
    'ana@acme.com',
    '040 123 456',
    'Acme d.o.o.',
    'Vodja',
    'Slovenska cesta 1',
    '1000',
    'Ljubljana',
    'Slovenija',
    'Udeleženka',
    'udelezenec2025',
    'kompetenca',
    'spletnoProgramiranje',
  ]) {
    await expect(page.getByText(value).first()).toBeVisible();
  }
});

test('EX-003-13 the last name <script>alert(1)</script> is shown as plain text and not run', async ({
  page,
}) => {
  const { ana, acme } = await owned();
  const c = await createContact(ana, acme, { lastName: '<script>alert(1)</script>' });
  let dialogs = 0;
  page.on('dialog', async (d) => {
    dialogs++;
    await d.dismiss();
  });

  await openAs(page, ana, paths.contact(acme, c.id));

  await expect(page.getByText('<script>alert(1)</script>').first()).toBeVisible();
  expect(dialogs).toBe(0);
});

test('AC-004-04 typing komp suggests kompetenca of the Group; the new key kompetentnost can be saved', async ({
  page,
}) => {
  const { ana, acme } = await owned();
  await createContact(ana, acme, { firstName: 'Boris', attributes: [{ key: 'kompetenca', value: 'java' }] });
  await openAs(page, ana, paths.newContact(acme));
  await field(page, 'Ime').fill('Ana');

  await page.getByRole('button', { name: 'Dodaj atribut' }).click();
  await page.getByLabel('Ključ', { exact: true }).last().fill('komp');

  await expect(page.getByRole('option', { name: 'kompetenca' })).toBeVisible();
  await page.getByLabel('Ključ', { exact: true }).last().fill('kompetentnost');
  await page.getByLabel('Vrednost', { exact: true }).last().fill('visoka');
  await page.getByRole('button', { name: 'Shrani kontakt' }).click();
  await expect(page.getByText('kompetentnost').first()).toBeVisible();
  const list = (await api(ana, 'GET', `/groups/${acme.id}/contacts?q=Ana`)).body.items;
  const saved = await getContact(ana, acme, list[0].id);
  expect(saved.attributes).toEqual([{ key: 'kompetentnost', value: 'visoka' }]);
});

test('AC-004-07 a Viewer sees the Attributes but no control to change them', async ({ page }) => {
  const { ana, acme } = await owned();
  const viola = await newUser('Viola Vidmar', 'viola');
  await addMember(ana, acme, viola, 'VIEWER');
  const c = await createContact(ana, acme, {
    firstName: 'Ana',
    attributes: [{ key: 'kompetenca', value: 'java' }],
  });

  await openAs(page, viola, paths.contact(acme, c.id));

  await expect(page.getByText('kompetenca').first()).toBeVisible();
  await expect(page.getByText('java').first()).toBeVisible();
  await expect(page.getByRole('button', { name: 'Uredi' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Dodaj atribut' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Izbriši' })).toHaveCount(0);
});

test('AC-004-08 the key projekt of another Group is not suggested in Acme', async ({ page }) => {
  const { ana, acme } = await owned();
  await createContact(ana, acme, { firstName: 'Boris', attributes: [{ key: 'kompetenca', value: 'java' }] });
  const beta = await createGroup(ana, 'Beta');
  await createContact(ana, beta, { firstName: 'Cene', attributes: [{ key: 'projekt', value: 'x' }] });
  await openAs(page, ana, paths.newContact(acme));

  await page.getByRole('button', { name: 'Dodaj atribut' }).click();
  await page.getByLabel('Ključ', { exact: true }).last().fill('k');
  await expect(page.getByRole('option', { name: 'kompetenca' })).toBeVisible();
  await page.getByLabel('Ključ', { exact: true }).last().fill('pro');

  await expect(page.getByRole('option', { name: 'projekt' })).toHaveCount(0);
});

test('AC-005-03 the Tag "udelezenec 2025" shows the rule and adds no Tag', async ({ page }) => {
  const { ana, acme } = await owned();
  const c = await createContact(ana, acme, { firstName: 'Ana', lastName: 'Novak' });
  await openAs(page, ana, paths.contact(acme, c.id));
  await page.getByRole('button', { name: 'Uredi' }).click();

  await page.getByLabel('Dodaj značko').fill('udelezenec 2025');
  await page.getByLabel('Dodaj značko').press('Enter');

  await expect(page.getByText('Ime značke lahko vsebuje črke, števke, - in _ (do 50 znakov).')).toBeVisible();
  expect((await getContact(ana, acme, c.id)).tags).toEqual([]);
});

test('AC-005-06 the Tag gost of Beta is not suggested in Acme', async ({ page }) => {
  const { ana, acme } = await owned();
  await createContact(ana, acme, { firstName: 'Boris', tags: ['gospodar'] });
  const beta = await createGroup(ana, 'Beta');
  await createContact(ana, beta, { firstName: 'Cene', tags: ['gost'] });
  await openAs(page, ana, paths.newContact(acme));

  await page.getByLabel('Dodaj značko').fill('go');

  await expect(page.getByRole('option', { name: 'gospodar' })).toBeVisible();
  await expect(page.getByRole('option', { name: 'gost', exact: true })).toHaveCount(0);
});

test('AC-005-07 the Tags neObvescaj and udelezenec2025 are shown in the list and on the Contact', async ({
  page,
}) => {
  const { ana, acme } = await owned();
  const viola = await newUser('Viola Vidmar', 'viola');
  await addMember(ana, acme, viola, 'VIEWER');
  const c = await createContact(ana, acme, {
    firstName: 'Ana',
    lastName: 'Novak',
    tags: ['neObvescaj', 'udelezenec2025'],
  });

  await openAs(page, viola, paths.contacts(acme));
  const row = page.getByRole('row').filter({ hasText: 'Novak' });
  await expect(row.getByText('neObvescaj')).toBeVisible();
  await expect(row.getByText('udelezenec2025')).toBeVisible();

  await page.goto(paths.contact(acme, c.id));
  await expect(page.getByText('neObvescaj').first()).toBeVisible();
  await expect(page.getByText('udelezenec2025').first()).toBeVisible();
});
