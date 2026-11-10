import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { configureApi } from './client';
import {
  createContact,
  filterParams,
  getAttributeKeys,
  getContact,
  getFacets,
  getTagNames,
  listContacts,
  NO_FILTERS,
  updateContact,
} from './contacts';
import { dismissDuplicate, findDuplicates, mergeContacts } from './duplicates';
import { ApiError } from './errors';
import { exportContacts, fileNameOf } from './exports';
import { createGroup, listGroups, listGroupTrash, restoreGroup, trashGroup } from './groups';
import { getContactHistory, getGroupActivity } from './history';
import { cancelInvitation, createInvitation, listInvitations } from './invitations';
import { deleteMe, getMe } from './me';
import { changeMemberRole, listMembers, removeMember } from './members';
import { deleteTag, listTags, renameTag } from './tags';
import { deleteContactForever, listTrash, restoreContact, trashContact } from './trash';
import { compareVersion, getVersion, listVersions, restoreVersion } from './versions';

interface Seen {
  method: string;
  path: string;
  query: string;
  body: unknown;
  contentType: string | null;
  accept: string | null;
}

const seen: Seen[] = [];
const server = setupServer(
  http.all('http://localhost:8080/api/v1/*', async ({ request }) => {
    const url = new URL(request.url);
    const text = await request.text();
    seen.push({
      method: request.method,
      path: url.pathname.replace('/api/v1', ''),
      query: url.search,
      body: text ? (JSON.parse(text) as unknown) : undefined,
      contentType: request.headers.get('content-type'),
      accept: request.headers.get('accept'),
    });
    if (url.pathname.endsWith('/exports')) {
      return new HttpResponse('xlsx', {
        headers: { 'Content-Disposition': 'attachment; filename="kontakti.xlsx"' },
      });
    }
    if (
      request.method === 'DELETE' ||
      url.pathname.endsWith('/trash') ||
      url.pathname.endsWith('/dismissals')
    ) {
      return new HttpResponse(null, { status: 204 });
    }
    if (url.pathname.endsWith('/facets')) return HttpResponse.json({});
    return HttpResponse.json([]);
  }),
);
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  seen.length = 0;
  configureApi(
    () => Promise.resolve(null),
    () => undefined,
  );
});
afterAll(() => server.close());

const last = (): Seen => seen[seen.length - 1] as Seen;

