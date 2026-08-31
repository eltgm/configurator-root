import { notifications } from '@mantine/notifications';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse, type HttpResponseResolver } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { createMemoryRouter } from 'react-router-dom';

import { App } from '@/app/App';
import { appRoutes } from '@/app/router/routes';
import type { CreateDomainRequest, Domain, DomainPage, ErrorResponse } from '@/shared/api';
import { queryClient } from '@/shared/query/query-client';
import { selectedDomainStorageKey } from '@/shared/config/preferences';
import { server, testApiBaseUrl } from '@/test/server';

const firstDomain: Domain = {
  id: 1,
  name: 'Рабочая станция',
  description: 'Основной каталог',
  createdAt: '2026-08-09T12:00:00Z',
};
const secondDomain: Domain = {
  id: 2,
  name: 'Домашний сервер',
  createdAt: '2026-08-09T13:00:00Z',
};

function renderRoute(path: string) {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] });
  return render(<App router={router} />);
}

function domainPage(domains: Array<Domain>): DomainPage {
  return { items: domains, page: 0, size: 100, totalItems: domains.length };
}

function useDomainHandlers(
  domains: Array<Domain>,
  overrides: {
    create?: HttpResponseResolver;
    remove?: HttpResponseResolver;
  } = {},
) {
  server.use(
    http.get(`${testApiBaseUrl}/domains`, () => HttpResponse.json(domainPage(domains))),
    http.post(
      `${testApiBaseUrl}/domains`,
      overrides.create ??
        (async ({ request }) => {
          const body = (await request.json()) as CreateDomainRequest;
          const created: Domain = {
            id: 201,
            ...body,
            createdAt: '2026-08-09T14:00:00Z',
          };
          domains.push(created);
          return HttpResponse.json(created, { status: 201 });
        }),
    ),
    http.post(`${testApiBaseUrl}/domains/demo`, () => {
      const demo: Domain = {
        id: 301,
        name: 'Сборка ПК',
        description: 'Демонстрационная область',
        createdAt: '2026-08-09T15:00:00Z',
      };
      domains.push(demo);
      return HttpResponse.json(demo, { status: 201 });
    }),
    http.put(`${testApiBaseUrl}/domains/:id`, async ({ params, request }) => {
      const body = (await request.json()) as CreateDomainRequest;
      const id = Number(params['id']);
      const current = domains.find((domain) => domain.id === id);
      if (!current) {
        return new HttpResponse(null, { status: 404 });
      }
      const updated: Domain = {
        id: current.id,
        name: body.name,
        createdAt: current.createdAt,
        ...(body.description === undefined ? {} : { description: body.description }),
      };
      domains.splice(domains.indexOf(current), 1, updated);
      return HttpResponse.json(updated);
    }),
    http.delete(
      `${testApiBaseUrl}/domains/:id`,
      overrides.remove ??
        (({ params }) => {
          const index = domains.findIndex((domain) => domain.id === Number(params['id']));
          if (index >= 0) {
            domains.splice(index, 1);
          }
          return new HttpResponse(null, { status: 204 });
        }),
    ),
  );
}

afterEach(() => {
  notifications.clean();
  window.localStorage.clear();
});

