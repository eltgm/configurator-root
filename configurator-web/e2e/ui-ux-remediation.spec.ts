import { expect, frontendApiBaseUrl, test } from './fixtures/mock-api';

test('never exposes previous-domain catalog actions during a failed switch', async ({ page }) => {
  await page.goto('/components');
  await expect(page.getByRole('heading', { name: 'Ryzen 7 7800X3D' })).toBeVisible();
  let release!: () => void;
  const held = new Promise<void>((resolve) => {
    release = resolve;
  });
  await page.route(frontendApiBaseUrl + '/domains/202/components*', async (route) => {
    await held;
    await route.fulfill({ status: 503, json: { status: 503, message: 'Domain B unavailable' } });
  });
  await page.getByRole('button', { name: 'Предметная область: Сборка ПК' }).click();
  await page.getByRole('menuitem', { name: 'Рабочая станция' }).click();
  await expect(page.getByRole('heading', { name: 'Ryzen 7 7800X3D' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: /Архивировать Ryzen/ })).toHaveCount(0);
  release();
  await expect(page.getByRole('alert')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Ryzen 7 7800X3D' })).toHaveCount(0);
});

test('guards a rule during domain switching and preserves cancelled edits', async ({ page }) => {
  await page.goto('/settings/compatibility/rules/new');
  const name = page.getByRole('textbox', { name: 'Название правила' });
  await name.fill('Черновик правила');
  await page.getByRole('button', { name: 'Предметная область: Сборка ПК' }).click();
  await page.getByRole('menuitem', { name: 'Рабочая станция' }).click();
  const dialog = page.getByRole('dialog', { name: 'Сменить область без сохранения?' });
  await dialog.getByRole('button', { name: 'Остаться' }).click();
  await expect(name).toHaveValue('Черновик правила');
  await page.getByRole('button', { name: 'Предметная область: Сборка ПК' }).click();
  await page.getByRole('menuitem', { name: 'Рабочая станция' }).click();
  await dialog.getByRole('button', { name: 'Сменить область' }).click();
  await expect(page).toHaveURL(/\/settings\/compatibility\/rules$/);
});

test('offers every settings route on mobile and closes the panel across breakpoints', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/configurator');
  const settings = page.getByRole('button', { name: 'Настройка', exact: true });
  await settings.click();
  const panel = page.getByRole('dialog', { name: 'Разделы настроек' });
  await expect(panel.getByRole('link')).toHaveCount(7);
  await panel.locator('a[href="/settings/attributes"]').click();
  await expect(panel).toBeHidden();
  await expect(page).toHaveURL(/\/settings\/attributes$/);
  await settings.click();
  await page.setViewportSize({ width: 1440, height: 900 });
  await expect(panel).toBeHidden();
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(panel).toBeHidden();
  await expect(page.locator('.mantine-AppShell-navbar')).toBeHidden();
});

test('shows attributes in quick view and retains search and focus after adding', async ({
  page,
}) => {
  await page.goto('/configurator');
  const browser = page.getByRole('region', { name: 'Доступные компоненты' });
  const search = browser.getByRole('textbox');
  await search.fill('Ryzen');
  await browser.getByRole('button', { name: 'Быстрый просмотр: Ryzen 7 7800X3D' }).click();
  const preview = page.getByRole('dialog', { name: 'Ryzen 7 7800X3D' });
  await expect(preview.getByRole('rowheader', { name: 'Количество ядер' })).toBeVisible();
  await expect(preview.getByRole('cell', { name: '8', exact: true })).toBeVisible();
  await page.keyboard.press('Escape');
  await browser
    .getByRole('button', { name: 'Добавить Ryzen 7 7800X3D в сборку', exact: true })
    .click();
  await expect(search).toHaveValue('Ryzen');
  await expect(browser.getByRole('heading', { name: 'Доступные компоненты' })).toBeFocused();
});

