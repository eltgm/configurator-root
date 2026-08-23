import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import type { PropsWithChildren } from 'react';
import { describe, expect, it } from 'vitest';

import {
  configuratorCompatibilityKeys,
  fetchBatchCompatibility,
  fetchCompatibilityIntersection,
  fetchDirectCompatibility,
  useBatchCompatibilityQuery,
  useCompatibilityIntersectionQuery,
  useDirectCompatibilityQuery,
} from '@/features/configurator/api/configurator-compatibility';
import type {
  ConfiguratorBatchSearchResponse,
  ConfiguratorIntersectionResponse,
  ConfiguratorResponse,
} from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

const directResponse: ConfiguratorResponse = {
  baseComponentId: 1,
  compatibleByType: [],
};
const batchResponse: ConfiguratorBatchSearchResponse = {
  results: [directResponse],
};
const intersectionResponse: ConfiguratorIntersectionResponse = {
  componentIds: [1, 2],
  compatibleByType: [],
};

function createWrapper() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return function Wrapper({ children }: PropsWithChildren) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
  };
}

describe('configurator compatibility API', () => {
  it('sends direct-only single, batch and intersection requests', async () => {
    const requests: Array<{ path: string; payload: unknown }> = [];
    server.use(
      http.get(`${testApiBaseUrl}/domains/:domainId/configurator/compatible`, ({ request }) => {
        const url = new URL(request.url);
        requests.push({
          path: url.pathname,
          payload: Object.fromEntries(url.searchParams.entries()),
        });
        return HttpResponse.json(directResponse);
      }),
      http.post(
        `${testApiBaseUrl}/domains/:domainId/configurator/compatible/search`,
        async ({ request }) => {
          requests.push({ path: new URL(request.url).pathname, payload: await request.json() });
          return HttpResponse.json(batchResponse);
        },
      ),
      http.post(
        `${testApiBaseUrl}/domains/:domainId/configurator/compatible/intersection`,
        async ({ request }) => {
          requests.push({ path: new URL(request.url).pathname, payload: await request.json() });
          return HttpResponse.json(intersectionResponse);
        },
      ),
    );

    await expect(fetchDirectCompatibility(7, 1)).resolves.toEqual(directResponse);
    await expect(fetchBatchCompatibility(7, [1, 2])).resolves.toEqual(batchResponse);
    await expect(fetchCompatibilityIntersection(7, [1, 2])).resolves.toEqual(intersectionResponse);
    expect(requests).toEqual([
      {
        path: '/api/domains/7/configurator/compatible',
        payload: { componentId: '1', includeTransitive: 'false' },
      },
      {
        path: '/api/domains/7/configurator/compatible/search',
        payload: { componentIds: [1, 2], includeTransitive: false },
      },
      {
        path: '/api/domains/7/configurator/compatible/intersection',
        payload: { componentIds: [1, 2], includeTransitive: false },
      },
    ]);
  });

  it('keeps keys domain-, mode- and order-scoped', () => {
    expect(configuratorCompatibilityKeys.batch(7, [1, 2])).not.toEqual(
      configuratorCompatibilityKeys.batch(7, [2, 1]),
    );
    expect(configuratorCompatibilityKeys.batch(7, [1, 2])).not.toEqual(
      configuratorCompatibilityKeys.intersection(7, [1, 2]),
    );
    expect(configuratorCompatibilityKeys.direct(7, 1)).not.toEqual(
      configuratorCompatibilityKeys.direct(8, 1),
    );
  });

  it('keeps hooks idle until their required input is available', () => {
    const direct = renderHook(() => useDirectCompatibilityQuery(null, null), {
      wrapper: createWrapper(),
    });
    const batch = renderHook(() => useBatchCompatibilityQuery(7, []), {
      wrapper: createWrapper(),
    });
    const intersection = renderHook(() => useCompatibilityIntersectionQuery(7, [1]), {
      wrapper: createWrapper(),
    });

    expect(direct.result.current.fetchStatus).toBe('idle');
    expect(batch.result.current.fetchStatus).toBe('idle');
    expect(intersection.result.current.fetchStatus).toBe('idle');
  });

  it('exposes successful and failed requests through query hooks', async () => {
    server.use(
      http.get(`${testApiBaseUrl}/domains/7/configurator/compatible`, () =>
        HttpResponse.json(directResponse),
      ),
      http.post(`${testApiBaseUrl}/domains/7/configurator/compatible/search`, () =>
        HttpResponse.json(
          {
            timestamp: '2026-08-23T12:00:00Z',
            status: 400,
            error: 'Bad Request',
            code: 'VALIDATION_ERROR',
            message: 'Invalid draft',
            path: '/domains/7/configurator/compatible/search',
            details: [],
          },
          { status: 400 },
        ),
      ),
    );
    const direct = renderHook(() => useDirectCompatibilityQuery(7, 1), {
      wrapper: createWrapper(),
    });
    const batch = renderHook(() => useBatchCompatibilityQuery(7, [1, 2]), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(direct.result.current.isSuccess).toBe(true));
    await waitFor(() => expect(batch.result.current.isError).toBe(true));
    expect(direct.result.current.data).toEqual(directResponse);
    expect(batch.result.current.error).toMatchObject({ status: 400 });
  });
});
