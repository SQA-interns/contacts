import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { CONTACT_FIELDS, type ContactField } from '@/api/contacts';
import { errorMessage } from '@/api/errors';
import {
  compareVersion,
  getVersion,
  listVersions,
  type VersionDifference,
  type VersionSummary,
} from '@/api/versions';
import { EmptyState } from '@/components/EmptyState';
import { ErrorState } from '@/components/ErrorState';
import { LoadingState } from '@/components/LoadingState';
import { ResponsiveTable, type Column } from '@/components/ResponsiveTable';
import { Button } from '@/components/ui/button';
import { Fields, TagsAndAttributes } from '@/features/contacts/ContactView';
import { formatDateTime } from '@/i18n/format';
import type { Language } from '@/i18n';

type View = { kind: 'list' } | { kind: 'open'; number: number } | { kind: 'compare'; number: number };

interface Ids {
  groupId: string;
  contactId: string;
}

const isField = (name: string): name is ContactField => (CONTACT_FIELDS as readonly string[]).includes(name);

/**
 * Tab "Različice" of a Contact for Owner and Editor (R-022-02..04, SC-07, WF-07): the list, one
 * earlier Version, or the comparison of a Version with the current one. No control changes or
 * deletes a Version (R-022-05).
 */
export function VersionsTab({ groupId, contactId }: Ids) {
  const { t } = useTranslation();
  const [view, setView] = useState<View>({ kind: 'list' });
  const query = useQuery({
    queryKey: ['contacts', groupId, contactId, 'versions'],
    queryFn: () => listVersions(groupId, contactId),
  });

  if (query.isPending) return <LoadingState rows={5} />;
  if (query.isError) {
    return <ErrorState message={errorMessage(t, query.error)} onRetry={() => void query.refetch()} />;
  }
  const versions = query.data;
  if (versions.length === 0) return <EmptyState title={t('versions.empty')} />;
  const current = versions[0]?.number ?? 0;
  const back = () => setView({ kind: 'list' });

  if (view.kind === 'open') {
    return <OpenVersion groupId={groupId} contactId={contactId} number={view.number} onBack={back} />;
  }
  if (view.kind === 'compare') {
    return (
      <CompareVersion
        groupId={groupId}
        contactId={contactId}
        number={view.number}
        current={current}
        onBack={back}
      />
    );
  }
  return <VersionList versions={versions} current={current} onChoose={setView} />;
}

function VersionList({
  versions,
  current,
  onChoose,
}: {
  versions: VersionSummary[];
  current: number;
  onChoose: (view: View) => void;
}) {
  const { t, i18n } = useTranslation();
  const columns: Column<VersionSummary>[] = [
    {
      key: 'number',
      header: t('versions.number'),
      cell: (v) => (
        <>
          {t('versions.short', { number: v.number })}
          {v.number === current ? (
            <span className="ml-2 rounded-[8px] border px-2 py-0.5 text-sm">{t('versions.current')}</span>
          ) : null}
        </>
      ),
    },
    {
      key: 'time',
      header: t('versions.time'),
      cell: (v) => formatDateTime(v.createdAt, i18n.language as Language),
    },
    { key: 'user', header: t('versions.user'), cell: (v) => v.userName },
    {
      key: 'actions',
      header: t('versions.actions'),
      cell: (v) => (
        <div className="flex flex-wrap gap-2">
          <Button variant="ghost" onClick={() => onChoose({ kind: 'open', number: v.number })}>
            {t('versions.open')}
          </Button>
          {v.number === current ? null : (
            <Button variant="ghost" onClick={() => onChoose({ kind: 'compare', number: v.number })}>
              {t('versions.compare')}
            </Button>
          )}
        </div>
      ),
    },
  ];
  return (
    <section className="rounded-[12px] border bg-surface-raised p-4">
      <h2 className="mb-2 text-xl font-semibold">{t('versions.title')}</h2>
      <ResponsiveTable
        columns={columns}
        rows={versions}
        rowKey={(v) => String(v.number)}
        caption={t('versions.caption')}
      />
    </section>
  );
}

function BackButton({ onBack }: { onBack: () => void }) {
  const { t } = useTranslation();
  return (
    <Button variant="secondary" onClick={onBack}>
      {t('versions.back')}
    </Button>
  );
}

function OpenVersion({ groupId, contactId, number, onBack }: Ids & { number: number; onBack: () => void }) {
  const { t } = useTranslation();
  const query = useQuery({
    queryKey: ['contacts', groupId, contactId, 'versions', number],
    queryFn: () => getVersion(groupId, contactId, number),
  });
  if (query.isPending) return <LoadingState rows={5} />;
  if (query.isError) {
    return <ErrorState message={errorMessage(t, query.error)} onRetry={() => void query.refetch()} />;
  }
  return (
    <div className="grid gap-4 lg:grid-cols-[2fr_1fr]">
      <section className="rounded-[12px] border bg-surface-raised p-4">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-xl font-semibold">{t('versions.versionTitle', { number })}</h2>
          <BackButton onBack={onBack} />
        </div>
        <Fields contact={query.data.content} />
      </section>
      <div className="flex flex-col gap-4">
        <TagsAndAttributes contact={query.data.content} />
      </div>
    </div>
  );
}

function CompareVersion({
  groupId,
  contactId,
  number,
  current,
  onBack,
}: Ids & { number: number; current: number; onBack: () => void }) {
  const { t } = useTranslation();
  const query = useQuery({
    queryKey: ['contacts', groupId, contactId, 'versions', number, 'compare'],
    queryFn: () => compareVersion(groupId, contactId, number),
  });
  if (query.isPending) return <LoadingState rows={5} />;
  if (query.isError) {
    return <ErrorState message={errorMessage(t, query.error)} onRetry={() => void query.refetch()} />;
  }
  const { differences } = query.data;
  return (
    <section className="rounded-[12px] border bg-surface-raised p-4">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-xl font-semibold">{t('versions.compareTitle', { number, current })}</h2>
        <BackButton onBack={onBack} />
      </div>
      {differences.length === 0 ? (
        <p>{t('versions.noDifferences')}</p>
      ) : (
        <DifferenceTable differences={differences} number={number} />
      )}
    </section>
  );
}

function DifferenceTable({ differences, number }: { differences: VersionDifference[]; number: number }) {
  const { t } = useTranslation();
  const empty = '—';
  const label = (d: VersionDifference) =>
    d.kind === 'field' && isField(d.name)
      ? t(`contacts.fields.${d.name}`)
      : d.kind === 'field'
        ? d.name
        : `${t(`versions.${d.kind}`)}: ${d.name}`;
  const isTag = (d: VersionDifference) => d.kind === 'tag';
  const columns: Column<VersionDifference>[] = [
    { key: 'name', header: t('versions.field'), cell: label },
    {
      key: 'version',
      header: t('versions.versionColumn', { number }),
      cell: (d) => d.versionValue ?? empty,
    },
    {
      key: 'current',
      header: t('versions.currentColumn'),
      cell: (d) => {
        if (isTag(d)) {
          if (d.currentValue == null) return `${empty} ${t('versions.removed')}`;
          return `${d.currentValue} ${t('versions.added')}`;
        }
        return d.currentValue ?? empty;
      },
    },
  ];
  return (
    <ResponsiveTable
      columns={columns}
      rows={differences}
      rowKey={(d) => `${d.kind}:${d.name}`}
      caption={t('versions.compareCaption', { number })}
    />
  );
}
