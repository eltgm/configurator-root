import { describe, expect, it } from 'vitest';

import {
  candidatesFromDirectResponse,
  candidatesFromIntersectionResponse,
  filterConfiguratorCandidates,
  replacementBaseComponentIds,
  validateConfiguratorAssembly,
} from '@/features/configurator/model/configurator-compatibility';
import type {
  ConfiguratorBatchSearchResponse,
  ConfiguratorIntersectionResponse,
  ConfiguratorResponse,
} from '@/shared/api';

const direct: ConfiguratorResponse = {
  baseComponentId: 1,
  compatibleByType: [
    {
      componentTypeId: 20,
      componentTypeName: 'Motherboard',
      components: [
        {
          id: 2,
          name: 'B650 Tomahawk',
          brand: 'MSI',
          componentTypeId: 20,
          explanations: [{ source: 'AUTOMATIC', ruleSetId: 7 }],
        },
      ],
    },
  ],
};

describe('configurator compatibility model', () => {
  it('normalizes direct and intersection candidates without losing explanations', () => {
    const intersection: ConfiguratorIntersectionResponse = {
      componentIds: [1, 3],
      compatibleByType: [
        {
          componentTypeId: 20,
          componentTypeName: 'Motherboard',
          components: [
            {
              id: 2,
              name: 'B650 Tomahawk',
              brand: 'MSI',
              componentTypeId: 20,
              compatibilityByBase: [
                { baseComponentId: 1, explanations: [{ source: 'AUTOMATIC', ruleSetId: 7 }] },
                { baseComponentId: 3, explanations: [{ source: 'MANUAL', linkId: 8 }] },
              ],
            },
          ],
        },
      ],
    };

    expect(candidatesFromDirectResponse(direct)[0]).toMatchObject({
      id: 2,
      componentTypeName: 'Motherboard',
      relation: 'direct',
      compatibilityByBase: [{ baseComponentId: 1, relation: 'direct' }],
      explanations: [{ source: 'AUTOMATIC', ruleSetId: 7 }],
    });
    expect(candidatesFromIntersectionResponse(intersection)[0]).toMatchObject({
      relation: 'direct',
      compatibilityByBase: [
        { baseComponentId: 1, relation: 'direct' },
        { baseComponentId: 3, relation: 'direct' },
      ],
      explanations: [
        { source: 'AUTOMATIC', ruleSetId: 7 },
        { source: 'MANUAL', linkId: 8 },
      ],
    });
  });

  it('filters candidates by name, brand, type and selected slots', () => {
    const candidates = [
      ...candidatesFromDirectResponse(direct),
      {
        id: 3,
        name: 'GeForce RTX',
        brand: 'NVIDIA',
        componentTypeId: 30,
        componentTypeName: 'GPU',
        relation: 'direct' as const,
        compatibilityByBase: [],
        explanations: [],
      },
    ];

    expect(filterConfiguratorCandidates(candidates, { search: 'msi' }).map(({ id }) => id)).toEqual(
      [2],
    );
    expect(
      filterConfiguratorCandidates(candidates, { search: '', componentTypeId: 30 }).map(
        ({ id }) => id,
      ),
    ).toEqual([3]);
    expect(
      filterConfiguratorCandidates(candidates, {
        search: '',
        excludedComponentTypeIds: new Set([20]),
      }).map(({ id }) => id),
    ).toEqual([3]);
    expect(
      filterConfiguratorCandidates(candidates, { search: '', excludedComponentId: 2 }).map(
        ({ id }) => id,
      ),
    ).toEqual([3]);
  });

  it('finds each incompatible pair once and marks all involved components', () => {
    const response: ConfiguratorBatchSearchResponse = {
      results: [
        {
          baseComponentId: 1,
          compatibleByType: [
            {
              ...direct.compatibleByType[0]!,
              components: [{ ...direct.compatibleByType[0]!.components[0]!, id: 2 }],
            },
          ],
        },
        {
          baseComponentId: 2,
          compatibleByType: [
            {
              componentTypeId: 10,
              componentTypeName: 'CPU',
              components: [{ id: 1, name: 'CPU', componentTypeId: 10, explanations: [] }],
            },
          ],
        },
        { baseComponentId: 3, compatibleByType: [] },
      ],
    };

    const result = validateConfiguratorAssembly([1, 2, 3], response);
    expect(result.compatible).toBe(false);
    expect(result.relation).toBe('incompatible');
    expect(result.pairs[0]).toMatchObject({ relation: 'direct' });
    expect(result.conflictPairs).toEqual([
      { leftComponentId: 1, rightComponentId: 3 },
      { leftComponentId: 2, rightComponentId: 3 },
    ]);
    expect([...result.conflictComponentIds]).toEqual([1, 3, 2]);
  });

  it('classifies mixed direct and transitive evidence for candidates and assemblies', () => {
    const intersection: ConfiguratorIntersectionResponse = {
      componentIds: [1, 3],
      compatibleByType: [
        {
          componentTypeId: 20,
          componentTypeName: 'Motherboard',
          components: [
            {
              id: 2,
              name: 'B650 Tomahawk',
              componentTypeId: 20,
              compatibilityByBase: [
                { baseComponentId: 1, explanations: [{ source: 'MANUAL', linkId: 7 }] },
                {
                  baseComponentId: 3,
                  explanations: [{ source: 'TRANSITIVE', pathComponentIds: [3, 4, 2] }],
                },
              ],
            },
          ],
        },
      ],
    };
    expect(candidatesFromIntersectionResponse(intersection)[0]).toMatchObject({
      relation: 'transitive',
      compatibilityByBase: [
        { baseComponentId: 1, relation: 'direct' },
        { baseComponentId: 3, relation: 'transitive' },
      ],
    });

    const response: ConfiguratorBatchSearchResponse = {
      results: [
        {
          baseComponentId: 1,
          compatibleByType: [
            {
              componentTypeId: 20,
              componentTypeName: 'Motherboard',
              components: [
                {
                  id: 2,
                  name: 'B650',
                  componentTypeId: 20,
                  explanations: [{ source: 'TRANSITIVE', pathComponentIds: [1, 3, 2] }],
                },
              ],
            },
          ],
        },
        {
          baseComponentId: 2,
          compatibleByType: [
            {
              componentTypeId: 10,
              componentTypeName: 'CPU',
              components: [
                {
                  id: 1,
                  name: 'CPU',
                  componentTypeId: 10,
                  explanations: [{ source: 'TRANSITIVE', pathComponentIds: [2, 3, 1] }],
                },
              ],
            },
          ],
        },
      ],
    };
    const validation = validateConfiguratorAssembly([1, 2], response);
    expect(validation).toMatchObject({
      compatible: true,
      directlyCompatible: false,
      relation: 'transitive',
      pairs: [{ leftComponentId: 1, rightComponentId: 2, relation: 'transitive' }],
    });
  });

  it('treats zero or one component as compatible and missing reciprocal evidence as a conflict', () => {
    expect(validateConfiguratorAssembly([], { results: [] }).compatible).toBe(true);
    expect(validateConfiguratorAssembly([1], { results: [] }).compatible).toBe(true);
    expect(validateConfiguratorAssembly([1, 2], { results: [direct] }).conflictPairs).toEqual([
      { leftComponentId: 1, rightComponentId: 2 },
    ]);
  });

  it('excludes only the replaced component while preserving draft order', () => {
    expect(replacementBaseComponentIds([8, 3, 5], 3)).toEqual([8, 5]);
    expect(replacementBaseComponentIds([8, 3, 5], null)).toEqual([8, 3, 5]);
  });
});
