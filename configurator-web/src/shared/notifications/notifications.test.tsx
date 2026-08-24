import { notifications } from '@mantine/notifications';
import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';

import { AppProviders } from '@/app/providers/AppProviders';
import type { ErrorResponse } from '@/shared/api';
import {
  showErrorNotification,
  showSuccessNotification,
} from '@/shared/notifications/notifications';

afterEach(() => {
  notifications.clean();
});

describe('application notifications', () => {
  it('shows a localized API error with the safe backend message', async () => {
    const error: ErrorResponse = {
      timestamp: '2026-08-09T12:00:00Z',
      status: 409,
      error: 'Conflict',
      code: 'ENTITY_ALREADY_EXISTS',
      message: 'Предметная область уже существует',
      path: '/domains',
      details: [],
    };
    render(
      <AppProviders>
        <div />
      </AppProviders>,
    );

    showErrorNotification(error);

    expect(await screen.findByText('Такая запись уже существует')).toBeInTheDocument();
    expect(screen.getByText('Предметная область уже существует')).toBeInTheDocument();
  });

  it('shows an explicit success notification', async () => {
    render(
      <AppProviders>
        <div />
      </AppProviders>,
    );

    showSuccessNotification('Сохранено', 'Изменения применены');

    expect(await screen.findByText('Сохранено')).toBeInTheDocument();
    expect(screen.getByText('Изменения применены')).toBeInTheDocument();
  });
});
