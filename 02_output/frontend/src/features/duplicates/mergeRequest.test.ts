import { describe, expect, it } from 'vitest';
import type { Contact } from '@/api/contacts';
import { contactName } from './contactName';
import { mergeRequest, type AttributeRow } from './preselect';

const contact = (id: string, version: number): Contact => ({
  id,
  version,
  tags: [],
  attributes: [],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
});

const row = (key: string, needsChoice: boolean, preselected: string | null): AttributeRow => ({
  key,
  values: [],
  needsChoice,
  preselected,
});

describe('the request of a confirmed merge (R-026-08)', () => {
  const ordered = [contact('a', 3), contact('b', 5)];

  it('names the Survivor, the version of every Contact, the fields and the Attributes', () => {
    const request = mergeRequest(
      ordered,
      { firstName: 'Ana' },
      [row('same', false, 'x'), row('pick', true, null), row('none', false, null), row('Other', true, null)],
      { pick: 'chosen', other: 'by-lower-case-key' },
    );
    expect(request).toEqual({
      survivorId: 'a',
      contacts: [
        { id: 'a', version: 3 },
        { id: 'b', version: 5 },
      ],
      fields: { firstName: 'Ana' },
      attributes: [
        { key: 'same', value: 'x' },
        { key: 'pick', value: 'chosen' },
        { key: 'Other', value: 'by-lower-case-key' },
      ],
    });
  });

  it('leaves out an Attribute with no value, also when the choice is missing', () => {
    expect(mergeRequest(ordered, {}, [row('k', true, null), row('j', false, null)], {}).attributes).toEqual(
      [],
    );
  });

  it('keeps an empty text that was chosen', () => {
    expect(mergeRequest(ordered, {}, [row('k', true, null)], { k: '' }).attributes).toEqual([
      { key: 'k', value: '' },
    ]);
  });

  it('refuses to merge nothing', () => {
    expect(() => mergeRequest([], {}, [], {})).toThrow('A merge needs Contacts');
  });
});

describe('how a Contact is named in the duplicate screens', () => {
  it('uses name, then company, then e-mail, then the fallback', () => {
    expect(contactName({ firstName: 'Ana', lastName: 'Novak', company: 'Acme' }, '?')).toBe('Ana Novak');
    expect(contactName({ firstName: 'Ana', company: 'Acme' }, '?')).toBe('Ana');
    expect(contactName({ lastName: 'Novak' }, '?')).toBe('Novak');
    expect(contactName({ firstName: null, lastName: '', company: 'Acme', email: 'a@b.si' }, '?')).toBe(
      'Acme',
    );
    expect(contactName({ email: 'a@b.si' }, '?')).toBe('a@b.si');
    expect(contactName({}, '?')).toBe('?');
  });
});
