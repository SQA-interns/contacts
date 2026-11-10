import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Toaster } from '@/components/ui/sonner';
import { setLanguage } from '@/i18n';
import { TagListPage } from './TagListPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
const group = (role: string) => [{ id: 'g1', name: 'Acme', role, createdAt: '2026-11-08T08:00:00Z' }];

interface Backend {
  tagRequests: number;
  sent: { method: string; url: string; body?: unknown }[];
}

function backend(
  role: string,
  tags: unknown[] | 'fail',
  change: (method: string) => Response = () => new Response(null, { status: 204 }),
): Backend {
  const state: Backend = { tagRequests: 0, sent: [] };
  vi.mocked(fetch).mockImplementation((url, init) => {
    const method = init?.method ?? 'GET';
    const path = String(url);
    if (method === 'GET' && path.endsWith('/groups')) return Promise.resolve(answer(group(role)));
    if (method === 'GET') {
      state.tagRequests++;
      return Promise.resolve(
        tags === 'fail' ? answer({ code: 'server.error', message: 'x' }, 500) : answer(tags),
      );
    }
    state.sent.push({ method, url: path, body: init?.body ? JSON.parse(String(init.body)) : undefined });
    return Promise.resolve(change(method));
  });
  return state;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/groups/g1/tags']}>
        <Routes>
          <Route path="/groups/:groupId/tags" element={<TagListPage />} />
        </Routes>
      </MemoryRouter>
      <Toaster />
    </QueryClientProvider>,
  );
}

const gost = { id: 't1', name: 'gost', contactCount: 5 };
const one = { id: 't2', name: 'vip', contactCount: 1 };

