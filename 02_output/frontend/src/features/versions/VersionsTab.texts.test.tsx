import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Toaster } from '@/components/ui/sonner';
import { setLanguage } from '@/i18n';
import { VersionsTab } from './VersionsTab';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

const list = [
  { number: 3, createdAt: '2026-11-08T10:00:00Z', userName: 'Boris Kos' },
  { number: 2, createdAt: '2026-07-07T08:12:00Z', userName: 'Boris Kos' },
  { number: 1, createdAt: '2026-11-06T14:40:00Z', userName: 'Ana Novak' },
];
const content = {
  id: 'c1',
  version: 1,
  firstName: 'Ana',
  lastName: 'Novak',
  company: 'Acme',
  tags: ['gost'],
  attributes: [{ key: 'skill', value: 'java' }],
  createdAt: '2026-11-06T14:40:00Z',
  updatedAt: '2026-11-06T14:40:00Z',
};
const comparison = {
  number: 1,
  differences: [
    { kind: 'field', name: 'company', versionValue: 'Acme', currentValue: 'Beta' },
    { kind: 'field', name: 'mystery', versionValue: null, currentValue: 'x' },
    { kind: 'tag', name: 'vip', versionValue: null, currentValue: 'vip' },
    { kind: 'tag', name: 'gost', versionValue: 'gost', currentValue: null },
    { kind: 'attribute', name: 'skill', versionValue: 'java', currentValue: null },
  ],
};

type Handler = (path: string, method: string) => Response | undefined;

function backend(handler: Handler = () => undefined, versions: unknown = list) {
  const paths: string[] = [];
  vi.mocked(fetch).mockImplementation((url, init) => {
    const path = String(url);
    const method = init?.method ?? 'GET';
    paths.push(`${method} ${path.replace(/^.*\/api\/v1/, '')}`);
    const custom = handler(path, method);
    if (custom) return Promise.resolve(custom);
    if (path.endsWith('/restore')) return Promise.resolve(answer({ ...content, version: 4 }));
    if (path.endsWith('/compare')) return Promise.resolve(answer(comparison));
    if (path.endsWith('/versions/1'))
      return Promise.resolve(
        answer({ number: 1, createdAt: list[2]?.createdAt, userName: 'Ana Novak', content }),
      );
    return Promise.resolve(answer(versions));
  });
  return paths;
}

function renderTab() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <VersionsTab groupId="g 1" contactId="c/1" />
      <Toaster />
    </QueryClientProvider>,
  );
}

const rowsOf = async () => {
  const table = await screen.findByRole('table', { name: 'Versions of the contact' });
  return within(table).getAllByRole('row');
};

