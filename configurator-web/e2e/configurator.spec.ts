import { expect, frontendApiBaseUrl, test } from './fixtures/mock-api';

test('opens the configurator frontend with the selected domain', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveURL(/\/configurator$/);
  await expect(
    page.getByRole('heading', { level: 1, name: 'Конфигуратор', exact: true }),
  ).toBeVisible();
  await expect(page.getByRole('button', { name: 'Предметная область: Сборка ПК' })).toBeVisible();

  const browser = page.getByRole('region', { name: 'Доступные компоненты' });
  const assembly = page.getByRole('region', { name: 'Текущая сборка' });
  await browser.getByRole('button', { name: 'Добавить' }).first().click();
  await expect(assembly.getByText('Ryzen 7 7800X3D')).toBeVisible();
  await expect(browser.getByText('B650 Tomahawk')).toBeVisible();
  await browser.getByRole('button', { name: 'Добавить' }).click();
  await expect(assembly.getByText('B650 Tomahawk')).toBeVisible();
  await expect(assembly.getByText('Сборка корректна')).toBeVisible();

  const replacementRequest = page.waitForRequest((request) => {
    const url = new URL(request.url());
    return url.pathname.endsWith('/configurator/candidates') && request.method() === 'POST';
  });
  await assembly.getByRole('button', { name: 'Заменить Ryzen 7 7800X3D' }).click();
  await replacementRequest;
  const replacementBrowser = page.getByRole('region', { name: 'Выбор замены' });
  await expect(replacementBrowser.getByText('Core Ultra 9 285K')).toBeVisible();
  await replacementBrowser.getByRole('button', { name: 'Выбрать' }).click();
  const replaceDialog = page.getByRole('dialog', { name: 'Заменить компонент этого типа?' });
  await expect(replaceDialog.getByText(/Core Ultra 9 285K/)).toBeVisible();
  await replaceDialog.getByRole('button', { name: 'Заменить' }).click();
  await expect(assembly.getByText('Core Ultra 9 285K')).toBeVisible();
  await expect(assembly.getByText('Ryzen 7 7800X3D')).toHaveCount(0);

  await assembly.getByRole('button', { name: 'Убрать Core Ultra 9 285K из сборки' }).click();
  await expect(assembly.getByText('Core Ultra 9 285K')).toHaveCount(0);

  await page.reload();
  await expect(page.getByText(/Локальный черновик восстановлен/)).toBeVisible();
  await expect(
    page.getByRole('region', { name: 'Текущая сборка' }).getByText('B650 Tomahawk'),
  ).toBeVisible();

  const restoredAssembly = page.getByRole('region', { name: 'Текущая сборка' });
  await restoredAssembly.getByRole('button', { name: 'Очистить' }).click();
  await page
    .getByRole('dialog', { name: 'Очистить текущую сборку?' })
    .getByRole('button', { name: 'Очистить' })
    .click();
  await expect(restoredAssembly.getByRole('heading', { name: 'Сборка пока пуста' })).toBeVisible();

  await page
    .getByRole('region', { name: 'Доступные компоненты' })
    .getByRole('button', { name: 'Добавить' })
    .first()
    .click();
  await expect(restoredAssembly.getByText('Ryzen 7 7800X3D')).toBeVisible();
  await page.getByRole('button', { name: 'Предметная область: Сборка ПК' }).click();
  await page.getByRole('menuitem', { name: 'Рабочая станция' }).click();
  await expect(
    page.getByRole('region', { name: 'Текущая сборка' }).getByRole('heading', {
      name: 'Сборка пока пуста',
    }),
  ).toBeVisible();
  await page.getByRole('button', { name: 'Предметная область: Рабочая станция' }).click();
  await page.getByRole('menuitem', { name: 'Сборка ПК' }).click();
  await expect(
    page.getByRole('region', { name: 'Текущая сборка' }).getByText('Ryzen 7 7800X3D'),
  ).toBeVisible();

  await page.setViewportSize({ width: 390, height: 844 });
  await expect(restoredAssembly).toBeVisible();
  const workspaceBox = await page.getByRole('main').boundingBox();
  expect(workspaceBox).not.toBeNull();
  expect(workspaceBox!.x + workspaceBox!.width).toBeLessThanOrEqual(390);
});

