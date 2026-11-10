import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/errors';
import { setLanguage } from '@/i18n';
import { ContactForm, valuesOf } from './ContactForm';

const answer = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

interface Props {
  initial?: Parameters<typeof ContactForm>[0]['initial'];
  busy?: boolean;
  error?: ApiError | null;
  onSubmit?: (...args: unknown[]) => void;
  onCancel?: () => void;
}

function renderForm({
  initial,
  busy = false,
  error = null,
  onSubmit = () => undefined,
  onCancel,
}: Props = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <ContactForm
        groupId="g 1"
        initial={initial}
        submitLabel="Save contact"
        busyLabel="Saving …"
        busy={busy}
        error={error}
        onSubmit={onSubmit}
        onCancel={onCancel}
      />
    </QueryClientProvider>,
  );
}

const fieldErrors = (...errors: [string, string][]) =>
  new ApiError(
    422,
    'validation.failed',
    'x',
    errors.map(([field, code]) => ({ field, code })),
  );

describe('valuesOf', () => {
  it('turns every field into text: an absent or null field is an empty string', () => {
    const values = valuesOf({ firstName: 'Ana', lastName: null, email: '' });
    expect(Object.keys(values)).toHaveLength(11);
    expect(values.firstName).toBe('Ana');
    expect(values.lastName).toBe('');
    expect(values.notes).toBe('');
    expect(Object.values(valuesOf())).toEqual(Array(11).fill(''));
  });
});

