import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  apiData,
  client,
  getDomainsByIdConfigurations,
  postDomainsByIdConfigurations,
  type ConfigurationPage,
  type CreateConfigurationRequest,
} from '@/shared/api';

export const configurationListPageSize = 10;

export const configurationKeys = {
  byDomain: (domainId: number | null) => ['domains', domainId, 'configurations'] as const,
  list: (domainId: number | null, page: number, size: number) =>
    [...configurationKeys.byDomain(domainId), 'list', page, size] as const,
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
      await queryClient.invalidateQueries({ queryKey: configurationKeys.byDomain(domainId) });
    },
  });
}
