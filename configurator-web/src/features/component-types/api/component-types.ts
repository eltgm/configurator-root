import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { attributeKeys } from '@/features/attributes/api/attributes';
import {
  apiData,
  client,
  deleteComponentTypesById,
  getDomainsByIdComponentTypes,
  postDomainsByIdComponentTypes,
  putComponentTypesById,
  type ComponentType,
  type CreateComponentTypeRequest,
} from '@/shared/api';

const nameCollator = new Intl.Collator(undefined, { numeric: true, sensitivity: 'base' });

export const componentTypeKeys = {
  byDomain: (domainId: number | null) => ['domains', domainId, 'component-types'] as const,
};

export function sortComponentTypes(types: ReadonlyArray<ComponentType>): Array<ComponentType> {
  return [...types].sort((left, right) => {
    const leftOrder = left.orderIndex ?? Number.MAX_SAFE_INTEGER;
    const rightOrder = right.orderIndex ?? Number.MAX_SAFE_INTEGER;
    return (
      leftOrder - rightOrder || nameCollator.compare(left.name, right.name) || left.id - right.id
    );
  });
}

export async function fetchComponentTypes(domainId: number): Promise<Array<ComponentType>> {
  const types = await apiData(
    getDomainsByIdComponentTypes({ client, path: { id: domainId }, throwOnError: true }),
  );
  return sortComponentTypes(types);
}

export function useComponentTypesQuery(domainId: number | null) {
  return useQuery({
    queryKey: componentTypeKeys.byDomain(domainId),
    queryFn: () => (domainId === null ? Promise.resolve([]) : fetchComponentTypes(domainId)),
    enabled: domainId !== null,
  });
}

interface CreateComponentTypeVariables {
  domainId: number;
  body: CreateComponentTypeRequest;
}

export function useCreateComponentTypeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ domainId, body }: CreateComponentTypeVariables) =>
      apiData(
        postDomainsByIdComponentTypes({
          client,
          path: { id: domainId },
          body,
          throwOnError: true,
        }),
      ),
    onSuccess: async (createdType) => {
      const queryKey = componentTypeKeys.byDomain(createdType.domainId);
      queryClient.setQueryData<Array<ComponentType>>(queryKey, (types = []) =>
        sortComponentTypes([...types, createdType]),
      );
      await queryClient.invalidateQueries({ queryKey });
    },
  });
}

interface UpdateComponentTypeVariables {
  domainId: number;
  id: number;
  body: CreateComponentTypeRequest;
}

export function useUpdateComponentTypeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: UpdateComponentTypeVariables) =>
      apiData(putComponentTypesById({ client, path: { id }, body, throwOnError: true })),
    onSuccess: async (updatedType, { domainId }) => {
      const queryKey = componentTypeKeys.byDomain(domainId);
      queryClient.setQueryData<Array<ComponentType>>(queryKey, (types = []) =>
        sortComponentTypes(types.map((type) => (type.id === updatedType.id ? updatedType : type))),
      );
      await queryClient.invalidateQueries({ queryKey });
    },
  });
}

interface DeleteComponentTypeVariables {
  domainId: number;
  id: number;
}

export function useDeleteComponentTypeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: DeleteComponentTypeVariables) =>
      apiData(deleteComponentTypesById({ client, path: { id }, throwOnError: true })),
    onSuccess: async (_response, { domainId, id }) => {
      const queryKey = componentTypeKeys.byDomain(domainId);
      queryClient.setQueryData<Array<ComponentType>>(queryKey, (types = []) =>
        types.filter((type) => type.id !== id),
      );
      queryClient.removeQueries({ queryKey: attributeKeys.byType(domainId, id) });
      await queryClient.invalidateQueries({ queryKey });
    },
  });
}
