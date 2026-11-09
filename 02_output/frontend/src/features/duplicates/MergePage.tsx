import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link, useParams, useSearchParams } from 'react-router';
import { getContact } from '@/api/contacts';
import { EmptyState } from '@/components/EmptyState';
import { ErrorState } from '@/components/ErrorState';
import { LoadingState } from '@/components/LoadingState';
import { Notice } from '@/components/Notice';
import { PageHeader } from '@/components/PageHeader';
import { useGroupRole } from '@/features/groups/useGroupRole';
import { MergeForm } from './MergeForm';

/**
 * SC-18 / WF-18 (route `/groups/:g/merge?contact=…`): the picked Contacts side by side with a choice
 * of the value to keep. Owner and Editor only (R-026-12); fewer than two different Contacts is
 * "nothing to merge" (R-026-09).
 */
export function MergePage() {
  const { t } = useTranslation();
  const { groupId = '' } = useParams();
  const [search] = useSearchParams();
  const ids = [...new Set(search.getAll('contact'))];
  const role = useGroupRole(groupId);
  const allowed = role === 'OWNER' || role === 'EDITOR';
  const enough = ids.length >= 2;
  const contacts = useQuery({
    queryKey: ['merge', groupId, ids],
    queryFn: () => Promise.all(ids.map((id) => getContact(groupId, id))),
    enabled: allowed && enough,
    gcTime: 0,
  });
  const back = `/groups/${encodeURIComponent(groupId)}/duplicates`;

  if (role === undefined) return <LoadingState rows={4} />;
  if (!allowed) {
    return (
      <>
        <PageHeader title={t('merge.title')} />
        <Notice tone="alert" title={t('merge.noPermission')} />
      </>
    );
  }
  return (
    <>
      <Link to={back} className="mb-2 inline-block min-h-11 py-2 text-text-muted underline">
        {t('merge.backLink')}
      </Link>
      <PageHeader title={t('merge.title')} />
      {enough ? <p className="-mt-4 mb-6 text-text-muted">{t('merge.intro')}</p> : null}
      {!enough ? (
        <EmptyState
          title={t('merge.nothingTitle')}
          body={t('merge.nothingBody')}
          action={
            <Link to={back} className="inline-flex min-h-11 items-center rounded-[8px] border px-4">
              {t('merge.back')}
            </Link>
          }
        />
      ) : contacts.isPending ? (
        <LoadingState rows={6} />
      ) : contacts.isError ? (
        <ErrorState onRetry={() => void contacts.refetch()} />
      ) : (
        <MergeForm groupId={groupId} contacts={contacts.data} />
      )}
    </>
  );
}
