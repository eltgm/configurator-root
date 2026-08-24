import { zodResolver } from '@hookform/resolvers/zod';
import {
  Button,
  Group,
  Modal,
  NumberInput,
  Select,
  Stack,
  Switch,
  TagsInput,
  TextInput,
} from '@mantine/core';
import { useEffect, useMemo } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';

import {
  useCreateAttributeMutation,
  useUpdateAttributeMutation,
} from '@/features/attributes/api/attributes';
import {
  getFieldErrors,
  type AttributeDefinition,
  type CreateAttributeDefinitionRequest,
} from '@/shared/api';
import { showSuccessNotification } from '@/shared/notifications/notifications';

type AttributeDataType = CreateAttributeDefinitionRequest['dataType'];

interface AttributeFormValues {
  name: string;
  label: string;
  dataType: AttributeDataType;
  enumValues: Array<string>;
  isRequired: boolean;
  orderIndex: number | '';
}

interface AttributeFormModalProps {
  opened: boolean;
  domainId: number;
  componentTypeId: number;
  attribute?: AttributeDefinition | undefined;
  onClose: () => void;
  onSaved: (attribute: AttributeDefinition) => void;
}

function normalizeEnumValues(values: ReadonlyArray<string>): Array<string> {
  return values.map((value) => value.trim()).filter(Boolean);
}

function toRequest(values: AttributeFormValues): CreateAttributeDefinitionRequest {
  return {
    name: values.name.trim(),
    label: values.label.trim(),
    dataType: values.dataType,
    ...(values.dataType === 'ENUM' ? { enumValues: normalizeEnumValues(values.enumValues) } : {}),
    isRequired: values.isRequired,
    ...(typeof values.orderIndex === 'number' ? { orderIndex: values.orderIndex } : {}),
  };
}

