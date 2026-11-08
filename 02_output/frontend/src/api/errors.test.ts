import { beforeEach, describe, expect, it } from 'vitest';
import i18n, { setLanguage } from '@/i18n';
import { ApiError, errorMessage } from './errors';

describe('error message in the active language (R-019-07)', () => {
  beforeEach(async () => {
    window.localStorage.clear();
    await setLanguage('sl');
  });

  it('AC-019-07 shows "changed by someone else" in Slovene, then in English', async () => {
    const error = new ApiError(409, 'contact.changed_by_someone_else', 'ignored server text');

    expect(errorMessage(i18n.t, error)).toBe(i18n.t('api.contact.changed_by_someone_else'));
    const slovene = errorMessage(i18n.t, error);
    await setLanguage('en');
    const english = errorMessage(i18n.t, error);

    expect(english).toMatch(/changed by someone else/i);
    expect(english).not.toBe(slovene);
  });

  it('never shows the server text, and falls back to the generic text for an unknown code or error', () => {
    const generic = i18n.t('error.generic');
    expect(errorMessage(i18n.t, new ApiError(500, 'made.up_code', 'stack trace'))).toBe(generic);
    expect(errorMessage(i18n.t, new Error('boom'))).toBe(generic);
  });
});
