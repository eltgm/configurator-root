import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { App } from '@/app/App';

describe('App', () => {
  it('renders the accessible frontend entrypoint', () => {
    render(<App />);

    expect(
      screen.getByRole('heading', { level: 1, name: 'Конфигуратор компонентов' }),
    ).toBeInTheDocument();
  });
});
