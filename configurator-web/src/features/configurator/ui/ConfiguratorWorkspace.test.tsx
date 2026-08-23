import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { createMemoryRouter } from 'react-router-dom';

import { App } from '@/app/App';
import { appRoutes } from '@/app/router/routes';
import type { Component, ComponentType } from '@/shared/api';
import { configuratorDraftStorageKey } from '@/shared/config/preferences';
import { server, testApiBaseUrl } from '@/test/server';

const domainId = 101;
const componentTypes: ComponentType[] = [
  { id: 11, domainId, name: 'Процессор', orderIndex: 1 },
  { id: 12, domainId, name: 'Видеокарта', orderIndex: 2 },
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
const components = [ryzen, intel, radeon];

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
  );
}

function renderPage() {
  const router = createMemoryRouter(appRoutes, { initialEntries: ['/configurator'] });
  return render(<App router={router} />);
}

afterEach(() => window.localStorage.clear());

describe('configurator workspace', () => {
  it('adds, explicitly replaces, removes and clears components while persisting the draft', async () => {
    const user = userEvent.setup();
    useHandlers();
    renderPage();

    const browser = await screen.findByRole('region', { name: 'Доступные компоненты' });
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

    await user.click(within(browser).getByRole('button', { name: 'Заменить' }));
    const replaceDialog = await screen.findByRole('dialog', {
      name: 'Заменить компонент этого типа?',
    });
    expect(within(replaceDialog).getByText(/Ryzen 7 7800X3D/)).toBeInTheDocument();
    await user.click(within(replaceDialog).getByRole('button', { name: 'Заменить' }));
    await waitFor(() => expect(replaceDialog).not.toBeInTheDocument());
    expect(await within(assembly).findByText(intel.name)).toBeInTheDocument();
    expect(within(assembly).queryByText(ryzen.name)).not.toBeInTheDocument();

    await user.click(within(browser).getAllByRole('button', { name: 'Добавить' }).at(-1)!);
    expect(await within(assembly).findByText(radeon.name)).toBeInTheDocument();
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
});
