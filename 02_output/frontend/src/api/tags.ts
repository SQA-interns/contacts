import { apiRequest } from './client';

/** Contract schema `TagCount`. */
export interface TagCount {
  id: string;
  name: string;
  contactCount: number;
}

/** `GET /groups/{g}/tags` (R-006-01). */
export function listTags(groupId: string): Promise<TagCount[]> {
  return apiRequest<TagCount[]>(`/groups/${groupId}/tags`);
}

/** `PATCH /groups/{g}/tags/{t}` (R-006-02..04). */
export function renameTag(groupId: string, tagId: string, name: string): Promise<TagCount> {
  return apiRequest<TagCount>(`/groups/${groupId}/tags/${tagId}`, { method: 'PATCH', body: { name } });
}

/** `DELETE /groups/{g}/tags/{t}` (R-006-05). */
export function deleteTag(groupId: string, tagId: string): Promise<void> {
  return apiRequest(`/groups/${groupId}/tags/${tagId}`, { method: 'DELETE' });
}
