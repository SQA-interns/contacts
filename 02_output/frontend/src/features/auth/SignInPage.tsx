import { LogIn } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, Navigate } from 'react-router';
import { useAuth } from '@/auth/AuthProvider';
import { FormField } from '@/components/FormField';
import { LoadingState } from '@/components/LoadingState';
import { Notice } from '@/components/Notice';
import { Button } from '@/components/ui/button';
import { AuthCard } from './AuthCard';
import { useAuthAction } from './useAuthAction';

/** SC-01 / WF-01: e-mail and password or Google (R-001-02, R-001-03, R-001-05, R-001-12). */
export function SignInPage() {
  const { t } = useTranslation();
  const { status, account, client, sessionExpired } = useAuth();
  const { busy, failure, run } = useAuthAction();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  if (account) return <Navigate to="/groups" replace />;
  if (status === 'loading') {
    return (
      <AuthCard>
        <LoadingState rows={4} />
      </AuthCard>
    );
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    void run(() => client.signIn(email.trim(), password));
  }

  return (
    <AuthCard>
      <h1 className="text-2xl font-semibold">{t('auth.signIn.title')}</h1>
      {failure ? (
        <Notice
          tone="alert"
          title={failure === 'invalid_credentials' ? t('auth.signIn.invalidCredentials') : t('error.generic')}
        />
      ) : sessionExpired ? (
        <Notice tone="status" title={t('api.auth.token_invalid')} />
      ) : null}
      <form noValidate onSubmit={submit} className="flex flex-col gap-4">
        <FormField
          id="sign-in-email"
          label={t('auth.email')}
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <FormField
          id="sign-in-password"
          label={t('auth.password')}
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <Link to="/reset-password" className="self-end text-sm underline">
          {t('auth.signIn.forgot')}
        </Link>
        <Button type="submit" className="h-11 w-full" disabled={busy} aria-busy={busy || undefined}>
          {busy ? t('auth.signIn.busy') : t('auth.signIn.submit')}
        </Button>
      </form>
      <div className="flex items-center gap-3 text-sm text-text-muted before:flex-1 before:border-t after:flex-1 after:border-t">
        {t('auth.signIn.or')}
      </div>
      <Button
        type="button"
        variant="secondary"
        className="h-11 w-full"
        disabled={busy}
        onClick={() => void run(() => client.signInWithGoogle())}
      >
        <LogIn aria-hidden="true" />
        {t('auth.signIn.google')}
      </Button>
      <p className="text-sm">
        {t('auth.signIn.noAccount')}{' '}
        <Link to="/register" className="underline">
          {t('auth.signIn.register')}
        </Link>
      </p>
    </AuthCard>
  );
}
