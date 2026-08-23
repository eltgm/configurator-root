import AxeBuilder from '@axe-core/playwright';
import { expect, type Page, type TestInfo } from '@playwright/test';

export const wcagTags = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];

type ScanResults = Awaited<ReturnType<AxeBuilder['analyze']>>;

export function createAxeBuilder(page: Page) {
  return new AxeBuilder({ page }).withTags(wcagTags);
}

export function formatAxeViolations(violations: ScanResults['violations']) {
  return violations
    .map((violation) => {
      const targets = violation.nodes
        .flatMap((node) => node.target.map((target) => String(target)))
        .join(', ');
      return `${violation.id} [${violation.impact ?? 'unknown'}]: ${violation.help}\n  ${targets}\n  ${violation.helpUrl}`;
    })
    .join('\n\n');
}

export async function scanForAccessibility(page: Page) {
  await page.evaluate('document.fonts.ready');
  await page.waitForFunction(
    'document.getAnimations().every(animation => animation.playState !== "running")',
  );
  return createAxeBuilder(page).analyze();
}

export async function expectNoAxeViolations(page: Page, testInfo: TestInfo) {
  const results = await scanForAccessibility(page);
  await testInfo.attach('axe-results.json', {
    body: Buffer.from(
      JSON.stringify(
        { violations: results.violations, incomplete: results.incomplete },
        undefined,
        2,
      ),
    ),
    contentType: 'application/json',
  });
  expect(
    results.violations,
    results.violations.length ? formatAxeViolations(results.violations) : undefined,
  ).toEqual([]);
}
