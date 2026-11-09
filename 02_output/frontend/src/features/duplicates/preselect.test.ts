import { describe, expect, it } from 'vitest';
import type { Contact } from '@/api/contacts';
import {
  attributeRows,
  combinedTags,
  fieldRows,
  mergeRequest,
  missingChoices,
  orderForMerge,
} from './preselect';

const contact = (id: string, createdAt: string, extra: Partial<Contact> = {}): Contact => ({
  id,
  version: 1,
  tags: [],
  attributes: [],
  createdAt,
  updatedAt: createdAt,
  ...extra,
});

const older = contact('b', '2026-11-06T10:00:00Z', { firstName: 'Ana' });
const newer = contact('a', '2026-11-07T10:00:00Z', { firstName: 'ana', phone: '040 123 456' });

describe('orderForMerge (R-026-02, EX-026-03)', () => {
  it('puts the oldest Contact first, then the others oldest first', () => {
    const oldest = contact('c', '2026-11-05T10:00:00Z');

    expect(orderForMerge([newer, oldest, older]).map((c) => c.id)).toEqual(['c', 'b', 'a']);
  });

  it('EX-026-03 with equal creation times the smaller id comes first', () => {
    const same = '2026-11-06T10:00:00Z';

    const ordered = orderForMerge([contact('00b', same), contact('00a', same)]);

    expect(ordered.map((c) => c.id)).toEqual(['00a', '00b']);
  });

  it('does not change the list it is given', () => {
    const list = [newer, older];

    orderForMerge(list);

    expect(list.map((c) => c.id)).toEqual(['a', 'b']);
  });
});

describe('fieldRows (R-026-02)', () => {
  it('AC-026-01 preselects the first name of the Survivor and the phone of the next Contact', () => {
    const rows = fieldRows(orderForMerge([newer, older]));

    const first = rows.find((r) => r.field === 'firstName');
    const phone = rows.find((r) => r.field === 'phone');
    expect(first).toMatchObject({ values: ['Ana', 'ana'], preselected: 'Ana', needsChoice: true });
    expect(phone).toMatchObject({ values: [null, '040 123 456'], preselected: '040 123 456' });
  });

  it('EX-026-02 a value all Contacts share is preselected and needs no choice', () => {
    const a = contact('a', '2026-11-06T10:00:00Z', { company: 'Acme' });
    const b = contact('b', '2026-11-07T10:00:00Z', { company: 'Acme' });

    const row = fieldRows([a, b]).find((r) => r.field === 'company');

    expect(row).toMatchObject({ preselected: 'Acme', needsChoice: false });
  });

  it('a field that is empty in all Contacts is empty and needs no choice', () => {
    const row = fieldRows([older, newer]).find((r) => r.field === 'jobTitle');

    expect(row).toMatchObject({ values: [null, null], preselected: null, needsChoice: false });
  });

  it('treats empty text like no value', () => {
    const a = contact('a', '2026-11-06T10:00:00Z', { city: ' ' });
    const b = contact('b', '2026-11-07T10:00:00Z', { city: 'Celje' });

    const row = fieldRows([a, b]).find((r) => r.field === 'city');

    expect(row).toMatchObject({ values: [null, 'Celje'], preselected: 'Celje' });
  });
});

describe('attributeRows (R-026-04, EX-026-08)', () => {
  const withAttribute = (id: string, at: string, key: string, value: string) =>
    contact(id, at, { attributes: [{ key, value }] });

  it('AC-026-03 two values for one key need a choice and are not preselected', () => {
    const rows = attributeRows([
      withAttribute('a', '2026-11-06T10:00:00Z', 'kompetenca', 'java'),
      withAttribute('b', '2026-11-07T10:00:00Z', 'kompetenca', 'spletnoProgramiranje'),
    ]);

    expect(rows).toEqual([
      {
        key: 'kompetenca',
        values: ['java', 'spletnoProgramiranje'],
        needsChoice: true,
        preselected: null,
      },
    ]);
  });

  it('EX-026-08 keys that differ in case are one key, shown in the first spelling', () => {
    const rows = attributeRows([
      withAttribute('a', '2026-11-06T10:00:00Z', 'Kompetenca', 'java'),
      withAttribute('b', '2026-11-07T10:00:00Z', 'kompetenca', 'sql'),
    ]);

    expect(rows).toHaveLength(1);
    expect(rows[0]).toMatchObject({ key: 'Kompetenca', values: ['java', 'sql'], needsChoice: true });
  });

  it('a key with the same value everywhere, or on one Contact only, is kept without a choice', () => {
    const rows = attributeRows([
      contact('a', '2026-11-06T10:00:00Z', {
        attributes: [
          { key: 'jezik', value: 'sl' },
          { key: 'vloga', value: 'vodja' },
        ],
      }),
      withAttribute('b', '2026-11-07T10:00:00Z', 'Jezik', 'sl'),
    ]);

    expect(rows).toEqual([
      { key: 'jezik', values: ['sl', 'sl'], needsChoice: false, preselected: 'sl' },
      { key: 'vloga', values: ['vodja', null], needsChoice: false, preselected: 'vodja' },
    ]);
  });
});

describe('combinedTags (R-026-03)', () => {
  it('lists the Tags of all Contacts once, ignoring case', () => {
    const a = contact('a', '2026-11-06T10:00:00Z', { tags: ['udelezenec2025', 'VIP'] });
    const b = contact('b', '2026-11-07T10:00:00Z', { tags: ['neObvescaj', 'vip'] });

    expect(combinedTags([a, b])).toEqual(['udelezenec2025', 'VIP', 'neObvescaj']);
  });
});

describe('missingChoices and mergeRequest (R-026-04, R-026-05)', () => {
  const clash = [
    contact('a', '2026-11-06T10:00:00Z', {
      firstName: 'Ana',
      attributes: [{ key: 'kompetenca', value: 'java' }],
    }),
    contact('b', '2026-11-07T10:00:00Z', {
      firstName: 'ana',
      phone: '040 123 456',
      attributes: [{ key: 'kompetenca', value: 'sql' }],
    }),
  ];

  it('names the keys with several values and no chosen one', () => {
    const rows = attributeRows(clash);

    expect(missingChoices(rows, {})).toEqual(['kompetenca']);
    expect(missingChoices(rows, { kompetenca: 'sql' })).toEqual([]);
  });

  it('builds the request with the Survivor, every version and the chosen values', () => {
    const request = mergeRequest(clash, { firstName: 'Ana', phone: '040 123 456' }, attributeRows(clash), {
      kompetenca: 'sql',
    });

    expect(request).toEqual({
      survivorId: 'a',
      contacts: [
        { id: 'a', version: 1 },
        { id: 'b', version: 1 },
      ],
      fields: { firstName: 'Ana', phone: '040 123 456' },
      attributes: [{ key: 'kompetenca', value: 'sql' }],
    });
  });

  it('sends an empty choice as null', () => {
    const request = mergeRequest(clash, { firstName: null }, [], {});

    expect(request.fields).toEqual({ firstName: null });
  });
});
