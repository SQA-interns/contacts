import { NO_FILTERS, type ContactFilters } from '@/api/contacts';

/** The URL parameters that hold the filters, besides `q`, `sort`, `dir` and `page`. */
const KEYS = ['tags', 'tagMode', 'withoutTags', 'country', 'company', 'attrKey', 'attrValue', 'attrExists'];

/** The filters of the address (they survive a reload and a shared link). */
export function filtersOf(params: URLSearchParams): ContactFilters {
  return {
    tags: params.getAll('tags'),
    tagMode: params.get('tagMode') === 'any' ? 'any' : 'all',
    withoutTags: params.getAll('withoutTags'),
    country: params.get('country') ?? '',
    company: params.get('company') ?? '',
    attrKey: params.get('attrKey') ?? '',
    attrValue: params.get('attrValue') ?? '',
    attrExists: params.get('attrExists') === 'true',
  };
}

/** The address parameters for the filters; page 1 is shown after a change (R-009-09). */
export function withFilters(old: URLSearchParams, filters: ContactFilters): URLSearchParams {
  const copy = new URLSearchParams(old);
  for (const key of KEYS) copy.delete(key);
  for (const tag of filters.tags) copy.append('tags', tag);
  if (filters.tags.length > 0) copy.set('tagMode', filters.tagMode);
  for (const tag of filters.withoutTags) copy.append('withoutTags', tag);
  if (filters.country) copy.set('country', filters.country);
  if (filters.company) copy.set('company', filters.company);
  if (filters.attrKey) copy.set('attrKey', filters.attrKey);
  if (filters.attrKey && filters.attrExists) copy.set('attrExists', 'true');
  else if (filters.attrKey && filters.attrValue) copy.set('attrValue', filters.attrValue);
  copy.set('page', '1');
  return copy;
}

/** How many filters are active: the Tag filters count once each, the Attribute filter once. */
export function activeCount(f: ContactFilters): number {
  return (
    (f.tags.length > 0 ? 1 : 0) +
    (f.withoutTags.length > 0 ? 1 : 0) +
    (f.country ? 1 : 0) +
    (f.company ? 1 : 0) +
    (f.attrKey && (f.attrValue.trim() || f.attrExists) ? 1 : 0)
  );
}

export { NO_FILTERS };