describe('TagListPage: texts, errors and busy states (R-006-01..08)', () => {
  beforeEach(async () => {
    await setLanguage('en');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('shows the title, the introduction, the table with its caption and headers, and the cards', async () => {
    backend('OWNER', [gost, one]);
    renderPage();
    expect(await screen.findByRole('heading', { name: 'Tags' })).toBeInTheDocument();
    expect(screen.getByText(/The Tags of this Group and the number of Contacts/)).toBeInTheDocument();
    const table = await screen.findByRole('table', { name: 'Tags of the Group' });
    expect(
      within(table)
        .getAllByRole('columnheader')
        .map((h) => h.textContent),
    ).toEqual(['Tag', 'Contacts', 'Actions']);
    expect(within(table).getByRole('button', { name: 'Rename tag gost' })).toHaveTextContent('Rename');
    expect(within(table).getByRole('button', { name: 'Delete tag gost' })).toHaveTextContent('Delete');
    const cards = within(screen.getByRole('list', { name: 'Tags of the Group' })).getAllByRole('listitem');
    expect(cards).toHaveLength(2);
    expect(cards[0]).toHaveTextContent('Contacts: 5');
    expect(cards[1]).toHaveTextContent('Contacts: 1');
    expect(
      within(cards[0] as HTMLElement).getByRole('button', { name: 'Rename tag gost' }),
    ).toBeInTheDocument();
    expect(
      within(cards[1] as HTMLElement).getByRole('button', { name: 'Delete tag vip' }),
    ).toBeInTheDocument();
  });

  it('shows the empty state with its explanation', async () => {
    backend('EDITOR', []);
    renderPage();
    expect(await screen.findByText('This group has no tags yet')).toBeInTheDocument();
    expect(screen.getByText(/Add tags on a contact/)).toBeInTheDocument();
  });

  it('shows an error with a retry that asks again', async () => {
    const state = backend('OWNER', 'fail');
    renderPage();
    const retry = await screen.findByRole('button', { name: /try again|retry/i });
    const before = state.tagRequests;
    await userEvent.click(retry);
    await waitFor(() => expect(state.tagRequests).toBeGreaterThan(before));
  });

  it('shows a Viewer no Tags and says why', async () => {
    backend('VIEWER', [gost]);
    renderPage();
    expect(await screen.findByText('You do not have permission to manage tags.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Tags' })).toBeInTheDocument();
    expect(screen.queryByText('gost')).not.toBeInTheDocument();
  });

  it('words the count of Contacts in the delete question by its plural form', async () => {
    backend('OWNER', [gost, one]);
    renderPage();
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Delete tag vip' }))[0] as HTMLElement,
    );
    let dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText('Delete tag vip?')).toBeInTheDocument();
    expect(dialog).toHaveTextContent('1 contact carry the tag.');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Delete tag gost' }))[0] as HTMLElement,
    );
    dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('5 contacts carry the tag.');
  });

  it('deletes with the DELETE of the Tag, says so and asks for the list again', async () => {
    const state = backend('OWNER', [gost]);
    renderPage();
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Delete tag gost' }))[0] as HTMLElement,
    );
    const dialog = await screen.findByRole('dialog');
    const before = state.tagRequests;
    await userEvent.click(within(dialog).getByRole('button', { name: 'Delete tag' }));
    expect(await screen.findByText('The tag was deleted.')).toBeInTheDocument();
    expect(state.sent).toEqual([
      { method: 'DELETE', url: expect.stringMatching(/\/groups\/g1\/tags\/t1$/) as string, body: undefined },
    ]);
    await waitFor(() => expect(state.tagRequests).toBeGreaterThan(before));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('keeps the dialog and shows a general error when the deletion fails', async () => {
    backend('OWNER', [gost], () => answer({ code: 'server.error', message: 'x' }, 500));
    renderPage();
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Delete tag gost' }))[0] as HTMLElement,
    );
    await userEvent.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Delete tag' }),
    );
    expect(await screen.findByText('Something went wrong. Please try again.')).toBeInTheDocument();
    expect(screen.queryByText('The tag was deleted.')).not.toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('opens the rename dialog with the name, its hint and the two buttons', async () => {
    backend('EDITOR', [gost]);
    renderPage();
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Rename tag gost' }))[0] as HTMLElement,
    );
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText('Rename tag')).toBeInTheDocument();
    expect(within(dialog).getByLabelText(/New name/)).toHaveValue('gost');
    expect(within(dialog).getByText('Letters, digits, - and _, up to 50 characters.')).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toBeEnabled();
    expect(within(dialog).getByRole('button', { name: 'Rename' })).toBeEnabled();
    expect(within(dialog).getByRole('button', { name: 'Rename' })).not.toHaveAttribute('aria-busy');
  });

  it('renames with the trimmed name, says so, closes and asks for the list again', async () => {
    const state = backend('OWNER', [gost], () => answer({ id: 't1', name: 'novo', contactCount: 5 }));
    renderPage();
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Rename tag gost' }))[0] as HTMLElement,
    );
    const dialog = await screen.findByRole('dialog');
    const field = within(dialog).getByLabelText(/New name/);
    await userEvent.clear(field);
    await userEvent.type(field, '  novo  ');
    const before = state.tagRequests;
    await userEvent.click(within(dialog).getByRole('button', { name: 'Rename' }));
    expect(await screen.findByText('The tag was renamed.')).toBeInTheDocument();
    expect(state.sent).toEqual([
      {
        method: 'PATCH',
        url: expect.stringMatching(/\/groups\/g1\/tags\/t1$/) as string,
        body: { name: 'novo' },
      },
    ]);
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await waitFor(() => expect(state.tagRequests).toBeGreaterThan(before));
  });

  it('shows the busy state while renaming and cannot be cancelled then', async () => {
    let finish: (r: Response) => void = () => undefined;
    vi.mocked(fetch).mockImplementation((url, init) => {
      if ((init?.method ?? 'GET') === 'GET') {
        return Promise.resolve(answer(String(url).endsWith('/groups') ? group('OWNER') : [gost]));
      }
      return new Promise<Response>((resolve) => (finish = resolve));
    });
    renderPage();
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Rename tag gost' }))[0] as HTMLElement,
    );
    const dialog = await screen.findByRole('dialog');
    const field = within(dialog).getByLabelText(/New name/);
    await userEvent.clear(field);
    await userEvent.type(field, 'novo');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Rename' }));
    const busy = await within(dialog).findByRole('button', { name: 'Renaming …' });
    expect(busy).toBeDisabled();
    expect(busy).toHaveAttribute('aria-busy', 'true');
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toBeDisabled();
    finish(answer({ id: 't1', name: 'novo', contactCount: 5 }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it.each([
    ['tag.name_taken', undefined, 'A tag with this name already exists.'],
    ['validation.failed', [{ field: 'name', code: 'validation.tag_invalid' }], null],
  ])('shows the refusal %s next to the field', async (code, fieldErrors, text) => {
    backend('OWNER', [gost], () =>
      answer({ code, message: 'x', fieldErrors }, code === 'tag.name_taken' ? 409 : 400),
    );
    renderPage();
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Rename tag gost' }))[0] as HTMLElement,
    );
    const dialog = await screen.findByRole('dialog');
    const field = within(dialog).getByLabelText(/New name/);
    await userEvent.clear(field);
    await userEvent.type(field, 'novo');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Rename' }));
    await waitFor(() => expect(field).toHaveAccessibleDescription(/\S/));
    if (text) expect(within(dialog).getByText(text)).toBeInTheDocument();
    expect(within(dialog).queryByText('Something went wrong. Please try again.')).not.toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('shows a general error in the dialog for an unexpected refusal, and a field error of another field is not shown at the field', async () => {
    backend('OWNER', [gost], () =>
      answer(
        {
          code: 'validation.failed',
          message: 'x',
          fieldErrors: [{ field: 'other', code: 'validation.too_long' }],
        },
        400,
      ),
    );
    renderPage();
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Rename tag gost' }))[0] as HTMLElement,
    );
    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Rename' }));
    expect(await within(dialog).findByRole('alert')).toBeInTheDocument();
  });

  it('forgets the error and the typed text when the dialog is closed and opened again', async () => {
    backend('OWNER', [gost, one]);
    renderPage();
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Rename tag gost' }))[0] as HTMLElement,
    );
    let dialog = await screen.findByRole('dialog');
    const field = within(dialog).getByLabelText(/New name/);
    await userEvent.clear(field);
    await userEvent.type(field, 'bad name');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Rename' }));
    expect(
      await within(dialog).findByText('Letters, digits, - and _, up to 50 characters.'),
    ).toBeInTheDocument();
    expect(within(dialog).getByLabelText(/New name/)).toHaveAttribute('aria-invalid', 'true');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await userEvent.click(
      (await screen.findAllByRole('button', { name: 'Rename tag vip' }))[0] as HTMLElement,
    );
    dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByLabelText(/New name/)).toHaveValue('vip');
    expect(within(dialog).getByLabelText(/New name/)).not.toHaveAttribute('aria-invalid', 'true');
  });
});
