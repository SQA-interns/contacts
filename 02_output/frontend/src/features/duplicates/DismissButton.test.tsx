import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Toaster } from '@/components/ui/sonner';
import { setLanguage } from '@/i18n';
import { DuplicatesPage } from './DuplicatesPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

const contact = (id: string, firstName: string) => ({
  id,
  firstName,
  lastName: null,
  email: 'ana@acme.com',
  company: null,
  city: null,
  country: null,
  tags: [],
  version: 1,
  createdAt: '2026-11-06T10:00:00Z',
});

const set = (key: string, ids: string[]) => ({
  key,
  mark: 'certain',
  reasonCode: 'duplicate.same_email',
  reasonParams: { email: 'ana@acme.com', count: String(ids.length) },
  contacts: ids.map((id) => contact(id, `Ana ${id}`)),
});

interface Sent {
  url: string;
  method: string;
  body: unknown;
}

/** The backend: `dismissal` answers the POST; a dismissed set leaves the next search. */
function backend(dismissal: () => Promise<Response>): { sent: Sent[]; shown: () => number } {
  const sent: Sent[] = [];
  let sets = [set('k1', ['c1', 'c2']), set('k2', ['c3', 'c4'])];
  vi.mocked(fetch).mockImplementation(async (url, init) => {
    const path = String(url);
    if (path.endsWith('/groups')) {
      return answer([{ id: 'g1', name: 'Acme', role: 'EDITOR', createdAt: '2026-11-01T00:00:00Z' }]);
    }
    const method = init?.method ?? 'GET';
    const body = init?.body ? (JSON.parse(String(init.body)) as unknown) : undefined;
    sent.push({ url: path, method, body });
    if (method === 'POST') {
      const response = await dismissal();
      if (response.ok) sets = sets.slice(1);
      return response;
    }
    return answer(sets);
  });
  return { sent, shown: () => sets.length };
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/groups/g1/duplicates']}>
        <Routes>
          <Route path="/groups/:groupId/duplicates" element={<DuplicatesPage />} />
        </Routes>
      </MemoryRouter>
      <Toaster />
    </QueryClientProvider>,
  );
}

describe('"Ni podvojeno" on WF-17 (R-025-01)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-025-01 sends the Contact ids of the set, shows a confirmation and drops the set', async () => {
    const { sent } = backend(() => Promise.resolve(new Response(null, { status: 204 })));
    renderPage();

    const buttons = await screen.findAllByRole('button', { name: 'Ni podvojeno' });
    expect(buttons).toHaveLength(2);
    await userEvent.click(buttons[0]!);

    expect(
      await screen.findByText('Množica je označena kot »ni podvojeno« in ni več prikazana.'),
    ).toBeInTheDocument();
    const post = sent.find((s) => s.method === 'POST')!;
    expect(post.url).toMatch(/\/groups\/g1\/duplicates\/dismissals$/);
    expect(post.body).toEqual({ contactIds: ['c1', 'c2'] });
    await vi.waitFor(() => expect(screen.getAllByRole('button', { name: 'Ni podvojeno' })).toHaveLength(1));
  });

  it('shows the busy state and blocks a second click while the request runs', async () => {
    let finish: (response: Response) => void = () => {};
    const { sent } = backend(() => new Promise<Response>((resolve) => (finish = resolve)));
    renderPage();

    await userEvent.click((await screen.findAllByRole('button', { name: 'Ni podvojeno' }))[0]!);

    const busy = await screen.findByRole('button', { name: 'Označevanje …' });
    expect(busy).toBeDisabled();
    await userEvent.click(busy);
    finish(new Response(null, { status: 204 }));
    await vi.waitFor(() => expect(sent.filter((s) => s.method === 'POST')).toHaveLength(1));
  });

  it('EX-025-03 says so when the set is gone and searches again', async () => {
    const { sent } = backend(() =>
      Promise.resolve(answer({ code: 'duplicate.not_found', message: 'x' }, 404)),
    );
    renderPage();

    await userEvent.click((await screen.findAllByRole('button', { name: 'Ni podvojeno' }))[0]!);

    expect(await screen.findByText('Množice podvojenih kontaktov ni mogoče najti.')).toBeInTheDocument();
    await vi.waitFor(() => expect(sent.filter((s) => s.method === 'GET').length).toBeGreaterThan(1));
  });

  it('shows the button in English', async () => {
    await setLanguage('en');
    backend(() => Promise.resolve(new Response(null, { status: 204 })));
    renderPage();

    expect((await screen.findAllByRole('button', { name: 'Not a duplicate' }))[0]).toBeInTheDocument();
  });
});
