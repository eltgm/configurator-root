import { expect, test } from '../fixtures/mock-api';
import { expectNoAxeViolations } from './axe-test';

for (const colorScheme of ['light', 'dark'] as const) {
  test(`attribute autofill remains accessible in ${colorScheme} mode`, async ({
    page,
  }, testInfo) => {
    await page.addInitScript((scheme) => {
      localStorage.setItem('configurator.color-scheme', scheme);
    }, colorScheme);
    await page.goto('/settings/attributes');
    await page.getByRole('button', { name: 'Новый атрибут' }).click();
    const dialog = page.getByRole('dialog', { name: 'Новый атрибут' });
    const label = dialog.getByRole('textbox', { name: 'Название для пользователя' });
    const name = dialog.getByRole('textbox', { name: /Системное имя/ });
    await label.fill('Объём памяти');
    await expect(name).toHaveValue('obyom_pamyati');
    await label.press('Tab');
    await expect(name).toBeFocused();
    await name.fill('custom');
    await name.press('Tab');
    const fillButton = dialog.getByRole('button', { name: 'Заполнять из названия' });
    await expect(fillButton).toBeFocused();
    await fillButton.press('Enter');
    await expect(name).toHaveValue('obyom_pamyati');
    await expect(name).toBeFocused();
    await expectNoAxeViolations(page, testInfo);
    await testInfo.attach('attribute-autofill.png', {
      body: await dialog.screenshot(),
      contentType: 'image/png',
    });
    await name.clear();
    await dialog.getByRole('button', { name: 'Создать', exact: true }).click();
    await expect(dialog.getByText('Введите системное имя')).toBeVisible();
    await expectNoAxeViolations(page, testInfo);
    await testInfo.attach('attribute-autofill-error.png', {
      body: await dialog.screenshot(),
      contentType: 'image/png',
    });
  });
}

test('preferences menu remains accessible when expanded', async ({ page }, testInfo) => {
  await page.goto('/components');
  await expect(page.getByRole('heading', { level: 1, name: 'Компоненты' })).toBeVisible();
  await page.getByRole('button', { name: 'Настройки интерфейса' }).click();
  await expect(page.getByRole('menu')).toBeVisible();

  await expectNoAxeViolations(page, testInfo);
});

test('domain menu remains accessible when expanded', async ({ page }, testInfo) => {
  await page.goto('/components');
  await page.getByRole('button', { name: 'Предметная область: Сборка ПК' }).click();
  await expect(page.getByRole('menu')).toBeVisible();

  await expectNoAxeViolations(page, testInfo);
});

test('component validation errors remain accessible', async ({ page }, testInfo) => {
  await page.goto('/components/new');
  await expect(page.getByRole('heading', { level: 1, name: 'Новый компонент' })).toBeVisible();
  await page.getByRole('button', { name: 'Создать' }).click();
  await expect(page.getByText('Выберите тип компонента')).toBeVisible();

  await expectNoAxeViolations(page, testInfo);
});

for (const colorScheme of ['light', 'dark'] as const) {
  test(`destructive domain confirmation remains accessible in ${colorScheme} mode`, async ({
    page,
  }, testInfo) => {
    await page.addInitScript((scheme) => {
      localStorage.setItem('configurator.color-scheme', scheme);
    }, colorScheme);
    await page.goto('/settings/domain');
    await expect(page.getByRole('heading', { level: 2, name: 'Сборка ПК' })).toBeVisible();
    await page.getByRole('button', { name: 'Удалить область Сборка ПК' }).click();
    const dialog = page.getByRole('dialog', { name: 'Удалить предметную область?' });
    await expect(dialog).toBeVisible();
    await expect(dialog.getByRole('button', { name: 'Удалить' })).toBeDisabled();
    await expectNoAxeViolations(page, testInfo);
    await dialog.getByRole('textbox', { name: /Название области/ }).fill('Сборка ПК');
    await expect(dialog.getByRole('button', { name: 'Удалить' })).toBeEnabled();
    await expectNoAxeViolations(page, testInfo);
    await testInfo.attach('domain-deletion.png', {
      body: await dialog.screenshot(),
      contentType: 'image/png',
    });
  });
}

test('selected graph details remain accessible', async ({ page }, testInfo) => {
  await page.goto('/settings/compatibility/graph');
  const node = page.getByLabel('Компонент Ryzen 7 7800X3D, тип Процессор');
  await expect(node).toBeVisible();
  await node.click();
  await expect(page.getByRole('heading', { level: 2, name: 'Ryzen 7 7800X3D' })).toBeVisible();

  await expectNoAxeViolations(page, testInfo);
});

test('compatibility explanation remains accessible when expanded', async ({ page }, testInfo) => {
  await page.goto('/configurator');
  const browser = page.getByRole('region', { name: 'Доступные компоненты' });
  await browser.getByRole('button', { name: 'Добавить' }).first().click();
  await browser.getByRole('button', { name: 'Почему совместим' }).click();
  await expect(
    page.getByRole('dialog', { name: 'Почему совместим «B650 Tomahawk»' }),
  ).toBeVisible();

  await expectNoAxeViolations(page, testInfo);
});
