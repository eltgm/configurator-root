import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { App } from '@/app/App';
import { appRoutes } from '@/app/router/routes';
import type { ComponentType, Configuration, ConfiguratorCandidatesResponse } from '@/shared/api';
import { configuratorDraftStorageKey, selectedDomainStorageKey } from '@/shared/config/preferences';
import { downloadTextFile } from '@/shared/lib/download';
import { server, testApiBaseUrl } from '@/test/server';

vi.mock('@/shared/lib/download', () => ({ downloadTextFile: vi.fn() }));

const componentTypes: ComponentType[] = [
  { id: 11, domainId: 101, name: 'Процессор', orderIndex: 10 },
  { id: 12, domainId: 101, name: 'Видеокарта', orderIndex: 20 },
  { id: 13, domainId: 101, name: 'Оперативная память', orderIndex: 30 },
];

const configuration: Configuration = {
  id: 91,
  domainId: 101,
  name: 'Рабочая станция',
  description: 'Тихая сборка',
  createdAt: '2026-08-23T10:00:00Z',
  components: [
    {
      id: 7,
      name: 'Ryzen 9',
      brand: 'AMD',
      componentTypeId: 11,
      componentTypeName: 'Процессор',
      archived: false,
    },
    {
      id: 8,
      name: 'RTX 5090',
      componentTypeId: 12,
      componentTypeName: 'Видеокарта',
      archived: false,
    },
  ],
};

function validAssembly(componentIds = [7, 8]): ConfiguratorCandidatesResponse {
  const assemblyDecisions = componentIds.flatMap((leftComponentId, leftIndex) =>
    componentIds.slice(leftIndex + 1).map((rightComponentId) => ({
      leftComponentId,
      rightComponentId,
      status: 'ALLOWED' as const,
      explanations: [{ source: 'MANUAL' as const, linkId: leftComponentId + rightComponentId }],
      blockingRules: [],
    })),
  );
  return {
    componentIds,
    assemblyStatus: 'VALID',
    assemblyDecisions,
    candidatesByType: [],
  };
}

function useConfigurationHandlers(value: Configuration = configuration) {
  server.use(
    http.get(`${testApiBaseUrl}/configurations/${value.id}`, () => HttpResponse.json(value)),
    http.get(`${testApiBaseUrl}/domains/101/component-types`, () =>
      HttpResponse.json(componentTypes),
    ),
    http.post(`${testApiBaseUrl}/domains/101/configurator/candidates`, async ({ request }) => {
      const body = (await request.json()) as { componentIds: number[] };
      return HttpResponse.json(validAssembly(body.componentIds));
    }),
    http.post(`${testApiBaseUrl}/domains/101/configurator/compatible/intersection`, () =>
      HttpResponse.json({ componentIds: [7, 8], compatibleByType: [] }),
    ),
    http.get(`${testApiBaseUrl}/domains/101/configurator/compatible`, ({ request }) => {
      const baseComponentId = Number(new URL(request.url).searchParams.get('componentId'));
      return HttpResponse.json({ baseComponentId, compatibleByType: [] });
    }),
  );
}

function renderAt(path: string) {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] });
  return { router, ...render(<App router={router} />) };
}

function findConfigurationHeading(name: string) {
  return screen.findByRole('heading', { level: 1, name }, { timeout: 10_000 });
}

afterEach(() => {
  window.localStorage.removeItem(selectedDomainStorageKey);
  window.localStorage.removeItem(configuratorDraftStorageKey(101));
  vi.mocked(downloadTextFile).mockClear();
});

