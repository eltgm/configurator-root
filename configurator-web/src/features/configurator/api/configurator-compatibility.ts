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

const directOnly = false;

export const configuratorCompatibilityKeys = {
  root: (domainId: number | null) =>
    ['domains', domainId, 'configurator', 'compatibility'] as const,
  direct: (domainId: number | null, componentId: number | null) =>
    [...configuratorCompatibilityKeys.root(domainId), 'direct', componentId, directOnly] as const,
  batch: (domainId: number | null, componentIds: ReadonlyArray<number>) =>
    [
      ...configuratorCompatibilityKeys.root(domainId),
      'batch',
      [...componentIds],
      directOnly,
    ] as const,
  intersection: (domainId: number | null, componentIds: ReadonlyArray<number>) =>
    [
      ...configuratorCompatibilityKeys.root(domainId),
      'intersection',
      [...componentIds],
      directOnly,
    ] as const,
};

export function fetchDirectCompatibility(
  domainId: number,
  componentId: number,
): Promise<ConfiguratorResponse> {
  return apiData(
    getDomainsByIdConfiguratorCompatible({
      client,
      path: { id: domainId },
      query: { componentId, includeTransitive: directOnly },
      throwOnError: true,
    }),
  );
}

export function fetchBatchCompatibility(
  domainId: number,
  componentIds: ReadonlyArray<number>,
): Promise<ConfiguratorBatchSearchResponse> {
  return apiData(
    postDomainsByIdConfiguratorCompatibleSearch({
      client,
      path: { id: domainId },
      body: { componentIds: [...componentIds], includeTransitive: directOnly },
      throwOnError: true,
    }),
  );
}

export function fetchCompatibilityIntersection(
  domainId: number,
  componentIds: ReadonlyArray<number>,
): Promise<ConfiguratorIntersectionResponse> {
  return apiData(
    postDomainsByIdConfiguratorCompatibleIntersection({
      client,
      path: { id: domainId },
      body: { componentIds: [...componentIds], includeTransitive: directOnly },
      throwOnError: true,
    }),
  );
}

export function useDirectCompatibilityQuery(
  domainId: number | null,
  componentId: number | null,
  enabled = true,
) {
  return useQuery({
    queryKey: configuratorCompatibilityKeys.direct(domainId, componentId),
    queryFn: () => {
      if (domainId === null || componentId === null) {
        throw new Error('Domain and base component are required');
      }
      return fetchDirectCompatibility(domainId, componentId);
    },
    enabled: enabled && domainId !== null && componentId !== null,
  });
}

export function useBatchCompatibilityQuery(
  domainId: number | null,
  componentIds: ReadonlyArray<number>,
  enabled = true,
) {
  return useQuery({
    queryKey: configuratorCompatibilityKeys.batch(domainId, componentIds),
    queryFn: () => {
      if (domainId === null || componentIds.length === 0) {
        throw new Error('Domain and at least one base component are required');
      }
      return fetchBatchCompatibility(domainId, componentIds);
    },
    enabled: enabled && domainId !== null && componentIds.length > 0,
  });
}

export function useCompatibilityIntersectionQuery(
  domainId: number | null,
  componentIds: ReadonlyArray<number>,
  enabled = true,
) {
  return useQuery({
    queryKey: configuratorCompatibilityKeys.intersection(domainId, componentIds),
    queryFn: () => {
      if (domainId === null || componentIds.length < 2) {
        throw new Error('Domain and at least two base components are required');
      }
      return fetchCompatibilityIntersection(domainId, componentIds);
    },
    enabled: enabled && domainId !== null && componentIds.length >= 2,
  });
}
