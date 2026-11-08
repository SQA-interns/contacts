import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { I18nextProvider, useTranslation } from 'react-i18next';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n, { setLanguage } from '@/i18n';
import { LanguageSwitch } from './LanguageSwitch';

function Page() {
  const { t } = useTranslation();
  const [name, setName] = useState('');
  return (
    <>
      <LanguageSwitch />
      <label>
        {t('auth.signIn.title')}
        <input value={name} onChange={(e) => setName(e.target.value)} />
      </label>
      <p>Šola neObvescaj</p>
    </>
  );
}

describe('LanguageSwitch (US-019)', () => {
  beforeEach(async () => {
    window.localStorage.clear();
    await setLanguage('sl');
  });

  it('AC-019-02 switches the texts at once and keeps what was typed', async () => {
    render(
      <I18nextProvider i18n={i18n}>
        <Page />
      </I18nextProvider>,
    );
    await userEvent.type(screen.getByRole('textbox'), 'Ana');

    await userEvent.click(screen.getByRole('button', { name: 'EN' }));

    expect(screen.getByLabelText('Sign in')).toHaveValue('Ana');
    expect(screen.getByRole('button', { name: 'EN' })).toHaveAttribute('aria-pressed', 'true');
    expect(window.localStorage.getItem('language')).toBe('en');
  });

  it('AC-019-06 leaves text entered by users unchanged', async () => {
    render(
      <I18nextProvider i18n={i18n}>
        <Page />
      </I18nextProvider>,
    );

    await userEvent.click(screen.getByRole('button', { name: 'EN' }));

    expect(screen.getByText('Šola neObvescaj')).toBeInTheDocument();
  });
});
