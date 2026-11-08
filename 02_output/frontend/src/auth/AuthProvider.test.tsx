import { screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiRequest } from '@/api/client';
import { setLanguage } from '@/i18n';
import { ANA, ANA_UNVERIFIED, fakeAuthClient } from '@/test/fakeAuth';
import { renderApp } from '@/test/renderApp';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

describe('AuthProvider and RequireVerified (US-001)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => answer([])),
    );
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-001-09 a restored Firebase session keeps the User signed in on the Group list', async () => {
    const { router } = renderApp('/groups', fakeAuthClient(ANA).client);
    expect(await screen.findByRole('heading', { level: 1, name: 'Moje skupine' })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/groups');
  });

  it('R-001-10 without a session the Group list sends the visitor to the sign-in page', async () => {
    const { router } = renderApp('/groups', fakeAuthClient(null).client);
    await waitFor(() => expect(router.state.location.pathname).toBe('/sign-in'));
    expect(screen.queryByRole('heading', { name: 'Moje skupine' })).not.toBeInTheDocument();
  });

  it('R-001-04 an unverified User is sent to the verify page instead of a Group', async () => {
    const { router } = renderApp('/groups', fakeAuthClient(ANA_UNVERIFIED).client);
    await waitFor(() => expect(router.state.location.pathname).toBe('/verify-email'));
    expect(screen.queryByRole('heading', { name: 'Moje skupine' })).not.toBeInTheDocument();
  });

  it('R-001-12 a 401 from the server ends the session and returns to the sign-in page', async () => {
    vi.mocked(fetch).mockResolvedValue(answer({ code: 'auth.token_invalid', message: 'Invalid token' }, 401));
    const { client } = fakeAuthClient(ANA);
    const { router } = renderApp('/groups', client);

    await waitFor(() => expect(router.state.location.pathname).toBe('/sign-in'));
    expect(client.signOut).toHaveBeenCalled();
  });

  it('R-018-01 requests carry the ID token of the session', async () => {
    renderApp('/sign-in', fakeAuthClient(ANA).client);
    await apiRequest('/groups');
    const [, init] = vi.mocked(fetch).mock.calls.at(-1)!;
    expect((init?.headers as Record<string, string>).Authorization).toBe('Bearer token');
  });
});
