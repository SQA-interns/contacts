import { describe, expect, it } from 'vitest';
import { ERROR_CODES } from '@/api/errorCodes';
import en from './locales/en.json';
import sl from './locales/sl.json';

type Tree = { [key: string]: string | Tree };

function keys(tree: Tree, prefix = ''): string[] {
  return Object.entries(tree).flatMap(([key, value]) =>
    typeof value === 'string' ? [`${prefix}${key}`] : keys(value, `${prefix}${key}.`),
  );
}
function lookup(tree: Tree, path: string): unknown {
  return path.split('.').reduce<unknown>((node, part) => (node as Tree | undefined)?.[part], tree);
}
function values(tree: Tree): string[] {
  return Object.values(tree).flatMap((v) => (typeof v === 'string' ? [v] : values(v)));
}

describe('message catalogs (NFR-11, UI-30)', () => {
  it('have identical key sets', () => {
    expect(keys(sl).sort()).toEqual(keys(en).sort());
  });

  it('have no empty text', () => {
    for (const text of [...values(sl), ...values(en)]) expect(text.trim()).not.toBe('');
  });

  it('have the same placeholders in both languages', () => {
    for (const key of keys(sl)) {
      const placeholders = (text: unknown) =>
        String(text)
          .match(/{{\s*\w+\s*}}/g)
          ?.sort() ?? [];
      expect(placeholders(lookup(en, key)), key).toEqual(placeholders(lookup(sl, key)));
    }
  });

  it('have a text for every backend error code in both languages', () => {
    for (const code of ERROR_CODES) {
      expect(typeof lookup(sl, `api.${code}`), `sl ${code}`).toBe('string');
      expect(typeof lookup(en, `api.${code}`), `en ${code}`).toBe('string');
    }
  });

  it('lists exactly the error codes of the specification (47)', () => {
    expect(new Set(ERROR_CODES).size).toBe(ERROR_CODES.length);
    expect(ERROR_CODES).toHaveLength(47);
  });
});
