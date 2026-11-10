import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Toaster } from '@/components/ui/sonner';
import { setLanguage } from '@/i18n';
import { GroupTrashPage } from './GroupTrashPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
const acme = { id: 'g 1', name: 'Acme', deletedAt: '2026-11-08T08:00:00Z', daysLeft: 30 };
const beta = { id: 'g2', name: 'Beta d.o.o.', deletedAt: '2026-11-07T23:30:00Z', daysLeft: 1 };

interface State {
  listRequests: number;
  groupRequests: number;
  sent: string[];
}

function backend(trash: unknown[] | 'fail', restore?: () => Response): State {
  const state: State = { listRequests: 0, groupRequests: 0, sent: [] };
  vi.mocked(fetch).mockImplementation((url, init) => {
    const method = init?.method ?? 'GET';
    const path = String(url).replace(/^.*\/api\/v1/, '');
    if (method === 'POST') {
      state.sent.push(`${method} ${path}`);
      return Promise.resolve(restore?.() ?? answer({ ...acme, role: 'OWNER' }));
    }
    if (path === '/groups') state.groupRequests++;
    else state.listRequests++;
    if (trash === 'fail') return Promise.resolve(answer({ code: 'server.error', message: 'x' }, 500));
    return Promise.resolve(answer(path === '/groups' ? [] : trash));
  });
  return state;
}

function Where() {
  return <output data-testid="where">{useLocation().pathname}</output>;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/group-trash']}>
        <Routes>
          <Route
            path="/group-trash"
            element={
              <>
                <GroupTrashPage />
                <Where />
              </>
            }
          />
          <Route path="*" element={<Where />} />
        </Routes>
      </MemoryRouter>
      <Toaster />
    </QueryClientProvider>,
  );
}

describe('GroupTrashPage: texts, list and Restore (R-020-04..06)', () => {
  beforeEach(async () => {
    await setLanguage('en');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('shows the title, the explanation, the link back and the table with its rows', async () => {
    backend([acme, beta]);
    renderPage();
    const table = await screen.findByRole('table', { name: 'Groups in the trash' });
    expect(screen.getByRole('heading', { name: 'Group trash' })).toBeInTheDocument();
    expect(screen.getByText(/Deleted groups of which you are an owner/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '← My groups' })).toHaveAttribute('href', '/groups');
    expect(
      within(table)
        .getAllByRole('columnheader')
        .map((h) => h.textContent),
    ).toEqual(['Group', 'Deleted', 'Days left', 'Actions']);
    const rows = within(table).getAllByRole('row').slice(1);
    expect(
      rows.map((r) =>
        within(r)
          .getAllByRole('cell')
          .map((c) => c.textContent),
      ),
    ).toEqual([
      ['Acme', '2026-11-08', '30 days', 'Restore'],
      ['Beta d.o.o.', '2026-11-08', '1 day', 'Restore'],
    ]);
    expect(within(rows[1] as HTMLElement).getByTitle('Beta d.o.o.')).toBeInTheDocument();
    expect(within(rows[0] as HTMLElement).getByRole('button', { name: 'Restore Acme' })).toBeInTheDocument();
  });

  it('shows the empty state with its explanation', async () => {
    backend([]);
    renderPage();
    expect(await screen.findByText('The group trash is empty')).toBeInTheDocument();
    expect(screen.getByText('Deleted groups stay here for 30 days.')).toBeInTheDocument();
  });

  it('shows an error with a retry that asks again', async () => {
    const state = backend('fail');
    renderPage();
    const retry = await screen.findByRole('button', { name: /try again|retry/i });
    const before = state.listRequests;
    await userEvent.click(retry);
    await waitFor(() => expect(state.listRequests).toBeGreaterThan(before));
  });

  it('restores the chosen Group, says so with a way to open it and asks for the list again', async () => {
    const state = backend([acme, beta]);
    renderPage();
    const before = { list: state.listRequests };
    await userEvent.click((await screen.findAllByRole('button', { name: 'Restore Acme' }))[0] as HTMLElement);
    expect(await screen.findByText('The group is restored.')).toBeInTheDocument();
    expect(state.sent).toEqual(['POST /group-trash/g%201/restore']);
    await waitFor(() => expect(state.listRequests).toBeGreaterThan(before.list));
    fireEvent.click(await screen.findByRole('button', { name: 'Open group' }));
    expect(screen.getByTestId('where')).toHaveTextContent('/groups/g 1/contacts');
  });

  it('disables only the button of the Group being restored', async () => {
    let finish: (r: Response) => void = () => undefined;
    vi.mocked(fetch).mockImplementation((url, init) => {
      if ((init?.method ?? 'GET') === 'POST') return new Promise<Response>((resolve) => (finish = resolve));
      return Promise.resolve(answer(String(url).endsWith('/groups') ? [] : [acme, beta]));
    });
    renderPage();
    const table = await screen.findByRole('table', { name: 'Groups in the trash' });
    await userEvent.click(within(table).getByRole('button', { name: 'Restore Acme' }));
    await waitFor(() => expect(within(table).getByRole('button', { name: 'Restore Acme' })).toBeDisabled());
    expect(within(table).getByRole('button', { name: 'Restore Beta d.o.o.' })).toBeEnabled();
    finish(answer({ ...acme, role: 'OWNER' }));
    await waitFor(() => expect(within(table).getByRole('button', { name: 'Restore Acme' })).toBeEnabled());
  });

  it('shows the hint about the name when the name is taken, and only then', async () => {
    backend([acme], () => answer({ code: 'group.name_taken', message: 'x' }, 409));
    renderPage();
    await userEvent.click((await screen.findAllByRole('button', { name: 'Restore Acme' }))[0] as HTMLElement);
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Rename or delete the existing group, then try again.');
    expect(screen.queryByText('The group is restored.')).not.toBeInTheDocument();
  });

  it('shows another refusal without the hint', async () => {
    backend([acme], () => answer({ code: 'group.not_found', message: 'x' }, 404));
    renderPage();
    await userEvent.click((await screen.findAllByRole('button', { name: 'Restore Acme' }))[0] as HTMLElement);
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Group not found.');
    expect(alert).not.toHaveTextContent('Rename or delete the existing group');
  });
});
