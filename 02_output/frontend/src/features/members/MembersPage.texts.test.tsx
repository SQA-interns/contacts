import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { MembersPage } from './MembersPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
const joined = '2026-11-08T08:00:00Z';
const ana = {
  userId: 'u 1',
  displayName: 'Ana Novak',
  email: 'ana@acme.com',
  role: 'OWNER',
  joinedAt: joined,
};
const boris = {
  userId: 'u2',
  displayName: 'Boris Kos',
  email: 'boris@acme.com',
  role: 'EDITOR',
  joinedAt: joined,
};

interface State {
  memberRequests: number;
  groupRequests: number;
  sent: { method: string; url: string; body?: unknown }[];
}

function backend(
  role: string,
  members: unknown[] | 'fail',
  change: (method: string) => Response | undefined = () => undefined,
): State {
  const state: State = { memberRequests: 0, groupRequests: 0, sent: [] };
  vi.mocked(fetch).mockImplementation((url, init) => {
    const path = String(url);
    const method = init?.method ?? 'GET';
    if (method === 'GET' && path.endsWith('/groups')) {
      state.groupRequests++;
      return Promise.resolve(answer([{ id: 'g 1', name: 'Acme', role, createdAt: joined }]));
    }
    if (method === 'GET') {
      state.memberRequests++;
      return Promise.resolve(
        members === 'fail' ? answer({ code: 'server.error', message: 'x' }, 500) : answer(members),
      );
    }
    state.sent.push({ method, url: path, body: init?.body ? JSON.parse(String(init.body)) : undefined });
    const custom = change(method);
    if (custom) return Promise.resolve(custom);
    return Promise.resolve(
      method === 'DELETE' ? new Response(null, { status: 204 }) : answer({ ...boris, role: 'VIEWER' }),
    );
  });
  return state;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/groups/g%201/members']}>
        <Routes>
          <Route path="/groups/:groupId/members" element={<MembersPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('MembersPage: texts, counts, refusals and the Owner controls (R-015-01..05)', () => {
  beforeEach(async () => {
    await setLanguage('en');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('shows the count of Members in the right plural form', async () => {
    backend('OWNER', [ana]);
    renderPage();
    expect(await screen.findByText('1 member')).toBeInTheDocument();
  });

  it('shows a Viewer the Members, the Roles as text, no controls and no invitation link', async () => {
    backend('VIEWER', [ana, boris]);
    renderPage();
    expect(await screen.findByText('2 members')).toBeInTheDocument();
    const table = screen.getByRole('table', { name: 'Members of the group' });
    expect(
      within(table)
        .getAllByRole('columnheader')
        .map((h) => h.textContent),
    ).toEqual(['Name', 'E-mail', 'Role']);
    const rows = within(table).getAllByRole('row').slice(1);
    expect(
      rows.map((r) =>
        within(r)
          .getAllByRole('cell')
          .map((c) => c.textContent),
      ),
    ).toEqual([
      ['Ana Novak', 'ana@acme.com', 'Owner'],
      ['Boris Kos', 'boris@acme.com', 'Editor'],
    ]);
    expect(
      within(rows[0] as HTMLElement)
        .getAllByRole('cell')
        .map((c) => c.getAttribute('title')),
    ).toEqual(['Ana Novak', 'ana@acme.com', null]);
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Remove/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Invite a person' })).not.toBeInTheDocument();
    const cards = within(screen.getByRole('list', { name: 'Members of the group' })).getAllByRole('listitem');
    expect(cards[1]).toHaveTextContent('Boris Kos');
    expect(cards[1]).toHaveTextContent('boris@acme.com');
    expect(cards[1]).toHaveTextContent('Editor');
  });

  it('gives an Owner the invitation link, a Role list and a Remove button for every Member', async () => {
    backend('OWNER', [ana, boris]);
    renderPage();
    expect(await screen.findByRole('link', { name: 'Invite a person' })).toHaveAttribute(
      'href',
      '/groups/g%201/invitations',
    );
    const table = await screen.findByRole('table', { name: 'Members of the group' });
    expect(
      within(table)
        .getAllByRole('columnheader')
        .map((h) => h.textContent),
    ).toEqual(['Name', 'E-mail', 'Role', 'Actions']);
    const selects = within(table).getAllByRole('combobox', { name: 'Role' });
    expect(selects.map((s) => (s as HTMLSelectElement).value)).toEqual(['OWNER', 'EDITOR']);
    expect(
      within(selects[0]?.closest('tr') as HTMLElement)
        .getAllByRole('option')
        .map((o) => o.textContent),
    ).toEqual(['Owner', 'Editor', 'Viewer']);
    expect(within(table).getByRole('button', { name: 'Remove Boris Kos' })).toHaveTextContent('Remove');
    expect(screen.getByRole('link', { name: '← Contacts' })).toHaveAttribute(
      'href',
      '/groups/g%201/contacts',
    );
    expect(
      within(screen.getByRole('list', { name: 'Members of the group' })).getAllByRole('button', {
        name: /Remove/,
      }),
    ).toHaveLength(2);
  });

  it('changes a Role with the PATCH of the Member, says so and asks for the lists again', async () => {
    const state = backend('OWNER', [ana, boris]);
    renderPage();
    const select = (await screen.findAllByRole('combobox', { name: 'Role' }))[1] as HTMLElement;
    const before = { members: state.memberRequests, groups: state.groupRequests };
    await userEvent.selectOptions(select, 'VIEWER');
    expect(await screen.findByText('Role changed.')).toBeInTheDocument();
    expect(state.sent).toEqual([
      {
        method: 'PATCH',
        url: expect.stringMatching(/\/groups\/g%201\/members\/u2$/) as string,
        body: { role: 'VIEWER' },
      },
    ]);
    await waitFor(() => expect(state.memberRequests).toBeGreaterThan(before.members));
    await waitFor(() => expect(state.groupRequests).toBeGreaterThan(before.groups));
  });

  it('disables the Role lists while a Role is being changed', async () => {
    let finish: (r: Response) => void = () => undefined;
    vi.mocked(fetch).mockImplementation((url, init) => {
      const path = String(url);
      if ((init?.method ?? 'GET') === 'PATCH') return new Promise<Response>((resolve) => (finish = resolve));
      return Promise.resolve(
        answer(
          path.endsWith('/groups')
            ? [{ id: 'g 1', name: 'Acme', role: 'OWNER', createdAt: joined }]
            : [ana, boris],
        ),
      );
    });
    renderPage();
    const selects = await screen.findAllByRole('combobox', { name: 'Role' });
    await userEvent.selectOptions(selects[1] as HTMLElement, 'VIEWER');
    await waitFor(() => expect(selects[0]).toBeDisabled());
    expect(selects[1]).toBeDisabled();
    finish(answer({ ...boris, role: 'VIEWER' }));
    await waitFor(() => expect(selects[0]).toBeEnabled());
  });

  it('shows the refusal of a Role change and no success message', async () => {
    backend('OWNER', [ana, boris], (method) =>
      method === 'PATCH' ? answer({ code: 'member.not_found', message: 'x' }, 404) : undefined,
    );
    renderPage();
    await userEvent.selectOptions(
      (await screen.findAllByRole('combobox', { name: 'Role' }))[1] as HTMLElement,
      'VIEWER',
    );
    expect(await screen.findByRole('alert')).toHaveTextContent('Member not found.');
    expect(screen.queryByText('Role changed.')).not.toBeInTheDocument();
  });

  it('asks before removing with the name, removes the chosen Member only, says so and closes', async () => {
    const state = backend('OWNER', [ana, boris]);
    renderPage();
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Remove Boris Kos' }))[0] as HTMLElement,
    );
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText('Remove Boris Kos?')).toBeInTheDocument();
    expect(within(dialog).getByText(/The member loses access to the group/)).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(state.sent).toEqual([]);
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Remove Boris Kos' }))[0] as HTMLElement,
    );
    await userEvent.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Remove member' }),
    );
    expect(await screen.findByText('Member removed.')).toBeInTheDocument();
    expect(state.sent).toEqual([
      {
        method: 'DELETE',
        url: expect.stringMatching(/\/groups\/g%201\/members\/u2$/) as string,
        body: undefined,
      },
    ]);
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('shows the refusal of a removal and closes the question', async () => {
    backend('OWNER', [ana, boris], (method) =>
      method === 'DELETE' ? answer({ code: 'member.not_found', message: 'x' }, 404) : undefined,
    );
    renderPage();
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Remove Boris Kos' }))[0] as HTMLElement,
    );
    await userEvent.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Remove member' }),
    );
    expect(await screen.findByRole('alert')).toHaveTextContent('Member not found.');
    expect(screen.queryByText('Member removed.')).not.toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('shows an error with a retry when the list cannot be loaded', async () => {
    const state = backend('OWNER', 'fail');
    renderPage();
    const retry = await screen.findByRole('button', { name: /try again|retry/i });
    expect(screen.queryByText(/members?$/)).not.toBeInTheDocument();
    const before = state.memberRequests;
    await userEvent.click(retry);
    await waitFor(() => expect(state.memberRequests).toBeGreaterThan(before));
  });
});
