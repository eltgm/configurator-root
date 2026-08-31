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

test('mock API rejects duplicate catalog names consistently including rename conflicts', async ({
  page,
}) => {
  await page.goto('/settings/attributes');
  const result = await page.evaluate(async () => {
    const write = async (path: string, name: string, method = 'POST') => {
      const response = await fetch('/api' + path, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, label: 'Same label', dataType: 'STRING' }),
      });
      return {
        status: response.status,
        body: (await response.json()) as { id: number; details?: Array<{ field: string }> },
      };
    };
    const created = await write('/domains/101/attributes', 'shared');
    const catalogConflict = await write('/domains/101/attributes', 'shared');
    const typeConflict = await write('/component-types/12/attributes', 'shared');
    const second = await write('/component-types/12/attributes', 'second');
    const renameConflict = await write('/attributes/' + second.body.id, 'shared', 'PUT');
    const ownName = await write('/attributes/' + created.body.id, 'shared', 'PUT');
    const otherDomain = await write('/domains/202/attributes', 'shared');
    const caseVariant = await write('/domains/101/attributes', 'Shared');
    return {
      statuses: [
        created,
        catalogConflict,
        typeConflict,
        second,
        renameConflict,
        ownName,
        otherDomain,
        caseVariant,
      ].map((r) => r.status),
      fields: [catalogConflict, typeConflict, renameConflict].map(
        (r) => r.body.details?.[0]?.field,
      ),
    };
  });
  expect(result.statuses).toEqual([201, 409, 409, 201, 409, 200, 201, 201]);
  expect(result.fields).toEqual(['name', 'name', 'name']);
});
