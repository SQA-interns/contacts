import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { ContactListPage } from '../ContactListPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
const FACETS = { tags: [], companies: [], countries: [], attributeKeys: [] };
const names = (page: number, size: number) =>
  Array.from({ length: size }, (_, i) => ({
    id: `c${page}-${i}`,
    lastName: `Priimek${page}-${i}`,
    firstName: 'Ime',
    tags: [],
  }));

/** The backend with `total` Contacts, 25 per page; the page and the search text come from the address. */
function backend(role: string, total: number, matching: (q: string) => number = () => total) {
  vi.mocked(fetch).mockImplementation((url) => {
    const text = String(url);
    if (text.endsWith('/groups')) {
      return Promise.resolve(answer([{ id: 'g1', name: 'Acme', role, createdAt: '2026-11-08T08:00:00Z' }]));
    }
    const parsed = new URL(text, 'http://localhost');
    if (parsed.pathname.endsWith('/facets')) return Promise.resolve(answer(FACETS));
    const page = Number(parsed.searchParams.get('page'));
    const all = matching(parsed.searchParams.get('q') ?? '');
    const size = Math.max(0, Math.min(25, all - (page - 1) * 25));
    return Promise.resolve(answer({ items: names(page, size), page, pageSize: 25, total: all }));
  });
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/groups/g1/contacts']}>
        <Routes>
          <Route path="/groups/:groupId/contacts" element={<ContactListPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const table = () => screen.getByRole('table', { name: 'Kontakti skupine' });
const rowBoxes = () => within(table()).getAllByRole('checkbox', { name: 'Izberi kontakt' });
const rowBox = (n: number) => rowBoxes()[n] as HTMLElement;
const pageBox = () => within(table()).getByRole('checkbox', { name: 'Izberi vse na tej strani' });
const bar = () => screen.getByRole('region', { name: 'Izbira' });

describe('Selection on the contact list (US-010)', () => {
  beforeEach(async () => {
    vi.stubGlobal('fetch', vi.fn());
    await setLanguage('sl');
  });
  afterEach(() => vi.unstubAllGlobals());

  it('shows "2 selected" after two rows are ticked and "1 selected" after one is unticked (AC-010-01, 06)', async () => {
    backend('EDITOR', 5);
    renderPage();
    await screen.findByRole('table');

    await userEvent.click(rowBox(0));
    await userEvent.click(rowBox(1));
    expect(bar()).toHaveTextContent('2 izbrana');

    await userEvent.click(rowBox(1));
    expect(bar()).toHaveTextContent(/\b1 izbran\b/);
  });

  it('keeps the ticked rows when the page changes and back (AC-010-02)', async () => {
    backend('OWNER', 60);
    renderPage();
    await screen.findByRole('table');
    await userEvent.click(rowBox(0));
    await userEvent.click(rowBox(1));
    const pager = screen.getByRole('navigation', { name: 'Strani' });

    await userEvent.click(within(pager).getByRole('button', { name: '2' }));
    await screen.findByText('Priimek2-0');
    expect(bar()).toHaveTextContent('2 izbrana');
    await userEvent.click(within(pager).getByRole('button', { name: '1' }));

    await screen.findByText('Priimek1-0');
    expect(rowBox(0)).toBeChecked();
    expect(rowBox(1)).toBeChecked();
    expect(rowBox(2)).not.toBeChecked();
  });

  it('selects the page, offers all 60 results and selects them (AC-010-03, 04)', async () => {
    backend('EDITOR', 60);
    renderPage();
    await screen.findByRole('table');

    await userEvent.click(pageBox());
    expect(bar()).toHaveTextContent('25 izbranih');
    await userEvent.click(within(bar()).getByRole('button', { name: 'Izberi vseh 60 zadetkov' }));

    expect(bar()).toHaveTextContent('60 izbranih');
    expect(within(bar()).queryByRole('button', { name: /Izberi vseh/ })).toBeNull();
  });

  it('offers no "select all" when the result is one page, and does when it is 26 (EX-010-01, 02)', async () => {
    backend('EDITOR', 25);
    renderPage();
    await screen.findByRole('table');
    await userEvent.click(pageBox());
    expect(bar()).toHaveTextContent('25 izbranih');
    expect(within(bar()).queryByRole('button', { name: /Izberi vseh/ })).toBeNull();
  });

  it('offers "select all 26 results" for 26 results (EX-010-02)', async () => {
    backend('EDITOR', 26);
    renderPage();
    await screen.findByRole('table');

    await userEvent.click(pageBox());

    expect(within(bar()).getByRole('button', { name: 'Izberi vseh 26 zadetkov' })).toBeInTheDocument();
  });

  it('keeps the Selection of all results when the sort order changes (AC-010-09)', async () => {
    backend('EDITOR', 60);
    renderPage();
    await screen.findByRole('table');
    await userEvent.click(pageBox());
    await userEvent.click(within(bar()).getByRole('button', { name: 'Izberi vseh 60 zadetkov' }));

    await userEvent.click(screen.getByRole('button', { name: 'Podjetje' }));

    expect(bar()).toHaveTextContent('60 izbranih');
  });

  it('clears the Selection with "Clear selection" (AC-010-08)', async () => {
    backend('EDITOR', 60);
    renderPage();
    await screen.findByRole('table');
    await userEvent.click(pageBox());
    await userEvent.click(within(bar()).getByRole('button', { name: 'Izberi vseh 60 zadetkov' }));

    await userEvent.click(within(bar()).getByRole('button', { name: 'Počisti izbiro' }));

    expect(bar()).toHaveTextContent('0 izbranih');
    expect(rowBox(0)).not.toBeChecked();
  });

  it('clears the Selection when the search text changes, also back to the earlier text (AC-010-05, EX-010-04)', async () => {
    backend('EDITOR', 60, (q) => (q === 'Kranj' ? 12 : 60));
    renderPage();
    await screen.findByRole('table');
    await userEvent.click(pageBox());
    expect(bar()).toHaveTextContent('25 izbranih');

    await userEvent.type(screen.getByLabelText('Iskanje'), 'Kranj');
    await screen.findAllByText('12 kontaktov', {}, { timeout: 4000 });
    expect(bar()).toHaveTextContent('0 izbranih');

    await userEvent.click(rowBox(0));
    await userEvent.clear(screen.getByLabelText('Iskanje'));
    await screen.findAllByText('60 kontaktov', {}, { timeout: 4000 });
    expect(bar()).toHaveTextContent('0 izbranih');
  });

  it('shows no checkbox and no Selection bar to a Viewer (AC-010-07)', async () => {
    backend('VIEWER', 5);
    renderPage();
    await screen.findByRole('table');

    expect(screen.queryAllByRole('checkbox')).toHaveLength(0);
    expect(screen.queryByRole('region', { name: 'Izbira' })).toBeNull();
  });

  it('starts empty after a reload (EX-010-03)', async () => {
    backend('EDITOR', 5);
    renderPage();
    await screen.findByRole('table');

    expect(bar()).toHaveTextContent('0 izbranih');
    expect(rowBox(0)).not.toBeChecked();
  });
});
