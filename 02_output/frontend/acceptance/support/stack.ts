import { expect, type Locator, type Page } from '@playwright/test';

/**
 * Harness of the UI acceptance and E2E tests: the test stack of `scripts/test-stack.sh` (environments.md,
 * "test"). Data is set up through the public REST interface (`docs/02_contracts/openapi.yaml`) and the Firebase
 * Auth emulator; the screens are driven by the roles, labels and texts of the wireframes (`docs/wireframes/`).
 * Every value here is a test value that exists only in the emulator and the throwaway database.
 */
export const API = process.env.API_BASE_URL ?? 'http://localhost:8080/api/v1';
export const EMULATOR = process.env.AUTH_EMULATOR_URL ?? 'http://localhost:9099';
export const PROJECT = 'demo-contact-groups';
export const PASSWORD = 'Geslo1234';

export type User = { email: string; displayName: string; uid: string; token: string };
export type Group = { id: string; name: string };
export type Answer = { status: number; body: any };

/** A unique address, so that tests running in parallel never share a person. */
export function unique(local: string, domain = 'acme.com'): string {
  return `${local}.${Date.now().toString(36)}${Math.random().toString(36).slice(2, 7)}@${domain}`;
}

async function emulator(method: string, path: string, body?: unknown, admin = false): Promise<any> {
  const response = await fetch(`${EMULATOR}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json', ...(admin ? { Authorization: 'Bearer owner' } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

/** Creates an account in the emulator; returns the uid. */
export async function createAccount(email: string, displayName: string, verified = true): Promise<string> {
  const answer = await emulator(
    'POST',
    `/identitytoolkit.googleapis.com/v1/projects/${PROJECT}/accounts`,
    { email, password: PASSWORD, displayName, emailVerified: verified },
    true,
  );
  expect(answer?.localId, JSON.stringify(answer)).toBeTruthy();
  return answer.localId;
}

export async function idToken(email: string): Promise<string> {
  const answer = await emulator(
    'POST',
    '/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key',
    { email, password: PASSWORD, returnSecureToken: true },
  );
  expect(answer?.idToken, JSON.stringify(answer)).toBeTruthy();
  return answer.idToken;
}

export async function setVerified(uid: string, verified: boolean): Promise<void> {
  await emulator(
    'POST',
    `/identitytoolkit.googleapis.com/v1/projects/${PROJECT}/accounts:update`,
    { localId: uid, emailVerified: verified },
    true,
  );
}

/** Number of emulator accounts with this e-mail address. */
export async function accountsWith(email: string): Promise<number> {
  const answer = await emulator(
    'POST',
    `/identitytoolkit.googleapis.com/v1/projects/${PROJECT}/accounts:lookup`,
    { email: [email] },
    true,
  );
  return Array.isArray(answer?.users) ? answer.users.length : 0;
}

/** The out-of-band e-mails (verification, password reset) the emulator recorded. */
export async function sentEmails(): Promise<{ email: string; requestType: string }[]> {
  const answer = await emulator('GET', `/emulator/v1/projects/${PROJECT}/oobCodes`);
  return answer?.oobCodes ?? [];
}

/** Opens the verification link the emulator recorded for {@code email}, as the person would (Q-49). */
export async function verifyByLink(email: string): Promise<void> {
  await expect
    .poll(async () => (await sentEmails()).some((m) => m.email === email && m.requestType === 'VERIFY_EMAIL'))
    .toBe(true);
  const codes = (await emulator('GET', `/emulator/v1/projects/${PROJECT}/oobCodes`))?.oobCodes ?? [];
  const code = codes.reverse().find((c: any) => c.email === email && c.requestType === 'VERIFY_EMAIL');
  const response = await fetch(code.oobLink);
  expect(response.ok).toBe(true);
}

/** Calls the REST interface with the token of {@code user}. */
export async function api(user: User | null, method: string, path: string, body?: unknown): Promise<Answer> {
  const response = await fetch(`${API}${path}`, {
    method,
    headers: {
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...(user ? { Authorization: `Bearer ${user.token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  let parsed: any = text;
  try {
    parsed = text ? JSON.parse(text) : null;
  } catch {
    // not JSON (e.g. an export file)
  }
  return { status: response.status, body: parsed };
}

async function ok(answer: Promise<Answer>, status: number): Promise<any> {
  const a = await answer;
  expect(a.status, JSON.stringify(a.body)).toBe(status);
  return a.body;
}

/** A person with an account; a verified person has also called `GET /me` once. */
export async function newUser(displayName: string, local: string, verified = true): Promise<User> {
  const email = unique(local);
  const uid = await createAccount(email, displayName, verified);
  const user = { email, displayName, uid, token: await idToken(email) };
  if (verified) await ok(api(user, 'GET', '/me'), 200);
  return user;
}

export async function createGroup(owner: User, name: string): Promise<Group> {
  const body = await ok(api(owner, 'POST', '/groups', { name }), 201);
  return { id: body.id, name: body.name };
}

/** Invites {@code user} with {@code role} and lets the person open the Group list (US-014 R-014-05). */
export async function addMember(owner: User, group: Group, user: User, role: 'OWNER' | 'EDITOR' | 'VIEWER') {
  await ok(api(owner, 'POST', `/groups/${group.id}/invitations`, { email: user.email, role }), 201);
  const groups = await ok(api(user, 'GET', '/groups'), 200);
  expect(groups.map((g: any) => g.id)).toContain(group.id);
}

export async function createContact(
  user: User,
  group: Group,
  contact: Record<string, unknown>,
): Promise<any> {
  return ok(api(user, 'POST', `/groups/${group.id}/contacts`, contact), 201);
}

export async function getContact(user: User, group: Group, id: string): Promise<any> {
  return ok(api(user, 'GET', `/groups/${group.id}/contacts/${id}`), 200);
}

/** Saves {@code changes} on top of the current Contact with its current version (BR-07). */
export async function editContact(user: User, group: Group, id: string, changes: Record<string, unknown>) {
  const current = await getContact(user, group, id);
  const fields = [
    'firstName',
    'lastName',
    'email',
    'phone',
    'company',
    'jobTitle',
    'street',
    'postalCode',
    'city',
    'country',
    'notes',
  ];
  const body: Record<string, unknown> = {
    tags: current.tags,
    attributes: current.attributes,
    version: current.version,
  };
  for (const f of fields) if (current[f] !== undefined) body[f] = current[f];
  return ok(api(user, 'PUT', `/groups/${group.id}/contacts/${id}`, { ...body, ...changes }), 200);
}

export const paths = {
  contacts: (g: Group) => `/groups/${g.id}/contacts`,
  contact: (g: Group, id: string) => `/groups/${g.id}/contacts/${id}`,
  newContact: (g: Group) => `/groups/${g.id}/contacts/new`,
  tags: (g: Group) => `/groups/${g.id}/tags`,
  trash: (g: Group) => `/groups/${g.id}/trash`,
  members: (g: Group) => `/groups/${g.id}/members`,
  invitations: (g: Group) => `/groups/${g.id}/invitations`,
  activity: (g: Group) => `/groups/${g.id}/activity`,
  duplicates: (g: Group) => `/groups/${g.id}/duplicates`,
};

/** Signs in through the sign-in screen (WF-01) and waits for the Group list (WF-05). */
export async function signIn(page: Page, user: User, expectGroupList = true): Promise<void> {
  await page.goto('/sign-in');
  await page.getByLabel('E-pošta').fill(user.email);
  await page.getByLabel('Geslo').fill(PASSWORD);
  await page.getByRole('button', { name: 'Prijava', exact: true }).click();
  if (expectGroupList)
    await expect(page.getByRole('heading', { level: 1, name: 'Moje skupine' })).toBeVisible();
}

/** Signs in and opens a page of the product. */
export async function openAs(page: Page, user: User, path: string): Promise<void> {
  await signIn(page, user);
  await page.goto(path);
}

/** Chooses an option of a select field, native or not, found by its label. */
export async function choose(page: Page, label: string, option: string): Promise<void> {
  const field = page.getByLabel(label, { exact: true });
  const tag = await field.evaluate((el) => el.tagName.toLowerCase());
  if (tag === 'select') {
    await field.selectOption({ label: option });
  } else {
    await field.click();
    await page.getByRole('option', { name: option, exact: true }).click();
  }
}

/** Fills a date field: ISO for a native date input, `d. M. yyyy` (Slovene date format) otherwise. */
export async function fillDate(field: Locator, iso: string): Promise<void> {
  const type = await field.getAttribute('type');
  if (type === 'date') {
    await field.fill(iso);
  } else {
    const [y, m, d] = iso.split('-').map(Number);
    await field.fill(`${d}. ${m}. ${y}`);
  }
}

/** The region of the Selection bar (WF-06). */
export function selectionBar(page: Page): Locator {
  return page.getByRole('region', { name: 'Izbira' });
}

/** The rows of the contact table (WF-06), without the header row. */
export function contactRows(page: Page): Locator {
  return page
    .getByRole('table', { name: 'Kontakti skupine' })
    .getByRole('row')
    .filter({ hasNot: page.getByRole('columnheader') });
}
