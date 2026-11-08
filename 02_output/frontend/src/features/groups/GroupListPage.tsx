import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { listGroups, type GroupSummary } from '@/api/groups';
import { EmptyState } from '@/components/EmptyState';
import { ErrorState } from '@/components/ErrorState';
import { LoadingState } from '@/components/LoadingState';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { CreateGroupDialog } from './CreateGroupDialog';

/** SC-05 / WF-05: the Groups of the User with the Role, and creating a Group (R-002-01..06). */
export function GroupListPage() {
  const { t } = useTranslation();
  const groups = useQuery({ queryKey: ['groups'], queryFn: listGroups });
  const [creating, setCreating] = useState(false);
  const createButton = (
    <Button className="h-11" onClick={() => setCreating(true)}>
      {t('groups.create')}
    </Button>
  );
  return (
    <>
      <PageHeader title={t('groups.title')} action={createButton} />
      <p className="-mt-4 mb-6 text-text-muted">{t('groups.intro')}</p>
      {groups.isPending ? (
        <LoadingState rows={3} />
      ) : groups.isError ? (
        <ErrorState onRetry={() => void groups.refetch()} />
      ) : groups.data.length === 0 ? (
        <EmptyState title={t('groups.emptyTitle')} body={t('groups.emptyBody')} action={createButton} />
      ) : (
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {groups.data.map((group) => (
            <GroupTile key={group.id} group={group} />
          ))}
        </ul>
      )}
      <CreateGroupDialog open={creating} onOpenChange={setCreating} />
    </>
  );
}

function GroupTile({ group }: { group: GroupSummary }) {
  const { t } = useTranslation();
  return (
    <li>
      <article className="flex flex-col gap-3 rounded-[12px] border bg-surface-raised p-4">
        <h2 className="text-xl font-semibold">
          <Link to={`/groups/${group.id}/contacts`} className="underline-offset-4 hover:underline">
            {group.name}
          </Link>
        </h2>
        <span className="self-start rounded-[8px] border px-2 py-0.5 text-sm">{t(`role.${group.role}`)}</span>
      </article>
    </li>
  );
}
