import { apiRequest } from './client';
import type { Role } from './groups';

/** Contract schema `Invitation`. */
export interface Invitation {
  id: string;
  email: string;
  role: Role;
  status: 'PENDING' | 'ACCEPTED' | 'CANCELLED' | 'EXPIRED';
  createdAt: string;
  expiresAt: string;
}

const base = (groupId: string) => `/groups/${encodeURIComponent(groupId)}/invitations`;

/** `GET /groups/{g}/invitations`: the pending Invitations (R-014-09, Owner only). */
export function listInvitations(groupId: string): Promise<Invitation[]> {
  return apiRequest<Invitation[]>(base(groupId));
}

/** `POST /groups/{g}/invitations` (R-014-01); no e-mail is sent (R-014-12). */
export function createInvitation(groupId: string, email: string, role: Role): Promise<Invitation> {
  return apiRequest<Invitation>(base(groupId), { method: 'POST', body: { email, role } });
}

/** `POST /groups/{g}/invitations/{i}/cancel` (R-014-07). */
export function cancelInvitation(groupId: string, invitationId: string): Promise<Invitation> {
  return apiRequest<Invitation>(`${base(groupId)}/${encodeURIComponent(invitationId)}/cancel`, {
    method: 'POST',
  });
}
