import { apiRequest } from './client';
import type { Contact } from './contacts';

/** Contract schema `VersionSummary`: one line of the Versions tab. */
export interface VersionSummary {
  number: number;
  createdAt: string;
  userName: string;
}

/** Contract schema `VersionDetail`: an earlier Version with its content. */
export interface VersionDetail extends VersionSummary {
  content: Contact;
}

/** One difference of `VersionComparison`; a value missing on one side is `null` or absent. */
export interface VersionDifference {
  kind: 'field' | 'tag' | 'attribute';
  name: string;
  versionValue?: string | null;
  currentValue?: string | null;
}

/** Contract schema `VersionComparison`. */
export interface VersionComparison {
  number: number;
  differences: VersionDifference[];
}

const base = (groupId: string, contactId: string) =>
  `/groups/${encodeURIComponent(groupId)}/contacts/${encodeURIComponent(contactId)}/versions`;

/** `GET /groups/{g}/contacts/{c}/versions` (R-022-02), newest first. */
export function listVersions(groupId: string, contactId: string): Promise<VersionSummary[]> {
  return apiRequest<VersionSummary[]>(base(groupId, contactId));
}

/** `GET /groups/{g}/contacts/{c}/versions/{n}` (R-022-03). */
export function getVersion(groupId: string, contactId: string, number: number): Promise<VersionDetail> {
  return apiRequest<VersionDetail>(`${base(groupId, contactId)}/${number}`);
}

/** `GET /groups/{g}/contacts/{c}/versions/{n}/compare` (R-022-04): the Version against the live Contact. */
export function compareVersion(
  groupId: string,
  contactId: string,
  number: number,
): Promise<VersionComparison> {
  return apiRequest<VersionComparison>(`${base(groupId, contactId)}/${number}/compare`);
}
