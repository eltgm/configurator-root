import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import type { PropsWithChildren } from 'react';
import { describe, expect, it } from 'vitest';

import {
  compatibilityKeys,
  fetchCompatibilityGraph,
  useCreateCompatibilityLinkMutation,
  useDeleteCompatibilityLinkMutation,
} from '@/features/compatibility/api/manual-compatibility';
import type { CreateCompatibilityLinkRequest, GraphResponse } from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

const domainId = 7;
const graph: GraphResponse = {
  nodes: [
    { id: 1, name: 'CPU', componentTypeId: 11, componentTypeName: 'Processor' },
    { id: 2, name: 'Board', componentTypeId: 12, componentTypeName: 'Motherboard' },
  ],
  edges: [],
};

function createQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

function createWrapper(queryClient: QueryClient) {
  return function Wrapper({ children }: PropsWithChildren) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe('manual compatibility API', () => {
  it('loads the selected domain graph with a domain-scoped key', async () => {
    let requestedDomainId: string | undefined;
    server.use(
      http.get(`${testApiBaseUrl}/domains/:domainId/compatibility/graph`, ({ params }) => {
        requestedDomainId = String(params['domainId']);
        return HttpResponse.json(graph);
      }),
    );

    expect(await fetchCompatibilityGraph(domainId)).toEqual(graph);
    expect(requestedDomainId).toBe(String(domainId));
    expect(compatibilityKeys.graph(domainId)).toEqual([
      'domains',
      domainId,
      'compatibility',
      'graph',
    ]);
  });

  it('creates a link with the requested payload and adds the authoritative response to cache', async () => {
    let submittedBody: CreateCompatibilityLinkRequest | undefined;
    server.use(
      http.post(`${testApiBaseUrl}/domains/:domainId/compatibility`, async ({ request }) => {
        submittedBody = (await request.json()) as CreateCompatibilityLinkRequest;
        return HttpResponse.json(
          {
            id: 91,
            domainId,
            componentAId: 1,
            componentBId: 2,
            comment: 'AM5',
          },
          { status: 201 },
        );
      }),
    );
    const queryClient = createQueryClient();
    queryClient.setQueryData(compatibilityKeys.graph(domainId), graph);
    const mutation = renderHook(() => useCreateCompatibilityLinkMutation(), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await mutation.result.current.mutateAsync({
        domainId,
        body: { componentAId: 2, componentBId: 1, comment: 'AM5' },
      });
    });

    expect(submittedBody).toEqual({ componentAId: 2, componentBId: 1, comment: 'AM5' });
    expect(
      queryClient.getQueryData<GraphResponse>(compatibilityKeys.graph(domainId))?.edges,
    ).toEqual([{ id: 91, source: 1, target: 2, comment: 'AM5' }]);
    expect(queryClient.getQueryData(compatibilityKeys.graph(8))).toBeUndefined();
  });

  it('removes a link only after a successful domain-scoped delete', async () => {
    let deletedPath: string | undefined;
    server.use(
      http.delete(`${testApiBaseUrl}/domains/:domainId/compatibility/:linkId`, ({ request }) => {
        deletedPath = new URL(request.url).pathname;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const queryClient = createQueryClient();
    queryClient.setQueryData<GraphResponse>(compatibilityKeys.graph(domainId), {
      ...graph,
      edges: [{ id: 91, source: 1, target: 2 }],
    });
    const mutation = renderHook(() => useDeleteCompatibilityLinkMutation(), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await mutation.result.current.mutateAsync({ domainId, linkId: 91 });
    });

    expect(deletedPath).toBe('/api/domains/7/compatibility/91');
    expect(
      queryClient.getQueryData<GraphResponse>(compatibilityKeys.graph(domainId))?.edges,
    ).toEqual([]);
  });

  it('keeps cached links when deletion fails', async () => {
    server.use(
      http.delete(`${testApiBaseUrl}/domains/:domainId/compatibility/:linkId`, () =>
        HttpResponse.json({ message: 'failed' }, { status: 500 }),
      ),
    );
    const cachedGraph: GraphResponse = {
      ...graph,
      edges: [{ id: 91, source: 1, target: 2 }],
    };
    const queryClient = createQueryClient();
    queryClient.setQueryData(compatibilityKeys.graph(domainId), cachedGraph);
    const mutation = renderHook(() => useDeleteCompatibilityLinkMutation(), {
      wrapper: createWrapper(queryClient),
    });

    act(() => mutation.result.current.mutate({ domainId, linkId: 91 }));
    await waitFor(() => expect(mutation.result.current.isError).toBe(true));

    expect(queryClient.getQueryData(compatibilityKeys.graph(domainId))).toEqual(cachedGraph);
  });
});
