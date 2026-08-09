import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  apiData,
  client,
  getComponentTypesByIdAttributes,
  postComponentTypesByIdAttributes,
  putAttributesById,
  type AttributeDefinition,
  type CreateAttributeDefinitionRequest,
} from '@/shared/api';

const labelCollator = new Intl.Collator(undefined, { numeric: true, sensitivity: 'base' });

export const attributeKeys = {
  byType: (domainId: number | null, componentTypeId: number | null) =>
    ['domains', domainId, 'component-types', componentTypeId, 'attributes'] as const,
};

export function sortAttributes(
  attributes: ReadonlyArray<AttributeDefinition>,
): Array<AttributeDefinition> {
  return [...attributes].sort((left, right) => {
    const leftOrder = left.orderIndex ?? Number.MAX_SAFE_INTEGER;
    const rightOrder = right.orderIndex ?? Number.MAX_SAFE_INTEGER;
    return (
      leftOrder - rightOrder ||
      labelCollator.compare(left.label, right.label) ||
      labelCollator.compare(left.name, right.name) ||
      left.id - right.id
    );
  });
}

export async function fetchAttributes(
  componentTypeId: number,
): Promise<Array<AttributeDefinition>> {
  const attributes = await apiData(
    getComponentTypesByIdAttributes({
      client,
      path: { id: componentTypeId },
      throwOnError: true,
    }),
  );
  return sortAttributes(attributes);
}

export function useAttributesQuery(domainId: number | null, componentTypeId: number | null) {
  return useQuery({
    queryKey: attributeKeys.byType(domainId, componentTypeId),
    queryFn: () =>
      componentTypeId === null ? Promise.resolve([]) : fetchAttributes(componentTypeId),
    enabled: domainId !== null && componentTypeId !== null,
  });
}

interface CreateAttributeVariables {
  domainId: number;
  componentTypeId: number;
  body: CreateAttributeDefinitionRequest;
}

export function useCreateAttributeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ componentTypeId, body }: CreateAttributeVariables) =>
      apiData(
        postComponentTypesByIdAttributes({
          client,
          path: { id: componentTypeId },
          body,
          throwOnError: true,
        }),
      ),
    onSuccess: async (createdAttribute, { domainId, componentTypeId }) => {
      const queryKey = attributeKeys.byType(domainId, componentTypeId);
      queryClient.setQueryData<Array<AttributeDefinition>>(queryKey, (attributes = []) =>
        sortAttributes([...attributes, createdAttribute]),
      );
      await queryClient.invalidateQueries({ queryKey });
    },
  });
}

interface UpdateAttributeVariables {
  domainId: number;
  componentTypeId: number;
  id: number;
  body: CreateAttributeDefinitionRequest;
}

export function useUpdateAttributeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: UpdateAttributeVariables) =>
      apiData(putAttributesById({ client, path: { id }, body, throwOnError: true })),
    onSuccess: async (updatedAttribute, { domainId, componentTypeId }) => {
      const queryKey = attributeKeys.byType(domainId, componentTypeId);
      queryClient.setQueryData<Array<AttributeDefinition>>(queryKey, (attributes = []) =>
        sortAttributes(
          attributes.map((attribute) =>
            attribute.id === updatedAttribute.id ? updatedAttribute : attribute,
          ),
        ),
      );
      await queryClient.invalidateQueries({ queryKey });
    },
  });
}