describe('endpoints send the method, path and body of the contract (AR-03)', () => {
  it('Contacts', async () => {
    await createContact('g 1', { firstName: 'A', tags: ['x'] });
    expect(last()).toMatchObject({
      method: 'POST',
      path: '/groups/g%201/contacts',
      body: { firstName: 'A', tags: ['x'] },
      contentType: 'application/json',
      accept: 'application/json',
    });
    await getContact('g', 'c/1');
    expect(last()).toMatchObject({ method: 'GET', path: '/groups/g/contacts/c%2F1' });
    expect(last().contentType).toBeNull();
    await updateContact('g', 'c/1', { version: 4 });
    expect(last()).toMatchObject({ method: 'PUT', path: '/groups/g/contacts/c%2F1', body: { version: 4 } });
  });

  it('facets: missing lists become empty, present lists are returned', async () => {
    await expect(getAttributeKeys('g')).resolves.toEqual([]);
    expect(last()).toMatchObject({ method: 'GET', path: '/groups/g/facets' });
    await expect(getTagNames('g')).resolves.toEqual([]);
    await expect(getFacets('g')).resolves.toEqual({
      tags: [],
      companies: [],
      countries: [],
      attributeKeys: [],
    });
    server.use(
      http.get('http://localhost:8080/api/v1/groups/g/facets', () =>
        HttpResponse.json({ tags: ['t'], companies: ['c'], countries: ['SI'], attributeKeys: ['k'] }),
      ),
    );
    await expect(getAttributeKeys('g')).resolves.toEqual(['k']);
    await expect(getTagNames('g')).resolves.toEqual(['t']);
    await expect(getFacets('g')).resolves.toEqual({
      tags: ['t'],
      companies: ['c'],
      countries: ['SI'],
      attributeKeys: ['k'],
    });
  });

  it('list query: page, sort and direction always, q and filters only when set (R-008-05, EX-009-01)', async () => {
    await listContacts('g', { page: 2, sort: 'city', dir: 'desc' });
    expect(last().path).toBe('/groups/g/contacts');
    expect(last().query).toBe('?page=2&sort=city&dir=desc');
    await listContacts('g', { page: 0, sort: 'lastName', dir: 'asc', q: '   ' });
    expect(last().query).toBe('?page=0&sort=lastName&dir=asc');
    await listContacts('g', { page: 0, sort: 'lastName', dir: 'asc', q: '  Novak ' });
    expect(last().query).toBe('?page=0&sort=lastName&dir=asc&q=Novak');
    await listContacts('g', {
      page: 0,
      sort: 'lastName',
      dir: 'asc',
      filters: { ...NO_FILTERS, company: 'Acme' },
    });
    expect(last().query).toBe('?page=0&sort=lastName&dir=asc&company=Acme');
    await listContacts('g', { page: 0, sort: 'lastName', dir: 'asc', filters: NO_FILTERS });
    expect(last().query).toBe('?page=0&sort=lastName&dir=asc');
  });

  it('filter parameters: each filter adds exactly its own parameters', () => {
    const p = (f: Partial<typeof NO_FILTERS>) => {
      const params = new URLSearchParams();
      filterParams(params, { ...NO_FILTERS, ...f });
      return params.toString();
    };
    expect(p({})).toBe('');
    expect(p({ tags: ['a', 'b'], tagMode: 'any' })).toBe('tags=a&tags=b&tagMode=any');
    expect(p({ tags: ['a'] })).toBe('tags=a&tagMode=all');
    expect(p({ tagMode: 'any' })).toBe('');
    expect(p({ withoutTags: ['x', 'y'] })).toBe('withoutTags=x&withoutTags=y');
    expect(p({ country: 'SI' })).toBe('country=SI');
    expect(p({ company: 'Acme' })).toBe('company=Acme');
    expect(p({ attrKey: 'k', attrExists: true, attrValue: 'v' })).toBe('attrKey=k&attrExists=true');
    expect(p({ attrKey: 'k', attrValue: '  v ' })).toBe('attrKey=k&attrValue=v');
    expect(p({ attrKey: 'k', attrValue: '   ' })).toBe('');
    expect(p({ attrKey: 'k' })).toBe('');
    expect(p({ attrValue: 'v' })).toBe('');
    expect(p({ attrExists: true })).toBe('');
  });

  it('Groups, Members, Invitations, Tags and account', async () => {
    await listGroups();
    expect(last()).toMatchObject({ method: 'GET', path: '/groups' });
    await createGroup('Moja');
    expect(last()).toMatchObject({ method: 'POST', path: '/groups', body: { name: 'Moja' } });
    await trashGroup('g/1', 'Moja');
    expect(last()).toMatchObject({ method: 'POST', path: '/groups/g%2F1/trash', body: { name: 'Moja' } });
    await listGroupTrash();
    expect(last()).toMatchObject({ method: 'GET', path: '/group-trash' });
    await restoreGroup('g/1');
    expect(last()).toMatchObject({ method: 'POST', path: '/group-trash/g%2F1/restore' });
    expect(last().body).toBeUndefined();

    await listMembers('g');
    expect(last()).toMatchObject({ method: 'GET', path: '/groups/g/members' });
    await changeMemberRole('g', 'u/1', 'EDITOR');
    expect(last()).toMatchObject({
      method: 'PATCH',
      path: '/groups/g/members/u%2F1',
      body: { role: 'EDITOR' },
    });
    await removeMember('g', 'u/1');
    expect(last()).toMatchObject({ method: 'DELETE', path: '/groups/g/members/u%2F1' });

    await listInvitations('g');
    expect(last()).toMatchObject({ method: 'GET', path: '/groups/g/invitations' });
    await createInvitation('g', 'a@b.si', 'VIEWER');
    expect(last()).toMatchObject({
      method: 'POST',
      path: '/groups/g/invitations',
      body: { email: 'a@b.si', role: 'VIEWER' },
    });
    await cancelInvitation('g', 'i/1');
    expect(last()).toMatchObject({ method: 'POST', path: '/groups/g/invitations/i%2F1/cancel' });

    await listTags('g');
    expect(last()).toMatchObject({ method: 'GET', path: '/groups/g/tags' });
    await renameTag('g', 't1', 'novo');
    expect(last()).toMatchObject({ method: 'PATCH', path: '/groups/g/tags/t1', body: { name: 'novo' } });
    await deleteTag('g', 't1');
    expect(last()).toMatchObject({ method: 'DELETE', path: '/groups/g/tags/t1' });

    await getMe();
    expect(last()).toMatchObject({ method: 'GET', path: '/me' });
    await deleteMe();
    expect(last()).toMatchObject({ method: 'DELETE', path: '/me' });
  });

  it('Trash, duplicates, merges and versions', async () => {
    await trashContact('g', 'c/1');
    expect(last()).toMatchObject({ method: 'POST', path: '/groups/g/contacts/c%2F1/trash' });
    expect(last().body).toBeUndefined();
    await listTrash('g');
    expect(last()).toMatchObject({ method: 'GET', path: '/groups/g/trash/contacts' });
    await restoreContact('g', 'c/1');
    expect(last()).toMatchObject({ method: 'POST', path: '/groups/g/trash/contacts/c%2F1/restore' });
    await deleteContactForever('g', 'c/1');
    expect(last()).toMatchObject({ method: 'DELETE', path: '/groups/g/trash/contacts/c%2F1' });

    await findDuplicates('g');
    expect(last()).toMatchObject({ method: 'GET', path: '/groups/g/duplicates' });
    await dismissDuplicate('g', ['a', 'b']);
    expect(last()).toMatchObject({
      method: 'POST',
      path: '/groups/g/duplicates/dismissals',
      body: { contactIds: ['a', 'b'] },
    });
    const request = { contacts: [{ id: 'a', version: 1 }], attributes: [{ key: 'k', value: 'v' }] };
    await mergeContacts('g', request as never);
    expect(last()).toMatchObject({ method: 'POST', path: '/groups/g/merges', body: request });

    await listVersions('g', 'c');
    expect(last()).toMatchObject({ method: 'GET', path: '/groups/g/contacts/c/versions' });
    await getVersion('g', 'c', 3);
    expect(last()).toMatchObject({ method: 'GET', path: '/groups/g/contacts/c/versions/3' });
    await compareVersion('g', 'c', 3);
    expect(last()).toMatchObject({ method: 'GET', path: '/groups/g/contacts/c/versions/3/compare' });
    await restoreVersion('g', 'c', 3, 7);
    expect(last()).toMatchObject({
      method: 'POST',
      path: '/groups/g/contacts/c/versions/3/restore',
      body: { version: 7 },
    });
  });

  it('history and activity: empty filters are left out', async () => {
    await getContactHistory('g', 'c', 2);
    expect(last()).toMatchObject({ method: 'GET', path: '/groups/g/contacts/c/history', query: '?page=2' });
    await getGroupActivity('g', { userId: '', action: '', from: '', to: '' } as never, 1);
    expect(last()).toMatchObject({ method: 'GET', path: '/groups/g/activity', query: '?page=1' });
    await getGroupActivity(
      'g',
      { userId: 'u', action: 'TAG_ADDED', from: '2026-01-01', to: '2026-02-01' } as never,
      0,
    );
    expect(last().path).toBe('/groups/g/activity');
    expect(last().query).toBe('?page=0&userId=u&action=TAG_ADDED&from=2026-01-01&to=2026-02-01');
  });

  it('export: POST with the selection and language, file name from the header (R-011-02)', async () => {
    const file = await exportContacts('g', { contactIds: ['a'] }, 'sl');
    expect(last()).toMatchObject({
      method: 'POST',
      path: '/groups/g/exports',
      body: { selection: { contactIds: ['a'] }, lang: 'sl' },
    });
    expect(file.fileName).toBe('kontakti.xlsx');
    expect(await file.blob.text()).toBe('xlsx');
  });

  it('file name of Content-Disposition: extended form first, then plain, then the default', () => {
    expect(fileNameOf("attachment; filename*=UTF-8''k%C4%8Do.xlsx")).toBe('kčo.xlsx');
    expect(fileNameOf("attachment; filename*=utf-8''a.xlsx; size=3")).toBe('a.xlsx');
    expect(fileNameOf("attachment; filename*=UTF-8'' b%20c.xlsx ")).toBe('b c.xlsx');
    expect(fileNameOf('attachment; filename="plain.xlsx"')).toBe('plain.xlsx');
    expect(fileNameOf('attachment; filename="a b.xlsx"; x=1')).toBe('a b.xlsx');
    expect(fileNameOf('attachment')).toBe('export.xlsx');
    expect(fileNameOf(null)).toBe('export.xlsx');
    expect(fileNameOf('attachment; filename=""')).toBe('export.xlsx');
  });
});

