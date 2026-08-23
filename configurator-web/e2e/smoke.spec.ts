import { expect, test, type Route } from '@playwright/test';
import { readFile } from 'node:fs/promises';

import type {
  CompatibilityRuleSet,
  GraphResponse,
  SaveCompatibilityRuleSetRequest,
} from '../src/shared/api/generated/types.gen';

const domainPage = {
  items: [
    {
      id: 101,
      name: 'Сборка ПК',
      description: 'Тестовая предметная область',
      createdAt: '2026-08-09T12:00:00Z',
    },
    {
      id: 202,
      name: 'Рабочая станция',
      description: 'Вторая тестовая предметная область',
      createdAt: '2026-08-10T12:00:00Z',
    },
  ],
  page: 0,
  size: 100,
  totalItems: 2,
};

const componentTypes = [
  {
    id: 11,
    domainId: 101,
    name: 'Процессор',
    code: 'CPU',
    description: 'Центральный процессор',
    orderIndex: 1,
  },
  {
    id: 12,
    domainId: 101,
    name: 'Материнская плата',
    code: 'MOTHERBOARD',
    description: 'Системная плата',
    orderIndex: 2,
  },
];

const attributesByType = {
  11: [
    {
      id: 1011,
      componentTypeId: 11,
      name: 'cores',
      label: 'Количество ядер',
      dataType: 'NUMBER',
      isRequired: true,
      orderIndex: 1,
    },
  ],
  12: [
    {
      id: 2011,
      componentTypeId: 12,
      name: 'pcie_lanes',
      label: 'Линии PCIe',
      dataType: 'NUMBER',
      isRequired: false,
      orderIndex: 1,
    },
  ],
} as const;

const componentPage = {
  items: [
    {
      id: 101,
      componentTypeId: 11,
      name: 'Ryzen 7 7800X3D',
      brand: 'AMD',
      archived: false,
      createdAt: '2026-08-09T12:00:00Z',
      attributes: [
        {
          attributeDefinitionId: 1011,
          name: 'cores',
          label: 'Количество ядер',
          dataType: 'NUMBER',
          value: '8',
        },
      ],
    },
    {
      id: 104,
      componentTypeId: 11,
      name: 'Core Ultra 9 285K',
      brand: 'Intel',
      archived: false,
      createdAt: '2026-08-10T12:00:00Z',
      attributes: [],
    },
    {
      id: 102,
      componentTypeId: 12,
      name: 'B650 Tomahawk',
      brand: 'MSI',
      archived: false,
      createdAt: '2026-08-11T12:00:00Z',
      attributes: [],
    },
  ],
  page: 0,
  size: 12,
  totalItems: 3,
};

function directlyCompatibleComponents(baseComponentId: number) {
  const base = componentPage.items.find((component) => component.id === baseComponentId);
  if (!base) {
    return [];
  }
  return componentPage.items.filter(
    (component) =>
      component.id !== baseComponentId && component.componentTypeId !== base.componentTypeId,
  );
}

function configuratorResponse(baseComponentId: number) {
  const groups = new Map<number, typeof componentPage.items>();
  for (const component of directlyCompatibleComponents(baseComponentId)) {
    groups.set(component.componentTypeId, [
      ...(groups.get(component.componentTypeId) ?? []),
      component,
    ]);
  }
  return {
    baseComponentId,
    compatibleByType: [...groups].map(([componentTypeId, components]) => ({
      componentTypeId,
      componentTypeName:
        componentTypes.find((componentType) => componentType.id === componentTypeId)?.name ??
        'Unknown',
      components: components.map((component) => ({
        id: component.id,
        name: component.name,
        brand: component.brand,
        componentTypeId,
        explanations: [{ source: 'MANUAL' as const, linkId: component.id + baseComponentId }],
      })),
    })),
  };
}

const frontendApiBaseUrl = 'http://127.0.0.1:5173/api';

