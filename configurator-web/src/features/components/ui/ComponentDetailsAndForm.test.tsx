import { notifications } from '@mantine/notifications';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';
import { createMemoryRouter } from 'react-router-dom';

import { App } from '@/app/App';
import { appRoutes } from '@/app/router/routes';
import type { AttributeDefinition, Component, ComponentType, ErrorResponse } from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

const domainId = 101;
const componentType: ComponentType = {
  id: 11,
  domainId,
  name: 'Процессор',
  code: 'CPU',
};
const attributes: Array<AttributeDefinition> = [
  {
    id: 1001,
    domainId,
    componentTypeId: 11,
    name: 'socket',
    label: 'Сокет',
    dataType: 'STRING',
    isRequired: true,
    orderIndex: 1,
  },
  {
    id: 1002,
    domainId,
    componentTypeId: 11,
    name: 'tdp',
    label: 'TDP',
    dataType: 'NUMBER',
    isRequired: false,
    orderIndex: 2,
  },
  {
    id: 1003,
    domainId,
    componentTypeId: 11,
    name: 'unlocked',
    label: 'Разгон',
    dataType: 'BOOLEAN',
    isRequired: false,
    orderIndex: 3,
  },
  {
    id: 1004,
    domainId,
    componentTypeId: 11,
    name: 'segment',
    label: 'Сегмент',
    dataType: 'ENUM',
    enumValues: ['Игровой', 'Офисный'],
    isRequired: false,
    orderIndex: 4,
  },
];
const component: Component = {
  id: 501,
  componentTypeId: 11,
  name: 'Ryzen 7 7800X3D',
  brand: 'AMD',
  description: 'Игровой процессор',
  archived: false,
  createdAt: '2026-08-09T12:00:00Z',
  attributes: [
    {
      attributeDefinitionId: 1001,
      name: 'socket',
      label: 'Сокет',
      dataType: 'STRING',
      value: 'AM5',
    },
    {
      attributeDefinitionId: 1002,
      name: 'tdp',
      label: 'TDP',
      dataType: 'NUMBER',
      value: '120',
    },
  ],
  images: [
    {
      id: 9001,
      url: '/component-images/9001/content',
      thumbnailUrl: '/component-images/9001/thumbnail',
      orderIndex: 0,
    },
  ],
};

function useHandlers(overrides?: { component?: Component }) {
  let current = { ...(overrides?.component ?? component) };
  let currentImages = [...(current.images ?? [])];
  server.use(
    http.get(`${testApiBaseUrl}/domains/:domainId/component-types`, () =>
      HttpResponse.json([componentType]),
    ),
    http.get(`${testApiBaseUrl}/component-types/:id/attributes`, () =>
      HttpResponse.json(attributes),
    ),
    http.get(testApiBaseUrl + '/components/:id', () => HttpResponse.json(current)),
    http.get(testApiBaseUrl + '/components/:id/images', () => HttpResponse.json(currentImages)),
    http.post(testApiBaseUrl + '/components/:id/images', () => {
      const createdImage = {
        id: 9002,
        url: '/component-images/9002/content',
        thumbnailUrl: '/component-images/9002/thumbnail',
        orderIndex: currentImages.length,
      };
      currentImages = [...currentImages, createdImage];
      current = { ...current, images: currentImages };
      return HttpResponse.json(createdImage, { status: 201 });
    }),
    http.delete(testApiBaseUrl + '/component-images/:id', ({ params }) => {
      currentImages = currentImages.filter((image) => image.id !== Number(params['id']));
      current = { ...current, images: currentImages };
      return new HttpResponse(null, { status: 204 });
    }),
    http.put(testApiBaseUrl + '/components/:id/images/order', async ({ request }) => {
      const body = (await request.json()) as { imageIds: Array<number> };
      currentImages = body.imageIds.map((id, orderIndex) => ({
        ...currentImages.find((image) => image.id === id)!,
        orderIndex,
      }));
      current = { ...current, images: currentImages };
      return HttpResponse.json(currentImages);
    }),
    http.post(`${testApiBaseUrl}/components`, async ({ request }) => {
      const body = (await request.json()) as Record<string, unknown>;
      current = { ...component, ...body, id: 777 };
      return HttpResponse.json(current, { status: 201 });
    }),
    http.put(`${testApiBaseUrl}/components/:id`, async ({ request }) => {
      const body = (await request.json()) as Record<string, unknown>;
      current = { ...current, ...body };
      return HttpResponse.json(current);
    }),
    http.delete(`${testApiBaseUrl}/components/:id`, () => {
      current = { ...current, archived: true };
      return new HttpResponse(null, { status: 204 });
    }),
    http.post(`${testApiBaseUrl}/components/:id/restore`, () => {
      current = { ...current, archived: false };
      return HttpResponse.json(current);
    }),
  );
}

