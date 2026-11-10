// Measures NFR-01..NFR-04 on a running stack with the Firebase Auth EMULATOR and the 10,000-Contact seed
// (seed/generate-large.sql, loaded by `scripts/seed-local.sh --large`). T-69, DoD-P03.
//
//   FIREBASE_PROJECT_ID=<id> node perf/measure.mjs [options]
//
//   --api <url>        backend address                         (default http://localhost:8080)
//   --emulator <url>   Auth emulator, a local address only     (default http://localhost:9099)
//   --group <name>     Group with the 10,000 Contacts          (default "Large Co")
//   --owner <e-mail>   seed account that owns the Group        (default ana@example.com)
//   --users <n>        Users at the same time for NFR-04       (default 20)
//   --minutes <n>      length of the NFR-04 run                (default 5)
//   --only <a,b>       run only some parts of nfr01, nfr02, nfr03, nfr04
//   --out <file>       also write the numbers as JSON
//
// It never touches real Firebase: the emulator address must be local. Every request stays below the rate
// limit of the backend (100 per User per minute), because a 429 would hide the speed of the answer;
// the run therefore takes several minutes. NFR-04 edits the notes of some seed Contacts. Exit code 0
// when every measured NFR meets its pass value, 1 otherwise.
import { writeFileSync } from 'node:fs';

const args = new Map();
for (let i = 2; i < process.argv.length; i += 2) args.set(process.argv[i].replace(/^--/, ''), process.argv[i + 1]);
const API = (args.get('api') ?? 'http://localhost:8080').replace(/\/$/, '');
const EMULATOR = (args.get('emulator') ?? 'http://localhost:9099').replace(/\/$/, '');
const GROUP_NAME = args.get('group') ?? 'Large Co';
const OWNER = args.get('owner') ?? 'ana@example.com';
const USERS = Number(args.get('users') ?? 20);
const MINUTES = Number(args.get('minutes') ?? 5);
const ONLY = new Set((args.get('only') ?? 'nfr01,nfr02,nfr03,nfr04').split(','));
const PROJECT = process.env.FIREBASE_PROJECT_ID;
const PASSWORD = 'Seed-Passw0rd!'; // the test password of the seed accounts, valid only in the emulator
const PER_MINUTE = 90; // our own ceiling per User, below the limit of 100

if (!PROJECT) throw new Error('FIREBASE_PROJECT_ID is not set');
if (!/^https?:\/\/(localhost|127\.0\.0\.1|emulator)(:\d+)?$/.test(EMULATOR)) {
  throw new Error(`refusing ${EMULATOR}: not a local emulator address`);
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const percentile = (sorted, p) => sorted[Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1)];
function stats(ms) {
  const sorted = [...ms].sort((a, b) => a - b);
  return { n: sorted.length, median: percentile(sorted, 50), p95: percentile(sorted, 95), max: sorted.at(-1) };
}
const fmt = (n) => `${Math.round(n)} ms`;

/** A signed-in User with a pacing limiter so that no minute holds more than PER_MINUTE requests. */
class Client {
  constructor(token) {
    this.token = token;
    this.sent = [];
    this.statuses = new Map();
  }
  async pace() {
    for (;;) {
      const now = Date.now();
      this.sent = this.sent.filter((t) => now - t < 60_000);
      if (this.sent.length < PER_MINUTE) return;
      await sleep(this.sent[0] + 60_000 - now + 5);
    }
  }
  /** @returns {{status:number, ms:number, body:any, bytes:number, type:string}} */
  async call(method, path, body) {
    await this.pace();
    this.sent.push(Date.now());
    const started = performance.now();
    const response = await fetch(`${API}/api/v1${path}`, {
      method,
      headers: { Authorization: `Bearer ${this.token}`, ...(body ? { 'Content-Type': 'application/json' } : {}) },
      body: body ? JSON.stringify(body) : undefined,
    });
    const type = response.headers.get('content-type') ?? '';
    const bytes = type.includes('json') ? undefined : (await response.arrayBuffer()).byteLength;
    const parsed = type.includes('json') ? await response.json() : undefined;
    const ms = performance.now() - started;
    this.statuses.set(response.status, (this.statuses.get(response.status) ?? 0) + 1);
    return { status: response.status, ms, body: parsed, bytes, type };
  }
}

