import { zodResolver } from '@hookform/resolvers/zod';
import {
  Button,
  Divider,
  Group,
  Modal,
  Paper,
  Select,
  Stack,
  Text,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core';
import { useMemo } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { z } from 'zod';

import { useAttributesQuery } from '@/features/attributes/api/attributes';
import { useRegisterDomainChangeGuard } from '@/features/domains/model/use-domain-change-guard';
import {
  useCreateComponentMutation,
  useUpdateComponentMutation,
} from '@/features/components/api/components';
import { useUnsavedChangesGuard } from '@/features/components/model/use-unsaved-changes-guard';
import {
  getFieldErrors,
  type AttributeDefinition,
  type Component,
  type ComponentType,
  type UpdateComponentRequest,
} from '@/shared/api';
import { showSuccessNotification } from '@/shared/notifications/notifications';
import { ErrorState, LoadingState } from '@/shared/ui';

interface ComponentFormValues {
  componentTypeId: string;
  name: string;
  brand: string;
  description: string;
  attributes: Record<string, string>;
}

interface ComponentFormProps {
  domainId: number;
  componentTypes: ReadonlyArray<ComponentType>;
  component?: Component | undefined;
}

const decimalPattern = /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)$/;

function initialValues(component?: Component): ComponentFormValues {
  return {
    componentTypeId: component ? String(component.componentTypeId) : '',
    name: component?.name ?? '',
    brand: component?.brand ?? '',
    description: component?.description ?? '',
    attributes: Object.fromEntries(
      (component?.attributes ?? []).map((attribute) => [
        String(attribute.attributeDefinitionId),
        attribute.value ?? '',
      ]),
    ),
  };
}

function buildRequest(
  values: ComponentFormValues,
  attributes: ReadonlyArray<AttributeDefinition>,
): UpdateComponentRequest {
  const brand = values.brand.trim();
  const description = values.description.trim();
  return {
    componentTypeId: Number(values.componentTypeId),
    name: values.name.trim(),
    ...(brand ? { brand } : {}),
    ...(description ? { description } : {}),
    attributes: attributes.flatMap((attribute) => {
      const value = values.attributes[String(attribute.id)]?.trim() ?? '';
      return value ? [{ attributeDefinitionId: attribute.id, value }] : [];
    }),
  };
}

function AttributeField({
  attribute,
  control,
}: {
  attribute: AttributeDefinition;
  control: ReturnType<typeof useForm<ComponentFormValues>>['control'];
}) {
  const { t } = useTranslation();
  const name = `attributes.${attribute.id}` as const;
  const label = attribute.isRequired
    ? t('components.form.requiredAttribute', { label: attribute.label })
    : attribute.label;

  return (
    <Controller
      name={name}
      control={control}
      defaultValue=""
      render={({ field, fieldState }) => {
        if (attribute.dataType === 'BOOLEAN') {
          return (
            <Select
              label={label}
              description={attribute.name}
              data={[
                { value: 'true', label: t('components.form.boolean.true') },
                { value: 'false', label: t('components.form.boolean.false') },
              ]}
              value={field.value || null}
              onChange={(value) => field.onChange(value ?? '')}
              onBlur={field.onBlur}
              clearable={!attribute.isRequired}
              allowDeselect={!attribute.isRequired}
              withAsterisk={attribute.isRequired}
              error={fieldState.error?.message}
            />
          );
        }
        if (attribute.dataType === 'ENUM') {
          return (
            <Select
              label={label}
              description={attribute.name}
              data={(attribute.enumValues ?? []).map((value) => ({ value, label: value }))}
              value={field.value || null}
              onChange={(value) => field.onChange(value ?? '')}
              onBlur={field.onBlur}
              searchable
              clearable={!attribute.isRequired}
              allowDeselect={!attribute.isRequired}
              withAsterisk={attribute.isRequired}
              error={fieldState.error?.message}
            />
          );
        }
        return (
          <TextInput
            label={label}
            description={attribute.name}
            inputMode={attribute.dataType === 'NUMBER' ? 'decimal' : 'text'}
            withAsterisk={attribute.isRequired}
            error={fieldState.error?.message}
            {...field}
          />
        );
      }}
    />
  );
}

