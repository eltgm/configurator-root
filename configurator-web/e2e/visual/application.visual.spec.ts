import type { Page } from '@playwright/test';

import { expect, expectNoUnexpectedHorizontalOverflow, test } from '../fixtures/mock-api';

async function waitForVisualReady(page: Page) {
  await page.evaluate('document.fonts.ready');
  await expect(page.locator('[role="progressbar"]')).toHaveCount(0);
  await page.evaluate('window.scrollTo(0, 0)');
}

test('configurator workspace', async ({ page }) => {
  await page.goto('/configurator');
  await expect(page.getByRole('heading', { level: 1, name: 'Конфигуратор' })).toBeVisible();
  await waitForVisualReady(page);

  await expect(page).toHaveScreenshot('configurator-light.png', { fullPage: true });
});

test('component catalog cards and table', async ({ page }) => {
  await page.goto('/components');
  await expect(page.getByRole('heading', { level: 2, name: 'Ryzen 7 7800X3D' })).toBeVisible();
  await waitForVisualReady(page);
  await expect(page).toHaveScreenshot('component-catalog-cards.png', { fullPage: true });

  await page.getByText('Таблица', { exact: true }).click();
  await expect(page.getByTestId('desktop-component-table')).toBeVisible();
  await expect(page).toHaveScreenshot('component-catalog-table.png', { fullPage: true });
});

test('component details and gallery', async ({ page }) => {
  await page.goto('/components/101');
  await expect(page.getByRole('heading', { level: 1, name: 'Ryzen 7 7800X3D' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Открыть изображение 1' })).toBeVisible();
  await waitForVisualReady(page);

  await expect(page).toHaveScreenshot('component-details-gallery.png', { fullPage: true });
});

test('types and compatibility rules', async ({ page }) => {
  await page.goto('/settings/types');
  await expect(page.getByText('Количество ядер')).toBeVisible();
  await waitForVisualReady(page);
  await expect(page).toHaveScreenshot('types-and-attributes.png', { fullPage: true });

  await page.goto('/settings/compatibility/rules');
  await expect(
    page.getByRole('heading', { level: 1, name: 'Автоматические правила' }),
  ).toBeVisible();
  await waitForVisualReady(page);
  await expect(page).toHaveScreenshot('compatibility-rules.png', { fullPage: true });
});

test('domain attribute catalog', async ({ page }) => {
  await page.goto('/settings/attributes');
  await expect(page.getByRole('heading', { level: 1, name: 'Атрибуты' })).toBeVisible();
  await expect(page.getByRole('heading', { level: 2, name: 'Количество ядер' })).toBeVisible();
  await waitForVisualReady(page);

  await expect(page).toHaveScreenshot('attribute-catalog.png', { fullPage: true });

  await page.setViewportSize({ width: 390, height: 844 });
  await waitForVisualReady(page);
  await expect(page).toHaveScreenshot('attribute-catalog-mobile.png', { fullPage: true });
});

test('compatibility graph with selected node', async ({ page }) => {
  await page.goto('/settings/compatibility/graph');
  const node = page.getByLabel('Компонент Ryzen 7 7800X3D, тип Процессор');
  await expect(node).toBeVisible();
  await node.click();
  await expect(page.getByRole('heading', { level: 2, name: 'Ryzen 7 7800X3D' })).toBeVisible();
  await waitForVisualReady(page);

  await expect(page).toHaveScreenshot('compatibility-graph-selected.png', { fullPage: true });
});

test('configurations and destructive confirmation', async ({ page }) => {
  await page.goto('/configurator');
  const componentBrowser = page.getByRole('region', { name: 'Доступные компоненты' });
  await componentBrowser.getByRole('button', { name: 'Добавить' }).first().click();
  await expect(componentBrowser.getByText('B650 Tomahawk')).toBeVisible();
  await componentBrowser.getByRole('button', { name: 'Добавить' }).click();
  await page.getByRole('button', { name: 'Сохранить конфигурацию' }).click();
  const saveDialog = page.getByRole('dialog', { name: 'Сохранение конфигурации' });
  await saveDialog.getByRole('textbox', { name: 'Название' }).fill('Рабочая сборка');
  await saveDialog.getByRole('button', { name: 'Сохранить конфигурацию', exact: true }).click();
  await expect(page).toHaveURL(/\/configurations$/);
  await waitForVisualReady(page);
  await expect(page).toHaveScreenshot('configurations-list.png', { fullPage: true });

  await page.getByRole('link', { name: 'Рабочая сборка' }).click();
  await page.getByRole('button', { name: 'Удалить' }).click();
  const dialog = page.getByRole('dialog', { name: 'Удалить конфигурацию?' });
  await expect(dialog).toBeVisible();
  await expect(dialog).toHaveScreenshot('configuration-delete-dialog.png');
});

test('mobile dark catalog, details and configurations', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.addInitScript(
    'localStorage.setItem("configurator.color-scheme", "dark"); localStorage.setItem("configurator.catalog.view", "cards");',
  );
  await page.goto('/components');
  await expect(page.locator('html')).toHaveAttribute('data-mantine-color-scheme', 'dark');
  await expect(page.getByRole('heading', { level: 2, name: 'Ryzen 7 7800X3D' })).toBeVisible();
  await waitForVisualReady(page);
  await expectNoUnexpectedHorizontalOverflow(page);

  await expect(page).toHaveScreenshot('component-catalog-mobile-dark.png');

  await page.goto('/components/101');
  await expect(page.getByRole('heading', { level: 1, name: 'Ryzen 7 7800X3D' })).toBeVisible();
  await waitForVisualReady(page);
  await expectNoUnexpectedHorizontalOverflow(page);
  await expect(page).toHaveScreenshot('component-details-mobile-dark.png');

  await page.goto('/configurations');
  await expect(page.getByRole('heading', { level: 1, name: 'Конфигурации' })).toBeVisible();
  await waitForVisualReady(page);
  await expectNoUnexpectedHorizontalOverflow(page);
  const mobileNavigationLinks = page
    .getByRole('navigation', { name: 'Мобильная навигация' })
    .getByRole('link');
  await expect(mobileNavigationLinks).toHaveCount(4);
  for (let index = 0; index < 4; index += 1) {
    await expect(mobileNavigationLinks.nth(index)).toBeInViewport();
  }
  await expect(page).toHaveScreenshot('configurations-mobile-dark.png');
});
