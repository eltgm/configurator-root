import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { createMemoryRouter } from 'react-router-dom';

import { App } from '@/app/App';
import { appRoutes } from '@/app/router/routes';
import type { GraphResponse } from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

const graph: GraphResponse = {
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
    },
  ],
  edges: [{ id: 201, source: 11, target: 12, comment: 'Сокет AM5' }],
};

function renderPage(response: GraphResponse = graph) {
  server.use(
    http.get(`${testApiBaseUrl}/domains/:domainId/compatibility/graph`, () =>
      HttpResponse.json(response),
    ),
  );
  const router = createMemoryRouter(appRoutes, {
    initialEntries: ['/settings/compatibility/graph'],
  });
  return render(<App router={router} />);
}

describe('compatibility graph exploration', () => {
  it('renders manual scope, counters, isolated nodes and graph controls', async () => {
    renderPage();

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Граф совместимости' }),
    ).toBeInTheDocument();
    expect(screen.getByText(/только явно созданные ручные связи/)).toBeInTheDocument();
    expect(await screen.findByText(/3 активных компонент/)).toBeInTheDocument();
    expect(await screen.findByText(/1 ручная связь/)).toBeInTheDocument();
    expect(await screen.findByText(/1 без связей/)).toBeInTheDocument();
    expect(screen.getByTestId('compatibility-graph-canvas')).toBeInTheDocument();
    expect(
      screen.getByText(/Узлы не перетаскиваются\. Выберите компонент через поиск/),
    ).toBeInTheDocument();
    expect(document.querySelector('.react-flow__node.draggable')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Сбросить раскладку' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Управлять связями' })).toHaveAttribute(
      'href',
      '/settings/compatibility/manual',
    );
  });

  it('selects nodes and exposes an accessible details alternative', async () => {
    renderPage();

    const processor = await screen.findByLabelText('Компонент Ryzen 7 7800X3D, тип Процессор');
    fireEvent.click(processor);
    expect(screen.getByRole('heading', { level: 2, name: 'Ryzen 7 7800X3D' })).toBeInTheDocument();
    expect(screen.getByText('1 непосредственная связь')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Открыть карточку компонента' })).toHaveAttribute(
      'href',
      '/components/11',
    );

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(screen.getByRole('heading', { name: 'Выберите элемент графа' })).toBeInTheDocument();
  });

  it('centers a component selected through searchable input and resets the layout', async () => {
    const user = userEvent.setup();
    renderPage();
    const search = await screen.findByRole('combobox', { name: 'Найти компонент' });

    await user.type(search, 'Radeon');
    await user.keyboard('{ArrowDown}{Enter}');
    expect(
      await screen.findByRole('heading', { level: 2, name: 'Radeon RX 7900 XTX' }),
    ).toBeInTheDocument();
    expect(screen.getByText('У компонента нет ручных связей.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Сбросить раскладку' }));
    expect(screen.getByRole('heading', { name: 'Выберите элемент графа' })).toBeInTheDocument();
  });

  it('shows an empty state without mounting the canvas', async () => {
    renderPage({ nodes: [], edges: [] });

    expect(
      await screen.findByRole('heading', { name: 'Нет активных компонентов' }),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByTestId('compatibility-graph-canvas')).not.toBeInTheDocument(),
    );
  });

  it('shows a safe retry state when the graph request fails', async () => {
    server.use(
      http.get(`${testApiBaseUrl}/domains/:domainId/compatibility/graph`, () =>
        HttpResponse.json(
          {
            timestamp: '2026-08-23T12:00:00Z',
            status: 500,
            error: 'Internal Server Error',
            code: 'INTERNAL_ERROR',
            message: 'Graph unavailable',
            path: '/domains/101/compatibility/graph',
            details: [],
          },
          { status: 500 },
        ),
      ),
    );
    const router = createMemoryRouter(appRoutes, {
      initialEntries: ['/settings/compatibility/graph'],
    });
    render(<App router={router} />);

    expect(
      await screen.findByText('Внутренняя ошибка сервера', undefined, { timeout: 3000 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Повторить' })).toBeInTheDocument();
  });
});
