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
    changes?: Record<string, FieldChange>;
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
