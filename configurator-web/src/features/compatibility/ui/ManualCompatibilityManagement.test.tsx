import { notifications } from '@mantine/notifications';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse, type HttpResponseResolver } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';
import { createMemoryRouter } from 'react-router-dom';

import { App } from '@/app/App';
import { appRoutes } from '@/app/router/routes';
import type { CreateCompatibilityLinkRequest, ErrorResponse, GraphResponse } from '@/shared/api';
import { queryClient } from '@/shared/query/query-client';
import { server, testApiBaseUrl } from '@/test/server';

const domainId = 101;
const baseGraph: GraphResponse = {
  nodes: [
    {
      id: 11,
      name: 'Ryzen 7 7800X3D',
      componentTypeId: 1,
      componentTypeName: 'Процессор',
      brand: 'AMD',
    },
    {
      id: 12,
      name: 'B650 Tomahawk',
      componentTypeId: 2,
      componentTypeName: 'Материнская плата',
      brand: 'MSI',
    },
    {
      id: 13,
      name: 'Radeon RX 7900 XTX',
      componentTypeId: 3,
      componentTypeName: 'Видеокарта',
      brand: 'AMD',
    },
  ],
  edges: [{ id: 201, source: 11, target: 12, comment: 'Сокет AM5' }],
};

function renderPage() {
  const router = createMemoryRouter(appRoutes, {
    initialEntries: ['/settings/compatibility/manual'],
  });
  return render(<App router={router} />);
}

function useHandlers(
  graph: GraphResponse,
  overrides: {
    getGraph?: HttpResponseResolver;
    create?: HttpResponseResolver;
    remove?: HttpResponseResolver;
  } = {},
) {
  server.use(
    http.get(
      `${testApiBaseUrl}/domains/:domainId/compatibility/graph`,
      overrides.getGraph ?? (() => HttpResponse.json(graph)),
    ),
    http.post(
      `${testApiBaseUrl}/domains/:domainId/compatibility`,
      overrides.create ??
        (async ({ request }) => {
          const body = (await request.json()) as CreateCompatibilityLinkRequest;
          const firstId = Math.min(body.componentAId, body.componentBId);
          const secondId = Math.max(body.componentAId, body.componentBId);
          const created = {
            id: 202,
            domainId,
            componentAId: firstId,
            componentBId: secondId,
            ...(body.comment ? { comment: body.comment } : {}),
          };
          graph.edges.push({
            id: created.id,
            source: firstId,
            target: secondId,
            ...(created.comment ? { comment: created.comment } : {}),
          });
          return HttpResponse.json(created, { status: 201 });
        }),
    ),
    http.delete(
      `${testApiBaseUrl}/domains/:domainId/compatibility/:linkId`,
      overrides.remove ??
        (({ params }) => {
          const edgeIndex = graph.edges.findIndex((edge) => edge.id === Number(params['linkId']));
          if (edgeIndex >= 0) {
            graph.edges.splice(edgeIndex, 1);
          }
          return new HttpResponse(null, { status: 204 });
        }),
    ),
  );
}

afterEach(() => {
  notifications.clean();
});

