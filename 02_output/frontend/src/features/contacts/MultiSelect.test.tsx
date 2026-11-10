import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { MultiSelect } from './MultiSelect';

function Harness({ initial = [], spy }: { initial?: string[]; spy?: (next: string[]) => void }) {
  const [value, setValue] = useState(initial);
  return (
    <MultiSelect
      label="Tags"
      placeholder="Any tag"
      options={['a', 'b', 'c']}
      value={value}
      onChange={(next) => {
        spy?.(next);
        setValue(next);
      }}
    />
  );
}

describe('MultiSelect (R-009-01..03)', () => {
  it('shows the placeholder, or the chosen values joined, and starts closed', () => {
    const { unmount } = render(<Harness />);
    const button = screen.getByRole('button', { name: /Tags/ });
    expect(button).toHaveAttribute('aria-expanded', 'false');
    expect(screen.getByText('Any tag')).toHaveClass('text-text-muted');
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    unmount();
    render(<Harness initial={['a', 'c']} />);
    expect(screen.getByText('a, c')).not.toHaveClass('text-text-muted');
    expect(screen.queryByText('Any tag')).not.toBeInTheDocument();
  });

  it('opens and closes with the button and marks the chosen options', async () => {
    render(<Harness initial={['b']} />);
    const button = screen.getByRole('button', { name: /Tags/ });
    await userEvent.click(button);
    expect(button).toHaveAttribute('aria-expanded', 'true');
    const options = screen.getAllByRole('option');
    expect(options.map((o) => o.getAttribute('aria-selected'))).toEqual(['false', 'true', 'false']);
    await userEvent.click(button);
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });

  it('adds an option with a click and takes it out with a second click', async () => {
    const spy = vi.fn();
    render(<Harness initial={['a']} spy={spy} />);
    await userEvent.click(screen.getByRole('button', { name: /Tags/ }));
    await userEvent.click(screen.getByRole('option', { name: 'c' }));
    expect(spy).toHaveBeenLastCalledWith(['a', 'c']);
    await userEvent.click(screen.getByRole('option', { name: 'a' }));
    expect(spy).toHaveBeenLastCalledWith(['c']);
    expect(spy).toHaveBeenCalledTimes(2);
  });

  it('chooses with Enter and with Space on an option, and ignores other keys', async () => {
    const spy = vi.fn();
    render(<Harness spy={spy} />);
    await userEvent.click(screen.getByRole('button', { name: /Tags/ }));
    screen.getByRole('option', { name: 'b' }).focus();
    await userEvent.keyboard('{Enter}');
    expect(spy).toHaveBeenLastCalledWith(['b']);
    await userEvent.keyboard(' ');
    expect(spy).toHaveBeenLastCalledWith([]);
    await userEvent.keyboard('x');
    await userEvent.keyboard('{ArrowDown}');
    expect(spy).toHaveBeenCalledTimes(2);
  });

  it('closes with Escape on the button and on an option, and with no other key', async () => {
    render(<Harness />);
    const button = screen.getByRole('button', { name: /Tags/ });
    await userEvent.click(button);
    button.focus();
    await userEvent.keyboard('x');
    expect(screen.getByRole('listbox')).toBeInTheDocument();
    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    await userEvent.click(button);
    screen.getByRole('option', { name: 'a' }).focus();
    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });
});
