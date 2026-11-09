import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { VersionsTab } from './VersionsTab';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

const list = [
  { number: 3, createdAt: '2026-11-08T10:00:00Z', userName: 'Boris Kos' },
  { number: 2, createdAt: '2026-11-07T08:12:00Z', userName: 'Boris Kos' },
  { number: 1, createdAt: '2026-11-06T14:40:00Z', userName: 'Ana Novak' },
];

const v1 = {
  number: 1,
  createdAt: '2026-11-06T14:40:00Z',
  userName: 'Ana Novak',
  content: {
    id: 'c1',
    version: 1,
    firstName: 'Ana',
    lastName: 'Novak',
    company: 'Acme',
    country: 'SI',
    tags: ['gost'],
    attributes: [{ key: 'kompetenca', value: 'java' }],
    createdAt: '2026-11-06T14:40:00Z',
    updatedAt: '2026-11-06T14:40:00Z',
  },
};

const comparison = {
  number: 1,
  differences: [
    { kind: 'field', name: 'company', versionValue: 'Acme', currentValue: 'Beta' },
    { kind: 'tag', name: 'vip', versionValue: null, currentValue: 'vip' },
    { kind: 'tag', name: 'gost', versionValue: 'gost', currentValue: null },
    { kind: 'attribute', name: 'kompetenca', versionValue: 'java', currentValue: 'kotlin' },
  ],
};

/** The backend: the list, one Version, the comparison; `listAnswer` can be replaced. */
function backend(
  listAnswer: () => Response = () => answer(list),
  compareAnswer = () => answer(comparison),
  restoreAnswer: () => Response = () => answer({ ...v1.content, version: 4 }),
) {
  vi.mocked(fetch).mockImplementation((url) => {
    const path = String(url);
    if (path.endsWith('/restore')) return Promise.resolve(restoreAnswer());
    if (path.endsWith('/compare')) return Promise.resolve(compareAnswer());
    if (path.endsWith('/versions/1')) return Promise.resolve(answer(v1));
    return Promise.resolve(listAnswer());
  });
}

function renderTab() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <VersionsTab groupId="g1" contactId="c1" />
    </QueryClientProvider>,
  );
}

