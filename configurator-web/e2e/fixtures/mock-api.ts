import { expect, test as base, type Page, type Request, type Route } from '@playwright/test';

import type {
  AttributeDefinition,
  CompatibilityRuleSet,
  Component,
  ComponentType,
  ConfiguratorCandidatesResponse,
  Domain,
  GraphResponse,
  SaveCompatibilityRuleSetRequest,
} from '../../src/shared/api/generated/types.gen';

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

const componentPage: {
  items: Array<Component>;
  page: number;
  size: number;
  totalItems: number;
} = {
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

function directlyCompatibleComponents(
  baseComponentId: number,
  components: ReadonlyArray<Component> = componentPage.items,
) {
  const base = components.find((component) => component.id === baseComponentId);
  if (!base) {
    return [];
  }
  return components.filter(
    (component) =>
      component.id !== baseComponentId && component.componentTypeId !== base.componentTypeId,
  );
}

function configuratorResponse(
  baseComponentId: number,
  components: ReadonlyArray<Component> = componentPage.items,
  types: ReadonlyArray<ComponentType> = componentTypes,
) {
  const groups = new Map<number, Array<Component>>();
  for (const component of directlyCompatibleComponents(baseComponentId, components)) {
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
        types.find((componentType) => componentType.id === componentTypeId)?.name ?? 'Unknown',
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

function configuratorCandidatesResponse(
  componentIds: number[],
  components: ReadonlyArray<Component> = componentPage.items,
  types: ReadonlyArray<ComponentType> = componentTypes,
): ConfiguratorCandidatesResponse {
  const isAllowed = (leftId: number, rightId: number) =>
    directlyCompatibleComponents(leftId, components).some((component) => component.id === rightId);
  const assemblyDecisions = componentIds.flatMap((leftId, leftIndex) =>
    componentIds.slice(leftIndex + 1).map((rightId) => ({
      leftComponentId: leftId,
      rightComponentId: rightId,
      status: isAllowed(leftId, rightId) ? ('ALLOWED' as const) : ('UNKNOWN' as const),
      explanations: isAllowed(leftId, rightId)
        ? [{ source: 'MANUAL' as const, linkId: leftId + rightId }]
        : [],
      blockingRules: [],
    })),
  );
  const adjacency = new Map(componentIds.map((componentId) => [componentId, new Set<number>()]));
  for (const decision of assemblyDecisions) {
    if (decision.status !== 'ALLOWED') continue;
    adjacency.get(decision.leftComponentId)?.add(decision.rightComponentId);
    adjacency.get(decision.rightComponentId)?.add(decision.leftComponentId);
  }
  const visited = new Set<number>();
  const pending = componentIds.length > 0 ? [componentIds[0]] : [];
  while (pending.length > 0) {
    const componentId = pending.pop()!;
    if (visited.has(componentId)) continue;
    visited.add(componentId);
    pending.push(...(adjacency.get(componentId) ?? []));
  }

  const candidates = components
    .filter((component) => !componentIds.includes(component.id))
    .map((component) => {
      const compatibilityByBase = componentIds.map((baseComponentId) => ({
        baseComponentId,
        status: isAllowed(baseComponentId, component.id)
          ? ('ALLOWED' as const)
          : ('UNKNOWN' as const),
        explanations: isAllowed(baseComponentId, component.id)
          ? [{ source: 'MANUAL' as const, linkId: baseComponentId + component.id }]
          : [],
        blockingRules: [],
      }));
      return {
        id: component.id,
        name: component.name,
        brand: component.brand,
        componentTypeId: component.componentTypeId,
        status: compatibilityByBase.some((decision) => decision.status === 'ALLOWED')
          ? ('AVAILABLE' as const)
          : ('UNRELATED' as const),
        compatibilityByBase,
      };
    });
  const groups = new Map<number, typeof candidates>();
  for (const candidate of candidates) {
    groups.set(candidate.componentTypeId, [
      ...(groups.get(candidate.componentTypeId) ?? []),
      candidate,
    ]);
  }
  return {
    componentIds,
    assemblyStatus:
      componentIds.length < 2 || visited.size === componentIds.length ? 'VALID' : 'DISCONNECTED',
    assemblyDecisions,
    candidatesByType: [...groups].map(([componentTypeId, groupedCandidates]) => ({
      componentTypeId,
      componentTypeName:
        types.find((componentType) => componentType.id === componentTypeId)?.name ?? 'Unknown',
      components: groupedCandidates,
    })),
  };
}

export const frontendApiBaseUrl = 'http://127.0.0.1:4173/api';

export async function expectNoUnexpectedHorizontalOverflow(page: Page) {
  const metrics = await page.evaluate<{ viewportWidth: number; contentWidth: number }>(
    '({ viewportWidth: document.documentElement.clientWidth, contentWidth: document.documentElement.scrollWidth })',
  );
  expect(metrics.contentWidth).toBeLessThanOrEqual(metrics.viewportWidth + 1);
}

export async function expectVisibleButtonTargetsAtLeast24Pixels(page: Page) {
  const buttons = page.locator('main button:visible');
  for (let index = 0; index < (await buttons.count()); index += 1) {
    const bounds = await buttons.nth(index).boundingBox();
    expect(bounds).not.toBeNull();
    expect(bounds?.width ?? 0).toBeGreaterThanOrEqual(24);
    expect(bounds?.height ?? 0).toBeGreaterThanOrEqual(24);
  }
}

export async function waitForJsonRequest<T>(
  page: Page,
  predicate: (request: Request) => boolean,
): Promise<T> {
  const request = await page.waitForRequest(predicate);
  return request.postDataJSON() as T;
}

async function installMockApi(page: Page) {
  await page.route(/^https?:\/\/(?!127\.0\.0\.1:4173(?:\/|$)).+/, async (route) => {
    await route.abort('blockedbyclient');
  });
  await page.route(frontendApiBaseUrl + '/**', async (route) => {
    const request = route.request();
    await route.fulfill({
      status: 501,
      json: {
        timestamp: '2026-08-23T12:00:00Z',
        status: 501,
        error: 'Not Implemented',
        code: 'E2E_ROUTE_NOT_IMPLEMENTED',
        message: `Unhandled E2E API request: ${request.method()} ${new URL(request.url()).pathname}`,
        path: new URL(request.url()).pathname,
        details: [],
      },
    });
  });

  let domains: Array<Domain> = structuredClone(domainPage.items);
  let nextDomainId = 303;
  let componentTypeState: Array<ComponentType> = structuredClone(componentTypes);
  let nextComponentTypeId = 13;
  const attributeState = new Map<number, Array<AttributeDefinition>>(
    Object.entries(attributesByType).map(([componentTypeId, attributes]) => [
      Number(componentTypeId),
      attributes.map((attribute) => ({ ...attribute })),
    ]),
  );
  let nextAttributeId = 3001;
  const toAttributeValues = (
    inputs: ReadonlyArray<{ attributeDefinitionId: number; value: string }> = [],
  ) =>
    inputs.flatMap((input) => {
      const definition = [...attributeState.values()]
        .flat()
        .find((attribute) => attribute.id === input.attributeDefinitionId);
      return definition
        ? [
            {
              attributeDefinitionId: definition.id,
              name: definition.name,
              label: definition.label,
              dataType: definition.dataType,
              value: input.value,
            },
          ]
        : [];
    });
  let componentState: Array<Component> = structuredClone(componentPage.items);
  let nextComponentId = 202;
  let configurations: Array<{
    id: number;
    domainId: number;
    name: string;
    description?: string;
    createdAt: string;
    components: Array<{
      id: number;
      name: string;
      brand?: string | null;
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
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    const domainId = Number(pathname.split('/').at(-1));
    if (pathname === '/api/domains' && request.method() === 'GET') {
      await route.fulfill({
        json: { ...domainPage, items: domains, totalItems: domains.length },
      });
      return;
    }
    if (pathname === '/api/domains' && request.method() === 'POST') {
      const body = request.postDataJSON() as Pick<Domain, 'name' | 'description'>;
      const created: Domain = {
        id: nextDomainId++,
        name: body.name,
        ...(body.description ? { description: body.description } : {}),
        createdAt: '2026-08-23T12:00:00Z',
      };
      domains = [...domains, created];
      await route.fulfill({ status: 201, json: created });
      return;
    }
    if (pathname === '/api/domains/demo' && request.method() === 'POST') {
      const created: Domain = {
        id: nextDomainId++,
        name: 'Сборка ПК (демо)',
        description: 'Демонстрационная предметная область',
        createdAt: '2026-08-23T12:00:00Z',
      };
      domains = [...domains, created];
      await route.fulfill({ status: 201, json: created });
      return;
    }
    const index = domains.findIndex((domain) => domain.id === domainId);
    if (index >= 0 && request.method() === 'PUT') {
      const body = request.postDataJSON() as Pick<Domain, 'name' | 'description'>;
      const updated: Domain = {
        ...domains[index],
        name: body.name,
        ...(body.description ? { description: body.description } : {}),
      };
      if (!body.description) delete updated.description;
      domains[index] = updated;
      await route.fulfill({ json: updated });
      return;
    }
    if (index >= 0 && request.method() === 'DELETE') {
      domains.splice(index, 1);
      await route.fulfill({ status: 204, body: '' });
      return;
    }
    await route.fallback();
  });
  await page.route(frontendApiBaseUrl + '/domains/*', async (route) => {
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    const pathSegments = pathname.split('/').filter(Boolean);
    if (pathSegments.length !== 3) {
      await route.fallback();
      return;
    }
    const domainId = Number(pathSegments.at(-1));
    const index = domains.findIndex((domain) => domain.id === domainId);
    if (index >= 0 && request.method() === 'PUT') {
      const body = request.postDataJSON() as Pick<Domain, 'name' | 'description'>;
      const updated: Domain = {
        ...domains[index],
        name: body.name,
        ...(body.description ? { description: body.description } : {}),
      };
      if (!body.description) delete updated.description;
      domains[index] = updated;
      await route.fulfill({ json: updated });
      return;
    }
    if (index >= 0 && request.method() === 'DELETE') {
      domains.splice(index, 1);
      await route.fulfill({ status: 204, body: '' });
      return;
    }
    await route.fallback();
  });
  await page.route(frontendApiBaseUrl + '/domains/*/component-types', async (route) => {
    const request = route.request();
    const domainId = Number(new URL(request.url()).pathname.split('/').at(-2));
    if (request.method() === 'GET') {
      await route.fulfill({
        json: componentTypeState.filter((componentType) => componentType.domainId === domainId),
      });
      return;
    }
    if (request.method() === 'POST') {
      const body = request.postDataJSON() as Omit<ComponentType, 'id' | 'domainId'>;
      const created: ComponentType = { id: nextComponentTypeId++, domainId, ...body };
      componentTypeState = [...componentTypeState, created];
      attributeState.set(created.id, []);
      await route.fulfill({ status: 201, json: created });
      return;
    }
    await route.fallback();
  });
  await page.route(frontendApiBaseUrl + '/component-types/*/attributes', async (route) => {
    const request = route.request();
    const typeId = Number(new URL(route.request().url()).pathname.split('/').at(-2));
    if (request.method() === 'GET') {
      await route.fulfill({ json: attributeState.get(typeId) ?? [] });
      return;
    }
    if (request.method() === 'POST') {
      const body = request.postDataJSON() as Omit<AttributeDefinition, 'id' | 'componentTypeId'>;
      const created: AttributeDefinition = {
        id: nextAttributeId++,
        componentTypeId: typeId,
        ...body,
      };
      attributeState.set(typeId, [...(attributeState.get(typeId) ?? []), created]);
      await route.fulfill({ status: 201, json: created });
      return;
    }
    await route.fallback();
  });
  await page.route(frontendApiBaseUrl + '/component-types/*', async (route) => {
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    if (pathname.endsWith('/attributes')) {
      await route.fallback();
      return;
    }
    const componentTypeId = Number(pathname.split('/').at(-1));
    const index = componentTypeState.findIndex(
      (componentType) => componentType.id === componentTypeId,
    );
    if (index >= 0 && request.method() === 'PUT') {
      const body = request.postDataJSON() as Omit<ComponentType, 'id' | 'domainId'>;
      const updated: ComponentType = { ...componentTypeState[index], ...body };
      componentTypeState[index] = updated;
      await route.fulfill({ json: updated });
      return;
    }
    if (index >= 0 && request.method() === 'DELETE') {
      componentTypeState.splice(index, 1);
      attributeState.delete(componentTypeId);
      await route.fulfill({ status: 204, body: '' });
      return;
    }
    await route.fallback();
  });
  await page.route(frontendApiBaseUrl + '/attributes/*', async (route) => {
    const request = route.request();
    const attributeId = Number(new URL(request.url()).pathname.split('/').at(-1));
    const entry = [...attributeState].find(([, attributes]) =>
      attributes.some((attribute) => attribute.id === attributeId),
    );
    const index = entry?.[1].findIndex((attribute) => attribute.id === attributeId) ?? -1;
    if (entry && index >= 0 && request.method() === 'PUT') {
      const body = request.postDataJSON() as Omit<AttributeDefinition, 'id' | 'componentTypeId'>;
      const updated: AttributeDefinition = { ...entry[1][index], ...body };
      entry[1][index] = updated;
      await route.fulfill({ json: updated });
      return;
    }
    await route.fallback();
  });
  await page.route(frontendApiBaseUrl + '/domains/*/components*', async (route) => {
    const url = new URL(route.request().url());
    const componentTypeValue = url.searchParams.get('componentTypeId');
    const componentTypeId = componentTypeValue === null ? null : Number(componentTypeValue);
    const name = url.searchParams.get('name')?.trim().toLocaleLowerCase() ?? '';
    const archived = url.searchParams.get('archived') === 'true';
    const items = componentState.filter(
      (component) =>
        component.archived === archived &&
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
          const component = componentState.find((item) => item.id === componentId);
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
                componentTypeState.find((type) => type.id === component.componentTypeId)?.name ??
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
          const component = componentState.find((item) => item.id === componentId);
          if (!component) return [];
          return [
            {
              id: component.id,
              name: component.name,
              brand: component.brand,
              componentTypeId: component.componentTypeId,
              componentTypeName:
                componentTypeState.find((type) => type.id === component.componentTypeId)?.name ??
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
    await route.fulfill({
      json: configuratorResponse(componentId, componentState, componentTypeState),
    });
  });
  await page.route(frontendApiBaseUrl + '/domains/*/configurator/candidates', async (route) => {
    const body = route.request().postDataJSON() as { componentIds: number[] };
    await route.fulfill({
      json: configuratorCandidatesResponse(body.componentIds, componentState, componentTypeState),
    });
  });
  await page.route(
    frontendApiBaseUrl + '/domains/*/configurator/compatible/search',
    async (route) => {
      const body = route.request().postDataJSON() as { componentIds: number[] };
      await route.fulfill({
        json: {
          results: body.componentIds.map((componentId) =>
            configuratorResponse(componentId, componentState, componentTypeState),
          ),
        },
      });
    },
  );
  await page.route(
    frontendApiBaseUrl + '/domains/*/configurator/compatible/intersection',
    async (route) => {
      const body = route.request().postDataJSON() as { componentIds: number[] };
      const candidates = componentState.filter(
        (component) =>
          !body.componentIds.includes(component.id) &&
          body.componentIds.every((baseComponentId) =>
            directlyCompatibleComponents(baseComponentId, componentState).some(
              (candidate) => candidate.id === component.id,
            ),
          ),
      );
      const groups = new Map<number, Array<Component>>();
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
              componentTypeState.find((componentType) => componentType.id === componentTypeId)
                ?.name ?? 'Unknown',
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
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    const segments = pathname.split('/');
    const componentIndex = segments.indexOf('components');
    const componentId = Number(segments[componentIndex + 1]);
    const index = componentState.findIndex((candidate) => candidate.id === componentId);
    if (index < 0) {
      await route.fulfill({ status: 404, json: { message: 'Component not found' } });
      return;
    }
    if (pathname.endsWith('/restore') && request.method() === 'POST') {
      const restored: Component = { ...componentState[index], archived: false };
      componentState[index] = restored;
      await route.fulfill({ json: restored });
      return;
    }
    if (request.method() === 'GET') {
      await route.fulfill({ json: componentState[index] });
      return;
    }
    if (request.method() === 'PUT') {
      const body = request.postDataJSON() as {
        componentTypeId: number;
        name: string;
        brand?: string | null;
        description?: string;
        attributes: Array<{ attributeDefinitionId: number; value: string }>;
      };
      const updated: Component = {
        ...componentState[index],
        ...body,
        id: componentId,
        attributes: toAttributeValues(body.attributes),
      };
      componentState[index] = updated;
      await route.fulfill({ json: updated });
      return;
    }
    if (request.method() === 'DELETE') {
      componentState[index] = { ...componentState[index], archived: true };
      await route.fulfill({ status: 204, body: '' });
      return;
    }
    await route.fallback();
  });
  await page.route(frontendApiBaseUrl + '/components/*/restore', async (route) => {
    const request = route.request();
    const componentId = Number(new URL(request.url()).pathname.split('/').at(-2));
    const index = componentState.findIndex((component) => component.id === componentId);
    if (index >= 0 && request.method() === 'POST') {
      const restored: Component = { ...componentState[index], archived: false };
      componentState[index] = restored;
      await route.fulfill({ json: restored });
      return;
    }
    await route.fallback();
  });
  await page.route(frontendApiBaseUrl + '/components', async (route) => {
    if (route.request().method() !== 'POST') {
      await route.fallback();
      return;
    }
    const body = route.request().postDataJSON() as {
      componentTypeId: number;
      name: string;
      brand?: string | null;
      description?: string;
      attributes?: Array<{ attributeDefinitionId: number; value: string }>;
    };
    const created: Component = {
      ...componentState[0],
      ...body,
      id: nextComponentId++,
      archived: false,
      createdAt: '2026-08-23T12:00:00Z',
      attributes: toAttributeValues(body.attributes),
    };
    componentState = [...componentState, created];
    await route.fulfill({ status: 201, json: created });
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
}

interface MockApiFixtures {
  mockApi: void;
}

function isBrowserResourceError(message: string) {
  return (
    message.startsWith('Failed to load resource:') ||
    message.startsWith('[JavaScript Error: "Cross-Origin Request Blocked:')
  );
}

export const test = base.extend<MockApiFixtures>({
  mockApi: [
    async ({ page }, use) => {
      const browserErrors: Array<string> = [];
      page.on('pageerror', (error) => browserErrors.push(error.stack ?? error.message));
      page.on('console', (message) => {
        if (message.type() === 'error' && !isBrowserResourceError(message.text())) {
          browserErrors.push(message.text());
        }
      });
      await installMockApi(page);
      await use();
      await page.close();
      expect(browserErrors, 'browser console/page errors').toEqual([]);
    },
    { auto: true },
  ],
});

export { expect };
