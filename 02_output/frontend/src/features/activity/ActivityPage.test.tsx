import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { ActivityPage } from './ActivityPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
const joined = '2026-11-08T08:00:00Z';
const members = [
  { userId: 'u1', displayName: 'Ana Novak', email: 'ana@acme.com', role: 'OWNER', joinedAt: joined },
  { userId: 'u2', displayName: 'Boris Kos', email: 'boris@acme.com', role: 'EDITOR', joinedAt: joined },
];
const entry = (id: string, action: string, details: unknown, userName = 'Ana Novak', contactId?: string) => ({
  id,
  occurredAt: '2026-11-08T10:00:00Z',
  userName,
  userEmail: 'ana@acme.com',
  action,
  contactId: contactId ?? null,
  details,
});
const items = [
  entry('e1', 'CONTACTS_EXPORTED', { count: 3 }),
  entry('e2', 'CONTACT_EDITED', { changes: { lastName: { old: 'Novak', new: 'Kos' } } }, 'Boris Kos', 'c1'),
  entry('e3', 'ROLE_CHANGED', { member: 'Boris Kos', email: 'boris@acme.com', old: 'EDITOR', new: 'VIEWER' }),
];

function backend(role: string, activity: unknown[] = items): string[] {
  const urls: string[] = [];
  vi.mocked(fetch).mockImplementation((url) => {
    const path = String(url);
    if (path.endsWith('/groups')) {
      return Promise.resolve(answer([{ id: 'g1', name: 'Acme', role, createdAt: joined }]));
    }
    if (path.includes('/members')) return Promise.resolve(answer(members));
    urls.push(path);
    return Promise.resolve(answer({ items: activity, page: 1, pageSize: 25, total: activity.length }));
  });
  return urls;
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

describe('ActivityPage WF-13 (R-017-01..07)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-017-01 lists time, user, action, Contact and details of every entry', async () => {
    backend('OWNER');
    renderPage();

    const table = await screen.findByRole('table', { name: 'Dejavnost skupine' });
    const rows = within(table).getAllByRole('row');
    expect(rows).toHaveLength(4);
    expect(rows[1]).toHaveTextContent('8. 11. 2026 11:00');
    expect(rows[1]).toHaveTextContent('Ana Novak');
    expect(rows[1]).toHaveTextContent('Kontakti izvoženi');
    expect(rows[1]).toHaveTextContent('3');
    expect(rows[2]).toHaveTextContent('Kontakt urejen');
    expect(rows[2]).toHaveTextContent('Priimek: Novak → Kos');
    expect(rows[3]).toHaveTextContent('Ogledovalec');
  });

  it('R-017-02, R-017-03, R-017-04, R-017-05 the chosen filters are sent together', async () => {
    const urls = backend('OWNER');
    const user = userEvent.setup();
    renderPage();
    await screen.findByRole('table', { name: 'Dejavnost skupine' });

    await user.selectOptions(await screen.findByLabelText('Uporabnik'), 'Boris Kos');
    await user.selectOptions(screen.getByLabelText('Dejanje'), 'Kontakt urejen');
    await user.type(screen.getByLabelText('Od'), '2026-11-07');
    await user.type(screen.getByLabelText('Do'), '2026-11-08');

    await waitFor(() => {
      const last = new URL(urls.at(-1) ?? '', 'http://x');
      expect(last.searchParams.get('userId')).toBe('u2');
      expect(last.searchParams.get('action')).toBe('CONTACT_EDITED');
      expect(last.searchParams.get('from')).toBe('2026-11-07');
      expect(last.searchParams.get('to')).toBe('2026-11-08');
    });
  });

  it('R-017-06 a start date after the end date shows the message and no list, and asks nothing', async () => {
    const urls = backend('OWNER');
    const user = userEvent.setup();
    renderPage();
    await screen.findByRole('table', { name: 'Dejavnost skupine' });

    await user.type(screen.getByLabelText('Od'), '2026-11-10');
    await waitFor(() =>
      expect(new URL(urls.at(-1) ?? '', 'http://x').searchParams.get('from')).toBe('2026-11-10'),
    );
    const before = urls.length;
    await user.type(screen.getByLabelText('Do'), '2026-11-01');

    expect(await screen.findByText('Začetni datum je po končnem.')).toBeInTheDocument();
    expect(screen.queryByRole('table', { name: 'Dejavnost skupine' })).toBeNull();
    expect(urls.length).toBe(before);
  });

  it('R-017-07 no match shows the message and a button that clears the filters', async () => {
    backend('OWNER', []);
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Nobena dejavnost ne ustreza' })).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('Dejanje'), 'Kontakt urejen');
    await user.click(screen.getAllByRole('button', { name: 'Počisti filtre' }).at(-1)!);

    expect(screen.getByLabelText('Dejanje')).toHaveValue('');
  });

  it('R-017-08 a Viewer sees a refusal and no request for the list is sent', async () => {
    const urls = backend('VIEWER');
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent('nimate pravice');
    expect(urls).toHaveLength(0);
  });
});
