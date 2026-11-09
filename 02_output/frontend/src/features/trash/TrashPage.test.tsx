import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Toaster } from '@/components/ui/sonner';
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
    if (method === 'DELETE') return Promise.resolve(restore?.() ?? new Response(null, { status: 204 }));
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
      <Toaster />
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

  it('F-17 announces the restore as a status message (R-012-04, UI-13)', async () => {
    backend('EDITOR', [ana]);
    renderPage();
    await userEvent.click((await screen.findAllByRole('button', { name: /Obnovi/ }))[0]!);

    await vi.waitFor(() => {
      const messages = screen.getAllByRole('status').map((element) => element.textContent);
      expect(messages.some((text) => text?.includes('Kontakt je obnovljen.'))).toBe(true);
    });
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

  it('R-013-01, R-013-02 asks for confirmation that says it cannot be undone, then deletes forever', async () => {
    const sent = backend('OWNER', [ana]);
    renderPage();
    await userEvent.click((await screen.findAllByRole('button', { name: /Izbriši za vedno/ }))[0]!);

    const dialog = await screen.findByRole('dialog', { name: 'Trajno izbrišem kontakt?' });
    expect(dialog).toHaveTextContent('Tega ni mogoče razveljaviti.');
    expect(sent.filter((s) => s.method === 'DELETE')).toHaveLength(0);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Izbriši za vedno' }));

    await vi.waitFor(() =>
      expect(sent.filter((s) => s.method === 'DELETE').map((s) => s.url)).toEqual([
        expect.stringContaining('/groups/g1/trash/contacts/c1'),
      ]),
    );
  });

  it('R-013-02 keeps the Contact when the confirmation is cancelled', async () => {
    const sent = backend('OWNER', [ana]);
    renderPage();
    await userEvent.click((await screen.findAllByRole('button', { name: /Izbriši za vedno/ }))[0]!);
    const dialog = await screen.findByRole('dialog', { name: 'Trajno izbrišem kontakt?' });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Prekliči' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(sent.filter((s) => s.method === 'DELETE')).toHaveLength(0);
  });

  it('R-013-03 shows an Editor no "Izbriši za vedno"', async () => {
    backend('EDITOR', [ana]);
    renderPage();

    expect((await screen.findAllByRole('button', { name: /Obnovi/ })).length).toBeGreaterThan(0);
    expect(screen.queryByRole('button', { name: /Izbriši za vedno/ })).not.toBeInTheDocument();
  });

  it('R-013-05 shows "not found" when the Contact is already gone', async () => {
    backend('OWNER', [ana], () => answer({ code: 'contact.not_found', message: 'x' }, 404));
    renderPage();
    await userEvent.click((await screen.findAllByRole('button', { name: /Izbriši za vedno/ }))[0]!);
    const dialog = await screen.findByRole('dialog', { name: 'Trajno izbrišem kontakt?' });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Izbriši za vedno' }));

    expect(await screen.findByText('Kontakta ni mogoče najti.')).toBeInTheDocument();
  });
});
