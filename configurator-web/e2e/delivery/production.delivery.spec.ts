import { expect, test } from '@playwright/test';

test('serves the production SPA and reaches the backend through the gateway', async ({ page }) => {
  const pageErrors: Error[] = [];
  page.on('pageerror', (error) => pageErrors.push(error));

  const domainsResponsePromise = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return response.request().method() === 'GET' && url.pathname === '/api/domains';
  });

  const rootResponse = await page.goto('/');
  const domainsResponse = await domainsResponsePromise;

  expect(rootResponse?.status()).toBe(200);
  expect(rootResponse?.headers()['content-type']).toContain('text/html');
  expect(domainsResponse.status()).toBe(200);
  expect(new URL(domainsResponse.url()).pathname).toBe('/api/domains');
  await expect(page).toHaveTitle(/Конфигуратор/);
  await expect(page.locator('main')).toBeVisible();

  const deepLinkResponse = await page.goto('/settings/compatibility/graph');

  expect(deepLinkResponse?.status()).toBe(200);
  expect(deepLinkResponse?.headers()['content-type']).toContain('text/html');
  await expect(page).toHaveURL(/\/settings\/compatibility\/graph$/);
  await expect(page.locator('main')).toBeVisible();
  expect(pageErrors).toEqual([]);
});
