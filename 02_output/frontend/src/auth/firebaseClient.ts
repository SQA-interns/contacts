import {
  createUserWithEmailAndPassword,
  GoogleAuthProvider,
  onAuthStateChanged,
  sendEmailVerification,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
  updateProfile,
  type Auth,
  type User,
} from 'firebase/auth';
import { AuthFailure, type AuthClient, type AuthFailureReason } from './client';
import { firebaseAuth } from './firebase';

type Reasons = Record<string, AuthFailureReason>;

const SIGN_IN: Reasons = {
  'auth/invalid-credential': 'invalid_credentials',
  'auth/wrong-password': 'invalid_credentials',
  'auth/user-not-found': 'invalid_credentials',
  'auth/invalid-email': 'invalid_credentials',
  'auth/missing-password': 'invalid_credentials',
};
const REGISTER: Reasons = {
  'auth/email-already-in-use': 'email_in_use',
  'auth/invalid-email': 'invalid_email',
};
const RESET: Reasons = { 'auth/invalid-email': 'invalid_email', 'auth/missing-email': 'invalid_email' };
const GOOGLE: Reasons = {
  'auth/popup-closed-by-user': 'cancelled',
  'auth/cancelled-popup-request': 'cancelled',
};

async function mapped<T>(call: Promise<T>, reasons: Reasons, accepted: string[] = []): Promise<void> {
  try {
    await call;
  } catch (error) {
    const code = (error as { code?: string }).code ?? '';
    if (accepted.includes(code)) return;
    throw new AuthFailure(reasons[code] ?? 'unavailable');
  }
}

/** A restored but unverified session is reloaded, so that a verification done elsewhere counts (EX-001-09). */
async function current(user: User): Promise<User> {
  if (user.emailVerified) return user;
  try {
    await user.reload();
    if (user.emailVerified) await user.getIdToken(true);
  } catch {
    /* offline: keep what Firebase remembered */
  }
  return user;
}

async function register(auth: Auth, displayName: string, email: string, password: string): Promise<void> {
  const { user } = await createUserWithEmailAndPassword(auth, email, password);
  await updateProfile(user, { displayName });
  await sendEmailVerification(user);
}

/** The Firebase implementation of {@link AuthClient}. */
export function createFirebaseClient(auth: () => Auth = firebaseAuth): AuthClient {
  const signedIn = (): User => {
    const user = auth().currentUser;
    if (!user) throw new AuthFailure('unavailable');
    return user;
  };
  return {
    onChange: (listener) =>
      onAuthStateChanged(auth(), (user) => {
        if (!user) return listener(null);
        void current(user).then((u) => listener({ email: u.email ?? '', emailVerified: u.emailVerified }));
      }),
    idToken: () => auth().currentUser?.getIdToken() ?? Promise.resolve(null),
    signIn: (email, password) => mapped(signInWithEmailAndPassword(auth(), email, password), SIGN_IN),
    signInWithGoogle: () => mapped(signInWithPopup(auth(), new GoogleAuthProvider()), GOOGLE),
    register: (displayName, email, password) =>
      mapped(register(auth(), displayName, email, password), REGISTER),
    resetPassword: (email) => mapped(sendPasswordResetEmail(auth(), email), RESET, ['auth/user-not-found']),
    resendVerification: () =>
      mapped(
        Promise.resolve().then(() => sendEmailVerification(signedIn())),
        {},
      ),
    signOut: () => signOut(auth()),
  };
}
