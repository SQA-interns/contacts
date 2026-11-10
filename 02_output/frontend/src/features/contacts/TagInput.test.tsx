import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { beforeEach, describe, expect, it } from 'vitest';
import { setLanguage } from '@/i18n';
import { TagInput } from './TagInput';

let latest: string[] = [];

function Harness({
  start = [],
  error,
  suggestions = [],
  onType,
}: {
  start?: string[];
  error?: string;
  suggestions?: string[];
  onType?: (text: string) => void;
}) {
  const [tags, setTags] = useState(start);
  latest = tags;
  return <TagInput tags={tags} onChange={setTags} error={error} suggestions={suggestions} onType={onType} />;
}

const input = () => screen.getByLabelText('Dodaj značko');

describe('TagInput WF-07 (R-005-01..07)', () => {
  beforeEach(() => setLanguage('sl'));

  it('R-005-01 adds the typed Tag on Enter and clears the field', async () => {
    render(<Harness />);

    await userEvent.type(input(), 'udelezenec2025{Enter}');

    expect(latest).toEqual(['udelezenec2025']);
    expect(input()).toHaveValue('');
    expect(screen.getByText('udelezenec2025')).toBeInTheDocument();
  });

  it('R-005-01 adds the typed Tag with the Add button', async () => {
    render(<Harness />);

    await userEvent.type(input(), 'gost');
    await userEvent.click(screen.getByRole('button', { name: 'Dodaj' }));

    expect(latest).toEqual(['gost']);
  });

  it('R-005-02 shows the rule and adds nothing for an invalid name', async () => {
    render(<Harness />);

    await userEvent.type(input(), 'udelezenec 2025{Enter}');

    expect(latest).toEqual([]);
    expect(
      screen.getByText('Ime značke lahko vsebuje črke, števke, - in _ (do 50 znakov).'),
    ).toBeInTheDocument();
    expect(input()).toHaveValue('udelezenec 2025');
  });

  it.each(['a', 'č_š-ž1', 'b'.repeat(50)])('EX-005-01/03 accepts %s', async (name) => {
    render(<Harness />);

    await userEvent.type(input(), `${name}{Enter}`);

    expect(latest).toEqual([name]);
  });

  it.each(['', 'a/b', 'a.b', 'a@b', 'b'.repeat(51)])('EX-005-02/04 refuses "%s"', async (name) => {
    render(<Harness />);

    await userEvent.type(input(), `${name}{Enter}`);

    expect(latest).toEqual([]);
  });

  it('EX-005-06 puts the same Tag on once, ignoring case', async () => {
    render(<Harness start={['gost']} />);

    await userEvent.type(input(), 'GOST{Enter}');

    expect(latest).toEqual(['gost']);
  });

  it('R-005-03 uses the spelling of a suggested Tag that equals the typed name ignoring case', async () => {
    render(<Harness suggestions={['neObvescaj']} />);

    await userEvent.type(input(), 'Neobvescaj{Enter}');

    expect(latest).toEqual(['neObvescaj']);
  });

  it('R-005-06 removes one Tag with its button', async () => {
    render(<Harness start={['a', 'b']} />);

    await userEvent.click(screen.getByRole('button', { name: 'Odstrani značko a' }));

    expect(latest).toEqual(['b']);
  });

  it('R-005-07 reports what is typed and shows the matching suggestions as visible options', async () => {
    const typed: string[] = [];
    render(<Harness suggestions={['gospodar', 'beta']} onType={(text) => typed.push(text)} />);

    await userEvent.type(input(), 'go');

    expect(typed.at(-1)).toBe('go');
    expect(screen.getByRole('listbox', { name: 'Predlagane značke' })).toBeVisible();
    expect(screen.getAllByRole('option').map((o) => o.textContent)).toEqual(['gospodar']);
  });

  it('R-005-07 puts a suggested Tag on with a click on its option', async () => {
    render(<Harness suggestions={['gospodar']} />);

    await userEvent.type(input(), 'go');
    await userEvent.click(screen.getByRole('option', { name: 'gospodar' }));

    expect(latest).toEqual(['gospodar']);
    expect(input()).toHaveValue('');
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });

  it('R-005-07 puts a suggested Tag on with the arrow key and Enter, and closes the list with Escape', async () => {
    render(<Harness suggestions={['gospodar', 'gostitelj']} />);

    await userEvent.type(input(), 'go{ArrowDown}{ArrowDown}{Enter}');
    expect(latest).toEqual(['gostitelj']);

    await userEvent.type(input(), 'go{Escape}');
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    expect(latest).toEqual(['gostitelj']);
  });

  it('R-005-04 refuses a 51st Tag and shows the limit', async () => {
    const fifty = Array.from({ length: 50 }, (_, i) => `t${i + 1}`);
    render(<Harness start={fifty} />);

    await userEvent.type(input(), 'novo{Enter}');

    expect(latest).toHaveLength(50);
    expect(screen.getAllByText('Kontakt ima lahko največ 50 značk.').length).toBeGreaterThan(0);
  });

  it('shows the message the server sent next to the field', () => {
    render(<Harness error="Napaka s strežnika" />);

    expect(screen.getByText('Napaka s strežnika')).toBeInTheDocument();
  });
});
