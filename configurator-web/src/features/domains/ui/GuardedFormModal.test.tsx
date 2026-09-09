import { Button, TextInput } from '@mantine/core';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { createMemoryRouter, RouterProvider, useNavigate } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AppProviders } from '@/app/providers/AppProviders';
import { useFormCompletion } from '@/features/domains/model/use-form-completion';
import { FormModalCancelButton, GuardedFormModal } from './GuardedFormModal';

function Form({ pending = false }: { pending?: boolean }) {
  const [opened, setOpened] = useState(true);
  const [value, setValue] = useState('');
  const navigate = useNavigate();
  const { markComplete, canLeave } = useFormCompletion(opened);
  return (
    <>
      <Button
        onClick={() => {
          setValue('');
          setOpened(true);
        }}
      >
        Open
      </Button>
      <GuardedFormModal
        opened={opened}
        dirty={Boolean(value)}
        pending={pending}
        canLeave={canLeave}
        title="Form"
        onClose={() => setOpened(false)}
      >
        <TextInput
          label="Value"
          value={value}
          onChange={(event) => setValue(event.currentTarget.value)}
        />
        <FormModalCancelButton>Cancel</FormModalCancelButton>
        <Button
          onClick={() => {
            markComplete();
            void navigate('/done');
            setOpened(false);
          }}
        >
          Save
        </Button>
      </GuardedFormModal>
    </>
  );
}
function setup(pending = false) {
  const router = createMemoryRouter([
    { path: '/', element: <Form pending={pending} /> },
    { path: '/done', element: <p>Done</p> },
  ]);
  render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  );
  return { router, user: userEvent.setup() };
}

describe('guarded modal lifecycle', () => {
  it('closes a pristine form immediately but keeps a pending form open', async () => {
    const { user } = setup(true);
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.getByRole('dialog', { name: 'Form' })).toBeInTheDocument();
  });
  it('preserves dirty values and restores focus after staying, then discards and reopens', async () => {
    const { user } = setup();
    const input = screen.getByRole('textbox', { name: 'Value' });
    await user.type(input, 'Draft');
    const event = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(event);
    expect(event.defaultPrevented).toBe(true);
    await user.keyboard('{Escape}');
    await user.click(await screen.findByRole('button', { name: 'Продолжить заполнение' }));
    expect(input).toHaveValue('Draft');
    await waitFor(() => expect(input).toHaveFocus());
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    await user.click(screen.getByRole('button', { name: 'Отбросить изменения' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    const cleanEvent = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(cleanEvent);
    expect(cleanEvent.defaultPrevented).toBe(false);
    await user.click(screen.getByRole('button', { name: 'Open' }));
    expect(await screen.findByRole('textbox')).toHaveValue('');
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });
  it('cancels and confirms route navigation', async () => {
    const { user, router } = setup();
    await user.type(screen.getByRole('textbox'), 'Draft');
    await act(async () => {
      await router.navigate('/done');
    });
    await user.click(await screen.findByRole('button', { name: 'Продолжить заполнение' }));
    expect(router.state.location.pathname).toBe('/');
    expect(screen.getByRole('textbox')).toHaveValue('Draft');
    await act(async () => {
      await router.navigate('/done');
    });
    await user.click(await screen.findByRole('button', { name: 'Отбросить изменения' }));
    expect(await screen.findByText('Done')).toBeInTheDocument();
  });
  it('allows navigation synchronously after a successful save', async () => {
    const { user } = setup();
    await user.type(screen.getByRole('textbox'), 'Saved');
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Done')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
