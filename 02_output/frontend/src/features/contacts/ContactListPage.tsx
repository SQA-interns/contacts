import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link, useParams, useSearchParams } from 'react-router';
import { listContacts, type SortColumn } from '@/api/contacts';
import { EmptyState } from '@/components/EmptyState';
import { ErrorState } from '@/components/ErrorState';
import { LoadingState } from '@/components/LoadingState';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { useGroupRole } from '@/features/groups/useGroupRole';
import { ContactTable, type Sorting } from './ContactTable';
import { SearchBox } from './SearchBox';

const COLUMNS: SortColumn[] = ['lastName', 'firstName', 'email', 'company', 'city', 'country'];

/** Slovene has four plural forms; the catalog keeps one text for each (`one`, `two`, `few`, `other`). */
function useTotalText() {
  const { t, i18n } = useTranslation();
  return (count: number) => {
    const form = new Intl.PluralRules(i18n.language).select(count);
    const key = form === 'one' || form === 'two' || form === 'few' ? form : 'other';
    return t(`contactList.total.${key}`, { count });
  };
}

/** The page numbers the pager offers: the first, the last and the ones around the current. */
function pageNumbers(current: number, pages: number): number[] {
  const wanted = new Set([1, pages, current - 1, current, current + 1]);
  return [...wanted].filter((n) => n >= 1 && n <= pages).sort((a, b) => a - b);
}

/** SC-06 / WF-06: the Contacts of the Group, sorted and paged (US-007) and searched (US-008). */
export function ContactListPage() {
  const { t } = useTranslation();
  const { groupId = '' } = useParams();
  const role = useGroupRole(groupId);
  const [params, setParams] = useSearchParams();
  const totalText = useTotalText();
  const sort = (params.get('sort') as SortColumn | null) ?? 'lastName';
  const sorting: Sorting = {
    column: COLUMNS.includes(sort) ? sort : 'lastName',
    dir: params.get('dir') === 'desc' ? 'desc' : 'asc',
  };
  const page = Math.max(1, Number.parseInt(params.get('page') ?? '1', 10) || 1);
  const q = params.get('q')?.trim() ?? '';
  const contacts = useQuery({
    queryKey: ['contacts', groupId, page, sorting.column, sorting.dir, q],
    queryFn: () => listContacts(groupId, { page, sort: sorting.column, dir: sorting.dir, q }),
    placeholderData: (previous) => previous,
  });
  const canAdd = role === 'OWNER' || role === 'EDITOR';
  const addLink = (
    <Button asChild>
      <Link to={`/groups/${encodeURIComponent(groupId)}/contacts/new`}>{t('contactList.add')}</Link>
    </Button>
  );

  const choosePage = (next: number) =>
    setParams((old) => {
      const copy = new URLSearchParams(old);
      copy.set('page', String(next));
      return copy;
    });
  /** A new search text shows page 1 of its result (R-008-03); an empty one is no search. */
  const chooseSearch = (text: string) =>
    setParams((old) => {
      const copy = new URLSearchParams(old);
      if (text) copy.set('q', text);
      else copy.delete('q');
      copy.set('page', '1');
      return copy;
    });
  const chooseSort = (column: SortColumn) =>
    setParams((old) => {
      const copy = new URLSearchParams(old);
      copy.set('sort', column);
      copy.set('dir', sorting.column === column && sorting.dir === 'asc' ? 'desc' : 'asc');
      copy.set('page', '1');
      return copy;
    });

  const data = contacts.data;
  const pages = data ? Math.max(1, Math.ceil(data.total / data.pageSize)) : 1;
  return (
    <>
      <PageHeader
        title={t('contactList.title')}
        action={canAdd && data && data.total > 0 ? addLink : undefined}
      />
      <SearchBox value={q} onSearch={chooseSearch} />
      {data ? <p className="-mt-4 mb-6 text-text-muted">{totalText(data.total)}</p> : null}
      {contacts.isPending ? (
        <LoadingState rows={6} />
      ) : contacts.isError ? (
        <ErrorState onRetry={() => void contacts.refetch()} />
      ) : data && data.total === 0 && q ? (
        <EmptyState
          title={t('contactList.noMatchTitle')}
          body={t('contactList.noMatchBody')}
          action={<Button onClick={() => chooseSearch('')}>{t('contactList.clearFilters')}</Button>}
        />
      ) : data && data.total === 0 ? (
        <EmptyState
          title={t('contactList.emptyTitle')}
          body={t('contactList.emptyBody')}
          action={canAdd ? addLink : undefined}
        />
      ) : data ? (
        <>
          <ContactTable groupId={groupId} rows={data.items} sorting={sorting} onSort={chooseSort} />
          <nav
            className="mt-6 flex flex-wrap items-center justify-between gap-3"
            aria-label={t('contactList.pagerLabel')}
          >
            <span>
              {t('contactList.pageSummary', {
                position: t('contactList.pageOf', { page, pages }),
                total: totalText(data.total),
              })}
            </span>
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" disabled={page <= 1} onClick={() => choosePage(page - 1)}>
                {t('contactList.previous')}
              </Button>
              {pageNumbers(page, pages).map((n) => (
                <Button
                  key={n}
                  variant="outline"
                  aria-current={n === page ? 'page' : undefined}
                  onClick={() => choosePage(n)}
                >
                  {n}
                </Button>
              ))}
              <Button variant="outline" disabled={page >= pages} onClick={() => choosePage(page + 1)}>
                {t('contactList.next')}
              </Button>
            </div>
          </nav>
        </>
      ) : null}
    </>
  );
}