test.beforeEach(async ({ page }) => {
  let configurations: Array<{
    id: number;
    domainId: number;
    name: string;
    description?: string;
    createdAt: string;
    components: Array<{
      id: number;
      name: string;
      brand?: string;
      componentTypeId: number;
      componentTypeName: string;
      archived: boolean;
    }>;
  }> = [];
  let nextConfigurationId = 901;
  let componentImages = [
    { id: 9001, url: '/component-images/9001/content', orderIndex: 0 },
    { id: 9002, url: '/component-images/9002/content', orderIndex: 1 },
  ];
  const compatibilityGraph: GraphResponse = {
    nodes: [
      {
        id: 101,
        name: 'Ryzen 7 7800X3D',
        componentTypeId: 11,
        componentTypeName: 'Процессор',
        brand: 'AMD',
      },
      {
        id: 102,
        name: 'B650 Tomahawk',
        componentTypeId: 12,
        componentTypeName: 'Материнская плата',
        brand: 'MSI',
      },
      {
        id: 103,
        name: 'Radeon RX 7900 XTX',
        componentTypeId: 13,
        componentTypeName: 'Видеокарта',
        brand: 'AMD',
      },
    ],
    edges: [{ id: 301, source: 101, target: 102, comment: 'Сокет AM5' }],
  };
  let nextRuleId = 502;
  let compatibilityRules: CompatibilityRuleSet[] = [
    {
      id: 501,
      domainId: 101,
      name: 'Количество линий',
      componentTypeAId: 11,
      componentTypeBId: 12,
      enabled: true,
      conditions: [
        {
          id: 601,
          ruleSetId: 501,
          leftAttributeDefinitionId: 1011,
          operator: 'GTE',
          rightAttributeDefinitionId: 2011,
          orderIndex: 0,
          createdAt: '2026-08-23T12:00:00',
        },
      ],
      createdAt: '2026-08-23T12:00:00',
    },
  ];
  const ruleFromRequest = (
    id: number,
    body: SaveCompatibilityRuleSetRequest,
  ): CompatibilityRuleSet => ({
    id,
    domainId: 101,
    ...body,
    conditions: body.conditions.map((condition, index) => ({
      id: id * 10 + index,
      ruleSetId: id,
      leftAttributeDefinitionId: condition.leftAttributeDefinitionId,
      operator: condition.operator,
      rightAttributeDefinitionId: condition.rightAttributeDefinitionId,
      orderIndex: condition.orderIndex ?? index,
      createdAt: '2026-08-23T12:00:00',
    })),
    createdAt: '2026-08-23T12:00:00',
  });
  await page.route(frontendApiBaseUrl + '/domains*', async (route) => {
    await route.fulfill({ json: domainPage });
  });
  await page.route(frontendApiBaseUrl + '/domains/*/component-types', async (route) => {
    await route.fulfill({ json: componentTypes });
  });
  await page.route(frontendApiBaseUrl + '/component-types/*/attributes', async (route) => {
    const typeId = Number(new URL(route.request().url()).pathname.split('/').at(-2));
    await route.fulfill({
      json: attributesByType[typeId as keyof typeof attributesByType] ?? [],
    });
  });
  await page.route(frontendApiBaseUrl + '/domains/*/components*', async (route) => {
    const url = new URL(route.request().url());
    const componentTypeValue = url.searchParams.get('componentTypeId');
    const componentTypeId = componentTypeValue === null ? null : Number(componentTypeValue);
    const name = url.searchParams.get('name')?.trim().toLocaleLowerCase() ?? '';
    const items = componentPage.items.filter(
      (component) =>
        (componentTypeId === null || component.componentTypeId === componentTypeId) &&
        (!name || component.name.toLocaleLowerCase().includes(name)),
    );
    await route.fulfill({
      json: {
        ...componentPage,
        items,
        totalItems: items.length,
      },
    });
  });
  await page.route(frontendApiBaseUrl + '/domains/*/configurations*', async (route) => {
    const request = route.request();
    if (request.method() === 'POST') {
      const body = request.postDataJSON() as {
        name: string;
        description?: string;
        componentIds: number[];
      };
      const created = {
        id: nextConfigurationId++,
        domainId: 101,
        name: body.name,
        ...(body.description ? { description: body.description } : {}),
        createdAt: '2026-08-23T12:00:00Z',
        components: body.componentIds.flatMap((componentId) => {
          const component = componentPage.items.find((item) => item.id === componentId);
          if (!component) {
            return [];
          }
          return [
            {
              id: component.id,
              name: component.name,
              brand: component.brand,
              componentTypeId: component.componentTypeId,
              componentTypeName:
                componentTypes.find((type) => type.id === component.componentTypeId)?.name ??
                'Unknown',
              archived: false,
            },
          ];
        }),
      };
      configurations = [created, ...configurations];
      await route.fulfill({ status: 201, json: created });
      return;
    }
    const url = new URL(request.url());
    const pageNumber = Number(url.searchParams.get('page') ?? 0);
    const size = Number(url.searchParams.get('size') ?? 10);
    await route.fulfill({
      json: {
        items: configurations.slice(pageNumber * size, pageNumber * size + size),
        page: pageNumber,
        size,
        totalItems: configurations.length,
      },
    });
  });
  await page.route(frontendApiBaseUrl + '/configurations/*/export/json', async (route) => {
    const pathSegments = new URL(route.request().url()).pathname.split('/');
    const configurationsSegment = pathSegments.indexOf('configurations');
    const configurationId = Number(pathSegments[configurationsSegment + 1]);
    const configuration = configurations.find((item) => item.id === configurationId);
    if (!configuration) {
      await route.fulfill({ status: 404, json: { message: 'Configuration not found' } });
      return;
    }
    await route.fulfill({
      json: {
        schemaVersion: 1,
        exportedAt: '2026-08-23T12:30:00Z',
        configuration,
      },
    });
  });
  await page.route(frontendApiBaseUrl + '/configurations/*', async (route) => {
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    const pathSegments = pathname.split('/');
    const configurationsSegment = pathSegments.indexOf('configurations');
    const configurationId = Number(pathSegments[configurationsSegment + 1]);
    const index = configurations.findIndex((configuration) => configuration.id === configurationId);
    if (index < 0) {
      await route.fulfill({
        status: 404,
        json: {
          timestamp: '2026-08-23T12:00:00Z',
          status: 404,
          error: 'Not Found',
          code: 'NOT_FOUND',
          message: 'Configuration not found',
          path: `/configurations/${configurationId}`,
          details: [],
        },
      });
      return;
    }
    if (request.method() === 'GET' && pathname.endsWith('/export/json')) {
      await route.fulfill({
        json: {
          schemaVersion: 1,
          exportedAt: '2026-08-23T12:30:00Z',
          configuration: configurations[index],
        },
      });
      return;
    }
    if (request.method() === 'GET') {
      await route.fulfill({ json: configurations[index] });
      return;
    }
    if (request.method() === 'DELETE') {
      configurations.splice(index, 1);
      await route.fulfill({ status: 204, body: '' });
      return;
    }
    if (request.method() === 'PUT') {
      const body = request.postDataJSON() as {
        name: string;
        description?: string;
        componentIds: number[];
      };
      const current = configurations[index];
      const updated = {
        ...current,
        name: body.name,
        ...(body.description ? { description: body.description } : {}),
        components: body.componentIds.flatMap((componentId) => {
          const component = componentPage.items.find((item) => item.id === componentId);
          if (!component) return [];
          return [
            {
              id: component.id,
              name: component.name,
              brand: component.brand,
              componentTypeId: component.componentTypeId,
              componentTypeName:
                componentTypes.find((type) => type.id === component.componentTypeId)?.name ??
                'Unknown',
              archived: false,
            },
          ];
        }),
      };
      if (!body.description) delete updated.description;
      configurations[index] = updated;
      await route.fulfill({ json: updated });
      return;
    }
    await route.fallback();
  });
  await page.route(frontendApiBaseUrl + '/domains/*/configurator/compatible*', async (route) => {
    if (route.request().method() !== 'GET') {
      await route.fallback();
      return;
    }
    const componentId = Number(new URL(route.request().url()).searchParams.get('componentId'));
    await route.fulfill({ json: configuratorResponse(componentId) });
  });
  await page.route(
    frontendApiBaseUrl + '/domains/*/configurator/compatible/search',
    async (route) => {
      const body = route.request().postDataJSON() as { componentIds: number[] };
      await route.fulfill({
        json: { results: body.componentIds.map(configuratorResponse) },
      });
    },
  );
  await page.route(
    frontendApiBaseUrl + '/domains/*/configurator/compatible/intersection',
    async (route) => {
      const body = route.request().postDataJSON() as { componentIds: number[] };
      const candidates = componentPage.items.filter(
        (component) =>
          !body.componentIds.includes(component.id) &&
          body.componentIds.every((baseComponentId) =>
            directlyCompatibleComponents(baseComponentId).some(
              (candidate) => candidate.id === component.id,
            ),
          ),
      );
      const groups = new Map<number, typeof componentPage.items>();
      for (const component of candidates) {
        groups.set(component.componentTypeId, [
          ...(groups.get(component.componentTypeId) ?? []),
          component,
        ]);
      }
      await route.fulfill({
        json: {
          componentIds: body.componentIds,
          compatibleByType: [...groups].map(([componentTypeId, components]) => ({
            componentTypeId,
            componentTypeName:
              componentTypes.find((componentType) => componentType.id === componentTypeId)?.name ??
              'Unknown',
            components: components.map((component) => ({
              id: component.id,
              name: component.name,
              brand: component.brand,
              componentTypeId,
              compatibilityByBase: body.componentIds.map((baseComponentId) => ({
                baseComponentId,
                explanations: [{ source: 'MANUAL', linkId: component.id + baseComponentId }],
              })),
            })),
          })),
        },
      });
    },
  );
  await page.route(frontendApiBaseUrl + '/domains/*/compatibility/graph', async (route) => {
    await route.fulfill({ json: compatibilityGraph });
  });
  const handleCompatibilityRules = async (route: Route) => {
    const request = route.request();
    const method = request.method();
    const path = new URL(request.url()).pathname;
    const ruleId = Number(path.split('/').at(-1));
    const isCollection = path.endsWith('/rules');
    if (isCollection && method === 'GET') {
      await route.fulfill({ json: compatibilityRules });
      return;
    }
    if (isCollection && method === 'POST') {
      const body = request.postDataJSON() as SaveCompatibilityRuleSetRequest;
      const created = ruleFromRequest(nextRuleId++, body);
      compatibilityRules = [...compatibilityRules, created];
      await route.fulfill({ status: 201, json: created });
      return;
    }
    const index = compatibilityRules.findIndex((rule) => rule.id === ruleId);
    if (index < 0) {
      await route.fulfill({ status: 404, json: { message: 'Rule not found' } });
      return;
    }
    if (method === 'GET') {
      await route.fulfill({ json: compatibilityRules[index] });
      return;
    }
    if (method === 'PUT') {
      const body = request.postDataJSON() as SaveCompatibilityRuleSetRequest;
      const updated = ruleFromRequest(ruleId, body);
      compatibilityRules[index] = updated;
      await route.fulfill({ json: updated });
      return;
    }
    if (method === 'DELETE') {
      compatibilityRules.splice(index, 1);
      await route.fulfill({ status: 204 });
      return;
    }
    await route.fallback();
  };
  await page.route(frontendApiBaseUrl + '/domains/*/compatibility/rules', handleCompatibilityRules);
  await page.route(
    frontendApiBaseUrl + '/domains/*/compatibility/rules/*',
    handleCompatibilityRules,
  );
  await page.route(frontendApiBaseUrl + '/domains/*/compatibility', async (route) => {
    const body = route.request().postDataJSON() as {
      componentAId: number;
      componentBId: number;
      comment?: string;
    };
    const created = {
      id: 302,
      domainId: 101,
      componentAId: Math.min(body.componentAId, body.componentBId),
      componentBId: Math.max(body.componentAId, body.componentBId),
      ...(body.comment ? { comment: body.comment } : {}),
    };
    compatibilityGraph.edges.push({
      id: created.id,
      source: created.componentAId,
      target: created.componentBId,
      ...(created.comment ? { comment: created.comment } : {}),
    });
    await route.fulfill({ status: 201, json: created });
  });
  await page.route(frontendApiBaseUrl + '/domains/*/compatibility/*', async (route) => {
    if (route.request().method() !== 'DELETE') {
      await route.fallback();
      return;
    }
    const path = new URL(route.request().url()).pathname;
    const linkId = Number(path.split('/').at(-1));
    const index = compatibilityGraph.edges.findIndex((edge) => edge.id === linkId);
    if (index >= 0) {
      compatibilityGraph.edges.splice(index, 1);
    }
    await route.fulfill({ status: 204 });
  });
  await page.route(frontendApiBaseUrl + '/components/*', async (route) => {
    const componentId = Number(new URL(route.request().url()).pathname.split('/').at(-1));
    const component = componentPage.items.find((candidate) => candidate.id === componentId);
    await route.fulfill(
      component ? { json: component } : { status: 404, json: { message: 'Component not found' } },
    );
  });
  await page.route(frontendApiBaseUrl + '/components', async (route) => {
    if (route.request().method() !== 'POST') {
      await route.fallback();
      return;
    }
    const body = route.request().postDataJSON() as Record<string, unknown>;
    await route.fulfill({
      status: 201,
      json: { ...componentPage.items[0], ...body, id: 202 },
    });
  });
  await page.route(frontendApiBaseUrl + '/components/*/images/order', async (route) => {
    const body = route.request().postDataJSON() as { imageIds: Array<number> };
    componentImages = body.imageIds.map((id, orderIndex) => ({
      ...componentImages.find((image) => image.id === id)!,
      orderIndex,
    }));
    await route.fulfill({ json: componentImages });
  });
  await page.route(frontendApiBaseUrl + '/components/*/images', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({ json: componentImages });
      return;
    }
    const image = {
      id: 9003,
      url: '/component-images/9003/content',
      orderIndex: componentImages.length,
    };
    componentImages = [...componentImages, image];
    await route.fulfill({ status: 201, json: image });
  });
  await page.route(frontendApiBaseUrl + '/component-images/*/content', async (route) => {
    await route.fulfill({
      contentType: 'image/png',
      body: Buffer.from(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
        'base64',
      ),
    });
  });
  await page.route(frontendApiBaseUrl + '/component-images/*', async (route) => {
    const imageId = Number(new URL(route.request().url()).pathname.split('/').at(-1));
    componentImages = componentImages.filter((image) => image.id !== imageId);
    await route.fulfill({ status: 204 });
  });
});

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
  await expect(assembly.getByText('Сборка совместима напрямую')).toBeVisible();

  const replacementRequest = page.waitForRequest((request) => {
    const url = new URL(request.url());
    return (
      url.pathname.endsWith('/configurator/compatible') &&
      url.searchParams.get('componentId') === '102'
    );
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

test('saves the current assembly and shows it in the configurations list', async ({ page }) => {
  await page.goto('/configurator');

  const browser = page.getByRole('region', { name: 'Доступные компоненты' });
  const assembly = page.getByRole('region', { name: 'Текущая сборка' });
  await browser.getByRole('button', { name: 'Добавить' }).first().click();
  await expect(assembly.getByText('Сборка совместима напрямую')).toBeVisible();
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
    componentIds: [101],
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
  await composition.getByRole('button', { name: 'Заменить' }).click();
  const replacementBrowser = page.getByRole('region', { name: 'Выбор замены' });
  await expect(replacementBrowser.getByText('Core Ultra 9 285K')).toBeVisible();
  await replacementBrowser.getByRole('button', { name: 'Выбрать' }).click();
  await expect(composition.getByText('Core Ultra 9 285K')).toBeVisible();

  const updateRequest = page.waitForRequest(
    (request) =>
      request.method() === 'PUT' && new URL(request.url()).pathname.endsWith('/configurations/901'),
  );
  await page.getByRole('button', { name: 'Сохранить изменения' }).click();
  expect((await updateRequest).postDataJSON()).toEqual({
    name: 'Домашний ПК 2026',
    description: 'Тихая сборка',
    componentIds: [104],
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
    componentIds: [104],
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
  await expect(assembly.getByText('Сборка совместима только с учётом цепочек')).toBeVisible();
  await expect(assembly.getByText(/нельзя сохранить/)).toBeVisible();
  await expect(assembly.getByRole('button', { name: 'Сохранить конфигурацию' })).toBeDisabled();
  await assembly.getByRole('button', { name: 'Показать проверку' }).click();
  await expect(page.getByRole('dialog', { name: 'Проверка текущей сборки' })).toBeVisible();
  await page.keyboard.press('Escape');

  await mode.uncheck();
  await expect(assembly.getByText('В сборке есть конфликт')).toBeVisible();
  await expect(browser.getByText('Подбор временно недоступен')).toBeVisible();
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

  await page.goto('/configurator');

  const assembly = page.getByRole('region', { name: 'Текущая сборка' });
  await expect(assembly.getByText('В сборке есть конфликт')).toBeVisible();
  await expect(assembly.getByText('Конфликт', { exact: true })).toHaveCount(2);
  await expect(
    page
      .getByRole('region', { name: 'Доступные компоненты' })
      .getByText('Подбор временно недоступен'),
  ).toBeVisible();

  const replacementRequest = page.waitForRequest((request) => {
    const url = new URL(request.url());
    return (
      url.pathname.endsWith('/configurator/compatible') &&
      url.searchParams.get('componentId') === '102'
    );
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
  await expect(assembly.getByText('Сборка совместима напрямую')).toBeVisible();
  await expect(assembly.getByText('Ryzen 7 7800X3D')).toHaveCount(0);
});

test('keeps domain management usable on a phone viewport', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/settings/domain');

  await expect(page.getByRole('heading', { level: 1, name: 'Предметные области' })).toBeVisible();
  await expect(page.getByRole('heading', { level: 2, name: 'Сборка ПК' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Предметная область: Сборка ПК' })).toBeVisible();
});

test('shows types and attributes for the selected domain', async ({ page }) => {
  await page.goto('/settings/types');

  await expect(page.getByRole('heading', { level: 1, name: 'Типы и атрибуты' })).toBeVisible();
  await expect(page.getByRole('heading', { level: 2, name: 'Процессор' })).toBeVisible();
  await expect(page.getByText('Количество ядер')).toBeVisible();
  await expect(page.getByText('Обязательный')).toBeVisible();
});

test('shows the component catalog and replaces the table with a compact mobile list', async ({
  page,
}) => {
  await page.goto('/components');

  await expect(page.getByRole('heading', { level: 1, name: 'Компоненты' })).toBeVisible();
  await expect(page.getByRole('heading', { level: 2, name: 'Ryzen 7 7800X3D' })).toBeVisible();
  await page.getByText('Таблица', { exact: true }).click();
  await expect(page.getByTestId('desktop-component-table')).toBeVisible();

  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByTestId('desktop-component-table')).toBeHidden();
  await expect(page.getByTestId('mobile-component-list')).toBeVisible();
  await expect(
    page.getByTestId('mobile-component-list').getByText('Ryzen 7 7800X3D', { exact: true }),
  ).toBeVisible();
});

test('opens component details and creates a component with dynamic attributes', async ({
  page,
}) => {
  await page.goto('/components');
  await page.getByRole('link', { name: 'Ryzen 7 7800X3D' }).first().click();
  await expect(page).toHaveURL(/\/components\/101$/);
  await expect(page.getByRole('heading', { level: 2, name: 'Характеристики' })).toBeVisible();
  await expect(page.getByText('Количество ядер')).toBeVisible();

  await page.getByRole('link', { name: 'К каталогу' }).click();
  await page.getByRole('link', { name: 'Новый компонент' }).click();
  await page.getByRole('combobox', { name: 'Тип компонента' }).click();
  await page.getByRole('option', { name: 'Процессор' }).click();
  await page.getByRole('textbox', { name: 'Название' }).fill('Ryzen 9 9950X3D');
  await page.getByRole('textbox', { name: 'Количество ядер' }).fill('16');
  await page.getByRole('button', { name: 'Создать' }).click();

  await expect(page).toHaveURL(/\/components\/202$/);
  await expect(page.getByText('Компонент создан')).toBeVisible();
});

test('manages the component image gallery on desktop and mobile', async ({ page }) => {
  await page.goto('/components');
  await page.getByRole('link', { name: 'Ryzen 7 7800X3D' }).first().click();

  await expect(page.getByRole('heading', { level: 1, name: 'Ryzen 7 7800X3D' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Открыть изображение 1' })).toBeVisible();
  await page.locator('input[type="file"]').setInputFiles({
    name: 'component.png',
    mimeType: 'image/png',
    buffer: Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
      'base64',
    ),
  });
  await page.getByRole('button', { name: 'Загрузить' }).click();
  await expect(page.getByText('Изображение загружено')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Открыть изображение 3' })).toBeVisible();

  await page.getByRole('button', { name: 'Изменить порядок' }).click();
  await page.getByRole('button', { name: 'Переместить позже' }).first().click();
  await page.getByRole('button', { name: 'Сохранить порядок' }).click();
  await expect(page.getByText('Порядок изображений сохранён')).toBeVisible();

  await page.getByRole('button', { name: 'Удалить изображение 1' }).click();
  await page
    .getByRole('dialog', { name: 'Удалить изображение?' })
    .getByRole('button', {
      name: 'Удалить',
    })
    .click();
  await expect(page.getByText('Изображение удалено')).toBeVisible();

  await page.setViewportSize({ width: 390, height: 844 });
  const filePicker = page.getByLabel('Новое изображение');
  await expect(filePicker).toBeVisible();
  const filePickerBox = await filePicker.boundingBox();
  expect(filePickerBox).not.toBeNull();
  expect(filePickerBox!.x + filePickerBox!.width).toBeLessThanOrEqual(390);
});

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

  const beforeDrag = await processor.boundingBox();
  expect(beforeDrag).not.toBeNull();
  if (beforeDrag) {
    await page.mouse.move(
      beforeDrag.x + beforeDrag.width / 2,
      beforeDrag.y + beforeDrag.height / 2,
    );
    await page.mouse.down();
    await page.mouse.move(beforeDrag.x + beforeDrag.width / 2 + 70, beforeDrag.y + 45, {
      steps: 5,
    });
    await page.mouse.up();
    const afterDrag = await processor.boundingBox();
    expect(afterDrag).not.toBeNull();
    expect(Math.abs((afterDrag?.x ?? beforeDrag.x) - beforeDrag.x)).toBeGreaterThan(30);
  }

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
