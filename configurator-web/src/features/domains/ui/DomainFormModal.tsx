import { zodResolver } from '@hookform/resolvers/zod';
import { Button, Group, Modal, Stack, Textarea, TextInput } from '@mantine/core';
import { useEffect, useMemo } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';

import { useCreateDomainMutation, useUpdateDomainMutation } from '@/features/domains/api/domains';
import type { Domain } from '@/shared/api';
import { getFieldErrors } from '@/shared/api';
import { showSuccessNotification } from '@/shared/notifications/notifications';

interface DomainFormValues {
  name: string;
  description: string;
}

interface DomainFormModalProps {
  opened: boolean;
  domain?: Domain | undefined;
  onClose: () => void;
  onSaved: (domain: Domain) => void;
}

function toRequest(values: DomainFormValues) {
  const name = values.name.trim();
  const description = values.description.trim();
  return description ? { name, description } : { name };
}

export function DomainFormModal({ opened, domain, onClose, onSaved }: DomainFormModalProps) {
  const { t } = useTranslation();
  const createDomain = useCreateDomainMutation();
  const updateDomain = useUpdateDomainMutation();
  const isEditing = Boolean(domain);
  const isPending = createDomain.isPending || updateDomain.isPending;
  const schema = useMemo(
    () =>
      z.object({
        name: z
          .string()
          .trim()
          .min(1, t('domains.form.validation.nameRequired'))
          .max(255, t('domains.form.validation.nameTooLong')),
        description: z.string(),
      }),
    [t],
  );
  const form = useForm<DomainFormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', description: '' },
  });

  useEffect(() => {
    if (opened) {
      form.reset({ name: domain?.name ?? '', description: domain?.description ?? '' });
    }
  }, [domain, form, opened]);

  const close = () => {
    if (!isPending) {
      onClose();
    }
  };

  const submit = form.handleSubmit(async (values) => {
    try {
      const request = toRequest(values);
      const savedDomain = domain
        ? await updateDomain.mutateAsync({ id: domain.id, body: request })
        : await createDomain.mutateAsync(request);
      showSuccessNotification(
        isEditing ? t('domains.notifications.updated') : t('domains.notifications.created'),
      );
      onSaved(savedDomain);
      onClose();
    } catch (error) {
      const fieldErrors = getFieldErrors(error);
      for (const [fieldPath, messages] of Object.entries(fieldErrors)) {
        const field = fieldPath.split('.').at(-1);
        if ((field === 'name' || field === 'description') && messages[0]) {
          form.setError(field, { message: messages[0] });
        }
      }
    }
  });

  return (
    <Modal
      opened={opened}
      onClose={close}
      title={isEditing ? t('domains.form.editTitle') : t('domains.form.createTitle')}
      centered
      closeOnClickOutside={!isPending}
      closeOnEscape={!isPending}
    >
      <form
        onSubmit={(event) => {
          void submit(event);
        }}
        noValidate
      >
        <Stack gap="md">
          <TextInput
            label={t('domains.form.name')}
            placeholder={t('domains.form.namePlaceholder')}
            withAsterisk
            autoFocus
            maxLength={255}
            error={form.formState.errors.name?.message}
            {...form.register('name')}
          />
          <Textarea
            label={t('domains.form.description')}
            placeholder={t('domains.form.descriptionPlaceholder')}
            rows={4}
            error={form.formState.errors.description?.message}
            {...form.register('description')}
          />
          <Group justify="flex-end">
            <Button variant="default" onClick={close} disabled={isPending}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" loading={isPending}>
              {isEditing ? t('common.save') : t('common.create')}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
