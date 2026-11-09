import { apiRequest } from './client';
import type { Role } from './groups';

/** Contract schema `Member`. */
export interface Member {
  userId: string;
  displayName: string;
  email: string;
  role: Role;
  joinedAt: string;
}

const base = (groupId: string) => `/groups/${encodeURIComponent(groupId)}/members`;

/** `GET /groups/{g}/members`: every Member of the Group (R-015-01). */
export function listMembers(groupId: string): Promise<Member[]> {
  return apiRequest<Member[]>(base(groupId));
}

/** `PATCH /groups/{g}/members/{u}`: changes the Role of a Member (R-015-02, Owner only). */
export function changeMemberRole(groupId: string, userId: string, role: Role): Promise<Member> {
  return apiRequest<Member>(`${base(groupId)}/${encodeURIComponent(userId)}`, {
    method: 'PATCH',
    body: { role },
  });
}

/** `DELETE /groups/{g}/members/{u}`: removes a Member (R-015-03, Owner only). */
export function removeMember(groupId: string, userId: string): Promise<void> {
  return apiRequest<void>(`${base(groupId)}/${encodeURIComponent(userId)}`, { method: 'DELETE' });
}
