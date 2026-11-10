import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
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
    // the 11 fields and the field that adds a Tag
    expect(screen.getAllByRole('textbox').length + screen.getAllByRole('combobox').length).toBe(12);
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
  it('R-004-01 sends the Attributes with the Contact', async () => {
    vi.mocked(fetch).mockImplementation((url) =>
      Promise.resolve(
        String(url).includes('/facets') ? answer({ attributeKeys: ['projekt'] }) : answer(created, 201),
      ),
    );
    renderPage();

    await userEvent.type(field('Ime'), 'Ana');
    await userEvent.click(screen.getByRole('button', { name: 'Dodaj atribut' }));
    await userEvent.type(field('Ključ'), 'kompetenca');
    await userEvent.type(field('Vrednost'), 'java');
    await save();

    expect(await screen.findByText('Contact page')).toBeInTheDocument();
    const post = vi.mocked(fetch).mock.calls.find(([, init]) => init?.method === 'POST');
    expect(JSON.parse(String(post?.[1]?.body)).attributes).toEqual([{ key: 'kompetenca', value: 'java' }]);
  });

  it('R-004-05 suggests the keys already used in the Group', async () => {
    vi.mocked(fetch).mockResolvedValue(answer({ attributeKeys: ['kompetenca'] }));
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: 'Dodaj atribut' }));

    const list = field('Ključ').getAttribute('list');
    await vi.waitFor(() => expect(document.querySelectorAll(`datalist#${list} option`)).toHaveLength(1));
    expect(String(vi.mocked(fetch).mock.calls[0]?.[0])).toContain('/api/v1/groups/g1/facets');
  });

  it('R-004-03 shows the message next to the second pair and keeps the pairs', async () => {
    vi.mocked(fetch).mockImplementation((url) =>
      Promise.resolve(
        String(url).includes('/facets')
          ? answer({ attributeKeys: [] })
          : answer(
              {
                code: 'validation.failed',
                message: 'x',
                fieldErrors: [{ field: 'attributes[1].key', code: 'validation.attribute_duplicate_key' }],
              },
              422,
            ),
      ),
    );
    renderPage();

    await userEvent.type(field('Ime'), 'Ana');
    for (const n of [1, 2]) {
      await userEvent.click(screen.getByRole('button', { name: 'Dodaj atribut' }));
      const row = within(screen.getByRole('group', { name: `Atribut ${n}` }));
      await userEvent.type(row.getByLabelText('Ključ', { exact: true }), n === 1 ? 'a' : 'A');
      await userEvent.type(row.getByLabelText('Vrednost', { exact: true }), 'x');
    }
    await save();

    expect(await screen.findByText('Ključ atributa se ponavlja.')).toBeInTheDocument();
    const second = within(screen.getByRole('group', { name: 'Atribut 2' })).getByLabelText('Ključ', {
      exact: true,
    });
    expect(second).toHaveAttribute('aria-invalid', 'true');
    expect(second).toHaveValue('A');
  });
  it('R-005-01 sends the typed Tags with the Contact', async () => {
    vi.mocked(fetch).mockImplementation((url) =>
      Promise.resolve(String(url).includes('/facets') ? answer({ tags: [] }) : answer(created, 201)),
    );
    renderPage();

    await userEvent.type(field('Ime'), 'Ana');
    await userEvent.type(field('Dodaj značko'), 'udelezenec2025{Enter}');
    await save();

    expect(await screen.findByText('Contact page')).toBeInTheDocument();
    const post = vi.mocked(fetch).mock.calls.find(([, init]) => init?.method === 'POST');
    expect(JSON.parse(String(post?.[1]?.body)).tags).toEqual(['udelezenec2025']);
  });

  it('R-005-07 suggests the Tags of the Group while typing', async () => {
    vi.mocked(fetch).mockResolvedValue(answer({ tags: ['gospodar'] }));
    renderPage();

    await userEvent.type(field('Dodaj značko'), 'go');

    const list = field('Dodaj značko').getAttribute('list');
    await vi.waitFor(() =>
      expect(document.querySelectorAll(`datalist[id="${list}"] option`)).toHaveLength(1),
    );
    expect(String(vi.mocked(fetch).mock.calls[0]?.[0])).toContain('/api/v1/groups/g1/facets');
  });

  it('R-005-02 shows the rule of the server next to the Tags and keeps them', async () => {
    vi.mocked(fetch).mockImplementation((url) =>
      Promise.resolve(
        String(url).includes('/facets')
          ? answer({ tags: [] })
          : answer(
              {
                code: 'validation.failed',
                message: 'x',
                fieldErrors: [{ field: 'tags[0]', code: 'validation.tag_limit' }],
              },
              422,
            ),
      ),
    );
    renderPage();

    await userEvent.type(field('Ime'), 'Ana');
    await userEvent.type(field('Dodaj značko'), 'gost{Enter}');
    await save();

    expect(await screen.findByText('Kontakt ima lahko največ 50 značk.')).toBeInTheDocument();
    expect(screen.getByText('gost')).toBeInTheDocument();
  });
});
