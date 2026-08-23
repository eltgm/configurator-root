import { notifications } from '@mantine/notifications';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse, type HttpResponseResolver } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';
import { createMemoryRouter } from 'react-router-dom';

import { App } from '@/app/App';
import { appRoutes } from '@/app/router/routes';
import type {
  CompatibilityRuleSet,
  ComponentType,
  SaveCompatibilityRuleSetRequest,
} from '@/shared/api';
import { queryClient } from '@/shared/query/query-client';
import { server, testApiBaseUrl } from '@/test/server';

const domainId = 101;
const componentTypes: ComponentType[] = [
  { id: 11, domainId, name: 'Процессор' },
  { id: 12, domainId, name: 'Материнская плата' },
];
const baseRule: CompatibilityRuleSet = {
  id: 301,
  domainId,
  name: 'Одинаковый сокет',
  componentTypeAId: 11,
  componentTypeBId: 12,
  enabled: true,
  conditions: [
    {
      id: 401,
      ruleSetId: 301,
      leftAttributeDefinitionId: 1011,
      operator: 'EQUALS',
      rightAttributeDefinitionId: 1012,
      orderIndex: 0,
      createdAt: '2026-08-23T12:00:00',
    },
  ],
  createdAt: '2026-08-23T12:00:00',
};

function replaceRule(
  existing: CompatibilityRuleSet,
  body: SaveCompatibilityRuleSetRequest,
): CompatibilityRuleSet {
  return {
    ...existing,
    ...body,
    conditions: body.conditions.map((condition, index) => ({
      id: existing.conditions[index]?.id ?? 900 + index,
      ruleSetId: existing.id,
      leftAttributeDefinitionId: condition.leftAttributeDefinitionId,
      operator: condition.operator,
      rightAttributeDefinitionId: condition.rightAttributeDefinitionId,
      orderIndex: condition.orderIndex ?? index,
      createdAt: existing.conditions[index]?.createdAt ?? '2026-08-23T12:00:00',
    })),
  };
}

function renderPage() {
  const router = createMemoryRouter(appRoutes, {
    initialEntries: ['/settings/compatibility/rules'],
  });
  return render(<App router={router} />);
}

function useHandlers(
  rules: CompatibilityRuleSet[],
  overrides: {
    list?: HttpResponseResolver;
    update?: HttpResponseResolver;
    remove?: HttpResponseResolver;
  } = {},
) {
  server.use(
    http.get(
      `${testApiBaseUrl}/domains/:domainId/compatibility/rules`,
      overrides.list ?? (() => HttpResponse.json(rules)),
    ),
    http.get(`${testApiBaseUrl}/domains/:domainId/component-types`, () =>
      HttpResponse.json(componentTypes),
    ),
    http.put(
      `${testApiBaseUrl}/domains/:domainId/compatibility/rules/:ruleId`,
      overrides.update ??
        (async ({ request }) => {
          const body = (await request.json()) as SaveCompatibilityRuleSetRequest;
          const updated = replaceRule(rules[0]!, body);
          rules[0] = updated;
          return HttpResponse.json(updated);
        }),
    ),
    http.delete(
      `${testApiBaseUrl}/domains/:domainId/compatibility/rules/:ruleId`,
      overrides.remove ??
        (() => {
          rules.splice(0, 1);
          return new HttpResponse(null, { status: 204 });
        }),
    ),
  );
}

afterEach(() => notifications.clean());

