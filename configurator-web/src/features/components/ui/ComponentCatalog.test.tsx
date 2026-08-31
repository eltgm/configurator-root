import { notifications } from '@mantine/notifications';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';
import { createMemoryRouter } from 'react-router-dom';

import { App } from '@/app/App';
import { appRoutes } from '@/app/router/routes';
import type { Component, ComponentType, ErrorResponse } from '@/shared/api';
import { componentCatalogViewStorageKey } from '@/shared/config/preferences';
import { server, testApiBaseUrl } from '@/test/server';

const domainId = 101;
const componentTypes: Array<ComponentType> = [
  { id: 11, domainId, name: 'Процессор', code: 'CPU', orderIndex: 1 },
  { id: 12, domainId, name: 'Видеокарта', code: 'GPU', orderIndex: 2 },
];
const ryzen: Component = {
  id: 101,
  componentTypeId: 11,
  name: 'Ryzen 7 7800X3D',
  brand: 'AMD',
  archived: false,
  createdAt: '2026-08-01T12:00:00Z',
  attributes: [
    {
      attributeDefinitionId: 1001,
      name: 'cores',
      label: 'Ядра',
      dataType: 'NUMBER',
      value: '8',
    },
  ],
  primaryImage: {
    id: 5001,
    url: '/component-images/5001/content',
    thumbnailUrl: '/component-images/5001/thumbnail',
    orderIndex: 0,
  },
  images: [
    {
      id: 5001,
      url: '/component-images/5001/content',
      thumbnailUrl: '/component-images/5001/thumbnail',
      orderIndex: 0,
    },
  ],
};
const radeon: Component = {
  id: 102,
  componentTypeId: 12,
  name: 'Radeon RX 7900 XTX',
  brand: 'AMD',
  archived: false,
  createdAt: '2026-08-02T12:00:00Z',
};
const archivedIntel: Component = {
  id: 201,
  componentTypeId: 11,
  name: 'Core i7-12700K',
  brand: 'Intel',
  archived: true,
  createdAt: '2026-07-01T12:00:00Z',
};

function renderPage() {
  const router = createMemoryRouter(appRoutes, { initialEntries: ['/components'] });
  return render(<App router={router} />);
}

function useCatalogHandlers(
  activeComponents: Array<Component> = [{ ...ryzen }, { ...radeon }],
  archivedComponents: Array<Component> = [{ ...archivedIntel }],
  onRequest?: (url: URL) => void,
) {
  server.use(
    http.get(`${testApiBaseUrl}/domains/:domainId/component-types`, () =>
      HttpResponse.json(componentTypes),
    ),
    http.get(`${testApiBaseUrl}/domains/:domainId/components`, ({ request }) => {
      const url = new URL(request.url);
      onRequest?.(url);
      const source =
        url.searchParams.get('archived') === 'true' ? archivedComponents : activeComponents;
      const name = url.searchParams.get('name')?.toLocaleLowerCase() ?? '';
      const typeId = Number(url.searchParams.get('componentTypeId')) || undefined;
      const page = Number(url.searchParams.get('page') ?? 0);
      const size = Number(url.searchParams.get('size') ?? 12);
      const filtered = source.filter(
        (component) =>
          component.name.toLocaleLowerCase().includes(name) &&
          (typeId === undefined || component.componentTypeId === typeId),
      );
      return HttpResponse.json({
        items: filtered.slice(page * size, page * size + size),
        page,
        size,
        totalItems: filtered.length,
      });
    }),
    http.delete(`${testApiBaseUrl}/components/:id`, ({ params }) => {
      const index = activeComponents.findIndex(
        (component) => component.id === Number(params['id']),
      );
      if (index >= 0) {
        const [component] = activeComponents.splice(index, 1);
        if (component) {
          archivedComponents.push({ ...component, archived: true });
        }
      }
      return new HttpResponse(null, { status: 204 });
    }),
    http.post(`${testApiBaseUrl}/components/:id/restore`, ({ params }) => {
      const index = archivedComponents.findIndex(
        (component) => component.id === Number(params['id']),
      );
      const [component] = index >= 0 ? archivedComponents.splice(index, 1) : [];
      if (!component) {
        return new HttpResponse(null, { status: 404 });
      }
      const restored = { ...component, archived: false };
      activeComponents.push(restored);
      return HttpResponse.json(restored);
    }),
  );
}

afterEach(() => {
  notifications.clean();
  window.localStorage.clear();
});

