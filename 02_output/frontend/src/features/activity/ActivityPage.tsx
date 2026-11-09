import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { CONTACT_FIELDS, type ContactField } from '@/api/contacts';
import { errorMessage } from '@/api/errors';
import { getGroupActivity, type ActivityFilters, type HistoryAction, type HistoryEntry } from '@/api/history';
import { listMembers } from '@/api/members';
import { EmptyState } from '@/components/EmptyState';
import { ErrorState } from '@/components/ErrorState';
import { LoadingState } from '@/components/LoadingState';
import { Notice } from '@/components/Notice';
import { PageHeader } from '@/components/PageHeader';
import { ResponsiveTable, type Column } from '@/components/ResponsiveTable';
import { Button } from '@/components/ui/button';
import { useGroupRole } from '@/features/groups/useGroupRole';
import type { Language } from '@/i18n';
import { formatDateTime } from '@/i18n/format';

const ACTIONS: HistoryAction[] = [
  'CONTACT_CREATED',
  'CONTACT_EDITED',
  'CONTACT_DELETED',
  'CONTACT_DELETED_FOREVER',
  'CONTACT_RESTORED',
  'TAG_ADDED',
  'TAG_REMOVED',
  'TAG_RENAMED',
  'TAG_DELETED',
  'ATTRIBUTE_ADDED',
  'ATTRIBUTE_CHANGED',
  'ATTRIBUTE_REMOVED',
  'CONTACTS_MERGED',
  'CONTACTS_EXPORTED',
  'MEMBER_JOINED',
  'ROLE_CHANGED',
  'MEMBER_REMOVED',
  'INVITATION_CREATED',
  'INVITATION_CANCELLED',
  'VERSION_RESTORED',
];

const NO_FILTERS: ActivityFilters = { userId: '', action: '', from: '', to: '' };
const isField = (name: string): name is ContactField => (CONTACT_FIELDS as readonly string[]).includes(name);
const selectClass = 'h-11 w-full rounded-md border border-input bg-transparent px-3';

type Translate = ReturnType<typeof useTranslation>['t'];

/** The "Details" text of an entry (ADR-08 shapes); never an edit control. */
function detailsText(entry: HistoryEntry, t: Translate): string {
  const d = entry.details ?? {};
  const role = (r?: string) => (r ? t(`role.${r}`, { defaultValue: r }) : '');
  const field = (name: string) => (isField(name) ? t(`contacts.fields.${name}`) : name);
  const parts: string[] = [];
  if (typeof d.restored === 'number') parts.push(t('history.restoredVersion', { number: d.restored }));
  if (d.changes) {
    for (const [name, c] of Object.entries(d.changes)) {
      parts.push(`${field(name)}: ${c.old ?? '—'} → ${c.new ?? '—'}`);
    }
  } else if (d.values) {
    const filled = Object.entries(d.values).filter(([, v]) => v);
    if (filled.length > 0 && filled.every(([, v]) => v === 'removed')) parts.push(t('activity.removed'));
    else parts.push(...filled.map(([name, v]) => `${field(name)}: ${v}`));
  }
  if (d.count !== undefined) parts.push(t('activity.count', { count: d.count }));
  if (entry.action === 'ROLE_CHANGED') parts.push(`${d.member}: ${role(d.old)} → ${role(d.new)}`);
  else if (entry.action === 'MEMBER_REMOVED') parts.push(`${d.member} · ${role(d.role)}`);
  else if (entry.action === 'INVITATION_CREATED') parts.push(`${d.email} · ${role(d.role)}`);
  else if (entry.action === 'INVITATION_CANCELLED') parts.push(d.email ?? '');
  else if (entry.action === 'MEMBER_JOINED') parts.push(role(d.role));
  else if (d.key) parts.push(`${d.key}: ${d.old ?? '—'} → ${d.new ?? '—'}`.replace(/^(.*): — → —$/, '$1'));
  else if (d.name) parts.push(d.old ? `${d.old} → ${d.new}` : d.name);
  return parts.filter(Boolean).join(' · ');
}

