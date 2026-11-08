import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { beforeEach, describe, expect, it } from 'vitest';
import { setLanguage } from '@/i18n';
import { routes } from './routes';

function renderAt(path: string) {
  return render(<RouterProvider router={createMemoryRouter(routes, { initialEntries: [path] })} />);
}

describe('app shell (UI-10, UI-19, US-019)', () => {
  beforeEach(async () => {
    window.localStorage.clear();
    await setLanguage('sl');
  });

  it('shows the product name, one h1, navigation, main and footer landmarks', () => {
    renderAt('/');
    expect(screen.getByRole('banner')).toHaveTextContent('Skupine kontaktov');
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    expect(screen.getByRole('main')).toBeInTheDocument();
    expect(screen.getByRole('contentinfo')).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Glavna navigacija' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Preskoči na vsebino' })).toHaveAttribute('href', '#content');
  });

  it('switches the whole UI to English and back without reloading', async () => {
    renderAt('/');
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Dobrodošli');
    await userEvent.click(screen.getByRole('button', { name: 'EN' }));
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Welcome');
    expect(screen.getByRole('banner')).toHaveTextContent('Contact Groups');
    expect(screen.getByRole('button', { name: 'EN' })).toHaveAttribute('aria-pressed', 'true');
    await userEvent.click(screen.getByRole('button', { name: 'SL' }));
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Dobrodošli');
  });

  it('shows the same page for an unknown route (BR-01)', () => {
    renderAt('/does/not/exist');
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('Strani ni mogoče najti');
    expect(screen.getByRole('link', { name: 'Nazaj na začetek' })).toHaveAttribute('href', '/');
  });
});
