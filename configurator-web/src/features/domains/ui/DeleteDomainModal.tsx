import { zodResolver } from '@hookform/resolvers/zod';
import { Button, Group, Modal, Stack, Text, TextInput } from '@mantine/core';
import { useMemo } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';

import { useDeleteDomainMutation } from '@/features/domains/api/domains';
import type { Domain } from '@/shared/api';
import { showSuccessNotification } from '@/shared/notifications/notifications';

interface DeleteDomainModalProps {
  domain: Domain;
  onClose: () => void;
}

export function DeleteDomainModal({ domain, onClose }: DeleteDomainModalProps) {
  const { t } = useTranslation();
  const deleteDomain = useDeleteDomainMutation();
  const schema = useMemo(
    () =>
      z.object({
        name: z.string().refine((value) => value === domain.name, t('domains.delete.nameMismatch')),
      }),
    [domain.name, t],
  );
  const form = useForm<{ name: string }>({
    resolver: zodResolver(schema),
    defaultValues: { name: '' },
  });
  const confirmationName = useWatch({ control: form.control, name: 'name' });
  const close = () => {
    if (!deleteDomain.isPending) onClose();
  };
  const submit = form.handleSubmit(async () => {
    if (deleteDomain.isPending) return;
    try {
      await deleteDomain.mutateAsync(domain.id);
      showSuccessNotification(t('domains.notifications.deleted'));
      onClose();
    } catch {
      // The global mutation policy shows the specific, localized API error; keep the modal open.
    }
  });

  return (
    <Modal
      opened
      onClose={close}
      title={t('domains.delete.title')}
      centered
      closeButtonProps={{ 'aria-label': t('common.close'), disabled: deleteDomain.isPending }}
      closeOnClickOutside={!deleteDomain.isPending}
      closeOnEscape={!deleteDomain.isPending}
    >
      <form
        onSubmit={(event) => {
          void submit(event);
        }}
        noValidate
      >
        <Stack gap="md">
          <Text>{t('domains.delete.description', { name: domain.name })}</Text>
          <Text size="sm" c="red">
            {t('domains.delete.warning')}
          </Text>
          <TextInput
            label={t('domains.delete.confirmName')}
            description={t('domains.delete.confirmNameDescription', { name: domain.name })}
            autoComplete="off"
            autoFocus
            withAsterisk
            disabled={deleteDomain.isPending}
            error={form.formState.errors.name?.message}
            {...form.register('name')}
          />
          <Group justify="flex-end">
            <Button variant="default" disabled={deleteDomain.isPending} onClick={close}>
              {t('common.cancel')}
            </Button>
            <Button
              type="submit"
              color="red"
              loading={deleteDomain.isPending}
              disabled={confirmationName !== domain.name || deleteDomain.isPending}
            >
              {t('domains.actions.delete')}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
