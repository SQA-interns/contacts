import { act } from '@testing-library/react';
import { vi, type Mock } from 'vitest';
import type { Account, AuthClient } from '@/auth/client';

type FakeAuthClient = { [K in keyof AuthClient]: Mock<AuthClient[K]> };

/** An {@link AuthClient} without Firebase: starts with `account` and records every call. */
export function fakeAuthClient(account: Account | null = null) {
  let listener: ((next: Account | null) => void) | undefined;
  const emit = (next: Account | null) => act(() => listener?.(next));
  const client: FakeAuthClient = {
    onChange: vi.fn((next: (a: Account | null) => void) => {
      listener = next;
      next(account);
      return () => undefined;
    }),
    idToken: vi.fn(() => Promise.resolve<string | null>(account ? 'token' : null)),
    signIn: vi.fn(() => Promise.resolve()),
    signInWithGoogle: vi.fn(() => Promise.resolve()),
    register: vi.fn(() => Promise.resolve()),
    resetPassword: vi.fn(() => Promise.resolve()),
    resendVerification: vi.fn(() => Promise.resolve()),
    signOut: vi.fn(async () => {
      listener?.(null);
    }),
  };
  return { client, emit };
}

export const ANA: Account = { email: 'ana@acme.com', emailVerified: true };
export const ANA_UNVERIFIED: Account = { email: 'ana@acme.com', emailVerified: false };
