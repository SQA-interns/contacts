import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { ContactListPage } from './ContactListPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
const group = (role: string) => [{ id: 'g1', name: 'Acme', role, createdAt: '2026-11-08T08:00:00Z' }];

const item = (n: number, extra: object = {}) => ({
  id: `c${n}`,
  lastName: `Priimek${n}`,
  firstName: 'Ime',
  tags: [],
  ...extra,
});

/** The backend: the Group list and the Contact list; returns the requested list URLs. */
function backend(role: string, page: (url: URL) => unknown) {
  const asked: URL[] = [];
  vi.mocked(fetch).mockImplementation((url) => {
    const text = String(url);
    if (text.endsWith('/groups')) return Promise.resolve(answer(group(role)));
    const parsed = new URL(text, 'http://localhost');
    asked.push(parsed);
    return Promise.resolve(answer(page(parsed)));
  });
  return asked;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/groups/g1/contacts']}>
        <Routes>
          <Route path="/groups/:groupId/contacts" element={<ContactListPage />} />
          <Route path="/groups/:groupId/contacts/:contactId" element={<p>Contact opened</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('ContactListPage', () => {
  beforeEach(async () => {
    vi.stubGlobal('fetch', vi.fn());
    await setLanguage('sl');
  });
  afterEach(() => vi.unstubAllGlobals());

  it('shows the Contacts, the total and asks for page 1 sorted by last name (R-007-01, 02)', async () => {
    const asked = backend('OWNER', () => ({ items: [item(1), item(2)], page: 1, pageSize: 25, total: 60 }));

    renderPage();

    expect((await screen.findAllByText('60 kontaktov')).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('row')).toHaveLength(3);
    const url = asked.find((u) => u.pathname.endsWith('/contacts'));
    expect(url?.searchParams.get('page')).toBe('1');
    expect(url?.searchParams.get('sort')).toBe('lastName');
    expect(url?.searchParams.get('dir')).toBe('asc');
  });

  it('sorts ascending on the first choice of a header and descending on the next (R-007-03)', async () => {
    const asked = backend('OWNER', () => ({ items: [item(1)], page: 1, pageSize: 25, total: 1 }));
    renderPage();
    await screen.findByRole('table', { name: 'Kontakti skupine' });

    await userEvent.click(screen.getByRole('button', { name: 'Podjetje' }));
    await userEvent.click(screen.getByRole('button', { name: 'Podjetje' }));

    const last = asked.filter((u) => u.pathname.endsWith('/contacts')).slice(-1)[0];
    expect(last?.searchParams.get('sort')).toBe('company');
    expect(last?.searchParams.get('dir')).toBe('desc');
    expect(screen.getByRole('columnheader', { name: 'Podjetje' })).toHaveAttribute('aria-sort', 'descending');
  });

  it('goes to another page and keeps the order (R-007-04)', async () => {
    const asked = backend('OWNER', (url) => ({
      items: [item(1)],
      page: Number(url.searchParams.get('page')),
      pageSize: 25,
      total: 60,
    }));
    renderPage();
    const pager = await screen.findByRole('navigation', { name: 'Strani' });

    await userEvent.click(within(pager).getByRole('button', { name: '3' }));

    const last = asked.filter((u) => u.pathname.endsWith('/contacts')).slice(-1)[0];
    expect(last?.searchParams.get('page')).toBe('3');
    expect(await screen.findByText(/Stran 3 od 3/)).toBeInTheDocument();
  });

  it('opens the Contact when its row is chosen (R-007-05)', async () => {
    backend('OWNER', () => ({ items: [item(7)], page: 1, pageSize: 25, total: 1 }));
    renderPage();

    await userEvent.click(await screen.findByText('Priimek7'));

    expect(await screen.findByText('Contact opened')).toBeInTheDocument();
  });

  it('offers "Add contact" in the empty state to an Editor only (R-007-06)', async () => {
    backend('EDITOR', () => ({ items: [], page: 1, pageSize: 25, total: 0 }));
    renderPage();
    expect(await screen.findByRole('heading', { name: 'V skupini še ni kontaktov' })).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Dodaj kontakt' }).length).toBeGreaterThan(0);
  });

  it('shows a Viewer the empty state without "Add contact" (R-007-06)', async () => {
    backend('VIEWER', () => ({ items: [], page: 1, pageSize: 25, total: 0 }));
    renderPage();
    expect(await screen.findByRole('heading', { name: 'V skupini še ni kontaktov' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Dodaj kontakt' })).toBeNull();
  });

  it('shows an empty page with the total when the page is beyond the last (R-007-08)', async () => {
    backend('OWNER', () => ({ items: [], page: 99, pageSize: 25, total: 60 }));
    renderPage();
    expect((await screen.findAllByText('60 kontaktov')).length).toBeGreaterThan(0);
    expect(screen.queryByRole('heading', { name: 'V skupini še ni kontaktov' })).toBeNull();
  });

  it('shows the error state with a retry when the request fails', async () => {
    vi.mocked(fetch).mockImplementation((url) =>
      Promise.resolve(String(url).endsWith('/groups') ? answer(group('OWNER')) : answer({ code: 'x' }, 500)),
    );
    renderPage();
    expect(await screen.findByRole('button', { name: 'Poskusi znova' })).toBeInTheDocument();
  });
});
