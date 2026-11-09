import { listContacts, type ContactFilters } from '@/api/contacts';
import type { ExportFilter, ExportSelection } from '@/api/exports';

/** The part of the Selection the export needs (see `useSelection`). */
interface ChosenContacts {
  allResults: boolean;
  ticked: string[];
  unticked: string[];
}

/** The search text and the active filters as the contract's `ContactFilter`; empty ones are left out. */
export function filterBody(q: string, f: ContactFilters): ExportFilter {
  const body: ExportFilter = {};
  if (q.trim()) body.q = q.trim();
  if (f.tags.length > 0) {
    body.tags = f.tags;
    body.tagMode = f.tagMode;
  }
  if (f.withoutTags.length > 0) body.withoutTags = f.withoutTags;
  if (f.country) body.country = f.country;
  if (f.company) body.company = f.company;
  if (f.attrKey && f.attrExists) {
    body.attrKey = f.attrKey;
    body.attrExists = true;
  } else if (f.attrKey && f.attrValue.trim()) {
    body.attrKey = f.attrKey;
    body.attrValue = f.attrValue.trim();
  }
  return body;
}

/**
 * What the export sends. Ticked Contacts go as ids; "all results" goes as the filter, so 10,000 ids do not travel
 * (R-010-06). The contract has no way to name Contacts taken out of "all results", so then the results are listed
 * page by page and the unticked ones are left out here [ASSUMPTION, see D-55].
 */
export async function resolveSelection(
  groupId: string,
  selection: ChosenContacts,
  q: string,
  filters: ContactFilters,
): Promise<ExportSelection> {
  if (!selection.allResults) return { contactIds: selection.ticked };
  if (selection.unticked.length === 0) return { filter: filterBody(q, filters) };
  const out = new Set(selection.unticked);
  const ids: string[] = [];
  for (let page = 1, pages = 1; page <= pages; page++) {
    const result = await listContacts(groupId, { page, sort: 'lastName', dir: 'asc', q, filters });
    pages = Math.ceil(result.total / result.pageSize);
    for (const row of result.items) if (!out.has(row.id)) ids.push(row.id);
  }
  return { contactIds: ids };
}
