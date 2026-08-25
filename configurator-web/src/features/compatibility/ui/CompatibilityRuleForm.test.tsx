import { notifications } from '@mantine/notifications';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';
import { createMemoryRouter } from 'react-router-dom';

import { App } from '@/app/App';
import { appRoutes } from '@/app/router/routes';
import type {
  AttributeDefinition,
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
const attributesByType: Record<number, AttributeDefinition[]> = {
  11: [
    {
      id: 1011,
      domainId,
      componentTypeId: 11,
      name: 'socket',
      label: 'Сокет процессора',
      dataType: 'STRING',
      isRequired: true,
      orderIndex: 0,
    },
    {
      id: 1012,
      domainId,
      componentTypeId: 11,
      name: 'cores',
      label: 'Количество ядер',
      dataType: 'NUMBER',
      isRequired: true,
      orderIndex: 1,
    },
  ],
  12: [
    {
      id: 2011,
      domainId,
      componentTypeId: 12,
      name: 'socket',
      label: 'Сокет платы',
      dataType: 'STRING',
      isRequired: true,
      orderIndex: 0,
    },
    {
      id: 2012,
      domainId,
      componentTypeId: 12,
      name: 'lanes',
      label: 'Линии PCIe',
      dataType: 'NUMBER',
      isRequired: false,
      orderIndex: 1,
    },
  ],
};
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
      rightAttributeDefinitionId: 2011,
      orderIndex: 0,
      createdAt: '2026-08-23T12:00:00',
    },
  ],
  createdAt: '2026-08-23T12:00:00',
};

function responseFromRequest(
  body: SaveCompatibilityRuleSetRequest,
  rule: CompatibilityRuleSet = baseRule,
): CompatibilityRuleSet {
  return {
    ...rule,
    ...body,
    conditions: body.conditions.map((condition, index) => ({
      id: 900 + index,
      ruleSetId: rule.id,
      leftAttributeDefinitionId: condition.leftAttributeDefinitionId,
      operator: condition.operator,
      rightAttributeDefinitionId: condition.rightAttributeDefinitionId,
      orderIndex: condition.orderIndex ?? index,
      createdAt: '2026-08-23T12:00:00',
    })),
  };
}

function useHandlers() {
  server.use(
    http.get(`${testApiBaseUrl}/domains/:domainId/component-types`, () =>
      HttpResponse.json(componentTypes),
    ),
    http.get(`${testApiBaseUrl}/component-types/:id/attributes`, ({ params }) =>
      HttpResponse.json(attributesByType[Number(params['id'])] ?? []),
    ),
    http.get(`${testApiBaseUrl}/domains/:domainId/compatibility/rules`, () =>
      HttpResponse.json([baseRule]),
    ),
    http.get(`${testApiBaseUrl}/domains/:domainId/compatibility/rules/:ruleId`, () =>
      HttpResponse.json(baseRule),
    ),
  );
}

function renderRoute(path: string) {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] });
  return { router, ...render(<App router={router} />) };
}

async function chooseByPosition(
  user: ReturnType<typeof userEvent.setup>,
  label: string,
  position = 0,
  occurrence = 0,
) {
  const controls = screen.getAllByRole('combobox', { name: label });
  await user.click(controls[occurrence]!);
  await user.keyboard(`${'{ArrowDown}'.repeat(position + 1)}{Enter}`);
}

afterEach(() => {
  notifications.clean();
  queryClient.clear();
  window.localStorage.clear();
});

