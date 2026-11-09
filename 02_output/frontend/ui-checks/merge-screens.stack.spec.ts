import { expect, test } from '@playwright/test';
import { createContact, createGroup, newUser, openAs, paths } from '../acceptance/support/stack';
import { check } from './check';
import { VIEWPORTS } from './viewports';

/**
 * build-ui checks of WF-18 (UI-14, UI-19, UI-22): the Merge screen with its choices, an Attribute still to choose,
 * the confirmation, the refusals, the busy button, loading, "nothing to merge" and the error state, at the three
 * widths. Needs the test stack, like `duplicate-screens.stack.spec.ts`.
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-18 Merge: choices, attribute, confirmation, refusals, busy, loading, empty, error', async ({
      page,
    }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');
      await createContact(ana, acme, {
        firstName: 'Ana',
        lastName: 'Novak',
        email: 'ana@acme.com',
        company: 'Acme d.o.o.',
        tags: ['udelezenec2025'],
        attributes: [
          { key: 'kompetenca', value: 'java' },
          { key: 'jezik', value: 'sl' },
        ],
      });
      await createContact(ana, acme, {
        firstName: 'ana',
        email: 'ana@acme.com',
        phone: '040 123 456',
        city: 'Celje',
        tags: ['neObvescaj'],
        attributes: [{ key: 'kompetenca', value: 'spletnoProgramiranje' }],
      });

      await openAs(page, ana, paths.duplicates(acme));
      await page.getByRole('button', { name: /Združi izbrane \(2\)/ }).click();
      await expect(page.getByRole('heading', { level: 1, name: 'Združi kontakte' })).toBeVisible();
      await expect(page.getByRole('radio', { name: 'Ana', exact: true })).toBeChecked();
      await check(page, 'WF-18_merge', 'success', viewport.name);

      await page.getByRole('button', { name: 'Združi', exact: true }).click();
      await expect(page.getByText('Izberite vrednost.')).toBeVisible();
      await check(page, 'WF-18_merge', 'attribute-choice-missing', viewport.name);

      await page.getByRole('radio', { name: 'spletnoProgramiranje' }).check();
      await page.getByRole('button', { name: 'Združi', exact: true }).click();
      await expect(page.getByRole('dialog', { name: 'Združim 2 kontakta?' })).toBeVisible();
      await check(page, 'WF-18_merge', 'confirm', viewport.name);

      await page.route('**/api/v1/groups/*/merges', (route) =>
        route.fulfill({
          status: 422,
          json: {
            code: 'validation.failed',
            message: 'x',
            fieldErrors: [{ field: 'firstName', code: 'validation.required_any' }],
          },
        }),
      );
      await page.getByRole('dialog').getByRole('button', { name: 'Združi', exact: true }).click();
      await expect(page.getByRole('alert').filter({ hasText: 'Podatki niso veljavni.' })).toBeVisible();
      await check(page, 'WF-18_merge', 'refused-validation', viewport.name);

      await page.unroute('**/api/v1/groups/*/merges');
      await page.route('**/api/v1/groups/*/merges', (route) =>
        route.fulfill({
          status: 409,
          json: { code: 'contact.changed_by_someone_else', message: 'x' },
        }),
      );
      await page.getByRole('button', { name: 'Združi', exact: true }).click();
      await page.getByRole('dialog').getByRole('button', { name: 'Združi', exact: true }).click();
      await expect(
        page.getByRole('alert').filter({ hasText: 'Kontakt je spremenil nekdo drug.' }),
      ).toBeVisible();
      await check(page, 'WF-18_merge', 'refused-conflict', viewport.name);

      await page.unroute('**/api/v1/groups/*/merges');
      let release: () => void = () => undefined;
      const gate = new Promise<void>((resolve) => (release = resolve));
      await page.route('**/api/v1/groups/*/merges', async (route) => {
        await gate;
        await route.fulfill({ status: 500, json: { code: 'x', message: 'x' } });
      });
      await page.getByRole('button', { name: 'Združi', exact: true }).click();
      await page.getByRole('dialog').getByRole('button', { name: 'Združi', exact: true }).click();
      await expect(page.getByRole('button', { name: 'Združevanje …' })).toBeVisible();
      await check(page, 'WF-18_merge', 'busy', viewport.name);
      release();
      await expect(page.getByRole('alert')).toBeVisible();
      await page.unroute('**/api/v1/groups/*/merges');

      let releaseContacts: () => void = () => undefined;
      const contactsGate = new Promise<void>((resolve) => (releaseContacts = resolve));
      await page.route('**/api/v1/groups/*/contacts/*', async (route) => {
        await contactsGate;
        await route.continue();
      });
      await page.reload();
      await expect(page.getByRole('status')).toBeAttached();
      await check(page, 'WF-18_merge', 'loading', viewport.name);
      releaseContacts();
      await expect(page.getByRole('radio', { name: 'Ana', exact: true })).toBeVisible();
      await page.unroute('**/api/v1/groups/*/contacts/*');

      await page.route('**/api/v1/groups/*/contacts/*', (route) =>
        route.fulfill({ status: 500, json: { code: 'x', message: 'x' } }),
      );
      await page.reload();
      await expect(page.getByRole('button', { name: 'Poskusi znova' })).toBeVisible();
      await check(page, 'WF-18_merge', 'error', viewport.name);
      await page.unroute('**/api/v1/groups/*/contacts/*');

      await page.goto(`/groups/${acme.id}/merge`);
      await expect(page.getByRole('heading', { name: 'Ni kontaktov za združitev' })).toBeVisible();
      await check(page, 'WF-18_merge', 'nothing-to-merge', viewport.name);

      await page.goto(paths.duplicates(acme));
      await page.getByRole('button', { name: /Združi izbrane \(2\)/ }).click();
      await page.getByRole('radio', { name: 'spletnoProgramiranje' }).check();
      await page.getByRole('button', { name: 'Združi', exact: true }).click();
      await page.getByRole('dialog').getByRole('button', { name: 'Združi', exact: true }).click();
      await expect(page.getByRole('status').filter({ hasText: 'Kontakti so združeni.' })).toBeVisible();
      await check(page, 'WF-18_merge', 'merged', viewport.name);
    });
  });
}