export function AttributeFormModal({
  opened,
  domainId,
  componentTypeId,
  attribute,
  onClose,
  onSaved,
}: AttributeFormModalProps) {
  const { t } = useTranslation();
  const createAttribute = useCreateAttributeMutation();
  const updateAttribute = useUpdateAttributeMutation();
  const isEditing = Boolean(attribute);
  const isPending = createAttribute.isPending || updateAttribute.isPending;
  const schema = useMemo(
    () =>
      z
        .object({
          name: z
            .string()
            .trim()
            .min(1, t('attributes.form.validation.nameRequired'))
            .max(255, t('attributes.form.validation.nameTooLong')),
          label: z
            .string()
            .trim()
            .min(1, t('attributes.form.validation.labelRequired'))
            .max(255, t('attributes.form.validation.labelTooLong')),
          dataType: z.enum(['STRING', 'NUMBER', 'BOOLEAN', 'ENUM']),
          enumValues: z.array(z.string()),
          isRequired: z.boolean(),
          orderIndex: z.union([
            z.literal(''),
            z
              .number()
              .int(t('common.validation.integer'))
              .min(0, t('common.validation.nonNegative')),
          ]),
        })
        .superRefine((values, context) => {
          if (values.dataType !== 'ENUM') {
            return;
          }
          const normalized = normalizeEnumValues(values.enumValues);
          if (normalized.length === 0) {
            context.addIssue({
              code: 'custom',
              path: ['enumValues'],
              message: t('attributes.form.validation.enumRequired'),
            });
          } else if (new Set(normalized).size !== normalized.length) {
            context.addIssue({
              code: 'custom',
              path: ['enumValues'],
              message: t('attributes.form.validation.enumUnique'),
            });
          }
        }),
    [t],
  );
  const form = useForm<AttributeFormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      label: '',
      dataType: 'STRING',
      enumValues: [],
      isRequired: false,
      orderIndex: '',
    },
  });
  const dataType = useWatch({ control: form.control, name: 'dataType' });

  useEffect(() => {
    if (opened) {
      form.reset({
        name: attribute?.name ?? '',
        label: attribute?.label ?? '',
        dataType: attribute?.dataType ?? 'STRING',
        enumValues: attribute?.enumValues ?? [],
        isRequired: attribute?.isRequired ?? false,
        orderIndex: attribute?.orderIndex ?? '',
      });
    }
  }, [attribute, form, opened]);

  const close = () => {
    if (!isPending) {
      onClose();
    }
  };

  const submit = form.handleSubmit(async (values) => {
    try {
      const body = toRequest(values);
      const savedAttribute = attribute
        ? await updateAttribute.mutateAsync({
            domainId,
            componentTypeId,
            id: attribute.id,
            body,
          })
        : await createAttribute.mutateAsync({ domainId, componentTypeId, body });
      showSuccessNotification(
        isEditing ? t('attributes.notifications.updated') : t('attributes.notifications.created'),
      );
      onSaved(savedAttribute);
      onClose();
    } catch (error) {
      const fieldErrors = getFieldErrors(error);
      for (const field of [
        'name',
        'label',
        'dataType',
        'enumValues',
        'isRequired',
        'orderIndex',
      ] as const) {
        const messages = Object.entries(fieldErrors).find(([path]) => path.includes(field))?.[1];
        if (messages?.[0]) {
          form.setError(field, { message: messages[0] });
        }
      }
    }
  });

  const typeOptions = (['STRING', 'NUMBER', 'BOOLEAN', 'ENUM'] as const).map((value) => ({
    value,
    label: t(`attributes.dataTypes.${value}`),
  }));

  return (
    <Modal
      opened={opened}
      onClose={close}
      title={isEditing ? t('attributes.form.editTitle') : t('attributes.form.createTitle')}
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
            label={t('attributes.form.name')}
            description={t('attributes.form.nameHint')}
            placeholder={t('attributes.form.namePlaceholder')}
            withAsterisk
            autoFocus
            maxLength={255}
            error={form.formState.errors.name?.message}
            {...form.register('name')}
          />
          <TextInput
            label={t('attributes.form.label')}
            placeholder={t('attributes.form.labelPlaceholder')}
            withAsterisk
            maxLength={255}
            error={form.formState.errors.label?.message}
            {...form.register('label')}
          />
          <Controller
            name="dataType"
            control={form.control}
            render={({ field, fieldState }) => (
              <Select
                label={t('attributes.form.dataType')}
                data={typeOptions}
                value={field.value}
                onChange={(value) => field.onChange(value ?? 'STRING')}
                onBlur={field.onBlur}
                allowDeselect={false}
                error={fieldState.error?.message}
              />
            )}
          />
          {dataType === 'ENUM' ? (
            <Controller
              name="enumValues"
              control={form.control}
              render={({ field, fieldState }) => (
                <TagsInput
                  label={t('attributes.form.enumValues')}
                  description={t('attributes.form.enumValuesHint')}
                  placeholder={t('attributes.form.enumValuesPlaceholder')}
                  value={field.value}
                  onChange={field.onChange}
                  onBlur={field.onBlur}
                  splitChars={[',']}
                  error={fieldState.error?.message}
                />
              )}
            />
          ) : null}
          <Controller
            name="isRequired"
            control={form.control}
            render={({ field }) => (
              <Switch
                label={t('attributes.form.isRequired')}
                checked={field.value}
                onChange={(event) => field.onChange(event.currentTarget.checked)}
              />
            )}
          />
          <Controller
            name="orderIndex"
            control={form.control}
            render={({ field, fieldState }) => (
              <NumberInput
                label={t('attributes.form.orderIndex')}
                description={t('attributes.form.orderIndexHint')}
                min={0}
                allowDecimal={false}
                allowNegative={false}
                clampBehavior="strict"
                value={field.value}
                onBlur={field.onBlur}
                onChange={field.onChange}
                error={fieldState.error?.message}
              />
            )}
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
