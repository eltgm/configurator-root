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
                        archived: true,
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
    expect(
      screen.queryByRole('button', { name: /Удалить|Экспорт|Копировать/ }),
    ).not.toBeInTheDocument();

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
});
