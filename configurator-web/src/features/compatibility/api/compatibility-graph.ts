import { useQuery } from '@tanstack/react-query';

import {
  apiData,
  client,
  getDomainsByIdCompatibilityGraph,
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
