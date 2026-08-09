import { expect, test } from '@playwright/test';

test('opens the configurator frontend', async ({ page }) => {
  await page.goto('/');

  await expect(
    page.getByRole('heading', { level: 1, name: 'Конфигуратор компонентов' }),
  ).toBeVisible();
});
