import { expect, test } from '@playwright/test';
import { API, newUser, paths, signIn } from '../acceptance/support/stack';

/**
 * E2E-03 Isolation (e2e-scenarios.md): US-018, US-002. Covers AC-002-06, AC-018-03, AC-018-04, DoD-P01, DoD-P02.
 */
test('E2E-03 isolation: Boris, no Member, sees nothing of Acme and its REST answer is 404 without data', async ({
  browser,
}) => {
  const ana = await newUser('Ana Novak', 'ana');
  const boris = await newUser('Boris Kos', 'boris');
  const anaContext = await browser.newContext();
  const borisContext = await browser.newContext();
  const anaPage = await anaContext.newPage();
  const borisPage = await borisContext.newPage();

  // 1. Ana creates Acme with 3 Contacts
  await signIn(anaPage, ana);
  await anaPage.getByRole('button', { name: 'Ustvari skupino' }).first().click();
  const dialog = anaPage.getByRole('dialog', { name: 'Nova skupina' });
  await dialog.getByLabel('Ime skupine').fill('Acme');
  await dialog.getByRole('button', { name: 'Ustvari', exact: true }).click();
  await expect(anaPage.getByRole('heading', { level: 1, name: 'Kontakti' })).toBeVisible();
  const acmeId = anaPage.url().match(/groups\/([^/]+)\/contacts/)![1];
  for (const lastName of ['Novakova', 'Kosova', 'Zupanova']) {
    await anaPage.goto(paths.newContact({ id: acmeId, name: 'Acme' }));
    await anaPage.getByLabel('Ime', { exact: true }).fill('Tajna');
    await anaPage.getByLabel('Priimek', { exact: true }).fill(lastName);
    await anaPage.getByRole('button', { name: 'Shrani kontakt' }).click();
    await expect(anaPage.getByText('Različica 1')).toBeVisible();
  }
  await anaPage.goto(paths.contacts({ id: acmeId, name: 'Acme' }));
  await expect(anaPage.getByText('3 kontakti').first()).toBeVisible();

  // 2. Boris signs in: his Group list is empty and Acme appears nowhere on his screens
  await signIn(borisPage, boris);
  await expect(borisPage.getByRole('heading', { name: 'Še nimate nobene skupine' })).toBeVisible();
  await expect(borisPage.getByText('Acme')).toHaveCount(0);
  await borisPage.goto(paths.contacts({ id: acmeId, name: 'Acme' }));
  await expect(borisPage.getByRole('heading', { name: 'Strani ni mogoče najti' })).toBeVisible();
  for (const secret of ['Acme', 'Novakova', 'Kosova', 'Zupanova']) {
    await expect(borisPage.getByText(secret)).toHaveCount(0);
  }

  // 3. with Boris's token, GET /groups/{Acme id}/contacts from the browser: 404 and no Contact data
  const answer = await borisPage.evaluate(
    async ({ url, token }) => {
      const response = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
      return { status: response.status, body: await response.text() };
    },
    { url: `${API}/groups/${acmeId}/contacts`, token: boris.token },
  );
  expect(answer.status).toBe(404);
  for (const secret of ['Novakova', 'Kosova', 'Zupanova', 'Tajna']) expect(answer.body).not.toContain(secret);

  await anaContext.close();
  await borisContext.close();
});
