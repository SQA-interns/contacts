import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { beforeEach, describe, expect, it } from 'vitest';
import type { AttributePair } from '@/api/contacts';
import { setLanguage } from '@/i18n';
import { AttributeEditor } from './AttributeEditor';

let latest: AttributePair[] = [];

function Harness({
  start = [],
  errors = {},
  suggestions = [],
}: {
  start?: AttributePair[];
  errors?: Record<string, string>;
  suggestions?: string[];
}) {
  const [pairs, setPairs] = useState(start);
  latest = pairs;
  return <AttributeEditor pairs={pairs} onChange={setPairs} errors={errors} suggestions={suggestions} />;
}

describe('AttributeEditor WF-07 (R-004-02..05)', () => {
  beforeEach(() => setLanguage('sl'));

  it('adds a pair with key and value', async () => {
    render(<Harness />);

    await userEvent.click(screen.getByRole('button', { name: 'Dodaj atribut' }));
    await userEvent.type(screen.getByLabelText('Ključ', { exact: true }), 'kompetenca');
    await userEvent.type(screen.getByLabelText('Vrednost', { exact: true }), 'java');

    expect(latest).toEqual([{ key: 'kompetenca', value: 'java' }]);
  });

  it('F-55 labels every field `Ključ` and `Vrednost` as in WF-07, the row is named by its group', () => {
    render(
      <Harness
        start={[
          { key: 'a', value: '1' },
          { key: 'b', value: '2' },
        ]}
      />,
    );

    expect(screen.getAllByLabelText('Ključ', { exact: true })).toHaveLength(2);
    expect(screen.getAllByLabelText('Vrednost', { exact: true })).toHaveLength(2);
    expect(screen.getByRole('group', { name: 'Atribut 2' })).toContainElement(
      screen.getAllByLabelText('Ključ', { exact: true })[1]!,
    );
  });

  it('removes a pair', async () => {
    render(
      <Harness
        start={[
          { key: 'a', value: '1' },
          { key: 'b', value: '2' },
        ]}
      />,
    );

    await userEvent.click(screen.getByRole('button', { name: 'Odstrani atribut 1' }));

    expect(latest).toEqual([{ key: 'b', value: '2' }]);
  });

  it('R-004-05 offers the keys of the Group while the user types a key', () => {
    render(<Harness start={[{ key: '', value: '' }]} suggestions={['kompetenca', 'projekt']} />);

    const list = screen.getByLabelText('Ključ', { exact: true }).getAttribute('list');
    const options = [...document.querySelectorAll(`datalist#${list} option`)];

    expect(options.map((o) => o.getAttribute('value'))).toEqual(['kompetenca', 'projekt']);
  });

  it('R-004-03 shows the message next to the pair it belongs to', () => {
    render(
      <Harness
        start={[
          { key: 'a', value: '1' },
          { key: 'A', value: '2' },
        ]}
        errors={{ 'attributes[1].key': 'Ključ atributa se ponavlja.' }}
      />,
    );

    const [first, second] = screen.getAllByRole('group', { name: /^Atribut \d$/ });
    expect(within(second!).getByLabelText('Ključ', { exact: true })).toHaveAccessibleDescription(
      'Ključ atributa se ponavlja.',
    );
    expect(within(first!).getByLabelText('Ključ', { exact: true })).not.toHaveAttribute('aria-invalid');
  });

  it('R-004-04 stops adding at 50 pairs and shows the limit', () => {
    const fifty = Array.from({ length: 50 }, (_, i) => ({ key: `k${i}`, value: 'v' }));
    render(<Harness start={fifty} />);

    expect(screen.getByRole('button', { name: 'Dodaj atribut' })).toBeDisabled();
    expect(screen.getByText('Kontakt ima lahko največ 50 atributov.')).toBeInTheDocument();
  });
});
