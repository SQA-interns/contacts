import AxeBuilder from '@axe-core/playwright';
import { expect, type Page } from '@playwright/test';

/**
 * One build-ui check of a screen state (UI-19, UI-22): no horizontal scroll, no Critical or Serious axe violation,
 * and a full-page screenshot in `02_output/logs/screens/<screen>/<state>-<width>.png`.
 */
export async function check(page: Page, screen: string, state: string, width: string) {
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
  );
  expect(overflow, `${screen} ${state} overflows`).toBeLessThanOrEqual(0);
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'])
    .analyze();
  const blocking = results.violations.filter((v) => v.impact === 'critical' || v.impact === 'serious');
  expect(blocking, JSON.stringify(blocking.map((v) => [v.id, v.nodes.map((n) => n.target)]))).toEqual([]);
  await page.screenshot({ path: `../logs/screens/${screen}/${state}-${width}.png`, fullPage: true });
}
