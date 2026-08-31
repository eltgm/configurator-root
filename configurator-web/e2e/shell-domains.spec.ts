import {
  expect,
  expectNoUnexpectedHorizontalOverflow,
  expectVisibleButtonTargetsAtLeast24Pixels,
  test,
  waitForJsonRequest,
} from './fixtures/mock-api';

test('creates the demo from the first-run state', async ({ page }) => {
  let domains: Array<{
    id: number;
    name: string;
    description: string;
    createdAt: string;
  }> = [];

  await page.route(/\/api\/domains(?:\/.*)?(?:\?.*)?$/, async (route) => {
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    if (pathname === '/api/domains/demo' && request.method() === 'POST') {
      const demo = {
        id: 303,
        name: 'Сборка ПК',
        description: 'Демонстрационная предметная область',
        createdAt: '2026-08-23T12:00:00Z',
      };
      domains = [demo];
      await route.fulfill({ status: 201, json: demo });
      return;
    }
    if (pathname === '/api/domains' && request.method() === 'GET') {
      await route.fulfill({
        json: { items: domains, page: 0, size: 100, totalItems: domains.length },
      });
      return;
    }
    await route.fallback();
  });

  await page.goto('/configurator');
  await expect(
    page.getByRole('heading', { level: 1, name: 'Начните с предметной области' }),
  ).toBeVisible();
  await page.getByRole('button', { name: 'Создать демо «Сборка ПК»' }).click();

  await expect(page.getByText('Демо «Сборка ПК» создано')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Предметная область: Сборка ПК' })).toBeVisible();
  await expect(page.getByRole('heading', { level: 1, name: 'Конфигуратор' })).toBeVisible();
});

test('keeps domain management usable on a phone viewport', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/settings/domain');

  await expect(page.getByRole('heading', { level: 1, name: 'Предметные области' })).toBeVisible();
  await expect(page.getByRole('heading', { level: 2, name: 'Сборка ПК' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Предметная область: Сборка ПК' })).toBeVisible();
});

test('creates, edits and permanently deletes a domain', async ({ page }) => {
  await page.goto('/settings/domain');
  await page.getByRole('button', { name: 'Новая область' }).click();

  const createDialog = page.getByRole('dialog', { name: 'Новая предметная область' });
  await createDialog.getByRole('textbox', { name: 'Название' }).fill('Ноутбуки');
  await createDialog.getByRole('textbox', { name: 'Описание' }).fill('Мобильные рабочие места');
  const createRequest = waitForJsonRequest<{ name: string; description: string }>(
    page,
    (request) => request.method() === 'POST' && new URL(request.url()).pathname === '/api/domains',
  );
  await createDialog.getByRole('button', { name: 'Создать' }).click();
  await expect(createRequest).resolves.toEqual({
    name: 'Ноутбуки',
    description: 'Мобильные рабочие места',
  });
  await expect(page.getByRole('heading', { level: 2, name: 'Ноутбуки' })).toBeVisible();

  await page.getByRole('button', { name: 'Редактировать область Ноутбуки' }).click();
  const editDialog = page.getByRole('dialog', { name: 'Редактирование области' });
  await editDialog.getByRole('textbox', { name: 'Название' }).fill('Мобильные ПК');
  await editDialog.getByRole('button', { name: 'Сохранить' }).click();
  await expect(page.getByRole('heading', { level: 2, name: 'Мобильные ПК' })).toBeVisible();

  await page.getByRole('button', { name: 'Удалить область Мобильные ПК' }).click();
  const deleteDialog = page.getByRole('dialog', { name: 'Удалить предметную область?' });
  await expect(deleteDialog.getByRole('button', { name: 'Удалить' })).toBeDisabled();
  await deleteDialog.getByRole('textbox', { name: /Название области/ }).fill('Мобильные ПК');
  await deleteDialog.getByRole('button', { name: 'Удалить' }).click();
  await expect(page.getByRole('heading', { level: 2, name: 'Мобильные ПК' })).toHaveCount(0);
});

test('shows types and attributes for the selected domain', async ({ page }) => {
  await page.goto('/settings/types');

  await expect(page.getByRole('heading', { level: 1, name: 'Типы и атрибуты' })).toBeVisible();
  await expect(page.getByRole('heading', { level: 2, name: 'Процессор' })).toBeVisible();
  await expect(page.getByText('Количество ядер')).toBeVisible();
  await expect(page.getByText('Обязательный')).toBeVisible();
});

test('creates and edits a type and attribute, then deletes an empty type', async ({ page }) => {
  await page.goto('/settings/types');
  await page.getByRole('button', { name: 'Новый тип' }).click();

  const createTypeDialog = page.getByRole('dialog', { name: 'Новый тип компонента' });
  await createTypeDialog.getByRole('textbox', { name: 'Название' }).fill('Видеокарта');
  await createTypeDialog.getByRole('textbox', { name: 'Код' }).fill('GPU');
  await createTypeDialog.getByRole('button', { name: 'Создать' }).click();
  await expect(page.getByRole('heading', { level: 2, name: 'Видеокарта' })).toBeVisible();

  await page.getByRole('button', { name: 'Редактировать тип Видеокарта' }).click();
  const editTypeDialog = page.getByRole('dialog', { name: 'Редактирование типа' });
  await editTypeDialog.getByRole('textbox', { name: 'Название' }).fill('Графический адаптер');
  await editTypeDialog.getByRole('button', { name: 'Сохранить' }).click();
  await expect(page.getByRole('heading', { level: 2, name: 'Графический адаптер' })).toBeVisible();

  await page.getByRole('button', { name: 'Добавить атрибут' }).first().click();
  const createAttributeDialog = page.getByRole('dialog', { name: 'Новый атрибут' });
  await createAttributeDialog.getByRole('textbox', { name: 'Системное имя' }).fill('memory');
  await createAttributeDialog.getByRole('textbox', { name: 'Название' }).fill('Объём памяти');
  await createAttributeDialog.getByRole('button', { name: 'Создать' }).click();
  await expect(page.getByText('Объём памяти')).toBeVisible();

  await page.getByRole('button', { name: 'Редактировать атрибут Объём памяти' }).click();
  const editAttributeDialog = page.getByRole('dialog', { name: 'Редактирование атрибута' });
  await editAttributeDialog.getByRole('textbox', { name: 'Название' }).fill('Видеопамять');
  await editAttributeDialog.getByRole('button', { name: 'Сохранить' }).click();
  await expect(page.getByText('Видеопамять')).toBeVisible();

  await page.getByRole('button', { name: 'Новый тип' }).click();
  await createTypeDialog.getByRole('textbox', { name: 'Название' }).fill('Корпус');
  await createTypeDialog.getByRole('button', { name: 'Создать' }).click();
  await page.getByRole('button', { name: 'Удалить тип Корпус' }).click();
  const deleteTypeDialog = page.getByRole('dialog', { name: 'Удалить тип компонента?' });
  await deleteTypeDialog.getByRole('button', { name: 'Удалить тип' }).click();
  await expect(page.getByText('Корпус', { exact: true })).toHaveCount(0);
});

test('supports 320 CSS pixel reflow and keyboard shell navigation', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 568 });
  const routes = [
    ['/configurator', 'Конфигуратор'],
    ['/components', 'Компоненты'],
    ['/configurations', 'Конфигурации'],
    ['/settings/types', 'Типы и атрибуты'],
    ['/settings/compatibility/manual', 'Ручная совместимость'],
    ['/settings/compatibility/rules', 'Автоматические правила'],
    ['/settings/compatibility/graph', 'Граф совместимости'],
    ['/settings/domain', 'Предметные области'],
  ] as const;

  for (const [route, heading] of routes) {
    await page.goto(route);
    await expect(page.getByRole('heading', { level: 1, name: heading })).toBeVisible();
    await expectNoUnexpectedHorizontalOverflow(page);
    await expectVisibleButtonTargetsAtLeast24Pixels(page);
  }

  await page.goto('/configurator');
  const skipLink = page.getByRole('link', { name: 'Перейти к содержимому' });
  await expect(page.locator('a[href], button, input, select, textarea').first()).toHaveText(
    'Перейти к содержимому',
  );
  await skipLink.focus();
  await expect(skipLink).toBeFocused();
  await expect(skipLink).toBeVisible();
  await page.keyboard.press('Enter');
  await expect(page.locator('#main-content')).toBeFocused();

  const mobileNavigation = page.getByRole('navigation', { name: 'Мобильная навигация' });
  const mobileLinks = mobileNavigation.getByRole('link');
  for (let index = 0; index < (await mobileLinks.count()); index += 1) {
    const bounds = await mobileLinks.nth(index).boundingBox();
    expect(bounds).not.toBeNull();
    expect(bounds?.width ?? 0).toBeGreaterThanOrEqual(44);
    expect(bounds?.height ?? 0).toBeGreaterThanOrEqual(44);
  }

  await mobileNavigation.getByRole('link', { name: 'Компоненты' }).click();
  await expect(page.getByTestId('route-announcement')).toContainText('Компоненты');
  const createComponentLink = page.getByRole('link', { name: 'Новый компонент' });
  await createComponentLink.focus();
  await expect(createComponentLink).toBeFocused();
  const focusedBounds = await createComponentLink.boundingBox();
  expect(focusedBounds).not.toBeNull();
  expect(focusedBounds?.y ?? 0).toBeGreaterThanOrEqual(64);
  expect((focusedBounds?.y ?? 0) + (focusedBounds?.height ?? 0)).toBeLessThanOrEqual(500);
  await expectNoUnexpectedHorizontalOverflow(page);

  await page.getByRole('button', { name: 'Настройки интерфейса' }).click();
  await page.getByRole('menuitem', { name: 'Тёмная' }).click();
  await expect(page.locator('html')).toHaveAttribute('data-mantine-color-scheme', 'dark');
  await page.getByRole('button', { name: 'Настройки интерфейса' }).click();
  await page.getByRole('menuitem', { name: 'English' }).click();
  await expect(page.locator('html')).toHaveAttribute('lang', 'en');
  await expect(page.getByRole('heading', { level: 1, name: 'Components' })).toBeVisible();
  await expectNoUnexpectedHorizontalOverflow(page);

  await page.setViewportSize({ width: 568, height: 320 });
  await expectNoUnexpectedHorizontalOverflow(page);
});
