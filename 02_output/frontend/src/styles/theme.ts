export type ThemeName = 'wireframe' | 'final';

/** The theme of the stage 2 build (wireframe). Kept as the default of `applyTheme`; `main.tsx` applies `FINAL_THEME`. */
export const ACTIVE_THEME: ThemeName = 'wireframe';

/** The theme of the shipped application, set by `/design` (stage 3). */
export const FINAL_THEME: ThemeName = 'final';

export function applyTheme(
  theme: ThemeName = ACTIVE_THEME,
  root: HTMLElement = document.documentElement,
): void {
  root.setAttribute('data-theme', theme);
}
