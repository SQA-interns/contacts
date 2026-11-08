import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { GroupListPage } from './GroupListPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <GroupListPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('GroupListPage WF-05, read-only slice of D-40 (R-001-03)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-001-03 shows every Group of the User with the Role', async () => {
    vi.mocked(fetch).mockResolvedValue(
      answer([
        { id: 'g1', name: 'Acme', role: 'OWNER', createdAt: '2026-11-08T08:00:00Z' },
        { id: 'g2', name: 'Klub', role: 'VIEWER', createdAt: '2026-11-08T09:00:00Z' },
      ]),
    );
    renderPage();

    expect(await screen.findByRole('heading', { level: 2, name: 'Acme' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'Moje skupine' })).toBeInTheDocument();
    expect(screen.getByText('Lastnik')).toBeInTheDocument();
    expect(screen.getByText('Ogledovalec')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Acme' })).toHaveAttribute('href', '/groups/g1/contacts');
  });

  it('shows the loading state while the list is read', () => {
    vi.mocked(fetch).mockReturnValue(new Promise(() => undefined));
    renderPage();
    expect(screen.getByRole('status')).toHaveTextContent('Nalaganje …');
  });

  it('shows the empty state when the User has no Group', async () => {
    vi.mocked(fetch).mockResolvedValue(answer([]));
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Še nimate nobene skupine' })).toBeInTheDocument();
  });

  it('shows the error state with a retry when the list cannot be read', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(answer({ code: 'server.error', message: 'x' }, 500));
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent('Nekaj je šlo narobe. Poskusite znova.');
    vi.mocked(fetch).mockResolvedValue(answer([]));
    await userEvent.click(screen.getByRole('button', { name: 'Poskusi znova' }));
    expect(await screen.findByRole('heading', { name: 'Še nimate nobene skupine' })).toBeInTheDocument();
  });
});
