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
