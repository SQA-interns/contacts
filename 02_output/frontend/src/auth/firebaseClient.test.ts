import { beforeEach, describe, expect, it, vi } from 'vitest';

const sdk = vi.hoisted(() => ({
  signInWithEmailAndPassword: vi.fn(),
  signInWithPopup: vi.fn(),
  createUserWithEmailAndPassword: vi.fn(),
  updateProfile: vi.fn(),
  sendEmailVerification: vi.fn(),
  sendPasswordResetEmail: vi.fn(),
  signOut: vi.fn(),
  onAuthStateChanged: vi.fn(),
  GoogleAuthProvider: vi.fn(),
}));
vi.mock('firebase/auth', () => sdk);

import { AuthFailure } from './client';
import { createFirebaseClient } from './firebaseClient';

const auth = { currentUser: null } as never;
const client = createFirebaseClient(() => auth);
const firebaseError = (code: string) => Object.assign(new Error(code), { code });

async function reasonOf(promise: Promise<unknown>): Promise<string> {
  try {
    await promise;
  } catch (error) {
    if (error instanceof AuthFailure) return error.reason;
    throw error;
  }
  return 'resolved';
}

describe('Firebase auth client (US-001)', () => {
  beforeEach(() => vi.clearAllMocks());

  it.each(['auth/invalid-credential', 'auth/wrong-password', 'auth/user-not-found', 'auth/invalid-email'])(
    'R-001-05 sign-in failure %s is the one reason invalid_credentials',
    async (code) => {
      sdk.signInWithEmailAndPassword.mockRejectedValue(firebaseError(code));
      expect(await reasonOf(client.signIn('ana@acme.com', 'x'))).toBe('invalid_credentials');
    },
  );

  it('R-001-05 another sign-in failure is unavailable, not a credential message', async () => {
    sdk.signInWithEmailAndPassword.mockRejectedValue(firebaseError('auth/network-request-failed'));
    expect(await reasonOf(client.signIn('ana@acme.com', 'x'))).toBe('unavailable');
  });

  it('R-001-01 register creates the account, saves the display name and sends the verification e-mail', async () => {
    const user = { uid: 'u1' };
    sdk.createUserWithEmailAndPassword.mockResolvedValue({ user });

    await client.register('Ana Novak', 'ana@acme.com', 'Geslo1234');

    expect(sdk.createUserWithEmailAndPassword).toHaveBeenCalledWith(auth, 'ana@acme.com', 'Geslo1234');
    expect(sdk.updateProfile).toHaveBeenCalledWith(user, { displayName: 'Ana Novak' });
    expect(sdk.sendEmailVerification).toHaveBeenCalledWith(user);
  });

  it('R-001-07 an address that is registered is the reason email_in_use', async () => {
    sdk.createUserWithEmailAndPassword.mockRejectedValue(firebaseError('auth/email-already-in-use'));
    expect(await reasonOf(client.register('Ana', 'ana@acme.com', 'Geslo1234'))).toBe('email_in_use');
    expect(sdk.sendEmailVerification).not.toHaveBeenCalled();
  });

  it('R-001-08 a reset for an unregistered address resolves like one for a registered address', async () => {
    sdk.sendPasswordResetEmail.mockRejectedValueOnce(firebaseError('auth/user-not-found'));
    expect(await reasonOf(client.resetPassword('nobody@acme.com'))).toBe('resolved');
    sdk.sendPasswordResetEmail.mockResolvedValueOnce(undefined);
    expect(await reasonOf(client.resetPassword('ana@acme.com'))).toBe('resolved');
  });

  it('R-001-08 an invalid address is the reason invalid_email', async () => {
    sdk.sendPasswordResetEmail.mockRejectedValue(firebaseError('auth/invalid-email'));
    expect(await reasonOf(client.resetPassword('ana@'))).toBe('invalid_email');
  });

  it('R-001-02 Sign in with Google opens the Google popup', async () => {
    sdk.signInWithPopup.mockResolvedValue({});
    await client.signInWithGoogle();
    expect(sdk.GoogleAuthProvider).toHaveBeenCalled();
    expect(sdk.signInWithPopup).toHaveBeenCalledWith(auth, expect.any(Object));
  });

  it('R-001-02 a closed Google popup is the reason cancelled', async () => {
    sdk.signInWithPopup.mockRejectedValue(firebaseError('auth/popup-closed-by-user'));
    expect(await reasonOf(client.signInWithGoogle())).toBe('cancelled');
  });

  it('R-001-04 an unverified account is reloaded so that a verification is seen after a reload (EX-001-09)', async () => {
    const user = {
      email: 'ana@acme.com',
      emailVerified: false,
      reload: vi.fn(async () => {
        user.emailVerified = true;
      }),
      getIdToken: vi.fn(async () => 'token'),
    };
    sdk.onAuthStateChanged.mockImplementation((_auth, next: (u: unknown) => void) => {
      void next(user);
      return () => undefined;
    });
    const seen = vi.fn();

    client.onChange(seen);

    await vi.waitFor(() => expect(seen).toHaveBeenCalledWith({ email: 'ana@acme.com', emailVerified: true }));
    expect(user.getIdToken).toHaveBeenCalledWith(true);
  });
});
