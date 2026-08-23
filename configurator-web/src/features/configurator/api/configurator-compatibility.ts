import { useQuery } from '@tanstack/react-query';

import {
  apiData,
  client,
  getDomainsByIdConfiguratorCompatible,
  postDomainsByIdConfiguratorCompatibleIntersection,
  postDomainsByIdConfiguratorCompatibleSearch,
  type ConfiguratorBatchSearchResponse,
  type ConfiguratorIntersectionResponse,
  type ConfiguratorResponse,
} from '@/shared/api';

export const configuratorCompatibilityKeys = {
  root: (domainId: number | null) =>
    ['domains', domainId, 'configurator', 'compatibility'] as const,
  direct: (domainId: number | null, componentId: number | null, includeTransitive: boolean) =>
    [
      ...configuratorCompatibilityKeys.root(domainId),
      'direct',
      componentId,
      includeTransitive,
    ] as const,
  batch: (
    domainId: number | null,
    componentIds: ReadonlyArray<number>,
    includeTransitive: boolean,
  ) =>
    [
      ...configuratorCompatibilityKeys.root(domainId),
      'batch',
      [...componentIds],
      includeTransitive,
    ] as const,
  intersection: (
    domainId: number | null,
    componentIds: ReadonlyArray<number>,
    includeTransitive: boolean,
  ) =>
    [
      ...configuratorCompatibilityKeys.root(domainId),
      'intersection',
      [...componentIds],
      includeTransitive,
    ] as const,
};

export function fetchDirectCompatibility(
  domainId: number,
  componentId: number,
  includeTransitive: boolean,
): Promise<ConfiguratorResponse> {
  return apiData(
    getDomainsByIdConfiguratorCompatible({
      client,
      path: { id: domainId },
      query: { componentId, includeTransitive },
      throwOnError: true,
    }),
  );
}

export function fetchBatchCompatibility(
  domainId: number,
  componentIds: ReadonlyArray<number>,
  includeTransitive: boolean,
): Promise<ConfiguratorBatchSearchResponse> {
  return apiData(
    postDomainsByIdConfiguratorCompatibleSearch({
      client,
      path: { id: domainId },
      body: { componentIds: [...componentIds], includeTransitive },
      throwOnError: true,
    }),
  );
}

export function fetchCompatibilityIntersection(
  domainId: number,
  componentIds: ReadonlyArray<number>,
  includeTransitive: boolean,
): Promise<ConfiguratorIntersectionResponse> {
  return apiData(
    postDomainsByIdConfiguratorCompatibleIntersection({
      client,
      path: { id: domainId },
      body: { componentIds: [...componentIds], includeTransitive },
      throwOnError: true,
    }),
  );
}

export function useDirectCompatibilityQuery(
  domainId: number | null,
  componentId: number | null,
  includeTransitive: boolean,
  enabled = true,
) {
  return useQuery({
    queryKey: configuratorCompatibilityKeys.direct(domainId, componentId, includeTransitive),
    queryFn: () => {
      if (domainId === null || componentId === null) {
        throw new Error('Domain and base component are required');
      }
      return fetchDirectCompatibility(domainId, componentId, includeTransitive);
    },
    enabled: enabled && domainId !== null && componentId !== null,
  });
}

export function useBatchCompatibilityQuery(
  domainId: number | null,
  componentIds: ReadonlyArray<number>,
  includeTransitive: boolean,
  enabled = true,
) {
  return useQuery({
    queryKey: configuratorCompatibilityKeys.batch(domainId, componentIds, includeTransitive),
    queryFn: () => {
      if (domainId === null || componentIds.length === 0) {
        throw new Error('Domain and at least one base component are required');
      }
      return fetchBatchCompatibility(domainId, componentIds, includeTransitive);
    },
    enabled: enabled && domainId !== null && componentIds.length > 0,
  });
}

export function useCompatibilityIntersectionQuery(
  domainId: number | null,
  componentIds: ReadonlyArray<number>,
  includeTransitive: boolean,
  enabled = true,
) {
  return useQuery({
    queryKey: configuratorCompatibilityKeys.intersection(domainId, componentIds, includeTransitive),
    queryFn: () => {
      if (domainId === null || componentIds.length < 2) {
        throw new Error('Domain and at least two base components are required');
      }
      return fetchCompatibilityIntersection(domainId, componentIds, includeTransitive);
    },
    enabled: enabled && domainId !== null && componentIds.length >= 2,
  });
}
