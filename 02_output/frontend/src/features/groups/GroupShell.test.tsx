import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { GroupShell } from './GroupShell';

const answer = (body: unknown) => new Response(JSON.stringify(body), { status: 200 });
const group = (role: string) => ({ id: 'g1', name: 'Acme', role, createdAt: '2026-11-08T08:00:00Z' });

function renderShell(role: string, path = '/groups/g1/tags') {
  vi.mocked(fetch).mockImplementation((_url, init) =>
    Promise.resolve(init?.method === 'POST' ? new Response(null, { status: 204 }) : answer([group(role)])),
  );
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/groups" element={<p>group list page</p>} />
          <Route path="/groups/:groupId" element={<GroupShell />}>
            <Route path="tags" element={<p>tags page</p>} />
            <Route path="contacts" element={<p>contacts page</p>} />
            <Route path="contacts/:contactId" element={<p>contact page</p>} />
            <Route path="contacts/new" element={<p>new contact page</p>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

/** The tab bar and the mobile menu are both in the DOM; CSS shows one of them (UI-22, UI-23). */
const tabBar = async () => within(await screen.findByRole('navigation', { name: 'Navigacija skupine' }));

describe('GroupShell: Group bar and Group navigation (WF-06..WF-13, F-01, F-02)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
  });
  afterEach(() => vi.unstubAllGlobals());

  it('shows the Group name and the Role of the User', async () => {
    renderShell('OWNER');

    expect(await screen.findByText('Acme')).toBeInTheDocument();
    expect(screen.getByText('Lastnik')).toBeInTheDocument();
  });

  it('F-02 links to the Tag list, so that a User reaches it without typing the address', async () => {
    renderShell('EDITOR', '/groups/g1/contacts');

    const tags = (await tabBar()).getByRole('link', { name: 'Značke' });

    expect(tags).toHaveAttribute('href', '/groups/g1/tags');
  });

  it('offers every Group screen to an Owner and marks the current one', async () => {
    renderShell('OWNER');

    const tabs = await tabBar();

    expect(tabs.getAllByRole('link').map((link) => link.textContent)).toEqual([
      'Kontakti',
      'Značke',
      'Koš',
      'Podvojeni kontakti',
      'Dejavnost',
      'Člani',
      'Povabila',
    ]);
    expect(tabs.getByRole('link', { name: 'Značke' })).toHaveAttribute('aria-current', 'page');
    expect(tabs.getByRole('link', { name: 'Kontakti' })).not.toHaveAttribute('aria-current');
  });

  it('keeps Invitations for the Owner (R-014-10)', async () => {
    renderShell('EDITOR');

    const tabs = await tabBar();

    expect(tabs.getByRole('link', { name: 'Koš' })).toBeInTheDocument();
    expect(tabs.getByRole('link', { name: 'Podvojeni kontakti' })).toBeInTheDocument();
    expect(tabs.queryByRole('link', { name: 'Povabila' })).not.toBeInTheDocument();
  });

  it('shows a Viewer only the screens a Viewer may open (R-006-01, R-012-08, R-017-08, R-015-01, R-024-09)', async () => {
    renderShell('VIEWER');

    const tabs = await tabBar();

    expect(tabs.getAllByRole('link').map((link) => link.textContent)).toEqual(['Kontakti', 'Člani']);
  });

  it('marks Contacts as current on a Contact page and on the new Contact page', async () => {
    renderShell('OWNER', '/groups/g1/contacts/c1');

    expect((await tabBar()).getByRole('link', { name: 'Kontakti' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByText('contact page')).toBeInTheDocument();
  });

  it('offers the same screens in the menu that replaces the tab bar on a phone', async () => {
    renderShell('OWNER');

    await userEvent.click(await screen.findByRole('button', { name: /Meni skupine/ }));

    const menu = await screen.findByRole('menu');
    expect(within(menu).getByRole('menuitem', { name: 'Značke' })).toHaveAttribute('href', '/groups/g1/tags');
    expect(within(menu).getAllByRole('menuitem')).toHaveLength(7);
  });

  it('shows the page of the route inside the shell', async () => {
    renderShell('OWNER');

    expect(await screen.findByText('tags page')).toBeInTheDocument();
  });

  it('R-020-01 offers an Owner "Izbriši skupino" in "Možnosti skupine" and shows the Group list afterwards', async () => {
    renderShell('OWNER');
    await userEvent.click(await screen.findByRole('button', { name: 'Možnosti skupine' }));
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Izbriši skupino' }));
    const dialog = await screen.findByRole('dialog', { name: 'Izbrišem skupino Acme?' });
    await userEvent.type(within(dialog).getByLabelText(/Za potrditev vpišite ime skupine/), 'Acme');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Izbriši skupino' }));

    expect(await screen.findByText('group list page')).toBeInTheDocument();
  });

  it('R-020-06 offers an Editor and a Viewer no "Možnosti skupine"', async () => {
    renderShell('EDITOR');
    await tabBar();

    expect(screen.queryByRole('button', { name: 'Možnosti skupine' })).not.toBeInTheDocument();
  });
});
