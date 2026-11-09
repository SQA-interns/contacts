import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { cancelInvitation, listInvitations, type Invitation } from '@/api/invitations';
import { errorMessage } from '@/api/errors';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { EmptyState } from '@/components/EmptyState';
import { ErrorState } from '@/components/ErrorState';
import { LoadingState } from '@/components/LoadingState';
import { Notice } from '@/components/Notice';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { useGroupRole } from '@/features/groups/useGroupRole';
import type { Language } from '@/i18n';
import { formatDate } from '@/i18n/format';
import { InvitationForm } from './InvitationForm';

const LIMIT = 20;

/** SC-12 / WF-12: invite a person and list the pending Invitations (R-014-01..04, R-014-07, R-014-09, R-014-10). */
export function InvitationsPage() {
  const { t } = useTranslation();
  const { groupId = '' } = useParams();
  const queryClient = useQueryClient();
  const role = useGroupRole(groupId);
  const allowed = role === 'OWNER';
  const invitations = useQuery({
    queryKey: ['invitations', groupId],
    queryFn: () => listInvitations(groupId),
    enabled: allowed,
  });
  const [cancelling, setCancelling] = useState<Invitation | undefined>();
  const cancel = useMutation({
    mutationFn: (id: string) => cancelInvitation(groupId, id),
    onSettled: async () => {
      setCancelling(undefined);
      await queryClient.invalidateQueries({ queryKey: ['invitations', groupId] });
    },
  });

  if (role === undefined) return <LoadingState rows={4} />;
  if (!allowed) {
    return (
      <>
        <PageHeader title={t('invitations.title')} />
        <Notice tone="alert" title={t('invitations.noPermission')} />
      </>
    );
  }
  return (
    <>
      <PageHeader title={t('invitations.title')} />
      {invitations.data ? (
        <p className="-mt-4 mb-6 text-text-muted">
          {t('invitations.count', { count: invitations.data.length, max: LIMIT })}
        </p>
      ) : null}
      {cancel.isError ? (
        <div className="mb-4">
          <Notice tone="alert" title={errorMessage(t, cancel.error)} />
        </div>
      ) : null}
      <div className="grid gap-6 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
        <InvitationForm groupId={groupId} />
        <section aria-labelledby="open-heading">
          <h2 id="open-heading" className="mb-3 text-lg font-semibold">
            {t('invitations.listTitle')}
          </h2>
          {invitations.isPending ? (
            <LoadingState rows={3} />
          ) : invitations.isError ? (
            <ErrorState onRetry={() => void invitations.refetch()} />
          ) : invitations.data.length === 0 ? (
            <EmptyState title={t('invitations.emptyTitle')} body={t('invitations.emptyBody')} />
          ) : (
            <InvitationTable invitations={invitations.data} onCancel={setCancelling} />
          )}
        </section>
      </div>
      <ConfirmDialog
        open={cancelling !== undefined}
        title={t('invitations.cancelTitle')}
        description={t('invitations.cancelBody', { email: cancelling?.email ?? '' })}
        confirmLabel={t('invitations.cancel')}
        destructive
        busy={cancel.isPending}
        onConfirm={() => cancelling && cancel.mutate(cancelling.id)}
        onCancel={() => setCancelling(undefined)}
      />
      <p className="mt-6">
        <Link to={`/groups/${groupId}/contacts`} className="underline-offset-4 hover:underline">
          {t('contacts.back')}
        </Link>
      </p>
    </>
  );
}

interface InvitationTableProps {
  invitations: Invitation[];
  onCancel: (invitation: Invitation) => void;
}

function InvitationTable({ invitations, onCancel }: InvitationTableProps) {
  const { t, i18n } = useTranslation();
  const language = i18n.language as Language;
  const cancelButton = (i: Invitation) => (
    <Button
      variant="ghost"
      className="h-11"
      aria-label={t('invitations.cancelFor', { email: i.email })}
      onClick={() => onCancel(i)}
    >
      {t('invitations.cancel')}
    </Button>
  );
  return (
    <>
      <table className="hidden w-full table-fixed border-collapse md:table">
        <caption className="sr-only">{t('invitations.listTitle')}</caption>
        <thead>
          <tr className="text-left">
            <th scope="col" className="border-b p-3 font-semibold">
              {t('invitations.emailColumn')}
            </th>
            <th scope="col" className="border-b p-3 font-semibold">
              {t('invitations.roleColumn')}
            </th>
            <th scope="col" className="border-b p-3 font-semibold">
              {t('invitations.expiresColumn')}
            </th>
            <th scope="col" className="w-48 border-b p-3 text-right font-semibold">
              {t('invitations.actionsColumn')}
            </th>
          </tr>
        </thead>
        <tbody>
          {invitations.map((i) => (
            <tr key={i.id}>
              <td className="truncate border-b p-3" title={i.email}>
                {i.email}
              </td>
              <td className="border-b p-3">{t(`role.${i.role}`)}</td>
              <td className="border-b p-3">{formatDate(i.expiresAt, language)}</td>
              <td className="border-b p-3 text-right">{cancelButton(i)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <ul className="flex flex-col gap-3 md:hidden" aria-label={t('invitations.listTitle')}>
        {invitations.map((i) => (
          <li key={i.id} className="flex flex-col gap-2 rounded-[12px] border bg-surface-raised p-4">
            <p className="min-w-0 truncate font-semibold">{i.email}</p>
            <p className="text-text-muted">
              {t('invitations.line', {
                role: t(`role.${i.role}`),
                date: formatDate(i.expiresAt, language),
              })}
            </p>
            {cancelButton(i)}
          </li>
        ))}
      </ul>
    </>
  );
}
