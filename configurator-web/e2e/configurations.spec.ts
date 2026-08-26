import { readFile } from 'node:fs/promises';

import { expect, test } from './fixtures/mock-api';

test('saves the current assembly and shows it in the configurations list', async ({ page }) => {
  await page.goto('/configurator');

  const browser = page.getByRole('region', { name: 'Доступные компоненты' });
  const assembly = page.getByRole('region', { name: 'Текущая сборка' });
  await browser.getByRole('button', { name: 'Добавить' }).first().click();
  await browser.getByRole('button', { name: 'Добавить' }).first().click();
  await expect(assembly.getByText('Сборка корректна')).toBeVisible();
  await expect(assembly.getByRole('button', { name: 'Сохранить конфигурацию' })).toBeEnabled();
  await assembly.getByRole('button', { name: 'Сохранить конфигурацию' }).click();

  const dialog = page.getByRole('dialog', { name: 'Сохранение конфигурации' });
  await expect(dialog.getByText('Ryzen 7 7800X3D')).toBeVisible();
  await dialog.getByRole('textbox', { name: /Название/ }).fill('Домашний ПК');
  await dialog.getByRole('textbox', { name: 'Описание' }).fill('Тихая сборка');
  const createRequest = page.waitForRequest(
    (request) =>
      request.method() === 'POST' &&
      new URL(request.url()).pathname.endsWith('/domains/101/configurations'),
  );
  await dialog.getByRole('button', { name: 'Сохранить конфигурацию' }).click();

  expect((await createRequest).postDataJSON()).toEqual({
    name: 'Домашний ПК',
    description: 'Тихая сборка',
    componentIds: [101, 102],
  });
  await expect(page).toHaveURL(/\/configurations$/);
  const card = page.getByRole('article');
  await expect(card.getByRole('heading', { name: 'Домашний ПК' })).toBeVisible();
  await expect(card.getByText('Тихая сборка')).toBeVisible();
  await expect(card.getByText('Ryzen 7 7800X3D')).toBeVisible();
  const storedDraft = await page.evaluate<string | null>(
    "window.localStorage.getItem('configurator.assembly-draft.v1.101')",
  );
  expect(JSON.parse(storedDraft ?? '{}') as unknown).toMatchObject({ version: 1, items: [] });

  await card.getByRole('link', { name: 'Открыть конфигурацию' }).click();
  await expect(page).toHaveURL(/\/configurations\/901$/);
  await expect(page.getByRole('heading', { level: 1, name: 'Домашний ПК' })).toBeVisible();
  await page.getByRole('link', { name: 'Редактировать' }).click();
  await expect(page).toHaveURL(/\/configurations\/901\/edit$/);

  await page.getByRole('textbox', { name: 'Название' }).fill('Домашний ПК 2026');
  const composition = page.getByRole('region', { name: 'Состав конфигурации' });
  await composition
    .locator('[data-with-border="true"]')
    .filter({ hasText: 'Ryzen 7 7800X3D' })
    .getByRole('button', { name: 'Заменить' })
    .click();
  const replacementBrowser = page.getByRole('region', { name: 'Выбор замены' });
  await expect(replacementBrowser.getByText('Core Ultra 9 285K')).toBeVisible();
  await replacementBrowser.getByRole('button', { name: 'Выбрать Core Ultra 9 285K' }).click();
  await expect(composition.getByText('Core Ultra 9 285K')).toBeVisible();

  const updateRequest = page.waitForRequest(
    (request) =>
      request.method() === 'PUT' && new URL(request.url()).pathname.endsWith('/configurations/901'),
  );
  await page.getByRole('button', { name: 'Сохранить изменения' }).click();
  expect((await updateRequest).postDataJSON()).toEqual({
    name: 'Домашний ПК 2026',
    description: 'Тихая сборка',
    componentIds: [104, 102],
  });
  await expect(page).toHaveURL(/\/configurations\/901$/);
  await expect(page.getByRole('heading', { level: 1, name: 'Домашний ПК 2026' })).toBeVisible();
  await expect(page.getByText('Core Ultra 9 285K')).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });

  await page.getByRole('button', { name: 'Копировать' }).click();
  const copyDialog = page.getByRole('dialog', { name: 'Копирование конфигурации' });
  await expect(copyDialog.getByRole('textbox', { name: /Название/ })).toHaveValue(
    'Домашний ПК 2026 — копия',
  );
  await expect(copyDialog.getByText('Core Ultra 9 285K')).toBeVisible();
  const copyRequest = page.waitForRequest(
    (request) =>
      request.method() === 'POST' &&
      new URL(request.url()).pathname.endsWith('/domains/101/configurations'),
  );
  await copyDialog.getByRole('button', { name: 'Создать копию' }).click();
  expect((await copyRequest).postDataJSON()).toEqual({
    name: 'Домашний ПК 2026 — копия',
    description: 'Тихая сборка',
    componentIds: [104, 102],
  });
  await expect(page).toHaveURL(/\/configurations\/902$/);

  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Скачать JSON' }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toBe('configuration-902.json');
  const downloadPath = await download.path();
  expect(downloadPath).not.toBeNull();
  const exported = JSON.parse(await readFile(downloadPath, 'utf8')) as {
    schemaVersion: number;
    configuration: { id: number; name: string };
  };
  expect(exported).toMatchObject({
    schemaVersion: 1,
    configuration: { id: 902, name: 'Домашний ПК 2026 — копия' },
  });
  const exportNotification = page.getByRole('alert').filter({ hasText: 'JSON-экспорт скачан' });
  await exportNotification.getByRole('button').click();

  await page.getByRole('button', { name: 'Удалить' }).click();
  let deleteDialog = page.getByRole('dialog', { name: 'Удалить конфигурацию?' });
  await deleteDialog.getByRole('button', { name: 'Отмена' }).click();
  await expect(deleteDialog).toBeHidden();
  await expect(page).toHaveURL(/\/configurations\/902$/);

  await page.getByRole('button', { name: 'Удалить' }).click();
  deleteDialog = page.getByRole('dialog', { name: 'Удалить конфигурацию?' });
  const deleteRequest = page.waitForRequest(
    (request) =>
      request.method() === 'DELETE' &&
      new URL(request.url()).pathname.endsWith('/configurations/902'),
  );
  await deleteDialog.getByRole('button', { name: 'Удалить' }).click();
  await deleteRequest;
  await expect(page).toHaveURL(/\/configurations$/);
  await expect(page.getByRole('heading', { name: 'Домашний ПК 2026' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Домашний ПК 2026 — копия' })).toHaveCount(0);
  const draftAfterOperations = await page.evaluate<string | null>(
    "window.localStorage.getItem('configurator.assembly-draft.v1.101')",
  );
  expect(JSON.parse(draftAfterOperations ?? '{}') as unknown).toMatchObject({
    version: 1,
    items: [],
  });

  await page.goto('/components');
  await expect(page.getByText('Core Ultra 9 285K')).toBeVisible();

  const mainBox = await page.getByRole('main').boundingBox();
  expect(mainBox).not.toBeNull();
  expect(mainBox!.x + mainBox!.width).toBeLessThanOrEqual(390);
});
