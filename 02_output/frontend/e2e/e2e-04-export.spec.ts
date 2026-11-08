import { expect, test } from '@playwright/test';
import ExcelJS from 'exceljs';
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
 * E2E-04 Export (e2e-scenarios.md): US-005, US-006, US-009, US-010, US-011, US-017.
 * Covers AC-006-01, AC-009-03, AC-010-03, AC-010-04, AC-011-02, AC-011-08, AC-011-07, AC-017-03, DoD-P01.
 */
const HEADER_SL = [
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
  'Značke',
];

function todayInLjubljana(): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Europe/Ljubljana',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date());
}

test('E2E-04 export: rename the mistyped Tag, filter, select all N results, export, see it in the activity list', async ({
  page,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  const acme = await createGroup(ana, 'Acme');
  const editor = await newUser('Eva Horvat', 'eva');
  await addMember(ana, acme, editor, 'EDITOR');
  for (let i = 1; i <= 28; i++) {
    await createContact(ana, acme, {
      firstName: 'Udelezenka',
      lastName: `Priimek${String(i).padStart(2, '0')}`,
      tags: ['udelezenec205'],
      attributes: i === 1 ? [{ key: 'kompetenca', value: 'spletnoProgramiranje' }] : [],
    });
  }
  await createContact(ana, acme, {
    firstName: 'Brez',
    lastName: 'Obvestil',
    tags: ['udelezenec205', 'neObvescaj'],
  });
  await createContact(ana, acme, { firstName: 'Drugi', lastName: 'Gost', tags: ['gost'] });

  // 1. the Editor renames the mistyped Tag udelezenec205 to udelezenec2025 in the Tag list
  await signIn(page, editor);
  await page.goto(paths.tags(acme));
  await page
    .getByRole('row')
    .filter({ has: page.getByText('udelezenec205', { exact: true }) })
    .getByRole('button', { name: 'Preimenuj' })
    .click();
  const rename = page.getByRole('dialog', { name: 'Preimenuj značko' });
  await rename.getByLabel('Novo ime').fill('udelezenec2025');
  await rename.getByRole('button', { name: 'Preimenuj' }).click();
  await expect(page.getByText('udelezenec205', { exact: true })).toHaveCount(0);
  await expect(
    page.getByRole('row').filter({ has: page.getByText('udelezenec2025', { exact: true }) }),
  ).toContainText('29');

  // 2. filter Tag udelezenec2025 ("has all") and Without-Tags neObvescaj
  await page.goto(paths.contacts(acme));
  const filters = page.getByRole('complementary', { name: 'Filtri' });
  await filters.getByText('Izberite značke').first().click();
  await page.getByRole('option', { name: 'udelezenec2025', exact: true }).click();
  await page.keyboard.press('Escape');
  await filters.getByLabel('ima vse te značke').check();
  await filters.getByText('Izberite značke').last().click();
  await page.getByRole('option', { name: 'neObvescaj', exact: true }).click();
  await page.keyboard.press('Escape');
  await expect(page.getByText('28 kontaktov').first()).toBeVisible();

  // 3. "select all on this page", then "select all N results"
  await page.getByRole('checkbox', { name: 'Izberi vse na tej strani' }).check();
  const bar = page.getByRole('region', { name: 'Izbira' });
  await expect(bar).toContainText('25 izbranih');
  await bar.getByRole('button', { name: 'Izberi vseh 28 zadetkov' }).click();
  await expect(bar).toContainText('28 izbranih');

  // 4. export: Acme_<date>.xlsx with the matching rows, the 11 columns, Tags and the Attribute column
  const download = page.waitForEvent('download');
  await bar.getByRole('button', { name: 'Izvozi v Excel' }).click();
  const file = await download;
  expect(file.suggestedFilename()).toBe(`Acme_${todayInLjubljana()}.xlsx`);
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.readFile(await file.path());
  expect(workbook.worksheets).toHaveLength(1);
  const sheet = workbook.worksheets[0];
  const header = (sheet.getRow(1).values as unknown[]).slice(1).map(String);
  expect(header).toEqual([...HEADER_SL, 'kompetenca']);
  const lastNames: string[] = [];
  const tagsCells: string[] = [];
  sheet.eachRow((row, n) => {
    if (n === 1) return;
    lastNames.push(String(row.getCell(2).value));
    tagsCells.push(String(row.getCell(12).value ?? ''));
  });
  expect(lastNames).toHaveLength(28);
  expect(lastNames).not.toContain('Obvestil');
  expect(lastNames).not.toContain('Gost');
  expect(tagsCells.every((t) => !t.includes('neObvescaj'))).toBe(true);
  expect(sheet.getColumn(13).values.filter((v) => v === 'spletnoProgramiranje')).toHaveLength(1);

  // 5. the Group activity list shows the export with the number of Contacts
  await page.goto(paths.activity(acme));
  const exportRow = page
    .getByRole('table', { name: 'Dejavnost skupine' })
    .getByRole('row')
    .filter({ hasText: 'Kontakti izvoženi' });
  await expect(exportRow).toHaveCount(1);
  await expect(exportRow).toContainText('Eva Horvat');
  await expect(exportRow).toContainText('28');
  const entries = (await api(ana, 'GET', `/groups/${acme.id}/activity?action=CONTACTS_EXPORTED`)).body.items;
  expect(entries).toHaveLength(1);
});
