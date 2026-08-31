import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { AppProviders } from '@/app/providers/AppProviders';
import { attributeKeys } from '@/features/attributes/api/attributes';
import { AttachAttributeModal } from '@/features/attributes/ui/AttachAttributeModal';
import type { ComponentTypeAttributeSettingsRequest } from '@/shared/api';
import { queryClient } from '@/shared/query/query-client';
import { server, testApiBaseUrl } from '@/test/server';

describe('AttachAttributeModal', () => {
  it('shows every same-name record and disables only linked IDs', async () => {
    const user = userEvent.setup();
    const catalog = [501, 502, 503].map((id) => ({
      id,
      domainId: 101,
      name: 'socket',
      label: 'Сокет',
      dataType: 'STRING' as const,
      componentTypeIds: id === 503 ? [11] : [],
    }));
    server.use(
      http.get(`${testApiBaseUrl}/domains/101/attributes`, () => HttpResponse.json(catalog)),
    );
    render(
      <AppProviders>
        <AttachAttributeModal
          opened
          domainId={101}
          componentTypeId={11}
          linkedAttributes={[catalog[0]!]}
          onClose={vi.fn()}
        />
      </AppProviders>,
    );
    const select = await screen.findByRole('combobox', { name: 'Атрибут из каталога' });
    await waitFor(() =>
      expect(queryClient.getQueryData(attributeKeys.catalog(101))).toHaveLength(3),
    );
    await waitFor(() => expect(select).toBeEnabled());
    await user.type(select, 'Сокет');
    // JSDOM has no layout: Mantine's floating dropdown is hidden for a zero-width anchor.
    // Browser E2E verifies visibility; here inspect all rendered options and keyboard selection.
    expect(await screen.findAllByRole('option', { hidden: true })).toHaveLength(3);
    const linked = screen.getAllByRole('option', {
      name: 'Сокет (socket) — уже подключён',
      hidden: true,
    });
    expect(linked).toHaveLength(2);
    linked.forEach((option) => expect(option).toHaveAttribute('data-combobox-disabled', 'true'));
    await user.keyboard('{ArrowDown}{Enter}');
    expect(select).toHaveValue('Сокет (socket)');
    expect(screen.getByRole('button', { name: 'Подключить' })).toBeEnabled();
  });

  it('marks linked definitions and attaches the selected catalog attribute with type settings', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    let submitted: ComponentTypeAttributeSettingsRequest | undefined;
    server.use(
      http.get(`${testApiBaseUrl}/domains/101/attributes`, () =>
        HttpResponse.json([
          {
            id: 501,
            domainId: 101,
            name: 'socket',
            label: 'Сокет',
            dataType: 'STRING',
            componentTypeIds: [11],
          },
          {
            id: 502,
            domainId: 101,
            name: 'memory_standard',
            label: 'Стандарт памяти',
            dataType: 'STRING',
            componentTypeIds: [],
          },
        ]),
      ),
      http.put(`${testApiBaseUrl}/component-types/11/attributes/502`, async ({ request }) => {
        submitted = (await request.json()) as ComponentTypeAttributeSettingsRequest;
        return HttpResponse.json({
          id: 502,
          domainId: 101,
          componentTypeId: 11,
          name: 'memory_standard',
          label: 'Стандарт памяти',
          dataType: 'STRING',
          isRequired: true,
          orderIndex: 5,
        });
      }),
    );

    render(
      <AppProviders>
        <AttachAttributeModal
          opened
          domainId={101}
          componentTypeId={11}
          linkedAttributes={[
            {
              id: 501,
              domainId: 101,
              componentTypeId: 11,
              name: 'socket',
              label: 'Сокет',
              dataType: 'STRING',
            },
          ]}
          onClose={onClose}
        />
      </AppProviders>,
    );

    const dialog = await screen.findByRole('dialog', {
      name: 'Использовать существующий атрибут',
    });
    const catalogSelect = within(dialog).getByRole('combobox', {
      name: 'Атрибут из каталога',
    });
    await waitFor(() =>
      expect(queryClient.getQueryData(attributeKeys.catalog(101))).toHaveLength(2),
    );
    await user.type(catalogSelect, 'Стандарт');
    await user.keyboard('{ArrowDown}{Enter}');
    expect(catalogSelect).toHaveValue('Стандарт памяти (memory_standard)');
    await user.click(within(dialog).getByRole('switch', { name: 'Обязательный атрибут' }));
    await user.type(within(dialog).getByRole('textbox', { name: /Индекс порядка/ }), '5');
    await user.click(within(dialog).getByRole('button', { name: 'Подключить' }));

    expect(submitted).toEqual({ isRequired: true, orderIndex: 5 });
    expect(onClose).toHaveBeenCalledOnce();
    expect(await screen.findByText('Атрибут подключён к типу')).toBeVisible();
  });
});
