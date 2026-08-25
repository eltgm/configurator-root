import { describe, expect, it } from 'vitest';

import {
  createEmptyCompatibilityCondition,
  filterCompatibilityRules,
  getCompatibilityOperators,
  getCompatibilityRuleFieldErrors,
  getCompatibilityTypeLabel,
  getCompatibleRightAttributes,
  hasDuplicateCompatibilityCondition,
  normalizeCompatibilityRuleFieldPath,
  toCompatibilityRuleFormValues,
  toSaveCompatibilityRuleRequest,
  toSaveCompatibilityRuleRequestFromRule,
} from '@/features/compatibility/model/compatibility-rules';
import type {
  AttributeDefinition,
  CompatibilityRuleSet,
  ComponentType,
  ErrorResponse,
} from '@/shared/api';

const types: ComponentType[] = [
  { id: 11, domainId: 7, name: 'Processor' },
  { id: 12, domainId: 7, name: 'Motherboard' },
];
const attributes: AttributeDefinition[] = [
  {
    id: 101,
    domainId: 7,
    componentTypeId: 11,
    name: 'socket',
    label: 'Socket',
    dataType: 'STRING',
    isRequired: true,
  },
  {
    id: 102,
    domainId: 7,
    componentTypeId: 11,
    name: 'cores',
    label: 'Cores',
    dataType: 'NUMBER',
    isRequired: true,
  },
];
const rule: CompatibilityRuleSet = {
  id: 91,
  domainId: 7,
  name: ' Socket match ',
  componentTypeAId: 11,
  componentTypeBId: 12,
  enabled: true,
  conditions: [
    {
      id: 502,
      ruleSetId: 91,
      leftAttributeDefinitionId: 102,
      operator: 'GTE',
      rightAttributeDefinitionId: 202,
      orderIndex: 1,
      createdAt: '2026-08-23T12:00:00',
    },
    {
      id: 501,
      ruleSetId: 91,
      leftAttributeDefinitionId: 101,
      operator: 'EQUALS',
      rightAttributeDefinitionId: 201,
      orderIndex: 0,
      createdAt: '2026-08-23T12:00:00',
    },
  ],
  createdAt: '2026-08-23T12:00:00',
};

describe('compatibility rules model', () => {
  it('converts a response to ordered form values and a trimmed sequential request', () => {
    const values = toCompatibilityRuleFormValues(rule);

    expect(values.conditions.map((condition) => condition.leftAttributeDefinitionId)).toEqual([
      '101',
      '102',
    ]);
    expect(toSaveCompatibilityRuleRequest(values)).toEqual({
      name: 'Socket match',
      componentTypeAId: 11,
      componentTypeBId: 12,
      enabled: true,
      conditions: [
        {
          leftAttributeDefinitionId: 101,
          operator: 'EQUALS',
          rightAttributeDefinitionId: 201,
          orderIndex: 0,
        },
        {
          leftAttributeDefinitionId: 102,
          operator: 'GTE',
          rightAttributeDefinitionId: 202,
          orderIndex: 1,
        },
      ],
    });
    expect(toSaveCompatibilityRuleRequestFromRule(rule, false).enabled).toBe(false);
  });

  it('creates a single empty condition for a new rule', () => {
    expect(toCompatibilityRuleFormValues()).toEqual({
      name: '',
      componentTypeAId: '',
      componentTypeBId: '',
      enabled: true,
      conditions: [createEmptyCompatibilityCondition()],
    });
  });

  it('restricts operators and right attributes by data type', () => {
    expect(getCompatibilityOperators('STRING')).toEqual(['EQUALS', 'NOT_EQUALS']);
    expect(getCompatibilityOperators('NUMBER')).toEqual([
      'EQUALS',
      'NOT_EQUALS',
      'GT',
      'GTE',
      'LT',
      'LTE',
    ]);
    expect(getCompatibilityOperators()).toEqual([]);
    expect(
      getCompatibleRightAttributes('101', attributes, [
        { ...attributes[0]!, id: 201, componentTypeId: 12 },
        { ...attributes[1]!, id: 202, componentTypeId: 12 },
      ]).map((attribute) => attribute.id),
    ).toEqual([201]);
  });

  it('detects complete duplicate conditions but ignores incomplete rows', () => {
    const condition = {
      leftAttributeDefinitionId: '101',
      operator: 'EQUALS' as const,
      rightAttributeDefinitionId: '201',
    };
    expect(hasDuplicateCompatibilityCondition([condition, { ...condition }], 0)).toBe(true);
    expect(
      hasDuplicateCompatibilityCondition([condition, createEmptyCompatibilityCondition()], 1),
    ).toBe(false);
  });

  it('filters by status, name and type while preserving stale fallbacks', () => {
    const disabled = { ...rule, id: 92, name: 'Disabled', enabled: false };
    expect(filterCompatibilityRules([rule, disabled], types, 'mother', 'all')).toEqual([
      rule,
      disabled,
    ]);
    expect(filterCompatibilityRules([rule, disabled], types, '', 'disabled')).toEqual([disabled]);
    expect(getCompatibilityTypeLabel(types, 99, (id) => `Type #${id}`)).toBe('Type #99');
  });

  it('normalizes indexed backend field paths without dropping messages', () => {
    const error: ErrorResponse = {
      timestamp: '2026-08-23T12:00:00Z',
      status: 400,
      error: 'Bad Request',
      code: 'VALIDATION_ERROR',
      message: 'Invalid rule',
      path: '/domains/7/compatibility/rules',
      details: [
        {
          field: 'conditions[0].leftAttributeDefinitionId',
          code: 'INVALID_ATTRIBUTE',
          message: 'Wrong attribute',
        },
      ],
    };

    expect(normalizeCompatibilityRuleFieldPath('conditions[12].operator')).toBe(
      'conditions.12.operator',
    );
    expect(getCompatibilityRuleFieldErrors(error)).toEqual({
      'conditions.0.leftAttributeDefinitionId': ['Wrong attribute'],
    });
  });
});