describe('VersionsTab: texts, views and refusals (R-022-02..05, R-023-01..04)', () => {
  beforeEach(async () => {
    await setLanguage('en');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('shows the heading, the headers, the current badge on the highest Version only, and the time zone of Ljubljana', async () => {
    const paths = backend();
    renderTab();
    const rows = await rowsOf();
    expect(screen.getByRole('heading', { name: 'Versions' })).toBeInTheDocument();
    expect(
      within(rows[0] as HTMLElement)
        .getAllByRole('columnheader')
        .map((h) => h.textContent),
    ).toEqual(['Version', 'Time', 'User', 'Actions']);
    const cells = rows.slice(1).map((r) =>
      within(r)
        .getAllByRole('cell')
        .map((c) => c.textContent),
    );
    expect(cells[0]?.slice(0, 3)).toEqual(['v3current', '2026-11-08 11:00', 'Boris Kos']);
    expect(cells[1]?.slice(0, 3)).toEqual(['v2', '2026-07-07 10:12', 'Boris Kos']);
    expect(cells[2]?.slice(0, 3)).toEqual(['v1', '2026-11-06 15:40', 'Ana Novak']);
    expect(cells.map((c) => c[3])).toEqual([
      'Open',
      'OpenCompare with currentRestore',
      'OpenCompare with currentRestore',
    ]);
    expect(paths).toEqual(['GET /groups/g%201/contacts/c%2F1/versions']);
  });

  it('shows the empty state, and an error with a retry', async () => {
    backend(() => undefined, []);
    renderTab();
    expect(await screen.findByText('This contact has no versions yet.')).toBeInTheDocument();
  });

  it('shows the error of the list with a retry that asks again', async () => {
    const paths = backend(() => answer({ code: 'server.error', message: 'x' }, 500));
    renderTab();
    const retry = await screen.findByRole('button', { name: /try again|retry/i });
    await userEvent.click(retry);
    await waitFor(() => expect(paths.length).toBeGreaterThan(1));
  });

  it('opens a Version with its title, content, a Restore button and a way back', async () => {
    const paths = backend();
    renderTab();
    const rows = await rowsOf();
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: 'Open' }));
    expect(await screen.findByRole('heading', { name: 'Version 1' })).toBeInTheDocument();
    expect(screen.getByText('Ana')).toBeInTheDocument();
    expect(screen.getByText('gost')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Restore this version' })).toBeInTheDocument();
    expect(paths.at(-1)).toBe('GET /groups/g%201/contacts/c%2F1/versions/1');
    await userEvent.click(screen.getByRole('button', { name: 'Back to versions' }));
    expect(await screen.findByRole('table', { name: 'Versions of the contact' })).toBeInTheDocument();
  });

  it('offers no restore on the opened current Version', async () => {
    backend((path) =>
      path.endsWith('/versions/3')
        ? answer({
            number: 3,
            createdAt: list[0]?.createdAt,
            userName: 'Boris Kos',
            content: { ...content, version: 3 },
          })
        : undefined,
    );
    renderTab();
    const rows = await rowsOf();
    await userEvent.click(within(rows[1] as HTMLElement).getByRole('button', { name: 'Open' }));
    expect(await screen.findByRole('heading', { name: 'Version 3' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Restore this version' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Back to versions' })).toBeInTheDocument();
  });

  it('shows the error of an opened Version with a retry', async () => {
    const paths = backend((path) =>
      path.endsWith('/versions/1') ? answer({ code: 'version.not_found', message: 'x' }, 404) : undefined,
    );
    renderTab();
    const rows = await rowsOf();
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: 'Open' }));
    expect(await screen.findByText('Version not found.')).toBeInTheDocument();
    const before = paths.length;
    await userEvent.click(screen.getByRole('button', { name: /try again|retry/i }));
    await waitFor(() => expect(paths.length).toBeGreaterThan(before));
  });

  it('compares with the current Version: title, headers, labels of fields, Tags and Attributes', async () => {
    const paths = backend();
    renderTab();
    const rows = await rowsOf();
    await userEvent.click(
      within(rows[3] as HTMLElement).getByRole('button', { name: 'Compare with current' }),
    );
    expect(
      await screen.findByRole('heading', { name: 'Differences: version 1 and current (v3)' }),
    ).toBeInTheDocument();
    expect(paths.at(-1)).toBe('GET /groups/g%201/contacts/c%2F1/versions/1/compare');
    const table = await screen.findByRole('table', {
      name: 'Differences between version 1 and the current one',
    });
    const diff = within(table).getAllByRole('row');
    expect(
      within(diff[0] as HTMLElement)
        .getAllByRole('columnheader')
        .map((h) => h.textContent),
    ).toEqual(['Field', 'Version 1', 'Current']);
    expect(
      diff.slice(1).map((r) =>
        within(r)
          .getAllByRole('cell')
          .map((c) => c.textContent),
      ),
    ).toEqual([
      ['Company', 'Acme', 'Beta'],
      ['mystery', '—', 'x'],
      ['Tag: vip', '—', 'vip (added)'],
      ['Tag: gost', 'gost', '— (removed)'],
      ['Attribute: skill', 'java', '—'],
    ]);
    await userEvent.click(screen.getByRole('button', { name: 'Back to versions' }));
    expect(await screen.findByRole('table', { name: 'Versions of the contact' })).toBeInTheDocument();
  });

  it('says there are no differences, and shows the error of the comparison with a retry', async () => {
    backend((path) => (path.endsWith('/compare') ? answer({ number: 1, differences: [] }) : undefined));
    renderTab();
    let rows = await rowsOf();
    await userEvent.click(
      within(rows[3] as HTMLElement).getByRole('button', { name: 'Compare with current' }),
    );
    expect(await screen.findByText('No differences.')).toBeInTheDocument();
    expect(screen.queryByRole('table', { name: /Differences between/ })).not.toBeInTheDocument();
    vi.unstubAllGlobals();
    vi.stubGlobal('fetch', vi.fn());
    const paths = backend((path) =>
      path.endsWith('/compare') ? answer({ code: 'server.error', message: 'x' }, 500) : undefined,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Back to versions' }));
    rows = await rowsOf();
    await userEvent.click(
      within(rows[2] as HTMLElement).getByRole('button', { name: 'Compare with current' }),
    );
    const retry = await screen.findByRole('button', { name: /try again|retry/i });
    const before = paths.length;
    await userEvent.click(retry);
    await waitFor(() => expect(paths.length).toBeGreaterThan(before));
  });

  it('asks before restoring with the number of the new Version, restores the shown Version and says so', async () => {
    const paths = backend();
    renderTab();
    const rows = await rowsOf();
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: 'Restore' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText('Restore version 1?')).toBeInTheDocument();
    expect(
      within(dialog).getByText(
        'The content of version 1 will be saved as the new version 4. Earlier versions stay.',
      ),
    ).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Restore' }));
    expect(await screen.findByText('Version 1 is restored as v4.')).toBeInTheDocument();
    const post = paths.find((p) => p.startsWith('POST'));
    expect(post).toBe('POST /groups/g%201/contacts/c%2F1/versions/1/restore');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('shows the stale refusal with its explanation, and the validation reasons once each, and clears them on the next try', async () => {
    let answerNext = () =>
      answer(
        {
          code: 'validation.failed',
          message: 'x',
          fieldErrors: [
            { field: 'email', code: 'validation.email_invalid' },
            { field: 'email2', code: 'validation.email_invalid' },
            { field: 'x', code: 'no.such.code' },
          ],
        },
        422,
      );
    backend((path) => (path.endsWith('/restore') ? answerNext() : undefined));
    renderTab();
    let rows = await rowsOf();
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: 'Restore' }));
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Restore' }));
    let alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('The data is not valid.');
    expect(
      within(alert)
        .getAllByRole('listitem')
        .map((li) => li.textContent),
    ).toEqual(['The e-mail address is not valid.']);
    expect(alert).toHaveTextContent('Nothing was restored.');
    answerNext = () =>
      answer(
        { code: 'contact.changed_by_someone_else', message: 'x', current: { ...content, version: 5 } },
        409,
      );
    rows = await rowsOf();
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: 'Restore' }));
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Restore' }));
    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('This contact was changed by someone else.'),
    );
    alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('The current version is shown. Nothing was restored.');
    expect(within(alert).queryByRole('listitem')).not.toBeInTheDocument();
  });

  it('shows a refusal that is not an API error without reasons', async () => {
    backend((path) => (path.endsWith('/restore') ? answer('not json', 500) : undefined));
    renderTab();
    const rows = await rowsOf();
    await userEvent.click(within(rows[3] as HTMLElement).getByRole('button', { name: 'Restore' }));
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Restore' }));
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Nothing was restored.');
    expect(within(alert).queryByRole('listitem')).not.toBeInTheDocument();
  });
});
