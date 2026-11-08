import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiRequest } from '@/api/client';
import { AuthFailure } from '@/auth/client';
import { setLanguage } from '@/i18n';
import { ANA, fakeAuthClient } from '@/test/fakeAuth';
import { renderApp } from '@/test/renderApp';

async function fillAndSubmit(email: string, password: string) {
  await userEvent.type(await screen.findByLabelText('E-pošta'), email);
  await userEvent.type(screen.getByLabelText('Geslo'), password);
  await userEvent.click(screen.getByRole('button', { name: 'Prijava' }));
}

describe('SignInPage WF-01 (US-001)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response('[]', { status: 200 })),
    );
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-001-03 a sign-in Firebase accepts opens the Group list', async () => {
    const { client, emit } = fakeAuthClient(null);
    client.signIn.mockImplementation(async () => emit(ANA));
    const { router } = renderApp('/sign-in', client);

    await fillAndSubmit('ana@acme.com', 'Geslo1234');

    expect(client.signIn).toHaveBeenCalledWith('ana@acme.com', 'Geslo1234');
    await waitFor(() => expect(router.state.location.pathname).toBe('/groups'));
  });

  it('R-001-05 a wrong e-mail or password shows the one message and signs nobody in', async () => {
    const { client } = fakeAuthClient(null);
    client.signIn.mockRejectedValue(new AuthFailure('invalid_credentials'));
    const { router } = renderApp('/sign-in', client);

    await fillAndSubmit('ana@acme.com', 'NapacnoGeslo1');

    expect(await screen.findByRole('alert')).toHaveTextContent('E-pošta ali geslo ni pravilno.');
    expect(router.state.location.pathname).toBe('/sign-in');
  });

  it('R-001-05 an unreachable Firebase shows the generic error, not the credential message', async () => {
    const { client } = fakeAuthClient(null);
    client.signIn.mockRejectedValue(new AuthFailure('unavailable'));
    renderApp('/sign-in', client);

    await fillAndSubmit('ana@acme.com', 'Geslo1234');

    expect(await screen.findByRole('alert')).toHaveTextContent('Nekaj je šlo narobe. Poskusite znova.');
  });

  it('R-001-02 "Prijava z Googlom" signs in through Google', async () => {
    const { client, emit } = fakeAuthClient(null);
    client.signInWithGoogle.mockImplementation(async () => emit(ANA));
    const { router } = renderApp('/sign-in', client);

    await userEvent.click(await screen.findByRole('button', { name: 'Prijava z Googlom' }));

    expect(client.signInWithGoogle).toHaveBeenCalled();
    await waitFor(() => expect(router.state.location.pathname).toBe('/groups'));
  });

  it('R-001-02 a closed Google window shows no error', async () => {
    const { client } = fakeAuthClient(null);
    client.signInWithGoogle.mockRejectedValue(new AuthFailure('cancelled'));
    renderApp('/sign-in', client);

    await userEvent.click(await screen.findByRole('button', { name: 'Prijava z Googlom' }));

    await waitFor(() => expect(client.signInWithGoogle).toHaveBeenCalled());
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('R-001-12 after a 401 the sign-in page says that the session expired', async () => {
    const { client } = fakeAuthClient(ANA);
    renderApp('/sign-in', client);
    vi.mocked(fetch).mockResolvedValue(
      new Response(JSON.stringify({ code: 'auth.token_invalid', message: 'Invalid token' }), { status: 401 }),
    );

    await expect(apiRequest('/groups')).rejects.toThrow();

    expect(await screen.findByText('Seja je potekla. Prijavite se znova.')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Seja je potekla. Prijavite se znova.');
    expect(screen.getByRole('heading', { level: 1, name: 'Prijava' })).toBeInTheDocument();
  });

  it('links to registration and password reset (WF-01)', async () => {
    renderApp('/sign-in', fakeAuthClient(null).client);
    expect(await screen.findByRole('link', { name: 'Registracija' })).toHaveAttribute('href', '/register');
    expect(screen.getByRole('link', { name: 'Pozabljeno geslo?' })).toHaveAttribute(
      'href',
      '/reset-password',
    );
  });
});
