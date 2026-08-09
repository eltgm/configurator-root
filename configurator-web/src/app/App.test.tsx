import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { createMemoryRouter } from 'react-router-dom';

import { App } from '@/app/App';
import { appRoutes } from '@/app/router/routes';
import { colorSchemeStorageKey, localeStorageKey } from '@/shared/config/preferences';
import { changeLocale } from '@/shared/i18n/i18n';

function renderRoute(path: string) {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] });
  return {
    router,
    ...render(<App router={router} />),
  };
}

afterEach(async () => {
  await changeLocale('ru');
  window.localStorage.clear();
});

describe('application shell', () => {
  it('redirects the root route and renders accessible desktop and mobile navigation', async () => {
    const { router } = renderRoute('/');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Конфигуратор' }),
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/configurator');

    const desktopNavigation = screen.getByRole('navigation', {
      name: 'Основная навигация',
    });
    const mobileNavigation = screen.getByRole('navigation', {
      name: 'Мобильная навигация',
    });
    expect(within(desktopNavigation).getByRole('link', { name: 'Конфигуратор' })).toHaveAttribute(
      'aria-current',
      'page',
    );
    expect(within(mobileNavigation).getByRole('link', { name: 'Конфигуратор' })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('navigates between primary and nested settings routes', async () => {
    const user = userEvent.setup();
    renderRoute('/configurator');
    const desktopNavigation = screen.getByRole('navigation', {
      name: 'Основная навигация',
    });

    await user.click(within(desktopNavigation).getByRole('link', { name: 'Компоненты' }));
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Компоненты' }),
    ).toBeInTheDocument();
    expect(document.title).toBe('Компоненты — Конфигуратор');

    await user.click(
      within(desktopNavigation).getByRole('link', { name: 'Автоматические правила' }),
    );
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Автоматические правила' }),
    ).toBeInTheDocument();
  });

  it('shows a localized not-found page inside the shell', () => {
    renderRoute('/missing-page');

    expect(screen.getByText('404')).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { level: 1, name: 'Страница не найдена' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Вернуться в конфигуратор' })).toHaveAttribute(
      'href',
      '/configurator',
    );
  });

  it('persists the selected color scheme', async () => {
    const user = userEvent.setup();
    renderRoute('/configurator');

    await user.click(screen.getByRole('button', { name: 'Настройки интерфейса' }));
    await user.click(await screen.findByRole('menuitem', { name: 'Тёмная' }));

    expect(window.localStorage.getItem(colorSchemeStorageKey)).toBe('dark');
    await waitFor(() => {
      expect(document.documentElement).toHaveAttribute('data-mantine-color-scheme', 'dark');
    });
  });

  it('switches and persists the interface language', async () => {
    const user = userEvent.setup();
    renderRoute('/components');

    await user.click(screen.getByRole('button', { name: 'Настройки интерфейса' }));
    await user.click(await screen.findByRole('menuitem', { name: 'English' }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Components' }),
    ).toBeInTheDocument();
    expect(document.title).toBe('Components — Configurator');
    expect(window.localStorage.getItem(localeStorageKey)).toBe('en');
    expect(document.documentElement).toHaveAttribute('lang', 'en');
    expect(screen.getByRole('navigation', { name: 'Main navigation' })).toBeInTheDocument();
  });
});
