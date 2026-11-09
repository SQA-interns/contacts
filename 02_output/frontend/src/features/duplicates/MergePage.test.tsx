import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Toaster } from '@/components/ui/sonner';
import { setLanguage } from '@/i18n';
import { MergePage } from './MergePage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

const full = (id: string, createdAt: string, extra: Record<string, unknown> = {}) => ({
  id,
  version: 1,
  tags: [],
  attributes: [],
  createdAt,
  updatedAt: createdAt,
  ...extra,
});

const ana = full('a', '2026-11-06T10:00:00Z', { firstName: 'Ana', email: 'skupna@acme.com' });
const anaB = full('b', '2026-11-07T10:00:00Z', {
  firstName: 'ana',
  email: 'skupna@acme.com',
  phone: '040 123 456',
});

interface Sent {
  url: string;
  method: string;
  body: unknown;
}

interface Options {
  role?: string;
  contacts?: Record<string, unknown>[];
  merge?: () => Response;
}

/** The backend: the Group list gives the Role, `GET .../contacts/{id}` a Contact, `POST .../merges` the answer. */
function backend({ role = 'EDITOR', contacts = [ana, anaB], merge }: Options = {}): Sent[] {
  const sent: Sent[] = [];
  vi.mocked(fetch).mockImplementation((url, init) => {
    const path = String(url);
    const method = init?.method ?? 'GET';
    if (path.endsWith('/groups')) {
      return Promise.resolve(answer([{ id: 'g1', name: 'Acme', role, createdAt: '2026-11-01T00:00:00Z' }]));
    }
    sent.push({
      url: path,
      method,
      body: init?.body ? (JSON.parse(String(init.body)) as unknown) : undefined,
    });
    if (method === 'POST') return Promise.resolve(merge ? merge() : answer({ ...ana, version: 2 }));
    const found = contacts.find((c) => path.endsWith(`/contacts/${String(c.id)}`));
    return Promise.resolve(found ? answer(found) : answer({ code: 'contact.not_found', message: 'x' }, 404));
  });
  return sent;
}

function Where() {
  const location = useLocation();
  return <p>at {location.pathname}</p>;
}

