import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import type { ApiError } from '@/api/errors';
import { errorKey } from '@/api/errors';
import { CONTACT_FIELDS, type ContactField, type ContactFields } from '@/api/contacts';
import { FormField } from '@/components/FormField';
import { Notice } from '@/components/Notice';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { countryName } from '@/i18n/format';
import type { Language } from '@/i18n';
import { COUNTRY_CODES } from './countries';

export type ContactValues = Record<ContactField, string>;

/** The values of a stored Contact as form text (an empty field is an empty string). */
export function valuesOf(contact?: ContactFields): ContactValues {
  const values = {} as ContactValues;
  for (const field of CONTACT_FIELDS) values[field] = contact?.[field] ?? '';
  return values;
}

const WIDE: readonly ContactField[] = ['street', 'country'];

function errorCodes(error: ApiError | null): Partial<Record<string, string>> {
  const codes: Partial<Record<string, string>> = {};
  for (const e of error?.status === 422 ? error.fieldErrors : []) codes[e.field] ??= e.code;
  return codes;
}

interface ContactFormProps {
  initial?: ContactFields;
  submitLabel: string;
  busyLabel: string;
  busy: boolean;
  error: ApiError | null;
  onSubmit: (values: ContactValues) => void;
  onCancel?: () => void;
}

/** WF-07 / WF-08: the 11 predefined fields; the server judges them, the form shows its answer (R-003-03). */
export function ContactForm({
  initial,
  submitLabel,
  busyLabel,
  busy,
  error,
  onSubmit,
  onCancel,
}: ContactFormProps) {
  const { t, i18n } = useTranslation();
  const [values, setValues] = useState(() => valuesOf(initial));
  const codes = errorCodes(error);
  const hasFieldErrors = Object.keys(codes).length > 0;
  const language = i18n.language as Language;
  const countries = [...COUNTRY_CODES].sort((a, b) =>
    countryName(a, language).localeCompare(countryName(b, language), language),
  );

  const set = (field: ContactField, value: string) => setValues((v) => ({ ...v, [field]: value }));
  const submit = (event: FormEvent) => {
    event.preventDefault();
    onSubmit(values);
  };
  const messageOf = (field: ContactField) => (codes[field] ? t(errorKey(codes[field])) : undefined);

  return (
    <form noValidate onSubmit={submit} className="flex flex-col gap-4">
      {hasFieldErrors ? (
        <Notice tone="alert" title={t('api.validation.failed')}>
          <p>{t('contacts.fixFields')}</p>
        </Notice>
      ) : error && error.status !== 409 ? (
        <Notice tone="alert" title={t(`api.${error.code}`, { defaultValue: t('error.generic') })} />
      ) : null}
      <div className="grid gap-4 sm:grid-cols-2">
        {CONTACT_FIELDS.map((field) => {
          const label = t(`contacts.fields.${field}`);
          const wide = WIDE.includes(field) || field === 'notes';
          const id = `contact-${field}`;
          if (field === 'country') {
            return (
              <div key={field} className={`flex flex-col gap-1.5 ${wide ? 'sm:col-span-2' : ''}`}>
                <Label htmlFor={id}>{label}</Label>
                <select
                  id={id}
                  value={values.country}
                  aria-invalid={codes.country ? true : undefined}
                  onChange={(e) => set('country', e.target.value)}
                  className="h-11 w-full rounded-md border border-input bg-transparent px-3 text-base"
                >
                  <option value="">{t('contacts.noCountry')}</option>
                  {countries.map((code) => (
                    <option key={code} value={code}>
                      {countryName(code, language)}
                    </option>
                  ))}
                </select>
                {messageOf('country') ? (
                  <p role="alert" className="text-sm font-semibold text-danger">
                    {messageOf('country')}
                  </p>
                ) : null}
              </div>
            );
          }
          if (field === 'notes') {
            return (
              <div key={field} className="flex flex-col gap-1.5 sm:col-span-2">
                <Label htmlFor={id}>{label}</Label>
                <Textarea
                  id={id}
                  value={values.notes}
                  aria-invalid={codes.notes ? true : undefined}
                  aria-describedby={`${id}-hint`}
                  onChange={(e) => set('notes', e.target.value)}
                />
                <p id={`${id}-hint`} className="text-sm text-text-muted">
                  {t('contacts.notesHint')}
                </p>
                {messageOf('notes') ? (
                  <p role="alert" className="text-sm font-semibold text-danger">
                    {messageOf('notes')}
                  </p>
                ) : null}
              </div>
            );
          }
          return (
            <div key={field} className={wide ? 'sm:col-span-2' : undefined}>
              <FormField
                id={id}
                label={label}
                autoComplete="off"
                value={values[field]}
                error={messageOf(field)}
                onChange={(e) => set(field, e.target.value)}
              />
            </div>
          );
        })}
      </div>
      <p className="text-sm text-text-muted">{t('contacts.oneOfFour')}</p>
      <div className="sticky bottom-0 flex justify-end gap-3 bg-surface py-3 sm:static">
        {onCancel ? (
          <Button type="button" variant="ghost" className="h-11" onClick={onCancel} disabled={busy}>
            {t('common.cancel')}
          </Button>
        ) : null}
        <Button type="submit" className="h-11" disabled={busy} aria-busy={busy || undefined}>
          {busy ? busyLabel : submitLabel}
        </Button>
      </div>
    </form>
  );
}
