import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  apiData,
  client,
  deleteDomainsById,
  getDomains,
  postDomains,
  postDomainsDemo,
  putDomainsById,
  type CreateDomainRequest,
  type Domain,
  type UpdateDomainRequest,
} from '@/shared/api';

const domainPageSize = 100;

export const domainKeys = {
  all: ['domains'] as const,
};

export async function fetchAllDomains(): Promise<Array<Domain>> {
  const domains: Array<Domain> = [];
  let page = 0;

  while (true) {
    const pageData = await apiData(
      getDomains({
        client,
        query: { page, size: domainPageSize },
        throwOnError: true,
      }),
    );
    domains.push(...pageData.items);

    if (domains.length >= pageData.totalItems || pageData.items.length === 0) {
      return domains;
    }
    page += 1;
  }
}

export function useDomainsQuery() {
  return useQuery({
    queryKey: domainKeys.all,
    queryFn: fetchAllDomains,
  });
}

export function useCreateDomainMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateDomainRequest) =>
      apiData(postDomains({ client, body, throwOnError: true })),
    onSuccess: async (createdDomain) => {
      queryClient.setQueryData<Array<Domain>>(domainKeys.all, (domains = []) => [
        ...domains,
        createdDomain,
      ]);
      await queryClient.invalidateQueries({ queryKey: domainKeys.all });
    },
  });
}

export function useCreateDemoDomainMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => apiData(postDomainsDemo({ client, throwOnError: true })),
    onSuccess: async (createdDomain) => {
      queryClient.setQueryData<Array<Domain>>(domainKeys.all, (domains = []) => [
        ...domains,
        createdDomain,
      ]);
      await queryClient.invalidateQueries({ queryKey: domainKeys.all });
    },
  });
}

export interface UpdateDomainVariables {
  id: number;
  body: UpdateDomainRequest;
}

export function useUpdateDomainMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: UpdateDomainVariables) =>
      apiData(putDomainsById({ client, path: { id }, body, throwOnError: true })),
    onSuccess: async (updatedDomain) => {
      queryClient.setQueryData<Array<Domain>>(domainKeys.all, (domains = []) =>
        domains.map((domain) => (domain.id === updatedDomain.id ? updatedDomain : domain)),
      );
      await queryClient.invalidateQueries({ queryKey: domainKeys.all });
    },
  });
}

export function useDeleteDomainMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      apiData(deleteDomainsById({ client, path: { id }, throwOnError: true })),
    onSuccess: async (_data, deletedDomainId) => {
      queryClient.setQueryData<Array<Domain>>(domainKeys.all, (domains = []) =>
        domains.filter((domain) => domain.id !== deletedDomainId),
      );
      await queryClient.invalidateQueries({ queryKey: domainKeys.all });
    },
  });
}