async function emulator(path, body) {
  const response = await fetch(`${EMULATOR}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: 'Bearer owner' },
    body: JSON.stringify(body),
  });
  return { ok: response.ok, body: await response.json() };
}
async function signIn(email) {
  const answer = await emulator('/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key', {
    email,
    password: PASSWORD,
    returnSecureToken: true,
  });
  if (!answer.body.idToken) throw new Error(`sign-in of ${email} failed: ${JSON.stringify(answer.body)}`);
  return new Client(answer.body.idToken);
}

const results = [];
function record(nfr, what, measured, passValue, pass) {
  results.push({ nfr, what, measured, passValue, pass });
  console.log(`${pass ? 'PASS' : 'FAIL'}  ${nfr}  ${what}: ${measured} (pass: ${passValue})`);
}

const owner = await signIn(OWNER);
const groups = (await owner.call('GET', '/groups')).body ?? [];
const group = groups.find((g) => g.name === GROUP_NAME);
if (!group) throw new Error(`Group "${GROUP_NAME}" not found for ${OWNER}; load the seed with scripts/seed-local.sh --large`);
const G = `/groups/${group.id}`;
const total = (await owner.call('GET', `${G}/contacts`)).body.total;
console.log(`Group "${GROUP_NAME}" holds ${total} Contacts`);
if (total < 10_000) console.warn('warning: fewer than 10,000 Contacts; the numbers are not those of NFR-01');

async function repeat(client, n, path) {
  const times = [];
  for (let i = 0; i < n; i++) {
    const answer = await client.call('GET', path);
    if (answer.status !== 200) throw new Error(`GET ${path} answered ${answer.status}`);
    times.push(answer.ms);
  }
  return stats(times);
}

if (ONLY.has('nfr01')) {
  const operations = [
    ['list', `${G}/contacts?page=2&sort=lastName`],
    ['search', `${G}/contacts?q=${encodeURIComponent('škerlj')}`],
    ['filter by country and company', `${G}/contacts?country=SI&company=${encodeURIComponent('Acme d.o.o.')}`],
    ['filter by Tag', `${G}/contacts?tags=tag01`],
  ];
  for (const [name, path] of operations) {
    const s = await repeat(owner, 50, path);
    record('NFR-01', `${name}, 50 requests`, `median ${fmt(s.median)}, p95 ${fmt(s.p95)}`, 'median and p95 below 1000 ms', s.median < 1000 && s.p95 < 1000);
  }
}

// ids of Contacts for the opens, the export and the edits
const ids = [];
for (let page = 1; ids.length < 1000 && page <= 60; page++) {
  const answer = await owner.call('GET', `${G}/contacts?page=${page}`);
  ids.push(...answer.body.items.map((c) => c.id));
}

if (ONLY.has('nfr02')) {
  const times = [];
  for (let i = 0; i < 50; i++) {
    const answer = await owner.call('GET', `${G}/contacts/${ids[(i * 17) % ids.length]}`);
    if (answer.status !== 200) throw new Error(`open answered ${answer.status}`);
    times.push(answer.ms);
  }
  const s = stats(times);
  record('NFR-02', '50 opens of a Contact', `median ${fmt(s.median)}, p95 ${fmt(s.p95)}`, 'p95 below 500 ms', s.p95 < 500);
}

if (ONLY.has('nfr03')) {
  const selection = { selection: { contactIds: ids.slice(0, 1000) }, lang: 'en' };
  const times = [];
  let valid = true;
  for (let i = 0; i < 5; i++) {
    const answer = await owner.call('POST', `${G}/exports`, selection);
    valid &&= answer.status === 200 && /spreadsheetml|octet-stream/.test(answer.type) && answer.bytes > 0;
    times.push(answer.ms);
  }
  const worst = Math.max(...times);
  record('NFR-03', `5 exports of ${selection.selection.contactIds.length} Contacts`, `${times.map(fmt).join(', ')}${valid ? '' : '; an answer was not a file'}`, 'each below 10000 ms', valid && worst < 10_000);
}

if (ONLY.has('nfr04')) {
  console.log(`NFR-04: ${USERS} Users for ${MINUTES} minutes (this takes a while)`);
  const clients = [owner];
  for (let n = 1; n < USERS; n++) {
    const email = `perf${n}@example.test`;
    const made = await emulator(`/identitytoolkit.googleapis.com/v1/projects/${PROJECT}/accounts`, {
      localId: `perf-user-${n}`,
      email,
      displayName: `Perf ${n}`,
      emailVerified: true,
      password: PASSWORD,
    });
    if (!made.ok && !['DUPLICATE_LOCAL_ID', 'EMAIL_EXISTS'].includes(made.body?.error?.message)) throw new Error(JSON.stringify(made.body));
    const invited = await owner.call('POST', `${G}/invitations`, { email, role: 'EDITOR' });
    if (![201, 409].includes(invited.status)) throw new Error(`invitation of ${email} answered ${invited.status}`);
    const client = await signIn(email);
    const me = await client.call('GET', '/me'); // joins the Group through the pending Invitation
    if (me.status !== 200) throw new Error(`/me of ${email} answered ${me.status}`);
    clients.push(client);
  }
  const members = (await owner.call('GET', `${G}/members`)).body.length;
  console.log(`${members} Members in the Group`);

  const until = Date.now() + MINUTES * 60_000;
  const reads = [];
  const bad = [];
  let requests = 0;
  async function work(client, index) {
    const own = ids[(index * 31) % ids.length];
    let round = 0;
    while (Date.now() < until) {
      round++;
      const steps = [
        ['read', 'GET', `${G}/contacts?page=${1 + ((round + index) % 40)}`],
        ['read', 'GET', `${G}/contacts?q=${encodeURIComponent('novak')}`],
        ['read', 'GET', `${G}/contacts?country=SI&company=${encodeURIComponent('Beta d.o.o.')}`],
        ['open', 'GET', `${G}/contacts/${own}`],
      ];
      for (const [kind, method, path] of steps) {
        const answer = await client.call(method, path);
        requests++;
        if (answer.status !== 200) bad.push(`${method} ${path} -> ${answer.status}`);
        else if (kind === 'read') reads.push(answer.ms);
        if (Date.now() >= until) return;
      }
      if (round % 4 === 0) {
        const current = (await client.call('GET', `${G}/contacts/${own}`)).body;
        const { id, createdAt, updatedAt, ...fields } = current;
        const edit = await client.call('PUT', `${G}/contacts/${own}`, { ...fields, notes: `perf ${index} round ${round}` });
        requests += 2;
        if (![200, 409].includes(edit.status)) bad.push(`edit -> ${edit.status}`);
      }
      if (round % 8 === 0) {
        const file = await client.call('POST', `${G}/exports`, { selection: { contactIds: ids.slice(index * 10, index * 10 + 50) }, lang: 'en' });
        requests++;
        if (file.status !== 200) bad.push(`export -> ${file.status}`);
      }
      await sleep(200);
    }
  }
  await Promise.all(clients.map(work));
  const s = stats(reads);
  const tooMany = clients.reduce((sum, c) => sum + (c.statuses.get(429) ?? 0), 0);
  record(
    'NFR-04',
    `${clients.length} Users for ${MINUTES} minutes`,
    `${requests} requests, ${bad.length} not 200 (${tooMany} times 429), reads median ${fmt(s.median)}, p95 ${fmt(s.p95)}, max ${fmt(s.max)}${bad.length ? `; first: ${bad[0]}` : ''}`,
    'no error, no 429, reads below 1000 ms',
    bad.length === 0 && tooMany === 0 && s.p95 < 1000,
  );
}

console.log('\n| NFR | Measured | Pass value | Result |\n|---|---|---|---|');
for (const r of results) console.log(`| ${r.nfr} ${r.what} | ${r.measured} | ${r.passValue} | ${r.pass ? 'Met' : 'Not met'} |`);
if (args.get('out')) writeFileSync(args.get('out'), JSON.stringify({ at: new Date().toISOString(), results }, null, 2));
process.exit(results.every((r) => r.pass) ? 0 : 1);
