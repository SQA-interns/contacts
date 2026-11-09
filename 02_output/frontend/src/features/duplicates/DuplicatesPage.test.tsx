import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { DuplicatesPage } from './DuplicatesPage';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

const contact = (id: string, firstName: string, extra: Record<string, unknown> = {}) => ({
  id,
  firstName,
  lastName: null,
  email: 'ana@acme.com',
  company: null,
  city: null,
  country: null,
  tags: [],
  version: 1,
  createdAt: '2026-11-06T10:00:00Z',
  ...extra,
});

const certain = (count: number) => ({
  key: 'k1',
  mark: 'certain',
  reasonCode: 'duplicate.same_email',
  reasonParams: { email: 'ana@acme.com', count: String(count) },
  contacts: Array.from({ length: count }, (_, i) => contact(`c${i}`, `Ana${i}`)),
});

const probable = (code: string) => ({
  key: 'k2',
  mark: 'probable',
  reasonCode: code,
  reasonParams: { name: 'Ana Novak', count: '2' },
  contacts: [
    contact('p1', 'Ana', { lastName: 'Novak', email: null, company: 'Acme', city: 'Ljubljana' }),
    contact('p2', 'ana', { lastName: 'novak', email: 'a@x.si', company: 'ACME' }),
  ],
});

interface Sent {
  url: string;
  method: string;
}

/** The backend: the Group list gives the Role, `GET .../duplicates` the sets. */
function backend(role: string, sets: () => Response): Sent[] {
  const sent: Sent[] = [];
  vi.mocked(fetch).mockImplementation((url, init) => {
    const path = String(url);
    if (path.endsWith('/groups')) {
      return Promise.resolve(answer([{ id: 'g1', name: 'Acme', role, createdAt: '2026-11-01T00:00:00Z' }]));
    }
    sent.push({ url: path, method: init?.method ?? 'GET' });
    return Promise.resolve(sets());
  });
  return sent;
}