describe('automatic compatibility rule form', () => {
  it('creates a rule from a full-page form with sequential condition order', async () => {
    const user = userEvent.setup();
    let submitted: SaveCompatibilityRuleSetRequest | undefined;
    useHandlers();
    server.use(
      http.post(`${testApiBaseUrl}/domains/:domainId/compatibility/rules`, async ({ request }) => {
        submitted = (await request.json()) as SaveCompatibilityRuleSetRequest;
        return HttpResponse.json(responseFromRequest(submitted), { status: 201 });
      }),
    );
    const { router } = renderRoute('/settings/compatibility/rules/new');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Новое автоматическое правило' }),
    ).toBeInTheDocument();
    await user.type(
      await screen.findByRole('textbox', { name: 'Название правила' }),
      '  Совместимость  ',
    );
    await chooseByPosition(user, 'Тип слева');
    await chooseByPosition(user, 'Тип справа');
    await chooseByPosition(user, 'Атрибут слева');
    await chooseByPosition(user, 'Оператор');
    await chooseByPosition(user, 'Атрибут справа');

    await user.click(screen.getByRole('button', { name: 'Добавить условие' }));
    await chooseByPosition(user, 'Атрибут слева', 1, 1);
    await chooseByPosition(user, 'Оператор', 3, 1);
    await chooseByPosition(user, 'Атрибут справа', 0, 1);
    await user.click(screen.getByRole('button', { name: 'Переместить условие 2 выше' }));
    await user.click(screen.getByRole('button', { name: 'Создать правило' }));

    await waitFor(() =>
      expect(router.state.location.pathname).toBe('/settings/compatibility/rules'),
    );
    expect(submitted).toEqual({
      name: 'Совместимость',
      componentTypeAId: 12,
      componentTypeBId: 11,
      enabled: true,
      conditions: [
        {
          leftAttributeDefinitionId: 2012,
          operator: 'GTE',
          rightAttributeDefinitionId: 1012,
          orderIndex: 0,
        },
        {
          leftAttributeDefinitionId: 2011,
          operator: 'EQUALS',
          rightAttributeDefinitionId: 1011,
          orderIndex: 1,
        },
      ],
    });
    expect(await screen.findByText('Автоматическое правило создано')).toBeInTheDocument();
  });

  it('loads an existing rule and replaces it through an authoritative PUT', async () => {
    const user = userEvent.setup();
    let submitted: SaveCompatibilityRuleSetRequest | undefined;
    useHandlers();
    server.use(
      http.put(
        `${testApiBaseUrl}/domains/:domainId/compatibility/rules/:ruleId`,
        async ({ request }) => {
          submitted = (await request.json()) as SaveCompatibilityRuleSetRequest;
          return HttpResponse.json(responseFromRequest(submitted));
        },
      ),
    );
    const { router } = renderRoute('/settings/compatibility/rules/301/edit');

    const name = await screen.findByRole('textbox', { name: 'Название правила' });
    expect(name).toHaveValue('Одинаковый сокет');
    await waitFor(() =>
      expect(screen.getByRole('combobox', { name: 'Атрибут слева' })).toHaveValue(
        'Сокет процессора · STRING',
      ),
    );
    await user.clear(name);
    await user.type(name, 'Обновлённый сокет');
    await user.click(screen.getByRole('switch', { name: /^Правило включено/ }));
    await user.click(screen.getByRole('button', { name: 'Сохранить правило' }));

    await waitFor(() =>
      expect(router.state.location.pathname).toBe('/settings/compatibility/rules'),
    );
    expect(submitted).toEqual({
      name: 'Обновлённый сокет',
      componentTypeAId: 11,
      componentTypeBId: 12,
      enabled: false,
      conditions: [
        {
          leftAttributeDefinitionId: 1011,
          operator: 'EQUALS',
          rightAttributeDefinitionId: 2011,
          orderIndex: 0,
        },
      ],
    });
    expect(await screen.findByText('Автоматическое правило сохранено')).toBeInTheDocument();
  });

  it('validates required values and duplicate condition triples before transport', async () => {
    const user = userEvent.setup();
    useHandlers();
    renderRoute('/settings/compatibility/rules/new');

    await user.click(await screen.findByRole('button', { name: 'Создать правило' }));
    expect(await screen.findByText('Введите название правила')).toBeInTheDocument();
    expect(screen.getByText('Выберите тип слева')).toBeInTheDocument();
    expect(screen.getByText('Выберите тип справа')).toBeInTheDocument();

    await user.type(screen.getByRole('textbox', { name: 'Название правила' }), 'Дубликаты');
    await chooseByPosition(user, 'Тип слева');
    await chooseByPosition(user, 'Тип справа');
    await chooseByPosition(user, 'Атрибут слева');
    await chooseByPosition(user, 'Оператор');
    await chooseByPosition(user, 'Атрибут справа');
    await user.click(screen.getByRole('button', { name: 'Добавить условие' }));

    await chooseByPosition(user, 'Атрибут слева', 0, 1);
    await chooseByPosition(user, 'Оператор', 0, 1);
    await chooseByPosition(user, 'Атрибут справа', 0, 1);
    await user.click(screen.getByRole('button', { name: 'Создать правило' }));

    expect(await screen.findAllByText('Такое условие уже добавлено')).toHaveLength(2);
  });

  it('guards dirty form navigation and lets the user stay or leave', async () => {
    const user = userEvent.setup();
    useHandlers();
    const { router } = renderRoute('/settings/compatibility/rules/new');

    await user.type(
      await screen.findByRole('textbox', { name: 'Название правила' }),
      'Несохранённое',
    );
    await user.click(screen.getByRole('link', { name: 'К списку правил' }));
    let dialog = await screen.findByRole('dialog', { name: 'Выйти без сохранения?' });
    expect(router.state.location.pathname).toBe('/settings/compatibility/rules/new');
    await user.click(within(dialog).getByRole('button', { name: 'Остаться' }));
    await waitFor(() =>
      expect(
        screen.queryByRole('dialog', { name: 'Выйти без сохранения?' }),
      ).not.toBeInTheDocument(),
    );

    await user.click(screen.getByRole('link', { name: 'К списку правил' }));
    dialog = await screen.findByRole('dialog', { name: 'Выйти без сохранения?' });
    await user.click(within(dialog).getByRole('button', { name: 'Выйти' }));
    await waitFor(() =>
      expect(router.state.location.pathname).toBe('/settings/compatibility/rules'),
    );
  });
});
