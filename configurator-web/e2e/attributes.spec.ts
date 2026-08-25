import { expect, test } from './fixtures/mock-api';

test('creates, reuses, detaches and deletes a domain catalog attribute', async ({ page }) => {
  await page.goto('/settings/attributes');
  await page.getByRole('button', { name: 'Новый атрибут' }).click();
  const createDialog = page.getByRole('dialog', { name: 'Новый атрибут' });
  await createDialog.getByRole('textbox', { name: /Системное имя/ }).fill('memory_standard');
  await createDialog
    .getByRole('textbox', { name: 'Название для пользователя' })
    .fill('Стандарт памяти');
  await createDialog.getByRole('button', { name: 'Создать' }).click();
  await expect(page.getByRole('heading', { name: 'Стандарт памяти' })).toBeVisible();

  await page.goto('/settings/types');
  await page.getByRole('button', { name: 'Добавить' }).click();
  await page.getByRole('menuitem', { name: 'Использовать существующий' }).click();
  const attachDialog = page.getByRole('dialog', { name: 'Использовать существующий атрибут' });
  await attachDialog.getByRole('combobox', { name: 'Атрибут из каталога' }).click();
  await page.getByRole('option', { name: /Стандарт памяти/ }).click();
  await attachDialog.getByRole('switch', { name: 'Обязательный атрибут' }).check();
  await attachDialog.getByRole('button', { name: 'Подключить' }).click();
  await expect(page.getByText('Стандарт памяти')).toBeVisible();

  await page.getByRole('button', { name: 'Убрать атрибут Стандарт памяти из типа' }).click();
  const detachDialog = page.getByRole('dialog', { name: 'Убрать атрибут из типа?' });
  await expect(detachDialog.getByText(/Значения этого атрибута/)).toBeVisible();
  await detachDialog.getByRole('button', { name: 'Убрать из типа' }).click();
  await expect(page.getByText('Стандарт памяти')).toHaveCount(0);

  await page.goto('/settings/attributes');
  await expect(page.getByRole('heading', { name: 'Стандарт памяти' })).toBeVisible();
  await page.getByRole('button', { name: 'Удалить атрибут Стандарт памяти' }).click();
  await page
    .getByRole('dialog', { name: 'Удалить атрибут из каталога?' })
    .getByRole('button', { name: 'Удалить атрибут' })
    .click();
  await expect(page.getByRole('heading', { name: 'Стандарт памяти' })).toHaveCount(0);
});
