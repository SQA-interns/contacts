import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useAuth } from '@/auth/AuthProvider';
import { FormField } from '@/components/FormField';
import { Notice } from '@/components/Notice';
import { Button } from '@/components/ui/button';
import { AuthCard } from './AuthCard';
import { isEmail } from './rules';
import { useAuthAction } from './useAuthAction';

/** SC-03 / WF-03: Firebase's reset e-mail, the same confirmation for every address (R-001-08). */
export function ResetPasswordPage() {
  const { t } = useTranslation();
  const { client } = useAuth();
  const { busy, failure, run } = useAuthAction();
  const [email, setEmail] = useState('');
  const [invalid, setInvalid] = useState(false);
  const [sent, setSent] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    const address = email.trim();
    setInvalid(!isEmail(address));
    if (isEmail(address)) setSent(await run(() => client.resetPassword(address)));
  }

  const back = (
    <Link to="/sign-in" className="text-sm underline">
      {t('auth.reset.back')}
    </Link>
  );

  return (
    <AuthCard title={t('auth.reset.title')}>
      {sent ? (
        <>
          <Notice tone="status" title={t('auth.reset.sentTitle')}>
            <p>{t('auth.reset.sentBody')}</p>
          </Notice>
          {back}
        </>
      ) : (
        <>
          {failure && failure !== 'invalid_email' ? <Notice tone="alert" title={t('error.generic')} /> : null}
          <p className="text-text-muted">{t('auth.reset.intro')}</p>
          <form noValidate onSubmit={(e) => void submit(e)} className="flex flex-col gap-4">
            <FormField
              id="reset-email"
              label={t('auth.email')}
              type="email"
              autoComplete="email"
              required
              value={email}
              error={invalid || failure === 'invalid_email' ? t('api.validation.email_invalid') : undefined}
              onChange={(e) => setEmail(e.target.value)}
            />
            <Button type="submit" className="h-11 w-full" disabled={busy} aria-busy={busy || undefined}>
              {busy ? t('auth.reset.busy') : t('auth.reset.submit')}
            </Button>
          </form>
          {back}
        </>
      )}
    </AuthCard>
  );
}
