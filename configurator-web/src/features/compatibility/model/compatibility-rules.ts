import type {
  AttributeDefinition,
  CompatibilityRuleOperator,
  CompatibilityRuleSet,
  ComponentType,
  SaveCompatibilityRuleSetRequest,
} from '@/shared/api';
import { getFieldErrors } from '@/shared/api/errors';

export type CompatibilityRuleStatusFilter = 'all' | 'enabled' | 'disabled';

export interface CompatibilityRuleConditionFormValue {
  leftAttributeDefinitionId: string;
  operator: string;
  rightAttributeDefinitionId: string;
}

export interface CompatibilityRuleFormValues {
  name: string;
  componentTypeAId: string;
  componentTypeBId: string;
  enabled: boolean;
  conditions: CompatibilityRuleConditionFormValue[];
}

export const equalityCompatibilityOperators: CompatibilityRuleOperator[] = ['EQUALS', 'NOT_EQUALS'];

export const numericCompatibilityOperators: CompatibilityRuleOperator[] = [
  ...equalityCompatibilityOperators,
  'GT',
  'GTE',
  'LT',
  'LTE',
];

export function createEmptyCompatibilityCondition(): CompatibilityRuleConditionFormValue {
  return {
    leftAttributeDefinitionId: '',
    operator: '',
    rightAttributeDefinitionId: '',
  };
}

export function toCompatibilityRuleFormValues(
  rule?: CompatibilityRuleSet,
): CompatibilityRuleFormValues {
  if (!rule) {
    return {
      name: '',
      componentTypeAId: '',
      componentTypeBId: '',
      enabled: true,
      conditions: [createEmptyCompatibilityCondition()],
    };
  }

  return {
    name: rule.name,
    componentTypeAId: String(rule.componentTypeAId),
    componentTypeBId: String(rule.componentTypeBId),
    enabled: rule.enabled,
    conditions: [...rule.conditions]
      .sort((left, right) => left.orderIndex - right.orderIndex || left.id - right.id)
      .map((condition) => ({
        leftAttributeDefinitionId: String(condition.leftAttributeDefinitionId),
        operator: condition.operator,
        rightAttributeDefinitionId: String(condition.rightAttributeDefinitionId),
      })),
  };
}

export function toSaveCompatibilityRuleRequest(
  values: CompatibilityRuleFormValues,
): SaveCompatibilityRuleSetRequest {
  return {
    name: values.name.trim(),
    componentTypeAId: Number(values.componentTypeAId),
    componentTypeBId: Number(values.componentTypeBId),
    enabled: values.enabled,
    conditions: values.conditions.map((condition, orderIndex) => ({
      leftAttributeDefinitionId: Number(condition.leftAttributeDefinitionId),
      operator: condition.operator as CompatibilityRuleOperator,
      rightAttributeDefinitionId: Number(condition.rightAttributeDefinitionId),
      orderIndex,
    })),
  };
}

export function toSaveCompatibilityRuleRequestFromRule(
  rule: CompatibilityRuleSet,
  enabled = rule.enabled,
): SaveCompatibilityRuleSetRequest {
  return toSaveCompatibilityRuleRequest({
    ...toCompatibilityRuleFormValues(rule),
    enabled,
  });
}

export function getCompatibilityOperators(
  dataType?: AttributeDefinition['dataType'],
): CompatibilityRuleOperator[] {
  if (!dataType) {
    return [];
  }
  return dataType === 'NUMBER'
    ? [...numericCompatibilityOperators]
    : [...equalityCompatibilityOperators];
}

export function getCompatibleRightAttributes(
  leftAttributeId: string,
  leftAttributes: ReadonlyArray<AttributeDefinition>,
  rightAttributes: ReadonlyArray<AttributeDefinition>,
) {
  const leftAttribute = leftAttributes.find(
    (attribute) => attribute.id === Number(leftAttributeId),
  );
  if (!leftAttribute) {
    return [];
  }
  return rightAttributes.filter((attribute) => attribute.dataType === leftAttribute.dataType);
}

export function hasDuplicateCompatibilityCondition(
  conditions: ReadonlyArray<CompatibilityRuleConditionFormValue>,
  conditionIndex: number,
) {
  const condition = conditions[conditionIndex];
  if (
    !condition ||
    !condition.leftAttributeDefinitionId ||
    !condition.operator ||
    !condition.rightAttributeDefinitionId
  ) {
    return false;
  }
  const key = `${condition.leftAttributeDefinitionId}:${condition.operator}:${condition.rightAttributeDefinitionId}`;
  return conditions.some(
    (candidate, candidateIndex) =>
      candidateIndex !== conditionIndex &&
      `${candidate.leftAttributeDefinitionId}:${candidate.operator}:${candidate.rightAttributeDefinitionId}` ===
        key,
  );
}

export function filterCompatibilityRules(
  rules: ReadonlyArray<CompatibilityRuleSet>,
  componentTypes: ReadonlyArray<ComponentType>,
  search: string,
  status: CompatibilityRuleStatusFilter,
) {
  const typeById = new Map(componentTypes.map((type) => [type.id, type]));
  const normalizedSearch = search.trim().toLocaleLowerCase();
  return rules.filter((rule) => {
    if (status === 'enabled' && !rule.enabled) {
      return false;
    }
    if (status === 'disabled' && rule.enabled) {
      return false;
    }
    if (!normalizedSearch) {
      return true;
    }
    const haystack = [
      rule.name,
      typeById.get(rule.componentTypeAId)?.name ?? '',
      typeById.get(rule.componentTypeBId)?.name ?? '',
    ]
      .join(' ')
      .toLocaleLowerCase();
    return haystack.includes(normalizedSearch);
  });
}

export function getCompatibilityTypeLabel(
  componentTypes: ReadonlyArray<ComponentType>,
  typeId: number,
  fallback: (id: number) => string,
) {
  return componentTypes.find((type) => type.id === typeId)?.name ?? fallback(typeId);
}

export function normalizeCompatibilityRuleFieldPath(field: string) {
  return field.replace(/conditions\[(\d+)]/g, 'conditions.$1');
}

export function getCompatibilityRuleFieldErrors(error: unknown) {
  const normalized: Record<string, ReadonlyArray<string>> = {};
  for (const [field, messages] of Object.entries(getFieldErrors(error))) {
    normalized[normalizeCompatibilityRuleFieldPath(field)] = messages;
  }
  return normalized;
}
