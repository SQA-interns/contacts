import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { TagListPage } from './TagListPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
const groups = [{ id: 'g1', name: 'Acme', role: 'OWNER', createdAt: '2026-11-08T08:00:00Z' }];

interface Sent {
  method: string;
  url: string;
  body?: unknown;
}

/** The backend: the Group list, the Tag list and the answers to rename and delete. */
function backend(
  tags: unknown[],
  onChange: (sent: Sent) => Response = () => new Response(null, { status: 204 }),
) {
  const sent: Sent[] = [];
  vi.mocked(fetch).mockImplementation((url, init) => {
    const method = init?.method ?? 'GET';
    if (method === 'GET') return Promise.resolve(answer(String(url).endsWith('/groups') ? groups : tags));
    sent.push({ method, url: String(url), body: init?.body ? JSON.parse(String(init.body)) : undefined });
    return Promise.resolve(onChange(sent[sent.length - 1] as Sent));
  });
  return sent;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/groups/g1/tags']}>
        <Routes>
          <Route path="/groups/:groupId/tags" element={<TagListPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const gost = { id: 't1', name: 'gost', contactCount: 5 };
/** The table and the card list are both in the DOM; CSS shows one of them (UI-16). */
const action = async (name: RegExp) => (await screen.findAllByRole('button', { name }))[0] as HTMLElement;
const row = (name: string) =>
  screen.getAllByRole('row').find((r) => r.textContent?.includes(name)) as HTMLElement;

describe('TagListPage WF-09 (R-006-01..06, R-006-08)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-006-01 lists every Tag with its number of Contacts', async () => {
    backend([gost, { id: 't2', name: 'vip', contactCount: 0 }]);
    renderPage();

    expect((await screen.findAllByText('gost'))[0]).toBeInTheDocument();
    expect(within(row('gost')).getByText('5')).toBeInTheDocument();
    expect(within(row('vip')).getByText('0')).toBeInTheDocument();
  });

  it('shows the empty state when the Group has no Tag', async () => {
    backend([]);
    renderPage();

    expect(await screen.findByText('V skupini še ni značk')).toBeInTheDocument();
  });

  it('R-006-06 asks with the number of Contacts before deleting, and deletes on confirm', async () => {
    const sent = backend([gost]);
    renderPage();
    await userEvent.click(await action(/Izbriši/));

    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('5 kontaktov');
    expect(sent).toHaveLength(0);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Izbriši značko' }));

    await vi.waitFor(() => expect(sent).toEqual([expect.objectContaining({ method: 'DELETE' })]));
    expect(sent[0]?.url).toContain('/groups/g1/tags/t1');
  });

  it('AC-006-05 cancelling the deletion sends nothing', async () => {
    const sent = backend([gost]);
    renderPage();
    await userEvent.click(await action(/Izbriši/));
    await userEvent.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Prekliči' }),
    );

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(sent).toHaveLength(0);
  });

  it('R-006-02 renames with the typed name', async () => {
    const sent = backend([gost], () => answer({ ...gost, name: 'obiskovalec' }));
    renderPage();
    await userEvent.click(await action(/Preimenuj/));
    const dialog = await screen.findByRole('dialog', { name: 'Preimenuj značko' });
    const field = within(dialog).getByLabelText(/Novo ime/);
    await userEvent.clear(field);
    await userEvent.type(field, 'obiskovalec');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Preimenuj' }));

    await vi.waitFor(() => expect(sent).toHaveLength(1));
    expect(sent[0]).toMatchObject({ method: 'PATCH', body: { name: 'obiskovalec' } });
  });

  it('R-006-03 shows why a rename is refused and keeps the dialog open', async () => {
    backend([gost], () => answer({ code: 'tag.name_taken', message: 'x' }, 409));
    renderPage();
    await userEvent.click(await action(/Preimenuj/));
    const dialog = await screen.findByRole('dialog');
    await userEvent.type(within(dialog).getByLabelText(/Novo ime/), 'x');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Preimenuj' }));

    expect(await within(dialog).findByText('Značka s tem imenom že obstaja.')).toBeInTheDocument();
  });

  it('R-006-03 checks the name before sending it', async () => {
    const sent = backend([gost]);
    renderPage();
    await userEvent.click(await action(/Preimenuj/));
    const dialog = await screen.findByRole('dialog');
    const field = within(dialog).getByLabelText(/Novo ime/);
    await userEvent.clear(field);
    await userEvent.type(field, 'nov gost');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Preimenuj' }));

    expect(await within(dialog).findByText(/Ime značke lahko vsebuje/)).toBeInTheDocument();
    expect(sent).toHaveLength(0);
  });

  it('R-006-08 offers no action to a Viewer', async () => {
    vi.mocked(fetch).mockImplementation((url) =>
      Promise.resolve(answer(String(url).endsWith('/groups') ? [{ ...groups[0], role: 'VIEWER' }] : [gost])),
    );
    renderPage();

    expect(await screen.findByText(/nimate pravice/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Preimenuj|Izbriši/ })).not.toBeInTheDocument();
  });
});
