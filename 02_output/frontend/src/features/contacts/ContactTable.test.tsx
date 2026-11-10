import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { ContactListItem } from '@/api/contacts';
import { setLanguage } from '@/i18n';
import { ContactTable, type Sorting } from './ContactTable';
import type { Selection } from './selection/useSelection';

const ana: ContactListItem = {
  id: 'c1',
  firstName: 'Ana',
  lastName: 'Novak',
  email: 'ana@acme.com',
  company: 'Acme',
  city: 'Kranj',
  country: 'SI',
  tags: ['vip', 'ljubljana'],
};
const noName: ContactListItem = { id: 'c2', email: 'x@y.si', tags: [] };
const nothing: ContactListItem = { id: 'c3', firstName: null, lastName: null, email: null, tags: [] };
const onlyFirst: ContactListItem = { id: 'c4', firstName: 'Boris', tags: [] };
const unknownCountry: ContactListItem = { id: 'c5', lastName: 'Kos', country: '!!', tags: [] };

function Where() {
  return <output data-testid="where">{useLocation().pathname}</output>;
}

function selectionStub(selected: string[] = [], overrides: Partial<Selection> = {}): Selection {
  return {
    isSelected: (id) => selected.includes(id),
    toggle: vi.fn(),
    pageSelected: (ids) => ids.length > 0 && ids.every((id) => selected.includes(id)),
    togglePage: vi.fn(),
    selectAll: vi.fn(),
    clear: vi.fn(),
    count: () => selected.length,
    allResults: false,
    ticked: selected,
    unticked: [],
    ...overrides,
  };
}

