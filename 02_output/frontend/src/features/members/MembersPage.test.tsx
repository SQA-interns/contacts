import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { MembersPage } from './MembersPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
const joined = '2026-11-08T08:00:00Z';
const members = [
  { userId: 'u1', displayName: 'Ana Novak', email: 'ana@acme.com', role: 'OWNER', joinedAt: joined },
  { userId: 'u2', displayName: 'Boris Kos', email: 'boris@acme.com', role: 'EDITOR', joinedAt: joined },
  { userId: 'u3', displayName: 'Viola Vidmar', email: 'viola@acme.com', role: 'VIEWER', joinedAt: joined },
];

interface Sent {
  url: string;
  method: string;
  body?: unknown;
}

function backend(role: string, reply?: (sent: Sent) => Response | undefined): Sent[] {
  const sent: Sent[] = [];
  vi.mocked(fetch).mockImplementation((url, init) => {
    const path = String(url);
    const method = init?.method ?? 'GET';
    if (path.endsWith('/groups')) {
      return Promise.resolve(answer([{ id: 'g1', name: 'Acme', role, createdAt: joined }]));
    }
    const call: Sent = { url: path, method, body: init?.body ? JSON.parse(String(init.body)) : undefined };
    sent.push(call);
    const custom = reply?.(call);
    if (custom) return Promise.resolve(custom);
    if (method === 'DELETE') return Promise.resolve(new Response(null, { status: 204 }));
    if (method === 'PATCH') return Promise.resolve(answer({ ...members[1], role: 'VIEWER' }));
    return Promise.resolve(answer(members));
  });
  return sent;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/groups/g1/members']}>
        <Routes>
          <Route path="/groups/:groupId/members" element={<MembersPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('MembersPage WF-11 (R-015-01..05)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-015-01, R-015-05 a Viewer sees names, e-mail addresses and Roles without controls', async () => {
    backend('VIEWER');
    renderPage();

    const table = await screen.findByRole('table', { name: 'Člani skupine' });
    expect(within(table).getAllByRole('row')).toHaveLength(4);
    expect(table).toHaveTextContent('boris@acme.com');
    expect(table).toHaveTextContent('Ogledovalec');
    expect(screen.queryByRole('combobox')).toBeNull();
    expect(screen.queryByRole('button', { name: /Odstrani/ })).toBeNull();
    expect(screen.getByText('3 člani')).toBeInTheDocument();
  });

  it('R-015-02 an Owner changes the Role and sees the confirmation', async () => {
    const sent = backend('OWNER');
    renderPage();
    const row = (await screen.findAllByRole('row')).find((r) => r.textContent?.includes('Boris'))!;
    await userEvent.selectOptions(within(row).getByRole('combobox', { name: 'Vloga' }), 'VIEWER');

    await vi.waitFor(() =>
      expect(sent.find((s) => s.method === 'PATCH')).toMatchObject({
        url: expect.stringContaining('/groups/g1/members/u2') as string,
        body: { role: 'VIEWER' },
      }),
    );
    expect(await screen.findByText('Vloga je spremenjena.')).toBeInTheDocument();
  });

  it('R-015-03 asks for confirmation, then removes the Member', async () => {
    const sent = backend('OWNER');
    renderPage();
    await userEvent.click((await screen.findAllByRole('button', { name: 'Odstrani člana Boris Kos' }))[0]!);
    const dialog = await screen.findByRole('dialog', { name: 'Odstranim člana Boris Kos?' });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Odstrani člana' }));

    await vi.waitFor(() => expect(sent.some((s) => s.method === 'DELETE')).toBe(true));
    expect(await screen.findByText('Član je odstranjen.')).toBeInTheDocument();
  });

  it('R-015-04 shows "A group needs at least one owner" when the server refuses', async () => {
    backend('OWNER', (s) =>
      s.method === 'PATCH' ? answer({ code: 'group.last_owner', message: 'x' }, 409) : undefined,
    );
    renderPage();
    const row = (await screen.findAllByRole('row')).find((r) => r.textContent?.includes('Ana'))!;
    await userEvent.selectOptions(within(row).getByRole('combobox', { name: 'Vloga' }), 'EDITOR');

    expect(await screen.findByText('Skupina potrebuje vsaj enega lastnika.')).toBeInTheDocument();
  });
});
