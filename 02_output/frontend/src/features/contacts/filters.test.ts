import { describe, expect, it } from 'vitest';
import { NO_FILTERS } from '@/api/contacts';
import { activeCount, filtersOf, withFilters } from './filters';

describe('filters in the address (R-009-09)', () => {
  it('reads every filter and defaults the empty ones', () => {
    expect(filtersOf(new URLSearchParams())).toEqual(NO_FILTERS);
    expect(
      filtersOf(
        new URLSearchParams(
          'tags=a&tags=b&tagMode=any&withoutTags=x&country=SI&company=Acme&attrKey=k&attrValue=v&attrExists=true',
        ),
      ),
    ).toEqual({
      tags: ['a', 'b'],
      tagMode: 'any',
      withoutTags: ['x'],
      country: 'SI',
      company: 'Acme',
      attrKey: 'k',
      attrValue: 'v',
      attrExists: true,
    });
    expect(filtersOf(new URLSearchParams('tagMode=all')).tagMode).toBe('all');
    expect(filtersOf(new URLSearchParams('tagMode=other')).tagMode).toBe('all');
    expect(filtersOf(new URLSearchParams('attrExists=false')).attrExists).toBe(false);
  });

  it('writes only the active filters, drops the old ones, keeps other parameters and goes to page 1', () => {
    const old = new URLSearchParams('q=Ana&sort=city&page=4&tags=old&country=HR&attrKey=zz&attrValue=zz');
    expect(withFilters(old, NO_FILTERS).toString()).toBe('q=Ana&sort=city&page=1');
    expect(
      withFilters(new URLSearchParams(), {
        ...NO_FILTERS,
        tags: ['a', 'b'],
        tagMode: 'any',
        withoutTags: ['x'],
        country: 'SI',
        company: 'Acme',
        attrKey: 'k',
        attrValue: 'v',
      }).toString(),
    ).toBe('tags=a&tags=b&tagMode=any&withoutTags=x&country=SI&company=Acme&attrKey=k&attrValue=v&page=1');
    expect(withFilters(new URLSearchParams(), { ...NO_FILTERS, tagMode: 'any' }).toString()).toBe('page=1');
    expect(
      withFilters(new URLSearchParams(), {
        ...NO_FILTERS,
        attrKey: 'k',
        attrExists: true,
        attrValue: 'v',
      }).toString(),
    ).toBe('attrKey=k&attrExists=true&page=1');
    expect(withFilters(new URLSearchParams(), { ...NO_FILTERS, attrKey: 'k' }).toString()).toBe(
      'attrKey=k&page=1',
    );
    expect(
      withFilters(new URLSearchParams(), { ...NO_FILTERS, attrValue: 'v', attrExists: true }).toString(),
    ).toBe('page=1');
  });

  it('counts the Tag filters once each and the Attribute filter once', () => {
    expect(activeCount(NO_FILTERS)).toBe(0);
    expect(activeCount({ ...NO_FILTERS, tags: ['a', 'b'] })).toBe(1);
    expect(activeCount({ ...NO_FILTERS, withoutTags: ['a', 'b'] })).toBe(1);
    expect(activeCount({ ...NO_FILTERS, country: 'SI' })).toBe(1);
    expect(activeCount({ ...NO_FILTERS, company: 'Acme' })).toBe(1);
    expect(activeCount({ ...NO_FILTERS, attrKey: 'k', attrValue: 'v' })).toBe(1);
    expect(activeCount({ ...NO_FILTERS, attrKey: 'k', attrExists: true })).toBe(1);
    expect(activeCount({ ...NO_FILTERS, attrKey: 'k', attrValue: '  ' })).toBe(0);
    expect(activeCount({ ...NO_FILTERS, attrKey: '', attrValue: 'v', attrExists: true })).toBe(0);
    expect(
      activeCount({
        tags: ['a'],
        tagMode: 'all',
        withoutTags: ['b'],
        country: 'SI',
        company: 'Acme',
        attrKey: 'k',
        attrValue: 'v',
        attrExists: false,
      }),
    ).toBe(5);
    expect(activeCount({ ...NO_FILTERS, tags: ['a'], country: 'SI' })).toBe(2);
  });
});