test('returns to catalog filters from full details', async ({ page }) => {
  await page.goto('/components?q=Ryzen&type=11');
  await page.getByRole('link', { name: 'Ryzen 7 7800X3D', exact: true }).click();
  await expect(page.getByRole('link', { name: 'Компоненты', exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'К каталогу' }).click();
  await expect(page).toHaveURL(/\/components\?q=Ryzen&type=11$/);
  await expect(page.getByRole('textbox')).toHaveValue('Ryzen');
});

test('protects dirty modal values on Escape and Cancel, restoring focus', async ({ page }) => {
  await page.goto('/settings/domain');
  await page.getByRole('button', { name: 'Новая область' }).click();
  const name = page.getByRole('textbox', { name: 'Название', exact: true });
  await name.fill('Черновик');
  await page.keyboard.press('Escape');
  const confirmation = page.getByRole('dialog', { name: 'Отбросить изменения?' });
  await expect(confirmation.getByRole('button', { name: 'Продолжить заполнение' })).toBeFocused();
  await confirmation.getByRole('button', { name: 'Продолжить заполнение' }).click();
  await expect(name).toHaveValue('Черновик');
  await expect(name).toBeFocused();
  await page.getByRole('dialog').getByRole('button', { name: 'Отмена' }).click();
  await confirmation.getByRole('button', { name: 'Отбросить изменения', exact: true }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await page.getByRole('button', { name: 'Новая область' }).click();
  await expect(name).toHaveValue('');
  await page.keyboard.press('Escape');
  await expect(page.getByRole('dialog')).toHaveCount(0);
});

test('focuses the first invalid controlled component type field', async ({ page }) => {
  await page.goto('/components/new');
  await page.getByRole('textbox', { name: 'Название', exact: true }).fill('New model');
  await page.getByRole('button', { name: 'Создать', exact: true }).click();
  await expect(page.getByRole('combobox', { name: /Тип компонента/ })).toBeFocused();
});

test('queries configurations across 1000 rows and provides recoverable empty results', async ({
  page,
}) => {
  await page.route(frontendApiBaseUrl + '/domains/*/configurations*', async (route) => {
    const url = new URL(route.request().url());
    const name = url.searchParams.get('name') ?? '';
    const rows = Array.from({ length: 1000 }, (_, id) => ({
      id,
      domainId: 101,
      name: `Build ${id}`,
      createdAt: '2026-09-09T12:00:00Z',
      trackInventory: false,
      components: [],
    }));
    const filtered = rows.filter((row) => row.name.includes(name));
    const size = Number(url.searchParams.get('size')) || 10;
    await route.fulfill({
      json: { items: filtered.slice(0, size), totalItems: filtered.length, page: 0, size },
    });
  });
  await page.goto('/configurations');
  const search = page.getByRole('textbox', { name: 'Поиск конфигураций' });
  await search.fill('Build 999');
  await expect(page.getByRole('heading', { name: 'Build 999' })).toBeVisible();
  await search.fill('No such build');
  await expect(page.getByRole('heading', { name: 'Ничего не найдено' })).toBeVisible();
  await page.getByRole('button', { name: 'Сбросить фильтры' }).first().click();
  await expect(search).toHaveValue('');
});

test('exposes every blocked candidate beyond the first twelve', async ({ page }) => {
  await page.route(frontendApiBaseUrl + '/domains/*/configurator/candidates', async (route) => {
    const components = Array.from({ length: 25 }, (_, index) => ({
      id: 1000 + index,
      name: `Blocked model ${index + 1}`,
      componentTypeId: 12,
      availableQuantity: 1,
      status: 'BLOCKED',
      compatibilityByBase: [
        {
          baseComponentId: 101,
          decision: 'DENIED',
          explanations: [],
          blockingRules: [{ ruleSetId: 1, ruleSetName: 'Socket mismatch' }],
        },
      ],
    }));
    await route.fulfill({
      json: {
        componentIds: [101],
        assemblyStatus: 'SINGLE_COMPONENT',
        assemblyDecisions: [],
        candidatesByType: [
          { componentTypeId: 12, componentTypeName: 'Материнская плата', components },
        ],
      },
    });
  });
  await page.goto('/configurator');
  const browser = page.getByRole('region', { name: 'Доступные компоненты' });
  await browser
    .getByRole('button', { name: 'Добавить Ryzen 7 7800X3D в сборку', exact: true })
    .click();
  await browser.getByRole('button', { name: /Недоступные варианты/ }).click();
  await expect(browser.getByText('Показано 12 из 25')).toBeVisible();
  await browser.getByRole('button', { name: 'Показать ещё' }).click();
  await expect(browser.getByText('Показано 24 из 25')).toBeVisible();
  await browser.getByRole('button', { name: 'Показать ещё' }).click();
  await expect(browser.getByRole('link', { name: 'Blocked model 25', exact: true })).toBeVisible();
  await expect(browser.getByRole('button', { name: 'Показать ещё' })).toHaveCount(0);
});

test('keeps a mutation conflict in the dialog and clears it when reopening', async ({ page }) => {
  await page.route(frontendApiBaseUrl + '/domains', async (route) => {
    if (route.request().method() !== 'POST') return route.fallback();
    await route.fulfill({
      status: 409,
      json: {
        timestamp: '2026-09-09T12:00:00Z',
        status: 409,
        error: 'Conflict',
        code: 'ENTITY_ALREADY_EXISTS',
        message: 'Domain already exists',
        path: '/domains',
        details: [],
      },
    });
  });
  await page.goto('/settings/domain');
  await page.getByRole('button', { name: 'Новая область' }).click();
  const form = page.getByRole('dialog', { name: 'Новая предметная область' });
  await form.getByRole('textbox', { name: 'Название', exact: true }).fill('Duplicate');
  await form.getByRole('button', { name: 'Создать', exact: true }).click();
  await expect(form.getByRole('alert')).toContainText('Domain already exists');
  await expect(form.getByRole('alert')).toBeFocused();
  await expect(page.getByRole('alert')).toHaveCount(1);
  await form.getByRole('button', { name: 'Отмена' }).click();
  await page.getByRole('button', { name: 'Отбросить изменения', exact: true }).click();
  await page.getByRole('button', { name: 'Новая область' }).click();
  await expect(form.getByRole('alert')).toHaveCount(0);
});

test('reflows candidate cards at 320 pixels and puts selection before the assembly', async ({
  page,
}) => {
  await page.setViewportSize({ width: 320, height: 780 });
  await page.goto('/configurator');
  const browser = page.getByRole('region', { name: 'Доступные компоненты' });
  const firstAdd = browser.getByRole('button', {
    name: 'Добавить Ryzen 7 7800X3D в сборку',
    exact: true,
  });
  await expect(firstAdd).toBeVisible();
  const browserBox = await browser.boundingBox();
  const assemblyBox = await page.getByRole('region', { name: 'Текущая сборка' }).boundingBox();
  expect(browserBox!.y).toBeLessThan(assemblyBox!.y);
  expect(await page.evaluate('document.documentElement.scrollWidth')).toBeLessThanOrEqual(321);
  await page.getByRole('button', { name: 'Выбрать компоненты', exact: true }).click();
  await expect(browser.getByRole('heading')).toBeFocused();
  await expect(firstAdd).toBeInViewport();
  await page.screenshot({ path: 'test-results/uiux-mobile.png', fullPage: true });
});

test.describe('touch targets', () => {
  test.use({ hasTouch: true });
  test('keeps small icon actions at least 44 CSS pixels on touch devices', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto('/settings/attributes');
    await expect(page.getByRole('heading', { name: 'Количество ядер' })).toBeVisible();
    expect(await page.evaluate("matchMedia('(pointer: coarse)').matches")).toBe(true);
    const actions = page.locator('[class~="mantine-ActionIcon-root"]:visible');
    expect(await actions.count()).toBeGreaterThan(1);
    for (const action of await actions.all()) {
      const box = await action.boundingBox();
      expect(box!.width).toBeGreaterThanOrEqual(44);
      expect(box!.height).toBeGreaterThanOrEqual(44);
    }
  });
});
