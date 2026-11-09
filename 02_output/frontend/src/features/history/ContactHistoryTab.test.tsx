import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { ContactHistoryTab } from './ContactHistoryTab';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

function entry(overrides: Record<string, unknown> = {}) {
  return {
    id: 'e1',
    occurredAt: '2026-01-15T10:00:00Z',
    userName: 'Ana Novak',
    userEmail: 'ana@acme.com',
    action: 'CONTACT_EDITED',
    contactId: 'c1',
    details: {},
    ...overrides,
  };
}

function page(items: unknown[], extra: Record<string, unknown> = {}) {
  return { items, page: 1, pageSize: 25, total: items.length, ...extra };
}

function renderTab() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <ContactHistoryTab groupId="g1" contactId="c1" />
    </QueryClientProvider>,
  );
}

describe('ContactHistoryTab WF-07 (R-016-03, AC-016-01..03, EX-016-04)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('AC-016-03 shows the times in Europe/Ljubljana, newest first', async () => {
    vi.mocked(fetch).mockResolvedValue(
      answer(
        page([
          entry({ id: 'e2', occurredAt: '2026-07-01T10:00:00Z' }),
          entry({ id: 'e1', occurredAt: '2026-01-15T10:00:00Z' }),
        ]),
      ),
    );
    renderTab();

    const items = await screen.findAllByRole('listitem');
    expect(within(items[0] as HTMLElement).getByText('1. 7. 2026 12:00')).toBeInTheDocument();
    expect(within(items[1] as HTMLElement).getByText('15. 1. 2026 11:00')).toBeInTheDocument();
  });

  it('EX-016-04 follows the clocks going forward on 2026-03-29', async () => {
    vi.mocked(fetch).mockResolvedValue(
      answer(
        page([
          entry({ id: 'e2', occurredAt: '2026-03-29T01:30:00Z' }),
          entry({ id: 'e1', occurredAt: '2026-03-29T00:30:00Z' }),
        ]),
      ),
    );
    renderTab();

    expect(await screen.findByText('29. 3. 2026 03:30')).toBeInTheDocument();
    expect(screen.getByText('29. 3. 2026 01:30')).toBeInTheDocument();
  });

  it('AC-019-04 shows the English date format after the language is switched', async () => {
    await setLanguage('en');
    vi.mocked(fetch).mockResolvedValue(answer(page([entry({ occurredAt: '2026-11-08T10:00:00Z' })])));
    renderTab();

    expect(await screen.findByText('2026-11-08 11:00')).toBeInTheDocument();
  });

  it('R-016-03 shows action, user name and e-mail address', async () => {
    vi.mocked(fetch).mockResolvedValue(answer(page([entry()])));
    renderTab();

    expect(await screen.findByText('Urejeno')).toBeInTheDocument();
    expect(screen.getByText(/Ana Novak/)).toBeInTheDocument();
    expect(screen.getByText(/ana@acme\.com/)).toBeInTheDocument();
  });

  it('R-023-02 and AC-023-02 an entry of a restore names the Version and shows company Cezar → Acme', async () => {
    vi.mocked(fetch).mockResolvedValue(
      answer(
        page([
          entry({
            action: 'VERSION_RESTORED',
            details: { restored: 1, changes: { company: { old: 'Cezar', new: 'Acme' } } },
          }),
        ]),
      ),
    );
    renderTab();

    expect(await screen.findByText('Obnovljena različica 1')).toBeInTheDocument();
    expect(screen.getByText('Podjetje: Cezar → Acme')).toBeInTheDocument();
  });

  it('AC-016-02 and EX-016-02 show old and new value per changed field, empty as a dash', async () => {
    vi.mocked(fetch).mockResolvedValue(
      answer(
        page([
          entry({
            details: {
              changes: {
                lastName: { old: 'Novak', new: 'Kos' },
                jobTitle: { old: null, new: 'Vodja' },
              },
            },
          }),
        ]),
      ),
    );
    renderTab();

    expect(await screen.findByText('Priimek: Novak → Kos')).toBeInTheDocument();
    expect(screen.getByText('Delovno mesto: — → Vodja')).toBeInTheDocument();
    expect(screen.queryByText(/Podjetje/)).toBeNull();
  });

  it('AC-016-01 shows the stored values of a created Contact', async () => {
    vi.mocked(fetch).mockResolvedValue(
      answer(
        page([
          entry({
            action: 'CONTACT_CREATED',
            details: { values: { firstName: 'Ana', company: 'Acme' } },
          }),
        ]),
      ),
    );
    renderTab();

    expect(await screen.findByText('Ustvarjeno')).toBeInTheDocument();
    expect(screen.getByText('Ime: Ana')).toBeInTheDocument();
    expect(screen.getByText('Podjetje: Acme')).toBeInTheDocument();
  });

  it('R-018-14 shows markup in a value as plain text', async () => {
    vi.mocked(fetch).mockResolvedValue(
      answer(page([entry({ details: { changes: { notes: { old: null, new: '<script>x()</script>' } } } })])),
    );
    renderTab();

    expect(await screen.findByText(/<script>x\(\)<\/script>/)).toBeInTheDocument();
    expect(document.querySelector('script')).toBeNull();
  });

  it('EX-016-06 pages with the next and previous buttons', async () => {
    vi.mocked(fetch).mockImplementation((url) =>
      Promise.resolve(
        String(url).includes('page=2')
          ? answer(page([entry({ id: 'old', action: 'CONTACT_CREATED' })], { page: 2, total: 26 }))
          : answer(page([entry()], { total: 26 })),
      ),
    );
    renderTab();

    expect(await screen.findByText('Stran 1 od 2')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Naslednja' }));
    expect(await screen.findByText('Stran 2 od 2')).toBeInTheDocument();
    expect(screen.getByText('Ustvarjeno')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Naslednja' })).toBeDisabled();
    await userEvent.click(screen.getByRole('button', { name: 'Prejšnja' }));
    expect(await screen.findByText('Stran 1 od 2')).toBeInTheDocument();
  });

  it('offers no control that changes or deletes an entry (R-016-05)', async () => {
    vi.mocked(fetch).mockResolvedValue(answer(page([entry()])));
    renderTab();

    await screen.findByText('Urejeno');
    expect(screen.queryByRole('button', { name: /uredi|izbriši|odstrani/i })).toBeNull();
  });

  it('shows an error with a retry button', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(answer({ code: 'server.error', message: 'x' }, 500));
    vi.mocked(fetch).mockResolvedValue(answer(page([entry()])));
    renderTab();

    await userEvent.click(await screen.findByRole('button', { name: 'Poskusi znova' }));

    expect(await screen.findByText('Urejeno')).toBeInTheDocument();
  });
});
