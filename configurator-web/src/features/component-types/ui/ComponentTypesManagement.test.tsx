import { notifications } from '@mantine/notifications';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse, type HttpResponseResolver } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { createMemoryRouter } from 'react-router-dom';

import { App } from '@/app/App';
import { appRoutes } from '@/app/router/routes';
import type {
  AttributeDefinition,
  ComponentType,
  CreateAttributeDefinitionRequest,
  CreateComponentTypeRequest,
  ErrorResponse,
} from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

const domainId = 101;
const processorType: ComponentType = {
  id: 11,
  domainId,
  name: 'Процессор',
  code: 'CPU',
  description: 'Центральный процессор',
  orderIndex: 1,
};
const graphicsType: ComponentType = {
  id: 12,
  domainId,
  name: 'Видеокарта',
  code: 'GPU',
  orderIndex: 2,
};
const coresAttribute: AttributeDefinition = {
  id: 1011,
  componentTypeId: 11,
  name: 'cores',
  label: 'Количество ядер',
  dataType: 'NUMBER',
  isRequired: true,
  orderIndex: 1,
};
const memoryAttribute: AttributeDefinition = {
  id: 1012,
  componentTypeId: 12,
  name: 'memory_type',
  label: 'Тип памяти',
  dataType: 'ENUM',
  enumValues: ['GDDR6', 'GDDR7'],
  isRequired: false,
  orderIndex: 1,
};

function renderPage() {
  const router = createMemoryRouter(appRoutes, { initialEntries: ['/settings/types'] });
  return render(<App router={router} />);
}

function useHandlers(
  componentTypes: Array<ComponentType>,
  attributesByType: Map<number, Array<AttributeDefinition>>,
  overrides: {
    createType?: HttpResponseResolver;
    deleteType?: HttpResponseResolver;
    createAttribute?: HttpResponseResolver;
    updateAttribute?: HttpResponseResolver;
  } = {},
) {
  server.use(
    http.get(`${testApiBaseUrl}/domains/:domainId/component-types`, () =>
      HttpResponse.json(componentTypes),
    ),
    http.post(
      `${testApiBaseUrl}/domains/:domainId/component-types`,
      overrides.createType ??
        (async ({ request }) => {
          const body = (await request.json()) as CreateComponentTypeRequest;
          const created: ComponentType = { id: 21, domainId, ...body };
          componentTypes.push(created);
          return HttpResponse.json(created, { status: 201 });
        }),
    ),
    http.put(`${testApiBaseUrl}/component-types/:id`, async ({ params, request }) => {
      const body = (await request.json()) as CreateComponentTypeRequest;
      const current = componentTypes.find((type) => type.id === Number(params['id']));
      if (!current) {
        return new HttpResponse(null, { status: 404 });
      }
      const updated: ComponentType = { id: current.id, domainId: current.domainId, ...body };
      componentTypes.splice(componentTypes.indexOf(current), 1, updated);
      return HttpResponse.json(updated);
    }),
    http.delete(
      `${testApiBaseUrl}/component-types/:id`,
      overrides.deleteType ??
        (({ params }) => {
          const index = componentTypes.findIndex((type) => type.id === Number(params['id']));
          if (index >= 0) {
            componentTypes.splice(index, 1);
          }
          return new HttpResponse(null, { status: 204 });
        }),
    ),
    http.get(`${testApiBaseUrl}/component-types/:id/attributes`, ({ params }) =>
      HttpResponse.json(attributesByType.get(Number(params['id'])) ?? []),
    ),
    http.post(
      `${testApiBaseUrl}/component-types/:id/attributes`,
      overrides.createAttribute ??
        (async ({ params, request }) => {
          const componentTypeId = Number(params['id']);
          const body = (await request.json()) as CreateAttributeDefinitionRequest;
          const created: AttributeDefinition = {
            id: 2021,
            componentTypeId,
            isRequired: false,
            ...body,
          };
          const attributes = attributesByType.get(componentTypeId) ?? [];
          attributes.push(created);
          attributesByType.set(componentTypeId, attributes);
          return HttpResponse.json(created, { status: 201 });
        }),
    ),
    http.put(
      `${testApiBaseUrl}/attributes/:id`,
      overrides.updateAttribute ??
        (async ({ params, request }) => {
          const id = Number(params['id']);
          const body = (await request.json()) as CreateAttributeDefinitionRequest;
          for (const attributes of attributesByType.values()) {
            const current = attributes.find((attribute) => attribute.id === id);
            if (current) {
              const updated: AttributeDefinition = {
                id,
                componentTypeId: current.componentTypeId,
                isRequired: false,
                ...body,
              };
              attributes.splice(attributes.indexOf(current), 1, updated);
              return HttpResponse.json(updated);
            }
          }
          return new HttpResponse(null, { status: 404 });
        }),
    ),
  );
}

afterEach(() => {
  notifications.clean();
});

