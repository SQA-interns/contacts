import { apiRequest } from './client';
import type { Contact } from './contacts';

/** Contract schema `TrashContact`. */
export interface TrashContact {
  id: string;
  firstName?: string | null;
  lastName?: string | null;
  email?: string | null;
  company?: string | null;
  deletedAt: string;
  deletedBy?: string | null;
  daysLeft: number;
}

const group = (groupId: string) => `/groups/${encodeURIComponent(groupId)}`;

/** `POST /groups/{g}/contacts/{c}/trash` (R-012-02); repeating it changes nothing (R-012-09). */
export function trashContact(groupId: string, contactId: string): Promise<void> {
  return apiRequest(`${group(groupId)}/contacts/${encodeURIComponent(contactId)}/trash`, {
    method: 'POST',
  });
}

/** `GET /groups/{g}/trash/contacts` (R-012-03). */
export function listTrash(groupId: string): Promise<TrashContact[]> {
  return apiRequest<TrashContact[]>(`${group(groupId)}/trash/contacts`);
}

/** `POST /groups/{g}/trash/contacts/{c}/restore` (R-012-04); 404 after 30 days (R-012-05). */
export function restoreContact(groupId: string, contactId: string): Promise<Contact> {
  return apiRequest<Contact>(`${group(groupId)}/trash/contacts/${encodeURIComponent(contactId)}/restore`, {
    method: 'POST',
  });
}
