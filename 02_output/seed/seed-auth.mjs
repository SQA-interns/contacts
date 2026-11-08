// Creates the fake sign-in accounts of the local seed in the Firebase Auth EMULATOR (never real Firebase).
// Usage: FIREBASE_PROJECT_ID=<id> node seed/seed-auth.mjs [emulator-url]
// The accounts are test values for local development only (see seed/README.md).
const emulator = process.argv[2] ?? 'http://localhost:9099';
const project = process.env.FIREBASE_PROJECT_ID;
if (!project) throw new Error('FIREBASE_PROJECT_ID is not set');
if (!/^https?:\/\/(localhost|127\.0\.0\.1|emulator)(:\d+)?$/.test(emulator)) {
  throw new Error(`refusing to seed ${emulator}: not a local emulator address`);
}

const users = [
  { localId: 'seed-ana', email: 'ana@example.com', displayName: 'Ana Seed' },
  { localId: 'seed-boris', email: 'boris@example.com', displayName: 'Boris Seed' },
  { localId: 'seed-cene', email: 'cene@example.com', displayName: 'Cene Seed' },
  { localId: 'seed-unverified', email: 'neza@example.com', displayName: 'Neža Seed', emailVerified: false },
];
const password = 'Seed-Passw0rd!';

for (const user of users) {
  const response = await fetch(`${emulator}/identitytoolkit.googleapis.com/v1/projects/${project}/accounts`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: 'Bearer owner' },
    body: JSON.stringify({ ...user, emailVerified: user.emailVerified ?? true, password }),
  });
  const body = await response.json();
  if (response.ok) console.log(`created ${user.email}`);
  else if (body?.error?.message === 'DUPLICATE_LOCAL_ID' || body?.error?.message === 'EMAIL_EXISTS') console.log(`exists  ${user.email}`);
  else throw new Error(`${user.email}: ${JSON.stringify(body)}`);
}
