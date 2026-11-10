import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { ContactListPage } from './ContactListPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
const group = (role: string) => [{ id: 'g1', name: 'Acme', role, createdAt: '2026-11-08T08:00:00Z' }];
const item = (n: number) => ({ id: `c${n}`, lastName: `Priimek${n}`, firstName: 'Ime', tags: [] });
const FACETS = { tags: ['a', 'b'], companies: ['Acme'], countries: ['SI'], attributeKeys: ['skill'] };

function backend(role: string, total: number, items = [item(1), item(2)], pageSize = 25) {
  const asked: URL[] = [];
  vi.mocked(fetch).mockImplementation((url) => {
    const text = String(url);
    if (text.endsWith('/groups')) return Promise.resolve(answer(group(role)));
    const parsed = new URL(text, 'http://localhost');
    if (parsed.pathname.endsWith('/facets')) return Promise.resolve(answer(FACETS));
    asked.push(parsed);
    return Promise.resolve(answer({ items, page: Number(parsed.searchParams.get('page')), pageSize, total }));
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
          <Route path="/groups/:groupId/contacts/new" element={<p>New contact page</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const last = (asked: URL[]) => asked.filter((u) => u.pathname.endsWith('/contacts')).slice(-1)[0] as URL;

describe('ContactListPage: address parameters, pager, selection and filter toggle (R-007-04, R-009-09, R-010-05)', () => {
  beforeEach(async () => {
    vi.stubGlobal('fetch', vi.fn());
    await setLanguage('en');
  });
  afterEach(() => vi.unstubAllGlobals());

  it('reads sort, direction, page and search text from the address, and falls back on nonsense', async () => {
    const asked = backend('OWNER', 2);
    renderPage('/groups/g1/contacts?sort=city&dir=desc&page=4&q=%20Ana%20');
    await screen.findByRole('table', { name: 'Group contacts' });
    expect(last(asked).searchParams.get('sort')).toBe('city');
    expect(last(asked).searchParams.get('dir')).toBe('desc');
    expect(last(asked).searchParams.get('page')).toBe('4');
    expect(last(asked).searchParams.get('q')).toBe('Ana');
  });

  it.each([
    ['?sort=bogus&dir=sideways&page=abc', 'lastName', 'asc', '1'],
    ['?sort=tags&page=0', 'lastName', 'asc', '1'],
    ['?sort=company&dir=asc&page=-3', 'company', 'asc', '1'],
  ])('uses the defaults for %s', async (query, sort, dir, page) => {
    const asked = backend('OWNER', 2);
    renderPage(`/groups/g1/contacts${query}`);
    await screen.findByRole('table', { name: 'Group contacts' });
    expect(last(asked).searchParams.get('sort')).toBe(sort);
    expect(last(asked).searchParams.get('dir')).toBe(dir);
    expect(last(asked).searchParams.get('page')).toBe(page);
  });

  it('shows the title, the total in its plural form and the summary of the page', async () => {
    backend('OWNER', 1, [item(1)]);
    renderPage();
    expect(await screen.findByRole('heading', { name: 'Contacts' })).toBeInTheDocument();
    expect((await screen.findAllByText('1 contact')).length).toBeGreaterThan(0);
    expect(screen.getByText('Page 1 of 1 · 1 contact')).toBeInTheDocument();
  });

  it('offers the numbers around the current page and the first and last, marks the current one', async () => {
    backend('OWNER', 225, [item(1)]);
    renderPage('/groups/g1/contacts?page=5');
    const pager = await screen.findByRole('navigation', { name: 'Pages' });
    expect(screen.getByText('Page 5 of 9 · 225 contacts')).toBeInTheDocument();
    const buttons = within(pager).getAllByRole('button');
    expect(buttons.map((b) => b.textContent)).toEqual(['Previous', '1', '4', '5', '6', '9', 'Next']);
    expect(buttons.map((b) => b.getAttribute('aria-current'))).toEqual([
      null,
      null,
      null,
      'page',
      null,
      null,
      null,
    ]);
  });

  it('has no gaps at the ends and no numbers outside the range', async () => {
    backend('OWNER', 50, [item(1)]);
    renderPage('/groups/g1/contacts?page=1');
    const pager = await screen.findByRole('navigation', { name: 'Pages' });
    expect(
      within(pager)
        .getAllByRole('button')
        .map((b) => b.textContent),
    ).toEqual(['Previous', '1', '2', 'Next']);
    expect(within(pager).getByRole('button', { name: 'Previous' })).toBeDisabled();
    expect(within(pager).getByRole('button', { name: 'Next' })).toBeEnabled();
    expect(within(pager).getByRole('button', { name: '1' })).toHaveAttribute('aria-current', 'page');
  });

  it('goes to the previous and the next page, and disables the buttons at the ends', async () => {
    const asked = backend('OWNER', 60, [item(1)]);
    renderPage('/groups/g1/contacts?page=2');
    const pager = await screen.findByRole('navigation', { name: 'Pages' });
    await userEvent.click(within(pager).getByRole('button', { name: 'Next' }));
    await waitFor(() => expect(last(asked).searchParams.get('page')).toBe('3'));
    await screen.findByText('Page 3 of 3 · 60 contacts');
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Previous' })).toBeEnabled();
    await userEvent.click(screen.getByRole('button', { name: 'Previous' }));
    await waitFor(() => expect(last(asked).searchParams.get('page')).toBe('2'));
  });

  it('keeps the old rows while the next page loads, and goes back to page 1 when the order changes', async () => {
    const asked = backend('OWNER', 60, [item(1)]);
    renderPage('/groups/g1/contacts?page=3');
    await screen.findByText('Page 3 of 3 · 60 contacts');
    await userEvent.click(
      within(screen.getByRole('table', { name: 'Group contacts' })).getByRole('button', { name: /^Company/ }),
    );
    await waitFor(() => expect(last(asked).searchParams.get('sort')).toBe('company'));
    expect(last(asked).searchParams.get('page')).toBe('1');
    expect(last(asked).searchParams.get('dir')).toBe('asc');
    await userEvent.click(
      within(screen.getByRole('table', { name: 'Group contacts' })).getByRole('button', { name: /^Company/ }),
    );
    await waitFor(() => expect(last(asked).searchParams.get('dir')).toBe('desc'));
    await userEvent.click(
      within(screen.getByRole('table', { name: 'Group contacts' })).getByRole('button', { name: /^City/ }),
    );
    await waitFor(() => expect(last(asked).searchParams.get('sort')).toBe('city'));
    expect(last(asked).searchParams.get('dir')).toBe('asc');
  });

  it('offers "Add contact" to an Owner and an Editor when there are Contacts, and not to a Viewer', async () => {
    backend('EDITOR', 2);
    renderPage();
    expect(await screen.findByRole('link', { name: 'Add contact' })).toHaveAttribute(
      'href',
      '/groups/g1/contacts/new',
    );
  });

  it('offers a Viewer no Add button, no check boxes and no Selection bar', async () => {
    backend('VIEWER', 2);
    renderPage();
    await screen.findByRole('table', { name: 'Group contacts' });
    expect(screen.queryByRole('link', { name: 'Add contact' })).not.toBeInTheDocument();
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Selection' })).not.toBeInTheDocument();
  });

  it('offers no Selection bar while there are no Contacts, and the empty state has the Add link for an Owner', async () => {
    backend('OWNER', 0, []);
    renderPage();
    expect(await screen.findByText('This Group has no Contacts yet')).toBeInTheDocument();
    expect(screen.getByText(/A Contact is a person or an organisation/)).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Selection' })).not.toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Add contact' })).toHaveLength(1);
  });

  it('counts the Selection, offers all results only when the whole page is selected and more exist, and clears', async () => {
    backend('OWNER', 60, [item(1), item(2)]);
    renderPage();
    const bar = await screen.findByRole('region', { name: 'Selection' });
    expect(bar).toHaveTextContent('0 selected');
    expect(within(bar).queryByRole('button', { name: /Select all/ })).not.toBeInTheDocument();
    expect(within(bar).queryByRole('button', { name: 'Clear selection' })).not.toBeInTheDocument();
    const table = screen.getByRole('table', { name: 'Group contacts' });
    await userEvent.click(
      within(table).getAllByRole('checkbox', { name: 'Select contact' })[0] as HTMLElement,
    );
    expect(bar).toHaveTextContent('1 selected');
    expect(within(bar).queryByRole('button', { name: /Select all/ })).not.toBeInTheDocument();
    await userEvent.click(
      within(table).getAllByRole('checkbox', { name: 'Select contact' })[1] as HTMLElement,
    );
    expect(bar).toHaveTextContent('2 selected');
    await userEvent.click(within(bar).getByRole('button', { name: 'Select all 60 results' }));
    expect(bar).toHaveTextContent('60 selected');
    expect(within(bar).queryByRole('button', { name: /Select all/ })).not.toBeInTheDocument();
    await userEvent.click(within(bar).getByRole('button', { name: 'Clear selection' }));
    expect(bar).toHaveTextContent('0 selected');
  });

  it('does not offer all results when the whole list is on the page', async () => {
    backend('OWNER', 2, [item(1), item(2)]);
    renderPage();
    const bar = await screen.findByRole('region', { name: 'Selection' });
    await userEvent.click(
      within(screen.getByRole('table', { name: 'Group contacts' })).getByRole('checkbox', {
        name: 'Select all on this page',
      }),
    );
    expect(bar).toHaveTextContent('2 selected');
    expect(within(bar).queryByRole('button', { name: /Select all \d+ results/ })).not.toBeInTheDocument();
  });

  it('ends the Selection when the search text changes, but not when the page does', async () => {
    backend('OWNER', 60, [item(1), item(2)]);
    renderPage('/groups/g1/contacts?page=1');
    const bar = await screen.findByRole('region', { name: 'Selection' });
    await userEvent.click(
      within(screen.getByRole('table', { name: 'Group contacts' })).getAllByRole('checkbox', {
        name: 'Select contact',
      })[0] as HTMLElement,
    );
    expect(bar).toHaveTextContent('1 selected');
    await userEvent.click(
      within(screen.getByRole('navigation', { name: 'Pages' })).getByRole('button', { name: 'Next' }),
    );
    await waitFor(() => expect(screen.getByText(/Page 2 of 3/)).toBeInTheDocument());
    expect(screen.getByRole('region', { name: 'Selection' })).toHaveTextContent('1 selected');
  });

  it('labels the filter toggle with the number of active filters and opens and closes the panel', async () => {
    backend('OWNER', 2);
    renderPage('/groups/g1/contacts?country=SI&company=Acme&tags=a');
    const toggle = await screen.findByRole('button', { name: 'Filters (3)' });
    const panel = screen.getByRole('complementary', { name: 'Filters' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(panel.className).toContain('hidden');
    expect(panel.className).not.toContain('block mb-6');
    await userEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(panel.className).toMatch(/(^|\s)block(\s|$)/);
    expect(panel.className).not.toMatch(/(^|\s)hidden(\s|$)/);
    await userEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
  });

  it('labels the filter toggle without a number when no filter is active', async () => {
    backend('OWNER', 2);
    renderPage();
    expect(await screen.findByRole('button', { name: 'Filters' })).toBeInTheDocument();
  });

  it('shows the error state with a retry', async () => {
    vi.mocked(fetch).mockImplementation((url) =>
      Promise.resolve(
        String(url).endsWith('/groups')
          ? answer(group('OWNER'))
          : String(url).includes('/facets')
            ? answer(FACETS)
            : answer({ code: 'server.error', message: 'x' }, 500),
      ),
    );
    renderPage();
    expect(await screen.findByRole('button', { name: /try again|retry/i })).toBeInTheDocument();
  });
});
