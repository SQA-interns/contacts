import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams } from 'react-router';
import { toast } from 'sonner';
import { errorMessage } from '@/api/errors';
import { deleteContactForever, listTrash, restoreContact, type TrashContact } from '@/api/trash';
import { EmptyState } from '@/components/EmptyState';
import { ErrorState } from '@/components/ErrorState';
import { LoadingState } from '@/components/LoadingState';
import { Notice } from '@/components/Notice';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { formatDate } from '@/i18n/format';
import type { Language } from '@/i18n';
import { useGroupRole } from '@/features/groups/useGroupRole';
import { DeleteForeverDialog } from './DeleteForeverDialog';
import { useDaysText } from './useDaysText';

function nameOf(contact: TrashContact, fallback: string): string {
  const name = [contact.firstName, contact.lastName].filter(Boolean).join(' ');
  return name || contact.company || contact.email || fallback;
}

/** SC-10 / WF-10: the Contacts in Trash of the Group with the days left and Restore (R-012-03, R-012-04). */
export function TrashPage() {
  const { t } = useTranslation();
  const { groupId = '' } = useParams();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const role = useGroupRole(groupId);
  const allowed = role === 'OWNER' || role === 'EDITOR';
  const trash = useQuery({
    queryKey: ['trash', groupId],
    queryFn: () => listTrash(groupId),
    enabled: allowed,
  });
  const restore = useMutation({
    mutationFn: (contactId: string) => restoreContact(groupId, contactId),
    onSuccess: (contact) => {
      toast.success(t('trash.restored'), {
        action: {
          label: t('trash.openContact'),
          onClick: () => void navigate(`/groups/${groupId}/contacts/${contact.id}`),
        },
      });
    },
    onSettled: async () => {
      await queryClient.invalidateQueries({ queryKey: ['trash', groupId] });
      await queryClient.invalidateQueries({ queryKey: ['contacts', groupId] });
    },
  });

  const [deleting, setDeleting] = useState<string | undefined>();
  const forever = useMutation({
    mutationFn: (contactId: string) => deleteContactForever(groupId, contactId),
    onSettled: async () => {
      setDeleting(undefined);
      await queryClient.invalidateQueries({ queryKey: ['trash', groupId] });
      await queryClient.invalidateQueries({ queryKey: ['contacts', groupId] });
    },
  });

  if (role === undefined) return <LoadingState rows={4} />;
  if (!allowed) {
    return (
      <>
        <PageHeader title={t('trash.title')} />
        <Notice tone="alert" title={t('trash.noPermission')} />
      </>
    );
  }
  return (
    <>
      <PageHeader title={t('trash.title')} />
      <p className="-mt-4 mb-6 text-text-muted">{t('trash.intro')}</p>
      {restore.isError || forever.isError ? (
        <div className="mb-4">
          <Notice tone="alert" title={errorMessage(t, restore.error ?? forever.error)} />
        </div>
      ) : null}
      {trash.isPending ? (
        <LoadingState rows={4} />
      ) : trash.isError ? (
        <ErrorState onRetry={() => void trash.refetch()} />
      ) : trash.data.length === 0 ? (
        <EmptyState title={t('trash.emptyTitle')} body={t('trash.emptyBody')} />
      ) : (
        <TrashTable
          contacts={trash.data}
          busyId={restore.isPending ? restore.variables : undefined}
          onRestore={(id) => restore.mutate(id)}
          onDeleteForever={role === 'OWNER' ? setDeleting : undefined}
          groupId={groupId}
        />
      )}
      <DeleteForeverDialog
        open={deleting !== undefined}
        busy={forever.isPending}
        onConfirm={() => deleting !== undefined && forever.mutate(deleting)}
        onCancel={() => setDeleting(undefined)}
      />
      <p className="mt-6">
        <Link to={`/groups/${groupId}/contacts`} className="underline-offset-4 hover:underline">
          {t('contacts.back')}
        </Link>
      </p>
    </>
  );
}

interface TrashTableProps {
  contacts: TrashContact[];
  busyId: string | undefined;
  onRestore: (contactId: string) => void;
  /** Only an Owner may delete forever (R-013-03); an Editor gets no button. */
  onDeleteForever?: (contactId: string) => void;
  groupId: string;
}

function TrashTable({ contacts, busyId, onRestore, onDeleteForever }: TrashTableProps) {
  const { t, i18n } = useTranslation();
  const language = i18n.language as Language;
  const daysText = useDaysText();
  const actionButtons = (c: TrashContact) => (
    <>
      {restoreButton(c)}
      {onDeleteForever ? (
        <Button
          variant="ghost"
          className="h-11"
          aria-label={t('trash.foreverFor', { name: nameOf(c, t('contacts.untitled')) })}
          onClick={() => onDeleteForever(c.id)}
        >
          {t('trash.forever')}
        </Button>
      ) : null}
    </>
  );
  const restoreButton = (c: TrashContact) => (
    <Button
      variant="secondary"
      className="h-11"
      disabled={busyId === c.id}
      aria-label={t('trash.restoreFor', { name: nameOf(c, t('contacts.untitled')) })}
      onClick={() => onRestore(c.id)}
    >
      {t('trash.restore')}
    </Button>
  );
  return (
    <>
      <table className="hidden w-full table-fixed border-collapse md:table">
        <caption className="sr-only">{t('trash.tableCaption')}</caption>
        <thead>
          <tr className="text-left">
            <th scope="col" className="border-b p-3 font-semibold">
              {t('trash.contactColumn')}
            </th>
            <th scope="col" className="hidden border-b p-3 font-semibold lg:table-cell">
              {t('trash.companyColumn')}
            </th>
            <th scope="col" className="border-b p-3 font-semibold">
              {t('trash.deletedByColumn')}
            </th>
            <th scope="col" className="hidden border-b p-3 font-semibold lg:table-cell">
              {t('trash.deletedAtColumn')}
            </th>
            <th scope="col" className="border-b p-3 font-semibold">
              {t('trash.daysLeftColumn')}
            </th>
            <th scope="col" className="w-72 border-b p-3 text-right font-semibold">
              {t('trash.actionsColumn')}
            </th>
          </tr>
        </thead>
        <tbody>
          {contacts.map((c) => (
            <tr key={c.id}>
              <td className="truncate border-b p-3" title={nameOf(c, t('contacts.untitled'))}>
                {nameOf(c, t('contacts.untitled'))}
              </td>
              <td className="hidden truncate border-b p-3 lg:table-cell" title={c.company ?? ''}>
                {c.company ?? '—'}
              </td>
              <td className="truncate border-b p-3">{c.deletedBy ?? '—'}</td>
              <td className="hidden border-b p-3 lg:table-cell">{formatDate(c.deletedAt, language)}</td>
              <td className="border-b p-3">{daysText(c.daysLeft)}</td>
              <td className="border-b p-3 text-right">{actionButtons(c)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <ul className="flex flex-col gap-3 md:hidden" aria-label={t('trash.tableCaption')}>
        {contacts.map((c) => (
          <li key={c.id} className="flex flex-col gap-2 rounded-[12px] border bg-surface-raised p-4">
            <p className="min-w-0 truncate font-semibold">{nameOf(c, t('contacts.untitled'))}</p>
            <p className="text-text-muted">
              {t('trash.deletedLine', {
                by: c.deletedBy ?? '—',
                date: formatDate(c.deletedAt, language),
              })}
            </p>
            <p>{daysText(c.daysLeft)}</p>
            {actionButtons(c)}
          </li>
        ))}
      </ul>
    </>
  );
}
