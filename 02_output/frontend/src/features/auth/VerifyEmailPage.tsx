import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Navigate, useNavigate } from 'react-router';
import { useAuth } from '@/auth/AuthProvider';
import { LoadingState } from '@/components/LoadingState';
import { Notice } from '@/components/Notice';
import { Button } from '@/components/ui/button';
import { AuthCard } from './AuthCard';
import { useAuthAction } from './useAuthAction';

/** SC-04 / WF-04: no Group until the e-mail is verified; resend and sign out (R-001-04, EX-001-09). */
export function VerifyEmailPage() {
  const { t } = useTranslation();
  const { status, account, client, signOut } = useAuth();
  const navigate = useNavigate();
  const { busy, failure, run } = useAuthAction();
  const [resent, setResent] = useState(false);

  if (status === 'loading') {
    return (
      <AuthCard>
        <LoadingState rows={3} />
      </AuthCard>
    );
  }
  if (!account) return <Navigate to="/sign-in" replace />;
  if (account.emailVerified) return <Navigate to="/groups" replace />;

  async function resend() {
    setResent(await run(() => client.resendVerification()));
  }

  async function leave() {
    await signOut();
    await navigate('/sign-in', { replace: true });
  }

  return (
    <AuthCard>
      <h1 className="text-2xl font-semibold">{t('api.auth.email_not_verified')}</h1>
      {failure ? <Notice tone="alert" title={t('error.generic')} /> : null}
      {resent ? <Notice tone="status" title={t('auth.verify.resent')} /> : null}
      <p className="text-text-muted">{t('auth.verify.body', { email: account.email })}</p>
      <Button
        className="h-11 w-full"
        disabled={busy}
        aria-busy={busy || undefined}
        onClick={() => void resend()}
      >
        {busy ? t('auth.verify.busy') : t('auth.verify.resend')}
      </Button>
      <Button variant="ghost" className="h-11 w-full" onClick={() => void leave()}>
        {t('nav.signOut')}
      </Button>
    </AuthCard>
  );
}
