import { onAuthStateChanged, signOut as firebaseSignOut, type User } from 'firebase/auth';
import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { configureApi } from '@/api/client';
import { firebaseAuth } from './firebase';

export type AuthStatus = 'loading' | 'signedOut' | 'signedIn';

interface AuthContextValue {
  status: AuthStatus;
  user: User | null;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue>({
  status: 'loading',
  user: null,
  signOut: () => Promise.resolve(),
});

/** Keeps the Firebase session and gives the API client its token (R-001-09, R-001-12). */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<{ status: AuthStatus; user: User | null }>({
    status: 'loading',
    user: null,
  });

  useEffect(() => {
    let auth;
    try {
      auth = firebaseAuth();
    } catch {
      // Firebase is not configured or cannot start: the shell still renders, signed out.
      setState({ status: 'signedOut', user: null });
      return undefined;
    }
    const current = auth;
    configureApi(
      () => (current.currentUser ? current.currentUser.getIdToken() : Promise.resolve(null)),
      () => void firebaseSignOut(current),
    );
    return onAuthStateChanged(current, (user) => setState({ status: user ? 'signedIn' : 'signedOut', user }));
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ ...state, signOut: () => firebaseSignOut(firebaseAuth()) }),
    [state],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  return useContext(AuthContext);
}
