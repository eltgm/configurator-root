import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it } from 'vitest';

import { App } from '@/app/App';
import { appRoutes } from '@/app/router/routes';
import type { ComponentType, Configuration, ConfiguratorBatchSearchResponse } from '@/shared/api';
import { selectedDomainStorageKey } from '@/shared/config/preferences';
import { server, testApiBaseUrl } from '@/test/server';

const componentTypes: ComponentType[] = [
  { id: 11, domainId: 101, name: 'Процессор', orderIndex: 10 },
  { id: 12, domainId: 101, name: 'Видеокарта', orderIndex: 20 },
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

function directBatch(componentIds = [7, 8]): ConfiguratorBatchSearchResponse {
  const [left, right] = componentIds;
  if (left === undefined || right === undefined) return { results: [] };
  return {
    results: [
      {
        baseComponentId: left,
        compatibleByType: [
          {
            componentTypeId: 12,
            componentTypeName: 'Видеокарта',
            components: [
              {
                id: right,
                name: 'RTX 5090',
                componentTypeId: 12,
                explanations: [{ source: 'MANUAL', linkId: 1 }],
              },
            ],
          },
        ],
      },
      {
        baseComponentId: right,
        compatibleByType: [
          {
            componentTypeId: 11,
            componentTypeName: 'Процессор',
            components: [
              {
                id: left,
                name: 'Ryzen 9',
                componentTypeId: 11,
                explanations: [{ source: 'MANUAL', linkId: 1 }],
              },
            ],
          },
        ],
      },
    ],
  };
}

function useConfigurationHandlers(value: Configuration = configuration) {
  server.use(
    http.get(`${testApiBaseUrl}/configurations/${value.id}`, () => HttpResponse.json(value)),
    http.get(`${testApiBaseUrl}/domains/101/component-types`, () =>
      HttpResponse.json(componentTypes),
    ),
    http.post(
      `${testApiBaseUrl}/domains/101/configurator/compatible/search`,
      async ({ request }) => {
        const body = (await request.json()) as { componentIds: number[] };
        return HttpResponse.json(directBatch(body.componentIds));
      },
    ),
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
    expect(screen.queryByRole('button', { name: /Удалить|Экспорт|Копировать/ })).toBeNull();
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
    await user.click(within(browser).getByRole('button', { name: 'Выбрать' }));
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
