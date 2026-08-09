import { expect, test } from '@playwright/test';

const domainPage = {
  items: [
    {
      id: 101,
      name: 'Сборка ПК',
      description: 'Тестовая предметная область',
      createdAt: '2026-08-09T12:00:00Z',
    },
  ],
  page: 0,
  size: 100,
  totalItems: 1,
};

const componentTypes = [
  {
    id: 11,
    domainId: 101,
    name: 'Процессор',
    code: 'CPU',
    description: 'Центральный процессор',
    orderIndex: 1,
  },
];

const attributes = [
  {
    id: 1011,
    componentTypeId: 11,
    name: 'cores',
    label: 'Количество ядер',
    dataType: 'NUMBER',
    isRequired: true,
    orderIndex: 1,
  },
];

test.beforeEach(async ({ page }) => {
  await page.route('**/api/domains*', async (route) => {
    await route.fulfill({ json: domainPage });
  });
  await page.route('**/api/domains/*/component-types', async (route) => {
    await route.fulfill({ json: componentTypes });
  });
  await page.route('**/api/component-types/*/attributes', async (route) => {
    await route.fulfill({ json: attributes });
  });
});

test('opens the configurator frontend with the selected domain', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveURL(/\/configurator$/);
  await expect(
    page.getByRole('heading', { level: 1, name: 'Конфигуратор', exact: true }),
  ).toBeVisible();
  await expect(page.getByRole('button', { name: 'Предметная область: Сборка ПК' })).toBeVisible();
});

test('keeps domain management usable on a phone viewport', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/settings/domain');

  await expect(page.getByRole('heading', { level: 1, name: 'Предметные области' })).toBeVisible();
  await expect(page.getByRole('heading', { level: 2, name: 'Сборка ПК' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Предметная область: Сборка ПК' })).toBeVisible();
});

test('shows types and attributes for the selected domain', async ({ page }) => {
  await page.goto('/settings/types');

  await expect(page.getByRole('heading', { level: 1, name: 'Типы и атрибуты' })).toBeVisible();
  await expect(page.getByRole('heading', { level: 2, name: 'Процессор' })).toBeVisible();
  await expect(page.getByText('Количество ядер')).toBeVisible();
  await expect(page.getByText('Обязательный')).toBeVisible();
});
