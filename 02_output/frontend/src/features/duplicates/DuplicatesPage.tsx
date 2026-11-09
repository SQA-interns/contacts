import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';
import { findDuplicates, type DuplicateSet } from '@/api/duplicates';
import type { ContactListItem } from '@/api/contacts';
import { EmptyState } from '@/components/EmptyState';
import { ErrorState } from '@/components/ErrorState';
import { LoadingState } from '@/components/LoadingState';
import { Notice } from '@/components/Notice';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { useGroupRole } from '@/features/groups/useGroupRole';
import type { Language } from '@/i18n';
import { formatDate } from '@/i18n/format';
import { DismissButton } from './DismissButton';
import { useReasonText } from './useReasonText';

function nameOf(contact: ContactListItem, fallback: string): string {
  const name = [contact.firstName, contact.lastName].filter(Boolean).join(' ');
  return name || contact.company || contact.email || fallback;
}

/** SC-17 / WF-17: the Duplicate sets of the Group with mark, reason and Contacts; it changes nothing (R-024-01, R-024-06); "Ni podvojeno" dismisses a set (R-025-01). */
export function DuplicatesPage() {
  const { t } = useTranslation();
  const { groupId = '' } = useParams();
  const role = useGroupRole(groupId);
  const allowed = role === 'OWNER' || role === 'EDITOR';
  const sets = useQuery({
    queryKey: ['duplicates', groupId],
    queryFn: () => findDuplicates(groupId),
    enabled: allowed,
  });

  if (role === undefined) return <LoadingState rows={4} />;
  if (!allowed) {
    return (
      <>
        <PageHeader title={t('duplicates.title')} />
        <Notice tone="alert" title={t('duplicates.noPermission')} />
      </>
    );
  }
  return (
    <>
      <PageHeader
        title={t('duplicates.title')}
        action={
          <Button
            variant="secondary"
            className="h-11"
            disabled={sets.isFetching}
            onClick={() => void sets.refetch()}
          >
            {t('duplicates.searchAgain')}
          </Button>
        }
      />
      <p className="-mt-4 mb-6 text-text-muted">{t('duplicates.intro')}</p>
      {sets.isPending ? (
        <>
          <LoadingState rows={4} />
          <p className="mt-3 text-text-muted">{t('duplicates.searching')}</p>
        </>
      ) : sets.isError ? (
        <ErrorState onRetry={() => void sets.refetch()} />
      ) : sets.data.length === 0 ? (
        <EmptyState title={t('duplicates.emptyTitle')} body={t('duplicates.emptyBody')} />
      ) : (
        <div className="flex flex-col gap-4">
          {sets.data.map((set) => (
            <DuplicateSetCard key={set.key} groupId={groupId} set={set} />
          ))}
        </div>
      )}
    </>
  );
}

function DuplicateSetCard({ groupId, set }: { groupId: string; set: DuplicateSet }) {
  const { t, i18n } = useTranslation();
  const reason = useReasonText();
  const language = i18n.language as Language;
  return (
    <article className="flex flex-col gap-3 rounded-[12px] border bg-surface-raised p-4">
      <div className="flex flex-wrap items-center gap-3">
        <span
          className={`inline-flex min-h-6 items-center rounded-full border px-2 text-xs font-semibold ${set.mark === 'certain' ? 'bg-primary text-primary-foreground' : 'bg-surface'}`}
        >
          {t(`duplicates.mark.${set.mark}`)}
        </span>
        <h2 className="min-w-0 font-semibold break-words">{reason(set)}</h2>
      </div>
      <ul className="flex flex-col gap-2">
        {set.contacts.map((contact) => (
          <li key={contact.id} className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
            <span className="min-w-0">
              <span className="font-semibold break-words">{nameOf(contact, t('contacts.untitled'))}</span>
              <br />
              <span className="text-text-muted break-words">
                {[contact.email, contact.company, contact.city].filter(Boolean).join(' · ')}
              </span>
            </span>
            {contact.createdAt ? (
              <span className="text-sm text-text-muted">
                {t('duplicates.createdOn', { date: formatDate(contact.createdAt, language) })}
              </span>
            ) : null}
          </li>
        ))}
      </ul>
      <div className="flex flex-wrap justify-end gap-2">
        <DismissButton groupId={groupId} set={set} />
      </div>
    </article>
  );
}
