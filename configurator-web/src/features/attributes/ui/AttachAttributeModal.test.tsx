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
  it('filters linked definitions and attaches the selected catalog attribute with type settings', async () => {
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