describe('configuration details and editor', () => {
  it('shows current metadata, ordered composition and archived state', async () => {
    useConfigurationHandlers({
      ...configuration,
      components: [
        configuration.components[0]!,
        { ...configuration.components[1]!, archived: true },
      ],
    });
    renderAt('/configurations/91');

    expect(await findConfigurationHeading('Рабочая станция')).toBeInTheDocument();
    expect(screen.getByText('Тихая сборка')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ryzen 9' })).toHaveAttribute('href', '/components/7');
    expect(screen.getByText('В архиве')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Редактировать' })).toHaveAttribute(
      'href',
      '/configurations/91/edit',
    );
    expect(screen.getByRole('button', { name: 'Копировать' })).toBeDisabled();
    expect(
      screen.getByText('Сначала удалите или замените архивные компоненты в редакторе.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Скачать JSON' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Удалить' })).toBeEnabled();
  });

  it('shows a safe not-found state for an invalid or missing configuration', async () => {
    const first = renderAt('/configurations/not-a-number');
    expect(await screen.findByText('Конфигурация не найдена')).toBeInTheDocument();
    first.unmount();

    server.use(
      http.get(`${testApiBaseUrl}/configurations/404`, () =>
        HttpResponse.json(
          {
            timestamp: '2026-08-23T12:00:00Z',
            status: 404,
            error: 'Not Found',
            code: 'NOT_FOUND',
            message: 'Not found',
            path: '/configurations/404',
            details: [],
          },
          { status: 404 },
        ),
      ),
    );
    renderAt('/configurations/404');
    expect(await screen.findByText('Конфигурация не найдена')).toBeInTheDocument();
  });

  it('creates an independent copy from immutable metadata and composition', async () => {
    const user = userEvent.setup();
    const draftKey = configuratorDraftStorageKey(101);
    const localDraft = JSON.stringify({ version: 1, componentIds: [777] });
    window.localStorage.setItem(draftKey, localDraft);
    let requestBody: unknown;
    useConfigurationHandlers();
    server.use(
      http.post(`${testApiBaseUrl}/domains/101/configurations`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          { ...configuration, id: 92, name: 'Рабочая станция — копия' },
          { status: 201 },
        );
      }),
    );
    const { router } = renderAt('/configurations/91');

    await findConfigurationHeading('Рабочая станция');
    await user.click(screen.getByRole('button', { name: 'Копировать' }));
    const dialog = await screen.findByRole('dialog', { name: 'Копирование конфигурации' });
    expect(within(dialog).getByRole('textbox', { name: /Название/ })).toHaveValue(
      'Рабочая станция — копия',
    );
    expect(within(dialog).getByRole('textbox', { name: 'Описание' })).toHaveValue('Тихая сборка');
    expect(within(dialog).getByText('Ryzen 9')).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: 'Создать копию' }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/configurations/92'));
    expect(requestBody).toEqual({
      name: 'Рабочая станция — копия',
      description: 'Тихая сборка',
      componentIds: [7, 8],
    });
    expect(window.localStorage.getItem(draftKey)).toBe(localDraft);
  });

  it('downloads the exact server export and keeps the detail screen open', async () => {
    const user = userEvent.setup();
    const exported = {
      schemaVersion: 1,
      exportedAt: '2026-08-23T12:00:00Z',
      configuration,
    };
    useConfigurationHandlers();
    server.use(
      http.get(`${testApiBaseUrl}/configurations/91/export/json`, () =>
        HttpResponse.json(exported),
      ),
    );
    const { router } = renderAt('/configurations/91');

    await findConfigurationHeading('Рабочая станция');
    await user.click(screen.getByRole('button', { name: 'Скачать JSON' }));

    await waitFor(() =>
      expect(downloadTextFile).toHaveBeenCalledWith({
        content: `${JSON.stringify(exported, null, 2)}\n`,
        fileName: 'configuration-91.json',
        mimeType: 'application/json;charset=utf-8',
      }),
    );
    expect(router.state.location.pathname).toBe('/configurations/91');
  });

  it('cancels and then confirms permanent deletion from details', async () => {
    const user = userEvent.setup();
    let deleteRequests = 0;
    useConfigurationHandlers();
    server.use(
      http.delete(`${testApiBaseUrl}/configurations/91`, () => {
        deleteRequests += 1;
        return new HttpResponse(null, { status: 204 });
      }),
      http.get(`${testApiBaseUrl}/domains/101/configurations`, () =>
        HttpResponse.json({ items: [], page: 0, size: 10, totalItems: 0 }),
      ),
    );
    const { router } = renderAt('/configurations/91');

    await findConfigurationHeading('Рабочая станция');
    await user.click(screen.getByRole('button', { name: 'Удалить' }));
    let dialog = await screen.findByRole('dialog', { name: 'Удалить конфигурацию?' });
    expect(within(dialog).getByText(/нельзя отменить/)).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: 'Отмена' }));
    expect(deleteRequests).toBe(0);
    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'Удалить конфигурацию?' })).toBeNull(),
    );

    await user.click(screen.getByRole('button', { name: 'Удалить' }));
    dialog = await screen.findByRole('dialog', { name: 'Удалить конфигурацию?' });
    await user.click(within(dialog).getByRole('button', { name: 'Удалить' }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/configurations'));
    expect(deleteRequests).toBe(1);
  });

  it('submits a complete strict update and returns to refreshed details', async () => {
    const user = userEvent.setup();
    let requestBody: unknown;
    useConfigurationHandlers();
    server.use(
      http.put(`${testApiBaseUrl}/configurations/91`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json({
          ...configuration,
          name: 'Новая станция',
          description: undefined,
        });
      }),
    );
    renderAt('/configurations/91/edit');

    const name = await screen.findByRole('textbox', { name: 'Название' });
    await user.clear(name);
    await user.type(name, '  Новая станция  ');
    await user.clear(screen.getByRole('textbox', { name: 'Описание' }));
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Сохранить изменения' })).toBeEnabled(),
    );
    await user.click(screen.getByRole('button', { name: 'Сохранить изменения' }));

    await findConfigurationHeading('Новая станция');
    expect(requestBody).toEqual({ name: 'Новая станция', componentIds: [7, 8] });
  });

  it('updates a connected assembly when some pairs are unknown', async () => {
    const user = userEvent.setup();
    let requestBody: unknown;
    const connectedConfiguration: Configuration = {
      ...configuration,
      components: [
        configuration.components[0]!,
        {
          ...configuration.components[1]!,
          name: 'B650',
          componentTypeName: 'Материнская плата',
        },
        {
          id: 9,
          name: 'DDR5',
          componentTypeId: 13,
          componentTypeName: 'Оперативная память',
          archived: false,
        },
      ],
    };
    useConfigurationHandlers(connectedConfiguration);
    server.use(
      http.post(`${testApiBaseUrl}/domains/101/configurator/candidates`, () =>
        HttpResponse.json<ConfiguratorCandidatesResponse>({
          componentIds: [7, 8, 9],
          assemblyStatus: 'VALID',
          assemblyDecisions: [
            {
              leftComponentId: 7,
              rightComponentId: 8,
              status: 'ALLOWED',
              explanations: [{ source: 'AUTOMATIC', ruleSetId: 71 }],
              blockingRules: [],
            },
            {
              leftComponentId: 7,
              rightComponentId: 9,
              status: 'UNKNOWN',
              explanations: [],
              blockingRules: [],
            },
            {
              leftComponentId: 8,
              rightComponentId: 9,
              status: 'ALLOWED',
              explanations: [{ source: 'MANUAL', linkId: 89 }],
              blockingRules: [],
            },
          ],
          candidatesByType: [],
        }),
      ),
      http.put(`${testApiBaseUrl}/configurations/91`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json({ ...connectedConfiguration, name: 'Связная сборка' });
      }),
    );
    renderAt('/configurations/91/edit');

    const name = await screen.findByRole('textbox', { name: 'Название' });
    await user.clear(name);
    await user.type(name, 'Связная сборка');
    expect(
      await screen.findByText('Компоненты образуют связную сборку без блокирующих правил.'),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Сохранить изменения' })).toBeEnabled(),
    );
    await user.click(screen.getByRole('button', { name: 'Сохранить изменения' }));

    await findConfigurationHeading('Связная сборка');
    expect(requestBody).toEqual({
      name: 'Связная сборка',
      description: 'Тихая сборка',
      componentIds: [7, 8, 9],
    });
  });

  it('allows adding a bridge component to repair a disconnected assembly', async () => {
    const user = userEvent.setup();
    let requestBody: unknown;
    useConfigurationHandlers();
    server.use(
      http.post(`${testApiBaseUrl}/domains/101/configurator/candidates`, async ({ request }) => {
        const { componentIds } = (await request.json()) as { componentIds: number[] };
        if (componentIds.length === 2) {
          return HttpResponse.json<ConfiguratorCandidatesResponse>({
            componentIds,
            assemblyStatus: 'DISCONNECTED',
            assemblyDecisions: [
              {
                leftComponentId: 7,
                rightComponentId: 8,
                status: 'UNKNOWN',
                explanations: [],
                blockingRules: [],
              },
            ],
            candidatesByType: [
              {
                componentTypeId: 13,
                componentTypeName: 'Оперативная память',
                components: [
                  {
                    id: 9,
                    name: 'DDR5 bridge',
                    componentTypeId: 13,
                    status: 'AVAILABLE',
                    compatibilityByBase: [
                      {
                        baseComponentId: 7,
                        status: 'ALLOWED',
                        explanations: [{ source: 'AUTOMATIC', ruleSetId: 79 }],
                        blockingRules: [],
                      },
                      {
                        baseComponentId: 8,
                        status: 'ALLOWED',
                        explanations: [{ source: 'MANUAL', linkId: 89 }],
                        blockingRules: [],
                      },
                    ],
                  },
                ],
              },
            ],
          });
        }
        return HttpResponse.json<ConfiguratorCandidatesResponse>({
          componentIds,
          assemblyStatus: 'VALID',
          assemblyDecisions: [
            {
              leftComponentId: 7,
              rightComponentId: 8,
              status: 'UNKNOWN',
              explanations: [],
              blockingRules: [],
            },
            {
              leftComponentId: 7,
              rightComponentId: 9,
              status: 'ALLOWED',
              explanations: [{ source: 'AUTOMATIC', ruleSetId: 79 }],
              blockingRules: [],
            },
            {
              leftComponentId: 8,
              rightComponentId: 9,
              status: 'ALLOWED',
              explanations: [{ source: 'MANUAL', linkId: 89 }],
              blockingRules: [],
            },
          ],
          candidatesByType: [],
        });
      }),
      http.put(`${testApiBaseUrl}/configurations/91`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json({
          ...configuration,
          components: [
            ...configuration.components,
            {
              id: 9,
              name: 'DDR5 bridge',
              componentTypeId: 13,
              componentTypeName: 'Оперативная память',
              archived: false,
            },
          ],
        });
      }),
    );
    renderAt('/configurations/91/edit');

    expect(await screen.findByText(/Состав не связан подтверждёнными связями/)).toBeInTheDocument();
    const browser = screen.getByRole('region', { name: 'Доступные компоненты' });
    const candidateName = await within(browser).findByText('DDR5 bridge');
    const candidateCard = candidateName.closest('[data-with-border="true"]');
    expect(candidateCard).not.toBeNull();
    await user.click(
      within(candidateCard as HTMLElement).getByRole('button', { name: 'Добавить' }),
    );

    expect(
      await screen.findByText('Компоненты образуют связную сборку без блокирующих правил.'),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Сохранить изменения' }));
    await findConfigurationHeading('Рабочая станция');
    expect(requestBody).toEqual({
      name: 'Рабочая станция',
      description: 'Тихая сборка',
      componentIds: [7, 8, 9],
    });
  });

  it('blocks DENIED pairs until a conflicting component is removed', async () => {
    const user = userEvent.setup();
    useConfigurationHandlers();
    server.use(
      http.post(`${testApiBaseUrl}/domains/101/configurator/candidates`, () =>
        HttpResponse.json<ConfiguratorCandidatesResponse>({
          componentIds: [7, 8],
          assemblyStatus: 'BLOCKED',
          assemblyDecisions: [
            {
              leftComponentId: 7,
              rightComponentId: 8,
              status: 'DENIED',
              explanations: [],
              blockingRules: [{ ruleSetId: 78, ruleSetName: 'Power limit' }],
            },
          ],
          candidatesByType: [],
        }),
      ),
    );
    renderAt('/configurations/91/edit');

    expect(await screen.findByText(/Состав нарушает автоматические правила/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Сохранить изменения' })).toBeDisabled();
    const composition = screen.getByRole('region', { name: 'Состав конфигурации' });
    const blockedRow = within(composition).getByText('RTX 5090').closest('.mantine-Paper-root');
    expect(blockedRow).not.toBeNull();
    await user.click(within(blockedRow as HTMLElement).getByRole('button', { name: 'Убрать' }));

    expect(
      await screen.findByText('Компоненты образуют связную сборку без блокирующих правил.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Сохранить изменения' })).toBeEnabled();
  });

  it('replaces a component with an active candidate of the same type', async () => {
    const user = userEvent.setup();
    let requestBody: unknown;
    const singleConfiguration = {
      ...configuration,
      components: [configuration.components[0]!],
    };
    useConfigurationHandlers(singleConfiguration);
    server.use(
      http.get(`${testApiBaseUrl}/domains/101/components`, ({ request }) => {
        expect(new URL(request.url).searchParams.get('componentTypeId')).toBe('11');
        return HttpResponse.json({
          items: [
            {
              id: 104,
              componentTypeId: 11,
              name: 'Core Ultra 9',
              brand: 'Intel',
              archived: false,
              createdAt: '2026-08-23T12:00:00Z',
              attributes: [],
            },
          ],
          page: 0,
          size: 12,
          totalItems: 1,
        });
      }),
      http.put(`${testApiBaseUrl}/configurations/91`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json({
          ...singleConfiguration,
          components: [
            {
              id: 104,
              name: 'Core Ultra 9',
              brand: 'Intel',
              componentTypeId: 11,
              componentTypeName: 'Процессор',
              archived: false,
            },
          ],
        });
      }),
    );
    renderAt('/configurations/91/edit');

    await screen.findByRole('heading', { name: 'Состав конфигурации' });
    await user.click(screen.getByRole('button', { name: 'Заменить' }));
    const browser = screen.getByRole('region', { name: 'Выбор замены' });
    expect(await within(browser).findByText('Core Ultra 9')).toBeInTheDocument();
    await user.click(within(browser).getByRole('button', { name: 'Выбрать Core Ultra 9' }));
    expect(screen.getByText('Core Ultra 9')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Сохранить изменения' }));

    await findConfigurationHeading('Рабочая станция');
    expect(requestBody).toEqual({
      name: 'Рабочая станция',
      description: 'Тихая сборка',
      componentIds: [104],
    });
  });

  it('binds structured backend details to metadata and preserves the draft', async () => {
    const user = userEvent.setup();
    const singleConfiguration = {
      ...configuration,
      components: [configuration.components[0]!],
    };
    useConfigurationHandlers(singleConfiguration);
    server.use(
      http.put(`${testApiBaseUrl}/configurations/91`, () =>
        HttpResponse.json(
          {
            timestamp: '2026-08-23T12:00:00Z',
            status: 400,
            error: 'Bad Request',
            code: 'VALIDATION_ERROR',
            message: 'Invalid configuration',
            path: '/configurations/91',
            details: [{ field: 'name', code: 'INVALID', message: 'Название уже недоступно' }],
          },
          { status: 400 },
        ),
      ),
    );
    renderAt('/configurations/91/edit');

    const name = await screen.findByRole('textbox', { name: 'Название' });
    await user.clear(name);
    await user.type(name, 'Новый черновик');
    await user.click(screen.getByRole('button', { name: 'Сохранить изменения' }));

    expect(await screen.findByText('Название уже недоступно')).toBeInTheDocument();
    expect(name).toHaveValue('Новый черновик');
    expect(screen.getByText('Ryzen 9')).toBeInTheDocument();
  });

  it('blocks an archived composition until the archived component is removed', async () => {
    const user = userEvent.setup();
    let requestBody: unknown;
    const archivedConfiguration = {
      ...configuration,
      components: [
        configuration.components[0]!,
        { ...configuration.components[1]!, archived: true },
      ],
    };
    useConfigurationHandlers(archivedConfiguration);
    server.use(
      http.put(`${testApiBaseUrl}/configurations/91`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json({ ...configuration, components: [configuration.components[0]!] });
      }),
    );
    renderAt('/configurations/91/edit');

    expect(
      await screen.findByText('Удалите или замените все архивные компоненты.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Сохранить изменения' })).toBeDisabled();
    const archivedRow =
      screen.getByText('RTX 5090').closest('[data-replacement-target]') ??
      screen.getByText('RTX 5090').closest('.mantine-Paper-root');
    expect(archivedRow).not.toBeNull();
    await user.click(within(archivedRow as HTMLElement).getByRole('button', { name: 'Убрать' }));
    await user.click(screen.getByRole('button', { name: 'Сохранить изменения' }));

    await findConfigurationHeading('Рабочая станция');
    expect(requestBody).toEqual({
      name: 'Рабочая станция',
      description: 'Тихая сборка',
      componentIds: [7],
    });
  });

  it('guards route navigation after metadata changes', async () => {
    const user = userEvent.setup();
    useConfigurationHandlers();
    const { router } = renderAt('/configurations/91/edit');

    await user.type(await screen.findByRole('textbox', { name: 'Название' }), ' changed');
    await user.click(screen.getByRole('link', { name: 'Отменить редактирование' }));

    expect(await screen.findByText('Изменения конфигурации будут потеряны.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Остаться' }));
    expect(router.state.location.pathname).toBe('/configurations/91/edit');

    await user.click(screen.getByRole('link', { name: 'Отменить редактирование' }));
    await user.click(screen.getByRole('button', { name: 'Выйти' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/configurations/91'));
  });

  it('asks before changing domain and then opens the new domain list', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem(selectedDomainStorageKey, '101');
    useConfigurationHandlers();
    server.use(
      http.get(`${testApiBaseUrl}/domains`, () =>
        HttpResponse.json({
          items: [
            { id: 101, name: 'Сборка ПК', createdAt: '2026-08-01T10:00:00Z' },
            { id: 202, name: 'Рабочее место', createdAt: '2026-08-02T10:00:00Z' },
          ],
          page: 0,
          size: 100,
          totalItems: 2,
        }),
      ),
      http.get(`${testApiBaseUrl}/domains/202/configurations`, () =>
        HttpResponse.json({ items: [], page: 0, size: 10, totalItems: 0 }),
      ),
    );
    const { router } = renderAt('/configurations/91/edit');

    await user.type(await screen.findByRole('textbox', { name: 'Название' }), ' changed');
    await user.click(screen.getByRole('button', { name: /Предметная область: Сборка ПК/ }));
    await user.click(await screen.findByRole('menuitem', { name: 'Рабочее место' }));

    expect(await screen.findByText('Сменить область без сохранения?')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Сменить область' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/configurations'));
    expect(window.localStorage.getItem(selectedDomainStorageKey)).toBe('202');
  });

  it('requires an explicit switch when a deep link belongs to another domain', async () => {
    const user = userEvent.setup();
    const foreignContextConfiguration = { ...configuration, domainId: 202 };
    server.use(
      http.get(`${testApiBaseUrl}/domains`, () =>
        HttpResponse.json({
          items: [
            { id: 101, name: 'Сборка ПК', createdAt: '2026-08-01T10:00:00Z' },
            { id: 202, name: 'Рабочее место', createdAt: '2026-08-02T10:00:00Z' },
          ],
          page: 0,
          size: 100,
          totalItems: 2,
        }),
      ),
      http.get(`${testApiBaseUrl}/configurations/91`, () =>
        HttpResponse.json(foreignContextConfiguration),
      ),
    );
    renderAt('/configurations/91');

    expect(await screen.findByText('Конфигурация находится в другой области')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { level: 1, name: 'Рабочая станция' })).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Переключиться на «Рабочее место»' }));
    expect(await findConfigurationHeading('Рабочая станция')).toBeInTheDocument();
  });
});
