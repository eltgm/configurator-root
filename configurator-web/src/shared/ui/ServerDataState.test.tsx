import { Button } from '@mantine/core';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { AppProviders } from '@/app/providers/AppProviders';
import type { ErrorResponse } from '@/shared/api';
import { PageHeader, ServerDataState } from '@/shared/ui';

function renderWithProviders(component: React.ReactNode) {
  return render(<AppProviders>{component}</AppProviders>);
}

describe('shared server-driven UI states', () => {
  it('renders an accessible loading state before other states', () => {
    renderWithProviders(
      <ServerDataState isLoading isEmpty emptyTitle="Nothing here">
        <div>Loaded content</div>
      </ServerDataState>,
    );

    expect(screen.getByRole('status', { name: 'Загрузка данных' })).toBeInTheDocument();
    expect(screen.queryByText('Loaded content')).not.toBeInTheDocument();
  });

  it('renders an empty state with a contextual action', () => {
    renderWithProviders(
      <ServerDataState
        isLoading={false}
        isEmpty
        emptyTitle="Компонентов пока нет"
        emptyDescription="Добавьте первый компонент"
        emptyAction={<Button>Добавить</Button>}
      >
        <div>Loaded content</div>
      </ServerDataState>,
    );

    expect(screen.getByRole('heading', { name: 'Компонентов пока нет' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Добавить' })).toBeInTheDocument();
  });

  it('shows a safe retryable network error and invokes retry', async () => {
    const user = userEvent.setup();
    const retry = vi.fn();
    renderWithProviders(
      <ServerDataState
        isLoading={false}
        error={new TypeError('internal browser diagnostic')}
        isEmpty={false}
        emptyTitle="Nothing here"
        onRetry={retry}
      >
        <div>Loaded content</div>
      </ServerDataState>,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('Нет связи с сервером');
    expect(screen.queryByText('internal browser diagnostic')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Повторить' }));
    expect(retry).toHaveBeenCalledOnce();
  });

  it('shows safe backend text but no retry for a validation error', () => {
    const error: ErrorResponse = {
      timestamp: '2026-08-09T12:00:00Z',
      status: 400,
      error: 'Bad Request',
      code: 'VALIDATION_ERROR',
      message: 'Имя обязательно',
      path: '/domains',
      details: [],
    };
    renderWithProviders(
      <ServerDataState
        isLoading={false}
        error={error}
        isEmpty={false}
        emptyTitle="Nothing here"
        onRetry={vi.fn()}
      >
        <div>Loaded content</div>
      </ServerDataState>,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('Проверьте введённые данные');
    expect(screen.getByRole('alert')).toHaveTextContent('Имя обязательно');
    expect(screen.queryByRole('button', { name: 'Повторить' })).not.toBeInTheDocument();
  });

  it('renders successful content and a reusable page header', () => {
    renderWithProviders(
      <>
        <PageHeader title="Компоненты" description="Каталог" actions={<Button>Создать</Button>} />
        <ServerDataState isLoading={false} isEmpty={false} emptyTitle="Nothing here">
          <div>Loaded content</div>
        </ServerDataState>
      </>,
    );

    expect(screen.getByRole('heading', { level: 1, name: 'Компоненты' })).toBeInTheDocument();
    expect(screen.getByText('Каталог')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Создать' })).toBeInTheDocument();
    expect(screen.getByText('Loaded content')).toBeInTheDocument();
  });
});
