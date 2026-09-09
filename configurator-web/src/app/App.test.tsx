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
  it('opens the complete mobile settings panel and offers consistent help', async () => {
    const user = userEvent.setup();
    renderRoute('/help');
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Как собрать конфигурацию' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '5. Подберите и сохраните сборку' })).toHaveAttribute(
      'href',
      '/configurator',
    );
    await user.click(screen.getByRole('button', { name: 'Настройка' }));
    const panel = await screen.findByRole('dialog', { name: 'Разделы настроек' });
    expect(within(panel).getAllByRole('link')).toHaveLength(7);
    await user.click(within(panel).getByRole('link', { name: 'Атрибуты' }));
    expect(await screen.findByRole('heading', { level: 1, name: 'Атрибуты' })).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

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
    expect(screen.getByTestId('route-announcement')).toHaveTextContent('Компоненты');

    await user.click(
      within(desktopNavigation).getByRole('link', { name: 'Автоматические правила' }),
    );
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Автоматические правила' }),
    ).toBeInTheDocument();
  });

  it('moves keyboard focus to main content through the skip link', async () => {
    const user = userEvent.setup();
    renderRoute('/components');
    await screen.findByRole('heading', { level: 1, name: 'Компоненты' });

    await user.tab();
    const skipLink = screen.getByRole('link', { name: 'Перейти к содержимому' });
    expect(skipLink).toHaveFocus();
    await user.keyboard('{Enter}');

    expect(document.getElementById('main-content')).toHaveFocus();
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
    renderRoute('/missing-page');
    await screen.findByRole('button', { name: 'Предметная область: Сборка ПК' });

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
    await screen.findByRole('heading', { level: 2, name: 'Каталог пока пуст' });

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