describe('automatic compatibility rule management', () => {
  it('renders counters, responsive representations and filters rules locally', async () => {
    const user = userEvent.setup();
    useHandlers([
      structuredClone(baseRule),
      { ...structuredClone(baseRule), id: 302, name: 'Отключённое правило', enabled: false },
    ]);
    renderPage();

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Автоматические правила' }),
    ).toBeInTheDocument();
    expect(await screen.findByText('2 правила')).toBeInTheDocument();
    expect(screen.getByText('1 включено')).toBeInTheDocument();
    expect(screen.getByText('1 отключено')).toBeInTheDocument();
    expect(screen.getByTestId('desktop-compatibility-rule-table')).toBeInTheDocument();
    expect(screen.getByTestId('mobile-compatibility-rule-list')).toBeInTheDocument();
    expect(screen.getAllByText('Процессор').length).toBeGreaterThan(0);

    await user.type(screen.getByRole('textbox', { name: 'Поиск' }), 'нет такого');
    expect(await screen.findByRole('heading', { name: 'Правила не найдены' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Сбросить фильтры' }));
    expect(screen.getAllByText('Одинаковый сокет').length).toBeGreaterThan(0);
    await user.click(screen.getByText('Отключённые'));
    expect(screen.queryByText('Одинаковый сокет')).not.toBeInTheDocument();
    expect(screen.getAllByText('Отключённое правило').length).toBeGreaterThan(0);
  });

  it('toggles through a full authoritative PUT without changing the rule shape', async () => {
    const user = userEvent.setup();
    const rules = [structuredClone(baseRule)];
    let submitted: SaveCompatibilityRuleSetRequest | undefined;
    useHandlers(rules, {
      update: async ({ request }) => {
        submitted = (await request.json()) as SaveCompatibilityRuleSetRequest;
        const updated = replaceRule(rules[0]!, submitted);
        rules[0] = updated;
        return HttpResponse.json(updated);
      },
    });
    renderPage();
    const switches = await screen.findAllByRole('switch', {
      name: 'Отключить правило Одинаковый сокет',
    });

    await user.click(switches[0]!);

    expect(await screen.findByText('Автоматическое правило отключено')).toBeInTheDocument();
    expect(submitted).toEqual({
      name: 'Одинаковый сокет',
      componentTypeAId: 11,
      componentTypeBId: 12,
      enabled: false,
      conditions: [
        {
          leftAttributeDefinitionId: 1011,
          operator: 'EQUALS',
          rightAttributeDefinitionId: 1012,
          orderIndex: 0,
        },
      ],
    });
    expect(
      screen.getAllByRole('switch', { name: 'Включить правило Одинаковый сокет' })[0],
    ).not.toBeChecked();
  });

  it('requires permanent-delete confirmation and updates the list after 204', async () => {
    const user = userEvent.setup();
    useHandlers([structuredClone(baseRule)]);
    renderPage();
    await screen.findAllByText('Одинаковый сокет');

    await user.click(
      screen.getAllByRole('button', { name: 'Удалить правило Одинаковый сокет' })[0]!,
    );
    const dialog = await screen.findByRole('dialog', { name: 'Удалить автоматическое правило?' });
    expect(within(dialog).getByText(/Действие необратимо/)).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: 'Отмена' }));
    expect(screen.getAllByText('Одинаковый сокет').length).toBeGreaterThan(0);

    await user.click(
      screen.getAllByRole('button', { name: 'Удалить правило Одинаковый сокет' })[0]!,
    );
    await user.click(
      within(
        await screen.findByRole('dialog', { name: 'Удалить автоматическое правило?' }),
      ).getByRole('button', { name: 'Удалить' }),
    );

    expect(await screen.findByText('Автоматическое правило удалено')).toBeInTheDocument();
    expect(
      await screen.findByRole('heading', { name: 'Автоматических правил пока нет' }),
    ).toBeInTheDocument();
  });

  it('shows empty and retry states', async () => {
    useHandlers([]);
    const first = renderPage();
    expect(
      await screen.findByRole('heading', { name: 'Автоматических правил пока нет' }),
    ).toBeInTheDocument();
    first.unmount();
    queryClient.clear();

    server.resetHandlers();
    useHandlers([], {
      list: () =>
        HttpResponse.json(
          {
            timestamp: '2026-08-23T12:00:00Z',
            status: 500,
            error: 'Internal Server Error',
            code: 'INTERNAL_ERROR',
            message: 'Rules unavailable',
            path: '/domains/101/compatibility/rules',
            details: [],
          },
          { status: 500 },
        ),
    });
    renderPage();
    expect(
      await screen.findByText('Внутренняя ошибка сервера', undefined, { timeout: 3000 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Повторить' })).toBeInTheDocument();
  });
});
