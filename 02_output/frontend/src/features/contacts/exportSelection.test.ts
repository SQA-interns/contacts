import { afterEach, describe, expect, it, vi } from 'vitest';
import { NO_FILTERS } from '@/api/contacts';
import { filterBody, resolveSelection } from './exportSelection';

const page = (n: number, size: number, total: number) =>
  new Response(
    JSON.stringify({
      items: Array.from({ length: size }, (_, i) => ({ id: `p${n}-${i}`, tags: [] })),
      page: n,
      pageSize: 25,
      total,
    }),
  );

describe('filterBody', () => {
  it('leaves out everything that is empty', () => {
    expect(filterBody('', NO_FILTERS)).toEqual({});
  });

  it('names the search text and the active filters as the contract does (R-011-02)', () => {
    expect(
      filterBody(' Ana ', {
        ...NO_FILTERS,
        tags: ['udelezenec2025'],
        withoutTags: ['neObvescaj'],
        country: 'SI',
        company: 'Acme',
        attrKey: 'kompetenca',
        attrExists: true,
      }),
    ).toEqual({
      q: 'Ana',
      tags: ['udelezenec2025'],
      tagMode: 'all',
      withoutTags: ['neObvescaj'],
      country: 'SI',
      company: 'Acme',
      attrKey: 'kompetenca',
      attrExists: true,
    });
  });

  it('sends an Attribute value when no "exists" is chosen', () => {
    expect(filterBody('', { ...NO_FILTERS, attrKey: 'k', attrValue: ' v ' })).toEqual({
      attrKey: 'k',
      attrValue: 'v',
    });
  });
});

describe('resolveSelection', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('sends the ticked ids as they are', async () => {
    const selection = { allResults: false, ticked: ['a', 'b'], unticked: [] };

    expect(await resolveSelection('g1', selection, '', NO_FILTERS)).toEqual({ contactIds: ['a', 'b'] });
  });

  it('sends the filter when "all results" has no unticked Contact', async () => {
    const selection = { allResults: true, ticked: [], unticked: [] };

    expect(await resolveSelection('g1', selection, 'Ana', NO_FILTERS)).toEqual({
      filter: { q: 'Ana' },
    });
  });

  it('lists the results page by page and leaves out the unticked ones (EX-010-05)', async () => {
    const fetchMock = vi.fn((url: string) => {
      const n = Number(new URL(url, 'http://localhost').searchParams.get('page'));
      return Promise.resolve(n === 1 ? page(1, 25, 27) : page(2, 2, 27));
    });
    vi.stubGlobal('fetch', fetchMock);
    const selection = { allResults: true, ticked: [], unticked: ['p1-3', 'p2-1'] };

    const result = await resolveSelection('g1', selection, '', NO_FILTERS);

    expect('contactIds' in result && result.contactIds).toHaveLength(25);
    expect('contactIds' in result && result.contactIds).not.toContain('p1-3');
    expect('contactIds' in result && result.contactIds).not.toContain('p2-1');
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
});
