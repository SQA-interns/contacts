import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { ActivityPage } from './ActivityPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
const joined = '2026-11-08T08:00:00Z';

const entry = (
  id: string,
  action: string,
  details: unknown,
  extra: Record<string, unknown> = {},
): Record<string, unknown> => ({
  id,
  occurredAt: '2026-11-08T10:00:00Z',
  userName: 'Ana Novak',
  userEmail: 'ana@acme.com',
  action,
  contactId: null,
  details,
  ...extra,
});

let activityRequests: URL[] = [];

function backend(role: string, items: unknown[], total = items.length, activityStatus = 200) {
  activityRequests = [];
  vi.mocked(fetch).mockImplementation((url) => {
    const path = String(url);
    if (path.endsWith('/groups')) {
      return Promise.resolve(answer([{ id: 'g1', name: 'Acme', role, createdAt: joined }]));
    }
    if (path.includes('/members')) return Promise.resolve(answer([]));
    activityRequests.push(new URL(path));
    if (activityStatus !== 200)
      return Promise.resolve(answer({ code: 'server.error', message: 'x' }, activityStatus));
    return Promise.resolve(answer({ items, page: 1, pageSize: 25, total }));
  });
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/groups/g1/activity']}>
        <Routes>
          <Route path="/groups/:groupId/activity" element={<ActivityPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

/** The cells of the data rows as text: time, user, action, contact, details. */
async function cellsOfRows(): Promise<string[][]> {
  const table = await screen.findByRole('table', { name: 'Group activity' });
  return within(table)
    .getAllByRole('row')
    .slice(1)
    .map((row) =>
      within(row)
        .getAllByRole('cell')
        .map((cell) => cell.textContent ?? ''),
    );
}

describe('ActivityPage: the Details text of every kind of entry (ADR-08, R-017-01)', () => {
  beforeEach(async () => {
    await setLanguage('en');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  const cases: [string, string, unknown, string][] = [
    ['a Tag', 'TAG_ADDED', { name: 'vip' }, 'vip'],
    ['a renamed Tag', 'TAG_RENAMED', { name: 'new', old: 'old', new: 'new' }, 'old → new'],
    ['an added Attribute', 'ATTRIBUTE_ADDED', { key: 'k', new: 'v' }, 'k: — → v'],
    ['a removed Attribute', 'ATTRIBUTE_REMOVED', { key: 'k', old: 'v' }, 'k: v → —'],
    ['a changed Attribute', 'ATTRIBUTE_CHANGED', { key: 'k', old: 'a', new: 'b' }, 'k: a → b'],
    ['an Attribute without values', 'ATTRIBUTE_ADDED', { key: 'k' }, 'k'],
    [
      'a Role change',
      'ROLE_CHANGED',
      { member: 'Boris', old: 'EDITOR', new: 'VIEWER' },
      'Boris: Editor → Viewer',
    ],
    ['a removed Member', 'MEMBER_REMOVED', { member: 'Boris', role: 'EDITOR' }, 'Boris · Editor'],
    ['a created Invitation', 'INVITATION_CREATED', { email: 'a@b.si', role: 'VIEWER' }, 'a@b.si · Viewer'],
    ['a cancelled Invitation', 'INVITATION_CANCELLED', { email: 'a@b.si' }, 'a@b.si'],
    ['a cancelled Invitation without address', 'INVITATION_CANCELLED', {}, ''],
    ['a joined Member', 'MEMBER_JOINED', { role: 'OWNER' }, 'Owner'],
    ['a joined Member with an unknown Role', 'MEMBER_JOINED', { role: 'GUEST' }, 'GUEST'],
    ['a joined Member without a Role', 'MEMBER_JOINED', {}, ''],
    ['an export', 'CONTACTS_EXPORTED', { count: 0 }, 'Contacts: 0'],
    ['a merge', 'CONTACTS_MERGED', { count: 3 }, 'Contacts: 3'],
    [
      'a creation with values',
      'CONTACT_CREATED',
      { values: { firstName: 'Ana', city: '', lastName: 'Novak' } },
      'First name: Ana · Last name: Novak',
    ],
    ['a deletion forever', 'CONTACT_DELETED_FOREVER', { values: { email: 'removed' } }, 'removed'],
    [
      'values partly removed',
      'CONTACT_EDITED',
      { values: { firstName: 'Ana', city: 'removed' } },
      'First name: Ana · City: removed',
    ],
    ['values that are all empty', 'CONTACT_CREATED', { values: { city: '' } }, ''],
    [
      'changes with empty sides',
      'CONTACT_EDITED',
      { changes: { email: { old: null, new: 'a@b.si' }, city: { old: 'Kranj' } } },
      'E-mail: — → a@b.si · City: Kranj → —',
    ],
    [
      'a change of an unknown field',
      'CONTACT_EDITED',
      { changes: { zzz: { old: 'x', new: 'y' } } },
      'zzz: x → y',
    ],
    [
      'a restored Version',
      'VERSION_RESTORED',
      { restored: 2, changes: { lastName: { old: 'A', new: 'B' } } },
      'Restored version 2 · Last name: A → B',
    ],
    ['a restored Version of the number 0', 'VERSION_RESTORED', { restored: 0 }, 'Restored version 0'],
    ['a restored text that is not a number', 'VERSION_RESTORED', { restored: '2' }, ''],
    ['no details', 'CONTACT_DELETED', null, ''],
  ];

  it.each(cases)('shows %s', async (_name, action, details, expected) => {
    backend('OWNER', [entry('e1', action, details)]);
    renderPage();
    const [cells] = await cellsOfRows();
    expect(cells?.[4]).toBe(expected);
  });

  it('shows the user as name and e-mail, the action by its name and the time of the Ljubljana zone', async () => {
    backend('OWNER', [
      entry('e1', 'TAG_ADDED', { name: 'x' }),
      entry('e2', 'TAG_ADDED', { name: 'x' }, { userName: null }),
      entry('e3', 'TAG_ADDED', { name: 'x' }, { userName: null, userEmail: null }),
    ]);
    renderPage();
    const rows = await cellsOfRows();
    expect(rows.map((r) => r[0])).toEqual(['2026-11-08 11:00', '2026-11-08 11:00', '2026-11-08 11:00']);
    expect(rows.map((r) => r[1])).toEqual(['Ana Novak · ana@acme.com', 'ana@acme.com', '']);
    expect(rows[0]?.[2]).toBe('Tag added');
  });

  it('links an entry of a Contact to it and shows a dash for the others', async () => {
    backend('EDITOR', [
      entry('e1', 'CONTACT_EDITED', null, { contactId: 'c 1' }),
      entry('e2', 'TAG_ADDED', { name: 'x' }),
    ]);
    renderPage();
    const links = await screen.findAllByRole('link', { name: 'Open contact' });
    expect(links.length).toBeGreaterThan(0);
    for (const link of links) expect(link).toHaveAttribute('href', '/groups/g1/contacts/c%201');
    const rows = await cellsOfRows();
    expect(rows[0]?.[3]).toBe('Open contact');
    expect(rows[1]?.[3]).toBe('—');
  });

  it('pages through the entries and keeps the buttons inside the range', async () => {
    backend('OWNER', [entry('e1', 'TAG_ADDED', { name: 'x' })], 60);
    const user = userEvent.setup();
    renderPage();
    await cellsOfRows();
    expect(screen.getByText('Page 1 of 3')).toBeInTheDocument();
    const previous = () => screen.getByRole('button', { name: 'Previous' });
    const next = () => screen.getByRole('button', { name: 'Next' });
    expect(previous()).toBeDisabled();
    expect(next()).toBeEnabled();
    await user.click(next());
    await waitFor(() => expect(screen.getByText('Page 2 of 3')).toBeInTheDocument());
    expect(activityRequests.at(-1)?.searchParams.get('page')).toBe('2');
    expect(previous()).toBeEnabled();
    expect(next()).toBeEnabled();
    await user.click(next());
    await waitFor(() => expect(screen.getByText('Page 3 of 3')).toBeInTheDocument());
    expect(next()).toBeDisabled();
    expect(previous()).toBeEnabled();
    await user.click(previous());
    await waitFor(() => expect(screen.getByText('Page 2 of 3')).toBeInTheDocument());
    expect(activityRequests.at(-1)?.searchParams.get('page')).toBe('2');
  });

  it('goes back to page 1 when a filter changes', async () => {
    backend('OWNER', [entry('e1', 'TAG_ADDED', { name: 'x' })], 60);
    const user = userEvent.setup();
    renderPage();
    await cellsOfRows();
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => expect(screen.getByText('Page 2 of 3')).toBeInTheDocument());
    await user.selectOptions(screen.getByLabelText('Action'), 'Tag added');
    await waitFor(() => expect(screen.getByText('Page 1 of 3')).toBeInTheDocument());
    expect(activityRequests.at(-1)?.searchParams.get('action')).toBe('TAG_ADDED');
    expect(activityRequests.at(-1)?.searchParams.get('page')).toBe('1');
  });

  it('shows one page for an empty total, and the empty state with a button that clears the filters', async () => {
    backend('OWNER', []);
    const user = userEvent.setup();
    renderPage();
    expect(await screen.findByText('No activity matches')).toBeInTheDocument();
    expect(screen.queryByRole('navigation', { name: /page/i })).not.toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('Action'), 'Tag added');
    await waitFor(() => expect(activityRequests.at(-1)?.searchParams.get('action')).toBe('TAG_ADDED'));
    const clear = await screen.findAllByRole('button', { name: 'Clear filters' });
    expect(clear.length).toBeGreaterThan(0);
    await user.click(clear[clear.length - 1] as HTMLElement);
    await waitFor(() => expect(screen.getByLabelText('Action')).toHaveValue(''));
  });

  it('shows an error with a retry', async () => {
    backend('OWNER', [], 0, 500);
    const user = userEvent.setup();
    renderPage();
    const retry = await screen.findByRole('button', { name: /try again|retry/i });
    const before = activityRequests.length;
    await user.click(retry);
    await waitFor(() => expect(activityRequests.length).toBeGreaterThan(before));
  });

  it('does not ask for the entries of a Viewer, and asks nothing when the dates are reversed', async () => {
    backend('VIEWER', []);
    renderPage();
    expect(await screen.findByText(/do not have permission/i)).toBeInTheDocument();
    expect(activityRequests).toEqual([]);
  });

  it('does not ask for entries while the start date is after the end date, and says so', async () => {
    backend('OWNER', [entry('e1', 'TAG_ADDED', { name: 'x' })]);
    const user = userEvent.setup();
    renderPage();
    await cellsOfRows();
    const count = activityRequests.length;
    await user.type(screen.getByLabelText('To'), '2026-11-01');
    await waitFor(() => expect(activityRequests.length).toBeGreaterThan(count));
    const now = activityRequests.length;
    await user.type(screen.getByLabelText('From'), '2026-11-05');
    expect(await screen.findByText('The start date is after the end date.')).toBeInTheDocument();
    expect(screen.getByLabelText('From')).toHaveAttribute('aria-invalid', 'true');
    expect(activityRequests.length).toBe(now);
  });

  it('accepts equal dates', async () => {
    backend('OWNER', [entry('e1', 'TAG_ADDED', { name: 'x' })]);
    const user = userEvent.setup();
    renderPage();
    await cellsOfRows();
    await user.type(screen.getByLabelText('To'), '2026-11-05');
    await user.type(screen.getByLabelText('From'), '2026-11-05');
    expect(screen.queryByText('The start date is after the end date.')).not.toBeInTheDocument();
    expect(screen.getByLabelText('From')).toHaveAttribute('aria-invalid', 'false');
  });
});
