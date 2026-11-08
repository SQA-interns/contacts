import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { AuthFailure } from '@/auth/client';
import { setLanguage } from '@/i18n';
import { fakeAuthClient } from '@/test/fakeAuth';
import { renderApp } from '@/test/renderApp';

async function register(displayName: string, email: string, password: string) {
  const name = await screen.findByLabelText(/Prikazno ime/);
  if (displayName) await userEvent.type(name, displayName);
  if (email) await userEvent.type(screen.getByLabelText(/E-pošta/), email);
  if (password) await userEvent.type(screen.getByLabelText(/Geslo/), password);
  await userEvent.click(screen.getByRole('button', { name: 'Ustvari račun' }));
}

describe('RegisterPage WF-02 (US-001)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
  });

  it('R-001-01 registering creates the account and asks to verify the e-mail (AC-001-01)', async () => {
    const { client } = fakeAuthClient(null);
    const { router } = renderApp('/register', client);

    await register('Ana Novak', 'ana@acme.com', 'Geslo1234');

    expect(client.register).toHaveBeenCalledWith('Ana Novak', 'ana@acme.com', 'Geslo1234');
    await waitFor(() => expect(router.state.location.pathname).toBe('/verify-email'));
  });

  it.each([
    ['EX-001-01 a password of exactly 8 characters', 'Ana Novak', 'Geslo123'],
    ['EX-001-03 a display name of 100 characters', 'A'.repeat(100), 'Geslo1234'],
  ])('%s creates the account', async (_name, displayName, password) => {
    const { client } = fakeAuthClient(null);
    renderApp('/register', client);

    await register(displayName, 'ana@acme.com', password);

    expect(client.register).toHaveBeenCalledWith(displayName, 'ana@acme.com', password);
  });

  it('R-001-06 a password of 7 characters creates no account and shows the rule at the field (EX-001-02)', async () => {
    const { client } = fakeAuthClient(null);
    renderApp('/register', client);

    await register('Nova Oseba', 'new@acme.com', 'Abc1234');

    expect(screen.getByLabelText(/Geslo/)).toHaveAccessibleDescription(
      expect.stringContaining('Geslo mora imeti vsaj 8 znakov.'),
    );
    expect(client.register).not.toHaveBeenCalled();
  });

  it.each([
    ['101 characters', 'A'.repeat(101)],
    ['empty', ''],
    ['only spaces', '   '],
  ])(
    'EX-001-04 a display name that is %s creates no account; the message is at the field',
    async (_n, name) => {
      const { client } = fakeAuthClient(null);
      renderApp('/register', client);

      await register(name, 'ana@acme.com', 'Geslo1234');

      expect(screen.getByLabelText(/Prikazno ime/)).toHaveAccessibleDescription(
        expect.stringContaining('Ime mora imeti od 1 do 100 znakov.'),
      );
      expect(client.register).not.toHaveBeenCalled();
    },
  );

  it('EX-001-06 the e-mail ana@ creates no account; the message is at the field and the values stay', async () => {
    const { client } = fakeAuthClient(null);
    renderApp('/register', client);

    await register('Ana Novak', 'ana@', 'Geslo1234');

    expect(screen.getByLabelText(/E-pošta/)).toHaveAccessibleDescription('E-poštni naslov ni veljaven.');
    expect(screen.getByLabelText(/Prikazno ime/)).toHaveValue('Ana Novak');
    expect(client.register).not.toHaveBeenCalled();
  });

  it('R-001-07 an address that is registered offers Sign in and Reset password (AC-001-07)', async () => {
    const { client } = fakeAuthClient(null);
    client.register.mockRejectedValue(new AuthFailure('email_in_use'));
    const { router } = renderApp('/register', client);

    await register('Ana Druga', 'ana@acme.com', 'Geslo1234');

    expect(await screen.findByRole('alert')).toHaveTextContent('Ta e-poštni naslov je že registriran.');
    await userEvent.click(screen.getByRole('button', { name: 'Ponastavi geslo' }));
    expect(router.state.location.pathname).toBe('/reset-password');
  });

  it('R-001-07 the Sign in offer opens the sign-in page', async () => {
    const { client } = fakeAuthClient(null);
    client.register.mockRejectedValue(new AuthFailure('email_in_use'));
    const { router } = renderApp('/register', client);

    await register('Ana Druga', 'ana@acme.com', 'Geslo1234');
    await userEvent.click(await screen.findByRole('button', { name: 'Prijava' }));

    expect(router.state.location.pathname).toBe('/sign-in');
  });

  it('shows the generic error when Firebase cannot be reached', async () => {
    const { client } = fakeAuthClient(null);
    client.register.mockRejectedValue(new AuthFailure('unavailable'));
    renderApp('/register', client);

    await register('Ana Novak', 'ana@acme.com', 'Geslo1234');

    expect(await screen.findByRole('alert')).toHaveTextContent('Nekaj je šlo narobe. Poskusite znova.');
  });
});
