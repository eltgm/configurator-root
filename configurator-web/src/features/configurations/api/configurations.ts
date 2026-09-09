import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { componentKeys } from '@/features/components/api/components';
import { configuratorCompatibilityKeys } from '@/features/configurator/api/configurator-compatibility';

import {
  apiData,
  client,
  deleteConfigurationsById,
  getConfigurationsById,
  getConfigurationsByIdExportJson,
  getDomainsByIdConfigurations,
  postDomainsByIdConfigurations,
  putConfigurationsById,
  type Configuration,
  type ConfigurationExport,
  type ConfigurationPage,
  type CreateConfigurationRequest,
  type UpdateConfigurationRequest,
} from '@/shared/api';

export const configurationListPageSize = 10;
export interface ConfigurationSearch {
  name?: string;
  sortBy?: 'createdAt' | 'name';
  sortDirection?: 'asc' | 'desc';
}

export type ConfigurationInventoryFilter = 'all' | 'tracked' | 'untracked';

export const configurationKeys = {
  byDomain: (domainId: number | null) => ['domains', domainId, 'configurations'] as const,
  lists: (domainId: number | null) => [...configurationKeys.byDomain(domainId), 'list'] as const,
  list: (
    domainId: number | null,
    page: number,
    size: number,
    inventoryFilter: ConfigurationInventoryFilter,
    search: ConfigurationSearch = {},
  ) => [...configurationKeys.lists(domainId), page, size, inventoryFilter, search] as const,
  detail: (domainId: number | null, configurationId: number | null) =>
    [...configurationKeys.byDomain(domainId), 'detail', configurationId] as const,
};

async function invalidateInventoryQueries(
  queryClient: ReturnType<typeof useQueryClient>,
  domainId: number,
) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: componentKeys.byDomain(domainId) }),
    queryClient.invalidateQueries({ queryKey: configuratorCompatibilityKeys.root(domainId) }),
  ]);
}

export async function fetchConfigurations(
  domainId: number,
  page: number,
  size = configurationListPageSize,
  inventoryFilter: ConfigurationInventoryFilter = 'all',
  search: ConfigurationSearch = {},
): Promise<ConfigurationPage> {
  return apiData(
    getDomainsByIdConfigurations({
      client,
      path: { id: domainId },
      query: {
        page,
        size,
        ...search,
        ...(inventoryFilter === 'all' ? {} : { trackInventory: inventoryFilter === 'tracked' }),
      },
      throwOnError: true,
    }),
  );
}

export function useConfigurationsQuery(
  domainId: number | null,
  page: number,
  size = configurationListPageSize,
  inventoryFilter: ConfigurationInventoryFilter = 'all',
  search: ConfigurationSearch = {},
) {
  return useQuery({
    queryKey: configurationKeys.list(domainId, page, size, inventoryFilter, search),
    queryFn: () =>
      domainId === null
        ? Promise.resolve({ items: [], page: 0, size, totalItems: 0 })
        : fetchConfigurations(domainId, page, size, inventoryFilter, search),
    enabled: domainId !== null,
    placeholderData: (previous, previousQuery) =>
      previousQuery?.queryKey[1] === domainId ? keepPreviousData(previous) : undefined,
  });
}

export function fetchConfiguration(configurationId: number): Promise<Configuration> {
  return apiData(
    getConfigurationsById({
      client,
      path: { id: configurationId },
      throwOnError: true,
    }),
  );
}

export function useConfigurationQuery(domainId: number | null, configurationId: number | null) {
  return useQuery({
    queryKey: configurationKeys.detail(domainId, configurationId),
    queryFn: () => {
      if (configurationId === null) {
        throw new Error('Configuration identifier is required');
      }
      return fetchConfiguration(configurationId);
    },
    enabled: domainId !== null && configurationId !== null,
  });
}

export interface CreateConfigurationVariables {
  domainId: number;
  body: CreateConfigurationRequest;
}

export function useCreateConfigurationMutation(errorHandledLocally = false) {
  const queryClient = useQueryClient();
  return useMutation({
    meta: { errorHandledLocally },
    mutationFn: ({ domainId, body }: CreateConfigurationVariables) =>
      apiData(
        postDomainsByIdConfigurations({
          client,
          path: { id: domainId },
          body,
          throwOnError: true,
        }),
      ),
    onSuccess: async (configuration, { domainId }) => {
      queryClient.setQueryData(configurationKeys.detail(domainId, configuration.id), configuration);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: configurationKeys.lists(domainId) }),
        invalidateInventoryQueries(queryClient, domainId),
      ]);
    },
  });
}

export interface UpdateConfigurationVariables {
  domainId: number;
  configurationId: number;
  body: UpdateConfigurationRequest;
}

export function useUpdateConfigurationMutation(errorHandledLocally = false) {
  const queryClient = useQueryClient();
  return useMutation({
    meta: { errorHandledLocally },
    mutationFn: ({ configurationId, body }: UpdateConfigurationVariables) =>
      apiData(
        putConfigurationsById({
          client,
          path: { id: configurationId },
          body,
          throwOnError: true,
        }),
      ),
    onSuccess: async (configuration, { domainId, configurationId }) => {
      queryClient.setQueryData(configurationKeys.detail(domainId, configurationId), configuration);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: configurationKeys.lists(domainId) }),
        invalidateInventoryQueries(queryClient, domainId),
      ]);
    },
  });
}

export function fetchConfigurationExport(configurationId: number): Promise<ConfigurationExport> {
  return apiData(
    getConfigurationsByIdExportJson({
      client,
      path: { id: configurationId },
      throwOnError: true,
    }),
  );
}

export function useExportConfigurationMutation() {
  return useMutation({
    mutationFn: (configurationId: number) => fetchConfigurationExport(configurationId),
  });
}

export interface DeleteConfigurationVariables {
  domainId: number;
  configurationId: number;
}

export function useDeleteConfigurationMutation(errorHandledLocally = false) {
  const queryClient = useQueryClient();
  return useMutation({
    meta: { errorHandledLocally },
    mutationFn: ({ configurationId }: DeleteConfigurationVariables) =>
      apiData(
        deleteConfigurationsById({
          client,
          path: { id: configurationId },
          throwOnError: true,
        }),
      ),
    onSuccess: async (_result, { domainId, configurationId }) => {
      queryClient.removeQueries({
        queryKey: configurationKeys.detail(domainId, configurationId),
        exact: true,
      });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: configurationKeys.lists(domainId) }),
        invalidateInventoryQueries(queryClient, domainId),
      ]);
    },
  });
}
