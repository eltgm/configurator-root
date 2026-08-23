import {
  ActionIcon,
  Badge,
  Group,
  Paper,
  Select,
  Stack,
  Text,
  Title,
  Tooltip,
} from '@mantine/core';
import { IconArrowDown, IconArrowUp, IconTrash } from '@tabler/icons-react';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';

import {
  getCompatibilityOperators,
  getCompatibleRightAttributes,
  type CompatibilityRuleConditionFormValue,
} from '@/features/compatibility/model/compatibility-rules';
import type { AttributeDefinition } from '@/shared/api';

import classes from './compatibility-rule-form.module.css';

interface CompatibilityRuleConditionFieldsProps {
  index: number;
  value: CompatibilityRuleConditionFormValue;
  leftAttributes: AttributeDefinition[];
  rightAttributes: AttributeDefinition[];
  leftError?: string | undefined;
  operatorError?: string | undefined;
  rightError?: string | undefined;
  loading: boolean;
  disabled: boolean;
  canRemove: boolean;
  canMoveUp: boolean;
  canMoveDown: boolean;
  onChange: (field: keyof CompatibilityRuleConditionFormValue, value: string) => void;
  onRemove: () => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
}

function attributeOption(attribute: AttributeDefinition) {
  return {
    value: String(attribute.id),
    label: `${attribute.label} · ${attribute.dataType}`,
  };
}

function withStaleOption(
  options: Array<{ value: string; label: string }>,
  value: string,
  staleLabel: (id: string) => string,
) {
  return value && !options.some((option) => option.value === value)
    ? [{ value, label: staleLabel(value) }, ...options]
    : options;
}

export function CompatibilityRuleConditionFields({
  index,
  value,
  leftAttributes,
  rightAttributes,
  leftError,
  operatorError,
  rightError,
  loading,
  disabled,
  canRemove,
  canMoveUp,
  canMoveDown,
  onChange,
  onRemove,
  onMoveUp,
  onMoveDown,
}: CompatibilityRuleConditionFieldsProps) {
  const { t } = useTranslation();
  const selectedLeft = leftAttributes.find(
    (attribute) => attribute.id === Number(value.leftAttributeDefinitionId),
  );
  const operatorOptions = getCompatibilityOperators(selectedLeft?.dataType).map((operator) => ({
    value: operator,
    label: t(`compatibilityRules.form.operators.${operator}`),
  }));
  const leftOptions = useMemo(
    () =>
      withStaleOption(leftAttributes.map(attributeOption), value.leftAttributeDefinitionId, (id) =>
        t('compatibilityRules.form.unknownAttribute', { id }),
      ),
    [leftAttributes, t, value.leftAttributeDefinitionId],
  );
  const rightOptions = useMemo(
    () =>
      withStaleOption(
        getCompatibleRightAttributes(
          value.leftAttributeDefinitionId,
          leftAttributes,
          rightAttributes,
        ).map(attributeOption),
        value.rightAttributeDefinitionId,
        (id) => t('compatibilityRules.form.unknownAttribute', { id }),
      ),
    [
      leftAttributes,
      rightAttributes,
      t,
      value.leftAttributeDefinitionId,
      value.rightAttributeDefinitionId,
    ],
  );
  const number = index + 1;

  return (
    <Paper className={classes.condition} p={{ base: 'md', sm: 'lg' }} radius="md" withBorder>
      <Stack gap="md">
        <Group justify="space-between" align="flex-start" wrap="nowrap">
          <Group gap="xs">
            <Title order={3} size="h4">
              {t('compatibilityRules.form.conditionTitle', { number })}
            </Title>
            {selectedLeft ? <Badge variant="light">{selectedLeft.dataType}</Badge> : null}
          </Group>
          <Group gap={4} wrap="nowrap">
            <Tooltip label={t('compatibilityRules.actions.moveConditionUp', { number })}>
              <ActionIcon
                variant="subtle"
                disabled={!canMoveUp || disabled}
                aria-label={t('compatibilityRules.actions.moveConditionUp', { number })}
                onClick={onMoveUp}
              >
                <IconArrowUp size={18} />
              </ActionIcon>
            </Tooltip>
            <Tooltip label={t('compatibilityRules.actions.moveConditionDown', { number })}>
              <ActionIcon
                variant="subtle"
                disabled={!canMoveDown || disabled}
                aria-label={t('compatibilityRules.actions.moveConditionDown', { number })}
                onClick={onMoveDown}
              >
                <IconArrowDown size={18} />
              </ActionIcon>
            </Tooltip>
            <Tooltip label={t('compatibilityRules.actions.removeCondition', { number })}>
              <ActionIcon
                variant="subtle"
                color="red"
                disabled={!canRemove || disabled}
                aria-label={t('compatibilityRules.actions.removeCondition', { number })}
                onClick={onRemove}
              >
                <IconTrash size={18} />
              </ActionIcon>
            </Tooltip>
          </Group>
        </Group>

        <div className={classes['condition-fields']}>
          <Select
            label={t('compatibilityRules.form.leftAttribute')}
            placeholder={t('compatibilityRules.form.leftAttributePlaceholder')}
            data={leftOptions}
            value={value.leftAttributeDefinitionId || null}
            onChange={(selectedValue) => {
              const nextValue = selectedValue ?? '';
              const nextAttribute = leftAttributes.find(
                (attribute) => attribute.id === Number(nextValue),
              );
              onChange('leftAttributeDefinitionId', nextValue);
              if (
                !getCompatibilityOperators(nextAttribute?.dataType).some(
                  (operator) => operator === value.operator,
                )
              ) {
                onChange('operator', '');
              }
              onChange('rightAttributeDefinitionId', '');
            }}
            searchable
            clearable
            withAsterisk
            nothingFoundMessage={t('compatibilityRules.form.noAttributes')}
            error={leftError}
            disabled={disabled || loading}
          />
          <Select
            label={t('compatibilityRules.form.operator')}
            placeholder={t('compatibilityRules.form.operatorPlaceholder')}
            data={operatorOptions}
            value={value.operator || null}
            onChange={(selectedValue) => onChange('operator', selectedValue ?? '')}
            withAsterisk
            error={operatorError}
            disabled={disabled || !selectedLeft}
          />
          <Select
            label={t('compatibilityRules.form.rightAttribute')}
            placeholder={
              selectedLeft
                ? t('compatibilityRules.form.rightAttributePlaceholder')
                : t('compatibilityRules.form.selectLeftAttributeFirst')
            }
            data={rightOptions}
            value={value.rightAttributeDefinitionId || null}
            onChange={(selectedValue) =>
              onChange('rightAttributeDefinitionId', selectedValue ?? '')
            }
            searchable
            clearable
            withAsterisk
            nothingFoundMessage={t('compatibilityRules.form.noCompatibleAttributes')}
            error={rightError}
            disabled={disabled || loading || !selectedLeft}
          />
        </div>
        {selectedLeft && rightOptions.length === 0 ? (
          <Text size="sm" c="orange">
            {t('compatibilityRules.form.noCompatibleAttributes')}
          </Text>
        ) : null}
      </Stack>
    </Paper>
  );
}
