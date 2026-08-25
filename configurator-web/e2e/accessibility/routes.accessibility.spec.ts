import { expect, test } from '../fixtures/mock-api';
import { expectNoAxeViolations } from './axe-test';

const routes = [
  ['/configurator', 'Конфигуратор'],
  ['/components', 'Компоненты'],
  ['/configurations', 'Конфигурации'],
  ['/settings/types', 'Типы и атрибуты'],
  ['/settings/attributes', 'Атрибуты'],
  ['/settings/compatibility/manual', 'Ручная совместимость'],
  ['/settings/compatibility/rules', 'Автоматические правила'],
  ['/settings/compatibility/graph', 'Граф совместимости'],
  ['/settings/domain', 'Предметные области'],
] as const;

for (const [route, heading] of routes) {
  test(`${route} has no automatically detectable WCAG A or AA violations`, async ({
    page,
  }, testInfo) => {
    await page.goto(route);
    await expect(page.getByRole('heading', { level: 1, name: heading })).toBeVisible();

    await expectNoAxeViolations(page, testInfo);
  });
}

test('dark catalog has no automatically detectable WCAG A or AA violations', async ({
  page,
}, testInfo) => {
  await page.addInitScript("window.localStorage.setItem('configurator.color-scheme', 'dark')");
  await page.goto('/components');
  await expect(page.getByRole('heading', { level: 1, name: 'Компоненты' })).toBeVisible();
  await expect(page.locator('html')).toHaveAttribute('data-mantine-color-scheme', 'dark');

  await expectNoAxeViolations(page, testInfo);
});
