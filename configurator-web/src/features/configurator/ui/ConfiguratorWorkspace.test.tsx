import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { createMemoryRouter } from 'react-router-dom';

import { App } from '@/app/App';
import { appRoutes } from '@/app/router/routes';
import type { Component, ComponentType, ConfiguratorCandidatesResponse } from '@/shared/api';
import { configuratorDraftStorageKey } from '@/shared/config/preferences';
import { server, testApiBaseUrl } from '@/test/server';

const domainId = 101;
const componentTypes: ComponentType[] = [
  { id: 11, domainId, name: 'Процессор', orderIndex: 1 },
  { id: 12, domainId, name: 'Видеокарта', orderIndex: 2 },
  { id: 13, domainId, name: 'Материнская плата', orderIndex: 3 },
];
const ryzen: Component = {
  id: 101,
  componentTypeId: 11,
  name: 'Ryzen 7 7800X3D',
  brand: 'AMD',
  archived: false,
  createdAt: '2026-08-01T12:00:00Z',
};
const intel: Component = {
  id: 102,
  componentTypeId: 11,
  name: 'Core Ultra 9',
  brand: 'Intel',
  archived: false,
  createdAt: '2026-08-02T12:00:00Z',
};
const radeon: Component = {
  id: 103,
  componentTypeId: 12,
  name: 'Radeon RX 7900 XTX',
  brand: 'AMD',
  archived: false,
  createdAt: '2026-08-03T12:00:00Z',
};
const motherboard: Component = {
  id: 104,
  componentTypeId: 13,
  name: 'B650 Tomahawk',
  brand: 'MSI',
  archived: false,
  createdAt: '2026-08-04T12:00:00Z',
};
const components = [ryzen, intel, radeon, motherboard];

function groupComponentsByType<T extends Component>(entries: ReadonlyArray<T>) {
  const groups = new Map<number, T[]>();
  for (const component of entries) {
    groups.set(component.componentTypeId, [
      ...(groups.get(component.componentTypeId) ?? []),
      component,
    ]);
  }
  return groups;
}

function compatibleComponents(baseComponentId: number) {
  return components.filter(
    (component) =>
      component.id !== baseComponentId &&
      !(baseComponentId === ryzen.id && component.id === intel.id) &&
      !(baseComponentId === intel.id && component.id === ryzen.id),
  );
}

function toCompatibilityResponse(baseComponentId: number) {
  const groups = groupComponentsByType(compatibleComponents(baseComponentId));
  return {
    baseComponentId,
    compatibleByType: [...groups].map(([componentTypeId, candidates]) => ({
      componentTypeId,
      componentTypeName:
        componentTypes.find((type) => type.id === componentTypeId)?.name ?? 'Unknown',
      components: candidates.map((component) => ({
        id: component.id,
        name: component.name,
        brand: component.brand,
        componentTypeId: component.componentTypeId,
        explanations: [{ source: 'MANUAL' as const, linkId: component.id + baseComponentId }],
      })),
    })),
  };
}

function toAssemblyCandidatesResponse(componentIds: number[]): ConfiguratorCandidatesResponse {
  const pairAllowed = (leftId: number, rightId: number) =>
    compatibleComponents(leftId).some((candidate) => candidate.id === rightId);
  const assemblyDecisions = componentIds.flatMap((leftId, leftIndex) =>
    componentIds.slice(leftIndex + 1).map((rightId) => ({
      leftComponentId: leftId,
      rightComponentId: rightId,
      status: pairAllowed(leftId, rightId) ? ('ALLOWED' as const) : ('UNKNOWN' as const),
      explanations: pairAllowed(leftId, rightId)
        ? [{ source: 'MANUAL' as const, linkId: leftId + rightId }]
        : [],
      blockingRules: [],
    })),
  );
  const candidates = components
    .filter((component) => !componentIds.includes(component.id))
    .map((component) => {
      const compatibilityByBase = componentIds.map((baseComponentId) => ({
        baseComponentId,
        status: pairAllowed(baseComponentId, component.id)
          ? ('ALLOWED' as const)
          : ('UNKNOWN' as const),
        explanations: pairAllowed(baseComponentId, component.id)
          ? [{ source: 'MANUAL' as const, linkId: baseComponentId + component.id }]
          : [],
        blockingRules: [],
      }));
      return {
        ...component,
        status: compatibilityByBase.some((decision) => decision.status === 'ALLOWED')
          ? ('AVAILABLE' as const)
          : ('UNRELATED' as const),
        compatibilityByBase,
      };
    });
  const groups = groupComponentsByType(candidates);
  const connected =
    componentIds.length < 2 || assemblyDecisions.every((decision) => decision.status === 'ALLOWED');
  return {
    componentIds,
    assemblyStatus: connected ? ('VALID' as const) : ('DISCONNECTED' as const),
    assemblyDecisions,
    candidatesByType: [...groups].map(([componentTypeId, entries]) => ({
      componentTypeId,
      componentTypeName:
        componentTypes.find((type) => type.id === componentTypeId)?.name ?? 'Unknown',
      components: entries,
    })),
  };
}

