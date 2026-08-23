import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  apiData,
  client,
  getConfigurationsById,
  getDomainsByIdConfigurations,
  postDomainsByIdConfigurations,
  putConfigurationsById,
  type Configuration,
  type ConfigurationPage,
  type CreateConfigurationRequest,
  type UpdateConfigurationRequest,
} from '@/shared/api';

export const configurationListPageSize = 10;

export const configurationKeys = {
  byDomain: (domainId: number | null) => ['domains', domainId, 'configurations'] as const,
  lists: (domainId: number | null) => [...configurationKeys.byDomain(domainId), 'list'] as const,
  list: (domainId: number | null, page: number, size: number) =>
    [...configurationKeys.lists(domainId), page, size] as const,
  detail: (domainId: number | null, configurationId: number | null) =>
    [...configurationKeys.byDomain(domainId), 'detail', configurationId] as const,
};

export async function fetchConfigurations(
  domainId: number,
  page: number,
  size = configurationListPageSize,
): Promise<ConfigurationPage> {
  return apiData(
    getDomainsByIdConfigurations({
      client,
      path: { id: domainId },
      query: { page, size },
      throwOnError: true,
    }),
  );
}

export function useConfigurationsQuery(
  domainId: number | null,
  page: number,
  size = configurationListPageSize,
) {
  return useQuery({
    queryKey: configurationKeys.list(domainId, page, size),
    queryFn: () =>
      domainId === null
        ? Promise.resolve({ items: [], page: 0, size, totalItems: 0 })
        : fetchConfigurations(domainId, page, size),
    enabled: domainId !== null,
    placeholderData: keepPreviousData,
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

export function useCreateConfigurationMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ domainId, body }: CreateConfigurationVariables) =>
      apiData(
        postDomainsByIdConfigurations({
          client,
          path: { id: domainId },
          body,
          throwOnError: true,
        }),
      ),
    onSuccess: async (_configuration, { domainId }) => {
      await queryClient.invalidateQueries({ queryKey: configurationKeys.lists(domainId) });
    },
  });
}

export interface UpdateConfigurationVariables {
  domainId: number;
  configurationId: number;
  body: UpdateConfigurationRequest;
}

export function useUpdateConfigurationMutation() {
  const queryClient = useQueryClient();
  return useMutation({
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
      await queryClient.invalidateQueries({ queryKey: configurationKeys.lists(domainId) });
    },
  });
}
