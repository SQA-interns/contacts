import { describe, expect, it } from 'vitest';
import { DISPLAY_NAME_MAX_LENGTH, isDisplayName, isEmail, PASSWORD_MIN_LENGTH } from './rules';

describe('client checks of the account screens (R-001-01, R-001-06, BR-06)', () => {
  it('knows the limits', () => {
    expect(PASSWORD_MIN_LENGTH).toBe(8);
    expect(DISPLAY_NAME_MAX_LENGTH).toBe(100);
  });

  it('accepts local@domain.tld and nothing around it', () => {
    expect(isEmail('ana@example.si')).toBe(true);
    expect(isEmail('a.b+c@mail.example.si')).toBe(true);
    expect(isEmail('ana@example')).toBe(false);
    expect(isEmail('ana example@example.si')).toBe(false);
    expect(isEmail('ana@@example.si')).toBe(false);
    expect(isEmail('@example.si')).toBe(false);
    expect(isEmail('ana@.si')).toBe(false);
    expect(isEmail('x ana@example.si')).toBe(false);
    expect(isEmail('ana@example.si x')).toBe(false);
    expect(isEmail('ana@example.si\nfoo')).toBe(false);
    expect(isEmail('')).toBe(false);
  });

  it('accepts a trimmed display name of 1 to 100 characters', () => {
    expect(isDisplayName('A')).toBe(true);
    expect(isDisplayName(' Ana ')).toBe(true);
    expect(isDisplayName('x'.repeat(100))).toBe(true);
    expect(isDisplayName('x'.repeat(101))).toBe(false);
    expect(isDisplayName(`  ${'x'.repeat(100)}  `)).toBe(true);
    expect(isDisplayName('   ')).toBe(false);
    expect(isDisplayName('')).toBe(false);
  });
});
