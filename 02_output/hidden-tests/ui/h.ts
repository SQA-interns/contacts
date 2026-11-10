import { expect, type Page } from '@playwright/test';

/** Self-contained helpers of the hidden UI cases (emulator and REST set-up, sign-in through WF-01). */
export const API = process.env.API_BASE_URL ?? 'http://localhost:8080/api/v1';
export const EMU = process.env.AUTH_EMULATOR_URL ?? 'http://localhost:9099';
export const PASSWORD = 'Geslo1234';
export type U = { email: string; uid: string; token: string };
export type G = { id: string; name: string };

export const mail = (local: string) => `${local}.${Date.now().toString(36)}${Math.random().toString(36).slice(2, 7)}@hidden.test`;

async function emu(method: string, path: string, body?: unknown, admin = false): Promise<any> {
  const r = await fetch(`${EMU}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json', ...(admin ? { Authorization: 'Bearer owner' } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const t = await r.text();
  return t ? JSON.parse(t) : null;
}

export async function accounts(email: string): Promise<number> {
  const a = await emu('POST', '/identitytoolkit.googleapis.com/v1/projects/demo-contact-groups/accounts:lookup', { email: [email] }, true);
  return a?.users?.length ?? 0;
}

export async function person(name: string, local: string, verified = true): Promise<U> {
  const email = mail(local);
  const a = await emu('POST', '/identitytoolkit.googleapis.com/v1/projects/demo-contact-groups/accounts', { email, password: PASSWORD, displayName: name, emailVerified: verified }, true);
  const s = await emu('POST', '/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key', { email, password: PASSWORD, returnSecureToken: true });
  const u = { email, uid: a.localId, token: s.idToken };
  if (verified) await call(u, 'GET', '/me');
  return u;
}

export async function call(u: U, method: string, path: string, body?: unknown): Promise<any> {
  const r = await fetch(`${API}${path}`, {
    method,
    headers: { Authorization: `Bearer ${u.token}`, ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const t = await r.text();
  try {
    return { status: r.status, body: t ? JSON.parse(t) : null };
  } catch {
    return { status: r.status, body: t };
  }
}

export async function group(owner: U, name: string): Promise<G> {
  const r = await call(owner, 'POST', '/groups', { name });
  expect(r.status).toBe(201);
  return { id: r.body.id, name };
}

export async function join(owner: U, g: G, u: U, role: string) {
  expect((await call(owner, 'POST', `/groups/${g.id}/invitations`, { email: u.email, role })).status).toBe(201);
  expect((await call(u, 'GET', '/groups')).status).toBe(200);
}

export async function contact(u: U, g: G, body: Record<string, unknown>): Promise<string> {
  const r = await call(u, 'POST', `/groups/${g.id}/contacts`, body);
  expect(r.status).toBe(201);
  return r.body.id;
}

export async function signIn(page: Page, u: U, wait = true) {
  await page.goto('/sign-in');
  await page.getByLabel('E-pošta').fill(u.email);
  await page.getByLabel('Geslo').fill(PASSWORD);
  await page.getByRole('button', { name: 'Prijava', exact: true }).click();
  if (wait) await expect(page.getByRole('heading', { level: 1, name: 'Moje skupine' })).toBeVisible();
}

export async function register(page: Page, name: string, email: string, password: string) {
  await page.goto('/register');
  await page.getByLabel('Prikazno ime').fill(name);
  await page.getByLabel('E-pošta').fill(email);
  await page.getByLabel('Geslo').fill(password);
  await page.getByRole('button', { name: 'Ustvari račun' }).click();
}

export const rows = (page: Page) =>
  page.getByRole('table', { name: 'Kontakti skupine' }).getByRole('row').filter({ hasNot: page.getByRole('columnheader') });
export const bar = (page: Page) => page.getByRole('region', { name: 'Izbira' });
export const lang = async (page: Page, l: 'SL' | 'EN') => {
  await page.getByRole('group', { name: /Jezik|Language/ }).getByRole('button', { name: l, exact: true }).first().click();
  await expect(page.locator('html')).toHaveAttribute('lang', l.toLowerCase());
};
