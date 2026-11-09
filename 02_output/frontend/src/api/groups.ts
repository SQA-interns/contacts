import { apiRequest } from './client';

export type Role = 'OWNER' | 'EDITOR' | 'VIEWER';

/** Contract schema `GroupSummary`. */
export interface GroupSummary {
  id: string;
  name: string;
  role: Role;
  createdAt: string;
}

/** `GET /groups`: the Groups of the signed-in User with the User's Role. */
export function listGroups(): Promise<GroupSummary[]> {
  return apiRequest<GroupSummary[]>('/groups');
}

/** `POST /groups`: creates a Group of which the signed-in User is the Owner (R-002-01). */
export function createGroup(name: string): Promise<GroupSummary> {
  return apiRequest<GroupSummary>('/groups', { method: 'POST', body: { name } });
}

/** Contract schema `GroupTrashItem`: a Group in the Group Trash. */
export interface GroupTrashItem {
  id: string;
  name: string;
  deletedAt: string;
  daysLeft: number;
}

/** `GET /group-trash`: the Groups in Trash of which the caller is an Owner (R-020-04). */
export function listGroupTrash(): Promise<GroupTrashItem[]> {
  return apiRequest<GroupTrashItem[]>('/group-trash');
}

/** `POST /group-trash/{g}/restore` (R-020-07); 409 `group.name_taken` (R-020-08), 404 after 30 days. */
export function restoreGroup(groupId: string): Promise<GroupSummary> {
  return apiRequest<GroupSummary>(`/group-trash/${encodeURIComponent(groupId)}/restore`, { method: 'POST' });
}
