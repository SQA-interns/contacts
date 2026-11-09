import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { CONTACT_FIELDS, type ContactField } from '@/api/contacts';
import { errorMessage } from '@/api/errors';
import { getContactHistory, type HistoryEntry } from '@/api/history';
import { EmptyState } from '@/components/EmptyState';
import { ErrorState } from '@/components/ErrorState';
import { LoadingState } from '@/components/LoadingState';
import { Button } from '@/components/ui/button';
import { formatDateTime } from '@/i18n/format';
import type { Language } from '@/i18n';

const isField = (name: string): name is ContactField => (CONTACT_FIELDS as readonly string[]).includes(name);

/** The lines under an entry: "Field: old → new" for an edit, "Field: value" for a creation. */
function useLines(entry: HistoryEntry): string[] {
  const { t } = useTranslation();
  const label = (name: string) => (isField(name) ? t(`contacts.fields.${name}`) : name);
  const empty = '—';
  const changes = Object.entries(entry.details?.changes ?? {}).map(
    ([name, change]) => `${label(name)}: ${change.old ?? empty} → ${change.new ?? empty}`,
  );
  const values = Object.entries(entry.details?.values ?? {})
    .filter(([, value]) => value)
    .map(([name, value]) => `${label(name)}: ${value}`);
  return [...changes, ...values];
}

function Entry({ entry }: { entry: HistoryEntry }) {
  const { t, i18n } = useTranslation();
  const lines = useLines(entry);
  const who = [entry.userName, entry.userEmail].filter(Boolean).join(' · ');
  return (
    <li className="border-b py-3 last:border-b-0">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <span className="font-semibold">{t(`history.actions.${entry.action}`)}</span>
        <span className="text-sm text-text-muted">
          {formatDateTime(entry.occurredAt, i18n.language as Language)}
        </span>
      </div>
      <p className="text-sm break-words">{who}</p>
      {lines.map((line, i) => (
        <p key={i} className="text-sm break-words whitespace-pre-wrap">
          {line}
        </p>
      ))}
    </li>
  );
}

/** Tab "Zgodovina" of a Contact for Owner and Editor (R-016-03, SC-07). No control edits an entry. */
export function ContactHistoryTab({ groupId, contactId }: { groupId: string; contactId: string }) {
  const { t } = useTranslation();
  const [page, setPage] = useState(1);
  const query = useQuery({
    queryKey: ['contacts', groupId, contactId, 'history', page],
    queryFn: () => getContactHistory(groupId, contactId, page),
  });

  if (query.isPending) return <LoadingState rows={5} />;
  if (query.isError) {
    return <ErrorState message={errorMessage(t, query.error)} onRetry={() => void query.refetch()} />;
  }
  const { items, total, pageSize } = query.data;
  if (items.length === 0 && page === 1) return <EmptyState title={t('history.empty')} />;
  const pages = Math.max(1, Math.ceil(total / pageSize));

  return (
    <section className="rounded-[12px] border bg-surface-raised p-4">
      <h2 className="mb-2 text-xl font-semibold">{t('history.title')}</h2>
      <ul>
        {items.map((entry) => (
          <Entry key={entry.id} entry={entry} />
        ))}
      </ul>
      <nav aria-label={t('history.pages')} className="mt-4 flex flex-wrap items-center justify-between gap-2">
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
    </section>
  );
}
