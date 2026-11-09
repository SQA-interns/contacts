import type { TFunction } from 'i18next';
import i18n from '@/i18n';

export interface FieldError {
  field: string;
  code: string;
}

/** Contract schema `SoleOwnerGroup`: a Group the User alone owns (R-021-02); `daysLeft` only in the Group Trash. */
export interface SoleOwnerGroup {
  id: string;
  name: string;
  inTrash: boolean;
  daysLeft: number | null;
}

/** An answer of the backend in the error format (ADR-10). `code` is translated by the UI. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: FieldError[];
  readonly current: unknown;
  /** The Groups of a 409 `account.sole_owner`. */
  readonly groups: SoleOwnerGroup[];

  constructor(
    status: number,
    code: string,
    message: string,
    fieldErrors: FieldError[] = [],
    current?: unknown,
    groups: SoleOwnerGroup[] = [],
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.current = current;
    this.groups = groups;
  }
}

/** Catalog key of an error code: `contact.not_found` -> `api.contact.not_found` (nested in the catalogs). */
export function errorKey(code: string): string {
  return `api.${code}`;
}

/** Message of a failed request in the active language; the server text is never shown (R-019-07). */
export function errorMessage(t: TFunction, error: unknown): string {
  if (error instanceof ApiError) {
    const key = errorKey(error.code);
    if (i18n.exists(key)) return t(key);
  }
  return t('error.generic');
}
