import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { InvitationsPage } from './InvitationsPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

const boris = {
  id: 'i1',
  email: 'boris@acme.com',
  role: 'EDITOR',
  status: 'PENDING',
  createdAt: '2026-11-08T08:00:00Z',
  expiresAt: '2026-11-22T08:00:00Z',
};

interface Sent {
  url: string;
  method: string;
  body?: unknown;
}

/** The backend: the Group list gives the Role, `/invitations` the list, `POST` creates, `/cancel` cancels. */
function backend(role: string, list: unknown[], reply?: (sent: Sent) => Response | undefined): Sent[] {
  const sent: Sent[] = [];
  vi.mocked(fetch).mockImplementation((url, init) => {
    const path = String(url);
    const method = init?.method ?? 'GET';
    if (path.endsWith('/groups')) {
      return Promise.resolve(answer([{ id: 'g1', name: 'Acme', role, createdAt: boris.createdAt }]));
    }
    const call: Sent = { url: path, method, body: init?.body ? JSON.parse(String(init.body)) : undefined };
    sent.push(call);
    const custom = reply?.(call);
    if (custom) return Promise.resolve(custom);
    if (method === 'POST' && path.endsWith('/cancel'))
      return Promise.resolve(answer({ ...boris, status: 'CANCELLED' }));
    if (method === 'POST') return Promise.resolve(answer(boris, 201));
    return Promise.resolve(answer(list));
  });
  return sent;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/groups/g1/invitations']}>
        <Routes>
          <Route path="/groups/:groupId/invitations" element={<InvitationsPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('InvitationsPage WF-12 (R-014-01..04, R-014-07, R-014-09, R-014-10)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-014-09 lists the pending Invitations with e-mail, Role and expiry date', async () => {
    backend('OWNER', [boris]);
    renderPage();

    const table = await screen.findByRole('table', { name: 'Odprta povabila' });
    const row = within(table).getAllByRole('row')[1]!;
    expect(row).toHaveTextContent('boris@acme.com');
    expect(row).toHaveTextContent('Urednik');
    expect(row).toHaveTextContent('22. 11. 2026');
    expect(screen.getByText('Odprta povabila: 1 od 20')).toBeInTheDocument();
  });

  it('shows an empty state when nothing is pending', async () => {
    backend('OWNER', []);
    renderPage();

    expect(await screen.findByText('Ni odprtih povabil')).toBeInTheDocument();
  });

  it('R-014-01 sends the e-mail and Role and tells the Owner to inform the person (R-014-12)', async () => {
    const sent = backend('OWNER', []);
    renderPage();
    await userEvent.type(await screen.findByLabelText(/E-pošta/), 'boris@acme.com');
    await userEvent.selectOptions(screen.getByLabelText(/Vloga/), 'EDITOR');
    await userEvent.click(screen.getByRole('button', { name: 'Ustvari povabilo' }));

    await vi.waitFor(() =>
      expect(sent.find((s) => s.method === 'POST')?.body).toEqual({
        email: 'boris@acme.com',
        role: 'EDITOR',
      }),
    );
    expect(
      await screen.findByText('Povabilo je ustvarjeno. Obvestite povabljeno osebo.'),
    ).toBeInTheDocument();
  });

  it('R-014-02 shows a message next to the field and sends nothing for "boris@"', async () => {
    const sent = backend('OWNER', []);
    renderPage();
    await userEvent.type(await screen.findByLabelText(/E-pošta/), 'boris@');
    await userEvent.click(screen.getByRole('button', { name: 'Ustvari povabilo' }));

    expect(await screen.findByText('E-poštni naslov ni veljaven.')).toBeInTheDocument();
    expect(sent.filter((s) => s.method === 'POST')).toHaveLength(0);
  });

  it('R-014-04 says the person is already a Member or invited', async () => {
    backend('OWNER', [], (s) =>
      s.method === 'POST' ? answer({ code: 'invitation.exists', message: 'x' }, 409) : undefined,
    );
    renderPage();
    await userEvent.type(await screen.findByLabelText(/E-pošta/), 'boris@acme.com');
    await userEvent.click(screen.getByRole('button', { name: 'Ustvari povabilo' }));

    expect(await screen.findByText('Ta oseba je že članica ali že ima odprto povabilo.')).toBeInTheDocument();
  });

  it('R-014-03 shows the limit', async () => {
    backend('OWNER', [], (s) =>
      s.method === 'POST' ? answer({ code: 'invitation.limit', message: 'x' }, 409) : undefined,
    );
    renderPage();
    await userEvent.type(await screen.findByLabelText(/E-pošta/), 'boris@acme.com');
    await userEvent.click(screen.getByRole('button', { name: 'Ustvari povabilo' }));

    expect(await screen.findByText('Skupina ima že 20 odprtih povabil.')).toBeInTheDocument();
  });

  it('R-014-07 asks for confirmation, then cancels the Invitation', async () => {
    const sent = backend('OWNER', [boris]);
    renderPage();
    await userEvent.click((await screen.findAllByRole('button', { name: /Prekliči povabilo/ }))[0]!);
    const dialog = await screen.findByRole('dialog', { name: 'Prekličem povabilo?' });
    expect(sent.filter((s) => s.method === 'POST')).toHaveLength(0);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Prekliči povabilo' }));

    await vi.waitFor(() =>
      expect(sent.filter((s) => s.method === 'POST').map((s) => s.url)).toEqual([
        expect.stringContaining('/groups/g1/invitations/i1/cancel'),
      ]),
    );
  });

  it('R-014-10 shows an Editor a refusal and asks the backend nothing', async () => {
    const sent = backend('EDITOR', [boris]);
    renderPage();

    expect(await screen.findByText(/nimate pravice/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ustvari povabilo' })).not.toBeInTheDocument();
    expect(sent).toHaveLength(0);
  });
});
