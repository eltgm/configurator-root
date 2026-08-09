import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  apiData,
  client,
  deleteComponentsById,
  getDomainsByDomainIdComponents,
  postComponentsByIdRestore,
  type ComponentPage,
} from '@/shared/api';

export const componentCatalogPageSize = 12;

export interface ComponentCatalogFilters {
  componentTypeId?: number | undefined;
  name?: string | undefined;
  archived: boolean;
  page: number;
  size: number;
}

export interface NormalizedComponentCatalogFilters {
  componentTypeId?: number;
  name?: string;
  archived: boolean;
  page: number;
  size: number;
}

export function normalizeComponentCatalogFilters(
  filters: ComponentCatalogFilters,
): NormalizedComponentCatalogFilters {
  const name = filters.name?.trim();
  return {
    ...(filters.componentTypeId === undefined ? {} : { componentTypeId: filters.componentTypeId }),
    ...(name ? { name } : {}),
    archived: filters.archived,
    page: filters.page,
    size: filters.size,
  };
}

export const componentKeys = {
  byDomain: (domainId: number | null) => ['domains', domainId, 'components'] as const,
  catalog: (domainId: number | null, filters: ComponentCatalogFilters) =>
    [...componentKeys.byDomain(domainId), normalizeComponentCatalogFilters(filters)] as const,
};

export async function fetchComponents(
  domainId: number,
  filters: ComponentCatalogFilters,
): Promise<ComponentPage> {
  const normalizedFilters = normalizeComponentCatalogFilters(filters);
  return apiData(
    getDomainsByDomainIdComponents({
      client,
      path: { domainId },
      query: normalizedFilters,
      throwOnError: true,
    }),
  );
}

export function useComponentsQuery(domainId: number | null, filters: ComponentCatalogFilters) {
  const normalizedFilters = normalizeComponentCatalogFilters(filters);
  return useQuery({
    queryKey: componentKeys.catalog(domainId, normalizedFilters),
    queryFn: () =>
      domainId === null
        ? Promise.resolve({ items: [], page: 0, size: normalizedFilters.size, totalItems: 0 })
        : fetchComponents(domainId, normalizedFilters),
    enabled: domainId !== null,
    placeholderData: keepPreviousData,
  });
}

interface ComponentMutationVariables {
  domainId: number;
  id: number;
}

export function useArchiveComponentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: ComponentMutationVariables) =>
      apiData(deleteComponentsById({ client, path: { id }, throwOnError: true })),
    onSuccess: async (_response, { domainId }) => {
      await queryClient.invalidateQueries({ queryKey: componentKeys.byDomain(domainId) });
    },
  });
}

export function useRestoreComponentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: ComponentMutationVariables) =>
      apiData(postComponentsByIdRestore({ client, path: { id }, throwOnError: true })),
    onSuccess: async (_component, { domainId }) => {
      await queryClient.invalidateQueries({ queryKey: componentKeys.byDomain(domainId) });
    },
  });
}
