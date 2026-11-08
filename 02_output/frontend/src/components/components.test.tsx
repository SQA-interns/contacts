import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import '@/i18n';
import { ConfirmDialog } from './ConfirmDialog';
import { EmptyState } from './EmptyState';
import { ErrorState } from './ErrorState';
import { LoadingState } from './LoadingState';
import { ResponsiveTable } from './ResponsiveTable';

describe('shared states (UI-14)', () => {
  it('EmptyState explains and offers the next action', () => {
    render(<EmptyState title="Title" body="Body" action={<button type="button">Go</button>} />);
    expect(screen.getByRole('heading', { name: 'Title' })).toBeInTheDocument();
    expect(screen.getByText('Body')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Go' })).toBeInTheDocument();
  });

  it('ErrorState is announced, offers a retry and shows no detail', async () => {
    const retry = vi.fn();
    render(<ErrorState onRetry={retry} />);
    expect(screen.getByRole('alert')).toHaveTextContent('Nekaj je šlo narobe');
    await userEvent.click(screen.getByRole('button', { name: 'Poskusi znova' }));
    expect(retry).toHaveBeenCalledOnce();
  });

  it('LoadingState has an accessible status', () => {
    render(<LoadingState rows={2} />);
    expect(screen.getByRole('status')).toHaveTextContent('Nalaganje');
  });
});

describe('ConfirmDialog (UI-15)', () => {
  it('confirms and cancels', async () => {
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    render(
      <ConfirmDialog
        open
        title="Delete?"
        description="Cannot be undone"
        confirmLabel="Delete"
        destructive
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    );
    const dialog = screen.getByRole('dialog', { name: 'Delete?' });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));
    expect(onConfirm).toHaveBeenCalledOnce();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Prekliči' }));
    expect(onCancel).toHaveBeenCalledOnce();
  });

  it('disables both buttons while busy', () => {
    render(
      <ConfirmDialog
        open
        busy
        title="T"
        description="D"
        confirmLabel="Go"
        onConfirm={() => undefined}
        onCancel={() => undefined}
      />,
    );
    expect(screen.getByRole('button', { name: 'Go' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Prekliči' })).toBeDisabled();
  });
});

describe('ResponsiveTable (UI-16)', () => {
  const columns = [{ key: 'name', header: 'Name', cell: (r: { id: string; name: string }) => r.name }];
  it('renders a table and a card list with the same rows', () => {
    render(
      <ResponsiveTable
        caption="People"
        columns={columns}
        rows={[
          { id: '1', name: 'Ana' },
          { id: '2', name: 'Boris' },
        ]}
        rowKey={(r) => r.id}
      />,
    );
    expect(screen.getAllByText('Ana')).toHaveLength(2);
    expect(screen.getAllByText('Boris')).toHaveLength(2);
    expect(screen.getByRole('list', { name: 'People', hidden: true })).toBeInTheDocument();
  });
});
