import { describe, expect, it } from 'vitest';

import {
  blockedCandidatesFromAssemblyResponse,
  candidatesFromAssemblyResponse,
  candidatesFromDirectResponse,
  candidatesFromIntersectionResponse,
  filterConfiguratorCandidates,
  replacementBaseComponentIds,
  validationFromAssemblyResponse,
} from '@/features/configurator/model/configurator-compatibility';
import type {
  ConfiguratorCandidatesResponse,
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
  it('uses assembly decisions for available blocked and connected validation states', () => {
    const response: ConfiguratorCandidatesResponse = {
      componentIds: [1, 2, 3],
      assemblyStatus: 'VALID',
      assemblyDecisions: [
        {
          leftComponentId: 1,
          rightComponentId: 2,
          status: 'ALLOWED',
          explanations: [{ source: 'MANUAL', linkId: 12 }],
          blockingRules: [],
        },
        {
          leftComponentId: 1,
          rightComponentId: 3,
          status: 'UNKNOWN',
          explanations: [],
          blockingRules: [],
        },
        {
          leftComponentId: 2,
          rightComponentId: 3,
          status: 'ALLOWED',
          explanations: [{ source: 'AUTOMATIC', ruleSetId: 23 }],
          blockingRules: [],
        },
      ],
      candidatesByType: [
        {
          componentTypeId: 40,
          componentTypeName: 'GPU',
          components: [
            {
              id: 4,
              name: 'Available GPU',
              componentTypeId: 40,
              status: 'AVAILABLE',
              compatibilityByBase: [
                {
                  baseComponentId: 1,
                  status: 'ALLOWED',
                  explanations: [{ source: 'MANUAL', linkId: 14 }],
                  blockingRules: [],
                },
                {
                  baseComponentId: 2,
                  status: 'UNKNOWN',
                  explanations: [],
                  blockingRules: [],
                },
              ],
            },
            {
              id: 5,
              name: 'Blocked GPU',
              componentTypeId: 40,
              status: 'BLOCKED',
              compatibilityByBase: [
                {
                  baseComponentId: 2,
                  status: 'DENIED',
                  explanations: [],
                  blockingRules: [{ ruleSetId: 7, ruleSetName: 'Power limit' }],
                },
              ],
            },
          ],
        },
      ],
    };

    expect(candidatesFromAssemblyResponse(response)).toEqual([
      expect.objectContaining({
        id: 4,
        relation: 'direct',
        compatibilityByBase: [expect.objectContaining({ baseComponentId: 1 })],
      }),
    ]);
    expect(blockedCandidatesFromAssemblyResponse(response)).toEqual([
      expect.objectContaining({
        id: 5,
        blockingByBase: [
          { baseComponentId: 2, blockingRules: [{ ruleSetId: 7, ruleSetName: 'Power limit' }] },
        ],
      }),
    ]);
    expect(validationFromAssemblyResponse(response)).toMatchObject({
      assemblyStatus: 'VALID',
      conflictPairs: [],
      pairs: [
        expect.objectContaining({ relation: 'direct' }),
        expect.objectContaining({ relation: 'unknown' }),
        expect.objectContaining({ relation: 'direct' }),
      ],
    });

    const blockedValidation = validationFromAssemblyResponse({
      ...response,
      assemblyStatus: 'BLOCKED',
      assemblyDecisions: [
        {
          leftComponentId: 1,
          rightComponentId: 3,
          status: 'DENIED',
          explanations: [],
          blockingRules: [{ ruleSetId: 9, ruleSetName: 'Socket' }],
        },
      ],
    });
    expect(blockedValidation.assemblyStatus).toBe('BLOCKED');
    expect(blockedValidation.conflictPairs).toEqual([{ leftComponentId: 1, rightComponentId: 3 }]);
    expect(blockedValidation.pairs[0]?.blockingRules).toEqual([
      { ruleSetId: 9, ruleSetName: 'Socket' },
    ]);
  });

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

  it('classifies mixed direct and transitive evidence for candidates', () => {
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
  });

  it('marks only components outside the root ALLOWED graph as disconnected', () => {
    const validation = validationFromAssemblyResponse({
      componentIds: [1, 2, 3, 4],
      assemblyStatus: 'DISCONNECTED',
      assemblyDecisions: [
        {
          leftComponentId: 1,
          rightComponentId: 2,
          status: 'ALLOWED',
          explanations: [],
          blockingRules: [],
        },
        {
          leftComponentId: 1,
          rightComponentId: 3,
          status: 'UNKNOWN',
          explanations: [],
          blockingRules: [],
        },
        {
          leftComponentId: 1,
          rightComponentId: 4,
          status: 'UNKNOWN',
          explanations: [],
          blockingRules: [],
        },
        {
          leftComponentId: 2,
          rightComponentId: 3,
          status: 'UNKNOWN',
          explanations: [],
          blockingRules: [],
        },
        {
          leftComponentId: 2,
          rightComponentId: 4,
          status: 'UNKNOWN',
          explanations: [],
          blockingRules: [],
        },
        {
          leftComponentId: 3,
          rightComponentId: 4,
          status: 'ALLOWED',
          explanations: [],
          blockingRules: [],
        },
      ],
      candidatesByType: [],
    });

    expect(validation.assemblyStatus).toBe('DISCONNECTED');
    expect(validation.conflictPairs).toEqual([]);
    expect([...validation.conflictComponentIds]).toEqual([3, 4]);
  });

  it('excludes only the replaced component while preserving draft order', () => {
    expect(replacementBaseComponentIds([8, 3, 5], 3)).toEqual([8, 5]);
    expect(replacementBaseComponentIds([8, 3, 5], null)).toEqual([8, 3, 5]);
  });
});
