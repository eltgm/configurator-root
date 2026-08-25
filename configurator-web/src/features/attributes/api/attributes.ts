import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  apiData,
  client,
  deleteAttributesById,
  deleteComponentTypesByComponentTypeIdAttributesByAttributeId,
  getComponentTypesByIdAttributes,
  getDomainsByDomainIdAttributes,
  postComponentTypesByIdAttributes,
  postDomainsByDomainIdAttributes,
  putAttributesById,
  putComponentTypesByComponentTypeIdAttributesByAttributeId,
  type AttributeDefinition,
  type ComponentTypeAttributeSettingsRequest,
  type CreateAttributeDefinitionRequest,
} from '@/shared/api';

const labelCollator = new Intl.Collator(undefined, { numeric: true, sensitivity: 'base' });

export const attributeKeys = {
  domain: (domainId: number | null) => ['domains', domainId, 'attributes'] as const,
  catalog: (domainId: number | null) => [...attributeKeys.domain(domainId), 'catalog'] as const,
  byType: (domainId: number | null, componentTypeId: number | null) =>
    [...attributeKeys.domain(domainId), 'component-types', componentTypeId] as const,
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
  return sortAttributes(
    await apiData(
      getComponentTypesByIdAttributes({
        client,
        path: { id: componentTypeId },
        throwOnError: true,
      }),
    ),
  );
}

export async function fetchAttributeCatalog(domainId: number): Promise<Array<AttributeDefinition>> {
  return sortAttributes(
    await apiData(
      getDomainsByDomainIdAttributes({
        client,
        path: { domainId },
        throwOnError: true,
      }),
    ),
  );
}

export function useAttributesQuery(domainId: number | null, componentTypeId: number | null) {
  return useQuery({
    queryKey: attributeKeys.byType(domainId, componentTypeId),
    queryFn: () =>
      componentTypeId === null ? Promise.resolve([]) : fetchAttributes(componentTypeId),
    enabled: domainId !== null && componentTypeId !== null,
  });
}

export function useAttributeCatalogQuery(domainId: number | null) {
  return useQuery({
    queryKey: attributeKeys.catalog(domainId),
    queryFn: () => (domainId === null ? Promise.resolve([]) : fetchAttributeCatalog(domainId)),
    enabled: domainId !== null,
  });
}

interface CreateAttributeVariables {
  domainId: number;
  componentTypeId: number;
  body: CreateAttributeDefinitionRequest;
}

interface CatalogAttributeVariables {
  domainId: number;
  body: CreateAttributeDefinitionRequest;
}

interface UpdateAttributeVariables extends CatalogAttributeVariables {
  id: number;
}

interface LinkAttributeVariables {
  domainId: number;
  componentTypeId: number;
  attributeId: number;
  body: ComponentTypeAttributeSettingsRequest;
}

interface RemoveAttributeVariables {
  domainId: number;
  componentTypeId: number;
  attributeId: number;
}

async function invalidateDomainAttributes(
  queryClient: ReturnType<typeof useQueryClient>,
  domainId: number,
) {
  await queryClient.invalidateQueries({ queryKey: attributeKeys.domain(domainId) });
}

export function useCreateAttributeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    retry: false,
    mutationFn: ({ componentTypeId, body }: CreateAttributeVariables) =>
      apiData(
        postComponentTypesByIdAttributes({
          client,
          path: { id: componentTypeId },
          body,
          throwOnError: true,
        }),
      ),
    onSuccess: async (_createdAttribute, { domainId }) =>
      invalidateDomainAttributes(queryClient, domainId),
  });
}

export function useCreateCatalogAttributeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    retry: false,
    mutationFn: ({ domainId, body }: CatalogAttributeVariables) =>
      apiData(
        postDomainsByDomainIdAttributes({
          client,
          path: { domainId },
          body,
          throwOnError: true,
        }),
      ),
    onSuccess: async (_createdAttribute, { domainId }) =>
      invalidateDomainAttributes(queryClient, domainId),
  });
}

export function useUpdateAttributeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    retry: false,
    mutationFn: ({ id, body }: UpdateAttributeVariables) =>
      apiData(putAttributesById({ client, path: { id }, body, throwOnError: true })),
    onSuccess: async (_updatedAttribute, { domainId }) =>
      invalidateDomainAttributes(queryClient, domainId),
  });
}

export function useAttachAttributeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    retry: false,
    mutationFn: ({ componentTypeId, attributeId, body }: LinkAttributeVariables) =>
      apiData(
        putComponentTypesByComponentTypeIdAttributesByAttributeId({
          client,
          path: { componentTypeId, attributeId },
          body,
          throwOnError: true,
        }),
      ),
    onSuccess: async (_attribute, { domainId }) =>
      invalidateDomainAttributes(queryClient, domainId),
  });
}

export function useDetachAttributeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    retry: false,
    mutationFn: ({ componentTypeId, attributeId }: RemoveAttributeVariables) =>
      apiData(
        deleteComponentTypesByComponentTypeIdAttributesByAttributeId({
          client,
          path: { componentTypeId, attributeId },
          throwOnError: true,
        }),
      ),
    onSuccess: async (_result, { domainId }) => invalidateDomainAttributes(queryClient, domainId),
  });
}

export function useDeleteAttributeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    retry: false,
    mutationFn: ({ attributeId }: Omit<RemoveAttributeVariables, 'componentTypeId'>) =>
      apiData(deleteAttributesById({ client, path: { id: attributeId }, throwOnError: true })),
    onSuccess: async (_result, { domainId }) => invalidateDomainAttributes(queryClient, domainId),
  });
}