describe('manual compatibility management', () => {
  it('renders manual links, metadata, counters and filters them locally', async () => {
    const user = userEvent.setup();
    useHandlers(structuredClone(baseGraph));
    renderPage();

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Ручная совместимость' }),
    ).toBeInTheDocument();
    expect(await screen.findByText(/3 активных компонент/)).toBeInTheDocument();
    expect(screen.getByText(/1 ручная связь/)).toBeInTheDocument();
    expect(screen.getAllByText('Ryzen 7 7800X3D').length).toBeGreaterThan(0);
    expect(screen.getAllByText('B650 Tomahawk').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Сокет AM5').length).toBeGreaterThan(0);
    expect(screen.getByTestId('desktop-manual-compatibility-table')).toBeInTheDocument();
    expect(screen.getByTestId('mobile-manual-compatibility-list')).toBeInTheDocument();

    await user.type(screen.getByRole('textbox', { name: 'Поиск' }), 'Radeon');
    expect(await screen.findByRole('heading', { name: 'Связи не найдены' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Очистить поиск' }));
    expect(screen.getAllByText('B650 Tomahawk').length).toBeGreaterThan(0);
  });

  it('creates a trimmed link and excludes self and an existing neighbour', async () => {
    const user = userEvent.setup();
    const graph = structuredClone(baseGraph);
    let submitted: CreateCompatibilityLinkRequest | undefined;
    useHandlers(graph, {
      create: async ({ request }) => {
        submitted = (await request.json()) as CreateCompatibilityLinkRequest;
        const created = {
          id: 202,
          domainId,
          componentAId: 11,
          componentBId: 13,
          comment: submitted.comment,
        };
        graph.edges.push({
          id: created.id,
          source: created.componentAId,
          target: created.componentBId,
          ...(created.comment ? { comment: created.comment } : {}),
        });
        return HttpResponse.json(created, { status: 201 });
      },
    });
    renderPage();
    await screen.findAllByText('Сокет AM5');

    await user.click(screen.getByRole('button', { name: 'Добавить связь' }));
    const dialog = await screen.findByRole('dialog', { name: 'Новая ручная связь' });
    await user.click(within(dialog).getByRole('button', { name: 'Добавить связь' }));
    expect(await within(dialog).findByText('Выберите компонент')).toBeInTheDocument();
    expect(within(dialog).getByText('Выберите совместимый компонент')).toBeInTheDocument();

    const source = within(dialog).getByRole('combobox', { name: 'Компонент' });
    await user.type(source, 'Ryzen');
    await user.keyboard('{ArrowDown}{Enter}');
    const target = within(dialog).getByRole('combobox', { name: 'Совместим с' });
    await user.type(target, 'Radeon');
    await user.keyboard('{ArrowDown}{Enter}');
    await user.type(
      within(dialog).getByRole('textbox', { name: 'Комментарий' }),
      '  Один блок питания  ',
    );
    await user.click(within(dialog).getByRole('button', { name: 'Добавить связь' }));

    expect(await screen.findByText('Ручная связь создана')).toBeInTheDocument();
    expect(submitted).toEqual({
      componentAId: 11,
      componentBId: 13,
      comment: 'Один блок питания',
    });
    expect(screen.getAllByText('Radeon RX 7900 XTX').length).toBeGreaterThan(0);
  });

  it('validates the comment length before sending the request', async () => {
    const user = userEvent.setup();
    useHandlers(structuredClone(baseGraph));
    renderPage();
    await screen.findAllByText('Сокет AM5');
    await user.click(screen.getByRole('button', { name: 'Добавить связь' }));
    const dialog = await screen.findByRole('dialog', { name: 'Новая ручная связь' });
    fireEvent.change(within(dialog).getByRole('textbox', { name: 'Комментарий' }), {
      target: { value: 'x'.repeat(1001) },
    });
    await user.click(within(dialog).getByRole('button', { name: 'Добавить связь' }));

    expect(
      await within(dialog).findByText('Комментарий должен содержать не более 1000 символов'),
    ).toBeInTheDocument();
  });

  it('deletes a link only after permanent-delete confirmation', async () => {
    const user = userEvent.setup();
    const graph = structuredClone(baseGraph);
    useHandlers(graph);
    renderPage();
    await screen.findAllByText('Сокет AM5');

    await user.click(
      screen.getAllByRole('button', {
        name: 'Удалить связь Ryzen 7 7800X3D и B650 Tomahawk',
      })[0]!,
    );
    const dialog = await screen.findByRole('dialog', { name: 'Удалить ручную связь?' });
    expect(within(dialog).getByText(/Действие необратимо/)).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: 'Отмена' }));
    expect(screen.getAllByText('Сокет AM5').length).toBeGreaterThan(0);

    await user.click(
      screen.getAllByRole('button', {
        name: 'Удалить связь Ryzen 7 7800X3D и B650 Tomahawk',
      })[0]!,
    );
    await user.click(
      within(await screen.findByRole('dialog', { name: 'Удалить ручную связь?' })).getByRole(
        'button',
        { name: 'Удалить' },
      ),
    );

    expect(await screen.findByText('Ручная связь удалена')).toBeInTheDocument();
    expect(
      await screen.findByRole('heading', { name: 'Ручных связей пока нет' }),
    ).toBeInTheDocument();
  });

  it('shows empty and retry states without changing the API contract', async () => {
    const emptyGraph: GraphResponse = { nodes: [], edges: [] };
    useHandlers(emptyGraph);
    const view = renderPage();
    expect(
      await screen.findByRole('heading', { name: 'Нет активных компонентов' }),
    ).toBeInTheDocument();
    view.unmount();
    queryClient.clear();

    const error: ErrorResponse = {
      timestamp: '2026-08-23T12:00:00Z',
      status: 500,
      error: 'Internal Server Error',
      code: 'INTERNAL_ERROR',
      message: 'Graph unavailable',
      path: `/domains/${domainId}/compatibility/graph`,
      details: [],
    };
    server.resetHandlers();
    server.use(
      http.get(`${testApiBaseUrl}/domains`, () =>
        HttpResponse.json({
          items: [
            {
              id: domainId,
              name: 'Сборка ПК',
              createdAt: '2026-08-09T12:00:00Z',
            },
          ],
          page: 0,
          size: 100,
          totalItems: 1,
        }),
      ),
      http.get(`${testApiBaseUrl}/domains/:domainId/compatibility/graph`, () =>
        HttpResponse.json(error, { status: 500 }),
      ),
    );
    renderPage();

    expect(
      await screen.findByText('Внутренняя ошибка сервера', undefined, { timeout: 3000 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Повторить' })).toBeInTheDocument();
  });

  it('keeps the link visible when deletion fails', async () => {
    const user = userEvent.setup();
    useHandlers(structuredClone(baseGraph), {
      remove: () =>
        HttpResponse.json(
          {
            timestamp: '2026-08-23T12:00:00Z',
            status: 500,
            error: 'Internal Server Error',
            code: 'INTERNAL_ERROR',
            message: 'Delete failed',
            path: `/domains/${domainId}/compatibility/201`,
            details: [],
          } satisfies ErrorResponse,
          { status: 500 },
        ),
    });
    renderPage();
    await screen.findAllByText('Сокет AM5');
    await user.click(
      screen.getAllByRole('button', {
        name: 'Удалить связь Ryzen 7 7800X3D и B650 Tomahawk',
      })[0]!,
    );
    await user.click(
      within(await screen.findByRole('dialog', { name: 'Удалить ручную связь?' })).getByRole(
        'button',
        { name: 'Удалить' },
      ),
    );

    await waitFor(() => expect(screen.getAllByText('Сокет AM5').length).toBeGreaterThan(0));
    expect(screen.getByRole('dialog', { name: 'Удалить ручную связь?' })).toBeInTheDocument();
  });
});
