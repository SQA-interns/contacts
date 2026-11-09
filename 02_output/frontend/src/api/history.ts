import { apiRequest } from './client';

/** Contract schema `HistoryAction`. */
export type HistoryAction =
  | 'CONTACT_CREATED'
  | 'CONTACT_EDITED'
  | 'CONTACT_DELETED'
  | 'CONTACT_DELETED_FOREVER'
  | 'CONTACT_RESTORED'
  | 'TAG_ADDED'
  | 'TAG_REMOVED'
  | 'TAG_RENAMED'
  | 'TAG_DELETED'
  | 'ATTRIBUTE_ADDED'
  | 'ATTRIBUTE_CHANGED'
  | 'ATTRIBUTE_REMOVED'
  | 'CONTACTS_MERGED'
  | 'CONTACTS_EXPORTED'
  | 'MEMBER_JOINED'
  | 'ROLE_CHANGED'
  | 'MEMBER_REMOVED'
  | 'INVITATION_CREATED'
  | 'INVITATION_CANCELLED'
  | 'VERSION_RESTORED';

/** The old and new value of one field; an empty value is `null` (R-016-02). */
export interface FieldChange {
  old: string | null;
  new: string | null;
}

/** Contract schema `HistoryEntry`; `details` has a fixed shape per action (ADR-08). */
export interface HistoryEntry {
  id: string;
  occurredAt: string;
  userName: string;
  userEmail?: string | null;
  action: HistoryAction;
  contactId?: string | null;
  details?: {
    values?: Record<string, string | null>;
    count?: number;
    member?: string;
    email?: string;
    role?: string;
    old?: string;
    new?: string;
    key?: string;
    name?: string;
    changes?: Record<string, FieldChange>;
    /** The number of the Version a `VERSION_RESTORED` entry restored (R-023-02); "removed" after a deletion forever. */
    restored?: number | string;
  };
}

/** Contract schema `HistoryPage`. */
export interface HistoryPage {
  items: HistoryEntry[];
  page: number;
  pageSize: number;
  total: number;
}

/** `GET /groups/{g}/contacts/{c}/history?page=` (R-016-03), newest first. */
export function getContactHistory(groupId: string, contactId: string, page: number): Promise<HistoryPage> {
  return apiRequest<HistoryPage>(
    `/groups/${encodeURIComponent(groupId)}/contacts/${encodeURIComponent(contactId)}/history`,
    { query: { page } },
  );
}

/** The filters of the Group activity list (R-017-02..05); an empty value does not filter. */
export interface ActivityFilters {
  userId: string;
  action: HistoryAction | '';
  from: string;
  to: string;
}

/** `GET /groups/{g}/activity?userId&action&from&to&page` (R-017-01), newest first. */
export function getGroupActivity(
  groupId: string,
  filters: ActivityFilters,
  page: number,
): Promise<HistoryPage> {
  return apiRequest<HistoryPage>(`/groups/${encodeURIComponent(groupId)}/activity`, {
    query: {
      page,
      userId: filters.userId || undefined,
      action: filters.action || undefined,
      from: filters.from || undefined,
      to: filters.to || undefined,
    },
  });
}