function useHandlers() {
  server.use(
    http.get(`${testApiBaseUrl}/domains/:domainId/component-types`, () =>
      HttpResponse.json(componentTypes),
    ),
    http.get(`${testApiBaseUrl}/domains/:domainId/components`, ({ request }) => {
      const url = new URL(request.url);
      const typeId = Number(url.searchParams.get('componentTypeId')) || undefined;
      const name = url.searchParams.get('name')?.toLocaleLowerCase() ?? '';
      const filtered = components.filter(
        (component) =>
          (typeId === undefined || component.componentTypeId === typeId) &&
          component.name.toLocaleLowerCase().includes(name),
      );
      return HttpResponse.json({
        items: filtered,
        page: 0,
        size: 12,
        totalItems: filtered.length,
      });
    }),
    http.get(`${testApiBaseUrl}/components/:id`, ({ params }) => {
      const component = components.find((candidate) => candidate.id === Number(params['id']));
      return component
        ? HttpResponse.json(component)
        : HttpResponse.json({ message: 'Not found' }, { status: 404 });
    }),
    http.get(`${testApiBaseUrl}/domains/:domainId/compatibility/graph`, () =>
      HttpResponse.json({
        nodes: components.map((component) => ({
          id: component.id,
          name: component.name,
          brand: component.brand,
          componentTypeId: component.componentTypeId,
          componentTypeName:
            componentTypes.find((type) => type.id === component.componentTypeId)?.name ?? 'Unknown',
        })),
        edges: [],
      }),
    ),
    http.get(`${testApiBaseUrl}/domains/:domainId/configurator/compatible`, ({ request }) => {
      const componentId = Number(new URL(request.url).searchParams.get('componentId'));
      return HttpResponse.json(toCompatibilityResponse(componentId));
    }),
    http.post(
      `${testApiBaseUrl}/domains/:domainId/configurator/compatible/search`,
      async ({ request }) => {
        const body = (await request.json()) as { componentIds: number[] };
        return HttpResponse.json({
          results: body.componentIds.map(toCompatibilityResponse),
        });
      },
    ),
    http.post(
      `${testApiBaseUrl}/domains/:domainId/configurator/compatible/intersection`,
      async ({ request }) => {
        const body = (await request.json()) as { componentIds: number[] };
        const candidates = components.filter(
          (component) =>
            !body.componentIds.includes(component.id) &&
            body.componentIds.every((baseId) =>
              compatibleComponents(baseId).some((candidate) => candidate.id === component.id),
            ),
        );
        const groups = groupComponentsByType(candidates);
        return HttpResponse.json({
          componentIds: body.componentIds,
          compatibleByType: [...groups].map(([componentTypeId, entries]) => ({
            componentTypeId,
            componentTypeName:
              componentTypes.find((type) => type.id === componentTypeId)?.name ?? 'Unknown',
            components: entries.map((component) => ({
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
        });
      },
    ),
    http.post(
      `${testApiBaseUrl}/domains/:domainId/configurator/candidates`,
      async ({ request }) => {
        const body = (await request.json()) as { componentIds: number[] };
        return HttpResponse.json(toAssemblyCandidatesResponse(body.componentIds));
      },
    ),
  );
}

function renderPage() {
  const router = createMemoryRouter(appRoutes, { initialEntries: ['/configurator'] });
  return render(<App router={router} />);
}

afterEach(() => window.localStorage.clear());

describe('configurator workspace', () => {
  it('saves a single-component assembly, clears its draft and opens the list', async () => {
    const user = userEvent.setup();
    let requestBody: unknown;
    useHandlers();
    server.use(
      http.post(`${testApiBaseUrl}/domains/${domainId}/configurations`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          {
            id: 501,
            domainId,
            name: 'Домашний ПК',
            description: 'Тихая сборка',
            createdAt: '2026-08-23T12:00:00Z',
            components: [
              {
                id: ryzen.id,
                name: ryzen.name,
                brand: ryzen.brand,
                componentTypeId: ryzen.componentTypeId,
                componentTypeName: 'Процессор',
                archived: false,
              },
            ],
          },
          { status: 201 },
        );
      }),
      http.get(`${testApiBaseUrl}/domains/${domainId}/configurations`, () =>
        HttpResponse.json({
          items: [
            {
              id: 501,
              domainId,
              name: 'Домашний ПК',
              description: 'Тихая сборка',
              createdAt: '2026-08-23T12:00:00Z',
              components: [],
            },
          ],
          page: 0,
          size: 10,
          totalItems: 1,
        }),
      ),
    );
    renderPage();

    const browser = await screen.findByRole(
      'region',
      { name: 'Доступные компоненты' },
      { timeout: 5000 },
    );
    await user.click(
      (await within(browser).findAllByRole('button', { name: 'Добавить' }, { timeout: 5000 }))[0]!,
    );
    const assembly = screen.getByRole('region', { name: 'Текущая сборка' });
    await within(assembly).findByText('Сборка корректна', {}, { timeout: 5000 });
    await user.click(within(assembly).getByRole('button', { name: 'Сохранить конфигурацию' }));
    const dialog = await screen.findByRole('dialog', { name: 'Сохранение конфигурации' });
    await user.type(within(dialog).getByRole('textbox', { name: /Название/ }), '  Домашний ПК  ');
    await user.type(within(dialog).getByRole('textbox', { name: 'Описание' }), ' Тихая сборка ');
    await user.click(within(dialog).getByRole('button', { name: 'Сохранить конфигурацию' }));

    expect(
      await screen.findByRole('heading', { name: 'Конфигурации', level: 1 }, { timeout: 5000 }),
    ).toBeInTheDocument();
    expect(
      await screen.findByRole('heading', { name: 'Домашний ПК' }, { timeout: 5000 }),
    ).toBeInTheDocument();
    expect(requestBody).toEqual({
      name: 'Домашний ПК',
      description: 'Тихая сборка',
      componentIds: [ryzen.id],
    });
    expect(
      JSON.parse(window.localStorage.getItem(configuratorDraftStorageKey(domainId)) ?? ''),
    ).toMatchObject({ version: 1, items: [] });
  });

  it('adds direct and intersected candidates, explicitly replaces and clears the draft', async () => {
    const user = userEvent.setup();
    useHandlers();
    renderPage();

    let browser = await screen.findByRole('region', { name: 'Доступные компоненты' });
    const assembly = screen.getByRole('region', { name: 'Текущая сборка' });
    expect(
      within(assembly).getByRole('heading', { name: 'Сборка пока пуста' }),
    ).toBeInTheDocument();

    const addButtons = await within(browser).findAllByRole('button', { name: 'Добавить' });
    await user.click(addButtons[0]!);
    expect(await within(assembly).findByText(ryzen.name)).toBeInTheDocument();
    expect(
      JSON.parse(window.localStorage.getItem(configuratorDraftStorageKey(domainId)) ?? ''),
    ).toMatchObject({
      version: 1,
      items: [{ componentId: ryzen.id, componentTypeId: ryzen.componentTypeId }],
    });

    expect(await within(assembly).findByText('Сборка корректна')).toBeInTheDocument();
    browser = screen.getByRole('region', { name: 'Доступные компоненты' });
    expect(await within(browser).findByText(radeon.name)).toBeInTheDocument();
    await user.click(within(browser).getAllByRole('button', { name: 'Добавить' })[0]!);
    expect(await within(assembly).findByText(radeon.name)).toBeInTheDocument();
    browser = screen.getByRole('region', { name: 'Доступные компоненты' });
    expect(await within(browser).findByText(motherboard.name)).toBeInTheDocument();
    await user.click(within(browser).getByRole('button', { name: 'Добавить' }));
    expect(await within(assembly).findByText(motherboard.name)).toBeInTheDocument();

    await user.click(within(assembly).getByRole('button', { name: `Заменить ${ryzen.name}` }));
    const replacementBrowser = screen.getByRole('region', { name: 'Выбор замены' });
    expect(
      await within(replacementBrowser).findByRole('heading', { name: 'Выбор замены' }),
    ).toBeInTheDocument();
    expect(await within(replacementBrowser).findByText(intel.name)).toBeInTheDocument();
    await user.click(
      within(replacementBrowser).getByRole('button', { name: `Выбрать ${intel.name}` }),
    );
    const replaceDialog = await screen.findByRole('dialog', {
      name: 'Заменить компонент этого типа?',
    });
    expect(within(replaceDialog).getByText(/Ryzen 7 7800X3D/)).toBeInTheDocument();
    await user.click(within(replaceDialog).getByRole('button', { name: 'Заменить' }));
    await waitFor(() => expect(replaceDialog).not.toBeInTheDocument());
    expect(await within(assembly).findByText(intel.name)).toBeInTheDocument();
    expect(within(assembly).queryByText(ryzen.name)).not.toBeInTheDocument();

    await user.click(
      within(assembly).getByRole('button', { name: `Убрать ${intel.name} из сборки` }),
    );
    await waitFor(() => expect(within(assembly).queryByText(intel.name)).not.toBeInTheDocument());

    await user.click(within(assembly).getByRole('button', { name: 'Очистить' }));
    const clearDialog = await screen.findByRole('dialog', { name: 'Очистить текущую сборку?' });
    await user.click(within(clearDialog).getByRole('button', { name: 'Очистить' }));
    await waitFor(() => expect(clearDialog).not.toBeInTheDocument());
    expect(
      await within(assembly).findByRole('heading', { name: 'Сборка пока пуста' }),
    ).toBeInTheDocument();
  });

  it('keeps blocked candidates out of suggestions and explains the blocking rule', async () => {
    const user = userEvent.setup();
    useHandlers();
    server.use(
      http.post(
        `${testApiBaseUrl}/domains/:domainId/configurator/candidates`,
        async ({ request }) => {
          const body = (await request.json()) as { componentIds: number[] };
          const response = toAssemblyCandidatesResponse(body.componentIds);
          if (body.componentIds.length === 1 && body.componentIds[0] === ryzen.id) {
            const board = response.candidatesByType
              .flatMap((group) => group.components)
              .find((component) => component.id === motherboard.id);
            if (board) {
              board.status = 'BLOCKED';
              board.compatibilityByBase = [
                {
                  baseComponentId: ryzen.id,
                  status: 'DENIED',
                  explanations: [],
                  blockingRules: [{ ruleSetId: 77, ruleSetName: 'Недостаточная мощность' }],
                },
              ];
            }
          }
          return HttpResponse.json(response);
        },
      ),
    );
    renderPage();

    let browser = await screen.findByRole('region', { name: 'Доступные компоненты' });
    await user.click((await within(browser).findAllByRole('button', { name: 'Добавить' }))[0]!);
    browser = screen.getByRole('region', { name: 'Доступные компоненты' });
    const unavailableControl = await within(browser).findByRole('button', {
      name: 'Недоступные варианты: 1',
    });
    const collapsedCandidate = within(browser).queryByText(motherboard.name);
    if (collapsedCandidate) expect(collapsedCandidate).not.toBeVisible();

    await user.click(unavailableControl);
    const blockedCandidate = await within(browser).findByText(motherboard.name);
    const blockedCard = blockedCandidate.closest('[data-with-border="true"]');
    expect(blockedCard).not.toBeNull();
    expect(
      within(blockedCard as HTMLElement).queryByRole('button', { name: 'Добавить' }),
    ).not.toBeInTheDocument();
    expect(within(browser).getByText(/Недостаточная мощность/)).toBeInTheDocument();
    expect(within(browser).getByText('Заблокирован')).toBeInTheDocument();
  });

  it('restores the ordered domain draft and keeps an unavailable item visible', async () => {
    window.localStorage.setItem(
      configuratorDraftStorageKey(domainId),
      JSON.stringify({
        version: 1,
        updatedAt: '2026-08-23T12:00:00.000Z',
        items: [
          { componentId: radeon.id, componentTypeId: radeon.componentTypeId },
          { componentId: 999, componentTypeId: 11 },
        ],
      }),
    );
    useHandlers();
    renderPage();

    expect(
      await screen.findByText('Локальный черновик восстановлен: 2 компонента.'),
    ).toBeInTheDocument();
    const assembly = screen.getByRole('region', { name: 'Текущая сборка' });
    expect(await within(assembly).findByText(radeon.name)).toBeInTheDocument();
    expect(
      await within(assembly).findByText(/Компонент #999 не удалось загрузить/),
    ).toBeInTheDocument();
    expect(within(assembly).getByRole('button', { name: 'Повторить' })).toBeInTheDocument();
    expect(within(assembly).getByRole('button', { name: 'Убрать' })).toBeInTheDocument();
  });

  it('keeps an incompatible restored draft and resolves it through slot-aware replacement', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem(
      configuratorDraftStorageKey(domainId),
      JSON.stringify({
        version: 1,
        updatedAt: '2026-08-23T12:00:00.000Z',
        items: [
          { componentId: ryzen.id, componentTypeId: ryzen.componentTypeId },
          { componentId: radeon.id, componentTypeId: radeon.componentTypeId },
        ],
      }),
    );
    useHandlers();
    server.use(
      http.post(
        `${testApiBaseUrl}/domains/:domainId/configurator/candidates`,
        async ({ request }) => {
          const body = (await request.json()) as { componentIds: number[] };
          const response = toAssemblyCandidatesResponse(body.componentIds);
          if (body.componentIds.includes(ryzen.id) && body.componentIds.includes(radeon.id)) {
            return HttpResponse.json({
              ...response,
              assemblyStatus: 'BLOCKED',
              assemblyDecisions: [
                {
                  leftComponentId: ryzen.id,
                  rightComponentId: radeon.id,
                  status: 'DENIED',
                  explanations: [],
                  blockingRules: [{ ruleSetId: 77, ruleSetName: 'Недостаточная мощность' }],
                },
              ],
            });
          }
          return HttpResponse.json(response);
        },
      ),
    );
    renderPage();

    const assembly = await screen.findByRole('region', { name: 'Текущая сборка' });
    const browser = screen.getByRole('region', { name: 'Доступные компоненты' });
    expect(await within(assembly).findByText('В сборке есть конфликт')).toBeInTheDocument();
    expect(within(assembly).getAllByText('Конфликт')).toHaveLength(2);
    expect(within(browser).queryByText('Подбор временно недоступен')).not.toBeInTheDocument();

    await user.click(within(assembly).getByRole('button', { name: `Заменить ${ryzen.name}` }));
    const replacementBrowser = screen.getByRole('region', { name: 'Выбор замены' });
    expect(await within(replacementBrowser).findByText(intel.name)).toBeInTheDocument();
    await user.click(
      within(replacementBrowser).getByRole('button', { name: `Выбрать ${intel.name}` }),
    );
    await user.click(
      within(
        await screen.findByRole('dialog', { name: 'Заменить компонент этого типа?' }),
      ).getByRole('button', { name: 'Заменить' }),
    );

    expect(await within(assembly).findByText(intel.name)).toBeInTheDocument();
    expect(await within(assembly).findByText('Сборка корректна')).toBeInTheDocument();
    expect(within(assembly).queryByText(ryzen.name)).not.toBeInTheDocument();
  });

  it('keeps an archived draft item visible and safely resets corrupted storage', async () => {
    const archivedRyzen = { ...ryzen, archived: true };
    window.localStorage.setItem(
      configuratorDraftStorageKey(domainId),
      JSON.stringify({
        version: 1,
        updatedAt: '2026-08-23T12:00:00.000Z',
        items: [{ componentId: ryzen.id, componentTypeId: ryzen.componentTypeId }],
      }),
    );
    useHandlers();
    server.use(
      http.get(`${testApiBaseUrl}/components/${ryzen.id}`, () => HttpResponse.json(archivedRyzen)),
    );
    const firstRender = renderPage();

    const assembly = await screen.findByRole('region', { name: 'Текущая сборка' });
    expect(await within(assembly).findByText('В архиве')).toBeInTheDocument();
    expect(within(assembly).getByText(ryzen.name)).toBeInTheDocument();
    firstRender.unmount();

    window.localStorage.setItem(configuratorDraftStorageKey(domainId), 'not-json');
    renderPage();
    expect(await screen.findByText('Повреждённый черновик сброшен')).toBeInTheDocument();
    expect(
      within(screen.getByRole('region', { name: 'Текущая сборка' })).getByRole('heading', {
        name: 'Сборка пока пуста',
      }),
    ).toBeInTheDocument();
  });

  it('continues to edit the in-memory assembly when local storage writes fail', async () => {
    const user = userEvent.setup();
    const originalSetItem = window.localStorage.setItem.bind(window.localStorage);
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
      if (key === configuratorDraftStorageKey(domainId)) {
        throw new DOMException('Quota exceeded');
      }
      originalSetItem(key, value);
    });
    try {
      useHandlers();
      renderPage();
      const browser = await screen.findByRole('region', { name: 'Доступные компоненты' });
      const addButtons = await within(browser).findAllByRole('button', { name: 'Добавить' });
      await user.click(addButtons[0]!);

      expect(await screen.findByText('Черновик не сохраняется')).toBeInTheDocument();
      expect(
        within(screen.getByRole('region', { name: 'Текущая сборка' })).getByText(ryzen.name),
      ).toBeInTheDocument();
    } finally {
      setItem.mockRestore();
    }
  });

  it('shows direct evidence and keeps transitive-only additions invalid for saving', async () => {
    const user = userEvent.setup();
    useHandlers();
    server.use(
      http.get(`${testApiBaseUrl}/domains/:domainId/configurator/compatible`, ({ request }) => {
        const url = new URL(request.url);
        const includeTransitive = url.searchParams.get('includeTransitive') === 'true';
        return HttpResponse.json({
          baseComponentId: ryzen.id,
          compatibleByType: [
            {
              componentTypeId: radeon.componentTypeId,
              componentTypeName: 'Видеокарта',
              components: [
                {
                  id: radeon.id,
                  name: radeon.name,
                  brand: radeon.brand,
                  componentTypeId: radeon.componentTypeId,
                  explanations: [
                    { source: 'MANUAL', linkId: 501, comment: 'Проверено производителем' },
                    {
                      source: 'AUTOMATIC',
                      ruleSetId: 601,
                      ruleSetName: 'Совпадающий интерфейс',
                      conditions: [
                        {
                          leftAttributeDefinitionId: 11,
                          leftAttributeName: 'Интерфейс',
                          leftValue: 'PCIe 4.0',
                          operator: 'EQUALS',
                          rightAttributeDefinitionId: 12,
                          rightAttributeName: 'Интерфейс',
                          rightValue: 'PCIe 4.0',
                        },
                      ],
                    },
                  ],
                },
              ],
            },
            ...(includeTransitive
              ? [
                  {
                    componentTypeId: motherboard.componentTypeId,
                    componentTypeName: 'Материнская плата',
                    components: [
                      {
                        id: motherboard.id,
                        name: motherboard.name,
                        brand: motherboard.brand,
                        componentTypeId: motherboard.componentTypeId,
                        explanations: [
                          {
                            source: 'TRANSITIVE',
                            pathComponentIds: [ryzen.id, radeon.id, motherboard.id],
                          },
                        ],
                      },
                    ],
                  },
                ]
              : []),
          ],
        });
      }),
      http.post(
        `${testApiBaseUrl}/domains/:domainId/configurator/candidates`,
        async ({ request }) => {
          const body = (await request.json()) as { componentIds: number[] };
          const response = toAssemblyCandidatesResponse(body.componentIds);
          if (body.componentIds.length === 1 && body.componentIds[0] === ryzen.id) {
            const gpuGroup = response.candidatesByType.find(
              (group) => group.componentTypeId === radeon.componentTypeId,
            );
            const gpu = gpuGroup?.components.find((component) => component.id === radeon.id);
            if (gpu) {
              gpu.compatibilityByBase[0]!.explanations = [
                { source: 'MANUAL', linkId: 501, comment: 'Проверено производителем' },
                {
                  source: 'AUTOMATIC',
                  ruleSetId: 601,
                  ruleSetName: 'Совпадающий интерфейс',
                  conditions: [
                    {
                      leftAttributeDefinitionId: 11,
                      leftAttributeName: 'Интерфейс',
                      leftValue: 'PCIe 4.0',
                      operator: 'EQUALS',
                      rightAttributeDefinitionId: 12,
                      rightAttributeName: 'Интерфейс',
                      rightValue: 'PCIe 4.0',
                    },
                  ],
                },
              ];
            }
          }
          if (body.componentIds.includes(ryzen.id) && body.componentIds.includes(motherboard.id)) {
            return HttpResponse.json({
              ...response,
              assemblyStatus: 'DISCONNECTED',
              assemblyDecisions: [
                {
                  leftComponentId: ryzen.id,
                  rightComponentId: motherboard.id,
                  status: 'UNKNOWN',
                  explanations: [],
                  blockingRules: [],
                },
              ],
            });
          }
          return HttpResponse.json(response);
        },
      ),
    );
    renderPage();

    let browser = await screen.findByRole('region', { name: 'Доступные компоненты' });
    await user.click((await within(browser).findAllByRole('button', { name: 'Добавить' }))[0]!);
    browser = screen.getByRole('region', { name: 'Доступные компоненты' });

    await user.click(
      (await within(browser).findAllByRole('button', { name: 'Почему совместим' }))[0]!,
    );
    let drawer = await screen.findByRole('dialog', { name: `Почему совместим «${radeon.name}»` });
    expect(within(drawer).getByText('Проверено производителем')).toBeInTheDocument();
    expect(within(drawer).getByText('Совпадающий интерфейс')).toBeInTheDocument();
    expect(within(drawer).getByText(/Интерфейс: PCIe 4.0/)).toBeInTheDocument();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(drawer).not.toBeInTheDocument());

    await user.click(screen.getByRole('switch', { name: /^Учитывать транзитивную совместимость/ }));
    browser = screen.getByRole('region', { name: 'Доступные компоненты' });
    expect(await within(browser).findByText(motherboard.name)).toBeInTheDocument();
    expect(within(browser).getByText('Транзитивная совместимость')).toBeInTheDocument();
    await user.click(within(browser).getAllByRole('button', { name: 'Почему совместим' }).at(-1)!);
    drawer = await screen.findByRole('dialog', {
      name: `Почему совместим «${motherboard.name}»`,
    });
    expect(await within(drawer).findByText(radeon.name)).toBeInTheDocument();
    expect(within(drawer).getByText(motherboard.name)).toBeInTheDocument();
    await user.keyboard('{Escape}');

    browser = screen.getByRole('region', { name: 'Доступные компоненты' });
    await user.click(within(browser).getAllByRole('button', { name: 'Добавить' }).at(-1)!);
    const assembly = screen.getByRole('region', { name: 'Текущая сборка' });
    expect(await within(assembly).findByText('В сборке есть конфликт')).toBeInTheDocument();

    await user.click(within(assembly).getByRole('button', { name: 'Показать проверку' }));
    expect(
      await screen.findByRole('dialog', { name: 'Проверка текущей сборки' }),
    ).toBeInTheDocument();
    await user.keyboard('{Escape}');

    await user.click(screen.getByRole('switch', { name: /^Учитывать транзитивную совместимость/ }));
    expect(await within(assembly).findByText('В сборке есть конфликт')).toBeInTheDocument();
  });
});
