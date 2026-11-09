import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Toaster } from '@/components/ui/sonner';
import { setLanguage } from '@/i18n';
import { GroupTrashPage } from './GroupTrashPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

const acme = { id: 'g1', name: 'Acme', deletedAt: '2026-11-08T08:00:00Z', daysLeft: 30 };

interface Sent {
  url: string;
  method: string;
}

/** The backend: `GET /group-trash` lists, `POST /group-trash/{g}/restore` restores. */
function backend(trash: unknown[], restore?: () => Response): Sent[] {
  const sent: Sent[] = [];
  vi.mocked(fetch).mockImplementation((url, init) => {
    const method = init?.method ?? 'GET';
    sent.push({ url: String(url), method });
    if (method === 'POST') return Promise.resolve(restore?.() ?? answer({ ...acme, role: 'OWNER' }));
    return Promise.resolve(answer(trash));
  });
  return sent;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/group-trash']}>
        <Routes>
          <Route path="/group-trash" element={<GroupTrashPage />} />
          <Route path="/groups" element={<p>Moje skupine stran</p>} />
        </Routes>
      </MemoryRouter>
      <Toaster />
    </QueryClientProvider>,
  );
}

describe('GroupTrashPage WF-15 (R-020-04, R-020-07, R-020-08)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-020-04 lists the Group with the day of deletion and the days left', async () => {
    backend([acme, { ...acme, id: 'g2', name: 'Beta', daysLeft: 1 }]);
    renderPage();

    const row = (await screen.findAllByRole('row')).find((r) => r.textContent?.includes('Acme'));
    expect(row).toHaveTextContent('8. 11. 2026');
    expect(row).toHaveTextContent('30 dni');
    expect(screen.getAllByText('1 dan').length).toBeGreaterThan(0);
    expect(screen.getByRole('heading', { level: 1, name: 'Koš skupin' })).toBeInTheDocument();
  });

  it('R-020-04 shows an empty state when no Group is in the Group Trash', async () => {
    backend([]);
    renderPage();

    expect(await screen.findByText('Koš skupin je prazen')).toBeInTheDocument();
  });

  it('R-020-04 shows an error state with a retry when the list cannot be loaded', async () => {
    vi.mocked(fetch).mockResolvedValue(answer({ code: 'x', message: 'x' }, 500));
    renderPage();

    expect(await screen.findByRole('button', { name: 'Poskusi znova' })).toBeInTheDocument();
  });

  it('R-020-07 restores the Group and offers to open it', async () => {
    const sent = backend([acme]);
    renderPage();
    await userEvent.click((await screen.findAllByRole('button', { name: 'Obnovi Acme' }))[0]!);

    await vi.waitFor(() =>
      expect(sent.filter((s) => s.method === 'POST').map((s) => s.url)).toEqual([
        expect.stringContaining('/group-trash/g1/restore'),
      ]),
    );
    expect(await screen.findByText('Skupina je obnovljena.')).toBeInTheDocument();
  });

  it('R-020-08 says a Group of this name exists when the restore is refused', async () => {
    backend([acme], () => answer({ code: 'group.name_taken', message: 'x' }, 409));
    renderPage();
    await userEvent.click((await screen.findAllByRole('button', { name: 'Obnovi Acme' }))[0]!);

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Skupino s tem imenom že imate.');
    expect(alert).toHaveTextContent('Preimenujte ali izbrišite obstoječo skupino');
    expect(screen.getAllByText('Acme').length).toBeGreaterThan(0);
  });

  it('R-020-07 shows "not found" when the 30 days are over', async () => {
    backend([acme], () => answer({ code: 'group.not_found', message: 'x' }, 404));
    renderPage();
    await userEvent.click((await screen.findAllByRole('button', { name: 'Obnovi Acme' }))[0]!);

    expect(await screen.findByText('Skupine ni mogoče najti.')).toBeInTheDocument();
  });

  it('links back to the Group list', async () => {
    backend([]);
    renderPage();
    await userEvent.click(await screen.findByRole('link', { name: /Moje skupine/ }));

    expect(screen.getByText('Moje skupine stran')).toBeInTheDocument();
  });
});