export function ComponentForm({ domainId, componentTypes, component }: ComponentFormProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const isEditing = Boolean(component);
  const schema = useMemo(
    () =>
      z.object({
        componentTypeId: z.string().min(1, t('components.form.validation.typeRequired')),
        name: z
          .string()
          .trim()
          .min(1, t('components.form.validation.nameRequired'))
          .max(255, t('components.form.validation.nameTooLong')),
        brand: z.string(),
        description: z.string(),
        attributes: z.record(z.string(), z.string()),
      }),
    [t],
  );
  const form = useForm<ComponentFormValues>({
    resolver: zodResolver(schema),
    defaultValues: initialValues(component),
  });
  const componentTypeIdValue = useWatch({ control: form.control, name: 'componentTypeId' });
  const componentTypeId = componentTypeIdValue ? Number(componentTypeIdValue) : null;
  const attributesQuery = useAttributesQuery(domainId, componentTypeId);
  const attributes = attributesQuery.data ?? [];
  const createComponent = useCreateComponentMutation();
  const updateComponent = useUpdateComponentMutation();
  const isPending = createComponent.isPending || updateComponent.isPending;
  const { blocker, allowNavigation } = useUnsavedChangesGuard(form.formState.isDirty);
  useRegisterDomainChangeGuard(form.formState.isDirty, allowNavigation);

  const validateAttributes = (values: ComponentFormValues) => {
    let valid = true;
    for (const attribute of attributes) {
      const field = `attributes.${attribute.id}` as const;
      const value = values.attributes[String(attribute.id)]?.trim() ?? '';
      if (attribute.isRequired && !value) {
        form.setError(field, { message: t('components.form.validation.attributeRequired') });
        valid = false;
      } else if (attribute.dataType === 'NUMBER' && value && !decimalPattern.test(value)) {
        form.setError(field, { message: t('components.form.validation.numberInvalid') });
        valid = false;
      }
    }
    return valid;
  };

  const submit = form.handleSubmit(async (values) => {
    if (!validateAttributes(values)) {
      return;
    }
    try {
      const body = buildRequest(values, attributes);
      const saved = component
        ? await updateComponent.mutateAsync({
            domainId,
            id: component.id,
            body,
          })
        : await createComponent.mutateAsync({ domainId, body });
      showSuccessNotification(
        component ? t('components.notifications.updated') : t('components.notifications.created'),
      );
      allowNavigation();
      void navigate(`/components/${saved.id}`, { replace: true });
    } catch (error) {
      const fieldErrors = getFieldErrors(error);
      for (const [path, messages] of Object.entries(fieldErrors)) {
        const message = messages[0];
        if (!message) continue;
        if (path.includes('componentTypeId')) form.setError('componentTypeId', { message });
        else if (path.includes('name')) form.setError('name', { message });
        else if (path.includes('brand')) form.setError('brand', { message });
        else if (path.includes('description')) form.setError('description', { message });
        else if (path.includes('attributes')) {
          const index = Number(path.match(/\[(\d+)]/)?.[1]);
          const definition = Number.isInteger(index) ? attributes[index] : undefined;
          if (definition) form.setError(`attributes.${definition.id}`, { message });
        }
      }
    }
  });

  return (
    <>
      <form onSubmit={(event) => void submit(event)} noValidate>
        <Stack gap="lg">
          <Paper p="lg" withBorder>
            <Stack gap="md">
              <Title order={2} size="h3">
                {t('components.form.sections.main')}
              </Title>
              <Controller
                name="componentTypeId"
                control={form.control}
                render={({ field, fieldState }) => (
                  <Select
                    label={t('components.form.type')}
                    placeholder={t('components.form.typePlaceholder')}
                    data={componentTypes.map((type) => ({
                      value: String(type.id),
                      label: type.name,
                    }))}
                    value={field.value || null}
                    onChange={(value) => {
                      field.onChange(value ?? '');
                      form.setValue('attributes', {}, { shouldDirty: true });
                    }}
                    onBlur={field.onBlur}
                    searchable
                    disabled={isEditing}
                    withAsterisk
                    error={fieldState.error?.message}
                  />
                )}
              />
              <TextInput
                label={t('components.form.name')}
                placeholder={t('components.form.namePlaceholder')}
                withAsterisk
                maxLength={255}
                autoFocus={!isEditing}
                error={form.formState.errors.name?.message}
                {...form.register('name')}
              />
              <TextInput
                label={t('components.form.brand')}
                placeholder={t('components.form.brandPlaceholder')}
                error={form.formState.errors.brand?.message}
                {...form.register('brand')}
              />
              <Textarea
                label={t('components.form.description')}
                placeholder={t('components.form.descriptionPlaceholder')}
                rows={4}
                error={form.formState.errors.description?.message}
                {...form.register('description')}
              />
            </Stack>
          </Paper>

          {componentTypeId !== null ? (
            <Paper p="lg" withBorder>
              <Stack gap="md">
                <Stack gap={3}>
                  <Title order={2} size="h3">
                    {t('components.form.sections.attributes')}
                  </Title>
                  <Text size="sm" c="dimmed">
                    {t('components.form.attributesHint')}
                  </Text>
                </Stack>
                <Divider />
                {attributesQuery.isPending ? (
                  <LoadingState label={t('components.form.loadingAttributes')} />
                ) : null}
                {attributesQuery.error ? (
                  <ErrorState
                    error={attributesQuery.error}
                    onRetry={() => void attributesQuery.refetch()}
                  />
                ) : null}
                {!attributesQuery.isPending && !attributesQuery.error && attributes.length === 0 ? (
                  <Text c="dimmed">{t('components.form.noAttributes')}</Text>
                ) : null}
                {attributes.map((attribute) => (
                  <AttributeField key={attribute.id} attribute={attribute} control={form.control} />
                ))}
              </Stack>
            </Paper>
          ) : null}

          <Group justify="flex-end">
            <Button
              variant="default"
              onClick={() =>
                void navigate(component ? `/components/${component.id}` : '/components')
              }
              disabled={isPending}
            >
              {t('common.cancel')}
            </Button>
            <Button
              type="submit"
              loading={isPending}
              disabled={
                componentTypeId !== null &&
                (attributesQuery.isPending || Boolean(attributesQuery.error))
              }
            >
              {component ? t('common.save') : t('common.create')}
            </Button>
          </Group>
        </Stack>
      </form>

      <Modal
        opened={blocker.state === 'blocked'}
        onClose={() => blocker.reset?.()}
        title={t('components.form.unsaved.title')}
        centered
      >
        <Stack gap="md">
          <Text>{t('components.form.unsaved.description')}</Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => blocker.reset?.()}>
              {t('components.form.unsaved.stay')}
            </Button>
            <Button color="red" onClick={() => blocker.proceed?.()}>
              {t('components.form.unsaved.leave')}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </>
  );
}
