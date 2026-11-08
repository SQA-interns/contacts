import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthFailure } from '@/auth/client';
import { setLanguage } from '@/i18n';
import { ANA, ANA_UNVERIFIED, fakeAuthClient } from '@/test/fakeAuth';
import { renderApp } from '@/test/renderApp';

const verifyHeading = 'Potrdite svoj e-poštni naslov, da nadaljujete.';

describe('VerifyEmailPage WF-04 (US-001)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response('[]', { status: 200 })),
    );
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-001-04 an unverified User sees the message and the resend button, and no Group (AC-001-04)', async () => {
    renderApp('/groups', fakeAuthClient(ANA_UNVERIFIED).client);

    expect(await screen.findByRole('heading', { level: 1, name: verifyHeading })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Pošlji znova' })).toBeInTheDocument();
    expect(screen.getByText(/ana@acme\.com/)).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Moje skupine' })).not.toBeInTheDocument();
  });

  it('R-001-04 the resend button sends the verification e-mail again', async () => {
    const { client } = fakeAuthClient(ANA_UNVERIFIED);
    renderApp('/verify-email', client);

    await userEvent.click(await screen.findByRole('button', { name: 'Pošlji znova' }));

    expect(client.resendVerification).toHaveBeenCalled();
    expect(await screen.findByRole('status')).toHaveTextContent('Povezavo smo poslali znova.');
  });

  it('R-001-04 a failed resend shows the generic error', async () => {
    const { client } = fakeAuthClient(ANA_UNVERIFIED);
    client.resendVerification.mockRejectedValue(new AuthFailure('unavailable'));
    renderApp('/verify-email', client);

    await userEvent.click(await screen.findByRole('button', { name: 'Pošlji znova' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Nekaj je šlo narobe. Poskusite znova.');
  });

  it('R-001-10 signing out on the page shows the sign-in page', async () => {
    const { client } = fakeAuthClient(ANA_UNVERIFIED);
    const { router } = renderApp('/verify-email', client);

    await userEvent.click(await screen.findByRole('button', { name: 'Odjava' }));

    expect(client.signOut).toHaveBeenCalled();
    await waitFor(() => expect(router.state.location.pathname).toBe('/sign-in'));
  });

  it('EX-001-09 once the e-mail is verified the page opens the Group list', async () => {
    const { client, emit } = fakeAuthClient(ANA_UNVERIFIED);
    const { router } = renderApp('/verify-email', client);
    await screen.findByRole('heading', { level: 1, name: verifyHeading });

    emit(ANA);

    await waitFor(() => expect(router.state.location.pathname).toBe('/groups'));
  });

  it('a visitor who is not signed in is sent to the sign-in page', async () => {
    const { router } = renderApp('/verify-email', fakeAuthClient(null).client);
    await waitFor(() => expect(router.state.location.pathname).toBe('/sign-in'));
  });
});
