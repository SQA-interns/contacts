import { act, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { NO_FILTERS, type ContactFilters, type Facets } from '@/api/contacts';
import { setLanguage } from '@/i18n';
import { FilterPanel } from './FilterPanel';

const facets: Facets = {
  tags: ['gost', 'vip'],
  companies: ['Acme', 'Beta'],
  countries: ['SI', 'HR'],
  attributeKeys: ['Skill', 'Level'],
};

function Harness({
  initial = NO_FILTERS,
  onChange,
  onClear = () => undefined,
}: {
  initial?: ContactFilters;
  onChange?: (f: ContactFilters) => void;
  onClear?: () => void;
}) {
  const [filters, setFilters] = useState(initial);
  return (
    <FilterPanel
      facets={facets}
      filters={filters}
      onChange={(next) => {
        onChange?.(next);
        setFilters(next);
      }}
      onClear={onClear}
      className="panel-class"
    />
  );
}

describe('FilterPanel WF-06 (R-009-01..07, R-009-11)', () => {
  beforeEach(async () => {
    await setLanguage('en');
  });
  afterEach(() => vi.useRealTimers());

  it('shows the title, the clear button, the hint and the class it is given', () => {
    render(<Harness />);
    const panel = screen.getByRole('complementary', { name: 'Filters' });
    expect(panel).toHaveClass('panel-class');
    expect(within(panel).getByRole('heading', { name: 'Filters' })).toBeInTheDocument();
    expect(within(panel).getByRole('button', { name: 'Clear filters' })).toBeInTheDocument();
    expect(
      within(panel).getByText('Only the Tags, companies, countries and keys of this Group are offered.'),
    ).toBeInTheDocument();
  });

  it('offers only what the Group has: countries by name, companies, and Attribute keys', () => {
    render(<Harness />);
    expect(
      within(screen.getByLabelText('Country'))
        .getAllByRole('option')
        .map((o) => o.textContent),
    ).toEqual(['All countries', 'Slovenia', 'Croatia']);
    expect(
      within(screen.getByLabelText('Company'))
        .getAllByRole('option')
        .map((o) => o.textContent),
    ).toEqual(['All companies', 'Acme', 'Beta']);
    const keys = within(screen.getByLabelText('Attribute')).getAllByRole('option');
    expect(keys.map((o) => o.textContent)).toEqual(['Key', 'Skill', 'Level']);
    expect(keys.map((o) => (o as HTMLOptionElement).value)).toEqual(['', 'skill', 'level']);
  });

  it('writes the code of a country the browser cannot name', () => {
    render(
      <FilterPanel
        facets={{ ...facets, countries: ['!!'] }}
        filters={NO_FILTERS}
        onChange={() => undefined}
        onClear={() => undefined}
      />,
    );
    expect(within(screen.getByLabelText('Country')).getByRole('option', { name: '!!' })).toBeInTheDocument();
  });

  it('calls onClear when the button is pressed', async () => {
    const onClear = vi.fn();
    render(<Harness onClear={onClear} />);
    await userEvent.click(screen.getByRole('button', { name: 'Clear filters' }));
    expect(onClear).toHaveBeenCalledOnce();
  });

  it('chooses Tags, offers the same Tags for "without", and sets the mode of the Tag filter', async () => {
    const onChange = vi.fn();
    render(<Harness onChange={onChange} />);
    expect(screen.getByRole('button', { name: /^Tags/ })).toHaveTextContent('Choose Tags');
    await userEvent.click(screen.getByRole('button', { name: /^Tags/ }));
    await userEvent.click(
      within(screen.getByRole('listbox', { name: 'Tags' })).getByRole('option', { name: 'vip' }),
    );
    expect(onChange).toHaveBeenLastCalledWith({ ...NO_FILTERS, tags: ['vip'] });
    await userEvent.click(screen.getByRole('button', { name: /^Without Tags/ }));
    await userEvent.click(
      within(screen.getByRole('listbox', { name: 'Without Tags' })).getByRole('option', { name: 'gost' }),
    );
    expect(onChange).toHaveBeenLastCalledWith({ ...NO_FILTERS, tags: ['vip'], withoutTags: ['gost'] });
    const group = screen.getByRole('group', { name: 'Tag filter mode' });
    const all = within(group).getByRole('radio', { name: 'has all of these Tags' });
    const any = within(group).getByRole('radio', { name: 'has any of these Tags' });
    expect(all).toBeChecked();
    expect(any).not.toBeChecked();
    await userEvent.click(any);
    expect(onChange).toHaveBeenLastCalledWith({
      ...NO_FILTERS,
      tags: ['vip'],
      withoutTags: ['gost'],
      tagMode: 'any',
    });
    expect(any).toBeChecked();
    expect(all).not.toBeChecked();
    await userEvent.click(all);
    expect(onChange).toHaveBeenLastCalledWith({
      ...NO_FILTERS,
      tags: ['vip'],
      withoutTags: ['gost'],
      tagMode: 'all',
    });
  });

  it('sets the country and the company', async () => {
    const onChange = vi.fn();
    render(<Harness onChange={onChange} />);
    await userEvent.selectOptions(screen.getByLabelText('Country'), 'HR');
    expect(onChange).toHaveBeenLastCalledWith({ ...NO_FILTERS, country: 'HR' });
    await userEvent.selectOptions(screen.getByLabelText('Company'), 'Beta');
    expect(onChange).toHaveBeenLastCalledWith({ ...NO_FILTERS, country: 'HR', company: 'Beta' });
  });

  it('keeps the Attribute choices disabled until a key is chosen, then chooses between value and existence', async () => {
    const onChange = vi.fn();
    render(<Harness onChange={onChange} />);
    const group = screen.getByRole('group', { name: 'Attribute filter mode' });
    expect(group).toBeDisabled();
    await userEvent.selectOptions(screen.getByLabelText('Attribute'), 'Skill');
    expect(onChange).toHaveBeenLastCalledWith({ ...NO_FILTERS, attrKey: 'skill' });
    expect(group).toBeEnabled();
    const value = screen.getByRole('textbox', { name: 'Value' });
    const hasValue = screen.getByRole('radio', { name: 'has value' });
    const exists = screen.getByRole('radio', { name: 'exists' });
    expect(hasValue).toBeChecked();
    expect(exists).not.toBeChecked();
    expect(value).toBeEnabled();
    await userEvent.click(exists);
    expect(onChange).toHaveBeenLastCalledWith({ ...NO_FILTERS, attrKey: 'skill', attrExists: true });
    expect(value).toBeDisabled();
    expect(exists).toBeChecked();
    expect(hasValue).not.toBeChecked();
    await userEvent.click(hasValue);
    expect(onChange).toHaveBeenLastCalledWith({ ...NO_FILTERS, attrKey: 'skill', attrExists: false });
    expect(value).toBeEnabled();
  });

  it('sends the Attribute value when the typing pauses, once, and not before', async () => {
    vi.useFakeTimers();
    const onChange = vi.fn();
    render(<Harness initial={{ ...NO_FILTERS, attrKey: 'skill' }} onChange={onChange} />);
    const value = screen.getByRole('textbox', { name: 'Value' });
    fireEvent.change(value, { target: { value: 'j' } });
    fireEvent.change(value, { target: { value: 'ja' } });
    fireEvent.change(value, { target: { value: 'jav' } });
    expect(onChange).not.toHaveBeenCalled();
    await act(() => vi.advanceTimersByTimeAsync(299));
    expect(onChange).not.toHaveBeenCalled();
    await act(() => vi.advanceTimersByTimeAsync(2));
    expect(onChange).toHaveBeenCalledExactlyOnceWith({ ...NO_FILTERS, attrKey: 'skill', attrValue: 'jav' });
    await act(() => vi.advanceTimersByTimeAsync(1000));
    expect(onChange).toHaveBeenCalledTimes(1);
  });

  it('shows a value that arrives from outside, without sending it back', async () => {
    vi.useFakeTimers();
    const onChange = vi.fn();
    const { rerender } = render(
      <FilterPanel
        facets={facets}
        filters={{ ...NO_FILTERS, attrKey: 'skill' }}
        onChange={onChange}
        onClear={() => undefined}
      />,
    );
    rerender(
      <FilterPanel
        facets={facets}
        filters={{ ...NO_FILTERS, attrKey: 'skill', attrValue: 'from-address' }}
        onChange={onChange}
        onClear={() => undefined}
      />,
    );
    expect(screen.getByRole('textbox', { name: 'Value' })).toHaveValue('from-address');
    await act(() => vi.advanceTimersByTimeAsync(1000));
    expect(onChange).not.toHaveBeenCalled();
  });
});