/** SC-13 / WF-13: every History entry of the Group with filters (R-017-01..07). Owner and Editor only. */
export function ActivityPage() {
  const { t, i18n } = useTranslation();
  const { groupId = '' } = useParams();
  const role = useGroupRole(groupId);
  const allowed = role === 'OWNER' || role === 'EDITOR';
  const [filters, setFilters] = useState<ActivityFilters>(NO_FILTERS);
  const [page, setPage] = useState(1);
  const inverted = filters.from !== '' && filters.to !== '' && filters.from > filters.to;
  useEffect(() => setPage(1), [filters]);
  const members = useQuery({
    queryKey: ['members', groupId],
    queryFn: () => listMembers(groupId),
    enabled: allowed,
  });
  const activity = useQuery({
    queryKey: ['activity', groupId, filters, page],
    queryFn: () => getGroupActivity(groupId, filters, page),
    enabled: allowed && !inverted,
  });
  const set = (change: Partial<ActivityFilters>) => setFilters((old) => ({ ...old, ...change }));

  if (role === undefined) return <LoadingState rows={4} />;
  if (!allowed) {
    return (
      <>
        <PageHeader title={t('activity.title')} />
        <Notice tone="alert" title={t('activity.noPermission')} />
      </>
    );
  }

  const columns: Column<HistoryEntry>[] = [
    {
      key: 'time',
      header: t('activity.time'),
      cell: (e) => formatDateTime(e.occurredAt, i18n.language as Language),
    },
    {
      key: 'user',
      header: t('activity.user'),
      cell: (e) => [e.userName, e.userEmail].filter(Boolean).join(' · '),
    },
    { key: 'action', header: t('activity.action'), cell: (e) => t(`activity.actions.${e.action}`) },
    {
      key: 'contact',
      header: t('activity.contact'),
      cell: (e) =>
        e.contactId ? (
          <Link
            className="underline-offset-4 hover:underline"
            to={`/groups/${encodeURIComponent(groupId)}/contacts/${encodeURIComponent(e.contactId)}`}
          >
            {t('activity.openContact')}
          </Link>
        ) : (
          t('activity.none')
        ),
    },
    { key: 'details', header: t('activity.details'), cell: (e) => detailsText(e, t) },
  ];
  const clear = (
    <Button variant="secondary" onClick={() => setFilters(NO_FILTERS)}>
      {t('activity.clear')}
    </Button>
  );
  const data = activity.data;
  const pages = data ? Math.max(1, Math.ceil(data.total / data.pageSize)) : 1;

  return (
    <>
      <PageHeader title={t('activity.title')} />
      <p className="-mt-4 mb-6 text-text-muted">{t('activity.intro')}</p>
      <form
        aria-label={t('activity.filters')}
        className="mb-4 grid grid-cols-[repeat(auto-fit,minmax(180px,1fr))] items-start gap-4"
        onSubmit={(e) => e.preventDefault()}
      >
        <div className="flex flex-col gap-1">
          <label htmlFor="activity-user">{t('activity.user')}</label>
          <select
            id="activity-user"
            className={selectClass}
            value={filters.userId}
            onChange={(e) => set({ userId: e.target.value })}
          >
            <option value="">{t('activity.allUsers')}</option>
            {(members.data ?? []).map((m) => (
              <option key={m.userId} value={m.userId}>
                {m.displayName}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="activity-action">{t('activity.action')}</label>
          <select
            id="activity-action"
            className={selectClass}
            value={filters.action}
            onChange={(e) => set({ action: e.target.value as HistoryAction | '' })}
          >
            <option value="">{t('activity.allActions')}</option>
            {ACTIONS.map((a) => (
              <option key={a} value={a}>
                {t(`activity.actions.${a}`)}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="activity-from">{t('activity.from')}</label>
          <input
            id="activity-from"
            type="date"
            className={selectClass}
            value={filters.from}
            max={filters.to || undefined}
            aria-invalid={inverted}
            aria-describedby={inverted ? 'activity-range' : undefined}
            onChange={(e) => set({ from: e.target.value })}
          />
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="activity-to">{t('activity.to')}</label>
          <input
            id="activity-to"
            type="date"
            className={selectClass}
            value={filters.to}
            aria-invalid={inverted}
            aria-describedby={inverted ? 'activity-range' : undefined}
            onChange={(e) => set({ to: e.target.value })}
          />
        </div>
        <div className="flex flex-col justify-end sm:col-span-full sm:items-end">{clear}</div>
      </form>
      {inverted ? (
        <p id="activity-range" role="alert" className="mb-4 font-semibold text-danger">
          {t('activity.range')}
        </p>
      ) : activity.isPending ? (
        <LoadingState rows={5} />
      ) : activity.isError ? (
        <ErrorState message={errorMessage(t, activity.error)} onRetry={() => void activity.refetch()} />
      ) : !data || data.items.length === 0 ? (
        <EmptyState title={t('activity.empty')} body={t('activity.emptyBody')} action={clear} />
      ) : (
        <>
          <ResponsiveTable
            columns={columns}
            rows={data?.items ?? []}
            rowKey={(e) => e.id}
            caption={t('activity.title')}
          />
          <nav
            aria-label={t('history.pages')}
            className="mt-4 flex flex-wrap items-center justify-between gap-2"
          >
            <span>{t('history.page', { page, pages })}</span>
            <div className="flex gap-2">
              <Button variant="secondary" disabled={page <= 1} onClick={() => setPage(page - 1)}>
                {t('history.previous')}
              </Button>
              <Button variant="secondary" disabled={page >= pages} onClick={() => setPage(page + 1)}>
                {t('history.next')}
              </Button>
            </div>
          </nav>
        </>
      )}
    </>
  );
}
