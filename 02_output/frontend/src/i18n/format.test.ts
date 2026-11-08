import { describe, expect, it } from 'vitest';
import { countryName, formatDate, formatDateTime, formatNumber, formatTime } from './format';

describe('formats of the project-wide facts (UI-18, AR-10)', () => {
  it('shows Slovene dates as d. M. yyyy and English dates as yyyy-MM-dd', () => {
    const date = '2026-11-08T12:00:00Z';
    expect(formatDate(date, 'sl')).toBe('8. 11. 2026');
    expect(formatDate(date, 'en')).toBe('2026-11-08');
  });

  it('converts UTC to Europe/Ljubljana: CET in winter, CEST in summer', () => {
    expect(formatTime('2026-01-15T10:30:00Z')).toBe('11:30');
    expect(formatTime('2026-07-15T10:30:00Z')).toBe('12:30');
  });

  it('moves the calendar day across midnight', () => {
    expect(formatDate('2026-11-07T23:30:00Z', 'en')).toBe('2026-11-08');
    expect(formatDateTime('2026-11-07T23:30:00Z', 'sl')).toBe('8. 11. 2026 00:30');
  });

  it('uses 24-hour time in both languages', () => {
    expect(formatTime('2026-11-08T21:05:00Z')).toBe('22:05');
  });

  it('uses a decimal comma in Slovene and a decimal point in English', () => {
    expect(formatNumber(1234.5, 'sl')).toMatch(/^1\.?\s?234,5$/);
    expect(formatNumber(1234.5, 'en')).toBe('1,234.5');
  });

  it('names countries in the active language (R-019-05)', () => {
    expect(countryName('SI', 'en')).toBe('Slovenia');
    expect(countryName('SI', 'sl')).toBe('Slovenija');
  });
});
