import type { Language } from './index';

/** Project-wide facts: UTC stored, shown in Europe/Ljubljana (AR-10, UI-18). */
export const TIME_ZONE = 'Europe/Ljubljana';

function parts(date: Date | string, options: Intl.DateTimeFormatOptions): Record<string, string> {
  const formatter = new Intl.DateTimeFormat('en-GB', { timeZone: TIME_ZONE, hourCycle: 'h23', ...options });
  const out: Record<string, string> = {};
  for (const part of formatter.formatToParts(typeof date === 'string' ? new Date(date) : date)) {
    out[part.type] = part.value;
  }
  return out;
}

/** Slovene `d. M. yyyy`, English `yyyy-MM-dd`. */
export function formatDate(date: Date | string, language: Language): string {
  const p = parts(date, { year: 'numeric', month: 'numeric', day: 'numeric' });
  if (language === 'sl') return `${Number(p.day)}. ${Number(p.month)}. ${p.year}`;
  return `${p.year}-${String(p.month).padStart(2, '0')}-${String(p.day).padStart(2, '0')}`;
}

/** `HH:mm`, 24 hours, in both languages. */
export function formatTime(date: Date | string): string {
  const p = parts(date, { hour: '2-digit', minute: '2-digit' });
  return `${p.hour}:${p.minute}`;
}

export function formatDateTime(date: Date | string, language: Language): string {
  return `${formatDate(date, language)} ${formatTime(date)}`;
}

/** Slovene decimal comma, English decimal point. */
export function formatNumber(value: number, language: Language): string {
  return new Intl.NumberFormat(language === 'sl' ? 'sl-SI' : 'en-GB').format(value);
}

/** Country name in the active language from an ISO 3166-1 alpha-2 code (R-019-05). */
export function countryName(code: string, language: Language): string {
  return new Intl.DisplayNames([language], { type: 'region' }).of(code) ?? code;
}
