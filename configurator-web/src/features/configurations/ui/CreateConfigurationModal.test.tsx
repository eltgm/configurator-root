import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { AppProviders } from '@/app/providers/AppProviders';
import { CreateConfigurationModal } from '@/features/configurations/ui/CreateConfigurationModal';
import { server, testApiBaseUrl } from '@/test/server';

describe('CreateConfigurationModal', () => {
  it('validates metadata and keeps the form open with a backend field error', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    server.use(
      http.post(`${testApiBaseUrl}/domains/101/configurations`, () =>
        HttpResponse.json(
          {
            timestamp: '2026-08-23T12:00:00Z',
            status: 400,
            error: 'Bad Request',
            code: 'VALIDATION_ERROR',
            message: 'Validation failed',
            path: '/domains/101/configurations',
            details: [{ field: 'name', code: 'INVALID_VALUE', message: 'Название уже занято' }],
          },
          { status: 400 },
        ),
      ),
    );
    render(
      <AppProviders>
        <CreateConfigurationModal
          opened
          domainId={101}
          trackInventory={false}
          componentItems={[{ componentId: 7, quantity: 1 }]}
          components={[
            {
              id: 7,
              name: 'Ryzen',
              typeName: 'Процессор',
              brand: 'AMD',
              quantity: 1,
              availableQuantity: 0,
            },
          ]}
          onClose={onClose}
          onSaved={vi.fn()}
        />
      </AppProviders>,
    );
    const dialog = screen.getByRole('dialog', { name: 'Сохранение конфигурации' });

    await user.click(within(dialog).getByRole('button', { name: 'Сохранить конфигурацию' }));
    expect(await within(dialog).findByText('Введите название')).toBeInTheDocument();
    await user.type(within(dialog).getByRole('textbox', { name: /Название/ }), 'Домашний ПК');
    await user.click(within(dialog).getByRole('button', { name: 'Сохранить конфигурацию' }));

    expect(await within(dialog).findByText('Название уже занято')).toBeInTheDocument();
    expect(within(dialog).getByDisplayValue('Домашний ПК')).toBeInTheDocument();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('preserves prefilled copy metadata and shows a non-field conflict', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(`${testApiBaseUrl}/domains/101/configurations`, () =>
        HttpResponse.json(
          {
            timestamp: '2026-08-23T12:00:00Z',
            status: 409,
            error: 'Conflict',
            code: 'CONFIGURATION_CONFLICT',
            message: 'Состав больше не совместим напрямую',
            path: '/domains/101/configurations',
            details: [],
          },
          { status: 409 },
        ),
      ),
    );
    render(
      <AppProviders>
        <CreateConfigurationModal
          opened
          mode="copy"
          domainId={101}
          trackInventory={false}
          componentItems={[
            { componentId: 7, quantity: 1 },
            { componentId: 8, quantity: 1 },
          ]}
          components={[
            {
              id: 7,
              name: 'Ryzen',
              typeName: 'Процессор',
              brand: 'AMD',
              quantity: 1,
              availableQuantity: 0,
            },
          ]}
          initialValues={{
            name: 'Домашний ПК — копия',
            description: 'Тихая сборка',
          }}
          onClose={vi.fn()}
          onSaved={vi.fn()}
        />
      </AppProviders>,
    );
    const dialog = screen.getByRole('dialog', { name: 'Копирование конфигурации' });

    await user.click(within(dialog).getByRole('button', { name: 'Создать копию' }));

    expect(await within(dialog).findByRole('alert')).toHaveTextContent(
      'Состав больше не совместим напрямую',
    );
    expect(within(dialog).getByRole('textbox', { name: /Название/ })).toHaveValue(
      'Домашний ПК — копия',
    );
    expect(within(dialog).getByRole('textbox', { name: 'Описание' })).toHaveValue('Тихая сборка');
  });

  it('shows every unavailable component using the current site language', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(`${testApiBaseUrl}/domains/101/configurations`, () =>
        HttpResponse.json(
          {
            timestamp: '2026-09-08T12:00:00Z',
            status: 409,
            error: 'Conflict',
            code: 'INSUFFICIENT_COMPONENT_AVAILABILITY',
            message: 'Insufficient component availability',
            path: '/domains/101/configurations',
            details: [
              {
                code: 'INSUFFICIENT_COMPONENT_AVAILABILITY',
                message: 'Insufficient component availability',
                parameters: {
                  componentId: '7',
                  componentName: 'Ryzen',
                  requestedQuantity: '3',
                  availableQuantity: '2',
                },
              },
              {
                code: 'INSUFFICIENT_COMPONENT_AVAILABILITY',
                message: 'Insufficient component availability',
                parameters: {
                  componentId: '8',
                  componentName: 'GeForce',
                  requestedQuantity: '2',
                  availableQuantity: '0',
                },
              },
            ],
          },
          { status: 409 },
        ),
      ),
    );
    render(
      <AppProviders>
        <CreateConfigurationModal
          opened
          domainId={101}
          trackInventory
          componentItems={[
            { componentId: 7, quantity: 3 },
            { componentId: 8, quantity: 2 },
          ]}
          components={[
            {
              id: 7,
              name: 'Ryzen',
              typeName: 'Процессор',
              quantity: 3,
              availableQuantity: 3,
            },
            {
              id: 8,
              name: 'GeForce',
              typeName: 'Видеокарта',
              quantity: 2,
              availableQuantity: 2,
            },
          ]}
          initialValues={{ name: 'Игровой ПК', description: '' }}
          onClose={vi.fn()}
          onSaved={vi.fn()}
        />
      </AppProviders>,
    );
    const dialog = screen.getByRole('dialog', { name: 'Сохранение конфигурации' });

    await user.click(within(dialog).getByRole('button', { name: 'Сохранить конфигурацию' }));

    const alert = await within(dialog).findByRole('alert');
    expect(alert).toHaveTextContent('Недостаточно компонентов');
    expect(alert).toHaveTextContent('«Ryzen»: требуется 3, доступно 2.');
    expect(alert).toHaveTextContent('«GeForce»: требуется 2, доступно 0.');
    expect(alert).not.toHaveTextContent('Insufficient component availability');
  });

  it('blocks a tracked copy before submitting when the requested quantity exceeds stock', () => {
    render(
      <AppProviders>
        <CreateConfigurationModal
          opened
          mode="copy"
          domainId={101}
          trackInventory
          componentItems={[{ componentId: 7, quantity: 3 }]}
          components={[
            {
              id: 7,
              name: 'Ryzen',
              typeName: 'Процессор',
              quantity: 3,
              availableQuantity: 2,
            },
          ]}
          initialValues={{ name: 'Игровой ПК — копия', description: '' }}
          onClose={vi.fn()}
          onSaved={vi.fn()}
        />
      </AppProviders>,
    );

    const dialog = screen.getByRole('dialog', { name: 'Копирование конфигурации' });
    expect(within(dialog).getByText('Доступно: 2 · выбрано: 3')).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: 'Создать копию' })).toBeDisabled();
    expect(within(dialog).queryByRole('checkbox')).toBeNull();
  });
});
