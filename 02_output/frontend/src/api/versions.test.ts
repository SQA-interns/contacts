import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { compareVersion, getVersion, listVersions, restoreVersion } from './versions';

const answer = (body: unknown) => new Response(JSON.stringify(body), { status: 200 });
const calledUrl = () => String(vi.mocked(fetch).mock.calls[0]?.[0]);

describe('api/versions (R-022-02..04, R-023-01)', () => {
  beforeEach(() => vi.stubGlobal('fetch', vi.fn()));
  afterEach(() => vi.unstubAllGlobals());

  it('R-022-02 asks for the Versions of the Contact', async () => {
    vi.mocked(fetch).mockResolvedValue(answer([{ number: 2, createdAt: 't', userName: 'Ana' }]));

    const list = await listVersions('g 1', 'c1');

    expect(calledUrl()).toMatch(/\/groups\/g%201\/contacts\/c1\/versions$/);
    expect(list[0]?.number).toBe(2);
  });

  it('R-022-03 asks for one Version', async () => {
    vi.mocked(fetch).mockResolvedValue(answer({ number: 1, content: { id: 'c1' } }));

    await getVersion('g1', 'c1', 1);

    expect(calledUrl()).toMatch(/\/contacts\/c1\/versions\/1$/);
  });

  it('R-022-04 asks for the comparison of a Version with the current one', async () => {
    vi.mocked(fetch).mockResolvedValue(answer({ number: 1, differences: [] }));

    const comparison = await compareVersion('g1', 'c1', 1);

    expect(calledUrl()).toMatch(/\/contacts\/c1\/versions\/1\/compare$/);
    expect(comparison.differences).toEqual([]);
  });

  it('R-023-01 and R-023-04 posts the version the user saw and returns the new Contact', async () => {
    vi.mocked(fetch).mockResolvedValue(answer({ id: 'c1', version: 4, company: 'Acme' }));

    const contact = await restoreVersion('g1', 'c1', 1, 3);

    expect(calledUrl()).toMatch(/\/contacts\/c1\/versions\/1\/restore$/);
    const init = vi.mocked(fetch).mock.calls[0]?.[1];
    expect(init?.method).toBe('POST');
    expect(JSON.parse(String(init?.body))).toEqual({ version: 3 });
    expect(contact.version).toBe(4);
  });
});