describe('error answers', () => {
  it('keep the defaults for an empty answer and a missing list of field errors', async () => {
    server.use(http.get('http://localhost:8080/api/v1/me', () => HttpResponse.json({}, { status: 500 })));
    const error = (await getMe().catch((e: unknown) => e)) as ApiError;
    expect(error).toBeInstanceOf(ApiError);
    expect(error.name).toBe('ApiError');
    expect(error.status).toBe(500);
    expect(error.code).toBe('server.error');
    expect(error.fieldErrors).toEqual([]);
    expect(error.groups).toEqual([]);
    expect(error.current).toBeUndefined();
  });

  it('keep the defaults for a body that is not JSON', async () => {
    server.use(
      http.get(
        'http://localhost:8080/api/v1/me',
        () => new HttpResponse('boom', { status: 502, statusText: 'Bad' }),
      ),
    );
    const error = (await getMe().catch((e: unknown) => e)) as ApiError;
    expect(error.code).toBe('server.error');
    expect(error.message).toBe('Bad');
    expect(error.fieldErrors).toEqual([]);
    expect(error.groups).toEqual([]);
  });

  it('send an empty query value never and an array value item by item', async () => {
    const { apiRequest } = await import('./client');
    await apiRequest('/groups', { query: { a: '', b: 'x', c: ['1', '2'], d: undefined, e: 0, f: false } });
    expect(last().query).toBe('?b=x&c=1&c=2&e=0&f=false');
  });

  it('call the unauthorized hook on 401 only', async () => {
    const calls: number[] = [];
    configureApi(
      () => Promise.resolve(null),
      () => calls.push(1),
    );
    server.use(http.get('http://localhost:8080/api/v1/me', () => HttpResponse.json({}, { status: 403 })));
    await getMe().catch(() => undefined);
    expect(calls).toHaveLength(0);
    server.use(http.get('http://localhost:8080/api/v1/me', () => HttpResponse.json({}, { status: 401 })));
    await getMe().catch(() => undefined);
    expect(calls).toHaveLength(1);
  });
});
