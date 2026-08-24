import { expect, test } from './fixtures/mock-api';

test('creates and permanently deletes a manual compatibility link on desktop and mobile', async ({
  page,
}) => {
  await page.goto('/settings/compatibility/manual');

  await expect(page.getByRole('heading', { level: 1, name: 'Ручная совместимость' })).toBeVisible();
  const desktopTable = page.getByTestId('desktop-manual-compatibility-table');
  await expect(desktopTable.getByText('Сокет AM5')).toBeVisible();
  await page.getByRole('button', { name: 'Добавить связь' }).click();
  const createDialog = page.getByRole('dialog', { name: 'Новая ручная связь' });
  await createDialog.getByRole('combobox', { name: 'Компонент' }).click();
  await page.getByRole('option', { name: /Ryzen 7 7800X3D/ }).click();
  await createDialog.getByRole('combobox', { name: 'Совместим с' }).click();
  await page.getByRole('option', { name: /Radeon RX 7900 XTX/ }).click();
  await createDialog.getByRole('textbox', { name: 'Комментарий' }).fill('Один блок питания');
  await createDialog.getByRole('button', { name: 'Добавить связь' }).click();

  await expect(page.getByText('Ручная связь создана')).toBeVisible();
  await expect(desktopTable.getByText('Один блок питания')).toBeVisible();
  await expect(desktopTable).toBeVisible();

  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByTestId('desktop-manual-compatibility-table')).toBeHidden();
  await expect(page.getByTestId('mobile-manual-compatibility-list')).toBeVisible();
  await page
    .getByRole('button', { name: 'Удалить связь Ryzen 7 7800X3D и Radeon RX 7900 XTX' })
    .click();
  await page
    .getByRole('dialog', { name: 'Удалить ручную связь?' })
    .getByRole('button', { name: 'Удалить' })
    .click();

  await expect(page.getByText('Ручная связь удалена')).toBeVisible();
  await expect(page.getByText('Один блок питания')).toHaveCount(0);
});

test('creates, edits, toggles and permanently deletes an automatic compatibility rule', async ({
  page,
}) => {
  await page.goto('/settings/compatibility/rules');

  await expect(
    page.getByRole('heading', { level: 1, name: 'Автоматические правила' }),
  ).toBeVisible();
  await expect(page.getByTestId('desktop-compatibility-rule-table')).toBeVisible();
  await expect(page.getByText('Количество линий').first()).toBeVisible();
  await page.getByRole('link', { name: 'Новое правило' }).click();
  await expect(page).toHaveURL(/\/settings\/compatibility\/rules\/new$/);

  await page.getByRole('textbox', { name: 'Название правила' }).fill('Сравнение линий');
  await page.getByRole('combobox', { name: 'Тип слева' }).click();
  await page.getByRole('option', { name: 'Процессор' }).click();
  await page.getByRole('combobox', { name: 'Тип справа' }).click();
  await page.getByRole('option', { name: 'Материнская плата' }).click();
  await page.getByRole('combobox', { name: 'Атрибут слева' }).click();
  await page.getByRole('option', { name: 'Количество ядер · NUMBER' }).click();
  await page.getByRole('combobox', { name: 'Оператор' }).click();
  await page.getByRole('option', { name: 'Больше или равно (≥)' }).click();
  await page.getByRole('combobox', { name: 'Атрибут справа' }).click();
  await page.getByRole('option', { name: 'Линии PCIe · NUMBER' }).click();
  await page.getByRole('button', { name: 'Создать правило' }).click();

  await expect(page).toHaveURL(/\/settings\/compatibility\/rules$/);
  await expect(page.getByText('Автоматическое правило создано')).toBeVisible();
  await expect(page.getByText('Сравнение линий').first()).toBeVisible();
  await page.getByRole('link', { name: 'Редактировать правило Сравнение линий' }).click();
  const name = page.getByRole('textbox', { name: 'Название правила' });
  await expect(name).toHaveValue('Сравнение линий');
  await name.fill('Сравнение линий PCIe');
  await page.getByRole('button', { name: 'Сохранить правило' }).click();

  await expect(page.getByText('Автоматическое правило сохранено')).toBeVisible();
  await page
    .getByRole('switch', { name: 'Отключить правило Сравнение линий PCIe' })
    .locator('..')
    .click();
  await expect(page.getByText('Автоматическое правило отключено')).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByTestId('desktop-compatibility-rule-table')).toBeHidden();
  await expect(page.getByTestId('mobile-compatibility-rule-list')).toBeVisible();
  await page.getByRole('button', { name: 'Удалить правило Сравнение линий PCIe' }).click();
  await page
    .getByRole('dialog', { name: 'Удалить автоматическое правило?' })
    .getByRole('button', { name: 'Удалить' })
    .click();

  await expect(page.getByText('Автоматическое правило удалено')).toBeVisible();
  await expect(page.getByText('Сравнение линий PCIe')).toHaveCount(0);
  await page.goto('/settings/compatibility/rules/new');
  await expect(page.getByRole('heading', { name: 'Новое автоматическое правило' })).toBeVisible();
  const mobileFormAction = page.getByRole('button', { name: 'Создать правило' });
  const mobileFormActionBox = await mobileFormAction.boundingBox();
  expect(mobileFormActionBox).not.toBeNull();
  expect(mobileFormActionBox!.x + mobileFormActionBox!.width).toBeLessThanOrEqual(390);
});

test('explores the manual compatibility graph on desktop and mobile', async ({ page }) => {
  await page.goto('/settings/compatibility/graph');

  await expect(page.getByRole('heading', { level: 1, name: 'Граф совместимости' })).toBeVisible();
  await expect(page.getByText(/только явно созданные ручные связи/)).toBeVisible();
  await expect(page.locator('.react-flow__node')).toHaveCount(3);
  await expect(page.locator('.react-flow__edge')).toHaveCount(1);

  const processor = page.getByLabel('Компонент Ryzen 7 7800X3D, тип Процессор');
  await processor.click();
  await expect(page.getByRole('heading', { level: 2, name: 'Ryzen 7 7800X3D' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Открыть карточку компонента' })).toHaveAttribute(
    'href',
    '/components/101',
  );

  await expect(processor).not.toHaveClass(/draggable/);

  await page.getByRole('button', { name: 'Сбросить раскладку' }).click();
  await expect(page.getByRole('heading', { name: 'Выберите элемент графа' })).toBeVisible();
  await page.getByLabel('Ручная связь между Ryzen 7 7800X3D и B650 Tomahawk').click();
  await expect(page.getByRole('heading', { name: 'Совместимые компоненты' })).toBeVisible();
  await expect(page.getByText('Сокет AM5')).toBeVisible();

  const search = page.getByRole('combobox', { name: 'Найти компонент' });
  await search.fill('Radeon');
  await page.getByRole('option', { name: /Radeon RX 7900 XTX/ }).click();
  await expect(page.getByRole('heading', { name: 'Radeon RX 7900 XTX' })).toBeVisible();
  await expect(page.getByText('У компонента нет ручных связей.')).toBeVisible();
  await page.getByRole('button', { name: 'Показать граф целиком' }).first().click();

  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByTestId('compatibility-graph-canvas')).toBeVisible();
  await expect(page.locator('.react-flow__minimap')).toBeHidden();
  await page.getByRole('link', { name: 'Управлять связями' }).click();
  await expect(page).toHaveURL(/\/settings\/compatibility\/manual$/);
});