function renderPage(search = '?contact=b&contact=a') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/groups/g1/merge${search}`]}>
        <Routes>
          <Route path="/groups/:groupId/merge" element={<MergePage />} />
          <Route path="/groups/:groupId/*" element={<Where />} />
        </Routes>
      </MemoryRouter>
      <Toaster />
    </QueryClientProvider>,
  );
}

const posts = (sent: Sent[]) => sent.filter((s) => s.method === 'POST');
const merge = () => screen.getByRole('button', { name: 'Združi' });

describe('MergePage WF-18 (R-026-01, R-026-02)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-026-01 loads every picked Contact and shows them side by side, the oldest as Survivor', async () => {
    const sent = backend();
    renderPage();

    const survivor = await screen.findByRole('columnheader', { name: /Ohranjeni kontakt/ });
    expect(survivor).toHaveTextContent('Ana');
    expect(survivor).toHaveTextContent('ustvarjen 6. 11. 2026');
    expect(screen.getByRole('columnheader', { name: /Kontakt 2/ })).toHaveTextContent(
      'ustvarjen 7. 11. 2026',
    );
    expect(screen.getByRole('heading', { level: 1, name: 'Združi kontakte' })).toBeInTheDocument();
    expect(sent.map((s) => s.url).sort()).toEqual([
      expect.stringMatching(/\/groups\/g1\/contacts\/a$/),
      expect.stringMatching(/\/groups\/g1\/contacts\/b$/),
    ]);
  });

  it('AC-026-01 preselects the first name of the Survivor and the phone of the next Contact', async () => {
    backend();
    renderPage();

    expect(await screen.findByRole('radio', { name: 'Ana' })).toBeChecked();
    expect(screen.getByRole('radio', { name: 'ana' })).not.toBeChecked();
    expect(screen.getByRole('radio', { name: '040 123 456' })).toBeChecked();
  });

  it('EX-026-02 a value all Contacts share is shown without a choice', async () => {
    backend();
    renderPage();

    await screen.findByRole('radio', { name: 'Ana' });
    const row = screen.getByRole('row', { name: /E-pošta/ });
    expect(within(row).getAllByText('skupna@acme.com')).toHaveLength(2);
    expect(within(row).queryAllByRole('radio')).toHaveLength(0);
  });

  it('R-026-02 lets the user choose another value of a field and an empty one', async () => {
    const sent = backend();
    renderPage();
    await userEvent.click(await screen.findByRole('radio', { name: 'ana' }));
    const phone = screen.getByRole('row', { name: /Telefon/ });
    await userEvent.click(within(phone).getByRole('radio', { name: '(prazno)' }));

    await userEvent.click(merge());
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Združi' }));

    await vi.waitFor(() => expect(posts(sent)).toHaveLength(1));
    expect(posts(sent)[0]?.body).toMatchObject({ fields: { firstName: 'ana', phone: null } });
  });

  it('EX-026-03 with equal creation times the Contact with the smaller id is the Survivor', async () => {
    const t = '2026-11-06T10:00:00Z';
    backend({ contacts: [full('b', t, { firstName: 'Velika' }), full('a', t, { firstName: 'Mala' })] });
    renderPage();

    expect(await screen.findByRole('columnheader', { name: /Ohranjeni kontakt/ })).toHaveTextContent('Mala');
  });
});

describe('MergePage Tags and Attributes (R-026-03, R-026-04)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  const withTags = [
    { ...ana, tags: ['udelezenec2025'] },
    { ...anaB, tags: ['neObvescaj'] },
  ];

  it('R-026-03 shows the Tags of each Contact and says the Survivor gets all of them', async () => {
    backend({ contacts: withTags });
    renderPage();

    const row = await screen.findByRole('row', { name: /Značke/ });
    expect(within(row).getByText('udelezenec2025')).toBeInTheDocument();
    expect(within(row).getByText('neObvescaj')).toBeInTheDocument();
    expect(screen.getByText(/Ohranjeni kontakt dobi vse značke obeh kontaktov/)).toBeInTheDocument();
  });

  const clash = [
    { ...ana, attributes: [{ key: 'kompetenca', value: 'java' }] },
    { ...anaB, attributes: [{ key: 'kompetenca', value: 'spletnoProgramiranje' }] },
  ];

  it('AC-026-03 two values of one key need a choice before the merge', async () => {
    const sent = backend({ contacts: clash });
    renderPage();
    expect(await screen.findByRole('radio', { name: 'java' })).not.toBeChecked();
    expect(screen.getByRole('radio', { name: 'spletnoProgramiranje' })).not.toBeChecked();

    await userEvent.click(merge());

    expect(screen.getByText('Izberite vrednost.')).toBeVisible();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(posts(sent)).toHaveLength(0);
  });

  it('R-026-04 sends the chosen value of the key once the user has chosen', async () => {
    const sent = backend({ contacts: clash });
    renderPage();
    await userEvent.click(await screen.findByRole('radio', { name: 'spletnoProgramiranje' }));

    await userEvent.click(merge());
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Združi' }));

    await vi.waitFor(() => expect(posts(sent)).toHaveLength(1));
    expect(posts(sent)[0]?.body).toMatchObject({
      attributes: [{ key: 'kompetenca', value: 'spletnoProgramiranje' }],
    });
  });

  it('R-026-04 an Attribute only one Contact has is kept and needs no choice', async () => {
    backend({ contacts: [{ ...ana, attributes: [{ key: 'jezik', value: 'sl' }] }, anaB] });
    renderPage();

    const row = await screen.findByRole('row', { name: /Atribut jezik/ });
    expect(within(row).getByText('sl')).toBeInTheDocument();
    expect(within(row).getByText('(le pri tem kontaktu, ohranjen)')).toBeInTheDocument();
    expect(within(row).queryAllByRole('radio')).toHaveLength(0);
  });
});

describe('MergePage confirmation and answers (R-026-05, R-026-06, R-026-08, R-026-10)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-026-10 and AC-026-12 nothing is merged until the user confirms in the dialog', async () => {
    const sent = backend();
    renderPage();
    await screen.findByRole('radio', { name: 'Ana' });
    expect(posts(sent)).toHaveLength(0);

    await userEvent.click(merge());
    const dialog = screen.getByRole('dialog', { name: 'Združim 2 kontakta?' });
    expect(dialog).toHaveTextContent('30 dni');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Prekliči' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(posts(sent)).toHaveLength(0);
  });

  it('R-026-05 a confirmed merge sends the Survivor, the versions and the chosen values and says so', async () => {
    const sent = backend();
    renderPage();
    await screen.findByRole('radio', { name: 'Ana' });

    await userEvent.click(merge());
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Združi' }));

    expect(await screen.findByText('Kontakti so združeni.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Odpri kontakt' })).toBeInTheDocument();
    expect(posts(sent)).toHaveLength(1);
    expect(posts(sent)[0]).toMatchObject({
      url: expect.stringMatching(/\/groups\/g1\/merges$/),
      body: {
        survivorId: 'a',
        contacts: [
          { id: 'a', version: 1 },
          { id: 'b', version: 1 },
        ],
        fields: { firstName: 'Ana', email: 'skupna@acme.com', phone: '040 123 456' },
        attributes: [],
      },
    });
    expect(await screen.findByText('at /groups/g1/duplicates')).toBeInTheDocument();
  });

  it('shows "Združevanje …" while the merge runs and cannot be started twice', async () => {
    let release: (r: Response) => void = () => undefined;
    const sent = backend({ merge: () => new Response('') });
    vi.mocked(fetch).mockImplementation((url, init) => {
      if ((init?.method ?? 'GET') === 'POST') {
        sent.push({ url: String(url), method: 'POST', body: undefined });
        return new Promise<Response>((resolve) => (release = resolve));
      }
      const path = String(url);
      if (path.endsWith('/groups')) {
        return Promise.resolve(answer([{ id: 'g1', name: 'Acme', role: 'EDITOR', createdAt: 'x' }]));
      }
      return Promise.resolve(answer(path.endsWith('/a') ? ana : anaB));
    });
    renderPage();
    await screen.findByRole('radio', { name: 'Ana' });
    await userEvent.click(merge());

    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Združi' }));

    const busy = await screen.findByRole('button', { name: 'Združevanje …' });
    expect(busy).toBeDisabled();
    release(answer({ ...ana, version: 2 }));
    expect(await screen.findByText('Kontakti so združeni.')).toBeInTheDocument();
  });

  it('R-026-08 a Contact changed by someone else: refused, nothing merged, the reason is shown', async () => {
    backend({
      merge: () =>
        answer(
          { code: 'contact.changed_by_someone_else', message: 'x', current: { ...anaB, version: 2 } },
          409,
        ),
    });
    renderPage();
    await screen.findByRole('radio', { name: 'Ana' });

    await userEvent.click(merge());
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Združi' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Kontakt je spremenil nekdo drug.');
    expect(alert).toHaveTextContent('Nič ni bilo združeno. Odprite združevanje znova.');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.queryByText('Kontakti so združeni.')).not.toBeInTheDocument();
  });

  it('R-026-06 a result that breaks BR-06 is refused and the reason is shown', async () => {
    backend({
      merge: () =>
        answer(
          {
            code: 'validation.failed',
            message: 'x',
            fieldErrors: [{ field: 'firstName', code: 'validation.required_any' }],
          },
          422,
        ),
    });
    renderPage();
    await screen.findByRole('radio', { name: 'Ana' });

    await userEvent.click(merge());
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Združi' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Podatki niso veljavni.');
    expect(alert).toHaveTextContent('Izpolnite vsaj ime, priimek, e-pošto ali podjetje.');
  });

  it('any other refusal is shown with its own text', async () => {
    backend({ merge: () => answer({ code: 'contact.in_trash', message: 'x' }, 409) });
    renderPage();
    await screen.findByRole('radio', { name: 'Ana' });

    await userEvent.click(merge());
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Združi' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Kontakt je v košu');
  });

  it('"Prekliči" leaves for the Duplicates screen and merges nothing', async () => {
    const sent = backend();
    renderPage();
    await screen.findByRole('radio', { name: 'Ana' });

    await userEvent.click(screen.getByRole('button', { name: 'Prekliči' }));

    expect(await screen.findByText('at /groups/g1/duplicates')).toBeInTheDocument();
    expect(posts(sent)).toHaveLength(0);
  });
});

describe('MergePage states and roles (UI-14, R-026-09, R-026-12)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('opened with fewer than two Contacts it says there is nothing to merge and asks for nothing', async () => {
    const sent = backend();
    renderPage('?contact=a');

    expect(await screen.findByRole('heading', { name: 'Ni kontaktov za združitev' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Podvojeni kontakti' })).toBeInTheDocument();
    expect(sent).toHaveLength(0);
  });

  it('the same Contact twice counts once', async () => {
    backend();
    renderPage('?contact=a&contact=a');

    expect(await screen.findByRole('heading', { name: 'Ni kontaktov za združitev' })).toBeInTheDocument();
  });

  it('R-026-12 a Viewer is told it is not allowed and nothing is loaded', async () => {
    const sent = backend({ role: 'VIEWER' });
    renderPage();

    expect(await screen.findByText('Za združevanje kontaktov nimate pravice.')).toBeInTheDocument();
    expect(sent).toHaveLength(0);
  });

  it('shows the loading state while the Contacts load', async () => {
    vi.mocked(fetch).mockImplementation((url) =>
      String(url).endsWith('/groups')
        ? Promise.resolve(answer([{ id: 'g1', name: 'Acme', role: 'EDITOR', createdAt: 'x' }]))
        : new Promise<Response>(() => undefined),
    );
    renderPage();

    expect(await screen.findByText('Nalaganje …')).toBeInTheDocument();
  });

  it('shows an error with a retry when a Contact cannot be loaded', async () => {
    let missing = true;
    vi.mocked(fetch).mockImplementation((url) => {
      const path = String(url);
      if (path.endsWith('/groups')) {
        return Promise.resolve(answer([{ id: 'g1', name: 'Acme', role: 'EDITOR', createdAt: 'x' }]));
      }
      if (path.endsWith('/b') && missing)
        return Promise.resolve(answer({ code: 'server.error', message: 'x' }, 500));
      return Promise.resolve(answer(path.endsWith('/a') ? ana : anaB));
    });
    renderPage();

    await screen.findByRole('alert');
    missing = false;
    await userEvent.click(screen.getByRole('button', { name: 'Poskusi znova' }));
    expect(await screen.findByRole('radio', { name: 'Ana' })).toBeInTheDocument();
  });

  it('shows the texts in English', async () => {
    await setLanguage('en');
    backend();
    renderPage();

    expect(await screen.findByRole('heading', { level: 1, name: 'Merge contacts' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Merge' }));
    expect(screen.getByRole('dialog', { name: 'Merge 2 contacts?' })).toBeInTheDocument();
  });
});
