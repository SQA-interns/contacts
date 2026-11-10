import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { InvitationsPage } from './InvitationsPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
const created = '2026-11-08T08:00:00Z';
const boris = {
  id: 'i 1',
  email: 'boris@acme.com',
  role: 'EDITOR',
  status: 'PENDING',
  createdAt: created,
  expiresAt: '2026-11-22T08:00:00Z',
};
const viola = {
  id: 'i2',
  email: 'viola@acme.com',
  role: 'VIEWER',
  status: 'PENDING',
  createdAt: created,
  expiresAt: '2026-12-01T23:30:00Z',
};

interface State {
  listRequests: number;
  sent: { method: string; url: string }[];
}

function backend(role: string, items: unknown[] | 'fail', change?: () => Response): State {
  const state: State = { listRequests: 0, sent: [] };
  vi.mocked(fetch).mockImplementation((url, init) => {
    const method = init?.method ?? 'GET';
    const path = String(url);
    if (method === 'GET' && path.endsWith('/groups')) {
      return Promise.resolve(answer([{ id: 'g 1', name: 'Acme', role, createdAt: created }]));
    }
    if (method === 'GET') {
      state.listRequests++;
      return Promise.resolve(
        items === 'fail' ? answer({ code: 'server.error', message: 'x' }, 500) : answer(items),
      );
    }
    state.sent.push({ method, url: path });
    return Promise.resolve(change ? change() : answer({ ...boris, status: 'CANCELLED' }));
  });
  return state;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/groups/g%201/invitations']}>
        <Routes>
          <Route path="/groups/:groupId/invitations" element={<InvitationsPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('InvitationsPage: texts, the list, cancelling and refusals (R-014-07..10)', () => {
  beforeEach(async () => {
    await setLanguage('en');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it.each(['EDITOR', 'VIEWER'])('refuses a %s and asks for nothing', async (role) => {
    const state = backend(role, [boris]);
    renderPage();
    expect(
      await screen.findByText('You may not manage invitations. Only owners can invite.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Invitations' })).toBeInTheDocument();
    expect(screen.queryByText('boris@acme.com')).not.toBeInTheDocument();
    expect(state.listRequests).toBe(0);
  });

  it('shows the count, the list heading, the table with headers and the dates of expiry', async () => {
    backend('OWNER', [boris, viola]);
    renderPage();
    expect(await screen.findByText('Pending invitations: 2 of 20')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Pending invitations' })).toBeInTheDocument();
    const table = screen.getByRole('table', { name: 'Pending invitations' });
    expect(
      within(table)
        .getAllByRole('columnheader')
        .map((h) => h.textContent),
    ).toEqual(['E-mail', 'Role', 'Expires', 'Actions']);
    const rows = within(table).getAllByRole('row').slice(1);
    expect(
      rows.map((r) =>
        within(r)
          .getAllByRole('cell')
          .slice(0, 3)
          .map((c) => c.textContent),
      ),
    ).toEqual([
      ['boris@acme.com', 'Editor', '2026-11-22'],
      ['viola@acme.com', 'Viewer', '2026-12-02'],
    ]);
    expect(within(rows[0] as HTMLElement).getAllByRole('cell')[0]).toHaveAttribute('title', 'boris@acme.com');
    const cards = within(screen.getByRole('list', { name: 'Pending invitations' })).getAllByRole('listitem');
    expect(cards[0]).toHaveTextContent('boris@acme.com');
    expect(cards[0]).toHaveTextContent('Editor, expires 2026-11-22');
    expect(cards[1]).toHaveTextContent('Viewer, expires 2026-12-02');
    expect(
      within(cards[1] as HTMLElement).getByRole('button', { name: 'Cancel invitation for viola@acme.com' }),
    ).toHaveTextContent('Cancel invitation');
    expect(screen.getByRole('link', { name: '← Contacts' })).toHaveAttribute('href', '/groups/g 1/contacts');
  });

  it('shows the empty state, and the form beside it', async () => {
    backend('OWNER', []);
    renderPage();
    expect(await screen.findByText('No pending invitations')).toBeInTheDocument();
    expect(screen.getByText('Invite a person with the form above.')).toBeInTheDocument();
    expect(screen.getByText('Pending invitations: 0 of 20')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Invite a person' })).toBeInTheDocument();
  });

  it('shows an error with a retry and no count', async () => {
    const state = backend('OWNER', 'fail');
    renderPage();
    const retry = await screen.findByRole('button', { name: /try again|retry/i });
    expect(screen.queryByText(/Pending invitations: /)).not.toBeInTheDocument();
    const before = state.listRequests;
    await userEvent.click(retry);
    await waitFor(() => expect(state.listRequests).toBeGreaterThan(before));
  });

  it('asks before cancelling with the address, sends the cancel of that invitation and asks for the list again', async () => {
    const state = backend('OWNER', [boris, viola]);
    renderPage();
    await userEvent.click(
      (
        await screen.findAllByRole('button', { name: 'Cancel invitation for boris@acme.com' })
      )[0] as HTMLElement,
    );
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText('Cancel the invitation?')).toBeInTheDocument();
    expect(
      within(dialog).getByText('boris@acme.com will not get access with this invitation.'),
    ).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(state.sent).toEqual([]);
    await userEvent.click(
      (
        await screen.findAllByRole('button', { name: 'Cancel invitation for boris@acme.com' })
      )[0] as HTMLElement,
    );
    const before = state.listRequests;
    await userEvent.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Cancel invitation' }),
    );
    await waitFor(() => expect(state.sent).toHaveLength(1));
    expect(state.sent[0]).toEqual({
      method: 'POST',
      url: expect.stringMatching(/\/groups\/g%201\/invitations\/i%201\/cancel$/) as string,
    });
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await waitFor(() => expect(state.listRequests).toBeGreaterThan(before));
  });

  it('shows the refusal of a cancellation and closes the question', async () => {
    backend('OWNER', [boris], () => answer({ code: 'invitation.not_found', message: 'x' }, 404));
    renderPage();
    await userEvent.click(
      (
        await screen.findAllByRole('button', { name: 'Cancel invitation for boris@acme.com' })
      )[0] as HTMLElement,
    );
    await userEvent.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Cancel invitation' }),
    );
    expect(await screen.findByRole('alert')).toHaveTextContent('Invitation not found.');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });
});
