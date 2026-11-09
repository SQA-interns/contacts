import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setLanguage } from '@/i18n';
import { ExportButton } from './ExportButton';

const xlsx = (name: string) =>
  new Response(new Blob(['x']), {
    headers: {
      'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'Content-Disposition': `attachment; filename="${name}"`,
    },
  });

describe('ExportButton', () => {
  beforeEach(async () => {
    await setLanguage('sl');
    vi.stubGlobal('fetch', vi.fn());
    URL.createObjectURL = vi.fn(() => 'blob:x');
    URL.revokeObjectURL = vi.fn();
  });
  afterEach(() => vi.unstubAllGlobals());

  it('is disabled while nothing is selected (R-011-01)', () => {
    render(<ExportButton groupId="g1" count={0} getSelection={() => Promise.resolve({ contactIds: [] })} />);

    expect(screen.getByRole('button', { name: 'Izvozi v Excel' })).toBeDisabled();
  });

  it('posts the selection with the language and saves the file under the name of the answer (R-011-02, R-011-04)', async () => {
    vi.mocked(fetch).mockResolvedValue(xlsx('Acme_2026-11-08.xlsx'));
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    render(
      <ExportButton
        groupId="g1"
        count={2}
        getSelection={() => Promise.resolve({ contactIds: ['a', 'b'] })}
      />,
    );

    await userEvent.click(screen.getByRole('button', { name: 'Izvozi v Excel' }));

    await waitFor(() => expect(click).toHaveBeenCalled());
    const [url, init] = vi.mocked(fetch).mock.calls[0] as [string, RequestInit];
    expect(url).toContain('/groups/g1/exports');
    expect(JSON.parse(String(init.body))).toEqual({ selection: { contactIds: ['a', 'b'] }, lang: 'sl' });
    expect(URL.createObjectURL).toHaveBeenCalled();
  });

  it('shows the message to narrow the Selection when it is too large (R-011-05)', async () => {
    vi.mocked(fetch).mockResolvedValue(
      new Response(JSON.stringify({ code: 'export.too_many', message: 'x' }), { status: 422 }),
    );
    render(<ExportButton groupId="g1" count={2} getSelection={() => Promise.resolve({ filter: {} })} />);

    await userEvent.click(screen.getByRole('button', { name: 'Izvozi v Excel' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Izbira ima več kot 10.000 kontaktov. Zožite jo.',
    );
    expect(URL.createObjectURL).not.toHaveBeenCalled();
  });

  it('is busy while the file is made', async () => {
    let finish: (r: Response) => void = () => undefined;
    vi.mocked(fetch).mockReturnValue(new Promise((resolve) => (finish = resolve)));
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    render(
      <ExportButton groupId="g1" count={1} getSelection={() => Promise.resolve({ contactIds: ['a'] })} />,
    );

    await userEvent.click(screen.getByRole('button', { name: 'Izvozi v Excel' }));

    expect(await screen.findByRole('button', { name: /Izvažanje/ })).toHaveAttribute('aria-busy', 'true');
    finish(xlsx('Acme_2026-11-08.xlsx'));
    expect(await screen.findByRole('button', { name: 'Izvozi v Excel' })).toBeEnabled();
  });
});
