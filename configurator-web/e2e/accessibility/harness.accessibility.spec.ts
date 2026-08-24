import { expect, test } from '../fixtures/mock-api';
import { scanForAccessibility, wcagTags } from './axe-test';

test('uses WCAG A and AA tags including WCAG 2.2', () => {
  expect(wcagTags).toEqual(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']);
});

test('detects an intentionally unnamed button', async ({ page }) => {
  await page.setContent('<main><button type="button"></button></main>');

  const results = await scanForAccessibility(page);

  expect(results.violations.map((violation) => violation.id)).toContain('button-name');
});
