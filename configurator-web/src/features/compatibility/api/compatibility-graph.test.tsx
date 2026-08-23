import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import type { PropsWithChildren } from 'react';
import { describe, expect, it } from 'vitest';

import {
  compatibilityKeys,
  fetchCompatibilityGraph,
  useCompatibilityGraphQuery,
} from '@/features/compatibility/api/compatibility-graph';
import type { GraphResponse } from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

const graph: GraphResponse = {
  nodes: [{ id: 1, name: 'CPU', componentTypeId: 11, componentTypeName: 'Processor' }],
  edges: [],
};

function createWrapper() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return function Wrapper({ children }: PropsWithChildren) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe('compatibility graph API', () => {
  it('loads the selected domain graph with a domain-scoped key', async () => {
    let requestedDomainId: string | undefined;
    server.use(
      http.get(`${testApiBaseUrl}/domains/:domainId/compatibility/graph`, ({ params }) => {
        requestedDomainId = String(params['domainId']);
        return HttpResponse.json(graph);
      }),
    );

    expect(await fetchCompatibilityGraph(7)).toEqual(graph);
    expect(requestedDomainId).toBe('7');
    expect(compatibilityKeys.graph(7)).toEqual(['domains', 7, 'compatibility', 'graph']);
    expect(compatibilityKeys.graph(8)).not.toEqual(compatibilityKeys.graph(7));
  });

  it('does not request a graph until a domain is selected', () => {
    const result = renderHook(() => useCompatibilityGraphQuery(null), {
      wrapper: createWrapper(),
    });

    expect(result.result.current.fetchStatus).toBe('idle');
    expect(result.result.current.data).toBeUndefined();
  });

  it('exposes a graph through the shared query hook', async () => {
    server.use(
      http.get(`${testApiBaseUrl}/domains/7/compatibility/graph`, () => HttpResponse.json(graph)),
    );
    const result = renderHook(() => useCompatibilityGraphQuery(7), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.result.current.isSuccess).toBe(true));
    expect(result.result.current.data).toEqual(graph);
  });
});
