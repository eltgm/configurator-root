import type {
  CompatibilityExplanation,
  ConfiguratorBatchSearchResponse,
  ConfiguratorIntersectionResponse,
  ConfiguratorResponse,
} from '@/shared/api';

export interface ConfiguratorComponentSelection {
  id: number;
  name: string;
  brand?: string;
  componentTypeId: number;
}

export interface ConfiguratorCandidate extends ConfiguratorComponentSelection {
  componentTypeName: string;
  relation: Exclude<CompatibilityRelation, 'incompatible'>;
  compatibilityByBase: ReadonlyArray<ConfiguratorBaseEvidence>;
  explanations: ReadonlyArray<CompatibilityExplanation>;
}

export type CompatibilityRelation = 'direct' | 'transitive' | 'incompatible';

export interface ConfiguratorBaseEvidence {
  baseComponentId: number;
  relation: Exclude<CompatibilityRelation, 'incompatible'>;
  explanations: ReadonlyArray<CompatibilityExplanation>;
}

export interface ConfiguratorConflictPair {
  leftComponentId: number;
  rightComponentId: number;
}

export interface ConfiguratorPairResult extends ConfiguratorConflictPair {
  relation: CompatibilityRelation;
  explanations: ReadonlyArray<CompatibilityExplanation>;
}

export interface ConfiguratorValidationResult {
  compatible: boolean;
  directlyCompatible: boolean;
  relation: CompatibilityRelation;
  pairs: ReadonlyArray<ConfiguratorPairResult>;
  conflictPairs: ReadonlyArray<ConfiguratorConflictPair>;
  conflictComponentIds: ReadonlySet<number>;
}

export function compatibilityRelationFromExplanations(
  explanations: ReadonlyArray<CompatibilityExplanation>,
): Exclude<CompatibilityRelation, 'incompatible'> {
  return explanations.length > 0 &&
    explanations.every((explanation) => explanation.source === 'TRANSITIVE')
    ? 'transitive'
    : 'direct';
}

function toBaseEvidence(
  baseComponentId: number,
  explanations: ReadonlyArray<CompatibilityExplanation>,
): ConfiguratorBaseEvidence {
  return {
    baseComponentId,
    relation: compatibilityRelationFromExplanations(explanations),
    explanations,
  };
}

function candidateRelation(
  evidence: ReadonlyArray<ConfiguratorBaseEvidence>,
): Exclude<CompatibilityRelation, 'incompatible'> {
  return evidence.some((entry) => entry.relation === 'transitive') ? 'transitive' : 'direct';
}

export function candidatesFromDirectResponse(response: ConfiguratorResponse) {
  return response.compatibleByType.flatMap((group) =>
    group.components.map<ConfiguratorCandidate>((component) => {
      const compatibilityByBase = [
        toBaseEvidence(response.baseComponentId, component.explanations),
      ];
      return {
        ...component,
        componentTypeName: group.componentTypeName,
        relation: candidateRelation(compatibilityByBase),
        compatibilityByBase,
        explanations: component.explanations,
      };
    }),
  );
}

export function candidatesFromIntersectionResponse(response: ConfiguratorIntersectionResponse) {
  return response.compatibleByType.flatMap((group) =>
    group.components.map<ConfiguratorCandidate>((component) => {
      const compatibilityByBase = component.compatibilityByBase.map((entry) =>
        toBaseEvidence(entry.baseComponentId, entry.explanations),
      );
      return {
        id: component.id,
        name: component.name,
        ...(component.brand === undefined ? {} : { brand: component.brand }),
        componentTypeId: component.componentTypeId,
        componentTypeName: group.componentTypeName,
        relation: candidateRelation(compatibilityByBase),
        compatibilityByBase,
        explanations: compatibilityByBase.flatMap((entry) => entry.explanations),
      };
    }),
  );
}

export function filterConfiguratorCandidates(
  candidates: ReadonlyArray<ConfiguratorCandidate>,
  options: {
    search: string;
    componentTypeId?: number;
    excludedComponentTypeIds?: ReadonlySet<number>;
    excludedComponentId?: number;
  },
) {
  const normalizedSearch = options.search.trim().toLocaleLowerCase();
  return candidates.filter(
    (candidate) =>
      candidate.id !== options.excludedComponentId &&
      (options.componentTypeId === undefined ||
        candidate.componentTypeId === options.componentTypeId) &&
      !options.excludedComponentTypeIds?.has(candidate.componentTypeId) &&
      (!normalizedSearch ||
        candidate.name.toLocaleLowerCase().includes(normalizedSearch) ||
        candidate.brand?.toLocaleLowerCase().includes(normalizedSearch)),
  );
}

function compatibleComponentsByBase(response: ConfiguratorBatchSearchResponse) {
  return new Map(
    response.results.map((result) => [
      result.baseComponentId,
      new Map(
        result.compatibleByType.flatMap((group) =>
          group.components.map((component) => [component.id, component] as const),
        ),
      ),
    ]),
  );
}

export function validateConfiguratorAssembly(
  componentIds: ReadonlyArray<number>,
  response: ConfiguratorBatchSearchResponse,
): ConfiguratorValidationResult {
  const compatibleByBase = compatibleComponentsByBase(response);
  const conflictPairs: ConfiguratorConflictPair[] = [];
  const conflictComponentIds = new Set<number>();
  const pairs: ConfiguratorPairResult[] = [];

  for (let leftIndex = 0; leftIndex < componentIds.length; leftIndex += 1) {
    const leftComponentId = componentIds[leftIndex]!;
    for (let rightIndex = leftIndex + 1; rightIndex < componentIds.length; rightIndex += 1) {
      const rightComponentId = componentIds[rightIndex]!;
      const leftToRight = compatibleByBase.get(leftComponentId)?.get(rightComponentId);
      const rightToLeft = compatibleByBase.get(rightComponentId)?.get(leftComponentId);
      const relation: CompatibilityRelation =
        leftToRight === undefined || rightToLeft === undefined
          ? 'incompatible'
          : compatibilityRelationFromExplanations(leftToRight.explanations) === 'direct' &&
              compatibilityRelationFromExplanations(rightToLeft.explanations) === 'direct'
            ? 'direct'
            : 'transitive';
      pairs.push({
        leftComponentId,
        rightComponentId,
        relation,
        explanations:
          leftToRight && leftToRight.explanations.length > 0
            ? leftToRight.explanations
            : (rightToLeft?.explanations ?? []),
      });
      if (relation === 'incompatible') {
        conflictPairs.push({ leftComponentId, rightComponentId });
        conflictComponentIds.add(leftComponentId);
        conflictComponentIds.add(rightComponentId);
      }
    }
  }

  const relation: CompatibilityRelation =
    conflictPairs.length > 0
      ? 'incompatible'
      : pairs.some((pair) => pair.relation === 'transitive')
        ? 'transitive'
        : 'direct';

  return {
    compatible: relation !== 'incompatible',
    directlyCompatible: relation === 'direct',
    relation,
    pairs,
    conflictPairs,
    conflictComponentIds,
  };
}

export function replacementBaseComponentIds(
  componentIds: ReadonlyArray<number>,
  replacedComponentId: number | null,
) {
  return replacedComponentId === null
    ? [...componentIds]
    : componentIds.filter((componentId) => componentId !== replacedComponentId);
}
