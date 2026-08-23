import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { AppProviders } from '@/app/providers/AppProviders';
import { DeleteConfigurationModal } from '@/features/configurations/ui/DeleteConfigurationModal';
import type { Configuration } from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

const configuration: Configuration = {
  id: 91,
  domainId: 101,
  name: 'Рабочая станция',
  createdAt: '2026-08-23T10:00:00Z',
  components: [],
};

describe('DeleteConfigurationModal', () => {
  it('keeps the confirmation open with an inline normalized delete error', async () => {
    const user = userEvent.setup();
    const onDeleted = vi.fn();
    let requests = 0;
    server.use(
      http.delete(`${testApiBaseUrl}/configurations/91`, () => {
        requests += 1;
        return HttpResponse.json(
          {
            timestamp: '2026-08-23T12:00:00Z',
            status: 404,
            error: 'Not Found',
            code: 'NOT_FOUND',
            message: 'Конфигурация уже удалена',
            path: '/configurations/91',
            details: [],
          },
          { status: 404 },
        );
      }),
    );
    render(
      <AppProviders>
        <DeleteConfigurationModal
          configuration={configuration}
          onClose={vi.fn()}
          onDeleted={onDeleted}
        />
      </AppProviders>,
    );
    const dialog = await screen.findByRole('dialog', { name: 'Удалить конфигурацию?' });

    await user.click(within(dialog).getByRole('button', { name: 'Удалить' }));

    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Конфигурация уже удалена');
    expect(requests).toBe(1);
    expect(onDeleted).not.toHaveBeenCalled();
    expect(dialog).toBeVisible();
  });
});
