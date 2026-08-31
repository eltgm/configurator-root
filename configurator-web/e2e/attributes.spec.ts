import { expect, test } from './fixtures/mock-api';

for (const entry of ['catalog', 'type'] as const) {
  test(`autofills and preserves manual attribute names from the ${entry}`, async ({
    page,
  }, testInfo) => {
    await page.goto(entry === 'catalog' ? '/settings/attributes' : '/settings/types');
    if (entry === 'catalog') {
      await page.getByRole('button', { name: 'Новый атрибут' }).click();
    } else {
      await page.getByRole('button', { name: 'Добавить', exact: true }).click();
      await page.getByRole('menuitem', { name: 'Создать новый' }).click();
    }
    const dialog = page.getByRole('dialog', { name: 'Новый атрибут' });
    const label = dialog.getByRole('textbox', { name: 'Название для пользователя' });
    const name = dialog.getByRole('textbox', { name: /Системное имя/ });
    await expect(label).toBeFocused();
    await label.fill('Разъём USB Type-C');
    await expect(name).toHaveValue('razyom_usb_type_c');
    await label.press('Tab');
    await expect(name).toBeFocused();
    await name.fill('Custom_USB');
    await label.fill('Основной разъём USB');
    await expect(name).toHaveValue('Custom_USB');
    await name.clear();
    await label.fill('MemorySize');
    await expect(name).toHaveValue('');
    await dialog.getByRole('button', { name: 'Заполнять из названия' }).click();
    await expect(name).toHaveValue('memory_size');
    await expect(name).toBeFocused();
    await label.fill('Объём памяти');
    await expect(name).toHaveValue('obyom_pamyati');
    await testInfo.attach('attribute-autofill.png', {
      body: await dialog.screenshot(),
      contentType: 'image/png',
    });
    await label.press('Enter');
    await expect(dialog).toBeHidden();

    await page
      .getByRole('button', { name: 'Редактировать атрибут Объём памяти', exact: true })
      .click();
    const editDialog = page.getByRole('dialog', { name: 'Редактирование атрибута' });
    const editLabel = editDialog.getByRole('textbox', { name: 'Название для пользователя' });
    const editName = editDialog.getByRole('textbox', { name: /Системное имя/ });
    await expect(editName).toHaveValue('obyom_pamyati');
    await editLabel.fill('Доступный объём памяти');
    await expect(editName).toHaveValue('obyom_pamyati');
    await editDialog.getByRole('button', { name: 'Сохранить', exact: true }).click();
    await expect(editDialog).toBeHidden();
    await page
      .getByRole('button', { name: 'Редактировать атрибут Доступный объём памяти', exact: true })
      .click();
    await expect(editName).toHaveValue('obyom_pamyati');
  });
}

test('keeps a conflicting generated name editable without inventing a suffix', async ({ page }) => {
  await page.goto('/settings/attributes');
  await page.getByRole('button', { name: 'Новый атрибут' }).click();
  const dialog = page.getByRole('dialog', { name: 'Новый атрибут' });
  const label = dialog.getByRole('textbox', { name: 'Название для пользователя' });
  const name = dialog.getByRole('textbox', { name: /Системное имя/ });
  await label.fill('Cores');
  await expect(name).toHaveValue('cores');
  await dialog.getByRole('button', { name: 'Создать', exact: true }).click();
  await expect(
    dialog.getByText(/Атрибут с таким системным именем уже есть в области/),
  ).toBeVisible();
  await expect(name).toHaveValue('cores');
  await name.fill('physical_cores');
  await label.fill('Physical Cores');
  await expect(name).toHaveValue('physical_cores');
  await dialog.getByRole('button', { name: 'Создать', exact: true }).click();
  await expect(dialog).toBeHidden();
});

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
  await expect(page.getByRole('option', { name: /Количество ядер.*уже подключён/ })).toBeVisible();
  await expect(
    page.getByRole('option', { name: /Количество ядер.*уже подключён/ }),
  ).toHaveAttribute('data-combobox-disabled', 'true');
  await page.getByRole('option', { name: /Стандарт памяти/ }).click();
  await attachDialog.getByRole('switch', { name: 'Обязательный атрибут' }).check();
  await attachDialog.getByRole('button', { name: 'Подключить' }).click();
  await expect(page.getByText('Стандарт памяти', { exact: true })).toBeVisible();

  await page.getByRole('button', { name: 'Убрать атрибут Стандарт памяти из типа' }).click();
  const detachDialog = page.getByRole('dialog', { name: 'Убрать атрибут из типа?' });
  await expect(detachDialog.getByText(/Значения этого атрибута/)).toBeVisible();
  await detachDialog.getByRole('button', { name: 'Убрать из типа' }).click();
  await expect(page.getByText('Стандарт памяти', { exact: true })).toHaveCount(0);

  await page.goto('/settings/attributes');
  await expect(page.getByRole('heading', { name: 'Стандарт памяти' })).toBeVisible();
  await page.getByRole('button', { name: 'Удалить атрибут Стандарт памяти' }).click();
  await page
    .getByRole('dialog', { name: 'Удалить атрибут из каталога?' })
    .getByRole('button', { name: 'Удалить атрибут' })
    .click();
  await expect(page.getByRole('heading', { name: 'Стандарт памяти' })).toHaveCount(0);
});

