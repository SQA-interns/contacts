import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { ContactPage } from './ContactPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

const ana = {
  id: 'c1',
  version: 2,
  firstName: 'Ana',
  lastName: 'Novak',
  email: 'ana@acme.com',
  phone: '040 123 456',
  company: 'Acme d.o.o.',
  jobTitle: 'Vodja',
  street: 'Slovenska cesta 1',
  postalCode: '1000',
  city: 'Ljubljana',
  country: 'SI',
  notes: 'Udeleženka',
  tags: ['udelezenec2025'],
  attributes: [{ key: 'kompetenca', value: 'spletnoProgramiranje' }],
  createdAt: '2026-11-08T08:00:00Z',
  updatedAt: '2026-11-08T09:00:00Z',
};

/** The backend: the Group list gives the Role, `GET` the Contact, `PUT` what `save` returns. */
function backend(role: string, contact: unknown, save?: () => Response) {
  vi.mocked(fetch).mockImplementation((url, init) => {
    const path = String(url);
    if (init?.method === 'PUT') return Promise.resolve(save?.() ?? answer(contact));
    if (path.endsWith('/groups')) {
      return Promise.resolve(answer([{ id: 'g1', name: 'Acme', role, createdAt: ana.createdAt }]));
    }
    return Promise.resolve(answer(contact));
  });
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/groups/g1/contacts/c1']}>
        <Routes>
          <Route path="/groups/:groupId/contacts/:contactId" element={<ContactPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const putBody = () => {
  const call = vi.mocked(fetch).mock.calls.find(([, init]) => init?.method === 'PUT');
  return JSON.parse(String(call?.[1]?.body));
};

describe('ContactPage WF-07 (R-003-06..09, R-003-11)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-003-08 shows all 11 fields, the Tag and the Attribute', async () => {
    backend('VIEWER', ana);
    renderPage();

    expect(await screen.findByRole('heading', { level: 1, name: 'Ana Novak' })).toBeInTheDocument();
    for (const text of [
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
      'ana@acme.com',
      '040 123 456',
      'Acme d.o.o.',
      'Vodja',
      'Slovenska cesta 1',
      '1000',
      'Ljubljana',
      'Slovenija',
      'Udeleženka',
      'udelezenec2025',
      'kompetenca',
      'spletnoProgramiranje',
      'Različica 2',
    ]) {
      expect(screen.getAllByText(text, { exact: true }).length, text).toBeGreaterThan(0);
    }
  });

  it('R-003-09 offers a Viewer no way to edit', async () => {
    backend('VIEWER', ana);
    renderPage();

    await screen.findByRole('heading', { level: 1, name: 'Ana Novak' });
    expect(screen.queryByRole('button', { name: 'Uredi' })).toBeNull();
  });

  it('EX-003-13 shows markup in a name as plain text', async () => {
    backend('OWNER', { ...ana, lastName: '<script>alert(1)</script>' });
    renderPage();

    expect(
      (await screen.findAllByText('<script>alert(1)</script>', { exact: false })).length,
    ).toBeGreaterThan(0);
    expect(document.querySelector('main script')).toBeNull();
  });

  it('EX-003-12 says the Contact is not found when the server does', async () => {
    vi.mocked(fetch).mockImplementation(() =>
      Promise.resolve(answer({ code: 'contact.not_found', message: 'x' }, 404)),
    );
    renderPage();

    expect(await screen.findByText('Kontakta ni mogoče najti.')).toBeInTheDocument();
  });

  it('R-003-06 saves the edited fields with the version the form was opened at', async () => {
    backend('EDITOR', ana, () => answer({ ...ana, version: 3, company: 'Beta d.o.o.' }));
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Uredi' }));
    const company = screen.getByLabelText('Podjetje', { exact: true });
    await userEvent.clear(company);
    await userEvent.type(company, 'Beta d.o.o.');
    await userEvent.click(screen.getByRole('button', { name: /^Shrani$/ }));

    expect(await screen.findByText('Različica 3')).toBeInTheDocument();
    expect(putBody()).toMatchObject({ company: 'Beta d.o.o.', version: 2, firstName: 'Ana' });
    expect(screen.queryByRole('button', { name: /^Shrani$/ })).toBeNull();
  });

  it('R-003-07 a stale save shows the message and the current Contact; nothing of the form stays', async () => {
    const current = { ...ana, version: 4, city: 'Maribor' };
    backend('EDITOR', ana, () =>
      answer({ code: 'contact.changed_by_someone_else', message: 'x', current }, 409),
    );
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Uredi' }));
    await userEvent.type(screen.getByLabelText('Opombe', { exact: true }), '!');
    await userEvent.click(screen.getByRole('button', { name: /^Shrani$/ }));

    const alert = await screen.findByRole('alert');
    expect(within(alert).getByText('Kontakt je spremenil nekdo drug.')).toBeInTheDocument();
    expect(within(alert).getByText(/različica 4/)).toBeInTheDocument();
    expect(screen.getByText('Maribor')).toBeInTheDocument();
    expect(screen.queryByLabelText('Opombe', { exact: true })).toBeNull();
  });

  it('R-003-03 keeps the form open and names the field when the server refuses the edit', async () => {
    backend('OWNER', ana, () =>
      answer(
        {
          code: 'validation.failed',
          message: 'x',
          fieldErrors: [{ field: 'phone', code: 'validation.phone_invalid' }],
        },
        422,
      ),
    );
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Uredi' }));
    await userEvent.clear(screen.getByLabelText('Telefon', { exact: true }));
    await userEvent.type(screen.getByLabelText('Telefon', { exact: true }), 'abc');
    await userEvent.click(screen.getByRole('button', { name: /^Shrani$/ }));

    expect(await screen.findByText(/Telefon lahko vsebuje/)).toBeInTheDocument();
    expect(screen.getByLabelText('Telefon', { exact: true })).toHaveValue('abc');
  });

  it('cancel leaves the edit form without a request', async () => {
    backend('OWNER', ana);
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Uredi' }));
    await userEvent.click(screen.getByRole('button', { name: 'Prekliči' }));

    expect(screen.getByRole('button', { name: 'Uredi' })).toBeInTheDocument();
    expect(vi.mocked(fetch).mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
  });
});

describe('ContactPage tabs (R-016-03, R-016-04)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('offers an Owner the tabs Podrobnosti and Zgodovina and opens the History', async () => {
    vi.mocked(fetch).mockImplementation((url) => {
      const path = String(url);
      if (path.includes('/history')) {
        return Promise.resolve(
          answer({
            items: [
              {
                id: 'e1',
                occurredAt: '2026-01-15T10:00:00Z',
                userName: 'Ana Novak',
                userEmail: 'ana@acme.com',
                action: 'CONTACT_CREATED',
                contactId: 'c1',
                details: {},
              },
            ],
            page: 1,
            pageSize: 25,
            total: 1,
          }),
        );
      }
      if (path.endsWith('/groups')) {
        return Promise.resolve(answer([{ id: 'g1', name: 'Acme', role: 'OWNER', createdAt: ana.createdAt }]));
      }
      return Promise.resolve(answer(ana));
    });
    renderPage();

    const tabs = await screen.findByRole('navigation', { name: 'Zavihki kontakta' });
    expect(within(tabs).getByRole('button', { name: 'Podrobnosti' })).toHaveAttribute('aria-current', 'page');
    await userEvent.click(within(tabs).getByRole('button', { name: 'Zgodovina' }));

    expect(await screen.findByText('Ustvarjeno')).toBeInTheDocument();
    expect(within(tabs).getByRole('button', { name: 'Zgodovina' })).toHaveAttribute('aria-current', 'page');
  });

  it('R-016-04 offers a Viewer no tabs and never asks for the History', async () => {
    backend('VIEWER', ana);
    renderPage();

    await screen.findByRole('heading', { level: 1, name: 'Ana Novak' });
    expect(screen.queryByRole('navigation', { name: 'Zavihki kontakta' })).toBeNull();
    expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).includes('/history'))).toBe(false);
  });
  it('R-004-06 saves a changed value and a removed pair with the version', async () => {
    backend('EDITOR', {
      ...ana,
      attributes: [
        { key: 'kompetenca', value: 'spletnoProgramiranje' },
        { key: 'projekt', value: 'x' },
      ],
    });
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Uredi' }));
    const value = screen.getByLabelText('Vrednost 1');
    await userEvent.clear(value);
    await userEvent.type(value, 'java');
    await userEvent.click(screen.getByRole('button', { name: 'Odstrani atribut 2' }));
    await userEvent.click(screen.getByRole('button', { name: 'Shrani' }));

    await vi.waitFor(() => expect(putBody().version).toBe(2));
    expect(putBody().attributes).toEqual([{ key: 'kompetenca', value: 'java' }]);
  });

  it('R-004-08 gives a Viewer no control to change an Attribute', async () => {
    backend('VIEWER', ana);
    renderPage();

    await screen.findByText('spletnoProgramiranje');

    expect(screen.queryByRole('button', { name: /atribut/i })).not.toBeInTheDocument();
  });
});
