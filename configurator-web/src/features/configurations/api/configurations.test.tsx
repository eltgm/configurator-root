import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import type { PropsWithChildren } from 'react';
import { describe, expect, it, vi } from 'vitest';

import {
  configurationKeys,
  fetchConfigurations,
  useConfigurationsQuery,
  useCreateConfigurationMutation,
} from '@/features/configurations/api/configurations';
import type { Configuration, ConfigurationPage } from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

const configuration: Configuration = {
  id: 41,
  domainId: 7,
  name: 'Home PC',
  createdAt: '2026-08-23T10:00:00Z',
  components: [],
};

function createTestContext() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const wrapper = ({ children }: PropsWithChildren) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { client, wrapper };
}

describe('configurations API', () => {
  it('loads the requested zero-based server page', async () => {
    let search = '';
    const response: ConfigurationPage = {
      items: [configuration],
      page: 2,
      size: 10,
      totalItems: 24,
    };
    server.use(
      http.get(`${testApiBaseUrl}/domains/7/configurations`, ({ request }) => {
        search = new URL(request.url).search;
        return HttpResponse.json(response);
      }),
    );

    await expect(fetchConfigurations(7, 2, 10)).resolves.toEqual(response);
    expect(search).toBe('?page=2&size=10');
  });

  it('keeps list keys isolated by domain and page and stays idle without a domain', () => {
    expect(configurationKeys.list(7, 0, 10)).not.toEqual(configurationKeys.list(8, 0, 10));
    expect(configurationKeys.list(7, 0, 10)).not.toEqual(configurationKeys.list(7, 1, 10));
    const { wrapper } = createTestContext();
    const query = renderHook(() => useConfigurationsQuery(null, 0), { wrapper });

    expect(query.result.current.fetchStatus).toBe('idle');
  });

  it('creates a configuration and invalidates only its domain family', async () => {
    let requestBody: unknown;
    server.use(
      http.post(`${testApiBaseUrl}/domains/7/configurations`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(configuration, { status: 201 });
      }),
    );
    const { client, wrapper } = createTestContext();
    const invalidate = vi.spyOn(client, 'invalidateQueries');
    const mutation = renderHook(() => useCreateConfigurationMutation(), { wrapper });

    await act(async () => {
      await mutation.result.current.mutateAsync({
        domainId: 7,
        body: { name: 'Home PC', componentIds: [3, 9] },
      });
    });

    expect(requestBody).toEqual({ name: 'Home PC', componentIds: [3, 9] });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: configurationKeys.byDomain(7) });
    expect(invalidate).not.toHaveBeenCalledWith({ queryKey: configurationKeys.byDomain(8) });
  });

  it('exposes a structured create error without retrying', async () => {
    let requests = 0;
    server.use(
      http.post(`${testApiBaseUrl}/domains/7/configurations`, () => {
        requests += 1;
        return HttpResponse.json(
          {
            timestamp: '2026-08-23T12:00:00Z',
            status: 409,
            error: 'Conflict',
            code: 'CONFIGURATION_CONFLICT',
            message: 'Assembly changed',
            path: '/domains/7/configurations',
            details: [],
          },
          { status: 409 },
        );
      }),
    );
    const { wrapper } = createTestContext();
    const mutation = renderHook(() => useCreateConfigurationMutation(), { wrapper });

    act(() => {
      mutation.result.current.mutate({
        domainId: 7,
        body: { name: 'Home PC', componentIds: [3, 9] },
      });
    });
    await waitFor(() => expect(mutation.result.current.isError).toBe(true));

    expect(requests).toBe(1);
    expect(mutation.result.current.error).toMatchObject({ status: 409 });
  });
});
