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
          componentIds={[7]}
          components={[{ id: 7, name: 'Ryzen', typeName: 'Процессор', brand: 'AMD' }]}
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
});
