import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { ANA, fakeAuthClient } from '@/test/fakeAuth';
import { renderApp } from '@/test/renderApp';

describe('app shell (UI-10, UI-19, US-019)', () => {
  beforeEach(async () => {
    window.localStorage.clear();
    await setLanguage('sl');
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response('[]', { status: 200 })),
    );
  });
  afterEach(() => vi.unstubAllGlobals());

  it('shows the product name, one h1, main and footer landmarks and the skip link', async () => {
    renderApp('/sign-in', fakeAuthClient(null).client);
    expect(await screen.findByRole('heading', { level: 1 })).toHaveTextContent('Prijava');
    expect(screen.getByRole('banner')).toHaveTextContent('Skupine kontaktov');
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    expect(screen.getByRole('main')).toBeInTheDocument();
    expect(screen.getByRole('contentinfo')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Preskoči na vsebino' })).toHaveAttribute('href', '#content');
  });

  it('shows the main navigation to a signed-in User with a verified e-mail', async () => {
    renderApp('/groups', fakeAuthClient(ANA).client);
    expect(await screen.findByRole('navigation', { name: 'Glavna navigacija' })).toBeInTheDocument();
  });

  it('switches the whole UI to English and back without reloading', async () => {
    renderApp('/sign-in', fakeAuthClient(null).client);
    expect(await screen.findByRole('heading', { level: 1 })).toHaveTextContent('Prijava');
    await userEvent.click(screen.getByRole('button', { name: 'EN' }));
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Sign in');
    expect(screen.getByRole('banner')).toHaveTextContent('Contact Groups');
    expect(screen.getByRole('button', { name: 'EN' })).toHaveAttribute('aria-pressed', 'true');
    await userEvent.click(screen.getByRole('button', { name: 'SL' }));
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Prijava');
  });

  it('shows the same page for an unknown route (BR-01)', () => {
    renderApp('/does/not/exist', fakeAuthClient(null).client);
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('Strani ni mogoče najti');
    expect(screen.getByRole('link', { name: 'Nazaj na začetek' })).toHaveAttribute('href', '/');
  });
});
