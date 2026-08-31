import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { AppProviders } from '@/app/providers/AppProviders';
import { AttributeFormModal } from '@/features/attributes/ui/AttributeFormModal';
import { server, testApiBaseUrl } from '@/test/server';

describe('attribute name conflicts', () => {
  it.each([
    { catalogOnly: true, editing: false, path: '/domains/101/attributes', method: 'POST' },
    { catalogOnly: false, editing: false, path: '/component-types/11/attributes', method: 'POST' },
    { catalogOnly: true, editing: true, path: '/attributes/501', method: 'PUT' },
    { catalogOnly: false, editing: true, path: '/attributes/501', method: 'PUT' },
  ])(
    'marks name and preserves the form for $method $path (catalog=$catalogOnly)',
    async ({ catalogOnly, editing, path, method }) => {
      const user = userEvent.setup();
      const onSaved = vi.fn();
      const onClose = vi.fn();
      const requests: Array<string> = [];
      server.use(
        http.all(`${testApiBaseUrl}${path}`, ({ request }) => {
          requests.push(request.method);
          return HttpResponse.json(
            {
              timestamp: '2026-08-31T12:00:00Z',
              status: 409,
              error: 'Conflict',
              code: 'ENTITY_ALREADY_EXISTS',
              message: 'Attribute name conflict',
              path,
              details: [
                {
                  field: 'name',
                  code: 'ENTITY_ALREADY_EXISTS',
                  message: 'Attribute name conflict',
                },
              ],
            },
            { status: 409 },
          );
        }),
      );
      render(
        <AppProviders>
          <AttributeFormModal
            opened
            domainId={101}
            componentTypeId={11}
            catalogOnly={catalogOnly}
            attribute={
              editing
                ? { id: 501, domainId: 101, name: 'other', label: 'Сокет', dataType: 'STRING' }
                : undefined
            }
            onSaved={onSaved}
            onClose={onClose}
          />
        </AppProviders>,
      );
      const dialog = await screen.findByRole('dialog');
      const name = within(dialog).getByRole('textbox', { name: /Системное имя/ });
      await user.clear(name);
      await user.type(name, 'socket');
      if (!editing)
        await user.type(
          within(dialog).getByRole('textbox', { name: 'Название для пользователя' }),
          'Сокет',
        );
      await user.click(
        within(dialog).getByRole('button', { name: editing ? 'Сохранить' : 'Создать' }),
      );
      expect(
        await within(dialog).findByText(/Атрибут с таким системным именем уже есть в области/),
      ).toBeVisible();
      expect(name).toHaveAttribute('aria-invalid', 'true');
      expect(name).toHaveValue('socket');
      expect(requests).toEqual([method]);
      expect(onSaved).not.toHaveBeenCalled();
      expect(onClose).not.toHaveBeenCalled();
    },
  );
});
