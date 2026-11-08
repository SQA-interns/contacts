export type ThemeName = 'wireframe' | 'final';

/** The theme the application starts with. Stage 2 (wireframe) until `/design` switches it to `final`. */
export const ACTIVE_THEME: ThemeName = 'wireframe';

export function applyTheme(
  theme: ThemeName = ACTIVE_THEME,
  root: HTMLElement = document.documentElement,
): void {
  root.setAttribute('data-theme', theme);
}
