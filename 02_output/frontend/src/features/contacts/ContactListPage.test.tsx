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
const FACETS = {
  tags: ['a', 'b', 'udelezenec2025'],
  companies: ['Acme d.o.o.'],
  countries: ['AT', 'SI'],
  attributeKeys: ['kompetenca'],
};

function backend(role: string, page: (url: URL) => unknown) {
  const asked: URL[] = [];
  vi.mocked(fetch).mockImplementation((url) => {
    const text = String(url);
    if (text.endsWith('/groups')) return Promise.resolve(answer(group(role)));
    const parsed = new URL(text, 'http://localhost');
    if (parsed.pathname.endsWith('/facets')) return Promise.resolve(answer(FACETS));
    asked.push(parsed);
    return Promise.resolve(answer(page(parsed)));
  });
  return asked;
}

function renderPage(entry = '/groups/g1/contacts') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[entry]}>
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

  it('names the link of a Contact without a last name by its first name, e-mail or "(brez imena)" (R-007-05, F-13)', async () => {
    backend('OWNER', () => ({
      items: [
        item(1, { lastName: undefined, firstName: 'Mojca' }),
        item(2, { lastName: undefined, firstName: undefined, email: 'brez@priimka.si' }),
        item(3, { lastName: undefined, firstName: undefined }),
      ],
      page: 1,
      pageSize: 25,
      total: 3,
    }));
    renderPage();
    const table = await screen.findByRole('table', { name: 'Kontakti skupine' });

    const links = within(table).getAllByRole('link');

    expect(links.map((link) => link.textContent)).toEqual(['Mojca', 'brez@priimka.si', '(brez imena)']);
    for (const link of links) expect(link).toHaveAccessibleName();
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

  it('shows a Viewer the Contacts and no "Add contact" action (R-003-09, AC-003-07)', async () => {
    backend('VIEWER', () => ({
      items: [item(1, { lastName: 'Novak', firstName: 'Ana' })],
      page: 1,
      pageSize: 25,
      total: 1,
    }));
    renderPage();

    expect(await screen.findByRole('heading', { level: 1, name: 'Kontakti' })).toBeInTheDocument();
    expect((await screen.findAllByRole('link', { name: /Novak/ })).length).toBeGreaterThan(0);
    expect(screen.queryByRole('button', { name: 'Dodaj kontakt' })).toBeNull();
    expect(screen.queryByRole('link', { name: 'Dodaj kontakt' })).toBeNull();
  });

  it('shows an empty page with the total when the page is beyond the last (R-007-08)', async () => {
    backend('OWNER', () => ({ items: [], page: 99, pageSize: 25, total: 60 }));
    renderPage();
    expect((await screen.findAllByText('60 kontaktov')).length).toBeGreaterThan(0);
    expect(screen.queryByRole('heading', { name: 'V skupini še ni kontaktov' })).toBeNull();
  });

  it('asks for page 1 with the search text, trimmed of nothing, and shows the count (R-008-01, R-008-03)', async () => {
    const asked = backend('OWNER', (url) => ({
      items: [item(1)],
      page: Number(url.searchParams.get('page')),
      pageSize: 25,
      total: url.searchParams.get('q') ? 30 : 60,
    }));
    renderPage('/groups/g1/contacts?page=3');
    await screen.findAllByText('60 kontaktov');

    await userEvent.type(screen.getByLabelText('Iskanje'), 'ana nov');

    expect((await screen.findAllByText('30 kontaktov')).length).toBeGreaterThan(0);
    const last = asked.filter((u) => u.pathname.endsWith('/contacts')).slice(-1)[0];
    expect(last?.searchParams.get('q')).toBe('ana nov');
    expect(last?.searchParams.get('page')).toBe('1');
  });

  it('treats a search text of spaces as no search (R-008-05)', async () => {
    const asked = backend('OWNER', () => ({ items: [item(1)], page: 1, pageSize: 25, total: 1 }));
    renderPage();
    await screen.findByRole('table', { name: 'Kontakti skupine' });

    await userEvent.type(screen.getByLabelText('Iskanje'), '   ');

    await new Promise((resolve) => setTimeout(resolve, 600));
    expect(asked.some((u) => u.searchParams.has('q'))).toBe(false);
  });

  it('shows "No contacts match" and "Clear filters" that empties the search (R-008-04)', async () => {
    const asked = backend('OWNER', (url) =>
      url.searchParams.get('q')
        ? { items: [], page: 1, pageSize: 25, total: 0 }
        : { items: [item(1)], page: 1, pageSize: 25, total: 1 },
    );
    renderPage('/groups/g1/contacts?q=%3Cscript%3E');

    expect(await screen.findByRole('heading', { name: 'Noben kontakt ne ustreza' })).toBeInTheDocument();
    expect(screen.getByLabelText('Iskanje')).toHaveValue('<script>');
    expect(screen.queryByRole('heading', { name: 'V skupini še ni kontaktov' })).toBeNull();

    await userEvent.click(screen.getAllByRole('button', { name: 'Počisti filtre' }).at(-1)!);

    expect(await screen.findByRole('table', { name: 'Kontakti skupine' })).toBeInTheDocument();
    expect(screen.getByLabelText('Iskanje')).toHaveValue('');
    const last = asked.filter((u) => u.pathname.endsWith('/contacts')).slice(-1)[0];
    expect(last?.searchParams.has('q')).toBe(false);
  });

  it('shows the error state with a retry when the request fails', async () => {
    vi.mocked(fetch).mockImplementation((url) =>
      Promise.resolve(String(url).endsWith('/groups') ? answer(group('OWNER')) : answer({ code: 'x' }, 500)),
    );
    renderPage();
    expect(await screen.findByRole('button', { name: 'Poskusi znova' })).toBeInTheDocument();
  });

  const lastList = (asked: URL[]) => asked.filter((u) => u.pathname.endsWith('/contacts')).slice(-1)[0];

  it('offers the Tags, countries, companies and keys of the Group and sends the Tag filters (R-009-01..03, R-009-11)', async () => {
    const asked = backend('OWNER', () => ({ items: [item(1)], page: 1, pageSize: 25, total: 1 }));
    renderPage('/groups/g1/contacts?page=3');
    await screen.findByRole('table', { name: 'Kontakti skupine' });
    const panel = screen.getByRole('complementary', { name: 'Filtri' });

    await userEvent.click(within(panel).getAllByText('Izberite značke')[0]!);
    expect(screen.getByRole('option', { name: 'udelezenec2025' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('option', { name: 'a' }));
    await userEvent.click(screen.getByRole('option', { name: 'b' }));
    await userEvent.click(within(panel).getByLabelText('ima katero koli od teh značk'));

    await vi.waitFor(() => expect(lastList(asked)?.searchParams.getAll('tags')).toEqual(['a', 'b']));
    expect(lastList(asked)?.searchParams.get('tagMode')).toBe('any');
    expect(lastList(asked)?.searchParams.get('page')).toBe('1');
  });

  it('sends the Without-Tags filter, the country and the company (R-009-03..05)', async () => {
    const asked = backend('OWNER', () => ({ items: [item(1)], page: 1, pageSize: 25, total: 1 }));
    renderPage();
    await screen.findByRole('table', { name: 'Kontakti skupine' });
    const panel = screen.getByRole('complementary', { name: 'Filtri' });

    await userEvent.click(within(panel).getByRole('button', { name: 'Brez značk' }));
    await userEvent.click(screen.getByRole('option', { name: 'b' }));
    await userEvent.selectOptions(within(panel).getByLabelText('Država'), 'Slovenija');
    await userEvent.selectOptions(within(panel).getByLabelText('Podjetje'), 'Acme d.o.o.');

    await vi.waitFor(() => expect(lastList(asked)?.searchParams.get('company')).toBe('Acme d.o.o.'));
    expect(lastList(asked)?.searchParams.getAll('withoutTags')).toEqual(['b']);
    expect(lastList(asked)?.searchParams.get('country')).toBe('SI');
  });

  it('sends the Attribute filter with a value, or with "exists" (R-009-06, R-009-07)', async () => {
    const asked = backend('OWNER', () => ({ items: [item(1)], page: 1, pageSize: 25, total: 1 }));
    renderPage();
    await screen.findByRole('table', { name: 'Kontakti skupine' });
    const panel = screen.getByRole('complementary', { name: 'Filtri' });

    await userEvent.selectOptions(within(panel).getByLabelText('Atribut'), 'kompetenca');
    await userEvent.type(within(panel).getByLabelText('Vrednost'), 'java');

    await vi.waitFor(() => expect(lastList(asked)?.searchParams.get('attrValue')).toBe('java'));
    expect(lastList(asked)?.searchParams.get('attrKey')).toBe('kompetenca');

    await userEvent.click(within(panel).getByLabelText('obstaja'));

    await vi.waitFor(() => expect(lastList(asked)?.searchParams.get('attrExists')).toBe('true'));
    expect(lastList(asked)?.searchParams.has('attrValue')).toBe(false);
  });

  it('clears every filter and the search text with the first "Clear filters" (R-009-10)', async () => {
    const asked = backend('OWNER', () => ({ items: [item(1)], page: 1, pageSize: 25, total: 1 }));
    renderPage('/groups/g1/contacts?q=ana&country=SI&tags=a&tagMode=all');
    await screen.findByRole('table', { name: 'Kontakti skupine' });
    expect(screen.getByLabelText('Država')).toHaveValue('SI');

    await userEvent.click(screen.getAllByRole('button', { name: 'Počisti filtre' })[0]!);

    await vi.waitFor(() => expect(lastList(asked)?.searchParams.has('country')).toBe(false));
    expect(lastList(asked)?.searchParams.has('q')).toBe(false);
    expect(lastList(asked)?.searchParams.has('tags')).toBe(false);
    expect(screen.getByLabelText('Iskanje')).toHaveValue('');
  });

  it('shows "No contacts match" when a filter matches nothing, and its button clears the filters (R-009-12)', async () => {
    backend('OWNER', (url) =>
      url.searchParams.has('country')
        ? { items: [], page: 1, pageSize: 25, total: 0 }
        : { items: [item(1)], page: 1, pageSize: 25, total: 1 },
    );
    renderPage('/groups/g1/contacts?country=AT');

    expect(await screen.findByRole('heading', { name: 'Noben kontakt ne ustreza' })).toBeInTheDocument();
    await userEvent.click(screen.getAllByRole('button', { name: 'Počisti filtre' }).at(-1)!);

    expect(await screen.findByRole('table', { name: 'Kontakti skupine' })).toBeInTheDocument();
  });

  it('drops a search text that is still waiting to be sent when the filters are cleared (R-009-10)', async () => {
    const asked = backend('OWNER', () => ({ items: [item(1)], page: 1, pageSize: 25, total: 1 }));
    renderPage('/groups/g1/contacts?country=SI');
    await screen.findByRole('table', { name: 'Kontakti skupine' });

    await userEvent.type(screen.getByLabelText('Iskanje'), 'ana');
    await userEvent.click(screen.getAllByRole('button', { name: 'Počisti filtre' })[0]!);

    await new Promise((resolve) => setTimeout(resolve, 600));
    expect(screen.getByLabelText('Iskanje')).toHaveValue('');
    expect(lastList(asked)?.searchParams.has('q')).toBe(false);
  });
});
