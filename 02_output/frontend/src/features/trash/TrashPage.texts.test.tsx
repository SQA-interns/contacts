import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Toaster } from '@/components/ui/sonner';
import { setLanguage } from '@/i18n';
import { TrashPage } from './TrashPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
const group = (role: string) => [{ id: 'g1', name: 'Acme', role, createdAt: '2026-11-08T08:00:00Z' }];

const ana = {
  id: 'c1',
  firstName: 'Ana',
  lastName: 'Novak',
  company: 'Acme d.o.o.',
  email: 'ana@acme.com',
  deletedAt: '2026-11-08T08:00:00Z',
  deletedBy: 'Boris Kos',
  daysLeft: 30,
};
const bare = {
  id: 'c2',
  email: 'x@y.si',
  deletedAt: '2026-01-02T08:00:00Z',
  deletedBy: null,
  company: null,
  daysLeft: 1,
};
const nobody = { id: 'c3', deletedAt: '2026-11-07T23:30:00Z', daysLeft: 2 };

interface State {
  listRequests: number;
  sent: { method: string; url: string }[];
}

function backend(
  role: string,
  items: unknown[] | 'fail',
  change?: (method: string, url: string) => Response,
): State {
  const state: State = { listRequests: 0, sent: [] };
  vi.mocked(fetch).mockImplementation((url, init) => {
    const method = init?.method ?? 'GET';
    const path = String(url);
    if (method === 'GET' && path.endsWith('/groups')) return Promise.resolve(answer(group(role)));
    if (method === 'GET') {
      state.listRequests++;
      return Promise.resolve(
        items === 'fail' ? answer({ code: 'server.error', message: 'x' }, 500) : answer(items),
      );
    }
    state.sent.push({ method, url: path });
    return Promise.resolve(
      change
        ? change(method, path)
        : method === 'DELETE'
          ? new Response(null, { status: 204 })
          : answer({ id: 'c1' }),
    );
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
      <MemoryRouter initialEntries={['/groups/g1/trash']}>
        <Routes>
          <Route
            path="/groups/:groupId/trash"
            element={
              <>
                <TrashPage />
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

describe('TrashPage: texts, names, errors and busy states (R-012-03..05, R-013-01..03)', () => {
  beforeEach(async () => {
    await setLanguage('en');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('shows the title, the explanation, the table headers and a link back to the contacts', async () => {
    backend('OWNER', [ana]);
    renderPage();
    const table = await screen.findByRole('table', { name: 'Contacts in the trash' });
    expect(screen.getByRole('heading', { name: 'Trash' })).toBeInTheDocument();
    expect(screen.getByText(/deleted permanently after 30 days/)).toBeInTheDocument();
    expect(
      within(table)
        .getAllByRole('columnheader')
        .map((h) => h.textContent),
    ).toEqual(['Contact', 'Company', 'Deleted by', 'Deleted', 'Days left', 'Actions']);
    expect(screen.getByRole('link', { name: '← Contacts' })).toHaveAttribute('href', '/groups/g1/contacts');
  });

  it('names a Contact by name, else company, else e-mail, else the untitled text, and fills the empty cells with a dash', async () => {
    backend('OWNER', [ana, bare, nobody, { ...bare, id: 'c4', email: null, company: 'Firma' }]);
    renderPage();
    const table = await screen.findByRole('table', { name: 'Contacts in the trash' });
    const rows = within(table).getAllByRole('row').slice(1);
    const cells = rows.map((r) =>
      within(r)
        .getAllByRole('cell')
        .map((c) => c.textContent),
    );
    expect(cells[0]).toEqual([
      'Ana Novak',
      'Acme d.o.o.',
      'Boris Kos',
      '2026-11-08',
      '30 days',
      'RestoreDelete forever',
    ]);
    expect(cells[1]?.slice(0, 5)).toEqual(['x@y.si', '—', '—', '2026-01-02', '1 day']);
    expect(cells[2]?.slice(0, 5)).toEqual(['Contact without a name', '—', '—', '2026-11-08', '2 days']);
    expect(cells[3]?.[0]).toBe('Firma');
    const titles = rows.map((r) =>
      within(r)
        .getAllByRole('cell')
        .map((c) => c.getAttribute('title')),
    );
    expect(titles[0]?.slice(0, 2)).toEqual(['Ana Novak', 'Acme d.o.o.']);
    expect(titles[1]?.slice(0, 2)).toEqual(['x@y.si', '']);
    const hidden = within(rows[0] as HTMLElement)
      .getAllByRole('cell')
      .map((c) => c.className.includes('lg:table-cell'));
    expect(hidden).toEqual([false, true, false, true, false, false]);
    expect(
      within(table)
        .getAllByRole('columnheader')
        .map((h) => h.className.includes('lg:table-cell')),
    ).toEqual([false, true, false, true, false, false]);
  });

  it('labels the buttons with the name and shows the card of each Contact', async () => {
    backend('OWNER', [ana, bare]);
    renderPage();
    const list = await screen.findByRole('list', { name: 'Contacts in the trash' });
    const cards = within(list).getAllByRole('listitem');
    expect(cards[0]).toHaveTextContent('Ana Novak');
    expect(cards[0]).toHaveTextContent('Deleted by Boris Kos, 2026-11-08');
    expect(cards[0]).toHaveTextContent('30 days');
    expect(cards[1]).toHaveTextContent('Deleted by —, 2026-01-02');
    expect(
      within(cards[0] as HTMLElement).getByRole('button', { name: 'Restore Ana Novak' }),
    ).toBeInTheDocument();
    expect(
      within(cards[0] as HTMLElement).getByRole('button', { name: 'Delete Ana Novak forever' }),
    ).toBeInTheDocument();
    expect(
      within(cards[1] as HTMLElement).getByRole('button', { name: 'Restore x@y.si' }),
    ).toBeInTheDocument();
  });

  it('shows the empty state with its explanation', async () => {
    backend('EDITOR', []);
    renderPage();
    expect(await screen.findByText('The trash is empty')).toBeInTheDocument();
    expect(screen.getByText('Deleted contacts stay here for 30 days.')).toBeInTheDocument();
  });

  it('shows an error with a retry that asks again', async () => {
    const state = backend('OWNER', 'fail');
    renderPage();
    const retry = await screen.findByRole('button', { name: /try again|retry/i });
    const before = state.listRequests;
    await userEvent.click(retry);
    await waitFor(() => expect(state.listRequests).toBeGreaterThan(before));
  });

  it('shows a Viewer a refusal and does not ask for the trash', async () => {
    const state = backend('VIEWER', [ana]);
    renderPage();
    expect(
      await screen.findByText('You do not have permission to open the trash. Viewers cannot.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Trash' })).toBeInTheDocument();
    expect(state.listRequests).toBe(0);
  });

  it('restores, says so with a way to open the Contact, and asks for the list again', async () => {
    const state = backend('EDITOR', [ana]);
    renderPage();
    const before = state.listRequests;
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Restore Ana Novak' }))[0] as HTMLElement,
    );
    expect(await screen.findByText('The contact was restored.')).toBeInTheDocument();
    expect(state.sent).toEqual([
      { method: 'POST', url: expect.stringMatching(/\/groups\/g1\/trash\/contacts\/c1\/restore$/) as string },
    ]);
    await waitFor(() => expect(state.listRequests).toBeGreaterThan(before));
    fireEvent.click(await screen.findByRole('button', { name: 'Open contact' }));
    expect(screen.getByTestId('where')).toHaveTextContent('/groups/g1/contacts/c1');
  });

  it('disables only the Restore button of the Contact being restored', async () => {
    let finish: (r: Response) => void = () => undefined;
    vi.mocked(fetch).mockImplementation((url, init) => {
      if ((init?.method ?? 'GET') === 'GET') {
        return Promise.resolve(answer(String(url).endsWith('/groups') ? group('OWNER') : [ana, bare]));
      }
      return new Promise<Response>((resolve) => (finish = resolve));
    });
    renderPage();
    const list = await screen.findByRole('table', { name: 'Contacts in the trash' });
    await userEvent.click(within(list).getByRole('button', { name: 'Restore Ana Novak' }));
    await waitFor(() =>
      expect(within(list).getByRole('button', { name: 'Restore Ana Novak' })).toBeDisabled(),
    );
    expect(within(list).getByRole('button', { name: 'Restore x@y.si' })).toBeEnabled();
    finish(answer({ id: 'c1' }));
    await waitFor(() =>
      expect(within(list).getByRole('button', { name: 'Restore Ana Novak' })).toBeEnabled(),
    );
  });

  it.each([
    ['restore', 'Restore Ana Novak'],
    ['delete', 'Delete Ana Novak forever'],
  ])('shows the refusal of the %s above the list', async (_what, button) => {
    backend('OWNER', [ana], (method) =>
      method === 'POST' || method === 'DELETE'
        ? answer({ code: 'contact.not_found', message: 'x' }, 404)
        : answer({}),
    );
    renderPage();
    await userEvent.click((await screen.findAllByRole('button', { name: button }))[0] as HTMLElement);
    if (button.startsWith('Delete')) {
      await userEvent.click(
        within(await screen.findByRole('dialog')).getByRole('button', { name: /Delete forever|Delete/ }),
      );
    }
    expect(await screen.findByRole('alert')).toHaveTextContent('Contact not found.');
  });

  it('asks before deleting forever, deletes the chosen Contact only and closes the question', async () => {
    const state = backend('OWNER', [ana, bare]);
    renderPage();
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Delete x@y.si forever' }))[0] as HTMLElement,
    );
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText('Delete the contact forever?')).toBeInTheDocument();
    expect(state.sent).toEqual([]);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(state.sent).toEqual([]);
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Delete x@y.si forever' }))[0] as HTMLElement,
    );
    await userEvent.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: /Delete forever|Delete/ }),
    );
    await waitFor(() => expect(state.sent).toHaveLength(1));
    expect(state.sent[0]).toEqual({
      method: 'DELETE',
      url: expect.stringMatching(/\/groups\/g1\/trash\/contacts\/c2$/) as string,
    });
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });
});