/** Stands for the Merge screen: shows where the page navigated to. */
function MergeProbe() {
  const location = useLocation();
  return <p>merge screen {location.search}</p>;
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/groups/g1/duplicates']}>
        <Routes>
          <Route path="/groups/:groupId/duplicates" element={<DuplicatesPage />} />
          <Route path="/groups/:groupId/merge" element={<MergeProbe />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('DuplicatesPage WF-17 (R-024-01, R-024-05, R-024-10)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-024-05 shows the mark, the reason and the Contacts of a certain set', async () => {
    backend('EDITOR', () => answer([certain(3)]));
    renderPage();

    expect(await screen.findByText('Zanesljivo')).toBeInTheDocument();
    expect(screen.getByText('3 kontakti imajo e-pošto ana@acme.com')).toBeInTheDocument();
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
    // the frozen e2e-05 counts the words of the marks on the page: the intro must not use them
    expect(screen.queryByText(/verjetno/i)).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'Podvojeni kontakti' })).toBeInTheDocument();
  });

  it('R-024-05 uses the Slovene plural forms of the reason', async () => {
    backend('EDITOR', () => answer([certain(2)]));
    renderPage();
    expect(await screen.findByText('2 kontakta imata e-pošto ana@acme.com')).toBeInTheDocument();
  });

  it('R-024-05 uses the Slovene form for five or more Contacts', async () => {
    backend('EDITOR', () => answer([certain(5)]));
    renderPage();
    expect(await screen.findByText('5 kontaktov ima e-pošto ana@acme.com')).toBeInTheDocument();
  });

  it('R-024-05 names what a probable set shares, for company, phone and both', async () => {
    backend('OWNER', () =>
      answer([
        probable('duplicate.same_name_company'),
        { ...probable('duplicate.same_name_phone'), key: 'k3' },
        { ...probable('duplicate.same_name_company_or_phone'), key: 'k4' },
      ]),
    );
    renderPage();

    expect(await screen.findAllByText('Verjetno')).toHaveLength(3);
    expect(screen.getByText('2 kontakta imata enako ime in podjetje: Ana Novak')).toBeInTheDocument();
    expect(screen.getByText('2 kontakta imata enako ime in telefon: Ana Novak')).toBeInTheDocument();
    expect(
      screen.getByText('2 kontakta imata enako ime ter podjetje ali telefon: Ana Novak'),
    ).toBeInTheDocument();
  });

  it('R-024-05 shows the name, the e-mail, the company, the city and the creation date', async () => {
    backend('OWNER', () => answer([probable('duplicate.same_name_company')]));
    renderPage();

    const first = (await screen.findAllByRole('listitem'))[0]!;
    expect(first).toHaveTextContent('Ana Novak');
    expect(first).toHaveTextContent('Acme');
    expect(first).toHaveTextContent('Ljubljana');
    expect(first).toHaveTextContent('ustvarjen 6. 11. 2026');
  });

  it('R-026-01 every Contact of a set has a box, all ticked, and the button counts them', async () => {
    backend('EDITOR', () => answer([certain(3)]));
    renderPage();

    const boxes = await screen.findAllByRole('checkbox');
    expect(boxes).toHaveLength(3);
    expect(boxes.every((box) => box.getAttribute('aria-checked') === 'true')).toBe(true);
    expect(screen.getByRole('checkbox', { name: 'Izberi kontakt Ana1' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Združi izbrane (3)' })).toBeInTheDocument();
  });

  it('R-026-01 "Združi izbrane" opens the Merge screen with the ticked Contacts only', async () => {
    backend('EDITOR', () => answer([certain(3)]));
    renderPage();
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Izberi kontakt Ana1' }));

    await userEvent.click(screen.getByRole('button', { name: 'Združi izbrane (2)' }));

    expect(await screen.findByText('merge screen ?contact=c0&contact=c2')).toBeInTheDocument();
  });

  it('R-026-01 with fewer than two ticked Contacts the button is off and says why', async () => {
    backend('EDITOR', () => answer([certain(2)]));
    renderPage();
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Izberi kontakt Ana1' }));

    const merge = screen.getByRole('button', { name: 'Združi izbrane (1)' });
    expect(merge).toHaveAttribute('aria-disabled', 'true');
    expect(screen.getByText('Za združitev izberite vsaj dva kontakta.')).toBeInTheDocument();
    await userEvent.click(merge);
    expect(screen.queryByText(/merge screen/)).not.toBeInTheDocument();
  });

  it('R-026-10 nothing is merged by the page itself: it only reads and navigates', async () => {
    const sent = backend('EDITOR', () => answer([certain(2)]));
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Združi izbrane (2)' }));

    expect(sent.every((s) => s.method === 'GET')).toBe(true);
  });

  it('R-024-10 shows "No duplicates found" when there is no set', async () => {
    backend('OWNER', () => answer([]));
    renderPage();
    expect(await screen.findByRole('heading', { name: 'Ni podvojenih kontaktov' })).toBeInTheDocument();
  });

  it('R-024-06 only reads: a search sends GET and "Poišči znova" sends GET again', async () => {
    const sent = backend('OWNER', () => answer([certain(2)]));
    renderPage();
    await screen.findByText('Zanesljivo');
    await userEvent.click(screen.getByRole('button', { name: 'Poišči znova' }));

    await vi.waitFor(() => expect(sent).toHaveLength(2));
    expect(sent.every((s) => s.method === 'GET' && s.url.endsWith('/groups/g1/duplicates'))).toBe(true);
  });

  it('R-024-09 asks a Viewer for nothing and says it is not allowed', async () => {
    const sent = backend('VIEWER', () => answer([]));
    renderPage();

    expect(await screen.findByText('Za iskanje podvojenih kontaktov nimate pravice.')).toBeInTheDocument();
    expect(sent).toHaveLength(0);
  });

  it('shows an error with a retry when the search fails', async () => {
    let fail = true;
    backend('OWNER', () => (fail ? answer({ code: 'error.internal', message: 'x' }, 500) : answer([])));
    renderPage();

    await screen.findByRole('alert');
    fail = false;
    await userEvent.click(screen.getByRole('button', { name: 'Poskusi znova' }));
    expect(await screen.findByRole('heading', { name: 'Ni podvojenih kontaktov' })).toBeInTheDocument();
  });

  it('shows the texts in English', async () => {
    await setLanguage('en');
    backend('OWNER', () => answer([certain(3)]));
    renderPage();

    expect(await screen.findByText('Certain')).toBeInTheDocument();
    expect(screen.getByText('3 contacts share the e-mail ana@acme.com')).toBeInTheDocument();
  });
});
