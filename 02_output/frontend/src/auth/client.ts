/** What the product knows about the signed-in person from Firebase (US-001). */
export interface Account {
  email: string;
  emailVerified: boolean;
}

/**
 * Why an auth call failed, already reduced to what the screens may tell: one reason for every wrong
 * e-mail or password (R-001-05), none for an unregistered address at reset (R-001-08).
 */
export type AuthFailureReason =
  | 'invalid_credentials'
  | 'email_in_use'
  | 'invalid_email'
  | 'cancelled'
  | 'unavailable';

export class AuthFailure extends Error {
  readonly reason: AuthFailureReason;

  constructor(reason: AuthFailureReason) {
    super(reason);
    this.name = 'AuthFailure';
    this.reason = reason;
  }
}

/** The calls the screens make to Firebase Authentication; only `src/auth` talks to Firebase (AR-03). */
export interface AuthClient {
  /** Calls `listener` with the current Account now and after every change; returns the unsubscribe. */
  onChange(listener: (account: Account | null) => void): () => void;
  idToken(): Promise<string | null>;
  signIn(email: string, password: string): Promise<void>;
  signInWithGoogle(): Promise<void>;
  register(displayName: string, email: string, password: string): Promise<void>;
  resetPassword(email: string): Promise<void>;
  resendVerification(): Promise<void>;
  signOut(): Promise<void>;
}
