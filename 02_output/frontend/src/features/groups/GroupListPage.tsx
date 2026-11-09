import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { MoreHorizontal } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { listGroups, type GroupSummary } from '@/api/groups';
import { EmptyState } from '@/components/EmptyState';
import { ErrorState } from '@/components/ErrorState';
import { LoadingState } from '@/components/LoadingState';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { DropdownMenuItem } from '@/components/ui/dropdown-menu';
import { CreateGroupDialog } from './CreateGroupDialog';
import { GroupOptionsMenu } from './GroupOptionsMenu';

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
      <PageHeader
        title={t('groups.title')}
        action={
          <div className="flex flex-wrap items-center gap-2">
            <Button asChild variant="ghost" className="h-11">
              <Link to="/group-trash">{t('groups.trashLink')}</Link>
            </Button>
            {createButton}
          </div>
        }
      />
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
        <div className="flex items-start justify-between gap-2">
          <h2 className="min-w-0 text-xl font-semibold break-words">
            <Link to={`/groups/${group.id}/contacts`} className="underline-offset-4 hover:underline">
              {group.name}
            </Link>
          </h2>
          {group.role === 'OWNER' ? (
            <GroupOptionsMenu
              group={group}
              trigger={
                <Button
                  variant="ghost"
                  className="size-11 shrink-0"
                  aria-label={t('groups.optionsFor', { name: group.name })}
                >
                  <MoreHorizontal aria-hidden="true" />
                </Button>
              }
            >
              <DropdownMenuItem asChild className="min-h-11">
                <Link to={`/groups/${group.id}/contacts`}>{t('groups.open')}</Link>
              </DropdownMenuItem>
            </GroupOptionsMenu>
          ) : null}
        </div>
        <span className="self-start rounded-[8px] border px-2 py-0.5 text-sm">{t(`role.${group.role}`)}</span>
      </article>
    </li>
  );
}
