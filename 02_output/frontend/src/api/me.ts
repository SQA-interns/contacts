import { apiRequest } from './client';

/** Contract schema `User`: the signed-in person as the product stores them. */
export interface Me {
  id: string;
  email: string;
  displayName: string;
  emailVerified: boolean;
  createdAt: string;
}

/** `GET /me` (R-001-11). */
export function getMe(): Promise<Me> {
  return apiRequest<Me>('/me');
}

/** `DELETE /me`: deletes my account (R-021-01); 409 `account.sole_owner` carries the Groups to deal with first. */
export function deleteMe(): Promise<void> {
  return apiRequest('/me', { method: 'DELETE' });
}
