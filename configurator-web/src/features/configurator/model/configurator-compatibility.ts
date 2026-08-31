import type {
  Component,
  CompatibilityExplanation,
  CompatibilityBlockingRule,
  ConfiguratorAssemblyStatus,
  ConfiguratorCandidatesResponse,
  ConfiguratorIntersectionResponse,
  ConfiguratorResponse,
} from '@/shared/api';

export interface ConfiguratorComponentSelection {
  id: number;
  name: string;
  brand?: string | null;
  componentTypeId: number;
  primaryImage?: Component['primaryImage'];
}

export interface ConfiguratorCandidate extends ConfiguratorComponentSelection {
  componentTypeName: string;
  relation: Exclude<CompatibilityRelation, 'incompatible'>;
  compatibilityByBase: ReadonlyArray<ConfiguratorBaseEvidence>;
  explanations: ReadonlyArray<CompatibilityExplanation>;
}

export interface ConfiguratorBlockedCandidate extends ConfiguratorComponentSelection {
  componentTypeName: string;
  blockingByBase: ReadonlyArray<{
    baseComponentId: number;
    blockingRules: ReadonlyArray<CompatibilityBlockingRule>;
  }>;
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
  relation: CompatibilityRelation | 'unknown';
  explanations: ReadonlyArray<CompatibilityExplanation>;
  blockingRules?: ReadonlyArray<CompatibilityBlockingRule>;
}

export interface ConfiguratorValidationResult {
  assemblyStatus: ConfiguratorAssemblyStatus;
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
        primaryImage: component.primaryImage,
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

export function candidatesFromAssemblyResponse(response: ConfiguratorCandidatesResponse) {
  return response.candidatesByType.flatMap((group) =>
    group.components
      .filter((component) => component.status === 'AVAILABLE')
      .map<ConfiguratorCandidate>((component) => {
        const compatibilityByBase = component.compatibilityByBase
          .filter((entry) => entry.status === 'ALLOWED')
          .map((entry) => toBaseEvidence(entry.baseComponentId, entry.explanations));
        return {
          primaryImage: component.primaryImage,
          id: component.id,
          name: component.name,
          ...(component.brand === undefined ? {} : { brand: component.brand }),
          componentTypeId: component.componentTypeId,
          componentTypeName: group.componentTypeName,
          relation: 'direct',
          compatibilityByBase,
          explanations: compatibilityByBase.flatMap((entry) => entry.explanations),
        };
      }),
  );
}

export function blockedCandidatesFromAssemblyResponse(response: ConfiguratorCandidatesResponse) {
  return response.candidatesByType.flatMap((group) =>
    group.components
      .filter((component) => component.status === 'BLOCKED')
      .map<ConfiguratorBlockedCandidate>((component) => ({
        primaryImage: component.primaryImage,
        id: component.id,
        name: component.name,
        ...(component.brand === undefined ? {} : { brand: component.brand }),
        componentTypeId: component.componentTypeId,
        componentTypeName: group.componentTypeName,
        blockingByBase: component.compatibilityByBase
          .filter((entry) => entry.status === 'DENIED')
          .map((entry) => ({
            baseComponentId: entry.baseComponentId,
            blockingRules: entry.blockingRules,
          })),
      })),
  );
}

export function validationFromAssemblyResponse(
  response: ConfiguratorCandidatesResponse,
): ConfiguratorValidationResult {
  const pairs: ConfiguratorPairResult[] = response.assemblyDecisions.map((decision) => ({
    leftComponentId: decision.leftComponentId,
    rightComponentId: decision.rightComponentId,
    relation:
      decision.status === 'ALLOWED'
        ? 'direct'
        : decision.status === 'DENIED'
          ? 'incompatible'
          : 'unknown',
    explanations: decision.explanations,
    ...(decision.blockingRules.length > 0 ? { blockingRules: decision.blockingRules } : {}),
  }));
  const conflictingPairs = response.assemblyDecisions.filter(
    (decision) => decision.status === 'DENIED',
  );
  const conflictPairs = conflictingPairs.map(({ leftComponentId, rightComponentId }) => ({
    leftComponentId,
    rightComponentId,
  }));
  const conflictComponentIds =
    response.assemblyStatus === 'DISCONNECTED'
      ? disconnectedComponentIds(response)
      : new Set(conflictPairs.flatMap((pair) => [pair.leftComponentId, pair.rightComponentId]));
  return {
    assemblyStatus: response.assemblyStatus,
    pairs,
    conflictPairs,
    conflictComponentIds,
  };
}

function disconnectedComponentIds(response: ConfiguratorCandidatesResponse) {
  const adjacency = new Map(
    response.componentIds.map((componentId) => [componentId, new Set<number>()]),
  );
  for (const decision of response.assemblyDecisions) {
    if (decision.status !== 'ALLOWED') continue;
    adjacency.get(decision.leftComponentId)?.add(decision.rightComponentId);
    adjacency.get(decision.rightComponentId)?.add(decision.leftComponentId);
  }

  const rootId = response.componentIds[0];
  if (rootId === undefined) return new Set<number>();
  const visited = new Set<number>();
  const pending = [rootId];
  while (pending.length > 0) {
    const componentId = pending.pop()!;
    if (visited.has(componentId)) continue;
    visited.add(componentId);
    pending.push(...(adjacency.get(componentId) ?? []));
  }
  return new Set(response.componentIds.filter((componentId) => !visited.has(componentId)));
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

export function replacementBaseComponentIds(
  componentIds: ReadonlyArray<number>,
  replacedComponentId: number | null,
) {
  return replacedComponentId === null
    ? [...componentIds]
    : componentIds.filter((componentId) => componentId !== replacedComponentId);
}
