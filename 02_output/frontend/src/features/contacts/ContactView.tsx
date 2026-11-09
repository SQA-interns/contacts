import { useTranslation } from 'react-i18next';
import { CONTACT_FIELDS, type Contact, type ContactField } from '@/api/contacts';
import { countryName } from '@/i18n/format';
import type { Language } from '@/i18n';

/** The 11 fields of a Contact as a list of labels and values; an empty one shows a dash. */
export function Fields({ contact }: { contact: Contact }) {
  const { t, i18n } = useTranslation();
  const valueOf = (field: ContactField): string => {
    const value = contact[field];
    if (!value) return '';
    return field === 'country' ? countryName(value, i18n.language as Language) : value;
  };
  return (
    <dl className="grid grid-cols-[max-content_1fr] gap-x-6 gap-y-2">
      {CONTACT_FIELDS.map((field) => (
        <div key={field} className="contents">
          <dt className="text-text-muted">{t(`contacts.fields.${field}`)}</dt>
          <dd className="break-words whitespace-pre-wrap">{valueOf(field) || '—'}</dd>
        </div>
      ))}
    </dl>
  );
}

/** The Tags and the Attributes of a Contact, each in its own card. */
export function TagsAndAttributes({ contact }: { contact: Contact }) {
  const { t } = useTranslation();
  return (
    <>
      <section className="rounded-[12px] border bg-surface-raised p-4">
        <h2 className="mb-3 text-xl font-semibold">{t('contacts.tags')}</h2>
        {contact.tags.length === 0 ? (
          <p className="text-text-muted">{t('contacts.noTags')}</p>
        ) : (
          <ul className="flex flex-wrap gap-2">
            {contact.tags.map((tag) => (
              <li key={tag} className="rounded-[8px] border px-2 py-0.5 text-sm">
                {tag}
              </li>
            ))}
          </ul>
        )}
      </section>
      <section className="rounded-[12px] border bg-surface-raised p-4">
        <h2 className="mb-3 text-xl font-semibold">{t('contacts.attributes')}</h2>
        {contact.attributes.length === 0 ? (
          <p className="text-text-muted">{t('contacts.noAttributes')}</p>
        ) : (
          <dl className="grid grid-cols-[max-content_1fr] gap-x-6 gap-y-2">
            {contact.attributes.map((attribute) => (
              <div key={attribute.key} className="contents">
                <dt className="text-text-muted">{attribute.key}</dt>
                <dd className="break-words">{attribute.value}</dd>
              </div>
            ))}
          </dl>
        )}
      </section>
    </>
  );
}
