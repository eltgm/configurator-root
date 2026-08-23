import {
  keepPreviousData,
  queryOptions,
  useMutation,
  useQueries,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import {
  apiData,
  client,
  deleteComponentsById,
  getComponentsById,
  getDomainsByDomainIdComponents,
  postComponents,
  postComponentsByIdRestore,
  putComponentsById,
  type Component,
  type ComponentPage,
  type CreateComponentRequest,
  type UpdateComponentRequest,
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
  detail: (domainId: number | null, id: number | null) =>
    [...componentKeys.byDomain(domainId), 'detail', id] as const,
  catalog: (domainId: number | null, filters: ComponentCatalogFilters) =>
    [...componentKeys.byDomain(domainId), normalizeComponentCatalogFilters(filters)] as const,
};

export function componentDetailQueryOptions(domainId: number | null, id: number | null) {
  return queryOptions({
    queryKey: componentKeys.detail(domainId, id),
    queryFn: () =>
      id === null
        ? Promise.resolve(null)
        : apiData(getComponentsById({ client, path: { id }, throwOnError: true })),
    enabled: domainId !== null && id !== null,
  });
}

export function useComponentQuery(domainId: number | null, id: number | null) {
  return useQuery(componentDetailQueryOptions(domainId, id));
}

export function useComponentDetailsQueries(
  domainId: number | null,
  componentIds: ReadonlyArray<number>,
) {
  return useQueries({
    queries: componentIds.map((id) => componentDetailQueryOptions(domainId, id)),
  });
}

interface CreateComponentVariables {
  domainId: number;
  body: CreateComponentRequest;
}

export function useCreateComponentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ body }: CreateComponentVariables) =>
      apiData(postComponents({ client, body, throwOnError: true })),
    onSuccess: async (createdComponent, { domainId }) => {
      queryClient.setQueryData<Component>(
        componentKeys.detail(domainId, createdComponent.id),
        createdComponent,
      );
      await queryClient.invalidateQueries({ queryKey: componentKeys.byDomain(domainId) });
    },
  });
}

interface UpdateComponentVariables {
  domainId: number;
  id: number;
  body: UpdateComponentRequest;
}

export function useUpdateComponentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: UpdateComponentVariables) =>
      apiData(putComponentsById({ client, path: { id }, body, throwOnError: true })),
    onSuccess: async (updatedComponent, { domainId }) => {
      queryClient.setQueryData<Component>(
        componentKeys.detail(domainId, updatedComponent.id),
        updatedComponent,
      );
      await queryClient.invalidateQueries({ queryKey: componentKeys.byDomain(domainId) });
    },
  });
}

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
    onSuccess: async (_response, { domainId, id }) => {
      queryClient.setQueryData<Component>(componentKeys.detail(domainId, id), (component) =>
        component ? { ...component, archived: true } : component,
      );
      await queryClient.invalidateQueries({ queryKey: componentKeys.byDomain(domainId) });
    },
  });
}

export function useRestoreComponentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: ComponentMutationVariables) =>
      apiData(postComponentsByIdRestore({ client, path: { id }, throwOnError: true })),
    onSuccess: async (component, { domainId }) => {
      queryClient.setQueryData<Component>(componentKeys.detail(domainId, component.id), component);
      await queryClient.invalidateQueries({ queryKey: componentKeys.byDomain(domainId) });
    },
  });
}
