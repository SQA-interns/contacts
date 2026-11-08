import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { GroupListPage } from './GroupListPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
const acme = { id: 'g1', name: 'Acme', role: 'OWNER', createdAt: '2026-11-08T08:00:00Z' };

/** The backend: `GET /groups` answers the current list, `POST /groups` answers `created`. */
function backend(created: () => Response) {
  const groups: unknown[] = [];
  const posted: unknown[] = [];
  vi.mocked(fetch).mockImplementation((_url, init) => {
    if (init?.method !== 'POST') return Promise.resolve(answer(groups));
    posted.push(JSON.parse(String(init.body)));
    const response = created();
    if (response.ok) groups.push(acme);
    return Promise.resolve(response);
  });
  return posted;
}

async function openCreateDialog() {
  const [createButton] = await screen.findAllByRole('button', { name: 'Ustvari skupino' });
  await userEvent.click(createButton as HTMLElement);
  return screen.findByRole('dialog', { name: 'Nova skupina' });
}

async function submitName(name: string) {
  const dialog = await openCreateDialog();
  const field = within(dialog).getByLabelText(/Ime skupine/);
  await userEvent.clear(field);
  if (name) await userEvent.type(field, name);
  await userEvent.click(within(dialog).getByRole('button', { name: 'Ustvari' }));
  return dialog;
}

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

describe('GroupListPage WF-05 (R-001-03, R-002-05, R-002-06)', () => {
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

describe('CreateGroupDialog WF-05 create dialog (R-002-01..03)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-002-06 the empty state offers "Ustvari skupino", which opens the dialog', async () => {
    backend(() => answer(acme, 201));
    renderPage();

    const empty = (await screen.findByRole('heading', { name: 'Še nimate nobene skupine' })).closest(
      'section',
    );
    await userEvent.click(within(empty as HTMLElement).getByRole('button', { name: 'Ustvari skupino' }));

    const dialog = await screen.findByRole('dialog', { name: 'Nova skupina' });
    expect(within(dialog).getByText('Do 100 znakov.')).toBeInTheDocument();
  });

  it('R-002-01 a valid name is sent, the dialog closes and the list shows the new Group', async () => {
    const posted = backend(() => answer(acme, 201));
    renderPage();

    await submitName('Acme');

    expect(posted).toEqual([{ name: 'Acme' }]);
    expect(await screen.findByRole('heading', { level: 2, name: 'Acme' })).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('R-002-02 an empty name shows the rule next to the field and sends nothing', async () => {
    const posted = backend(() => answer(acme, 201));
    renderPage();

    const dialog = await submitName('   ');

    expect(within(dialog).getByRole('alert')).toHaveTextContent('Ime mora imeti od 1 do 100 znakov.');
    expect(within(dialog).getByLabelText(/Ime skupine/)).toHaveAttribute('aria-invalid', 'true');
    expect(posted).toEqual([]);
  });

  it('R-002-02 a name of 101 characters shows the rule and sends nothing', async () => {
    const posted = backend(() => answer(acme, 201));
    renderPage();

    const dialog = await submitName('a'.repeat(101));

    expect(within(dialog).getByRole('alert')).toHaveTextContent('Ime mora imeti od 1 do 100 znakov.');
    expect(posted).toEqual([]);
  });

  it('R-002-02 the rule from the server is shown next to the field', async () => {
    backend(() =>
      answer(
        {
          code: 'validation.failed',
          message: 'x',
          fieldErrors: [{ field: 'name', code: 'validation.name_invalid' }],
        },
        422,
      ),
    );
    renderPage();

    const dialog = await submitName('Acme');

    expect(within(dialog).getByRole('alert')).toHaveTextContent('Ime mora imeti od 1 do 100 znakov.');
  });

  it('R-002-03 a name the User already owns shows the message and keeps the input', async () => {
    backend(() => answer({ code: 'group.name_taken', message: 'x' }, 409));
    renderPage();

    const dialog = await submitName('ACME');

    expect(within(dialog).getByRole('alert')).toHaveTextContent('Skupino s tem imenom že imate.');
    expect(within(dialog).getByLabelText(/Ime skupine/)).toHaveValue('ACME');
  });

  it('shows a general error without technical detail when the server fails', async () => {
    backend(() => answer({ code: 'server.error', message: 'boom' }, 500));
    renderPage();

    const dialog = await submitName('Acme');

    expect(await within(dialog).findByRole('alert')).toHaveTextContent(
      'Nekaj je šlo narobe. Poskusite znova.',
    );
    expect(within(dialog).queryByText('boom')).not.toBeInTheDocument();
  });
});
