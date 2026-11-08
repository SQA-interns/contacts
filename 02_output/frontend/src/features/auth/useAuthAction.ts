import { useState } from 'react';
import { AuthFailure, type AuthFailureReason } from '@/auth/client';

/** Runs one auth call at a time and keeps why it failed; a cancelled Google window is no failure. */
export function useAuthAction() {
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<AuthFailureReason | null>(null);

  async function run(action: () => Promise<void>): Promise<boolean> {
    setBusy(true);
    setFailure(null);
    try {
      await action();
      return true;
    } catch (error) {
      const reason = error instanceof AuthFailure ? error.reason : 'unavailable';
      if (reason !== 'cancelled') setFailure(reason);
      return false;
    } finally {
      setBusy(false);
    }
  }

  return { busy, failure, run };
}