test('uses the same name rule from both creation screens and reuses the definition across types', async ({
  page,
}) => {
  await page.goto('/settings/types');
  await page.getByRole('button', { name: 'Добавить', exact: true }).click();
  await page.getByRole('menuitem', { name: 'Создать новый' }).click();
  let dialog = page.getByRole('dialog', { name: 'Новый атрибут' });
  await dialog.getByRole('textbox', { name: /Системное имя/ }).fill('shared_connector');
  await dialog.getByRole('textbox', { name: 'Название для пользователя' }).fill('Общий разъём');
  await dialog.getByRole('button', { name: 'Создать', exact: true }).click();
  await expect(page.getByText('Общий разъём', { exact: true })).toBeVisible();

  await page.goto('/settings/attributes');
  await page.getByRole('button', { name: 'Новый атрибут', exact: true }).click();
  dialog = page.getByRole('dialog', { name: 'Новый атрибут' });
  await dialog.getByRole('textbox', { name: /Системное имя/ }).fill('shared_connector');
  await dialog.getByRole('textbox', { name: 'Название для пользователя' }).fill('Другой разъём');
  await dialog.getByRole('button', { name: 'Создать', exact: true }).click();
  await expect(
    dialog.getByText(/Атрибут с таким системным именем уже есть в области/),
  ).toBeVisible();
  await dialog.getByRole('button', { name: 'Отмена', exact: true }).click();

  await page.goto('/settings/types');
  await page.getByRole('button', { name: /Материнская плата/ }).click();
  await page.getByRole('button', { name: 'Добавить', exact: true }).click();
  await page.getByRole('menuitem', { name: 'Создать новый' }).click();
  dialog = page.getByRole('dialog', { name: 'Новый атрибут' });
  await dialog.getByRole('textbox', { name: /Системное имя/ }).fill('shared_connector');
  await dialog.getByRole('textbox', { name: 'Название для пользователя' }).fill('Другой разъём');
  await dialog.getByRole('button', { name: 'Создать', exact: true }).click();
  await expect(
    dialog.getByText(/Атрибут с таким системным именем уже есть в области/),
  ).toBeVisible();
  await dialog.getByRole('button', { name: 'Отмена', exact: true }).click();

  await page.getByRole('button', { name: 'Добавить', exact: true }).click();
  await page.getByRole('menuitem', { name: 'Использовать существующий' }).click();
  dialog = page.getByRole('dialog', { name: 'Использовать существующий атрибут' });
  await dialog.getByRole('combobox', { name: 'Атрибут из каталога' }).click();
  await page.getByRole('option', { name: /Общий разъём/ }).click();
  await dialog.getByRole('button', { name: 'Подключить', exact: true }).click();
  await expect(page.getByText('Общий разъём', { exact: true })).toBeVisible();

  const catalog = await page.evaluate(
    async () =>
      (await (await fetch('/api/domains/101/attributes')).json()) as Array<{
        name: string;
        componentTypeIds: Array<number>;
      }>,
  );
  const shared = catalog.filter((attribute) => attribute.name === 'shared_connector');
  expect(shared).toHaveLength(1);
  expect(shared[0].componentTypeIds.sort()).toEqual([11, 12]);
});