test('explains a transitive candidate and returns the draft to strict validation', async ({
  page,
}) => {
  await page.addInitScript({
    content: `
      window.localStorage.setItem('configurator.selected-domain-id', '101');
      window.localStorage.setItem(
        'configurator.assembly-draft.v1.101',
        '{"version":1,"updatedAt":"2026-08-23T12:00:00.000Z","items":[{"componentId":101,"componentTypeId":11}]}'
      );
    `,
  });
  await page.route(frontendApiBaseUrl + '/domains/*/configurator/compatible*', async (route) => {
    if (route.request().method() !== 'GET') {
      await route.fallback();
      return;
    }
    const url = new URL(route.request().url());
    const includeTransitive = url.searchParams.get('includeTransitive') === 'true';
    await route.fulfill({
      json: {
        baseComponentId: 101,
        compatibleByType: includeTransitive
          ? [
              {
                componentTypeId: 12,
                componentTypeName: 'Материнская плата',
                components: [
                  {
                    id: 102,
                    name: 'B650 Tomahawk',
                    brand: 'MSI',
                    componentTypeId: 12,
                    explanations: [{ source: 'TRANSITIVE', pathComponentIds: [101, 103, 102] }],
                  },
                ],
              },
            ]
          : [],
      },
    });
  });
  await page.route(
    frontendApiBaseUrl + '/domains/*/configurator/compatible/search',
    async (route) => {
      const body = route.request().postDataJSON() as {
        componentIds: number[];
        includeTransitive: boolean;
      };
      await route.fulfill({
        json: {
          results: body.componentIds.map((baseComponentId) => ({
            baseComponentId,
            compatibleByType: body.includeTransitive
              ? [
                  {
                    componentTypeId: baseComponentId === 101 ? 12 : 11,
                    componentTypeName: baseComponentId === 101 ? 'Материнская плата' : 'Процессор',
                    components: [
                      {
                        id: baseComponentId === 101 ? 102 : 101,
                        name: baseComponentId === 101 ? 'B650 Tomahawk' : 'Ryzen 7 7800X3D',
                        componentTypeId: baseComponentId === 101 ? 12 : 11,
                        explanations: [
                          {
                            source: 'TRANSITIVE',
                            pathComponentIds:
                              baseComponentId === 101 ? [101, 103, 102] : [102, 103, 101],
                          },
                        ],
                      },
                    ],
                  },
                ]
              : [],
          })),
        },
      });
    },
  );
  await page.route(frontendApiBaseUrl + '/domains/*/configurator/candidates', async (route) => {
    const body = route.request().postDataJSON() as { componentIds: number[] };
    const hasTransitiveOnlyPair =
      body.componentIds.includes(101) && body.componentIds.includes(102);
    await route.fulfill({
      json: {
        componentIds: body.componentIds,
        assemblyStatus: hasTransitiveOnlyPair ? 'DISCONNECTED' : 'VALID',
        assemblyDecisions: hasTransitiveOnlyPair
          ? [
              {
                leftComponentId: 101,
                rightComponentId: 102,
                status: 'UNKNOWN',
                explanations: [],
                blockingRules: [],
              },
            ]
          : [],
        candidatesByType: [],
      },
    });
  });

  await page.goto('/configurator');

  const mode = page.getByRole('switch', { name: /Учитывать транзитивную совместимость/ });
  await expect(mode).not.toBeChecked();
  await mode.check();
  const browser = page.getByRole('region', { name: 'Доступные компоненты' });
  await expect(browser.getByText('B650 Tomahawk')).toBeVisible();
  await expect(browser.getByText('Транзитивная совместимость')).toBeVisible();

  await browser.getByRole('button', { name: 'Почему совместим' }).click();
  const explanation = page.getByRole('dialog', { name: 'Почему совместим «B650 Tomahawk»' });
  await expect(explanation.getByText('Radeon RX 7900 XTX')).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(explanation).toBeHidden();

  await browser.getByRole('button', { name: 'Добавить' }).click();
  const assembly = page.getByRole('region', { name: 'Текущая сборка' });
  await expect(assembly.getByText('В сборке есть конфликт')).toBeVisible();
  await expect(assembly.getByRole('button', { name: 'Сохранить конфигурацию' })).toBeDisabled();
  await assembly.getByRole('button', { name: 'Показать проверку' }).click();
  await expect(page.getByRole('dialog', { name: 'Проверка текущей сборки' })).toBeVisible();
  await page.keyboard.press('Escape');

  await mode.uncheck();
  await expect(assembly.getByText('В сборке есть конфликт')).toBeVisible();
  await expect(browser.getByText('Подбор временно недоступен')).toHaveCount(0);
});

