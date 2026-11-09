import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { toast } from 'sonner';
import { ApiError, errorMessage } from '@/api/errors';
import { listGroupTrash, restoreGroup, type GroupTrashItem } from '@/api/groups';
import { EmptyState } from '@/components/EmptyState';
import { ErrorState } from '@/components/ErrorState';
import { LoadingState } from '@/components/LoadingState';
import { Notice } from '@/components/Notice';
import { PageHeader } from '@/components/PageHeader';
import { ResponsiveTable, type Column } from '@/components/ResponsiveTable';
import { Button } from '@/components/ui/button';
import { formatDate } from '@/i18n/format';
import type { Language } from '@/i18n';
import { useDaysText } from '@/features/trash/useDaysText';

/** SC-15 / WF-15: the Groups in Trash of which the User is an Owner, with the days left and Restore (R-020-04). */
export function GroupTrashPage() {
  const { t, i18n } = useTranslation();
  const language = i18n.language as Language;
  const daysText = useDaysText();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const trash = useQuery({ queryKey: ['group-trash'], queryFn: listGroupTrash });
  const restore = useMutation({
    mutationFn: restoreGroup,
    onSuccess: (group) => {
      toast.success(t('groupTrash.restored'), {
        action: {
          label: t('groupTrash.openGroup'),
          onClick: () => void navigate(`/groups/${group.id}/contacts`),
        },
      });
    },
    onSettled: async () => {
      await queryClient.invalidateQueries({ queryKey: ['group-trash'] });
      await queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });

  const columns: Column<GroupTrashItem>[] = [
    { key: 'group', header: t('groupTrash.groupColumn'), cell: (g) => <span title={g.name}>{g.name}</span> },
    {
      key: 'deletedAt',
      header: t('groupTrash.deletedAtColumn'),
      cell: (g) => formatDate(g.deletedAt, language),
    },
    { key: 'daysLeft', header: t('groupTrash.daysLeftColumn'), cell: (g) => daysText(g.daysLeft) },
    {
      key: 'actions',
      header: t('groupTrash.actionsColumn'),
      cell: (g) => (
        <Button
          variant="secondary"
          className="h-11"
          disabled={restore.isPending && restore.variables === g.id}
          aria-label={t('groupTrash.restoreFor', { name: g.name })}
          onClick={() => restore.mutate(g.id)}
        >
          {t('groupTrash.restore')}
        </Button>
      ),
    },
  ];

  return (
    <>
      <p className="mb-2">
        <Link to="/groups" className="underline-offset-4 hover:underline">
          {t('groupTrash.back')}
        </Link>
      </p>
      <PageHeader title={t('groupTrash.title')} />
      <p className="-mt-4 mb-6 text-text-muted">{t('groupTrash.intro')}</p>
      {restore.isError ? (
        <div className="mb-4">
          <Notice tone="alert" title={errorMessage(t, restore.error)}>
            {restore.error instanceof ApiError && restore.error.code === 'group.name_taken' ? (
              <p>{t('groupTrash.nameTakenHint')}</p>
            ) : null}
          </Notice>
        </div>
      ) : null}
      {trash.isPending ? (
        <LoadingState rows={3} />
      ) : trash.isError ? (
        <ErrorState onRetry={() => void trash.refetch()} />
      ) : trash.data.length === 0 ? (
        <EmptyState title={t('groupTrash.emptyTitle')} body={t('groupTrash.emptyBody')} />
      ) : (
        <ResponsiveTable
          caption={t('groupTrash.tableCaption')}
          columns={columns}
          rows={trash.data}
          rowKey={(g) => g.id}
        />
      )}
    </>
  );
}
