import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { App } from '@/app/App';
import { appRoutes } from '@/app/router/routes';
import type { ConfigurationPage } from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

function renderPage() {
  const router = createMemoryRouter(appRoutes, { initialEntries: ['/configurations'] });
  return render(<App router={router} />);
}

describe('ConfigurationsPage', () => {
  it('keeps a fifty-model composition compact until it is expanded', async () => {
    const user = userEvent.setup();
    server.use(
      http.get(`${testApiBaseUrl}/domains/101/configurations`, () =>
        HttpResponse.json({
          page: 0,
          size: 10,
          totalItems: 1,
          items: [
            {
              id: 91,
              domainId: 101,
              name: 'Большая сборка',
              createdAt: '2026-09-09T12:00:00Z',
              trackInventory: false,
              components: Array.from({ length: 50 }, (_, index) => ({
                id: index + 1,
                name: `Модель ${index + 1}`,
                componentTypeId: 11,
                componentTypeName: 'Модуль',
                quantity: 1,
                availableQuantity: 1,
                archived: false,
              })),
            },
          ],
        }),
      ),
    );
    renderPage();
    await screen.findByRole('heading', { name: 'Большая сборка' });
    expect(screen.getAllByRole('link', { name: /^Модель / })).toHaveLength(3);
    await user.click(screen.getByRole('button', { name: 'Ещё 47' }));
    expect(screen.getAllByRole('link', { name: /^Модель / })).toHaveLength(50);
    await user.click(screen.getByRole('button', { name: 'Свернуть' }));
    expect(screen.getAllByRole('link', { name: /^Модель / })).toHaveLength(3);
  });

  it('sends debounced search and sorting to the server and resets empty filters', async () => {
    const user = userEvent.setup();
    const requests: URL[] = [];
    server.use(
      http.get(`${testApiBaseUrl}/domains/101/configurations`, ({ request }) => {
        requests.push(new URL(request.url));
        return HttpResponse.json({ page: 0, size: 10, totalItems: 0, items: [] });
      }),
    );
    renderPage();
    const search = await screen.findByRole('textbox', { name: 'Поиск конфигураций' });
    await user.type(search, 'Work');
    await waitFor(() => expect(requests.at(-1)?.searchParams.get('name')).toBe('Work'));
    await user.tab();
    expect(screen.getByRole('combobox', { name: 'Сортировка' })).toHaveFocus();
    await user.keyboard('{ArrowDown}{ArrowDown}{ArrowDown}{Enter}');
    await waitFor(() => expect(requests.at(-1)?.searchParams.get('sortBy')).toBe('name'));
    expect(requests.at(-1)?.searchParams.get('sortDirection')).toBe('asc');
    expect(await screen.findByRole('heading', { name: 'Ничего не найдено' })).toBeInTheDocument();
    await user.click(screen.getAllByRole('button', { name: 'Сбросить фильтры' })[0]!);
    await waitFor(() =>
      expect(screen.queryByRole('heading', { name: 'Ничего не найдено' })).not.toBeInTheDocument(),
    );
    expect(search).toHaveValue('');
  });

  it('renders server-ordered cards, archived composition and pagination', async () => {
    const user = userEvent.setup();
    const requestedPages: number[] = [];
    server.use(
      http.get(`${testApiBaseUrl}/domains/101/configurations`, ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page') ?? 0);
        requestedPages.push(page);
        const response: ConfigurationPage = {
          page,
          size: 10,
          totalItems: 11,
          items:
            page === 0
              ? [
                  {
                    id: 91,
                    domainId: 101,
                    name: 'Рабочая станция',
                    description: 'Тихая сборка',
                    createdAt: '2026-08-23T10:00:00Z',
                    trackInventory: false,
                    components: [
                      {
                        id: 7,
                        name: 'Ryzen 9',
                        brand: 'AMD',
                        componentTypeId: 11,
                        componentTypeName: 'Процессор',
                        archived: false,
                        quantity: 1,
                        availableQuantity: 8,
                      },
                      {
                        id: 8,
                        name: 'RTX 5090',
                        componentTypeId: 12,
                        componentTypeName: 'Видеокарта',
                        archived: true,
                        quantity: 1,
                        availableQuantity: 0,
                      },
                    ],
                  },
                ]
              : [
                  {
                    id: 80,
                    domainId: 101,
                    name: 'Старая сборка',
                    createdAt: '2026-08-01T10:00:00Z',
                    trackInventory: false,
                    components: [],
                  },
                ],
        };
        return HttpResponse.json(response);
      }),
    );
    renderPage();

    const card = await screen.findByRole('article');
    expect(within(card).getByRole('heading', { name: 'Рабочая станция' })).toBeInTheDocument();
    expect(within(card).getByText('Тихая сборка')).toBeInTheDocument();
    expect(within(card).getByText('Ryzen 9')).toBeInTheDocument();
    expect(within(card).getByText('RTX 5090')).toBeInTheDocument();
    expect(within(card).getByText('В архиве')).toBeInTheDocument();
    await user.click(
      within(card).getByRole('button', {
        name: 'Действия с конфигурацией Рабочая станция',
      }),
    );
    const copyAction = await screen.findByRole('menuitem', { name: /Копировать/ });
    expect(copyAction).toHaveAttribute('data-disabled', 'true');
    expect(
      screen.getByText('Сначала удалите или замените архивные компоненты в редакторе.'),
    ).toBeInTheDocument();
    await user.keyboard('{Escape}');

    await user.click(screen.getByRole('button', { name: '2' }));
    expect(await screen.findByRole('heading', { name: 'Старая сборка' })).toBeInTheDocument();
    expect(requestedPages).toEqual([0, 1]);
  });

  it('shows an empty state that returns to the configurator', async () => {
    server.use(
      http.get(`${testApiBaseUrl}/domains/101/configurations`, () =>
        HttpResponse.json({ items: [], page: 0, size: 10, totalItems: 0 }),
      ),
    );
    renderPage();

    expect(await screen.findByText('Сохранённых конфигураций пока нет')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Открыть конфигуратор' })).not.toHaveLength(0);
  });

  it('shows a retryable error state', async () => {
    let requests = 0;
    server.use(
      http.get(`${testApiBaseUrl}/domains/101/configurations`, () => {
        requests += 1;
        if (requests <= 2) {
          return HttpResponse.json(
            {
              timestamp: '2026-08-23T12:00:00Z',
              status: 500,
              error: 'Internal Server Error',
              code: 'INTERNAL_ERROR',
              message: 'Unavailable',
              path: '/domains/101/configurations',
              details: [],
            },
            { status: 500 },
          );
        }
        return HttpResponse.json({ items: [], page: 0, size: 10, totalItems: 0 });
      }),
    );
    const user = userEvent.setup();
    renderPage();

    const retry = await screen.findByRole('button', { name: 'Повторить' }, { timeout: 3000 });
    await user.click(retry);
    await waitFor(() => expect(requests).toBe(3));
  });

  it('returns to the previous page after deleting its last configuration', async () => {
    const user = userEvent.setup();
    let deleted = false;
    const requestedPages: number[] = [];
    server.use(
      http.get(`${testApiBaseUrl}/domains/101/configurations`, ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page') ?? 0);
        requestedPages.push(page);
        return HttpResponse.json({
          page,
          size: 10,
          totalItems: deleted ? 10 : 11,
          items:
            page === 1 && !deleted
              ? [
                  {
                    id: 80,
                    domainId: 101,
                    name: 'Последняя на странице',
                    createdAt: '2026-08-01T10:00:00Z',
                    trackInventory: false,
                    components: [],
                  },
                ]
              : page === 0
                ? [
                    {
                      id: 79,
                      domainId: 101,
                      name: 'Предыдущая страница',
                      createdAt: '2026-08-01T10:00:00Z',
                      trackInventory: false,
                      components: [],
                    },
                  ]
                : [],
        });
      }),
      http.delete(`${testApiBaseUrl}/configurations/80`, () => {
        deleted = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderPage();

    await screen.findByRole('heading', { name: 'Предыдущая страница' });
    await user.click(screen.getByRole('button', { name: '2' }));
    await screen.findByRole('heading', { name: 'Последняя на странице' });
    await user.click(
      screen.getByRole('button', { name: 'Действия с конфигурацией Последняя на странице' }),
    );
    await user.click(await screen.findByRole('menuitem', { name: 'Удалить' }));
    const dialog = await screen.findByRole('dialog', { name: 'Удалить конфигурацию?' });
    await user.click(within(dialog).getByRole('button', { name: 'Удалить' }));

    expect(await screen.findByRole('heading', { name: 'Предыдущая страница' })).toBeInTheDocument();
    expect(requestedPages.at(-1)).toBe(0);
  });
});
