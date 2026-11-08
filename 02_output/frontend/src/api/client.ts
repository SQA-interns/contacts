import { readConfig } from '@/config';
import { ApiError, type FieldError } from './errors';

type TokenProvider = () => Promise<string | null>;
let tokenProvider: TokenProvider = () => Promise.resolve(null);
let onUnauthorized: () => void = () => undefined;

/** Called once by the auth module so that requests carry the Firebase ID token (R-018-01). */
export function configureApi(provider: TokenProvider, unauthorized: () => void): void {
  tokenProvider = provider;
  onUnauthorized = unauthorized;
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  query?: Record<string, string | number | boolean | string[] | undefined>;
  signal?: AbortSignal;
  /** Return the raw response (for the .xlsx download). */
  raw?: boolean;
}

function buildUrl(path: string, query: RequestOptions['query']): string {
  const url = new URL(`${readConfig().apiBaseUrl}/api/v1${path}`, window.location.origin);
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value === undefined || value === '') continue;
    for (const item of Array.isArray(value) ? value : [value]) url.searchParams.append(key, String(item));
  }
  return url.toString();
}

/** The one place that calls the backend (AR-03). */
export async function apiRequest<T = void>(path: string, options: RequestOptions = {}): Promise<T> {
  const token = await tokenProvider();
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';
  const response = await fetch(buildUrl(path, options.query), {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    signal: options.signal,
  });
  if (response.ok) {
    if (options.raw) return response as unknown as T;
    if (response.status === 204) return undefined as T;
    return (await response.json()) as T;
  }
  let code = 'server.error';
  let message = response.statusText;
  let fieldErrors: FieldError[] = [];
  let current: unknown;
  try {
    const body = (await response.json()) as {
      code?: string;
      message?: string;
      fieldErrors?: FieldError[];
      current?: unknown;
    };
    code = body.code ?? code;
    message = body.message ?? message;
    fieldErrors = body.fieldErrors ?? [];
    current = body.current;
  } catch {
    /* not JSON: keep the defaults, show nothing technical */
  }
  if (response.status === 401) onUnauthorized();
  throw new ApiError(response.status, code, message, fieldErrors, current);
}
