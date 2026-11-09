import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Toaster } from '@/components/ui/sonner';
import { setLanguage } from '@/i18n';
import { DeleteGroupDialog } from './DeleteGroupDialog';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

interface Sent {
  url: string;
  body: unknown;
}

/** The backend: `POST /groups/{g}/trash` answers `reply`. */
function backend(reply: () => Response = () => new Response(null, { status: 204 })): Sent[] {
  const sent: Sent[] = [];
  vi.mocked(fetch).mockImplementation((url, init) => {
    sent.push({ url: String(url), body: init?.body ? JSON.parse(String(init.body)) : undefined });
    return Promise.resolve(init?.method === 'POST' ? reply() : answer([]));
  });
  return sent;
}

function renderDialog(name = 'Acme', onDeleted = vi.fn(), onClose = vi.fn()) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <DeleteGroupDialog group={{ id: 'g1', name }} onClose={onClose} onDeleted={onDeleted} />
        <Toaster />
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return { onDeleted, onClose };
}

const field = () => screen.getByLabelText(/Za potrditev vpišite ime skupine/);
const confirm = () => within(screen.getByRole('dialog')).getByRole('button', { name: 'Izbriši skupino' });

describe('DeleteGroupDialog WF-14 (R-020-01, R-020-02)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('names the Group in the title and says it stays 30 days in the Group Trash', () => {
    renderDialog();

    expect(screen.getByRole('dialog', { name: 'Izbrišem skupino Acme?' })).toHaveTextContent(
      '30 dni v košu skupin',
    );
  });

  it('R-020-02 keeps the button disabled while the field is empty', () => {
    renderDialog();

    expect(confirm()).toBeDisabled();
  });

  it('AC-020-02 keeps the button disabled when the name is typed in another case', async () => {
    renderDialog();
    await userEvent.type(field(), 'acme');

    expect(confirm()).toBeDisabled();
  });

  it('R-020-01 enables the button when the exact name is typed and sends it', async () => {
    const sent = backend();
    renderDialog();
    await userEvent.type(field(), 'Acme');
    await userEvent.click(confirm());

    await vi.waitFor(() =>
      expect(sent).toEqual([{ url: expect.stringContaining('/groups/g1/trash'), body: { name: 'Acme' } }]),
    );
  });

  it('EX-020-01 accepts the name typed with leading and trailing spaces', async () => {
    renderDialog();
    await userEvent.type(field(), '  Acme  ');

    expect(confirm()).toBeEnabled();
  });

  it('EX-020-02 accepts a Group name of 100 characters', async () => {
    const name = 'g'.repeat(100);
    renderDialog(name);
    await userEvent.click(field());
    await userEvent.paste(name);

    expect(confirm()).toBeEnabled();
  });

  it('R-020-01 confirms in a toast that offers the Group Trash and tells the caller', async () => {
    backend();
    const { onDeleted } = renderDialog();
    await userEvent.type(field(), 'Acme');
    await userEvent.click(confirm());

    expect(await screen.findByText('Skupina je v košu skupin.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Odpri koš skupin' })).toBeInTheDocument();
    expect(onDeleted).toHaveBeenCalledTimes(1);
  });

  it('R-020-02 shows the message under the field when the server refuses the name', async () => {
    backend(() => answer({ code: 'group.name_mismatch', message: 'x' }, 422));
    const { onDeleted } = renderDialog();
    await userEvent.type(field(), 'Acme');
    await userEvent.click(confirm());

    expect(await screen.findByText('Vpisano ime ni enako imenu skupine.')).toBeInTheDocument();
    expect(onDeleted).not.toHaveBeenCalled();
  });

  it('R-020-06 shows the refusal when the caller may not delete the Group', async () => {
    backend(() => answer({ code: 'auth.role_forbidden', message: 'x' }, 403));
    renderDialog();
    await userEvent.type(field(), 'Acme');
    await userEvent.click(confirm());

    expect(await screen.findByRole('alert')).toHaveTextContent('Za to dejanje nimate pravice.');
  });

  it('closes without a request when Cancel is pressed', async () => {
    const sent = backend();
    const { onClose } = renderDialog();
    await userEvent.click(screen.getByRole('button', { name: 'Prekliči' }));

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(sent).toHaveLength(0);
  });
});
