import { initializeApp, type FirebaseApp } from 'firebase/app';
import { connectAuthEmulator, getAuth, type Auth } from 'firebase/auth';
import { readConfig } from '@/config';

let app: FirebaseApp | undefined;
let auth: Auth | undefined;

/** The Firebase Authentication client. Only `src/auth` may import Firebase (AR-03). */
export function firebaseAuth(): Auth {
  if (!auth) {
    const config = readConfig();
    app = initializeApp(config.firebase);
    auth = getAuth(app);
    if (config.authEmulatorUrl) connectAuthEmulator(auth, config.authEmulatorUrl, { disableWarnings: true });
  }
  return auth;
}