describe('ContactForm WF-07 / WF-08 (R-003-01..05)', () => {
  beforeEach(async () => {
    await setLanguage('en');
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.resolve(answer([]))),
    );
  });
  afterEach(() => vi.unstubAllGlobals());

  it('shows the eleven fields with their labels, the initial values and the two hints', () => {
    renderForm({ initial: { firstName: 'Ana', lastName: 'Novak', country: 'SI', notes: 'Hello' } });
    for (const label of [
      'First name',
      'Last name',
      'E-mail',
      'Phone',
      'Company',
      'Job title',
      'Street and house number',
      'Postal code',
      'City',
      'Country',
      'Notes',
    ]) {
      expect(screen.getByLabelText(label)).toBeInTheDocument();
    }
    expect(screen.getByLabelText('First name')).toHaveValue('Ana');
    expect(screen.getByLabelText('Last name')).toHaveValue('Novak');
    expect(screen.getByLabelText('Country')).toHaveValue('SI');
    expect(screen.getByLabelText('Notes')).toHaveValue('Hello');
    expect(screen.getByLabelText('Notes')).toHaveAccessibleDescription('Up to 5,000 characters.');
    expect(
      screen.getByText('Fill in at least one of first name, last name, e-mail or company.'),
    ).toBeInTheDocument();
    expect(screen.getByLabelText('E-mail')).toHaveAttribute('autocomplete', 'off');
  });

  it('lets Street and Country and Notes use the whole width, and the other fields half', () => {
    renderForm();
    const wide = (label: string) => screen.getByLabelText(label).closest('.sm\\:col-span-2') !== null;
    expect(wide('Street and house number')).toBe(true);
    expect(wide('Country')).toBe(true);
    expect(wide('Notes')).toBe(true);
    expect(wide('First name')).toBe(false);
    expect(wide('City')).toBe(false);
  });

  it('offers the countries by name in alphabetical order after "none"', () => {
    renderForm();
    const options = within(screen.getByLabelText('Country')).getAllByRole('option');
    expect(options[0]).toHaveTextContent('— none —');
    expect((options[0] as HTMLOptionElement).value).toBe('');
    const names = options.slice(1).map((o) => o.textContent ?? '');
    expect(names.length).toBeGreaterThan(150);
    expect(names).toEqual([...names].sort((a, b) => a.localeCompare(b, 'en')));
    expect(names).toContain('Slovenia');
    expect(options.find((o) => o.textContent === 'Slovenia')).toHaveProperty('value', 'SI');
  });

  it('hands the typed values, Attributes and Tags to onSubmit when the form is sent', async () => {
    const onSubmit = vi.fn();
    renderForm({ onSubmit, initial: { company: 'Acme' } });
    await userEvent.type(screen.getByLabelText('First name'), 'Ana');
    await userEvent.type(screen.getByLabelText('Notes'), 'Hi');
    await userEvent.selectOptions(screen.getByLabelText('Country'), 'SI');
    await userEvent.click(screen.getByRole('button', { name: 'Add attribute' }));
    await userEvent.type(screen.getByLabelText('Key 1'), 'skill');
    await userEvent.type(screen.getByLabelText('Value 1'), 'java');
    await userEvent.type(screen.getByLabelText('Add tag'), 'gost{Enter}');
    await userEvent.click(screen.getByRole('button', { name: 'Save contact' }));
    expect(onSubmit).toHaveBeenCalledOnce();
    const [values, pairs, tags] = onSubmit.mock.calls[0] as [Record<string, string>, unknown, unknown];
    expect(values).toMatchObject({
      firstName: 'Ana',
      company: 'Acme',
      country: 'SI',
      notes: 'Hi',
      lastName: '',
    });
    expect(pairs).toEqual([{ key: 'skill', value: 'java' }]);
    expect(tags).toEqual(['gost']);
  });

  it('starts with the Attributes and Tags of the Contact', async () => {
    const onSubmit = vi.fn();
    renderForm({ onSubmit, initial: { attributes: [{ key: 'k', value: 'v' }], tags: ['t1'] } });
    expect(screen.getByLabelText('Key 1')).toHaveValue('k');
    await userEvent.click(screen.getByRole('button', { name: 'Save contact' }));
    expect(onSubmit).toHaveBeenCalledWith(expect.anything(), [{ key: 'k', value: 'v' }], ['t1']);
  });

  it('shows the submit label, or the busy label with a disabled button while saving', () => {
    renderForm();
    const button = screen.getByRole('button', { name: 'Save contact' });
    expect(button).toBeEnabled();
    expect(button).not.toHaveAttribute('aria-busy');
  });

  it('is busy while saving: busy label, disabled buttons, aria-busy', () => {
    renderForm({ busy: true, onCancel: () => undefined });
    const button = screen.getByRole('button', { name: 'Saving …' });
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
  });

  it('shows Cancel only when it is given, and calls it', async () => {
    const onCancel = vi.fn();
    renderForm({ onCancel });
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onCancel).toHaveBeenCalledOnce();
  });

  it('has no Cancel without onCancel', () => {
    renderForm();
    expect(screen.queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument();
  });

  it('shows no notice without an error', () => {
    renderForm();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('shows the field errors next to the fields and one notice that asks to correct them', () => {
    renderForm({
      error: fieldErrors(
        ['email', 'validation.email_invalid'],
        ['email', 'validation.too_long'],
        ['phone', 'validation.phone_invalid'],
      ),
    });
    const notice = screen.getAllByRole('alert')[0] as HTMLElement;
    expect(notice).toHaveTextContent('The data is not valid.');
    expect(notice).toHaveTextContent('Correct the marked fields. The values you entered are kept.');
    expect(screen.getByLabelText('E-mail')).toHaveAccessibleDescription('The e-mail address is not valid.');
    expect(screen.getByLabelText('Phone')).toHaveAccessibleDescription(
      'The phone number may contain digits, spaces and + - ( ) . /.',
    );
    expect(screen.queryByText('The value is too long.')).not.toBeInTheDocument();
  });

  it('shows the errors of Country and Notes as alerts and marks the fields invalid', () => {
    renderForm({
      error: fieldErrors(['country', 'validation.country_invalid'], ['notes', 'validation.too_long']),
    });
    expect(screen.getByText('The country is not on the list.')).toHaveAttribute('role', 'alert');
    expect(screen.getByText('The value is too long.')).toHaveAttribute('role', 'alert');
    expect(screen.getByLabelText('Country')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByLabelText('Notes')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByLabelText('First name')).not.toHaveAttribute('aria-invalid', 'true');
  });

  it('marks no field invalid without errors', () => {
    renderForm();
    expect(screen.getByLabelText('Country')).not.toHaveAttribute('aria-invalid');
    expect(screen.getByLabelText('Notes')).not.toHaveAttribute('aria-invalid');
  });

  it('shows Attribute and Tag errors at the Attributes and the Tags', () => {
    renderForm({
      initial: { attributes: [{ key: '', value: 'v' }] },
      error: fieldErrors(
        ['attributes[0].key', 'validation.attribute_key_invalid'],
        ['tags[0]', 'validation.tag_invalid'],
      ),
    });
    expect(screen.getByText('The attribute key must have 1 to 50 characters.')).toBeInTheDocument();
    expect(
      screen.getByText('A tag name may contain letters, digits, - and _ (up to 50 characters).'),
    ).toBeInTheDocument();
  });

  it('ignores field errors of a refusal that is not 422', () => {
    renderForm({
      error: new ApiError(400, 'validation.failed', 'x', [
        { field: 'email', code: 'validation.email_invalid' },
      ]),
    });
    expect(screen.queryByText('The e-mail address is not valid.')).not.toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('The data is not valid.');
  });

  it.each([
    [409, 'contact.in_trash', 'The contact is in the trash and cannot be changed.'],
    [500, 'server.error', 'Something went wrong. Please try again.'],
    [404, 'unknown.code', 'Something went wrong. Please try again.'],
  ])('shows one notice for the refusal %s %s', (status, code, text) => {
    renderForm({ error: new ApiError(status, code, 'x') });
    expect(screen.getByRole('alert')).toHaveTextContent(text);
  });

  it('shows no notice for a conflict of versions: the page shows it', () => {
    renderForm({ error: new ApiError(409, 'contact.changed_by_someone_else', 'x') });
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('asks for Tag suggestions only after a Tag name is typed, and for Attribute keys only with an Attribute', async () => {
    renderForm();
    const calls = () => vi.mocked(fetch).mock.calls.map(([url]) => String(url));
    expect(calls()).toEqual([]);
    await userEvent.type(screen.getByLabelText('Add tag'), 'g');
    await waitFor(() => expect(calls().some((u) => u.endsWith('/groups/g%201/facets'))).toBe(true));
    const before = calls().length;
    await userEvent.click(screen.getByRole('button', { name: 'Add attribute' }));
    await waitFor(() => expect(calls().length).toBeGreaterThan(before));
    expect(calls().every((u) => u.endsWith('/groups/g%201/facets'))).toBe(true);
  });
});
