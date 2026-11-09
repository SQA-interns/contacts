import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { toast } from 'sonner';
import {
  CONTACT_FIELDS,
  getContact,
  updateContact,
  type AttributePair,
  type Contact,
  type ContactField,
} from '@/api/contacts';
import { ApiError, errorMessage } from '@/api/errors';
import { ErrorState } from '@/components/ErrorState';
import { LoadingState } from '@/components/LoadingState';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { countryName } from '@/i18n/format';
import type { Language } from '@/i18n';
import { useGroupRole } from '@/features/groups/useGroupRole';
import { ContactHistoryTab } from '@/features/history/ContactHistoryTab';
import { ConflictBanner } from './ConflictBanner';
import { ContactForm, type ContactValues } from './ContactForm';

function titleOf(contact: Contact, fallback: string): string {
  const name = [contact.firstName, contact.lastName].filter(Boolean).join(' ');
  return name || contact.company || contact.email || fallback;
}

/** SC-07 / WF-07: one Contact; Owner and Editor can edit it (R-003-06..09, R-003-11). */
export function ContactPage() {
  const { t } = useTranslation();
  const { groupId = '', contactId = '' } = useParams();
  const queryClient = useQueryClient();
  const role = useGroupRole(groupId);
  const [editing, setEditing] = useState(false);
  const [conflict, setConflict] = useState(false);
  const [tab, setTab] = useState<'details' | 'history'>('details');
  const query = useQuery({
    queryKey: ['contacts', groupId, contactId],
    queryFn: () => getContact(groupId, contactId),
  });
  const save = useMutation({
    mutationFn: (input: { values: ContactValues; attributes: AttributePair[]; tags: string[] }) =>
      updateContact(groupId, contactId, {
        ...input.values,
        attributes: input.attributes,
        tags: input.tags,
        version: query.data?.version,
      }),
    onSuccess: async (contact) => {
      queryClient.setQueryData(['contacts', groupId, contactId], contact);
      await queryClient.invalidateQueries({ queryKey: ['contacts', groupId], exact: true });
      setEditing(false);
      setConflict(false);
      toast.success(t('contacts.saved'));
    },
    onError: (error) => {
      if (error instanceof ApiError && error.code === 'contact.changed_by_someone_else') {
        queryClient.setQueryData(['contacts', groupId, contactId], error.current as Contact);
        setEditing(false);
        setConflict(true);
      }
    },
  });

  if (query.isPending) return <LoadingState rows={6} />;
  if (query.isError) {
    return <ErrorState message={errorMessage(t, query.error)} onRetry={() => void query.refetch()} />;
  }
  const contact = query.data;
  const canEdit = role === 'OWNER' || role === 'EDITOR';
  const error = save.error instanceof ApiError ? save.error : null;

  return (
    <>
      <Link
        to={`/groups/${groupId}/contacts`}
        className="mb-2 inline-block underline-offset-4 hover:underline"
      >
        {t('contacts.back')}
      </Link>
      <PageHeader
        title={titleOf(contact, t('contacts.untitled'))}
        action={
          canEdit && !editing ? (
            <Button
              className="h-11"
              onClick={() => {
                save.reset();
                setConflict(false);
                setEditing(true);
              }}
            >
              {t('contacts.edit')}
            </Button>
          ) : undefined
        }
      />
      <p className="-mt-4 mb-6 text-text-muted">{t('contacts.version', { version: contact.version })}</p>
      {canEdit ? <ContactTabs tab={tab} onChange={setTab} /> : null}
      {conflict ? <ConflictBanner version={contact.version} /> : null}
      {tab === 'history' && canEdit ? (
        <ContactHistoryTab groupId={groupId} contactId={contactId} />
      ) : (
        <div className="mt-4 grid gap-4 lg:grid-cols-[2fr_1fr]">
          <section className="rounded-[12px] border bg-surface-raised p-4">
            <h2 className="mb-4 text-xl font-semibold">{t('contacts.data')}</h2>
            {editing ? (
              <ContactForm
                groupId={groupId}
                key={contact.version}
                initial={contact}
                submitLabel={t('contacts.saveChanges')}
                busyLabel={t('contacts.saving')}
                busy={save.isPending}
                error={error}
                onSubmit={(values, attributes, tags) => save.mutate({ values, attributes, tags })}
                onCancel={() => setEditing(false)}
              />
            ) : (
              <Fields contact={contact} />
            )}
          </section>
          <div className="flex flex-col gap-4">
            <TagsAndAttributes contact={contact} />
          </div>
        </div>
      )}
    </>
  );
}

/** Tabs of SC-07; a Viewer has none (R-016-04). */
function ContactTabs({
  tab,
  onChange,
}: {
  tab: 'details' | 'history';
  onChange: (tab: 'details' | 'history') => void;
}) {
  const { t } = useTranslation();
  const tabs = [
    { id: 'details', label: t('contacts.tabDetails') },
    { id: 'history', label: t('contacts.tabHistory') },
  ] as const;
  return (
    <nav aria-label={t('contacts.tabs')} className="mb-4 flex gap-2 border-b">
      {tabs.map(({ id, label }) => (
        <Button
          key={id}
          variant="ghost"
          className="h-11 rounded-none"
          aria-current={tab === id ? 'page' : undefined}
          onClick={() => onChange(id)}
        >
          {label}
        </Button>
      ))}
    </nav>
  );
}

function Fields({ contact }: { contact: Contact }) {
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

function TagsAndAttributes({ contact }: { contact: Contact }) {
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
