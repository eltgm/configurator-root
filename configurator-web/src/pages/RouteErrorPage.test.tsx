import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';

import { AppProviders } from '@/app/providers/AppProviders';
import { RouteErrorPage } from '@/pages/RouteErrorPage';

function BrokenRoute(): never {
  throw new Error('sensitive internal render diagnostic');
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('route error fallback', () => {
  it('shows a localized safe fallback without internal diagnostics', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const router = createMemoryRouter(
      [
        {
          path: '/broken',
          element: <BrokenRoute />,
          errorElement: <RouteErrorPage />,
        },
      ],
      { initialEntries: ['/broken'] },
    );

    render(
      <AppProviders>
        <RouterProvider router={router} />
      </AppProviders>,
    );

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Произошла ошибка' }),
    ).toBeInTheDocument();
    expect(screen.getByText(/Не удалось выполнить действие/)).toBeInTheDocument();
    expect(screen.queryByText(/sensitive internal render diagnostic/)).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Вернуться в конфигуратор' })).toHaveAttribute(
      'href',
      '/configurator',
    );
  });
});
