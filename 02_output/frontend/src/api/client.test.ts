import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { apiRequest, configureApi } from './client';
import { ApiError } from './errors';

const server = setupServer();
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  server.resetHandlers();
  configureApi(
    () => Promise.resolve(null),
    () => undefined,
  );
});
afterAll(() => server.close());

const BASE = 'http://localhost:8080/api/v1';

describe('API client (AR-03, R-018-01)', () => {
  it('sends the Firebase ID token as a bearer token', async () => {
    let seen: string | null = null;
    server.use(
      http.get(`${BASE}/me`, ({ request }) => {
        seen = request.headers.get('authorization');
        return HttpResponse.json({ id: '1' });
      }),
    );
    configureApi(
      () => Promise.resolve('token-123'),
      () => undefined,
    );
    await expect(apiRequest<{ id: string }>('/me')).resolves.toEqual({ id: '1' });
    expect(seen).toBe('Bearer token-123');
  });

  it('sends no Authorization header without a token', async () => {
    let seen: string | null = 'unset';
    server.use(
      http.get(`${BASE}/groups`, ({ request }) => {
        seen = request.headers.get('authorization');
        return HttpResponse.json([]);
      }),
    );
    await apiRequest('/groups');
    expect(seen).toBeNull();
  });

  it('maps an error answer to ApiError with code, field errors and the current object', async () => {
    server.use(
      http.put(`${BASE}/groups/g/contacts/c`, () =>
        HttpResponse.json(
          { code: 'contact.changed_by_someone_else', message: 'x', current: { id: 'c', version: 3 } },
          { status: 409 },
        ),
      ),
    );
    const error = await apiRequest('/groups/g/contacts/c', { method: 'PUT', body: { version: 2 } }).catch(
      (e: unknown) => e,
    );
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 409,
      code: 'contact.changed_by_someone_else',
      current: { id: 'c', version: 3 },
    });
  });

  it('keeps field errors of a validation answer', async () => {
    server.use(
      http.post(`${BASE}/groups`, () =>
        HttpResponse.json(
          {
            code: 'validation.failed',
            message: 'x',
            fieldErrors: [{ field: 'name', code: 'validation.name_invalid' }],
          },
          { status: 422 },
        ),
      ),
    );
    const error = (await apiRequest('/groups', { method: 'POST', body: { name: '' } }).catch(
      (e: unknown) => e,
    )) as ApiError;
    expect(error.fieldErrors).toEqual([{ field: 'name', code: 'validation.name_invalid' }]);
  });

  it('tells the auth module about a 401 and still throws', async () => {
    const unauthorized = vi.fn();
    configureApi(() => Promise.resolve('expired'), unauthorized);
    server.use(
      http.get(`${BASE}/me`, () =>
        HttpResponse.json({ code: 'auth.token_invalid', message: 'x' }, { status: 401 }),
      ),
    );
    await expect(apiRequest('/me')).rejects.toMatchObject({ status: 401, code: 'auth.token_invalid' });
    expect(unauthorized).toHaveBeenCalledOnce();
  });

  it('shows nothing technical for an answer that is not JSON', async () => {
    server.use(http.get(`${BASE}/me`, () => new HttpResponse('<html>boom</html>', { status: 502 })));
    await expect(apiRequest('/me')).rejects.toMatchObject({ status: 502, code: 'server.error' });
  });

  it('returns undefined for 204 and builds the query without empty values', async () => {
    let query = '';
    server.use(
      http.delete(`${BASE}/groups/g/members/u`, ({ request }) => {
        query = new URL(request.url).search;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    await expect(
      apiRequest('/groups/g/members/u', {
        method: 'DELETE',
        query: { page: 2, q: '', tags: ['a', 'b'], flag: false },
      }),
    ).resolves.toBeUndefined();
    expect(query).toBe('?page=2&tags=a&tags=b&flag=false');
  });
});