function renderRoute(path: string) {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] });
  return { router, ...render(<App router={router} />) };
}

afterEach(() => {
  notifications.clean();
  window.localStorage.clear();
});

describe('component details and form', () => {
  it('shows separate value fields for each attribute ID even with identical names', async () => {
    useHandlers();
    server.use(
      http.get(`${testApiBaseUrl}/component-types/11/attributes`, () =>
        HttpResponse.json([attributes[0]!, { ...attributes[0]!, id: 9999 }]),
      ),
    );
    renderRoute('/components/501/edit');
    const fields = await screen.findAllByRole('textbox', { name: 'Сокет' }, { timeout: 5_000 });
    expect(fields).toHaveLength(2);
    expect(fields[0]).toHaveValue('AM5');
    expect(fields[1]).toHaveValue('');
  });

  it('creates a component with fields for every attribute data type', async () => {
    const user = userEvent.setup();
    let submittedBody: unknown;
    useHandlers();
    server.use(
      http.post(`${testApiBaseUrl}/components`, async ({ request }) => {
        submittedBody = await request.json();
        return HttpResponse.json(
          { ...component, ...(submittedBody as object), id: 777 },
          { status: 201 },
        );
      }),
    );
    const { router } = renderRoute('/components/new');

    await user.click(await screen.findByRole('textbox', { name: 'Название' }));
    await user.type(screen.getByRole('textbox', { name: 'Название' }), '  Ryzen 9  ');
    await user.type(screen.getByRole('textbox', { name: 'Бренд' }), ' AMD ');
    await user.click(screen.getByRole('combobox', { name: 'Тип компонента' }));
    await user.keyboard('{ArrowDown}{Enter}');

    await user.type(await screen.findByRole('textbox', { name: 'Сокет' }), ' AM5 ');
    await user.type(screen.getByRole('textbox', { name: 'TDP' }), '120.5');
    await user.click(screen.getByRole('combobox', { name: 'Разгон' }));
    await user.keyboard('{ArrowDown}{Enter}');
    await user.click(screen.getByRole('combobox', { name: 'Сегмент' }));
    await user.keyboard('{ArrowDown}{Enter}');
    await user.click(screen.getByRole('button', { name: 'Создать' }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/components/777'));
    expect(submittedBody).toEqual({
      componentTypeId: 11,
      name: 'Ryzen 9',
      brand: 'AMD',
      attributes: [
        { attributeDefinitionId: 1001, value: 'AM5' },
        { attributeDefinitionId: 1002, value: '120.5' },
        { attributeDefinitionId: 1003, value: 'true' },
        { attributeDefinitionId: 1004, value: 'Игровой' },
      ],
    });
    expect(await screen.findByText('Компонент создан')).toBeInTheDocument();
  });

  it('validates required and numeric attributes before sending the form', async () => {
    const user = userEvent.setup();
    useHandlers();
    renderRoute('/components/new');

    await user.click(await screen.findByRole('button', { name: 'Создать' }));
    expect(await screen.findByText('Выберите тип компонента')).toBeInTheDocument();

    await user.click(await screen.findByRole('combobox', { name: 'Тип компонента' }));
    await user.keyboard('{ArrowDown}{Enter}');
    await user.type(screen.getByRole('textbox', { name: 'Название' }), 'Ryzen');
    await user.type(await screen.findByRole('textbox', { name: 'TDP' }), 'not-a-number');
    await user.click(screen.getByRole('button', { name: 'Создать' }));

    expect(await screen.findByText('Заполните обязательную характеристику')).toBeInTheDocument();
    expect(screen.getByText('Введите число через точку')).toBeInTheDocument();
  });

  it('renders details through backend image URLs and edits without allowing type changes', async () => {
    const user = userEvent.setup();
    let updateBody: unknown;
    useHandlers();
    server.use(
      http.put(`${testApiBaseUrl}/components/:id`, async ({ request }) => {
        updateBody = await request.json();
        return HttpResponse.json({ ...component, ...(updateBody as object) });
      }),
    );
    const { container, router } = renderRoute('/components/501');

    expect(
      await screen.findByRole('heading', { level: 1, name: component.name }),
    ).toBeInTheDocument();
    expect(screen.getByText('Игровой процессор')).toBeInTheDocument();
    expect(screen.getByText('AM5')).toBeInTheDocument();
    await waitFor(() =>
      expect(container.querySelector('img')).toHaveAttribute(
        'src',
        '/api/component-images/9001/thumbnail',
      ),
    );

    await user.click(screen.getByRole('link', { name: 'Редактировать' }));
    const typeInput = await screen.findByRole('combobox', { name: 'Тип компонента' });
    expect(typeInput).toBeDisabled();
    const name = screen.getByRole('textbox', { name: 'Название' });
    await user.clear(name);
    await user.type(name, 'Ryzen 7 Pro');
    await user.click(screen.getByRole('button', { name: 'Сохранить' }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/components/501'));
    expect(updateBody).toMatchObject({
      componentTypeId: 11,
      name: 'Ryzen 7 Pro',
      attributes: [
        { attributeDefinitionId: 1001, value: 'AM5' },
        { attributeDefinitionId: 1002, value: '120' },
      ],
    });
  });

  it('guards unsaved changes during internal navigation', async () => {
    const user = userEvent.setup();
    useHandlers();
    const { router } = renderRoute('/components/new');

    await user.type(await screen.findByRole('textbox', { name: 'Название' }), 'Unsaved');
    await user.click(screen.getByRole('link', { name: 'К каталогу' }));
    const dialog = await screen.findByRole('dialog', { name: 'Выйти без сохранения?' });
    expect(router.state.location.pathname).toBe('/components/new');

    await user.click(within(dialog).getByRole('button', { name: 'Выйти' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/components'));
  });

  it('uploads, previews and permanently deletes component images', async () => {
    const user = userEvent.setup();
    let uploadContentType: string | null = null;
    let deletedImageId: string | null = null;
    useHandlers();
    server.use(
      http.post(testApiBaseUrl + '/components/:id/images', ({ request }) => {
        uploadContentType = request.headers.get('content-type');
        return HttpResponse.json(
          {
            id: 9002,
            url: '/component-images/9002/content',
            thumbnailUrl: '/component-images/9002/thumbnail',
            orderIndex: 1,
          },
          { status: 201 },
        );
      }),
      http.delete(testApiBaseUrl + '/component-images/:id', ({ params }) => {
        deletedImageId = String(params['id']);
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { container } = renderRoute('/components/501');

    const file = new File(['png-content'], 'component.png', { type: 'image/png' });
    await screen.findByLabelText('Новое изображение');
    await user.upload(container.querySelector('input[type="file"]')!, file);
    await user.click(screen.getByRole('button', { name: 'Загрузить' }));

    expect(await screen.findByText('Изображение загружено')).toBeInTheDocument();
    expect(uploadContentType).toMatch(/^multipart\/form-data; boundary=/);
    await user.click(screen.getByRole('button', { name: 'Открыть изображение 2' }));
    expect(await screen.findByRole('dialog', { name: 'Просмотр изображения' })).toBeInTheDocument();
    await user.keyboard('{Escape}');

    await user.click(screen.getByRole('button', { name: 'Удалить изображение 1' }));
    const dialog = await screen.findByRole('dialog', { name: 'Удалить изображение?' });
    await user.click(within(dialog).getByRole('button', { name: 'Удалить' }));

    expect(await screen.findByText('Изображение удалено')).toBeInTheDocument();
    expect(deletedImageId).toBe('9001');
  });

  it('keeps image order local until the complete order is saved', async () => {
    const user = userEvent.setup();
    let submittedOrder: unknown;
    useHandlers({
      component: {
        ...component,
        images: [
          {
            id: 9001,
            url: '/component-images/9001/content',
            thumbnailUrl: '/component-images/9001/thumbnail',
            orderIndex: 0,
          },
          {
            id: 9002,
            url: '/component-images/9002/content',
            thumbnailUrl: '/component-images/9002/thumbnail',
            orderIndex: 1,
          },
        ],
      },
    });
    server.use(
      http.put(testApiBaseUrl + '/components/:id/images/order', async ({ request }) => {
        submittedOrder = await request.json();
        return HttpResponse.json([
          {
            id: 9002,
            url: '/component-images/9002/content',
            thumbnailUrl: '/component-images/9002/thumbnail',
            orderIndex: 0,
          },
          {
            id: 9001,
            url: '/component-images/9001/content',
            thumbnailUrl: '/component-images/9001/thumbnail',
            orderIndex: 1,
          },
        ]);
      }),
    );
    renderRoute('/components/501');

    await user.click(await screen.findByRole('button', { name: 'Изменить порядок' }));
    expect(screen.getByRole('button', { name: 'Загрузить' })).toBeDisabled();
    await user.click(screen.getAllByRole('button', { name: 'Переместить позже' })[0]!);
    expect(submittedOrder).toBeUndefined();
    await user.click(screen.getByRole('button', { name: 'Отменить изменения' }));
    expect(submittedOrder).toBeUndefined();

    await user.click(screen.getByRole('button', { name: 'Изменить порядок' }));
    await user.click(screen.getAllByRole('button', { name: 'Переместить позже' })[0]!);
    await user.click(screen.getByRole('button', { name: 'Сохранить порядок' }));

    expect(await screen.findByText('Порядок изображений сохранён')).toBeInTheDocument();
    expect(submittedOrder).toEqual({ imageIds: [9002, 9001] });
  });

  it('makes a chosen image primary and preserves the relative order of the other images', async () => {
    const user = userEvent.setup();
    const images = [1, 2, 3].map((n) => ({
      id: 9000 + n,
      url: `/component-images/${9000 + n}/content`,
      thumbnailUrl: `/component-images/${9000 + n}/thumbnail`,
      orderIndex: n - 1,
    }));
    useHandlers({ component: { ...component, images } });
    let submittedOrder: unknown;
    server.use(
      http.put(testApiBaseUrl + '/components/:id/images/order', async ({ request }) => {
        submittedOrder = await request.json();
        return HttpResponse.json(
          [images[2], images[0], images[1]].map((image, orderIndex) => ({ ...image, orderIndex })),
        );
      }),
    );
    renderRoute('/components/501');
    await user.click(
      await screen.findByRole('button', { name: 'Сделать изображение 3 заглавным' }),
    );
    expect(await screen.findByText('Заглавное изображение изменено')).toBeInTheDocument();
    expect(submittedOrder).toEqual({ imageIds: [9003, 9001, 9002] });
    expect(
      within(screen.getByRole('button', { name: 'Открыть изображение 1' })).getByRole('img'),
    ).toHaveAttribute('src', '/api/component-images/9003/thumbnail');
    expect(screen.getByText('Заглавное', { exact: true })).toBeInTheDocument();
  });

  it('retains the saved primary image when the server rejects a stale selection', async () => {
    const user = userEvent.setup();
    const images = [1, 2].map((n) => ({
      id: 9000 + n,
      url: `/component-images/${9000 + n}/content`,
      thumbnailUrl: `/component-images/${9000 + n}/thumbnail`,
      orderIndex: n - 1,
    }));
    useHandlers({ component: { ...component, images } });
    server.use(
      http.put(testApiBaseUrl + '/components/:id/images/order', () =>
        HttpResponse.json({ message: 'Image order changed' }, { status: 400 }),
      ),
    );
    renderRoute('/components/501');
    await user.click(
      await screen.findByRole('button', { name: 'Сделать изображение 2 заглавным' }),
    );
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Сделать изображение 2 заглавным' })).toBeEnabled(),
    );
    expect(
      within(screen.getByRole('button', { name: 'Открыть изображение 1' })).getByRole('img'),
    ).toHaveAttribute('src', '/api/component-images/9001/thumbnail');
    expect(screen.queryByText('Заглавное изображение изменено')).not.toBeInTheDocument();
  });

  it('rejects unsupported files before upload', async () => {
    const user = userEvent.setup({ applyAccept: false });
    useHandlers();
    const { container } = renderRoute('/components/501');

    await screen.findByLabelText('Новое изображение');
    await user.upload(
      container.querySelector('input[type="file"]')!,
      new File(['gif'], 'component.gif', { type: 'image/gif' }),
    );

    expect(await screen.findByText('Поддерживаются только JPEG, PNG и WebP')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Загрузить' })).toBeDisabled();
  });

  it('isolates a retryable gallery failure from the rest of component details', async () => {
    const error: ErrorResponse = {
      timestamp: '2026-08-23T12:00:00Z',
      status: 503,
      error: 'Service Unavailable',
      code: 'EXTERNAL_STORAGE_UNAVAILABLE',
      message: 'Image storage is temporarily unavailable',
      path: '/components/501/images',
      details: [],
    };
    useHandlers();
    server.use(
      http.get(testApiBaseUrl + '/components/:id/images', () =>
        HttpResponse.json(error, { status: 503 }),
      ),
    );
    renderRoute('/components/501');

    expect(await screen.findByText('Игровой процессор')).toBeInTheDocument();
    expect(
      await screen.findByText('Хранилище изображений недоступно', {}, { timeout: 3_000 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Повторить' })).toBeInTheDocument();
  });

  it('keeps archived details read-only and restores the component', async () => {
    const user = userEvent.setup();
    useHandlers({ component: { ...component, archived: true } });
    renderRoute('/components/501');

    expect(await screen.findByText('В архиве')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Редактировать' })).not.toBeInTheDocument();
    expect(screen.getByText('Архивная галерея доступна только для просмотра.')).toBeInTheDocument();
    expect(screen.queryByLabelText('Новое изображение')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Удалить изображение 1' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Восстановить' }));

    expect(await screen.findByText('Компонент восстановлен')).toBeInTheDocument();
    expect(await screen.findByRole('link', { name: 'Редактировать' })).toBeInTheDocument();
  });

  it('archives an active component only after confirmation', async () => {
    const user = userEvent.setup();
    useHandlers();
    renderRoute('/components/501');

    await user.click(await screen.findByRole('button', { name: 'В архив' }));
    const dialog = await screen.findByRole('dialog', { name: 'Архивировать компонент?' });
    await user.click(within(dialog).getByRole('button', { name: 'В архив' }));

    expect(await screen.findByText('Компонент перемещён в архив')).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Восстановить' })).toBeInTheDocument();
  });

  it('shows a safe empty state for an invalid component id', async () => {
    useHandlers();
    renderRoute('/components/not-a-number');
    expect(await screen.findByRole('heading', { name: 'Компонент не найден' })).toBeInTheDocument();
  });

  it('directs an empty catalog to component type settings', async () => {
    server.use(
      http.get(`${testApiBaseUrl}/domains/:domainId/component-types`, () => HttpResponse.json([])),
    );
    renderRoute('/components/new');
    expect(
      await screen.findByRole('heading', { name: 'Сначала создайте тип компонента' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Настроить типы' })).toHaveAttribute(
      'href',
      '/settings/types',
    );
  });

  it('binds structured backend name errors to the form', async () => {
    const user = userEvent.setup();
    const error: ErrorResponse = {
      timestamp: '2026-08-09T12:00:00Z',
      status: 409,
      error: 'Conflict',
      code: 'ENTITY_ALREADY_EXISTS',
      message: 'Component name must be unique',
      path: '/components',
      details: [{ code: 'DUPLICATE', field: 'name', message: 'Название уже используется' }],
    };
    useHandlers();
    server.use(
      http.post(`${testApiBaseUrl}/components`, () => HttpResponse.json(error, { status: 409 })),
    );
    renderRoute('/components/new');

    await user.click(await screen.findByRole('combobox', { name: 'Тип компонента' }));
    await user.keyboard('{ArrowDown}{Enter}');
    await user.type(screen.getByRole('textbox', { name: 'Название' }), 'Ryzen');
    await user.type(await screen.findByRole('textbox', { name: 'Сокет' }), 'AM5');
    await user.click(screen.getByRole('button', { name: 'Создать' }));

    expect(await screen.findByText('Название уже используется')).toBeInTheDocument();
  });
});
