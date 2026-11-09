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

/** The columns the Contact list sorts by (contract parameter `sort`). */
export type SortColumn = 'lastName' | 'firstName' | 'email' | 'company' | 'city' | 'country' | 'tags';

/** Contract schema `ContactListItem`. */
export interface ContactListItem {
  id: string;
  firstName?: string | null;
  lastName?: string | null;
  email?: string | null;
  company?: string | null;
  city?: string | null;
  country?: string | null;
  tags: string[];
  version?: number;
  createdAt?: string;
}

/** Contract schema `ContactPage`. */
export interface ContactPage {
  items: ContactListItem[];
  page: number;
  pageSize: number;
  total: number;
}

/** The filters of the Contact list (US-009), as the contract parameters of `listContacts`. */
export interface ContactFilters {
  tags: string[];
  tagMode: 'all' | 'any';
  withoutTags: string[];
  country: string;
  company: string;
  attrKey: string;
  attrValue: string;
  attrExists: boolean;
}

export const NO_FILTERS: ContactFilters = {
  tags: [],
  tagMode: 'all',
  withoutTags: [],
  country: '',
  company: '',
  attrKey: '',
  attrValue: '',
  attrExists: false,
};

/** Adds the active filters to the query; an empty one is left out (R-009-01..07, EX-009-01). */
export function filterParams(params: URLSearchParams, f: ContactFilters): void {
  for (const tag of f.tags) params.append('tags', tag);
  if (f.tags.length > 0) params.set('tagMode', f.tagMode);
  for (const tag of f.withoutTags) params.append('withoutTags', tag);
  if (f.country) params.set('country', f.country);
  if (f.company) params.set('company', f.company);
  if (f.attrKey && f.attrExists) {
    params.set('attrKey', f.attrKey);
    params.set('attrExists', 'true');
  } else if (f.attrKey && f.attrValue.trim()) {
    params.set('attrKey', f.attrKey);
    params.set('attrValue', f.attrValue.trim());
  }
}

/** `GET /groups/{g}/contacts` (R-007-01..04, R-008-01, R-009-01..09); an empty `q` is left out (R-008-05). */
export function listContacts(
  groupId: string,
  query: {
    page: number;
    sort: SortColumn;
    dir: 'asc' | 'desc';
    q?: string;
    filters?: ContactFilters;
  },
): Promise<ContactPage> {
  const params = new URLSearchParams({ page: String(query.page), sort: query.sort, dir: query.dir });
  if (query.q?.trim()) params.set('q', query.q.trim());
  if (query.filters) filterParams(params, query.filters);
  return apiRequest<ContactPage>(`${base(groupId)}?${params.toString()}`);
}

/** Contract schema `Facets`: what the filters offer in the Group (R-009-11). */
export interface Facets {
  tags: string[];
  companies: string[];
  countries: string[];
  attributeKeys: string[];
}

/** `GET /groups/{g}/facets`; a missing list is empty. */
export async function getFacets(groupId: string): Promise<Facets> {
  const facets = await apiRequest<Partial<Facets>>(`/groups/${encodeURIComponent(groupId)}/facets`);
  return {
    tags: facets.tags ?? [],
    companies: facets.companies ?? [],
    countries: facets.countries ?? [],
    attributeKeys: facets.attributeKeys ?? [],
  };
}
