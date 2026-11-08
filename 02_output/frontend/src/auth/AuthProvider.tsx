import { useQueryClient } from '@tanstack/react-query';
import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { configureApi } from '@/api/client';
import type { Account, AuthClient } from './client';
import { createFirebaseClient } from './firebaseClient';

export type AuthStatus = 'loading' | 'signedOut' | 'signedIn';

interface AuthContextValue {
  status: AuthStatus;
  account: Account | null;
  /** The server answered 401 and the session was ended (R-001-12). */
  sessionExpired: boolean;
  client: AuthClient;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * Keeps the Firebase session (R-001-09), gives the API client its token, ends the session on a 401
 * (R-001-12) and forgets every cached answer when nobody is signed in (R-001-10).
 */
export function AuthProvider({ client: given, children }: { client?: AuthClient; children: ReactNode }) {
  const [client] = useState(() => given ?? createFirebaseClient());
  const queryClient = useQueryClient();
  const [session, setSession] = useState<{ status: AuthStatus; account: Account | null }>({
    status: 'loading',
    account: null,
  });
  const [sessionExpired, setSessionExpired] = useState(false);

  useEffect(() => {
    configureApi(
      () => client.idToken(),
      () => {
        setSessionExpired(true);
        void client.signOut();
      },
    );
    try {
      return client.onChange((account) => {
        if (account) setSessionExpired(false);
        else queryClient.clear();
        setSession({ status: account ? 'signedIn' : 'signedOut', account });
      });
    } catch {
      // Firebase is not configured or cannot start: the shell still renders, signed out.
      setSession({ status: 'signedOut', account: null });
      return undefined;
    }
  }, [client, queryClient]);

  const value = useMemo<AuthContextValue>(
    () => ({ ...session, sessionExpired, client, signOut: () => client.signOut() }),
    [session, sessionExpired, client],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth needs an AuthProvider');
  return value;
}
