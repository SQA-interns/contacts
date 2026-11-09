import { chromium } from '/Users/kyuhi/Desktop/School/SQA_Project/01_Konkretne_Implementacije_Projektov/00_Vecnajemniski-Sistem-za-Vodenje-Kontaktov/02_output/frontend/node_modules/@playwright/test/index.mjs';
import fs from 'node:fs';
const OUT = '/Users/kyuhi/Desktop/School/SQA_Project/01_Konkretne_Implementacije_Projektov/00_Vecnajemniski-Sistem-za-Vodenje-Kontaktov/02_output/logs/screens/review-INC-2';
fs.mkdirSync(OUT, { recursive: true });
const API = 'http://localhost:8080/api/v1', EM = 'http://localhost:9099', P = 'demo-contact-groups', PW = 'Geslo1234', BASE = 'http://localhost:5173';
const uq = (l) => `${l}.${Date.now().toString(36)}${Math.random().toString(36).slice(2,6)}@acme.com`;
async function em(method, path, body, admin) { const r = await fetch(EM + path, { method, headers: { 'Content-Type': 'application/json', ...(admin ? { Authorization: 'Bearer owner' } : {}) }, body: body && JSON.stringify(body) }); const t = await r.text(); return t ? JSON.parse(t) : null; }
async function api(user, method, path, body) { const r = await fetch(API + path, { method, headers: { ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}), Authorization: 'Bearer ' + user.token }, body: body === undefined ? undefined : JSON.stringify(body) }); const t = await r.text(); let b = t; try { b = t ? JSON.parse(t) : null } catch {} return { s: r.status, b }; }
async function mk(name, l) { const email = uq(l); const a = await em('POST', `/identitytoolkit.googleapis.com/v1/projects/${P}/accounts`, { email, password: PW, displayName: name, emailVerified: true }, true); const t = await em('POST', '/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key', { email, password: PW, returnSecureToken: true }); const user = { email, token: t.idToken }; await api(user, 'GET', '/me'); return user; }
const ana = await mk('Ana Novak', 'ana');
const acme = (await api(ana, 'POST', '/groups', { name: 'Acme' })).b;
const beta = (await api(ana, 'POST', '/groups', { name: 'Beta' })).b;
const mkc = async (b) => (await api(ana, 'POST', `/groups/${acme.id}/contacts`, b)).b;
const A = await mkc({ firstName: 'Ana', lastName: 'Novak', email: 'ana@acme.com', company: 'Acme d.o.o.', tags: ['udelezenec2025'], attributes: [{ key: 'kompetenca', value: 'java' }, { key: 'jezik', value: 'sl' }] });
const B = await mkc({ firstName: 'ana', email: 'ana@acme.com', phone: '040 123 456', city: 'Celje', tags: ['neObvescaj'], attributes: [{ key: 'kompetenca', value: 'spletnoProgramiranje' }] });
const C = await mkc({ firstName: 'Janez', lastName: 'Horvat', company: 'Beta d.o.o.', phone: '041 555 111' });
const D = await mkc({ firstName: 'janez', lastName: 'horvat', company: 'BETA d.o.o.', city: 'Maribor' });
// versions: edit A twice
for (const c of ['Beta d.o.o.', 'Cezar d.o.o.']) { const cur = (await api(ana, 'GET', `/groups/${acme.id}/contacts/${A.id}`)).b; const f = ['firstName','lastName','email','phone','company','jobTitle','street','postalCode','city','country','notes']; const body = { tags: cur.tags, attributes: cur.attributes, version: cur.version }; for (const k of f) if (cur[k] !== undefined) body[k] = cur[k]; body.company = c; const r = await api(ana, 'PUT', `/groups/${acme.id}/contacts/${A.id}`, body); if (r.s !== 200) console.log('EDIT FAILED', r.s, JSON.stringify(r.b)); }
console.log('trash beta', (await api(ana, 'POST', `/groups/${beta.id}/trash`, { name: 'Beta' })).s);
const results = [];
const browser = await chromium.launch();
const W = [['mobile', 375, 812], ['tablet', 768, 1024], ['desktop', 1440, 900]];
for (const [name, width, height] of W) {
  const ctx = await browser.newContext({ viewport: { width, height } });
  const page = await ctx.newPage(); page.setDefaultTimeout(8000);
  const shot = async (id, state) => {
    await page.waitForTimeout(500);
    const o = await page.evaluate(() => ({ sw: document.documentElement.scrollWidth, cw: document.documentElement.clientWidth, h1: document.querySelectorAll('h1').length, title: document.querySelector('h1')?.textContent }));
    results.push(`${id} ${state} ${name}: scrollWidth ${o.sw}/${o.cw}${o.sw > o.cw ? ' OVERFLOW' : ''}, h1=${o.h1} "${o.title ?? ''}"`);
    await page.screenshot({ path: `${OUT}/${id}_${state}_${name}.png`, fullPage: true });
  };
  const step = async (label, fn) => { try { await fn(); } catch (e) { results.push(`STEP FAILED ${label} ${name}: ${String(e.message).split('\n')[0]}`); } };
  await page.goto(BASE + '/sign-in');
  await page.getByLabel('E-pošta').fill(ana.email); await page.getByLabel('Geslo').fill(PW);
  await page.getByRole('button', { name: 'Prijava', exact: true }).click();
  await page.getByRole('heading', { level: 1, name: 'Moje skupine' }).waitFor();
  await step('WF-05', async () => { await shot('WF-05', 'list'); });
  await step('WF-05 menu + WF-14', async () => {
    await page.getByRole('button', { name: /Možnosti skupine/ }).first().click(); await page.waitForTimeout(300); await shot('WF-05', 'menu');
    await page.getByRole('menuitem', { name: 'Izbriši skupino' }).click(); await page.waitForTimeout(500);
    await shot('WF-14', 'empty-input');
    await page.getByRole('dialog').getByRole('textbox').fill('wrong'); await shot('WF-14', 'wrong-name'); await page.getByRole('dialog').getByRole('textbox').fill(' Acme '); await shot('WF-14', 'exact-name');
    await page.keyboard.press('Escape');
  });
  await step('WF-15', async () => { await page.goto(BASE + '/group-trash'); await page.waitForTimeout(800); await shot('WF-15', 'list'); });
  await step('WF-07 versions', async () => {
    await page.goto(`${BASE}/groups/${acme.id}/contacts/${A.id}?tab=versions`); await page.waitForTimeout(800); await shot('WF-07', 'versions-list');
    await page.getByRole('button', { name: /Primerjaj/ }).first().click({ timeout: 8000 }); await page.waitForTimeout(500); await shot('WF-07', 'versions-compare');
  });
  await step('WF-07 restore', async () => {
    await page.goto(`${BASE}/groups/${acme.id}/contacts/${A.id}?tab=versions`); await page.waitForTimeout(800);
    await page.getByRole('button', { name: /Obnovi/ }).first().click({ timeout: 8000 }); await page.waitForTimeout(400); await shot('WF-07', 'restore-confirm');
    await page.keyboard.press('Escape');
  });
  await step('WF-17', async () => { await page.goto(`${BASE}/groups/${acme.id}/duplicates`); await page.waitForTimeout(900); await shot('WF-17', 'list'); });
  await step('WF-18', async () => {
    await page.goto(`${BASE}/groups/${acme.id}/duplicates`); await page.waitForTimeout(900);
    await page.getByRole('button', { name: /Združi izbrane/ }).first().click(); await page.waitForTimeout(900); results.push('WF-18 has Kraj row: ' + (await page.getByText('Kraj', { exact: true }).count()) + ', Celje: ' + (await page.getByText('Celje').count())); await shot('WF-18', 'merge');
  });
  await step('WF-16', async () => {
    await page.goto(BASE + '/account'); await page.waitForTimeout(700); await shot('WF-16', 'details');
    await page.getByRole('button', { name: 'Izbriši račun' }).first().click(); await page.waitForTimeout(300); await shot('WF-16', 'confirm');
    await page.getByRole('dialog').getByRole('button', { name: /Izbriši račun/ }).click(); await page.waitForTimeout(900); await shot('WF-16', 'refused');
  });
  await ctx.close();
}
await browser.close();
console.log(results.join('\n'));
console.log(JSON.stringify({ acme: acme.id, A: A.id, user: ana.email }));
