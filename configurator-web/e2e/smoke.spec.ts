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

const componentPage = {
  items: [
    {
      id: 101,
      componentTypeId: 11,
      name: 'Ryzen 7 7800X3D',
      brand: 'AMD',
      archived: false,
      createdAt: '2026-08-09T12:00:00Z',
      attributes: [
        {
          attributeDefinitionId: 1011,
          name: 'cores',
          label: 'Количество ядер',
          dataType: 'NUMBER',
          value: '8',
        },
      ],
    },
  ],
  page: 0,
  size: 12,
  totalItems: 1,
};

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
  await page.route('**/api/domains/*/components*', async (route) => {
    await route.fulfill({ json: componentPage });
  });
  await page.route('**/api/components/*', async (route) => {
    await route.fulfill({ json: componentPage.items[0] });
  });
  await page.route('**/api/components', async (route) => {
    if (route.request().method() !== 'POST') {
      await route.fallback();
      return;
    }
    const body = route.request().postDataJSON() as Record<string, unknown>;
    await route.fulfill({
      status: 201,
      json: { ...componentPage.items[0], ...body, id: 202 },
    });
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

test('shows the component catalog and replaces the table with a compact mobile list', async ({
  page,
}) => {
  await page.goto('/components');

  await expect(page.getByRole('heading', { level: 1, name: 'Компоненты' })).toBeVisible();
  await expect(page.getByRole('heading', { level: 2, name: 'Ryzen 7 7800X3D' })).toBeVisible();
  await page.getByText('Таблица', { exact: true }).click();
  await expect(page.getByTestId('desktop-component-table')).toBeVisible();

  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByTestId('desktop-component-table')).toBeHidden();
  await expect(page.getByTestId('mobile-component-list')).toBeVisible();
  await expect(
    page.getByTestId('mobile-component-list').getByText('Ryzen 7 7800X3D', { exact: true }),
  ).toBeVisible();
});

test('opens component details and creates a component with dynamic attributes', async ({
  page,
}) => {
  await page.goto('/components');
  await page.getByRole('link', { name: 'Ryzen 7 7800X3D' }).first().click();
  await expect(page).toHaveURL(/\/components\/101$/);
  await expect(page.getByRole('heading', { level: 2, name: 'Характеристики' })).toBeVisible();
  await expect(page.getByText('Количество ядер')).toBeVisible();

  await page.getByRole('link', { name: 'К каталогу' }).click();
  await page.getByRole('link', { name: 'Новый компонент' }).click();
  await page.getByRole('combobox', { name: 'Тип компонента' }).click();
  await page.getByRole('option', { name: 'Процессор' }).click();
  await page.getByRole('textbox', { name: 'Название' }).fill('Ryzen 9 9950X3D');
  await page.getByRole('textbox', { name: 'Количество ядер' }).fill('16');
  await page.getByRole('button', { name: 'Создать' }).click();

  await expect(page).toHaveURL(/\/components\/202$/);
  await expect(page.getByText('Компонент создан')).toBeVisible();
});