function renderTable(
  rows: ContactListItem[],
  options: { sorting?: Sorting; onSort?: (c: string) => void; selection?: Selection } = {},
) {
  return render(
    <MemoryRouter initialEntries={['/groups/g%201/contacts']}>
      <Routes>
        <Route
          path="*"
          element={
            <>
              <ContactTable
                groupId="g 1"
                rows={rows}
                sorting={options.sorting ?? { column: 'lastName', dir: 'asc' }}
                onSort={options.onSort ?? (() => undefined)}
                selection={options.selection}
              />
              <Where />
            </>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

const table = () => screen.getByRole('table', { name: 'Group contacts' });
const list = () => screen.getByRole('list', { name: 'Group contacts' });

describe('ContactTable WF-06 (R-007-01..05)', () => {
  beforeEach(() => setLanguage('en'));

  it('has a column header for each of the seven columns; six of them can sort', () => {
    renderTable([ana]);
    const headers = within(table()).getAllByRole('columnheader');
    expect(headers.map((h) => h.textContent?.replace(/[▲▼]/g, ''))).toEqual([
      'Last name',
      'First name',
      'E-mail',
      'Company',
      'City',
      'Country',
      'Tags',
    ]);
    expect(within(table()).getAllByRole('button')).toHaveLength(6);
    expect(within(headers[6] as HTMLElement).queryByRole('button')).not.toBeInTheDocument();
  });

  it('marks only the sorted column, ascending with an up arrow and descending with a down arrow', () => {
    const { unmount } = renderTable([ana], { sorting: { column: 'email', dir: 'asc' } });
    let headers = within(table()).getAllByRole('columnheader');
    expect(headers.map((h) => h.getAttribute('aria-sort'))).toEqual([
      null,
      null,
      'ascending',
      null,
      null,
      null,
      null,
    ]);
    expect(headers.map((h) => h.textContent?.match(/[▲▼]/)?.[0] ?? '')).toEqual([
      '',
      '',
      '▲',
      '',
      '',
      '',
      '',
    ]);
    unmount();
    renderTable([ana], { sorting: { column: 'city', dir: 'desc' } });
    headers = within(table()).getAllByRole('columnheader');
    expect(headers.map((h) => h.getAttribute('aria-sort'))).toEqual([
      null,
      null,
      null,
      null,
      'descending',
      null,
      null,
    ]);
    expect(headers.map((h) => h.textContent?.match(/[▲▼]/)?.[0] ?? '')).toEqual([
      '',
      '',
      '',
      '',
      '▼',
      '',
      '',
    ]);
  });

  it('asks for the sort by the column of the clicked header', async () => {
    const onSort = vi.fn();
    renderTable([ana], { onSort });
    await userEvent.click(within(table()).getByRole('button', { name: 'Company' }));
    expect(onSort).toHaveBeenCalledExactlyOnceWith('company');
    await userEvent.click(within(table()).getByRole('button', { name: /Country/ }));
    expect(onSort).toHaveBeenLastCalledWith('country');
  });

  it('hides City and Country from a tablet in the header and in every row, and no other column', () => {
    renderTable([ana, noName]);
    const hidden = (el: Element) => el.className.includes('hidden lg:table-cell');
    expect(within(table()).getAllByRole('columnheader').map(hidden)).toEqual([
      false,
      false,
      false,
      false,
      true,
      true,
      false,
    ]);
    for (const row of within(table()).getAllByRole('row').slice(1)) {
      expect(within(row).getAllByRole('cell').map(hidden)).toEqual([
        false,
        false,
        false,
        false,
        true,
        true,
        false,
      ]);
    }
  });

  it('shows the values, the country by its name, the Tags as chips and the full text as a title', () => {
    renderTable([ana]);
    const row = within(table()).getAllByRole('row')[1] as HTMLElement;
    const cells = within(row).getAllByRole('cell');
    expect(cells.map((c) => c.textContent)).toEqual([
      'Novak',
      'Ana',
      'ana@acme.com',
      'Acme',
      'Kranj',
      'Slovenia',
      'vipljubljana',
    ]);
    expect(cells.map((c) => c.getAttribute('title'))).toEqual([
      'Novak',
      'Ana',
      'ana@acme.com',
      'Acme',
      'Kranj',
      'Slovenia',
      'vip, ljubljana',
    ]);
    expect(
      within(cells[6] as HTMLElement)
        .getAllByText(/vip|ljubljana/)
        .map((c) => c.getAttribute('title')),
    ).toEqual(['vip', 'ljubljana']);
  });

  it('names a Contact by last name, else first name, else e-mail, else "(no name)"', () => {
    renderTable([ana, onlyFirst, noName, nothing]);
    const links = within(table()).getAllByRole('link');
    expect(links.map((l) => l.textContent)).toEqual(['Novak', 'Boris', 'x@y.si', '(no name)']);
    expect(links.map((l) => l.getAttribute('href'))).toEqual([
      '/groups/g%201/contacts/c1',
      '/groups/g%201/contacts/c4',
      '/groups/g%201/contacts/c2',
      '/groups/g%201/contacts/c3',
    ]);
  });

  it('leaves empty values empty and writes the code of a country the browser cannot name', () => {
    renderTable([nothing, unknownCountry]);
    const rows = within(table()).getAllByRole('row').slice(1);
    expect(
      within(rows[0] as HTMLElement)
        .getAllByRole('cell')
        .map((c) => c.textContent),
    ).toEqual(['(no name)', '', '', '', '', '', '']);
    expect(
      within(rows[0] as HTMLElement)
        .getAllByRole('cell')
        .map((c) => c.getAttribute('title')),
    ).toEqual(['', '', '', '', '', '', '']);
    expect(within(rows[1] as HTMLElement).getAllByRole('cell')[5]).toHaveTextContent('!!');
  });

  it('opens the Contact when the row is clicked, and once when the name is clicked', async () => {
    renderTable([ana, noName]);
    await userEvent.click(within(table()).getAllByRole('cell')[3] as HTMLElement);
    expect(screen.getByTestId('where')).toHaveTextContent('/groups/g%201/contacts/c1');
    await userEvent.click(within(table()).getByRole('link', { name: 'x@y.si' }));
    expect(screen.getByTestId('where')).toHaveTextContent('/groups/g%201/contacts/c2');
  });

  it('has no check boxes without a Selection', () => {
    renderTable([ana]);
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
  });

  it('shows a check box per row and one for the page, in the table and in the cards (R-010-01, R-010-02)', () => {
    renderTable([ana, noName], { selection: selectionStub(['c1']) });
    expect(
      within(table())
        .getAllByRole('checkbox', { name: 'Select contact' })
        .map((b) => b.getAttribute('data-state')),
    ).toEqual(['checked', 'unchecked']);
    expect(screen.getAllByRole('checkbox', { name: 'Select all on this page' })).toHaveLength(2);
    expect(within(list()).getAllByRole('checkbox', { name: 'Select contact' })).toHaveLength(2);
  });

  it('shows the page box checked, half-checked or empty by how much of the page is selected', () => {
    const state = (selected: string[], rows = [ana, noName]) => {
      const { unmount } = renderTable(rows, { selection: selectionStub(selected) });
      const states = screen
        .getAllByRole('checkbox', { name: 'Select all on this page' })
        .map((b) => b.getAttribute('data-state'));
      unmount();
      return states;
    };
    expect(state(['c1', 'c2'])).toEqual(['checked', 'checked']);
    expect(state(['c2'])).toEqual(['indeterminate', 'indeterminate']);
    expect(state([])).toEqual(['unchecked', 'unchecked']);
    expect(state([], [])).toEqual(['unchecked', 'unchecked']);
  });

  it('ticks a Contact without opening it, and the page box ticks the page', async () => {
    const selection = selectionStub();
    renderTable([ana, noName], { selection });
    await userEvent.click(
      within(table()).getAllByRole('checkbox', { name: 'Select contact' })[1] as HTMLElement,
    );
    expect(selection.toggle).toHaveBeenCalledExactlyOnceWith('c2');
    expect(screen.getByTestId('where')).toHaveTextContent('/groups/g%201/contacts');
    expect(screen.getByTestId('where').textContent).not.toMatch(/c2/);
    await userEvent.click(within(table()).getByRole('checkbox', { name: 'Select all on this page' }));
    expect(selection.togglePage).toHaveBeenCalledExactlyOnceWith(['c1', 'c2']);
    await userEvent.click(
      screen.getAllByRole('checkbox', { name: 'Select all on this page' })[1] as HTMLElement,
    );
    expect(selection.togglePage).toHaveBeenCalledTimes(2);
    await userEvent.click(
      within(list()).getAllByRole('checkbox', { name: 'Select contact' })[0] as HTMLElement,
    );
    expect(selection.toggle).toHaveBeenLastCalledWith('c1');
  });

  it('shows a card per Contact below 768 px with the name and the other five values', () => {
    renderTable([ana, onlyFirst, nothing]);
    const cards = within(list()).getAllByRole('listitem');
    expect(cards).toHaveLength(3);
    expect(within(cards[0] as HTMLElement).getByRole('link')).toHaveTextContent('Novak Ana');
    expect(within(cards[1] as HTMLElement).getByRole('link')).toHaveTextContent('Boris');
    expect(within(cards[2] as HTMLElement).getByRole('link')).toHaveTextContent('(no name)');
    expect(within(cards[0] as HTMLElement).getByRole('link')).toHaveAttribute('title', 'Novak');
    const labels = Array.from((cards[0] as HTMLElement).querySelectorAll('dt')).map((d) => d.textContent);
    expect(labels).toEqual(['E-mail', 'Company', 'City', 'Country', 'Tags']);
    const values = Array.from((cards[0] as HTMLElement).querySelectorAll('dd')).map((d) => d.textContent);
    expect(values).toEqual(['ana@acme.com', 'Acme', 'Kranj', 'Slovenia', 'vipljubljana']);
    const titles = Array.from((cards[0] as HTMLElement).querySelectorAll('dd')).map((d) =>
      d.getAttribute('title'),
    );
    expect(titles).toEqual(['ana@acme.com', 'Acme', 'Kranj', 'Slovenia', 'vip, ljubljana']);
  });
});
