import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router';
import { toast } from 'sonner';
import { getContact, updateContact, type AttributePair, type Contact } from '@/api/contacts';
import { ApiError, errorMessage } from '@/api/errors';
import { trashContact } from '@/api/trash';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { ErrorState } from '@/components/ErrorState';
import { LoadingState } from '@/components/LoadingState';
import { Notice } from '@/components/Notice';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { useGroupRole } from '@/features/groups/useGroupRole';
import { ContactHistoryTab } from '@/features/history/ContactHistoryTab';
import { ConflictBanner } from './ConflictBanner';
import { ContactForm, type ContactValues } from './ContactForm';
import { Fields, TagsAndAttributes } from './ContactView';

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
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const tab = params.get('tab') === 'history' ? 'history' : 'details';
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

  const remove = useMutation({
    mutationFn: () => trashContact(groupId, contactId),
    onSuccess: async () => {
      queryClient.removeQueries({ queryKey: ['contacts', groupId, contactId] });
      await queryClient.invalidateQueries({ queryKey: ['contacts', groupId] });
      await queryClient.invalidateQueries({ queryKey: ['trash', groupId] });
      toast.success(t('contacts.movedToTrash'), {
        action: { label: t('contacts.openTrash'), onClick: () => void navigate(`/groups/${groupId}/trash`) },
      });
      void navigate(`/groups/${groupId}/contacts`);
    },
    onSettled: () => setConfirmingDelete(false),
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
            <div className="flex gap-2">
              <Button variant="outline" className="h-11" onClick={() => setConfirmingDelete(true)}>
                {t('contacts.delete')}
              </Button>
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
            </div>
          ) : undefined
        }
      />
      <ConfirmDialog
        open={confirmingDelete}
        title={t('contacts.deleteTitle')}
        description={t('contacts.deleteBody')}
        confirmLabel={remove.isPending ? t('contacts.deleteBusy') : t('contacts.deleteConfirm')}
        destructive
        busy={remove.isPending}
        onConfirm={() => remove.mutate()}
        onCancel={() => setConfirmingDelete(false)}
      />
      {remove.isError ? (
        <div className="mb-4">
          <Notice tone="alert" title={errorMessage(t, remove.error)} />
        </div>
      ) : null}
      <p className="-mt-4 mb-6 text-text-muted">{t('contacts.version', { version: contact.version })}</p>
      {canEdit ? <ContactTabs tab={tab} /> : null}
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

/** Tabs of SC-07 as links to `?tab=`; a Viewer has none (R-016-04). */
function ContactTabs({ tab }: { tab: 'details' | 'history' }) {
  const { t } = useTranslation();
  const tabs = [
    { id: 'details', label: t('contacts.tabDetails') },
    { id: 'history', label: t('contacts.tabHistory') },
  ] as const;
  return (
    <nav aria-label={t('contacts.tabs')} className="mb-4 flex gap-2 border-b">
      {tabs.map(({ id, label }) => (
        <Link
          key={id}
          to={id === 'details' ? '?' : `?tab=${id}`}
          replace
          className="inline-flex h-11 items-center rounded-none px-4 font-medium hover:bg-surface"
          aria-current={tab === id ? 'page' : undefined}
        >
          {label}
        </Link>
      ))}
    </nav>
  );
}
