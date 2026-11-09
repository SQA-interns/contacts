import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { toast } from 'sonner';
import { ApiError, errorMessage, type SoleOwnerGroup } from '@/api/errors';
import { deleteMe, getMe } from '@/api/me';
import { useAuth } from '@/auth/AuthProvider';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { ErrorState } from '@/components/ErrorState';
import { LoadingState } from '@/components/LoadingState';
import { Notice } from '@/components/Notice';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { DeleteGroupDialog } from '@/features/groups/DeleteGroupDialog';
import { useDaysText } from '@/features/trash/useDaysText';

/** What stops the deletion: the Groups of a refused deletion, or the message of another failure. */
interface Refusal {
  message: string;
  groups: SoleOwnerGroup[];
}

/** SC-16 / WF-16: my name and e-mail, and deleting my account after a confirmation (R-021-01, R-021-02, R-021-06). */
export function AccountPage() {
  const { t } = useTranslation();
  const { signOut } = useAuth();
  const navigate = useNavigate();
  const me = useQuery({ queryKey: ['me'], queryFn: getMe });
  const [confirming, setConfirming] = useState(false);
  const [refusal, setRefusal] = useState<Refusal>();
  const remove = useMutation({
    mutationFn: deleteMe,
    onSuccess: async () => {
      toast.success(t('account.done'));
      await signOut();
      await navigate('/sign-in', { replace: true });
    },
    onError: (error) => {
      setConfirming(false);
      setRefusal({
        message: errorMessage(t, error),
        groups: error instanceof ApiError ? error.groups : [],
      });
    },
  });

  /** A Group the User deleted from the list is no longer in the way; with none left the refusal goes. */
  function dropGroup(id: string) {
    setRefusal((current) => {
      const groups = current?.groups.filter((g) => g.id !== id) ?? [];
      return groups.length > 0 && current ? { ...current, groups } : undefined;
    });
  }

  return (
    <>
      <PageHeader title={t('account.title')} />
      {me.isPending ? (
        <LoadingState rows={3} />
      ) : me.isError ? (
        <ErrorState onRetry={() => void me.refetch()} />
      ) : (
        <div className="flex max-w-[720px] flex-col gap-6">
          <section className="rounded-[12px] border bg-surface-raised p-6">
            <h2 className="text-xl font-semibold">{t('account.details')}</h2>
            <dl className="mt-3 grid gap-x-6 gap-y-2 sm:grid-cols-[max-content_1fr]">
              <dt className="text-text-muted">{t('account.displayName')}</dt>
              <dd className="min-w-0 break-words">{me.data.displayName}</dd>
              <dt className="text-text-muted">{t('account.email')}</dt>
              <dd className="min-w-0 break-words">{me.data.email}</dd>
            </dl>
          </section>
          <section className="rounded-[12px] border bg-surface-raised p-6">
            <h2 className="text-xl font-semibold">{t('account.deleteTitle')}</h2>
            <p className="mt-3">{t('account.deleteIntro')}</p>
            <Button
              variant="destructive"
              className="mt-4 h-11"
              onClick={() => {
                setRefusal(undefined);
                setConfirming(true);
              }}
            >
              {t('account.deleteButton')}
            </Button>
          </section>
          {refusal ? <RefusalNotice refusal={refusal} onGroupDeleted={dropGroup} /> : null}
        </div>
      )}
      <ConfirmDialog
        open={confirming}
        title={t('account.confirmTitle')}
        description={t('account.confirmBody')}
        confirmLabel={remove.isPending ? t('account.busy') : t('account.deleteButton')}
        destructive
        busy={remove.isPending}
        onConfirm={() => remove.mutate()}
        onCancel={() => setConfirming(false)}
      />
    </>
  );
}

/** The refusal of R-021-02: the Groups with what to do for each (hand over, delete, or wait in the Group Trash). */
function RefusalNotice({
  refusal,
  onGroupDeleted,
}: {
  refusal: Refusal;
  onGroupDeleted: (groupId: string) => void;
}) {
  const { t } = useTranslation();
  const daysText = useDaysText();
  const [deleting, setDeleting] = useState<SoleOwnerGroup>();
  return (
    <Notice tone="alert" title={refusal.message}>
      {refusal.groups.length > 0 ? (
        <>
          <p>{t('account.soleOwnerHint')}</p>
          <ul className="mt-1 flex flex-col gap-2">
            {refusal.groups.map((group) => (
              <li key={group.id} className="flex flex-wrap items-center justify-between gap-2">
                <span className="min-w-0 break-words">
                  <span className="font-semibold">{group.name}</span>
                  {group.inTrash && group.daysLeft !== null ? (
                    <span className="text-sm text-text-muted">
                      {' · '}
                      {t('account.inTrash', { days: daysText(group.daysLeft) })}
                    </span>
                  ) : null}
                </span>
                <span className="flex flex-wrap gap-2">
                  {group.inTrash ? (
                    <Button asChild variant="ghost" className="h-11">
                      <Link to="/group-trash">{t('groupTrash.title')}</Link>
                    </Button>
                  ) : (
                    <>
                      <Button asChild variant="ghost" className="h-11">
                        <Link
                          to={`/groups/${group.id}/members`}
                          aria-label={t('account.membersFor', { name: group.name })}
                        >
                          {t('account.members')}
                        </Link>
                      </Button>
                      <Button
                        variant="ghost"
                        className="h-11"
                        aria-label={t('account.deleteGroupFor', { name: group.name })}
                        onClick={() => setDeleting(group)}
                      >
                        {t('deleteGroup.submit')}
                      </Button>
                    </>
                  )}
                </span>
              </li>
            ))}
          </ul>
        </>
      ) : null}
      <DeleteGroupDialog group={deleting} onClose={() => setDeleting(undefined)} onDeleted={onGroupDeleted} />
    </Notice>
  );
}
