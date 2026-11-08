import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { useAuth } from '@/auth/AuthProvider';
import { FormField } from '@/components/FormField';
import { Notice } from '@/components/Notice';
import { Button } from '@/components/ui/button';
import { AuthCard } from './AuthCard';
import { isDisplayName, isEmail, PASSWORD_MIN_LENGTH } from './rules';
import { useAuthAction } from './useAuthAction';

type FieldErrors = Partial<Record<'displayName' | 'email' | 'password', string>>;

/** SC-02 / WF-02: display name, e-mail and password (R-001-01, R-001-06, R-001-07). */
export function RegisterPage() {
  const { t } = useTranslation();
  const { client } = useAuth();
  const navigate = useNavigate();
  const { busy, failure, run } = useAuthAction();
  const [values, setValues] = useState({ displayName: '', email: '', password: '' });
  const [errors, setErrors] = useState<FieldErrors>({});

  function check(): FieldErrors {
    const found: FieldErrors = {};
    if (!isDisplayName(values.displayName)) found.displayName = t('api.validation.name_invalid');
    if (!isEmail(values.email.trim())) found.email = t('api.validation.email_invalid');
    if (values.password.length < PASSWORD_MIN_LENGTH) found.password = t('auth.register.passwordShort');
    return found;
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    const found = check();
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    const created = await run(() =>
      client.register(values.displayName.trim(), values.email.trim(), values.password),
    );
    if (created) await navigate('/verify-email', { replace: true });
  }

  const field = (name: keyof typeof values) => ({
    value: values[name],
    error:
      errors[name] ??
      (name === 'email' && failure === 'invalid_email' ? t('api.validation.email_invalid') : undefined),
    required: true,
    onChange: (e: { target: { value: string } }) => setValues({ ...values, [name]: e.target.value }),
  });

  return (
    <AuthCard>
      <h1 className="text-2xl font-semibold">{t('auth.register.title')}</h1>
      {failure === 'email_in_use' ? (
        <Notice tone="alert" title={t('auth.register.emailInUse')}>
          <div className="flex flex-wrap gap-2">
            <Button variant="secondary" className="h-11" onClick={() => void navigate('/sign-in')}>
              {t('auth.signIn.submit')}
            </Button>
            <Button variant="secondary" className="h-11" onClick={() => void navigate('/reset-password')}>
              {t('auth.register.resetPassword')}
            </Button>
          </div>
        </Notice>
      ) : failure && failure !== 'invalid_email' ? (
        <Notice tone="alert" title={t('error.generic')} />
      ) : null}
      <form noValidate onSubmit={(e) => void submit(e)} className="flex flex-col gap-4">
        <FormField
          id="register-name"
          label={t('auth.displayName')}
          hint={t('auth.register.displayNameHint')}
          autoComplete="name"
          {...field('displayName')}
        />
        <FormField
          id="register-email"
          label={t('auth.email')}
          type="email"
          autoComplete="email"
          {...field('email')}
        />
        <FormField
          id="register-password"
          label={t('auth.password')}
          hint={t('auth.register.passwordHint')}
          type="password"
          autoComplete="new-password"
          {...field('password')}
        />
        <Button type="submit" className="h-11 w-full" disabled={busy} aria-busy={busy || undefined}>
          {busy ? t('auth.register.busy') : t('auth.register.submit')}
        </Button>
      </form>
      <p className="text-sm">
        {t('auth.register.haveAccount')}{' '}
        <Link to="/sign-in" className="underline">
          {t('auth.signIn.submit')}
        </Link>
      </p>
    </AuthCard>
  );
}
