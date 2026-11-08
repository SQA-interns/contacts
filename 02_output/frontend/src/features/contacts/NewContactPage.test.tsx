import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { NewContactPage } from './NewContactPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
const created = { id: 'c1', version: 1, firstName: 'Ana', tags: [], attributes: [] };

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/groups/g1/contacts/new']}>
        <Routes>
          <Route path="/groups/:groupId/contacts/new" element={<NewContactPage />} />
          <Route path="/groups/:groupId/contacts/:contactId" element={<p>Contact page</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const field = (label: string) => screen.getByLabelText(label, { exact: true });
const save = () => userEvent.click(screen.getByRole('button', { name: 'Shrani kontakt' }));

describe('NewContactPage WF-08 (R-003-01..05)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-003-02 offers exactly the 11 predefined fields', () => {
    renderPage();

    for (const label of [
      'Ime',
      'Priimek',
      'E-pošta',
      'Telefon',
      'Podjetje',
      'Delovno mesto',
      'Ulica in hišna številka',
      'Poštna številka',
      'Kraj',
      'Država',
      'Opombe',
    ]) {
      expect(field(label)).toBeInTheDocument();
    }
    expect(screen.getAllByRole('textbox').length + screen.getAllByRole('combobox').length).toBe(11);
  });

  it('R-003-01 sends the typed fields to the Group and opens the new Contact', async () => {
    vi.mocked(fetch).mockResolvedValue(answer(created, 201));
    renderPage();

    await userEvent.type(field('Ime'), 'Ana');
    await userEvent.type(field('E-pošta'), 'ANA@Acme.com');
    await userEvent.selectOptions(field('Država'), 'SI');
    await save();

    expect(await screen.findByText('Contact page')).toBeInTheDocument();
    const [url, init] = vi.mocked(fetch).mock.calls[0] ?? [];
    expect(String(url)).toContain('/api/v1/groups/g1/contacts');
    expect(init?.method).toBe('POST');
    expect(JSON.parse(String(init?.body))).toMatchObject({
      firstName: 'Ana',
      email: 'ANA@Acme.com',
      country: 'SI',
    });
  });

  it('R-003-03 shows each message next to its field and keeps what was typed', async () => {
    vi.mocked(fetch).mockResolvedValue(
      answer(
        {
          code: 'validation.failed',
          message: 'Validation failed',
          fieldErrors: [{ field: 'email', code: 'validation.email_invalid' }],
        },
        422,
      ),
    );
    renderPage();

    await userEvent.type(field('Ime'), 'Ana');
    await userEvent.type(field('E-pošta'), 'ana@');
    await save();

    expect(await screen.findByText('E-poštni naslov ni veljaven.')).toBeInTheDocument();
    expect(field('E-pošta')).toHaveAttribute('aria-invalid', 'true');
    expect(field('Ime')).toHaveValue('Ana');
    expect(field('E-pošta')).toHaveValue('ana@');
  });

  it('AC-003-02 says on each of the four fields that one of them is needed', async () => {
    const needed = ['firstName', 'lastName', 'email', 'company'].map((f) => ({
      field: f,
      code: 'validation.required_any',
    }));
    vi.mocked(fetch).mockResolvedValue(
      answer({ code: 'validation.failed', message: 'x', fieldErrors: needed }, 422),
    );
    renderPage();

    await userEvent.type(field('Telefon'), '040 123 456');
    await save();

    const message = 'Izpolnite vsaj ime, priimek, e-pošto ali podjetje.';
    expect(await screen.findAllByText(message)).toHaveLength(4);
    expect(field('Telefon')).toHaveValue('040 123 456');
  });

  it('R-003-09 tells a Viewer in the language of the user that the role does not allow it', async () => {
    vi.mocked(fetch).mockResolvedValue(
      answer({ code: 'auth.role_forbidden', message: 'Your role does not allow this' }, 403),
    );
    renderPage();

    await userEvent.type(field('Ime'), 'Ana');
    await save();

    expect(await screen.findByRole('alert')).toHaveTextContent('Za to dejanje nimate pravice.');
  });
});
