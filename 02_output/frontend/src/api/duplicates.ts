import { apiRequest } from './client';
import type { Contact, ContactFields, ContactListItem } from './contacts';

/** Contract schema `DuplicateSet`: Contacts that probably describe one person. */
export interface DuplicateSet {
  key: string;
  mark: 'certain' | 'probable';
  /** Code of the reason text, for example `duplicate.same_email`. */
  reasonCode: string;
  reasonParams?: Record<string, string>;
  contacts: ContactListItem[];
}

/** `GET /groups/{g}/duplicates` (R-024-01): the search reports only and changes nothing (R-024-06). */
export function findDuplicates(groupId: string): Promise<DuplicateSet[]> {
  return apiRequest<DuplicateSet[]>(`/groups/${encodeURIComponent(groupId)}/duplicates`);
}

/**
 * `POST /groups/{g}/duplicates/dismissals` (R-025-01): marks the set of exactly these Contacts "not a
 * duplicate"; no Contact changes (R-025-05).
 */
export function dismissDuplicate(groupId: string, contactIds: string[]): Promise<void> {
  return apiRequest<void>(`/groups/${encodeURIComponent(groupId)}/duplicates/dismissals`, {
    method: 'POST',
    body: { contactIds },
  });
}

/** Contract schema `MergeRequest` (US-026); the Survivor gets the Tags of all merged Contacts (R-026-03). */
export interface MergeRequest {
  survivorId: string;
  contacts: { id: string; version: number }[];
  fields: ContactFields;
  attributes: { key: string; value: string }[];
}

/**
 * `POST /groups/{g}/merges` (R-026-05): gives the Survivor the chosen values and moves the others to
 * Trash; nothing is merged unless every Contact still has the version sent (R-026-08).
 */
export function mergeContacts(groupId: string, request: MergeRequest): Promise<Contact> {
  return apiRequest<Contact>(`/groups/${encodeURIComponent(groupId)}/merges`, {
    method: 'POST',
    body: request,
  });
}
