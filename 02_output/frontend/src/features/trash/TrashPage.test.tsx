import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { TrashPage } from './TrashPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

const ana = {
  id: 'c1',
  firstName: 'Ana',
  lastName: 'Novak',
  company: 'Acme d.o.o.',
  deletedAt: '2026-11-08T08:00:00Z',
  deletedBy: 'Boris Kos',
  daysLeft: 30,
};

interface Sent {
  url: string;
  method: string;
}

/** The backend: the Group list gives the Role, `GET .../trash/contacts` the list, `POST` the restore. */
function backend(role: string, trash: unknown[], restore?: () => Response): Sent[] {
  const sent: Sent[] = [];
  vi.mocked(fetch).mockImplementation((url, init) => {
    const path = String(url);
    const method = init?.method ?? 'GET';
    if (path.endsWith('/groups')) {
      return Promise.resolve(answer([{ id: 'g1', name: 'Acme', role, createdAt: ana.deletedAt }]));
    }
    sent.push({ url: path, method });
    if (method === 'POST') return Promise.resolve(restore?.() ?? answer({ id: 'c1', version: 2 }));
    return Promise.resolve(answer(trash));
  });
  return sent;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/groups/g1/trash']}>
        <Routes>
          <Route path="/groups/:groupId/trash" element={<TrashPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('TrashPage WF-10 (R-012-03, R-012-04, R-012-08)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-012-03 lists the Contact with who deleted it and the days left', async () => {
    backend('EDITOR', [ana]);
    renderPage();

    const row = (await screen.findAllByRole('row')).find((r) => r.textContent?.includes('Novak'));
    expect(row).toBeDefined();
    expect(row).toHaveTextContent('Ana Novak');
    expect(row).toHaveTextContent('Boris Kos');
    expect(row).toHaveTextContent('30 dni');
    expect(screen.getByRole('heading', { level: 1, name: 'Koš' })).toBeInTheDocument();
  });

  it('R-012-03 counts the days with the Slovene forms', async () => {
    backend('EDITOR', [
      { ...ana, daysLeft: 1 },
      { ...ana, id: 'c2', lastName: 'Kos', daysLeft: 2 },
    ]);
    renderPage();

    expect((await screen.findAllByText('1 dan')).length).toBeGreaterThan(0);
    expect(screen.getAllByText('2 dneva').length).toBeGreaterThan(0);
  });

  it('EX-012-07 shows an empty state when nothing is in Trash', async () => {
    backend('OWNER', []);
    renderPage();

    expect(await screen.findByText('Koš je prazen')).toBeInTheDocument();
  });

  it('R-012-04 restores the Contact and lists the rest', async () => {
    const sent = backend('EDITOR', [ana]);
    renderPage();
    await userEvent.click((await screen.findAllByRole('button', { name: /Obnovi/ }))[0]!);

    await vi.waitFor(() =>
      expect(sent.filter((s) => s.method === 'POST').map((s) => s.url)).toEqual([
        expect.stringContaining('/groups/g1/trash/contacts/c1/restore'),
      ]),
    );
  });

  it('R-012-05 shows "not found" when the restore is refused', async () => {
    backend('EDITOR', [ana], () => answer({ code: 'contact.not_found', message: 'x' }, 404));
    renderPage();
    await userEvent.click((await screen.findAllByRole('button', { name: /Obnovi/ }))[0]!);

    expect(await screen.findByText('Kontakta ni mogoče najti.')).toBeInTheDocument();
  });

  it('R-012-08 shows a Viewer a refusal and asks the backend nothing', async () => {
    const sent = backend('VIEWER', [ana]);
    renderPage();

    expect(await screen.findByText(/nimate dovoljenja/i)).toBeInTheDocument();
    expect(within(document.body).queryByRole('button', { name: /Obnovi/ })).not.toBeInTheDocument();
    expect(sent).toHaveLength(0);
  });
});