test('keeps a conflicting draft and repairs it with a slot-aware replacement', async ({ page }) => {
  await page.addInitScript({
    content: `
      window.localStorage.setItem('configurator.selected-domain-id', '101');
      window.localStorage.setItem(
        'configurator.assembly-draft.v1.101',
        '{"version":1,"updatedAt":"2026-08-23T12:00:00.000Z","items":[{"componentId":101,"componentTypeId":11},{"componentId":102,"componentTypeId":12}]}'
      );
    `,
  });
  await page.route(
    frontendApiBaseUrl + '/domains/*/configurator/compatible/search',
    async (route) => {
      const body = route.request().postDataJSON() as { componentIds: number[] };
      if (body.componentIds.includes(101)) {
        await route.fulfill({
          json: {
            results: body.componentIds.map((baseComponentId) => ({
              baseComponentId,
              compatibleByType: [],
            })),
          },
        });
        return;
      }
      await route.fallback();
    },
  );
  await page.route(frontendApiBaseUrl + '/domains/*/configurator/candidates', async (route) => {
    const body = route.request().postDataJSON() as { componentIds: number[] };
    const hasBlockedPair = body.componentIds.includes(101) && body.componentIds.includes(102);
    const hasRepairedPair = body.componentIds.includes(104) && body.componentIds.includes(102);
    const compatibilityByBase = body.componentIds.map((baseComponentId) => ({
      baseComponentId,
      status: baseComponentId === 102 ? 'ALLOWED' : 'UNKNOWN',
      explanations: baseComponentId === 102 ? [{ source: 'MANUAL', linkId: 106 }] : [],
      blockingRules: [],
    }));
    await route.fulfill({
      json: {
        componentIds: body.componentIds,
        assemblyStatus: hasBlockedPair ? 'BLOCKED' : 'VALID',
        assemblyDecisions: hasBlockedPair
          ? [
              {
                leftComponentId: 101,
                rightComponentId: 102,
                status: 'DENIED',
                explanations: [],
                blockingRules: [{ ruleSetId: 77, ruleSetName: 'Несовместимый сокет' }],
              },
            ]
          : hasRepairedPair
            ? [
                {
                  leftComponentId: 104,
                  rightComponentId: 102,
                  status: 'ALLOWED',
                  explanations: [{ source: 'MANUAL', linkId: 106 }],
                  blockingRules: [],
                },
              ]
            : [],
        candidatesByType: body.componentIds.includes(104)
          ? []
          : [
              {
                componentTypeId: 11,
                componentTypeName: 'Процессор',
                components: [
                  {
                    id: 104,
                    name: 'Core Ultra 9 285K',
                    brand: 'Intel',
                    componentTypeId: 11,
                    status: compatibilityByBase.some((decision) => decision.status === 'ALLOWED')
                      ? 'AVAILABLE'
                      : 'UNRELATED',
                    compatibilityByBase,
                  },
                ],
              },
            ],
      },
    });
  });

  await page.goto('/configurator');

  const assembly = page.getByRole('region', { name: 'Текущая сборка' });
  await expect(assembly.getByText('В сборке есть конфликт')).toBeVisible();
  await expect(assembly.getByText('Конфликт', { exact: true })).toHaveCount(2);
  await expect(page.getByRole('region', { name: 'Доступные компоненты' })).toBeVisible();

  const replacementRequest = page.waitForRequest((request) => {
    const url = new URL(request.url());
    return url.pathname.endsWith('/configurator/candidates') && request.method() === 'POST';
  });
  await assembly.getByRole('button', { name: 'Заменить Ryzen 7 7800X3D' }).click();
  await replacementRequest;
  const replacementBrowser = page.getByRole('region', { name: 'Выбор замены' });
  await replacementBrowser.getByRole('button', { name: 'Выбрать' }).click();
  await page
    .getByRole('dialog', { name: 'Заменить компонент этого типа?' })
    .getByRole('button', { name: 'Заменить' })
    .click();

  await expect(assembly.getByText('Core Ultra 9 285K')).toBeVisible();
  await expect(assembly.getByText('Сборка корректна')).toBeVisible();
  await expect(assembly.getByText('Ryzen 7 7800X3D')).toHaveCount(0);
});