describe('domain first run and management', () => {
  it('creates the first domain with client validation, trimmed payload and automatic selection', async () => {
    const user = userEvent.setup();
    const domains: Array<Domain> = [];
    let submittedBody: CreateDomainRequest | undefined;
    useDomainHandlers(domains, {
      create: async ({ request }) => {
        submittedBody = (await request.json()) as CreateDomainRequest;
        const created: Domain = {
          id: 201,
          ...submittedBody,
          createdAt: '2026-08-09T14:00:00Z',
        };
        domains.push(created);
        return HttpResponse.json(created, { status: 201 });
      },
    });
    renderRoute('/configurator');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Начните с предметной области' }),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Новая область' }));
    const createDialog = await screen.findByRole('dialog', { name: 'Новая предметная область' });
    await user.click(within(createDialog).getByRole('button', { name: 'Создать' }));
    expect(await screen.findByText('Введите название')).toBeInTheDocument();

    await user.type(
      within(createDialog).getByRole('textbox', { name: /Название/ }),
      '  Новый каталог  ',
    );
    await user.type(
      within(createDialog).getByRole('textbox', { name: 'Описание' }),
      '  Для тестов  ',
    );
    await user.click(within(createDialog).getByRole('button', { name: 'Создать' }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Конфигуратор' }),
    ).toBeInTheDocument();
    expect(submittedBody).toEqual({ name: 'Новый каталог', description: 'Для тестов' });
    expect(
      screen.getByRole('button', { name: 'Предметная область: Новый каталог' }),
    ).toBeInTheDocument();
    expect(window.localStorage.getItem(selectedDomainStorageKey)).toBe('201');
    expect(await screen.findByText('Предметная область создана')).toBeInTheDocument();
  });

  it('shows a backend field error inside the create form', async () => {
    const user = userEvent.setup();
    const error: ErrorResponse = {
      timestamp: '2026-08-09T12:00:00Z',
      status: 400,
      error: 'Bad Request',
      code: 'VALIDATION_ERROR',
      message: 'Validation failed',
      path: '/domains',
      details: [{ field: 'name', code: 'INVALID_VALUE', message: 'Название недоступно' }],
    };
    useDomainHandlers([], {
      create: () => HttpResponse.json(error, { status: 400 }),
    });
    renderRoute('/configurator');

    await screen.findByRole('heading', { name: 'Начните с предметной области' });
    await user.click(screen.getByRole('button', { name: 'Новая область' }));
    const createDialog = await screen.findByRole('dialog', { name: 'Новая предметная область' });
    await user.type(within(createDialog).getByRole('textbox', { name: /Название/ }), 'Дубликат');
    await user.click(within(createDialog).getByRole('button', { name: 'Создать' }));

    expect(await screen.findByText('Название недоступно')).toBeInTheDocument();
  });

  it('creates the demo as a separate first-run action', async () => {
    const user = userEvent.setup();
    const domains: Array<Domain> = [];
    useDomainHandlers(domains);
    renderRoute('/components');

    await screen.findByRole('heading', { name: 'Начните с предметной области' });
    await user.click(screen.getByRole('button', { name: 'Создать демо «Сборка ПК»' }));

    expect(await screen.findByRole('heading', { name: 'Компоненты' })).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Предметная область: Сборка ПК' }),
    ).toBeInTheDocument();
    expect(window.localStorage.getItem(selectedDomainStorageKey)).toBe('301');
  });

  it('restores the last domain and switches it from the header menu', async () => {
    const user = userEvent.setup();
    const domains = [firstDomain, secondDomain];
    useDomainHandlers(domains);
    window.localStorage.setItem(selectedDomainStorageKey, '2');
    renderRoute('/configurator');

    const selector = await screen.findByRole('button', {
      name: 'Предметная область: Домашний сервер',
    });
    await user.click(selector);
    await user.click(await screen.findByRole('menuitem', { name: 'Рабочая станция' }));

    expect(
      screen.getByRole('button', { name: 'Предметная область: Рабочая станция' }),
    ).toBeInTheDocument();
    expect(window.localStorage.getItem(selectedDomainStorageKey)).toBe('1');
  });

  it('falls back to the first domain when the persisted selection no longer exists', async () => {
    useDomainHandlers([firstDomain, secondDomain]);
    window.localStorage.setItem(selectedDomainStorageKey, '999');
    renderRoute('/configurator');

    expect(
      await screen.findByRole('button', { name: 'Предметная область: Рабочая станция' }),
    ).toBeInTheDocument();
    expect(window.localStorage.getItem(selectedDomainStorageKey)).toBe('1');
  });

  it('retries a failed domain list request', async () => {
    let shouldFail = true;
    server.use(
      http.get(`${testApiBaseUrl}/domains`, () =>
        shouldFail
          ? HttpResponse.json(
              {
                timestamp: '2026-08-09T12:00:00Z',
                status: 503,
                error: 'Service Unavailable',
                code: 'INTERNAL_ERROR',
                message: 'Temporary failure',
                path: '/domains',
                details: [],
              } satisfies ErrorResponse,
              { status: 503 },
            )
          : HttpResponse.json(domainPage([firstDomain])),
      ),
    );
    const user = userEvent.setup();
    renderRoute('/configurator');

    const retry = await screen.findByRole('button', { name: 'Повторить' }, { timeout: 3_000 });
    shouldFail = false;
    await user.click(retry);

    expect(
      await screen.findByRole('button', { name: 'Предметная область: Рабочая станция' }),
    ).toBeInTheDocument();
  });

  it('edits a domain and selects it from the management page', async () => {
    const user = userEvent.setup();
    const domains = [{ ...firstDomain }, { ...secondDomain }];
    useDomainHandlers(domains);
    renderRoute('/settings/domain');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Предметные области' }),
    ).toBeInTheDocument();
    await user.click(
      await screen.findByRole('button', { name: 'Редактировать область Домашний сервер' }),
    );
    const editDialog = await screen.findByRole('dialog', { name: 'Редактирование области' });
    const nameInput = within(editDialog).getByRole('textbox', { name: /Название/ });
    await user.clear(nameInput);
    await user.type(nameInput, 'Домашнее облако');
    await user.click(within(editDialog).getByRole('button', { name: 'Сохранить' }));

    expect(await screen.findByRole('heading', { name: 'Домашнее облако' })).toBeInTheDocument();
    expect(window.localStorage.getItem(selectedDomainStorageKey)).toBe('2');
    expect(await screen.findByText('Изменения сохранены')).toBeInTheDocument();
  });

  it('confirms irreversible deletion and selects the remaining fallback', async () => {
    const user = userEvent.setup();
    const domains = [{ ...firstDomain }, { ...secondDomain }];
    useDomainHandlers(domains);
    window.localStorage.setItem(selectedDomainStorageKey, '1');
    renderRoute('/settings/domain');

    await screen.findByRole('heading', { name: 'Предметные области' });
    await user.click(
      await screen.findByRole('button', { name: 'Удалить область Рабочая станция' }),
    );
    const dialog = await screen.findByRole('dialog', { name: 'Удалить предметную область?' });
    expect(within(dialog).getByText(/Действие необратимо/)).toBeInTheDocument();
    const input = within(dialog).getByRole('textbox', { name: /Название области/ });
    const confirm = within(dialog).getByRole('button', { name: 'Удалить' });
    expect(confirm).toBeDisabled();
    await user.type(input, 'рабочая станция{Enter}');
    expect(confirm).toBeDisabled();
    expect(domains).toHaveLength(2);
    await user.clear(input);
    queryClient.setQueryData(['domains', 1, 'components'], ['deleted catalog']);
    queryClient.setQueryData(['domains', 2, 'components'], ['retained catalog']);
    await user.type(
      within(dialog).getByRole('textbox', { name: /Название области/ }),
      firstDomain.name,
    );
    await user.click(within(dialog).getByRole('button', { name: 'Удалить' }));

    await waitFor(() => {
      expect(screen.queryByRole('heading', { name: 'Рабочая станция' })).not.toBeInTheDocument();
    });
    expect(
      screen.getByRole('button', { name: 'Предметная область: Домашний сервер' }),
    ).toBeInTheDocument();
    expect(window.localStorage.getItem(selectedDomainStorageKey)).toBe('2');
    expect(queryClient.getQueryData(['domains', 1, 'components'])).toBeUndefined();
    expect(queryClient.getQueryData(['domains', 2, 'components'])).toEqual(['retained catalog']);
  });

  it('resets confirmation when cancelled and when another domain is opened', async () => {
    const user = userEvent.setup();
    const remove = vi.fn(() => new HttpResponse(null, { status: 204 }));
    useDomainHandlers([{ ...firstDomain }, { ...secondDomain }], { remove });
    renderRoute('/settings/domain');
    await user.click(
      await screen.findByRole('button', { name: 'Удалить область Рабочая станция' }),
    );
    let dialog = await screen.findByRole('dialog', { name: 'Удалить предметную область?' });
    await user.type(
      within(dialog).getByRole('textbox', { name: /Название области/ }),
      firstDomain.name,
    );
    await user.click(within(dialog).getByRole('button', { name: 'Отмена' }));
    await user.click(screen.getByRole('button', { name: 'Удалить область Домашний сервер' }));
    dialog = await screen.findByRole('dialog', { name: 'Удалить предметную область?' });
    expect(within(dialog).getByRole('textbox', { name: /Название области/ })).toHaveValue('');
    expect(within(dialog).getByRole('button', { name: 'Удалить' })).toBeDisabled();
    expect(remove).not.toHaveBeenCalled();
  });

  it('blocks repeated submission and closing while deletion is pending', async () => {
    const user = userEvent.setup();
    const domains = [{ ...firstDomain }];
    let releaseRequest = () => {};
    const requestGate = new Promise<void>((resolve) => {
      releaseRequest = resolve;
    });
    const remove = vi.fn(async () => {
      await requestGate;
      domains.splice(0, 1);
      return new HttpResponse(null, { status: 204 });
    });
    useDomainHandlers(domains, { remove });
    renderRoute('/settings/domain');
    await user.click(
      await screen.findByRole('button', { name: 'Удалить область Рабочая станция' }),
    );
    const dialog = await screen.findByRole('dialog', { name: 'Удалить предметную область?' });
    await user.type(
      within(dialog).getByRole('textbox', { name: /Название области/ }),
      firstDomain.name,
    );
    await user.click(within(dialog).getByRole('button', { name: 'Удалить' }));
    try {
      await waitFor(() => expect(remove).toHaveBeenCalledTimes(1));
      expect(within(dialog).getByRole('textbox', { name: /Название области/ })).toBeDisabled();
      expect(within(dialog).getByRole('button', { name: 'Отмена' })).toBeDisabled();
      expect(within(dialog).getByRole('button', { name: 'Закрыть' })).toBeDisabled();
      await user.keyboard('{Escape}{Enter}');
      expect(dialog).toBeInTheDocument();
      expect(remove).toHaveBeenCalledTimes(1);
    } finally {
      releaseRequest();
    }
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('returns to first run after deleting the last domain', async () => {
    const user = userEvent.setup();
    const domains = [{ ...firstDomain }];
    useDomainHandlers(domains);
    renderRoute('/settings/domain');

    await screen.findByRole('heading', { name: 'Предметные области' });
    await user.click(
      await screen.findByRole('button', { name: 'Удалить область Рабочая станция' }),
    );
    const dialog = await screen.findByRole('dialog', { name: 'Удалить предметную область?' });
    await user.type(
      within(dialog).getByRole('textbox', { name: /Название области/ }),
      firstDomain.name,
    );
    await user.click(within(dialog).getByRole('button', { name: 'Удалить' }));

    await user.click(screen.getByRole('link', { name: 'Конфигуратор компонентов' }));
    expect(
      await screen.findByRole('heading', { name: 'Начните с предметной области' }),
    ).toBeInTheDocument();
    expect(window.localStorage.getItem(selectedDomainStorageKey)).toBeNull();
  });

  it('keeps the domain and explains that configurations must be deleted first', async () => {
    const user = userEvent.setup();
    const conflict: ErrorResponse = {
      timestamp: '2026-08-09T12:00:00Z',
      status: 409,
      error: 'Conflict',
      code: 'DOMAIN_HAS_CONFIGURATIONS',
      message: 'Delete all configurations first',
      path: '/domains/1',
      details: [],
    };
    const domains = [{ ...firstDomain }];
    useDomainHandlers(domains, {
      remove: vi.fn(() => HttpResponse.json(conflict, { status: 409 })),
    });
    renderRoute('/settings/domain');

    await screen.findByRole('heading', { name: 'Предметные области' });
    await user.click(
      await screen.findByRole('button', { name: 'Удалить область Рабочая станция' }),
    );
    const deleteDialog = await screen.findByRole('dialog', {
      name: 'Удалить предметную область?',
    });
    await user.type(
      within(deleteDialog).getByRole('textbox', { name: /Название области/ }),
      firstDomain.name,
    );
    await user.click(within(deleteDialog).getByRole('button', { name: 'Удалить' }));

    expect(
      await screen.findByText(
        'Невозможно удалить область: в ней есть конфигурации. Сначала удалите все конфигурации.',
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Рабочая станция' })).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });
});
