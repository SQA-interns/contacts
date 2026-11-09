import { expect, test } from '@playwright/test';
import { api, createGroup, newUser, openAs, paths } from '../acceptance/support/stack';
import { check } from './check';
import { VIEWPORTS } from './viewports';

/**
 * build-ui checks of WF-14 (delete a Group) and WF-15 (Group Trash) (UI-13, UI-14, UI-15, UI-19, UI-22): the card
 * menu, the dialog with an empty, a wrong and the exact name, the message after the deletion, the list, the refused
 * restore, the restored message, and the loading, error and empty states, at the three widths. Needs the test stack.
 */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('WF-14 dialog states, WF-15 list, restore refused, restored, empty', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');
      await createGroup(ana, 'Klub tekačev');

      await openAs(page, ana, '/groups');
      await page.getByRole('button', { name: 'Možnosti skupine Acme' }).click();
      await expect(page.getByRole('menuitem', { name: 'Izbriši skupino' })).toBeVisible();
      await check(page, 'WF-05_group-list', 'card-menu', viewport.name);
      await page.getByRole('menuitem', { name: 'Izbriši skupino' }).click();

      const dialog = page.getByRole('dialog', { name: 'Izbrišem skupino Acme?' });
      const typed = dialog.getByLabel('Za potrditev vpišite ime skupine');
      await expect(dialog.getByRole('button', { name: 'Izbriši skupino' })).toBeDisabled();
      await check(page, 'WF-14_delete-group', 'empty-input', viewport.name);
      await typed.fill('acme');
      await expect(dialog.getByRole('button', { name: 'Izbriši skupino' })).toBeDisabled();
      await check(page, 'WF-14_delete-group', 'wrong-name', viewport.name);
      await typed.fill('Acme');
      await expect(dialog.getByRole('button', { name: 'Izbriši skupino' })).toBeEnabled();
      await check(page, 'WF-14_delete-group', 'exact-name', viewport.name);
      await dialog.getByRole('button', { name: 'Izbriši skupino' }).click();

      await expect(page.getByText('Skupina je v košu skupin.')).toBeVisible();
      await expect(page.getByRole('heading', { level: 2, name: 'Acme', exact: true })).toHaveCount(0);
      await check(page, 'WF-14_delete-group', 'deleted', viewport.name);

      await page.goto('/group-trash');
      await expect(page.getByRole('heading', { level: 1, name: 'Koš skupin' })).toBeVisible();
      await expect(page.getByText('30 dni').filter({ visible: true })).toBeVisible();
      await check(page, 'WF-15_group-trash', 'list', viewport.name);

      await createGroup(ana, 'ACME');
      await page.getByRole('button', { name: 'Obnovi Acme', exact: true }).click();
      await expect(page.getByRole('alert')).toContainText('Skupino s tem imenom že imate.');
      await check(page, 'WF-15_group-trash', 'restore-refused', viewport.name);

      await api(ana, 'POST', `/groups/${await groupId(ana, 'ACME')}/trash`, { name: 'ACME' });
      await page.reload();
      await page.getByRole('button', { name: 'Obnovi Acme', exact: true }).click();
      await expect(page.getByText('Skupina je obnovljena.')).toBeVisible();
      await check(page, 'WF-15_group-trash', 'restored', viewport.name);
      expect((await api(ana, 'GET', '/groups')).body.map((g: { id: string }) => g.id)).toContain(acme.id);
    });

    test('WF-15 loading, error and empty states', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      await createGroup(ana, 'Acme');

      await page.route('**/api/v1/group-trash', async (route) => {
        await new Promise((resolve) => setTimeout(resolve, 1500));
        await route.fulfill({ status: 500, json: { code: 'server.error', message: 'x' } });
      });
      await openAs(page, ana, '/group-trash');
      await expect(page.getByRole('status')).toBeVisible();
      await check(page, 'WF-15_group-trash', 'loading', viewport.name);
      await expect(page.getByRole('button', { name: 'Poskusi znova' })).toBeVisible();
      await check(page, 'WF-15_group-trash', 'error', viewport.name);

      await page.unroute('**/api/v1/group-trash');
      await page.getByRole('button', { name: 'Poskusi znova' }).click();
      await expect(page.getByText('Koš skupin je prazen')).toBeVisible();
      await check(page, 'WF-15_group-trash', 'empty', viewport.name);
    });

    test('WF-06 Owner opens "Možnosti skupine" in the Group bar and deletes the Group', async ({ page }) => {
      const ana = await newUser('Ana Novak', 'ana');
      const acme = await createGroup(ana, 'Acme');

      await openAs(page, ana, paths.contacts(acme));
      await page.getByRole('button', { name: 'Možnosti skupine' }).click();
      await check(page, 'WF-06_contact-list', 'group-options-menu', viewport.name);
      await page.getByRole('menuitem', { name: 'Izbriši skupino' }).click();
      await page.getByLabel('Za potrditev vpišite ime skupine').fill('Acme');
      await page.getByRole('dialog').getByRole('button', { name: 'Izbriši skupino' }).click();

      await expect(page).toHaveURL(/\/groups$/);
      await expect(page.getByText('Skupina je v košu skupin.')).toBeVisible();
    });
  });
}

async function groupId(user: Awaited<ReturnType<typeof newUser>>, name: string): Promise<string> {
  const groups = (await api(user, 'GET', '/groups')).body as { id: string; name: string }[];
  return groups.find((g) => g.name === name)!.id;
}
