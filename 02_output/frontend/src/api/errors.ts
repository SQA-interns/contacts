export interface FieldError {
  field: string;
  code: string;
}

/** An answer of the backend in the error format (ADR-10). `code` is translated by the UI. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: FieldError[];
  readonly current: unknown;

  constructor(
    status: number,
    code: string,
    message: string,
    fieldErrors: FieldError[] = [],
    current?: unknown,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.current = current;
  }
}

/** Catalog key of an error code: `contact.not_found` -> `api.contact.not_found` (nested in the catalogs). */
export function errorKey(code: string): string {
  return `api.${code}`;
}
