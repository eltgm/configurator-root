import { expect, test } from './mock-api';

test('starts each test with fresh mutable API state', async ({ page }) => {
  await page.goto('/configurator');

  const totals = await page.evaluate(async () => {
    const initial = (await (await fetch('/api/domains?page=0&size=100')).json()) as {
      totalItems: number;
    };
    await fetch('/api/domains', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'Isolated domain' }),
    });
    const updated = (await (await fetch('/api/domains?page=0&size=100')).json()) as {
      totalItems: number;
    };
    return [initial.totalItems, updated.totalItems];
  });

  expect(totals).toEqual([2, 3]);
});

test('fails an unhandled API request with an explicit deterministic contract', async ({ page }) => {
  await page.goto('/configurator');

  const response = await page.evaluate(async () => {
    const result = await fetch('/api/not-implemented');
    return {
      status: result.status,
      body: (await result.json()) as { code: string; message: string },
    };
  });

  expect(response.status).toBe(501);
  expect(response.body.code).toBe('E2E_ROUTE_NOT_IMPLEMENTED');
  expect(response.body.message).toContain('GET /api/not-implemented');
});

test('blocks requests outside the local frontend boundary', async ({ page }) => {
  await page.goto('/configurator');

  const errorName = await page.evaluate(async () => {
    try {
      await fetch('https://example.com/unexpected-e2e-request');
      return 'NO_ERROR';
    } catch (error) {
      return error instanceof Error ? error.name : 'UNKNOWN_ERROR';
    }
  });

  expect(errorName).toBe('TypeError');
});
