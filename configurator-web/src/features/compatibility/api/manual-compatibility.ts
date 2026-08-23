import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  apiData,
  client,
  deleteDomainsByIdCompatibilityByLinkId,
  getDomainsByIdCompatibilityGraph,
  postDomainsByIdCompatibility,
  type CompatibilityLink,
  type CreateCompatibilityLinkRequest,
  type GraphResponse,
} from '@/shared/api';

export const compatibilityKeys = {
  graph: (domainId: number | null) => ['domains', domainId, 'compatibility', 'graph'] as const,
};

export async function fetchCompatibilityGraph(domainId: number): Promise<GraphResponse> {
  return apiData(
    getDomainsByIdCompatibilityGraph({
      client,
      path: { id: domainId },
      throwOnError: true,
    }),
  );
}

export function useCompatibilityGraphQuery(domainId: number | null) {
  return useQuery({
    queryKey: compatibilityKeys.graph(domainId),
    queryFn: () =>
      domainId === null
        ? Promise.resolve<GraphResponse>({ nodes: [], edges: [] })
        : fetchCompatibilityGraph(domainId),
    enabled: domainId !== null,
    refetchOnMount: 'always',
  });
}

interface CreateCompatibilityLinkVariables {
  domainId: number;
  body: CreateCompatibilityLinkRequest;
}

export function useCreateCompatibilityLinkMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ domainId, body }: CreateCompatibilityLinkVariables) =>
      apiData(
        postDomainsByIdCompatibility({
          client,
          path: { id: domainId },
          body,
          throwOnError: true,
        }),
      ),
    onSuccess: async (createdLink: CompatibilityLink, { domainId }) => {
      const queryKey = compatibilityKeys.graph(domainId);
      queryClient.setQueryData<GraphResponse>(queryKey, (graph) => {
        if (!graph || graph.edges.some((edge) => edge.id === createdLink.id)) {
          return graph;
        }
        return {
          ...graph,
          edges: [
            ...graph.edges,
            {
              id: createdLink.id,
              source: createdLink.componentAId,
              target: createdLink.componentBId,
              ...(createdLink.comment ? { comment: createdLink.comment } : {}),
            },
          ].sort((left, right) => left.id - right.id),
        };
      });
      await queryClient.invalidateQueries({ queryKey });
    },
  });
}

interface DeleteCompatibilityLinkVariables {
  domainId: number;
  linkId: number;
}

export function useDeleteCompatibilityLinkMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ domainId, linkId }: DeleteCompatibilityLinkVariables) =>
      apiData(
        deleteDomainsByIdCompatibilityByLinkId({
          client,
          path: { id: domainId, linkId },
          throwOnError: true,
        }),
      ),
    onSuccess: async (_response, { domainId, linkId }) => {
      const queryKey = compatibilityKeys.graph(domainId);
      queryClient.setQueryData<GraphResponse>(queryKey, (graph) =>
        graph ? { ...graph, edges: graph.edges.filter((edge) => edge.id !== linkId) } : graph,
      );
      await queryClient.invalidateQueries({ queryKey });
    },
  });
}
