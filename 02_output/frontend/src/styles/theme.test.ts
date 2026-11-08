import { describe, expect, it } from 'vitest';
import { ACTIVE_THEME, applyTheme } from './theme';

describe('theme switch (UI-03)', () => {
  it('starts with the wireframe theme', () => {
    expect(ACTIVE_THEME).toBe('wireframe');
  });

  it('switches by one attribute on the root element', () => {
    const root = document.createElement('html');
    applyTheme('wireframe', root);
    expect(root.getAttribute('data-theme')).toBe('wireframe');
    applyTheme('final', root);
    expect(root.getAttribute('data-theme')).toBe('final');
  });
});