describe('VersionsTab WF-07 (R-022-02..04, AC-022-01..04, EX-022-01..02)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('AC-022-01 lists v3, v2, v1 with time in Europe/Ljubljana and user', async () => {
    backend();
    renderTab();

    const rows = await screen.findAllByRole('row');
    expect(within(rows[1] as HTMLElement).getByText('v3')).toBeInTheDocument();
    expect(within(rows[1] as HTMLElement).getByText('8. 11. 2026 11:00')).toBeInTheDocument();
    expect(within(rows[1] as HTMLElement).getByText('Boris Kos')).toBeInTheDocument();
    expect(within(rows[2] as HTMLElement).getByText('v2')).toBeInTheDocument();
    expect(within(rows[3] as HTMLElement).getByText('v1')).toBeInTheDocument();
    expect(within(rows[3] as HTMLElement).getByText('Ana Novak')).toBeInTheDocument();
  });

  it('R-022-02 marks the highest Version as the current one and offers no comparison for it', async () => {
    backend();
    renderTab();

    const rows = await screen.findAllByRole('row');
    expect(within(rows[1] as HTMLElement).getByText('trenutna')).toBeInTheDocument();
    expect(within(rows[1] as HTMLElement).queryByRole('button', { name: /Primerjaj/ })).toBeNull();
    expect(within(rows[2] as HTMLElement).getByRole('button', { name: /Primerjaj/ })).toBeInTheDocument();
  });

  it('AC-022-02 opening v1 shows its fields, Tags and Attributes', async () => {
    backend();
    renderTab();

    const rows = await screen.findAllByRole('row');
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: /Odpri/ }));

    expect(await screen.findByRole('heading', { name: 'Različica 1' })).toBeInTheDocument();
    expect(screen.getByText('Acme')).toBeInTheDocument();
    expect(screen.getByText('Slovenija')).toBeInTheDocument();
    expect(screen.getByText('gost')).toBeInTheDocument();
    expect(screen.getByText('kompetenca')).toBeInTheDocument();
    expect(screen.getByText('java')).toBeInTheDocument();
  });

  it('AC-022-03 and AC-022-04 comparing shows both values, a Tag added or removed and an Attribute', async () => {
    backend();
    renderTab();

    const rows = await screen.findAllByRole('row');
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: /Primerjaj/ }));

    expect(
      await screen.findByRole('heading', { name: /Razlike: različica 1 in trenutna \(v3\)/ }),
    ).toBeInTheDocument();
    const company = screen.getByRole('row', { name: /Podjetje/ });
    expect(within(company).getByText('Acme')).toBeInTheDocument();
    expect(within(company).getByText('Beta')).toBeInTheDocument();
    expect(within(screen.getByRole('row', { name: /vip/ })).getByText(/dodana/)).toBeInTheDocument();
    expect(within(screen.getByRole('row', { name: /gost/ })).getByText(/odstranjena/)).toBeInTheDocument();
    expect(screen.getByRole('row', { name: /kompetenca/ })).toHaveTextContent('kotlin');
  });

  it('EX-022-01 shows "no differences" when there is none', async () => {
    backend(undefined, () => answer({ number: 1, differences: [] }));
    renderTab();

    const rows = await screen.findAllByRole('row');
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: /Primerjaj/ }));

    expect(await screen.findByText('Ni razlik.')).toBeInTheDocument();
  });

  it('goes back from an opened Version to the list', async () => {
    backend();
    renderTab();

    const rows = await screen.findAllByRole('row');
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: /Odpri/ }));
    await userEvent.click(await screen.findByRole('button', { name: /Nazaj na različice/ }));

    expect(await screen.findAllByRole('row')).toHaveLength(4);
  });

  it('shows no control that changes or deletes a Version (R-022-05)', async () => {
    backend();
    renderTab();

    await screen.findAllByRole('row');
    expect(screen.queryByRole('button', { name: /Izbriši|Uredi/ })).toBeNull();
  });

  it('UI-14 shows the error state with a retry', async () => {
    backend(() => answer({ code: 'x', message: 'x' }, 500));
    renderTab();

    expect(await screen.findByRole('button', { name: 'Poskusi znova' })).toBeInTheDocument();
  });

  it('shows the English labels after the language is switched', async () => {
    await setLanguage('en');
    backend();
    renderTab();

    const rows = await screen.findAllByRole('row');
    expect(within(rows[1] as HTMLElement).getByText('current')).toBeInTheDocument();
    expect(within(rows[1] as HTMLElement).getByText('2026-11-08 11:00')).toBeInTheDocument();
  });
});

const restoreCalls = () =>
  vi
    .mocked(fetch)
    .mock.calls.filter(([url]) => String(url).endsWith('/restore'))
    .map(([url, init]) => ({ url: String(url), method: init?.method, body: JSON.parse(String(init?.body)) }));

