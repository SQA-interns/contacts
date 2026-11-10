import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { InvitationForm } from './InvitationForm';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

function renderForm() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={queryClient}>
      <InvitationForm groupId="g1" />
    </QueryClientProvider>,
  );
}

const sentBodies = () =>
  vi
    .mocked(fetch)
    .mock.calls.filter(([, init]) => init?.method === 'POST')
    .map(([, init]) => JSON.parse(String(init?.body)) as unknown);

describe('InvitationForm: the e-mail check and the answers (R-014-02..04)', () => {
  beforeEach(async () => {
    await setLanguage('en');
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.resolve(answer({ id: 'i1' }, 201))),
    );
  });
  afterEach(() => vi.unstubAllGlobals());

  it.each([
    'plain',
    'a@b',
    '@b.si',
    'a@@b.si',
    'a@b@c.si',
    'a b@c.si',
    'a@.si',
    'a@b.',
    'a@b..si',
    'ab@c d.si',
  ])('refuses %s without asking the server', async (address) => {
    renderForm();
    await userEvent.type(screen.getByLabelText(/E-mail/), address);
    await userEvent.click(screen.getByRole('button', { name: 'Create invitation' }));
    expect(await screen.findByText('The e-mail address is not valid.')).toBeInTheDocument();
    expect(sentBodies()).toEqual([]);
  });

  it('sends a trimmed address with the chosen Role, shows the notice and empties the field', async () => {
    renderForm();
    const field = screen.getByLabelText(/E-mail/);
    await userEvent.type(field, '  boris@acme.com  ');
    await userEvent.selectOptions(screen.getByLabelText(/Role/), 'VIEWER');
    await userEvent.click(screen.getByRole('button', { name: 'Create invitation' }));
    expect(await screen.findByText('Invitation created. Tell the invited person.')).toBeInTheDocument();
    expect(sentBodies()).toEqual([{ email: 'boris@acme.com', role: 'VIEWER' }]);
    expect(field).toHaveValue('');
  });

  it('offers Editor as the first choice of the Role', () => {
    renderForm();
    expect(screen.getByLabelText(/Role/)).toHaveValue('EDITOR');
  });

  it('removes the format error as soon as the address is edited', async () => {
    renderForm();
    await userEvent.type(screen.getByLabelText(/E-mail/), 'x');
    await userEvent.click(screen.getByRole('button', { name: 'Create invitation' }));
    expect(await screen.findByText('The e-mail address is not valid.')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText(/E-mail/), 'y');
    expect(screen.queryByText('The e-mail address is not valid.')).not.toBeInTheDocument();
  });

  it.each([
    ['invitation.exists', 'This person is already a member or has a pending invitation.'],
    ['invitation.limit', 'The group already has 20 pending invitations.'],
  ])('shows %s next to the e-mail field', async (code, text) => {
    vi.mocked(fetch).mockImplementation(() => Promise.resolve(answer({ code, message: 'x' }, 409)));
    renderForm();
    await userEvent.type(screen.getByLabelText(/E-mail/), 'boris@acme.com');
    await userEvent.click(screen.getByRole('button', { name: 'Create invitation' }));
    expect(await screen.findByText(text)).toBeInTheDocument();
    expect(screen.queryByText('Something went wrong.', { exact: false })).not.toBeInTheDocument();
  });

  it('shows the server field error of the e-mail address, but not one of another field', async () => {
    vi.mocked(fetch).mockImplementation(() =>
      Promise.resolve(
        answer(
          {
            code: 'validation.failed',
            message: 'x',
            fieldErrors: [
              { field: 'role', code: 'validation.too_long' },
              { field: 'email', code: 'validation.email_invalid' },
            ],
          },
          400,
        ),
      ),
    );
    renderForm();
    await userEvent.type(screen.getByLabelText(/E-mail/), 'boris@acme.com');
    await userEvent.click(screen.getByRole('button', { name: 'Create invitation' }));
    expect(await screen.findByText('The e-mail address is not valid.')).toBeInTheDocument();
    expect(screen.queryByText('The value is too long.')).not.toBeInTheDocument();
  });

  it('shows a general error for any other failure', async () => {
    vi.mocked(fetch).mockImplementation(() =>
      Promise.resolve(answer({ code: 'server.error', message: 'x' }, 500)),
    );
    renderForm();
    await userEvent.type(screen.getByLabelText(/E-mail/), 'boris@acme.com');
    await userEvent.click(screen.getByRole('button', { name: 'Create invitation' }));
    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });

  it('disables the button while the invitation is being created', async () => {
    let finish: (r: Response) => void = () => undefined;
    vi.mocked(fetch).mockImplementation(() => new Promise<Response>((resolve) => (finish = resolve)));
    renderForm();
    await userEvent.type(screen.getByLabelText(/E-mail/), 'boris@acme.com');
    const button = screen.getByRole('button', { name: 'Create invitation' });
    expect(button).toBeEnabled();
    expect(button).not.toHaveAttribute('aria-busy');
    await userEvent.click(button);
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute('aria-busy', 'true');
    finish(answer({ id: 'i1' }, 201));
    expect(await screen.findByText('Invitation created. Tell the invited person.')).toBeInTheDocument();
    expect(button).toBeEnabled();
  });
});
