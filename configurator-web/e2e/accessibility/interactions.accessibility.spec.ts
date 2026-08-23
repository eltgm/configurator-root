import { expect, test } from '../fixtures/mock-api';
import { expectNoAxeViolations } from './axe-test';

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

test('destructive domain dialog remains accessible', async ({ page }, testInfo) => {
  await page.goto('/settings/domain');
  await expect(page.getByRole('heading', { level: 2, name: 'Сборка ПК' })).toBeVisible();
  await page.getByRole('button', { name: 'Удалить область Сборка ПК' }).click();
  await expect(page.getByRole('dialog', { name: 'Удалить предметную область?' })).toBeVisible();

  await expectNoAxeViolations(page, testInfo);
});

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