describe('VersionsTab restore WF-07 (R-023-01, R-023-03, R-023-04, AC-023-01, AC-023-04, AC-023-05)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('R-023-01 and EX-023-01 offer "Obnovi" for v2 and v1 but not for the current Version', async () => {
    backend();
    renderTab();

    const rows = await screen.findAllByRole('row');
    expect(within(rows[1] as HTMLElement).queryByRole('button', { name: 'Obnovi' })).toBeNull();
    expect(within(rows[2] as HTMLElement).getByRole('button', { name: 'Obnovi' })).toBeInTheDocument();
    expect(within(rows[3] as HTMLElement).getByRole('button', { name: 'Obnovi' })).toBeInTheDocument();
  });

  it('R-023-01 asks for confirmation that names the new Version and sends nothing on "Prekliči"', async () => {
    backend();
    renderTab();

    const rows = await screen.findAllByRole('row');
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: 'Obnovi' }));

    const dialog = await screen.findByRole('dialog', { name: 'Obnovim različico 1?' });
    expect(dialog).toHaveTextContent(
      'Vsebina različice 1 bo shranjena kot nova različica 4. Prejšnje različice ostanejo.',
    );
    await userEvent.click(within(dialog).getByRole('button', { name: 'Prekliči' }));

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(restoreCalls()).toEqual([]);
  });

  it('AC-023-01 confirming sends the shown version and reloads the list with the new Version', async () => {
    let restored = false;
    const withV4 = [{ number: 4, createdAt: '2026-11-09T09:00:00Z', userName: 'Boris Kos' }, ...list];
    backend(
      () => answer(restored ? withV4 : list),
      undefined,
      () => {
        restored = true;
        return answer({ ...v1.content, version: 4 });
      },
    );
    renderTab();

    const rows = await screen.findAllByRole('row');
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: 'Obnovi' }));
    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Obnovi' }));

    expect(await screen.findAllByRole('row')).toHaveLength(5);
    expect(within(screen.getAllByRole('row')[1] as HTMLElement).getByText('v4')).toBeInTheDocument();
    expect(restoreCalls()).toEqual([
      {
        url: expect.stringMatching(/\/contacts\/c1\/versions\/1\/restore$/),
        method: 'POST',
        body: { version: 3 },
      },
    ]);
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('R-023-01 an opened Version has "Obnovi to različico" and the list is shown after the restore', async () => {
    backend();
    renderTab();

    const rows = await screen.findAllByRole('row');
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: /Odpri/ }));
    await userEvent.click(await screen.findByRole('button', { name: 'Obnovi to različico' }));
    const dialog = await screen.findByRole('dialog', { name: 'Obnovim različico 1?' });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Obnovi' }));

    expect(await screen.findAllByRole('row')).toHaveLength(4);
    expect(restoreCalls()).toHaveLength(1);
  });

  it('R-023-01 the confirm button is disabled and says "Obnavljanje …" while the restore runs', async () => {
    let finish: (r: Response) => void = () => undefined;
    vi.mocked(fetch).mockImplementation((url) => {
      const path = String(url);
      if (path.endsWith('/restore')) return new Promise((resolve) => (finish = resolve));
      return Promise.resolve(answer(list));
    });
    renderTab();

    const rows = await screen.findAllByRole('row');
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: 'Obnovi' }));
    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Obnovi' }));

    expect(await within(dialog).findByRole('button', { name: 'Obnavljanje …' })).toBeDisabled();
    finish(answer({ ...v1.content, version: 4 }));
  });

  it('AC-023-05 a stale restore shows "changed by someone else", nothing restored, and the new list', async () => {
    const current = { ...v1.content, version: 4 };
    backend(undefined, undefined, () =>
      answer({ code: 'contact.changed_by_someone_else', message: 'x', current }, 409),
    );
    renderTab();

    const rows = await screen.findAllByRole('row');
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: 'Obnovi' }));
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Obnovi' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Kontakt je spremenil nekdo drug.');
    expect(alert).toHaveTextContent('Prikazana je trenutna različica. Nič ni bilo obnovljeno.');
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('AC-023-04 a restore that breaks BR-06 shows the reason and nothing is restored', async () => {
    backend(undefined, undefined, () =>
      answer(
        {
          code: 'validation.failed',
          message: 'x',
          fieldErrors: [{ field: 'email', code: 'validation.email_invalid' }],
        },
        422,
      ),
    );
    renderTab();

    const rows = await screen.findAllByRole('row');
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: 'Obnovi' }));
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Obnovi' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Podatki niso veljavni.');
    expect(alert).toHaveTextContent('E-poštni naslov ni veljaven.');
    expect(screen.getAllByRole('row')).toHaveLength(4);
  });

  it('R-023-01 the refusal goes away when the user tries again and it works', async () => {
    let first = true;
    backend(undefined, undefined, () => {
      if (first) {
        first = false;
        return answer({ code: 'validation.failed', message: 'x', fieldErrors: [] }, 422);
      }
      return answer({ ...v1.content, version: 4 });
    });
    renderTab();

    for (let attempt = 0; attempt < 2; attempt += 1) {
      const rows = await screen.findAllByRole('row');
      await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: 'Obnovi' }));
      await userEvent.click(
        within(await screen.findByRole('dialog')).getByRole('button', { name: 'Obnovi' }),
      );
      if (attempt === 0) await screen.findByRole('alert');
    }

    await waitFor(() => expect(screen.queryByRole('alert')).toBeNull());
  });

  it('shows the English labels of the restore after the language is switched', async () => {
    await setLanguage('en');
    backend();
    renderTab();

    const rows = await screen.findAllByRole('row');
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: 'Restore' }));

    expect(await screen.findByRole('dialog', { name: 'Restore version 1?' })).toBeInTheDocument();
  });
});
