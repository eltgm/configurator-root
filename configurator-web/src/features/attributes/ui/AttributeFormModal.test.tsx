import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import type { ComponentProps } from 'react';
import { describe, expect, it, vi } from 'vitest';

import { AppProviders } from '@/app/providers/AppProviders';
import { AttributeFormModal } from '@/features/attributes/ui/AttributeFormModal';
import type { AttributeDefinition } from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

const savedAttribute: AttributeDefinition = {
  id: 501,
  domainId: 101,
  name: 'Memory_Size',
  label: 'Объём памяти',
  dataType: 'STRING',
};

async function renderForm(overrides: Partial<ComponentProps<typeof AttributeFormModal>> = {}) {
  const props = {
    opened: true,
    domainId: 101,
    componentTypeId: 11,
    catalogOnly: true,
    onClose: vi.fn(),
    onSaved: vi.fn(),
    ...overrides,
  };
  const view = render(<AttributeFormModal {...props} />, { wrapper: AppProviders });
  const dialog = within(await screen.findByRole('dialog'));
  return {
    ...view,
    props,
    dialog,
    user: userEvent.setup(),
    label: dialog.getByRole('textbox', { name: 'Название для пользователя' }),
    name: dialog.getByRole('textbox', { name: /Системное имя/ }),
  };
}

describe('attribute name autofill', () => {
  it.each([
    { catalogOnly: true, path: '/domains/101/attributes' },
    { catalogOnly: false, path: '/component-types/11/attributes' },
  ])('submits the latest suggestion immediately from $path', async ({ catalogOnly, path }) => {
    const requests: Array<unknown> = [];
    server.use(
      http.post(`${testApiBaseUrl}${path}`, async ({ request }) => {
        requests.push(await request.json());
        return HttpResponse.json(savedAttribute);
      }),
    );
    const { user, label, name, props } = await renderForm({ catalogOnly });
    await user.type(label, 'Объём памяти');
    expect(name).toHaveValue('obyom_pamyati');
    await user.type(label, ' DDR5{Enter}');
    await waitFor(() => expect(props.onSaved).toHaveBeenCalledOnce());
    expect(requests).toEqual([
      {
        name: 'obyom_pamyati_ddr5',
        label: 'Объём памяти DDR5',
        dataType: 'STRING',
        ...(catalogOnly ? {} : { isRequired: false }),
      },
    ]);
  });

  it('keeps autofill after focus and blur, and clears a suggestion with its label', async () => {
    const { user, label, name } = await renderForm();
    await user.type(label, 'Memory');
    await user.tab();
    expect(name).toHaveFocus();
    await user.type(label, 'Size');
    expect(name).toHaveValue('memory_size');
    await user.clear(label);
    expect(name).toHaveValue('');
    await user.type(label, 'Разъём USB');
    expect(name).toHaveValue('razyom_usb');
  });

  it('preserves a manually entered name even if it was filled first', async () => {
    const { user, label, name } = await renderForm();
    await user.type(name, 'MyCustom_Name');
    await user.type(label, 'Объём памяти');
    expect(name).toHaveValue('MyCustom_Name');
  });

  it('does not resume autofill after manual clearing or restoring the suggested text', async () => {
    const { user, label, name, dialog } = await renderForm();
    await user.type(label, 'Memory');
    await user.clear(name);
    await user.type(label, ' Size');
    expect(name).toHaveValue('');
    await user.type(name, 'memory_size');
    await user.type(label, ' DDR5');
    expect(name).toHaveValue('memory_size');
    await user.click(dialog.getByRole('button', { name: 'Заполнять из названия' }));
    expect(name).toHaveValue('memory_size_ddr5');
    expect(name).toHaveFocus();
    await user.type(label, ' Max');
    expect(name).toHaveValue('memory_size_ddr5_max');
  });

  it('never derives an existing name until the user explicitly enables autofill', async () => {
    const { user, label, name, dialog } = await renderForm({ attribute: savedAttribute });
    expect(name).toHaveValue('Memory_Size');
    await user.type(label, ' DDR5');
    expect(name).toHaveValue('Memory_Size');
    await user.clear(name);
    await user.type(label, ' Max');
    expect(name).toHaveValue('');
    await user.click(dialog.getByRole('button', { name: 'Заполнять из названия' }));
    expect(name).toHaveValue('obyom_pamyati_ddr5_max');
    await user.clear(label);
    await user.type(label, 'Capacity');
    expect(name).toHaveValue('capacity');
  });

  it('resets manual mode and values when the creation dialog is reopened', async () => {
    const { user, label, name, props, rerender } = await renderForm();
    await user.type(name, 'manual');
    await user.type(label, 'First');
    rerender(<AttributeFormModal {...props} opened={false} />);
    rerender(<AttributeFormModal {...props} />);
    const reopenedLabel = screen.getByRole('textbox', { name: 'Название для пользователя' });
    const reopenedName = screen.getByRole('textbox', { name: /Системное имя/ });
    expect(reopenedLabel).toHaveValue('');
    expect(reopenedName).toHaveValue('');
    await user.type(reopenedLabel, 'Second');
    expect(reopenedName).toHaveValue('second');
  });

  it.each([{ domainId: 202 }, { componentTypeId: 12 }, { catalogOnly: false }])(
    'resets autofill when the form context changes: %j',
    async (context) => {
      const { user, label, name, props, rerender } = await renderForm();
      await user.type(name, 'manual');
      await user.type(label, 'First');
      rerender(<AttributeFormModal {...props} {...context} />);
      expect(name).toHaveValue('');
      await user.type(label, 'Second');
      expect(name).toHaveValue('second');
    },
  );

  it('protects another saved attribute when switching from an explicitly enabled edit', async () => {
    const { user, label, name, dialog, props, rerender } = await renderForm({
      attribute: savedAttribute,
    });
    await user.click(dialog.getByRole('button', { name: 'Заполнять из названия' }));
    rerender(
      <AttributeFormModal
        {...props}
        attribute={{ ...savedAttribute, id: 502, name: 'custom', label: 'Other' }}
      />,
    );
    await user.type(label, ' Changed');
    expect(name).toHaveValue('custom');
    rerender(<AttributeFormModal {...props} attribute={undefined} />);
    await user.type(label, 'New');
    expect(name).toHaveValue('new');
  });

  it('does not invent a name for unsupported characters and allows a manual replacement', async () => {
    const { user, label, name, dialog, props } = await renderForm();
    await user.type(label, '💻 !!!');
    expect(name).toHaveValue('');
    await user.click(dialog.getByRole('button', { name: 'Создать' }));
    expect(await dialog.findByText('Введите системное имя')).toBeVisible();
    expect(props.onSaved).not.toHaveBeenCalled();
    await user.type(name, 'computer');
    expect(dialog.queryByText('Введите системное имя')).not.toBeInTheDocument();
  });

  it('shows expansion overflow without truncation and clears the error after correction', async () => {
    const { user, label, name, dialog, props } = await renderForm();
    fireEvent.change(label, { target: { value: 'щ'.repeat(65) } });
    expect(name).toHaveValue('shch'.repeat(65));
    expect(
      await dialog.findByText(/Системное имя должно содержать не более 255 символов/),
    ).toBeVisible();
    await user.click(dialog.getByRole('button', { name: 'Создать' }));
    expect(props.onSaved).not.toHaveBeenCalled();
    fireEvent.change(label, { target: { value: 'Щука' } });
    expect(name).toHaveValue('shchuka');
    await waitFor(() => expect(name).not.toHaveAttribute('aria-invalid', 'true'));
  });
});

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
