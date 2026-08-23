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
  explanations: ReadonlyArray<CompatibilityExplanation>;
}

export interface ConfiguratorConflictPair {
  leftComponentId: number;
  rightComponentId: number;
}

export interface ConfiguratorValidationResult {
  compatible: boolean;
  conflictPairs: ReadonlyArray<ConfiguratorConflictPair>;
  conflictComponentIds: ReadonlySet<number>;
}

export function candidatesFromDirectResponse(response: ConfiguratorResponse) {
  return response.compatibleByType.flatMap((group) =>
    group.components.map<ConfiguratorCandidate>((component) => ({
      ...component,
      componentTypeName: group.componentTypeName,
      explanations: component.explanations,
    })),
  );
}

export function candidatesFromIntersectionResponse(response: ConfiguratorIntersectionResponse) {
  return response.compatibleByType.flatMap((group) =>
    group.components.map<ConfiguratorCandidate>((component) => ({
      id: component.id,
      name: component.name,
      ...(component.brand === undefined ? {} : { brand: component.brand }),
      componentTypeId: component.componentTypeId,
      componentTypeName: group.componentTypeName,
      explanations: component.compatibilityByBase.flatMap((entry) => entry.explanations),
    })),
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

function compatibleIdsByBase(response: ConfiguratorBatchSearchResponse) {
  return new Map(
    response.results.map((result) => [
      result.baseComponentId,
      new Set(
        result.compatibleByType.flatMap((group) =>
          group.components.map((component) => component.id),
        ),
      ),
    ]),
  );
}

export function validateConfiguratorAssembly(
  componentIds: ReadonlyArray<number>,
  response: ConfiguratorBatchSearchResponse,
): ConfiguratorValidationResult {
  const compatibleByBase = compatibleIdsByBase(response);
  const conflictPairs: ConfiguratorConflictPair[] = [];
  const conflictComponentIds = new Set<number>();

  for (let leftIndex = 0; leftIndex < componentIds.length; leftIndex += 1) {
    const leftComponentId = componentIds[leftIndex]!;
    for (let rightIndex = leftIndex + 1; rightIndex < componentIds.length; rightIndex += 1) {
      const rightComponentId = componentIds[rightIndex]!;
      const compatible =
        compatibleByBase.get(leftComponentId)?.has(rightComponentId) === true &&
        compatibleByBase.get(rightComponentId)?.has(leftComponentId) === true;
      if (!compatible) {
        conflictPairs.push({ leftComponentId, rightComponentId });
        conflictComponentIds.add(leftComponentId);
        conflictComponentIds.add(rightComponentId);
      }
    }
  }

  return {
    compatible: conflictPairs.length === 0,
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
