import { notifications } from '@mantine/notifications';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';
import { createMemoryRouter } from 'react-router-dom';

import { App } from '@/app/App';
import { appRoutes } from '@/app/router/routes';
import type { AttributeDefinition, CreateAttributeDefinitionRequest } from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

const domainId = 101;

function renderPage() {
  return render(
    <App router={createMemoryRouter(appRoutes, { initialEntries: ['/settings/attributes'] })} />,
  );
}

afterEach(() => notifications.clean());

describe('attribute catalog page', () => {
  it('creates and deletes catalog attributes with destructive confirmation', async () => {
    const user = userEvent.setup();
    const catalog: Array<AttributeDefinition> = [];
    server.use(
      http.get(`${testApiBaseUrl}/domains/:domainId/attributes`, () => HttpResponse.json(catalog)),
      http.post(`${testApiBaseUrl}/domains/:domainId/attributes`, async ({ request }) => {
        const body = (await request.json()) as CreateAttributeDefinitionRequest;
        const created: AttributeDefinition = {
          id: 501,
          domainId,
          componentTypeIds: [],
          ...body,
        };
        catalog.push(created);
        return HttpResponse.json(created, { status: 201 });
      }),
      http.delete(`${testApiBaseUrl}/attributes/:id`, ({ params }) => {
        const index = catalog.findIndex((attribute) => attribute.id === Number(params['id']));
        catalog.splice(index, 1);
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Каталог атрибутов пуст' })).toBeVisible();
    await user.click(screen.getAllByRole('button', { name: 'Новый атрибут' })[0]!);
    const createDialog = await screen.findByRole('dialog', { name: 'Новый атрибут' });
    await user.type(within(createDialog).getByRole('textbox', { name: /Системное имя/ }), 'socket');
    await user.type(
      within(createDialog).getByRole('textbox', { name: 'Название для пользователя' }),
      'Сокет',
    );
    await user.click(within(createDialog).getByRole('button', { name: 'Создать' }));

    expect(await screen.findByRole('heading', { name: 'Сокет' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Удалить атрибут Сокет' }));
    const deleteDialog = await screen.findByRole('dialog', {
      name: 'Удалить атрибут из каталога?',
    });
    expect(within(deleteDialog).getByText(/Все связи и значения/)).toBeInTheDocument();
    await user.click(within(deleteDialog).getByRole('button', { name: 'Удалить атрибут' }));

    expect(await screen.findByRole('heading', { name: 'Каталог атрибутов пуст' })).toBeVisible();
  });

  it('updates shared fields and keeps a rule-referenced attribute after a delete conflict', async () => {
    const user = userEvent.setup();
    let attribute: AttributeDefinition = {
      id: 601,
      domainId,
      name: 'socket',
      label: 'Сокет',
      dataType: 'STRING',
      componentTypeIds: [11],
    };
    server.use(
      http.get(`${testApiBaseUrl}/domains/:domainId/attributes`, () =>
        HttpResponse.json([attribute]),
      ),
      http.put(`${testApiBaseUrl}/attributes/:id`, async ({ request }) => {
        attribute = {
          ...attribute,
          ...((await request.json()) as CreateAttributeDefinitionRequest),
        };
        return HttpResponse.json(attribute);
      }),
      http.delete(`${testApiBaseUrl}/attributes/:id`, () =>
        HttpResponse.json(
          {
            timestamp: '2026-08-25T12:00:00Z',
            status: 409,
            error: 'Conflict',
            code: 'ENTITY_HAS_RELATED_ENTITIES',
            message: 'Attribute is used by a compatibility rule',
            path: '/attributes/601',
            details: [],
          },
          { status: 409 },
        ),
      ),
    );
    renderPage();

    await screen.findByRole('heading', { name: 'Сокет' });
    await user.click(screen.getByRole('button', { name: 'Редактировать атрибут Сокет' }));
    const editDialog = await screen.findByRole('dialog', { name: 'Редактирование атрибута' });
    const label = within(editDialog).getByRole('textbox', { name: 'Название для пользователя' });
    await user.clear(label);
    await user.type(label, 'Разъём');
    await user.click(within(editDialog).getByRole('button', { name: 'Сохранить' }));
    expect(await screen.findByRole('heading', { name: 'Разъём' })).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Удалить атрибут Разъём' }));
    const deleteDialog = await screen.findByRole('dialog', {
      name: 'Удалить атрибут из каталога?',
    });
    await user.click(within(deleteDialog).getByRole('button', { name: 'Удалить атрибут' }));

    expect(await screen.findByText('Запись используется другими данными')).toBeVisible();
    expect(screen.getByRole('dialog', { name: 'Удалить атрибут из каталога?' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Разъём' })).toBeVisible();
  });
});
