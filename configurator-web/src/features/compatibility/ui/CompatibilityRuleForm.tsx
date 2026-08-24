import { zodResolver } from '@hookform/resolvers/zod';
import {
  Alert,
  Button,
  Divider,
  Group,
  Modal,
  Paper,
  Select,
  Stack,
  Switch,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
import { IconInfoCircle, IconPlus } from '@tabler/icons-react';
import { useEffect, useMemo, useRef } from 'react';
import { Controller, type FieldPath, useFieldArray, useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { z } from 'zod';

import { useAttributesQuery } from '@/features/attributes/api/attributes';
import {
  useCreateCompatibilityRuleMutation,
  useUpdateCompatibilityRuleMutation,
} from '@/features/compatibility/api/compatibility-rules';
import {
  createEmptyCompatibilityCondition,
  getCompatibilityOperators,
  getCompatibilityRuleFieldErrors,
  hasDuplicateCompatibilityCondition,
  toCompatibilityRuleFormValues,
  toSaveCompatibilityRuleRequest,
  type CompatibilityRuleFormValues,
} from '@/features/compatibility/model/compatibility-rules';
import { CompatibilityRuleConditionFields } from '@/features/compatibility/ui/CompatibilityRuleConditionFields';
import { useUnsavedChangesGuard } from '@/features/components/model/use-unsaved-changes-guard';
import type { CompatibilityRuleSet, ComponentType } from '@/shared/api';
import { showSuccessNotification } from '@/shared/notifications/notifications';
import { ErrorState, LoadingState } from '@/shared/ui';

import classes from './compatibility-rule-form.module.css';

interface CompatibilityRuleFormProps {
  domainId: number;
  componentTypes: ComponentType[];
  rule?: CompatibilityRuleSet;
}

export function CompatibilityRuleForm({
  domainId,
  componentTypes,
  rule,
}: CompatibilityRuleFormProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const schema = useMemo(
    () =>
      z
        .object({
          name: z
            .string()
            .trim()
            .min(1, t('compatibilityRules.form.validation.nameRequired'))
            .max(255, t('compatibilityRules.form.validation.nameTooLong')),
          componentTypeAId: z
            .string()
            .min(1, t('compatibilityRules.form.validation.leftTypeRequired')),
          componentTypeBId: z
            .string()
            .min(1, t('compatibilityRules.form.validation.rightTypeRequired')),
          enabled: z.boolean(),
          conditions: z
            .array(
              z.object({
                leftAttributeDefinitionId: z
                  .string()
                  .min(1, t('compatibilityRules.form.validation.leftAttributeRequired')),
                operator: z
                  .string()
                  .min(1, t('compatibilityRules.form.validation.operatorRequired')),
                rightAttributeDefinitionId: z
                  .string()
                  .min(1, t('compatibilityRules.form.validation.rightAttributeRequired')),
              }),
            )
            .min(1, t('compatibilityRules.form.validation.conditionRequired')),
        })
        .superRefine((values, context) => {
          if (values.componentTypeAId && values.componentTypeAId === values.componentTypeBId) {
            context.addIssue({
              code: 'custom',
              path: ['componentTypeBId'],
              message: t('compatibilityRules.form.validation.distinctTypes'),
            });
          }
          values.conditions.forEach((_condition, index) => {
            if (hasDuplicateCompatibilityCondition(values.conditions, index)) {
              context.addIssue({
                code: 'custom',
                path: ['conditions', index, 'rightAttributeDefinitionId'],
                message: t('compatibilityRules.form.validation.duplicateCondition'),
              });
            }
          });
        }),
    [t],
  );
  const form = useForm<CompatibilityRuleFormValues>({
    resolver: zodResolver(schema),
    defaultValues: toCompatibilityRuleFormValues(rule),
  });
  const { fields, append, remove, move } = useFieldArray({
    control: form.control,
    name: 'conditions',
  });
  const componentTypeAValue = useWatch({ control: form.control, name: 'componentTypeAId' });
  const componentTypeBValue = useWatch({ control: form.control, name: 'componentTypeBId' });
  const watchedConditions = useWatch({ control: form.control, name: 'conditions' });
  const conditions = useMemo(() => watchedConditions ?? [], [watchedConditions]);
  const componentTypeAId = componentTypeAValue ? Number(componentTypeAValue) : null;
  const componentTypeBId = componentTypeBValue ? Number(componentTypeBValue) : null;
  const leftAttributesQuery = useAttributesQuery(domainId, componentTypeAId);
  const rightAttributesQuery = useAttributesQuery(domainId, componentTypeBId);
  const leftAttributes = leftAttributesQuery.data ?? [];
  const rightAttributes = rightAttributesQuery.data ?? [];
  const createRule = useCreateCompatibilityRuleMutation();
  const updateRule = useUpdateCompatibilityRuleMutation();
  const isPending = createRule.isPending || updateRule.isPending;
  const { blocker, allowNavigation } = useUnsavedChangesGuard(form.formState.isDirty);
  const previousTypes = useRef({ a: componentTypeAValue, b: componentTypeBValue });

  const validateAttributeSemantics = (values: CompatibilityRuleFormValues) => {
    let valid = true;
    values.conditions.forEach((condition, index) => {
      const leftAttribute = leftAttributes.find(
        (attribute) => attribute.id === Number(condition.leftAttributeDefinitionId),
      );
      const rightAttribute = rightAttributes.find(
        (attribute) => attribute.id === Number(condition.rightAttributeDefinitionId),
      );
      if (!leftAttribute) {
        form.setError(`conditions.${index}.leftAttributeDefinitionId`, {
          message: t('compatibilityRules.form.validation.unavailableAttribute'),
        });
        valid = false;
      }
      if (!rightAttribute) {
        form.setError(`conditions.${index}.rightAttributeDefinitionId`, {
          message: t('compatibilityRules.form.validation.unavailableAttribute'),
        });
        valid = false;
      }
      if (leftAttribute && rightAttribute && leftAttribute.dataType !== rightAttribute.dataType) {
        form.setError(`conditions.${index}.rightAttributeDefinitionId`, {
          message: t('compatibilityRules.form.validation.incompatibleAttributes'),
        });
        valid = false;
      }
      if (
        leftAttribute &&
        !getCompatibilityOperators(leftAttribute.dataType).some(
          (operator) => operator === condition.operator,
        )
      ) {
        form.setError(`conditions.${index}.operator`, {
          message: t('compatibilityRules.form.validation.numericOperator'),
        });
        valid = false;
      }
    });
    return valid;
  };

  useEffect(() => {
    if (previousTypes.current.a !== componentTypeAValue) {
      conditions.forEach((_condition, index) => {
        form.setValue(`conditions.${index}.leftAttributeDefinitionId`, '', {
          shouldDirty: true,
        });
        form.setValue(`conditions.${index}.operator`, '', { shouldDirty: true });
        form.setValue(`conditions.${index}.rightAttributeDefinitionId`, '', {
          shouldDirty: true,
        });
      });
      previousTypes.current.a = componentTypeAValue;
    }
  }, [componentTypeAValue, conditions, form]);

  useEffect(() => {
    if (previousTypes.current.b !== componentTypeBValue) {
      conditions.forEach((_condition, index) => {
        form.setValue(`conditions.${index}.rightAttributeDefinitionId`, '', {
          shouldDirty: true,
        });
      });
      previousTypes.current.b = componentTypeBValue;
    }
  }, [componentTypeBValue, conditions, form]);

  const submit = form.handleSubmit(async (values) => {
    if (!validateAttributeSemantics(values)) {
      return;
    }
    try {
      const body = toSaveCompatibilityRuleRequest(values);
      if (rule) {
        await updateRule.mutateAsync({ domainId, ruleId: rule.id, body });
        showSuccessNotification(t('compatibilityRules.notifications.updated'));
      } else {
        await createRule.mutateAsync({ domainId, body });
        showSuccessNotification(t('compatibilityRules.notifications.created'));
      }
      allowNavigation();
      void navigate('/settings/compatibility/rules', { replace: true });
    } catch (error) {
      for (const [path, messages] of Object.entries(getCompatibilityRuleFieldErrors(error))) {
        const message = messages[0];
        if (message) {
          form.setError(path as FieldPath<CompatibilityRuleFormValues>, { message });
        }
      }
    }
  });

  const typeOptions = componentTypes.map((type) => ({ value: String(type.id), label: type.name }));
  const attributesPending =
    (componentTypeAId !== null && leftAttributesQuery.isPending) ||
    (componentTypeBId !== null && rightAttributesQuery.isPending);
  const attributesError =
    (componentTypeAId !== null ? leftAttributesQuery.error : null) ??
    (componentTypeBId !== null ? rightAttributesQuery.error : null);

  return (
    <>
      <form onSubmit={(event) => void submit(event)} noValidate>
        <Stack gap="lg">
          <Paper p={{ base: 'md', sm: 'lg' }} radius="md" withBorder>
            <Stack gap="md">
              <Group justify="space-between" align="flex-start">
                <Title order={2} size="h3">
                  {t('compatibilityRules.form.name')}
                </Title>
                <Controller
                  name="enabled"
                  control={form.control}
                  render={({ field }) => (
                    <Switch
                      label={t('compatibilityRules.form.enabled')}
                      description={t('compatibilityRules.form.enabledHint')}
                      checked={field.value}
                      onChange={(event) => field.onChange(event.currentTarget.checked)}
                      disabled={isPending}
                    />
                  )}
                />
              </Group>
              <TextInput
                label={t('compatibilityRules.form.name')}
                placeholder={t('compatibilityRules.form.namePlaceholder')}
                withAsterisk
                maxLength={255}
                autoFocus={!rule}
                disabled={isPending}
                error={form.formState.errors.name?.message}
                {...form.register('name')}
              />
              <div className={classes['type-fields']}>
                <Controller
                  name="componentTypeAId"
                  control={form.control}
                  render={({ field, fieldState }) => (
                    <Select
                      label={t('compatibilityRules.form.leftType')}
                      placeholder={t('compatibilityRules.form.leftTypePlaceholder')}
                      data={typeOptions.filter((option) => option.value !== componentTypeBValue)}
                      value={field.value || null}
                      onChange={(value) => field.onChange(value ?? '')}
                      onBlur={field.onBlur}
                      searchable
                      withAsterisk
                      nothingFoundMessage={t('compatibilityRules.form.typeNothingFound')}
                      error={fieldState.error?.message}
                      disabled={isPending}
                    />
                  )}
                />
                <Controller
                  name="componentTypeBId"
                  control={form.control}
                  render={({ field, fieldState }) => (
                    <Select
                      label={t('compatibilityRules.form.rightType')}
                      placeholder={t('compatibilityRules.form.rightTypePlaceholder')}
                      data={typeOptions.filter((option) => option.value !== componentTypeAValue)}
                      value={field.value || null}
                      onChange={(value) => field.onChange(value ?? '')}
                      onBlur={field.onBlur}
                      searchable
                      withAsterisk
                      nothingFoundMessage={t('compatibilityRules.form.typeNothingFound')}
                      error={fieldState.error?.message}
                      disabled={isPending}
                    />
                  )}
                />
              </div>
              <Alert icon={<IconInfoCircle size={18} />}>
                {t('compatibilityRules.form.normalizationHint')}
              </Alert>
            </Stack>
          </Paper>

          <Paper p={{ base: 'md', sm: 'lg' }} radius="md" withBorder>
            <Stack gap="md">
              <Group justify="space-between" align="flex-start">
                <Stack gap={3}>
                  <Title order={2} size="h3">
                    {t('compatibilityRules.form.conditionsTitle')}
                  </Title>
                  <Text size="sm" c="dimmed">
                    {t('compatibilityRules.form.conditionsDescription')}
                  </Text>
                </Stack>
                <Button
                  variant="light"
                  leftSection={<IconPlus size={17} />}
                  onClick={() => append(createEmptyCompatibilityCondition())}
                  disabled={isPending}
                >
                  {t('compatibilityRules.actions.addCondition')}
                </Button>
              </Group>
              {rule ? (
                <Alert color="orange" icon={<IconInfoCircle size={18} />}>
                  {t('compatibilityRules.form.replacementHint')}
                </Alert>
              ) : null}
              <Divider />
              {attributesPending && componentTypeAId !== null && componentTypeBId !== null ? (
                <LoadingState label={t('states.loading')} />
              ) : null}
              {attributesError ? (
                <ErrorState
                  error={attributesError}
                  onRetry={() =>
                    void Promise.all([
                      leftAttributesQuery.refetch(),
                      rightAttributesQuery.refetch(),
                    ])
                  }
                />
              ) : null}
              <Stack gap="sm">
                {fields.map((field, index) => {
                  const value = conditions[index] ?? createEmptyCompatibilityCondition();
                  const errors = form.formState.errors.conditions?.[index];
                  return (
                    <CompatibilityRuleConditionFields
                      key={field.id}
                      index={index}
                      value={value}
                      leftAttributes={leftAttributes}
                      rightAttributes={rightAttributes}
                      leftError={errors?.leftAttributeDefinitionId?.message}
                      operatorError={errors?.operator?.message}
                      rightError={errors?.rightAttributeDefinitionId?.message}
                      loading={attributesPending}
                      disabled={isPending}
                      canRemove={fields.length > 1}
                      canMoveUp={index > 0}
                      canMoveDown={index < fields.length - 1}
                      onChange={(name, nextValue) =>
                        form.setValue(`conditions.${index}.${name}`, nextValue, {
                          shouldDirty: true,
                          shouldValidate: form.formState.isSubmitted,
                        })
                      }
                      onRemove={() => remove(index)}
                      onMoveUp={() => move(index, index - 1)}
                      onMoveDown={() => move(index, index + 1)}
                    />
                  );
                })}
              </Stack>
            </Stack>
          </Paper>

          <Group justify="flex-end">
            <Button
              variant="default"
              disabled={isPending}
              onClick={() => void navigate('/settings/compatibility/rules')}
            >
              {t('common.cancel')}
            </Button>
            <Button
              type="submit"
              loading={isPending}
              disabled={attributesPending || Boolean(attributesError)}
            >
              {rule
                ? t('compatibilityRules.actions.save')
                : t('compatibilityRules.actions.createRule')}
            </Button>
          </Group>
        </Stack>
      </form>

      <Modal
        opened={blocker.state === 'blocked'}
        onClose={() => blocker.reset?.()}
        title={t('compatibilityRules.form.unsaved.title')}
        centered
      >
        <Stack gap="md">
          <Text>{t('compatibilityRules.form.unsaved.description')}</Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => blocker.reset?.()}>
              {t('compatibilityRules.form.unsaved.stay')}
            </Button>
            <Button color="red" onClick={() => blocker.proceed?.()}>
              {t('compatibilityRules.form.unsaved.leave')}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </>
  );
}
