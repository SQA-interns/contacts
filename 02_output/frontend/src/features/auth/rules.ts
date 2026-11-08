/** The client checks of the account screens; Firebase owns passwords, so R-001-06 lives only here (D-31g). */
export const PASSWORD_MIN_LENGTH = 8; // R-001-01, R-001-06
export const DISPLAY_NAME_MAX_LENGTH = 100; // R-001-01

/** The simple e-mail shape `local@domain.tld` (BR-06); Firebase checks the address again. */
export function isEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

export function isDisplayName(value: string): boolean {
  const name = value.trim();
  return name.length >= 1 && name.length <= DISPLAY_NAME_MAX_LENGTH;
}
