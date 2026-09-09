import { useFormCompletion } from '@/features/domains/model/use-form-completion';
import { ErrorState } from '@/shared/ui';
import { GuardedFormModal, FormModalCancelButton } from '@/features/domains/ui/GuardedFormModal';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button, Group, NumberInput, Stack, Textarea, TextInput } from '@mantine/core';
import { useEffect, useMemo } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';

import {
  useCreateComponentTypeMutation,
  useUpdateComponentTypeMutation,
} from '@/features/component-types/api/component-types';
import { getFieldErrors, type ComponentType, type CreateComponentTypeRequest } from '@/shared/api';
import { showSuccessNotification } from '@/shared/notifications/notifications';

interface ComponentTypeFormValues {
  name: string;
  code: string;
  description: string;
  orderIndex: number | '';
}

interface ComponentTypeFormModalProps {
  opened: boolean;
  domainId: number;
  componentType?: ComponentType | undefined;
  onClose: () => void;
  onSaved: (componentType: ComponentType) => void;
}

function toRequest(values: ComponentTypeFormValues): CreateComponentTypeRequest {
  const code = values.code.trim();
  const description = values.description.trim();
  return {
    name: values.name.trim(),
    ...(code ? { code } : {}),
    ...(description ? { description } : {}),
    ...(typeof values.orderIndex === 'number' ? { orderIndex: values.orderIndex } : {}),
  };
}

export function ComponentTypeFormModal({
  opened,
  domainId,
  componentType,
  onClose,
  onSaved,
}: ComponentTypeFormModalProps) {
  const { t } = useTranslation();
  const { markComplete, canLeave } = useFormCompletion(opened);
  const createType = useCreateComponentTypeMutation(true);
  const updateType = useUpdateComponentTypeMutation(true);
  const resetCreateType = createType.reset;
  const resetUpdateType = updateType.reset;
  useEffect(() => {
    if (opened) {
      resetCreateType();
      resetUpdateType();
    }
  }, [opened, resetCreateType, resetUpdateType]);
  const isEditing = Boolean(componentType);
  const isPending = createType.isPending || updateType.isPending;
  const schema = useMemo(
    () =>
      z.object({
        name: z
          .string()
          .trim()
          .min(1, t('componentTypes.form.validation.nameRequired'))
          .max(255, t('componentTypes.form.validation.nameTooLong')),
        code: z.string().trim().max(100, t('componentTypes.form.validation.codeTooLong')),
        description: z.string(),
        orderIndex: z.union([
          z.literal(''),
          z.number().int(t('common.validation.integer')).min(0, t('common.validation.nonNegative')),
        ]),
      }),
    [t],
  );
  const form = useForm<ComponentTypeFormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', code: '', description: '', orderIndex: '' },
  });

  useEffect(() => {
    if (opened) {
      form.reset({
        name: componentType?.name ?? '',
        code: componentType?.code ?? '',
        description: componentType?.description ?? '',
        orderIndex: componentType?.orderIndex ?? '',
      });
    }
  }, [componentType, form, opened]);

  const close = () => {
    if (!isPending) {
      onClose();
    }
  };

  const submit = form.handleSubmit(async (values) => {
    try {
      const body = toRequest(values);
      const savedType = componentType
        ? await updateType.mutateAsync({ domainId, id: componentType.id, body })
        : await createType.mutateAsync({ domainId, body });
      showSuccessNotification(
        isEditing
          ? t('componentTypes.notifications.updated')
          : t('componentTypes.notifications.created'),
      );
      markComplete();
      onSaved(savedType);
      onClose();
    } catch (error) {
      const fieldErrors = getFieldErrors(error);
      for (const field of ['name', 'code', 'description', 'orderIndex'] as const) {
        const messages = Object.entries(fieldErrors).find(([path]) => path.includes(field))?.[1];
        if (messages?.[0]) {
          form.setError(field, { message: messages[0] });
        }
      }
    }
  });

  return (
    <GuardedFormModal
      canLeave={canLeave}
      dirty={form.formState.isDirty}
      pending={isPending}
      opened={opened}
      onClose={close}
      title={isEditing ? t('componentTypes.form.editTitle') : t('componentTypes.form.createTitle')}
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
          {(createType.error ?? updateType.error) ? (
            <ErrorState autoFocus error={createType.error ?? updateType.error} />
          ) : null}
          <TextInput
            label={t('componentTypes.form.name')}
            placeholder={t('componentTypes.form.namePlaceholder')}
            withAsterisk
            autoFocus
            maxLength={255}
            error={form.formState.errors.name?.message}
            {...form.register('name')}
          />
          <TextInput
            label={t('componentTypes.form.code')}
            placeholder={t('componentTypes.form.codePlaceholder')}
            maxLength={100}
            error={form.formState.errors.code?.message}
            {...form.register('code')}
          />
          <Textarea
            label={t('componentTypes.form.description')}
            placeholder={t('componentTypes.form.descriptionPlaceholder')}
            rows={3}
            error={form.formState.errors.description?.message}
            {...form.register('description')}
          />
          <Controller
            name="orderIndex"
            control={form.control}
            render={({ field, fieldState }) => (
              <NumberInput
                label={t('componentTypes.form.orderIndex')}
                description={t('componentTypes.form.orderIndexHint')}
                min={0}
                allowDecimal={false}
                allowNegative={false}
                clampBehavior="strict"
                value={field.value}
                ref={field.ref}
                onBlur={field.onBlur}
                onChange={field.onChange}
                error={fieldState.error?.message}
              />
            )}
          />
          <Group justify="flex-end">
            <FormModalCancelButton variant="default" disabled={isPending}>
              {t('common.cancel')}
            </FormModalCancelButton>
            <Button type="submit" loading={isPending}>
              {isEditing ? t('common.save') : t('common.create')}
            </Button>
          </Group>
        </Stack>
      </form>
    </GuardedFormModal>
  );
}
