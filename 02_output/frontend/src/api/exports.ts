import { apiRequest } from './client';

/** Contract schema `ContactFilter` as the body of an export. */
export interface ExportFilter {
  q?: string;
  tags?: string[];
  tagMode?: 'all' | 'any';
  withoutTags?: string[];
  country?: string;
  company?: string;
  attrKey?: string;
  attrValue?: string;
  attrExists?: boolean;
}

/** Contract schema `ExportRequest.selection`: the ids, or the filter of "all results". */
export type ExportSelection = { contactIds: string[] } | { filter: ExportFilter };

export interface ExportedFile {
  blob: Blob;
  fileName: string;
}

/** The file name of `Content-Disposition`, plain or RFC 5987 form; `export.xlsx` when there is none. */
export function fileNameOf(disposition: string | null): string {
  const extended = /filename\*=(?:UTF-8|utf-8)''([^;]+)/.exec(disposition ?? '');
  if (extended?.[1]) return decodeURIComponent(extended[1].trim());
  return /filename="([^"]+)"/.exec(disposition ?? '')?.[1] ?? 'export.xlsx';
}

/** `POST /groups/{g}/exports` (R-011-02): the Excel file of the Selection. */
export async function exportContacts(
  groupId: string,
  selection: ExportSelection,
  lang: 'sl' | 'en',
): Promise<ExportedFile> {
  const response = await apiRequest<Response>(`/groups/${encodeURIComponent(groupId)}/exports`, {
    method: 'POST',
    body: { selection, lang },
    raw: true,
  });
  return { blob: await response.blob(), fileName: fileNameOf(response.headers.get('Content-Disposition')) };
}
