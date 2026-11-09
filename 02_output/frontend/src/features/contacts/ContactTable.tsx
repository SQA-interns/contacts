import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import type { ContactListItem, SortColumn } from '@/api/contacts';

export interface Sorting {
  column: SortColumn;
  dir: 'asc' | 'desc';
}

const COLUMNS: { key: SortColumn; sortable: boolean; hideOnTablet: boolean }[] = [
  { key: 'lastName', sortable: true, hideOnTablet: false },
  { key: 'firstName', sortable: true, hideOnTablet: false },
  { key: 'email', sortable: true, hideOnTablet: false },
  { key: 'company', sortable: true, hideOnTablet: false },
  { key: 'city', sortable: true, hideOnTablet: true },
  { key: 'country', sortable: true, hideOnTablet: true },
  { key: 'tags', sortable: false, hideOnTablet: false },
];

/** The country name in the language of the page; the code when the browser does not know it. */
function useCountryName() {
  const { i18n } = useTranslation();
  return (code?: string | null) => {
    if (!code) return '';
    try {
      return new Intl.DisplayNames([i18n.language], { type: 'region' }).of(code) ?? code;
    } catch {
      return code;
    }
  };
}

function cellText(row: ContactListItem, key: SortColumn, country: (code?: string | null) => string) {
  if (key === 'tags') return row.tags.join(', ');
  if (key === 'country') return country(row.country);
  return row[key] ?? '';
}

function Chips({ tags }: { tags: string[] }) {
  return (
    <span className="flex flex-wrap gap-1">
      {tags.map((tag) => (
        <span key={tag} className="rounded-full border bg-surface px-2 text-sm" title={tag}>
          {tag}
        </span>
      ))}
    </span>
  );
}

interface ContactTableProps {
  groupId: string;
  rows: ContactListItem[];
  sorting: Sorting;
  onSort: (column: SortColumn) => void;
}

/** SC-06 / WF-06: sortable columns from 768 px up, cards below; a row opens the Contact (R-007-03, 05). */
export function ContactTable({ groupId, rows, sorting, onSort }: ContactTableProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const country = useCountryName();
  const href = (row: ContactListItem) => `/groups/${encodeURIComponent(groupId)}/contacts/${row.id}`;
  const nameOf = (row: ContactListItem) =>
    row.lastName || row.firstName || row.email || t('contactList.noName');
  return (
    <>
      <table className="hidden w-full table-fixed border-collapse md:table">
        <caption className="sr-only">{t('contactList.tableCaption')}</caption>
        <thead>
          <tr className="text-left">
            {COLUMNS.map((c) => (
              <th
                key={c.key}
                scope="col"
                className={`border-b p-0 font-semibold ${c.hideOnTablet ? 'hidden lg:table-cell' : ''}`}
                aria-sort={
                  sorting.column === c.key ? (sorting.dir === 'asc' ? 'ascending' : 'descending') : undefined
                }
              >
                {c.sortable ? (
                  <button
                    type="button"
                    className="flex min-h-11 w-full items-center gap-1 p-3 text-left font-semibold"
                    onClick={() => onSort(c.key)}
                  >
                    {t(`contactList.${c.key}`)}
                    <span aria-hidden="true">
                      {sorting.column === c.key ? (sorting.dir === 'asc' ? '▲' : '▼') : ''}
                    </span>
                  </button>
                ) : (
                  <span className="block p-3">{t(`contactList.${c.key}`)}</span>
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr
              key={row.id}
              className="cursor-pointer hover:bg-surface"
              onClick={() => void navigate(href(row))}
            >
              {COLUMNS.map((c) => (
                <td
                  key={c.key}
                  className={`truncate border-b p-3 ${c.hideOnTablet ? 'hidden lg:table-cell' : ''}`}
                  title={cellText(row, c.key, country)}
                >
                  {c.key === 'lastName' ? (
                    <Link
                      to={href(row)}
                      onClick={(e) => e.stopPropagation()}
                      className="underline-offset-4 hover:underline"
                    >
                      {row.lastName || ''}
                    </Link>
                  ) : c.key === 'tags' ? (
                    <Chips tags={row.tags} />
                  ) : (
                    cellText(row, c.key, country)
                  )}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      <ul className="flex flex-col gap-3 md:hidden" aria-label={t('contactList.tableCaption')}>
        {rows.map((row) => (
          <li key={row.id} className="rounded-[12px] border bg-surface-raised p-4">
            <Link to={href(row)} className="block min-w-0 truncate font-semibold" title={nameOf(row)}>
              {[row.lastName, row.firstName].filter(Boolean).join(' ') || nameOf(row)}
            </Link>
            <dl className="mt-2 grid grid-cols-[auto_1fr] gap-x-3 gap-y-1">
              {COLUMNS.filter((c) => c.key !== 'lastName' && c.key !== 'firstName').map((c) => (
                <div key={c.key} className="contents">
                  <dt className="text-text-muted">{t(`contactList.${c.key}`)}</dt>
                  <dd className="min-w-0 truncate" title={cellText(row, c.key, country)}>
                    {c.key === 'tags' ? <Chips tags={row.tags} /> : cellText(row, c.key, country)}
                  </dd>
                </div>
              ))}
            </dl>
          </li>
        ))}
      </ul>
    </>
  );
}
