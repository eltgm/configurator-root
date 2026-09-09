import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';

import { AppProviders } from '@/app/providers/AppProviders';
import type { ErrorResponse } from '@/shared/api';
import { i18n } from '@/shared/i18n/i18n';
import { ErrorState } from '@/shared/ui/ErrorState';

describe('ErrorState', () => {
  afterEach(async () => {
    await i18n.changeLanguage('ru');
  });

  it('focuses a submitted error summary without stealing focus on later renders', () => {
    const { rerender } = render(
      <AppProviders>
        <ErrorState autoFocus error={new TypeError('offline')} />
        <button>Next field</button>
      </AppProviders>,
    );
    expect(screen.getByRole('alert')).toHaveFocus();
    screen.getByRole('button', { name: 'Next field' }).focus();
    rerender(
      <AppProviders>
        <ErrorState autoFocus error={new TypeError('offline')} />
        <button>Next field</button>
      </AppProviders>,
    );
    expect(screen.getByRole('button', { name: 'Next field' })).toHaveFocus();
  });

  it('renders an inventory conflict in English when the site language is English', async () => {
    await i18n.changeLanguage('en');
    const error: ErrorResponse = {
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
      ],
    };

    render(
      <AppProviders>
        <ErrorState error={error} />
      </AppProviders>,
    );

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Not enough components available');
    expect(alert).toHaveTextContent('“Ryzen”: 3 required, 2 available.');
  });

  it('localizes a component total below its reserved quantity', () => {
    const error: ErrorResponse = {
      timestamp: '2026-09-08T12:00:00Z',
      status: 409,
      error: 'Conflict',
      code: 'COMPONENT_TOTAL_BELOW_ALLOCATED',
      message: 'Component total quantity cannot be lower than allocated quantity',
      path: '/components/7',
      details: [
        {
          code: 'COMPONENT_TOTAL_BELOW_ALLOCATED',
          message: 'Component total quantity is below allocated quantity',
          parameters: {
            componentId: '7',
            componentName: 'Ryzen',
            totalQuantity: '2',
            allocatedQuantity: '3',
          },
        },
      ],
    };

    render(
      <AppProviders>
        <ErrorState error={error} />
      </AppProviders>,
    );

    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('Количество меньше зарезервированного');
    expect(alert).toHaveTextContent('«Ryzen»: указано 2, уже зарезервировано 3.');
  });
});
