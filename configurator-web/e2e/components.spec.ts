import { expect, test } from './fixtures/mock-api';

test('recovers after a retryable catalog request failure', async ({ page }) => {
  let failedAttempts = 0;
  const catalogRoute = '**/api/domains/*/components*';
  const failCatalogTwice: Parameters<typeof page.route>[1] = async (route) => {
    if (failedAttempts < 2) {
      failedAttempts += 1;
      await route.fulfill({
        status: 503,
        json: {
          timestamp: '2026-08-23T12:00:00Z',
          status: 503,
          error: 'Service Unavailable',
          code: 'INTERNAL_ERROR',
          message: 'Catalog is temporarily unavailable',
          path: '/domains/101/components',
          details: [],
        },
      });
      return;
    }
    await route.fallback();
  };
  await page.route(catalogRoute, failCatalogTwice);

  await page.goto('/components');
  await expect(page.getByRole('alert')).toContainText('Catalog is temporarily unavailable');
  await page.unroute(catalogRoute, failCatalogTwice);
  await page.getByRole('button', { name: 'Повторить' }).click();
  await expect(page.getByRole('heading', { level: 2, name: 'Ryzen 7 7800X3D' })).toBeVisible();
});

test('guards unsaved component form changes during navigation', async ({ page }) => {
  await page.goto('/components/new');
  await page.getByRole('textbox', { name: 'Название' }).fill('Несохранённый компонент');
  await page.getByRole('link', { name: 'К каталогу' }).click();

  const dialog = page.getByRole('dialog', { name: 'Выйти без сохранения?' });
  await expect(dialog).toBeVisible();
  await expect(page).toHaveURL(/\/components\/new$/);
  await dialog.getByRole('button', { name: 'Выйти' }).click();
  await expect(page).toHaveURL(/\/components$/);
});

test('guards unsaved component changes during a domain switch', async ({ page }) => {
  await page.goto('/components/new');
  await page.getByRole('textbox', { name: 'Название' }).fill('Несохранённый компонент');
  await page.getByRole('button', { name: 'Предметная область: Сборка ПК' }).click();
  await page.getByRole('menuitem', { name: 'Рабочая станция' }).click();

  const dialog = page.getByRole('dialog', { name: 'Сменить область без сохранения?' });
  await expect(dialog).toBeVisible();
  await dialog.getByRole('button', { name: 'Остаться' }).click();
  await expect(page.getByRole('textbox', { name: 'Название' })).toHaveValue(
    'Несохранённый компонент',
  );

  await page.getByRole('button', { name: 'Предметная область: Сборка ПК' }).click();
  await page.getByRole('menuitem', { name: 'Рабочая станция' }).click();
  await dialog.getByRole('button', { name: 'Сменить область' }).click();
  await expect(
    page.getByRole('button', { name: 'Предметная область: Рабочая станция' }),
  ).toBeVisible();
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

test('edits, archives and restores a component', async ({ page }) => {
  await page.goto('/components/101');
  await page.getByRole('link', { name: 'Редактировать' }).click();
  await page.getByRole('textbox', { name: 'Название' }).fill('Ryzen 7 7800X3D Updated');
  await page.getByRole('button', { name: 'Сохранить' }).click();
  await expect(
    page.getByRole('heading', { level: 1, name: 'Ryzen 7 7800X3D Updated' }),
  ).toBeVisible();

  await page.getByRole('button', { name: 'В архив' }).click();
  const archiveDialog = page.getByRole('dialog', { name: 'Архивировать компонент?' });
  await archiveDialog.getByRole('button', { name: 'В архив' }).click();
  await expect(page.getByRole('button', { name: 'Восстановить' })).toBeVisible();

  await page.getByRole('button', { name: 'Восстановить' }).click();
  await expect(page.getByText('Компонент восстановлен')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Редактировать' })).toBeVisible();
});

test('manages the component image gallery on desktop and mobile', async ({ page }) => {
  await page.goto('/components');
  await page.getByRole('link', { name: 'Ryzen 7 7800X3D' }).first().click();

  await expect(page.getByRole('heading', { level: 1, name: 'Ryzen 7 7800X3D' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Открыть изображение 1' })).toBeVisible();
  await page.locator('input[type="file"]').setInputFiles({
    name: 'component.png',
    mimeType: 'image/png',
    buffer: Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
      'base64',
    ),
  });
  await page.getByRole('button', { name: 'Загрузить' }).click();
  await expect(page.getByText('Изображение загружено')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Открыть изображение 3' })).toBeVisible();

  await page.getByRole('button', { name: 'Изменить порядок' }).click();
  await page.getByRole('button', { name: 'Переместить позже' }).first().click();
  await page.getByRole('button', { name: 'Сохранить порядок' }).click();
  await expect(page.getByText('Порядок изображений сохранён')).toBeVisible();

  await page.getByRole('button', { name: 'Удалить изображение 1' }).click();
  await page
    .getByRole('dialog', { name: 'Удалить изображение?' })
    .getByRole('button', {
      name: 'Удалить',
    })
    .click();
  await expect(page.getByText('Изображение удалено')).toBeVisible();

  await page.setViewportSize({ width: 390, height: 844 });
  const filePicker = page.getByLabel('Новое изображение');
  await expect(filePicker).toBeVisible();
  const filePickerBox = await filePicker.boundingBox();
  expect(filePickerBox).not.toBeNull();
  expect(filePickerBox!.x + filePickerBox!.width).toBeLessThanOrEqual(390);
});
