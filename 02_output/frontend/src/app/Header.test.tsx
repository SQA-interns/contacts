import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { ANA, ANA_UNVERIFIED, fakeAuthClient } from '@/test/fakeAuth';
import { renderApp } from '@/test/renderApp';

describe('Header (US-001)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response('[]', { status: 200 })),
    );
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-001-10 signing out from the user menu ends the session and shows the sign-in page', async () => {
    const { client } = fakeAuthClient(ANA);
    const { router, queryClient } = renderApp('/groups', client);

    await userEvent.click(await screen.findByRole('button', { name: 'Uporabniški meni' }));
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Odjava' }));

    expect(client.signOut).toHaveBeenCalled();
    await waitFor(() => expect(router.state.location.pathname).toBe('/sign-in'));
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0);
  });

  it('R-001-04 an unverified User sees no Group navigation', async () => {
    renderApp('/verify-email', fakeAuthClient(ANA_UNVERIFIED).client);
    expect(await screen.findByRole('button', { name: 'Uporabniški meni' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Skupine' })).not.toBeInTheDocument();
  });

  it('a visitor who is not signed in sees no user menu', async () => {
    renderApp('/sign-in', fakeAuthClient(null).client);
    expect(await screen.findByRole('heading', { level: 1, name: 'Prijava' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Uporabniški meni' })).not.toBeInTheDocument();
  });
});