describe('component types and attributes management', () => {
  it('creates the first component type with validation and trimmed optional values', async () => {
    const user = userEvent.setup();
    const componentTypes: Array<ComponentType> = [];
    let submitted: CreateComponentTypeRequest | undefined;
    useHandlers(componentTypes, new Map(), {
      createType: async ({ request }) => {
        submitted = (await request.json()) as CreateComponentTypeRequest;
        const created: ComponentType = { id: 21, domainId, ...submitted };
        componentTypes.push(created);
        return HttpResponse.json(created, { status: 201 });
      },
    });
    renderPage();

    expect(
      await screen.findByRole('heading', { name: 'Типов компонентов пока нет' }),
    ).toBeInTheDocument();
    await user.click(screen.getAllByRole('button', { name: 'Новый тип' })[0]!);
    const dialog = await screen.findByRole('dialog', { name: 'Новый тип компонента' });
    await user.click(within(dialog).getByRole('button', { name: 'Создать' }));
    expect(await within(dialog).findByText('Введите название')).toBeInTheDocument();

    await user.type(within(dialog).getByRole('textbox', { name: 'Название' }), '  Процессор  ');
    await user.type(within(dialog).getByRole('textbox', { name: 'Код' }), '  CPU  ');
    await user.type(
      within(dialog).getByRole('textbox', { name: 'Описание' }),
      '  Центральный процессор  ',
    );
    await user.type(within(dialog).getByRole('textbox', { name: /Индекс порядка/ }), '2');
    await user.click(within(dialog).getByRole('button', { name: 'Создать' }));

    expect(await screen.findByRole('heading', { level: 2, name: 'Процессор' })).toBeInTheDocument();
    expect(submitted).toEqual({
      name: 'Процессор',
      code: 'CPU',
      description: 'Центральный процессор',
      orderIndex: 2,
    });
    expect(await screen.findByText('Тип компонента создан')).toBeInTheDocument();
  });

  it('switches types and loads attributes from type-scoped caches', async () => {
    const user = userEvent.setup();
    useHandlers(
      [processorType, graphicsType],
      new Map([
        [processorType.id, [coresAttribute]],
        [graphicsType.id, [memoryAttribute]],
      ]),
    );
    renderPage();

    expect(await screen.findByText('Количество ядер')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Видеокарта/ }));

    expect(await screen.findByText('Тип памяти')).toBeInTheDocument();
    expect(screen.getByText('Варианты: GDDR6, GDDR7')).toBeInTheDocument();
    expect(screen.queryByText('Количество ядер')).not.toBeInTheDocument();
  });

  it('edits a type and deletes it with fallback to the remaining type', async () => {
    const user = userEvent.setup();
    const componentTypes = [{ ...processorType }, { ...graphicsType }];
    useHandlers(componentTypes, new Map());
    renderPage();

    await screen.findByRole('heading', { level: 2, name: 'Процессор' });
    await user.click(screen.getByRole('button', { name: 'Редактировать тип Процессор' }));
    const editDialog = await screen.findByRole('dialog', { name: 'Редактирование типа' });
    const name = within(editDialog).getByRole('textbox', { name: 'Название' });
    await user.clear(name);
    await user.type(name, 'Центральный процессор');
    await user.click(within(editDialog).getByRole('button', { name: 'Сохранить' }));
    expect(
      await screen.findByRole('heading', { level: 2, name: 'Центральный процессор' }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Видеокарта/ }));
    await user.click(screen.getByRole('button', { name: 'Удалить тип Видеокарта' }));
    const deleteDialog = await screen.findByRole('dialog', { name: 'Удалить тип компонента?' });
    expect(within(deleteDialog).getByText(/Действие необратимо/)).toBeInTheDocument();
    await user.click(within(deleteDialog).getByRole('button', { name: 'Удалить тип' }));

    expect(
      await screen.findByRole('heading', { level: 2, name: 'Центральный процессор' }),
    ).toBeInTheDocument();
    expect(screen.queryByText('Видеокарта')).not.toBeInTheDocument();
  });

  it('keeps a type and confirmation open when related data causes a conflict', async () => {
    const user = userEvent.setup();
    const conflict: ErrorResponse = {
      timestamp: '2026-08-09T12:00:00Z',
      status: 409,
      error: 'Conflict',
      code: 'ENTITY_HAS_RELATED_ENTITIES',
      message: 'Type has related entities',
      path: '/component-types/11',
      details: [],
    };
    useHandlers([{ ...processorType }], new Map(), {
      deleteType: vi.fn(() => HttpResponse.json(conflict, { status: 409 })),
    });
    renderPage();

    await screen.findByRole('heading', { level: 2, name: 'Процессор' });
    await user.click(screen.getByRole('button', { name: 'Удалить тип Процессор' }));
    const dialog = await screen.findByRole('dialog', { name: 'Удалить тип компонента?' });
    await user.click(within(dialog).getByRole('button', { name: 'Удалить тип' }));

    expect(await screen.findByText('Запись используется другими данными')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Процессор' })).toBeInTheDocument();
    expect(screen.getByRole('dialog', { name: 'Удалить тип компонента?' })).toBeInTheDocument();
  });

  it('creates an enum attribute, validates its options and exposes no unavailable delete action', async () => {
    const user = userEvent.setup();
    const attributes = new Map<number, Array<AttributeDefinition>>([[processorType.id, []]]);
    let submitted: CreateAttributeDefinitionRequest | undefined;
    useHandlers([{ ...processorType }], attributes, {
      createAttribute: async ({ params, request }) => {
        submitted = (await request.json()) as CreateAttributeDefinitionRequest;
        const created: AttributeDefinition = {
          id: 2021,
          componentTypeId: Number(params['id']),
          isRequired: false,
          ...submitted,
        };
        attributes.get(processorType.id)!.push(created);
        return HttpResponse.json(created, { status: 201 });
      },
    });
    renderPage();

    await screen.findByRole('heading', { name: 'Атрибутов пока нет' });
    await user.click(screen.getAllByRole('button', { name: 'Добавить атрибут' })[0]!);
    const dialog = await screen.findByRole('dialog', { name: 'Новый атрибут' });
    await user.type(within(dialog).getByRole('textbox', { name: /Системное имя/ }), ' socket ');
    await user.type(
      within(dialog).getByRole('textbox', { name: 'Название для пользователя' }),
      ' Сокет ',
    );
    const dataType = within(dialog).getByRole('combobox', { name: 'Тип данных' });
    await user.click(dataType);
    await user.keyboard('{ArrowDown}{ArrowDown}{ArrowDown}{Enter}');
    expect(dataType).toHaveValue('Список');
    await user.click(within(dialog).getByRole('button', { name: 'Создать' }));
    expect(
      await within(dialog).findByText('Добавьте хотя бы одно допустимое значение'),
    ).toBeInTheDocument();

    const enumValues = within(dialog).getByRole('combobox', { name: /Допустимые значения/ });
    await user.type(enumValues, 'AM4{Enter}');
    await user.type(enumValues, 'AM5{Enter}');
    await user.click(within(dialog).getByRole('switch', { name: 'Обязательный атрибут' }));
    await user.click(within(dialog).getByRole('button', { name: 'Создать' }));

    expect(await screen.findByText('Сокет')).toBeInTheDocument();
    expect(submitted).toEqual({
      name: 'socket',
      label: 'Сокет',
      dataType: 'ENUM',
      enumValues: ['AM4', 'AM5'],
      isRequired: true,
    });
    expect(screen.queryByRole('button', { name: /Удалить атрибут/ })).not.toBeInTheDocument();
  });

  it('edits an attribute and keeps the form open when backend rejects a data type change', async () => {
    const user = userEvent.setup();
    const validationError: ErrorResponse = {
      timestamp: '2026-08-09T12:00:00Z',
      status: 400,
      error: 'Bad Request',
      code: 'VALIDATION_ERROR',
      message: 'Cannot change data type because persisted values exist',
      path: '/attributes/1011',
      details: [],
    };
    useHandlers([{ ...processorType }], new Map([[processorType.id, [{ ...coresAttribute }]]]), {
      updateAttribute: () => HttpResponse.json(validationError, { status: 400 }),
    });
    renderPage();

    await screen.findByText('Количество ядер');
    await user.click(screen.getByRole('button', { name: 'Редактировать атрибут Количество ядер' }));
    const dialog = await screen.findByRole('dialog', { name: 'Редактирование атрибута' });
    const dataType = within(dialog).getByRole('combobox', { name: 'Тип данных' });
    await user.click(dataType);
    await user.keyboard('{ArrowUp}{Enter}');
    expect(dataType).toHaveValue('Текст');
    await user.click(within(dialog).getByRole('button', { name: 'Сохранить' }));

    expect(await screen.findByText('Проверьте введённые данные')).toBeInTheDocument();
    expect(screen.getByRole('dialog', { name: 'Редактирование атрибута' })).toBeInTheDocument();
  });

  it('binds a structured backend name error to the type form', async () => {
    const user = userEvent.setup();
    const fieldError: ErrorResponse = {
      timestamp: '2026-08-09T12:00:00Z',
      status: 409,
      error: 'Conflict',
      code: 'ENTITY_ALREADY_EXISTS',
      message: 'Duplicate type',
      path: '/domains/101/component-types',
      details: [{ field: 'name', code: 'DUPLICATE', message: 'Название уже используется' }],
    };
    useHandlers([], new Map(), {
      createType: () => HttpResponse.json(fieldError, { status: 409 }),
    });
    renderPage();

    await screen.findByRole('heading', { name: 'Типов компонентов пока нет' });
    await user.click(screen.getAllByRole('button', { name: 'Новый тип' })[0]!);
    const dialog = await screen.findByRole('dialog', { name: 'Новый тип компонента' });
    await user.type(within(dialog).getByRole('textbox', { name: 'Название' }), 'Процессор');
    await user.click(within(dialog).getByRole('button', { name: 'Создать' }));

    expect(await within(dialog).findByText('Название уже используется')).toBeInTheDocument();
  });
});
