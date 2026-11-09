import { apiRequest } from './client';

/** The 11 predefined fields of a Contact (R-003-02); an empty field is absent or `null`. */
export const CONTACT_FIELDS = [
  'firstName',
  'lastName',
  'email',
  'phone',
  'company',
  'jobTitle',
  'street',
  'postalCode',
  'city',
  'country',
  'notes',
] as const;

export type ContactField = (typeof CONTACT_FIELDS)[number];
export type ContactFields = Partial<Record<ContactField, string | null>>;

export interface AttributePair {
  key: string;
  value: string;
}

/** Contract schema `Contact`. */
export interface Contact extends ContactFields {
  id: string;
  version: number;
  tags: string[];
  attributes: AttributePair[];
  createdAt: string;
  updatedAt: string;
}

/** Contract schema `ContactInput`: `version` is required on edit only. */
export interface ContactInput extends ContactFields {
  tags?: string[];
  attributes?: AttributePair[];
  version?: number;
}

const base = (groupId: string) => `/groups/${encodeURIComponent(groupId)}/contacts`;

/** `POST /groups/{g}/contacts` (R-003-01). */
export function createContact(groupId: string, input: ContactInput): Promise<Contact> {
  return apiRequest<Contact>(base(groupId), { method: 'POST', body: input });
}

/** `GET /groups/{g}/contacts/{c}` (R-003-08). */
export function getContact(groupId: string, contactId: string): Promise<Contact> {
  return apiRequest<Contact>(`${base(groupId)}/${encodeURIComponent(contactId)}`);
}

/** `PUT /groups/{g}/contacts/{c}` with the version the form was opened at (R-003-06). */
export function updateContact(groupId: string, contactId: string, input: ContactInput): Promise<Contact> {
  return apiRequest<Contact>(`${base(groupId)}/${encodeURIComponent(contactId)}`, {
    method: 'PUT',
    body: input,
  });
}

/** The Attribute keys used in the Group, for suggestions (R-004-05): `GET /groups/{g}/facets`. */
export async function getAttributeKeys(groupId: string): Promise<string[]> {
  const facets = await apiRequest<{ attributeKeys?: string[] }>(
    `/groups/${encodeURIComponent(groupId)}/facets`,
  );
  return facets.attributeKeys ?? [];
}

/** The Tag names of the Group, for suggestions (R-005-07): `GET /groups/{g}/facets`. */
export async function getTagNames(groupId: string): Promise<string[]> {
  const facets = await apiRequest<{ tags?: string[] }>(`/groups/${encodeURIComponent(groupId)}/facets`);
  return facets.tags ?? [];
}