describe('component catalog', () => {
  it('renders component cards with type, attributes and a backend image URL', async () => {
    useCatalogHandlers();
    const { container } = renderPage();

    expect(await screen.findByRole('heading', { level: 2, name: ryzen.name })).toBeInTheDocument();
    expect(screen.getByText('2 компонента')).toBeInTheDocument();
    expect(screen.getByText('Ядра')).toBeInTheDocument();
    expect(screen.getByText('8')).toBeInTheDocument();
    expect(screen.getAllByText('Процессор').length).toBeGreaterThan(0);
    expect(container.querySelector('img')).toHaveAttribute(
      'src',
      '/api/component-images/5001/thumbnail',
    );
  });

  it('sends search and type filters to the server and resets them', async () => {
    const user = userEvent.setup();
    const requests: Array<URL> = [];
    useCatalogHandlers([{ ...ryzen }, { ...radeon }], [{ ...archivedIntel }], (url) =>
      requests.push(url),
    );
    renderPage();
    await screen.findByRole('heading', { level: 2, name: ryzen.name });

    await user.type(screen.getByRole('textbox', { name: 'Поиск' }), 'Radeon');
    expect(await screen.findByRole('heading', { level: 2, name: radeon.name })).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByRole('heading', { level: 2, name: ryzen.name })).not.toBeInTheDocument(),
    );
    expect(requests.some((url) => url.searchParams.get('name') === 'Radeon')).toBe(true);

    await user.click(screen.getByRole('button', { name: 'Сбросить фильтры' }));
    expect(await screen.findByRole('heading', { level: 2, name: ryzen.name })).toBeInTheDocument();

    await user.click(screen.getByRole('combobox', { name: 'Тип компонента' }));
    await user.keyboard('{ArrowDown}{ArrowDown}{Enter}');
    await waitFor(() =>
      expect(requests.some((url) => url.searchParams.get('componentTypeId') === '12')).toBe(true),
    );

    await user.click(screen.getByRole('button', { name: 'Сбросить фильтры' }));
    expect(await screen.findByRole('heading', { level: 2, name: ryzen.name })).toBeInTheDocument();
  });

  it('switches to the table representation and persists it', async () => {
    const user = userEvent.setup();
    useCatalogHandlers();
    renderPage();
    await screen.findByRole('heading', { level: 2, name: ryzen.name });

    await user.click(screen.getByText('Таблица'));

    expect(await screen.findByRole('table')).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Бренд' })).toBeInTheDocument();
    expect(window.localStorage.getItem(componentCatalogViewStorageKey)).toBe('table');
  });

  it('archives a component only after confirmation', async () => {
    const user = userEvent.setup();
    const active = [{ ...ryzen }];
    useCatalogHandlers(active, []);
    renderPage();
    await screen.findByRole('heading', { level: 2, name: ryzen.name });

    await user.click(screen.getByRole('button', { name: 'В архив' }));
    const dialog = await screen.findByRole('dialog', { name: 'Архивировать компонент?' });
    expect(within(dialog).getByText(/Ryzen 7 7800X3D/)).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: 'В архив' }));

    expect(await screen.findByRole('heading', { name: 'Каталог пока пуст' })).toBeInTheDocument();
    expect(await screen.findByText('Компонент перемещён в архив')).toBeInTheDocument();
  });

  it('opens the archive and restores a component', async () => {
    const user = userEvent.setup();
    useCatalogHandlers([], [{ ...archivedIntel }]);
    renderPage();
    await screen.findByRole('heading', { name: 'Каталог пока пуст' });

    await user.click(screen.getByText('Архив'));
    expect(
      await screen.findByRole('heading', { level: 2, name: archivedIntel.name }),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Восстановить' }));

    expect(await screen.findByRole('heading', { name: 'Архив пуст' })).toBeInTheDocument();
    expect(await screen.findByText('Компонент восстановлен')).toBeInTheDocument();
  });

  it('shows a filtered empty state and clears the search', async () => {
    const user = userEvent.setup();
    useCatalogHandlers();
    renderPage();
    await screen.findByRole('heading', { level: 2, name: ryzen.name });

    await user.type(screen.getByRole('textbox', { name: 'Поиск' }), 'missing');
    expect(await screen.findByRole('heading', { name: 'Ничего не найдено' })).toBeInTheDocument();
    await user.click(screen.getAllByRole('button', { name: 'Сбросить фильтры' })[0]!);
    expect(await screen.findByRole('heading', { level: 2, name: ryzen.name })).toBeInTheDocument();
  });

  it('shows a safe retry state when the initial catalog request fails', async () => {
    const error: ErrorResponse = {
      timestamp: '2026-08-09T12:00:00Z',
      status: 500,
      error: 'Internal Server Error',
      code: 'INTERNAL_ERROR',
      message: 'Catalog unavailable',
      path: '/domains/101/components',
      details: [],
    };
    server.use(
      http.get(`${testApiBaseUrl}/domains/:domainId/component-types`, () =>
        HttpResponse.json(componentTypes),
      ),
      http.get(`${testApiBaseUrl}/domains/:domainId/components`, () =>
        HttpResponse.json(error, { status: 500 }),
      ),
    );
    renderPage();

    expect(
      await screen.findByText('Внутренняя ошибка сервера', undefined, { timeout: 3000 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Повторить' })).toBeInTheDocument();
  });

  it('returns pagination to the first page when the search changes', async () => {
    const user = userEvent.setup();
    const components = Array.from({ length: 13 }, (_, index) => ({
      ...ryzen,
      id: 1000 + index,
      name: index === 12 ? 'Special Ryzen' : `Ryzen ${index + 1}`,
    }));
    const requestedPages: Array<string | null> = [];
    useCatalogHandlers(components, [], (url) => requestedPages.push(url.searchParams.get('page')));
    renderPage();
    await screen.findByRole('heading', { level: 2, name: 'Ryzen 1' });

    await user.click(screen.getByRole('button', { name: /2/ }));
    expect(
      await screen.findByRole('heading', { level: 2, name: 'Special Ryzen' }),
    ).toBeInTheDocument();
    await user.type(screen.getByRole('textbox', { name: 'Поиск' }), 'Ryzen 1');
    await screen.findByRole('heading', { level: 2, name: 'Ryzen 1' });

    expect(requestedPages).toContain('1');
    await waitFor(() => expect(requestedPages.at(-1)).toBe('0'));
  });
});
