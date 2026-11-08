import { beforeEach, describe, expect, it } from 'vitest';
import i18n, { DEFAULT_LANGUAGE, isLanguage, setLanguage, storedLanguage } from './index';

describe('language choice (US-019)', () => {
  beforeEach(async () => {
    window.localStorage.clear();
    await setLanguage('sl');
    window.localStorage.clear();
  });

  it('starts in Slovene when nothing is stored (R-019-01)', () => {
    expect(DEFAULT_LANGUAGE).toBe('sl');
    expect(storedLanguage({ getItem: () => null })).toBe('sl');
  });

  it('ignores a stored value that is not a language', () => {
    expect(storedLanguage({ getItem: () => 'de' })).toBe('sl');
    expect(isLanguage('de')).toBe(false);
  });

  it('falls back to Slovene when storage throws', () => {
    expect(
      storedLanguage({
        getItem: () => {
          throw new Error('blocked');
        },
      }),
    ).toBe('sl');
  });

  it('changes every text at once and remembers the choice (R-019-02, R-019-03)', async () => {
    expect(i18n.t('auth.signIn.title')).toBe('Prijava');
    await setLanguage('en');
    expect(i18n.t('auth.signIn.title')).toBe('Sign in');
    expect(window.localStorage.getItem('language')).toBe('en');
    expect(document.documentElement.lang).toBe('en');
    expect(storedLanguage()).toBe('en');
  });
});
