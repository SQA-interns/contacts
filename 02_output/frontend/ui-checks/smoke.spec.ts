import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';
import { VIEWPORTS } from './viewports';

/** Smoke test: the shell (on the sign-in page) at the three widths, no overflow, accessible, both languages. */
for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} (${viewport.width} px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('sign-in page has no horizontal scroll, no serious accessibility violation, and a screenshot', async ({
      page,
    }, info) => {
      await page.goto('/sign-in');
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Prijava');
      const overflow = await page.evaluate(
        () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
      );
      expect(overflow).toBeLessThanOrEqual(0);

      const results = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'])
        .analyze();
      const blocking = results.violations.filter((v) => v.impact === 'critical' || v.impact === 'serious');
      expect(blocking, JSON.stringify(blocking.map((v) => v.id))).toEqual([]);

      await page.screenshot({
        path: `test-results/screens/${info.project.name}-sign-in-${viewport.name}.png`,
        fullPage: true,
      });
    });

    test('language switch changes the UI at once', async ({ page }) => {
      await page.goto('/sign-in');
      await page.getByRole('button', { name: 'EN', exact: true }).click();
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Sign in');
      await expect(page.locator('html')).toHaveAttribute('lang', 'en');
      await page.reload();
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Sign in');
    });
  });
}

test('unknown route shows the not found page', async ({ page }) => {
  await page.goto('/nothing/here');
  await expect(page.getByRole('heading', { level: 2 })).toHaveText('Strani ni mogoče najti');
});
