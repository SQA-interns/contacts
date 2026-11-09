import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '@/auth/AuthProvider';
import { Toaster } from '@/components/ui/sonner';
import { setLanguage } from '@/i18n';
import { ANA, fakeAuthClient } from '@/test/fakeAuth';
import { AccountPage } from './AccountPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

const ana = {
  id: 'u1',
  email: 'ana@acme.com',
  displayName: 'Ana Novak',
  emailVerified: true,
  createdAt: '2026-11-01T08:00:00Z',
};

interface Sent {
  url: string;
  method: string;
}

/** The backend: `GET /me` answers `me`, `DELETE /me` answers `deletion`. */
function backend(deletion: () => Response, me: () => Response = () => answer(ana)): Sent[] {
  const sent: Sent[] = [];
  vi.mocked(fetch).mockImplementation((url, init) => {
    const method = init?.method ?? 'GET';
    sent.push({ url: String(url), method });
    return Promise.resolve(method === 'DELETE' ? deletion() : me());
  });
  return sent;
}

const soleOwner = (groups: unknown[]) => answer({ code: 'account.sole_owner', message: 'x', groups }, 409);

function renderPage() {
  const auth = fakeAuthClient(ANA);
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider client={auth.client}>
        <MemoryRouter initialEntries={['/account']}>
          <Routes>
            <Route path="/account" element={<AccountPage />} />
            <Route path="/sign-in" element={<p>Prijava stran</p>} />
            <Route path="/group-trash" element={<p>Koš skupin stran</p>} />
            <Route path="/groups/:groupId/members" element={<p>Člani stran</p>} />
          </Routes>
        </MemoryRouter>
        <Toaster />
      </AuthProvider>
    </QueryClientProvider>,
  );
  return auth.client;
}

async function openDialog() {
  await userEvent.click(await screen.findByRole('button', { name: 'Izbriši račun' }));
  return screen.getByRole('dialog', { name: 'Izbrišem račun?' });
}

describe('AccountPage WF-16 (US-021)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('shows the display name and the e-mail address of the User', async () => {
    backend(() => new Response(null, { status: 204 }));
    renderPage();

    expect(await screen.findByText('Ana Novak')).toBeInTheDocument();
    expect(screen.getByText('ana@acme.com')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'Moj račun' })).toBeInTheDocument();
  });

  it('shows a loading state, then an error state with a retry when the User cannot be loaded', async () => {
    backend(
      () => new Response(null, { status: 204 }),
      () => answer({ code: 'server.error', message: 'x' }, 500),
    );
    renderPage();

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Poskusi znova' })).toBeInTheDocument();
  });

  it('R-021-06 asks for a confirmation that says the deletion cannot be undone', async () => {
    backend(() => new Response(null, { status: 204 }));
    renderPage();

    const dialog = await openDialog();

    expect(dialog).toHaveTextContent('Tega ni mogoče razveljaviti.');
  });

  it('EX-021-03 cancelling the confirmation sends nothing and closes the dialog', async () => {
    const sent = backend(() => new Response(null, { status: 204 }));
    renderPage();

    const dialog = await openDialog();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Prekliči' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(sent.filter((s) => s.method === 'DELETE')).toEqual([]);
  });

  it('R-021-01 confirming deletes the account, signs out and goes to the sign-in page', async () => {
    const sent = backend(() => new Response(null, { status: 204 }));
    const client = renderPage();

    const dialog = await openDialog();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Izbriši račun' }));

    expect(await screen.findByText('Prijava stran')).toBeInTheDocument();
    expect(sent.filter((s) => s.method === 'DELETE').map((s) => new URL(s.url).pathname)).toEqual([
      '/api/v1/me',
    ]);
    expect(client.signOut).toHaveBeenCalled();
    expect(await screen.findByText('Račun je izbrisan.')).toBeInTheDocument();
  });

  it('R-021-02 a Sole Owner sees the refusal with every Group and what to do', async () => {
    backend(() =>
      soleOwner([
        { id: 'g1', name: 'Acme', inTrash: false, daysLeft: null },
        { id: 'g2', name: 'Stari', inTrash: true, daysLeft: 12 },
      ]),
    );
    const client = renderPage();

    const dialog = await openDialog();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Izbriši račun' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Računa ni mogoče izbrisati, ker ste edini lastnik skupine.');
    expect(alert).toHaveTextContent('Vlogo lastnika predajte drugemu članu');
    expect(within(alert).getByText('Acme')).toBeInTheDocument();
    expect(within(alert).getByText('Stari')).toBeInTheDocument();
    expect(alert).toHaveTextContent('v košu skupin, še 12 dni');
    expect(within(alert).getByRole('link', { name: 'Člani Acme' })).toHaveAttribute(
      'href',
      '/groups/g1/members',
    );
    expect(within(alert).getByRole('button', { name: 'Izbriši skupino Acme' })).toBeInTheDocument();
    expect(within(alert).getByRole('link', { name: 'Koš skupin' })).toHaveAttribute('href', '/group-trash');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(client.signOut).not.toHaveBeenCalled();
  });

  it('R-021-02 a Group deleted from the refusal leaves the list; with none left the refusal goes', async () => {
    const sent = backend(() =>
      soleOwner([
        { id: 'g1', name: 'Acme', inTrash: false, daysLeft: null },
        { id: 'g2', name: 'Beta', inTrash: false, daysLeft: null },
      ]),
    );
    renderPage();
    const dialog = await openDialog();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Izbriši račun' }));
    const alert = await screen.findByRole('alert');

    for (const name of ['Acme', 'Beta']) {
      await userEvent.click(within(alert).getByRole('button', { name: `Izbriši skupino ${name}` }));
      const groupDialog = await screen.findByRole('dialog', { name: `Izbrišem skupino ${name}?` });
      await userEvent.type(within(groupDialog).getByLabelText('Za potrditev vpišite ime skupine'), name);
      await userEvent.click(within(groupDialog).getByRole('button', { name: 'Izbriši skupino' }));
      await vi.waitFor(() =>
        expect(sent.some((s) => s.method === 'POST' && s.url.includes(name === 'Acme' ? 'g1' : 'g2'))).toBe(
          true,
        ),
      );
      if (name === 'Acme') {
        await vi.waitFor(() => expect(within(alert).queryByText('Acme')).not.toBeInTheDocument());
        expect(within(alert).getByText('Beta')).toBeInTheDocument();
      }
    }

    await vi.waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
  });

  it('R-021-02 any other failure shows a plain message and keeps the User signed in', async () => {
    backend(() => answer({ code: 'server.error', message: 'x' }, 500));
    const client = renderPage();

    const dialog = await openDialog();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Izbriši račun' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Nekaj je šlo narobe');
    expect(client.signOut).not.toHaveBeenCalled();
  });
});
