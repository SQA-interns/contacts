import { renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import type { DuplicateSet } from '@/api/duplicates';
import { useReasonText } from '@/features/duplicates/useReasonText';
import { useDaysText } from '@/features/trash/useDaysText';
import { setLanguage } from '@/i18n';
import { formatDate } from './format';

afterEach(() => setLanguage('sl'));

describe('plural forms of Slovene (R-024-05, UI-16)', () => {
  it('chooses one, two, few and other by the count of days', async () => {
    await setLanguage('sl');
    const { result } = renderHook(() => useDaysText());
    expect(result.current(1)).toBe('1 dan');
    expect(result.current(2)).toBe('2 dneva');
    expect(result.current(3)).toBe('3 dni');
    expect(result.current(4)).toBe('4 dni');
    expect(result.current(5)).toBe('5 dni');
    expect(result.current(0)).toBe('0 dni');
  });

  it('chooses the form of the reason by the count of Contacts, from the parameters or from the set', async () => {
    await setLanguage('sl');
    const { result } = renderHook(() => useReasonText());
    const set = (n: number, count?: string): DuplicateSet =>
      ({
        reasonCode: 'duplicate.same_email',
        reasonParams: { email: 'a@b.si', ...(count ? { count } : {}) },
        contacts: Array.from({ length: n }, (_, i) => ({ id: `c${i}` })),
      }) as unknown as DuplicateSet;
    expect(result.current(set(2))).toBe('2 kontakta imata e-pošto a@b.si');
    expect(result.current(set(3))).toBe('3 kontakti imajo e-pošto a@b.si');
    expect(result.current(set(5))).toBe('5 kontaktov ima e-pošto a@b.si');
    expect(result.current(set(1))).toBe('1 kontakt ima e-pošto a@b.si');
    expect(result.current(set(2, '4'))).toBe('4 kontakti imajo e-pošto a@b.si');
  });
});

describe('English dates and the document language', () => {
  it('pads the month and the day', () => {
    expect(formatDate('2026-03-05T12:00:00Z', 'en')).toBe('2026-03-05');
  });

  it('sets the language of the page when the language changes', async () => {
    await setLanguage('en');
    expect(document.documentElement.lang).toBe('en');
    await setLanguage('sl');
    expect(document.documentElement.lang).toBe('sl');
  });
});
