import type { ReactNode } from 'react';

export interface Column<T> {
  key: string;
  header: string;
  cell: (row: T) => ReactNode;
}

interface ResponsiveTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  caption: string;
}

/** Table from 768 px up, stacked cards below (UI-16); long text truncates with the full value in `title`. */
export function ResponsiveTable<T>({ columns, rows, rowKey, caption }: ResponsiveTableProps<T>) {
  return (
    <>
      <table className="hidden w-full table-fixed border-collapse md:table">
        <caption className="sr-only">{caption}</caption>
        <thead>
          <tr className="text-left">
            {columns.map((c) => (
              <th key={c.key} scope="col" className="border-b p-3 font-semibold">
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={rowKey(row)}>
              {columns.map((c) => (
                <td key={c.key} className="truncate border-b p-3">
                  {c.cell(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      <ul className="flex flex-col gap-3 md:hidden" aria-label={caption}>
        {rows.map((row) => (
          <li key={rowKey(row)} className="rounded-[12px] border bg-surface-raised p-4">
            <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1">
              {columns.map((c) => (
                <div key={c.key} className="contents">
                  <dt className="font-semibold">{c.header}</dt>
                  <dd className="min-w-0 truncate">{c.cell(row)}</dd>
                </div>
              ))}
            </dl>
          </li>
        ))}
      </ul>
    </>
  );
}
