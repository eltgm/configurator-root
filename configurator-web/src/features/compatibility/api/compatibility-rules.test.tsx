import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import type { PropsWithChildren } from 'react';
import { describe, expect, it } from 'vitest';

import {
  compatibilityRuleKeys,
  fetchCompatibilityRule,
  fetchCompatibilityRules,
  useCreateCompatibilityRuleMutation,
  useDeleteCompatibilityRuleMutation,
  useUpdateCompatibilityRuleMutation,
} from '@/features/compatibility/api/compatibility-rules';
import type { CompatibilityRuleSet, SaveCompatibilityRuleSetRequest } from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

const domainId = 7;
const body: SaveCompatibilityRuleSetRequest = {
  name: 'Socket match',
  componentTypeAId: 11,
  componentTypeBId: 12,
  enabled: true,
  conditions: [
    {
      leftAttributeDefinitionId: 101,
      operator: 'EQUALS',
      rightAttributeDefinitionId: 102,
      orderIndex: 0,
    },
  ],
};
const rule: CompatibilityRuleSet = {
  id: 91,
  domainId,
  ...body,
  conditions: [
    {
      id: 501,
      ruleSetId: 91,
      ...body.conditions[0]!,
      orderIndex: 0,
      createdAt: '2026-08-23T12:00:00',
    },
  ],
  createdAt: '2026-08-23T12:00:00',
};

function createQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

function createWrapper(queryClient: QueryClient) {
  return function Wrapper({ children }: PropsWithChildren) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe('compatibility rules API', () => {
  it('loads list and detail through domain-scoped endpoints and stable keys', async () => {
    server.use(
      http.get(`${testApiBaseUrl}/domains/:domainId/compatibility/rules`, () =>
        HttpResponse.json([{ ...rule, id: 92 }, rule]),
      ),
      http.get(`${testApiBaseUrl}/domains/:domainId/compatibility/rules/:ruleId`, () =>
        HttpResponse.json(rule),
      ),
    );

    expect((await fetchCompatibilityRules(domainId)).map((item) => item.id)).toEqual([91, 92]);
    expect(await fetchCompatibilityRule(domainId, rule.id)).toEqual(rule);
    expect(compatibilityRuleKeys.list(domainId)).not.toEqual(compatibilityRuleKeys.list(8));
    expect(compatibilityRuleKeys.detail(domainId, rule.id)).toEqual([
      'domains',
      domainId,
      'compatibility',
      'rules',
      'detail',
      rule.id,
    ]);
  });

  it('creates a rule and synchronizes only current domain caches', async () => {
    let submitted: SaveCompatibilityRuleSetRequest | undefined;
    server.use(
      http.post(`${testApiBaseUrl}/domains/:domainId/compatibility/rules`, async ({ request }) => {
        submitted = (await request.json()) as SaveCompatibilityRuleSetRequest;
        return HttpResponse.json(rule, { status: 201 });
      }),
    );
    const queryClient = createQueryClient();
    queryClient.setQueryData(compatibilityRuleKeys.list(domainId), []);
    queryClient.setQueryData(compatibilityRuleKeys.list(8), [{ ...rule, domainId: 8 }]);
    const mutation = renderHook(() => useCreateCompatibilityRuleMutation(), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => mutation.result.current.mutateAsync({ domainId, body }));

    expect(submitted).toEqual(body);
    expect(queryClient.getQueryData(compatibilityRuleKeys.list(domainId))).toEqual([rule]);
    expect(queryClient.getQueryData(compatibilityRuleKeys.detail(domainId, rule.id))).toEqual(rule);
    expect(
      queryClient.getQueryData<Array<CompatibilityRuleSet>>(compatibilityRuleKeys.list(8)),
    ).toHaveLength(1);
  });

  it('updates list and detail with the authoritative replacement', async () => {
    const updated = { ...rule, name: 'Updated', enabled: false };
    server.use(
      http.put(`${testApiBaseUrl}/domains/:domainId/compatibility/rules/:ruleId`, () =>
        HttpResponse.json(updated),
      ),
    );
    const queryClient = createQueryClient();
    queryClient.setQueryData(compatibilityRuleKeys.list(domainId), [rule]);
    queryClient.setQueryData(compatibilityRuleKeys.detail(domainId, rule.id), rule);
    const mutation = renderHook(() => useUpdateCompatibilityRuleMutation(), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => mutation.result.current.mutateAsync({ domainId, ruleId: rule.id, body }));

    expect(queryClient.getQueryData(compatibilityRuleKeys.list(domainId))).toEqual([updated]);
    expect(queryClient.getQueryData(compatibilityRuleKeys.detail(domainId, rule.id))).toEqual(
      updated,
    );
  });

  it('deletes only after success and keeps cache unchanged on failure', async () => {
    let shouldFail = true;
    server.use(
      http.delete(`${testApiBaseUrl}/domains/:domainId/compatibility/rules/:ruleId`, () =>
        shouldFail
          ? HttpResponse.json({ message: 'failed' }, { status: 500 })
          : new HttpResponse(null, { status: 204 }),
      ),
    );
    const queryClient = createQueryClient();
    queryClient.setQueryData(compatibilityRuleKeys.list(domainId), [rule]);
    queryClient.setQueryData(compatibilityRuleKeys.detail(domainId, rule.id), rule);
    const mutation = renderHook(() => useDeleteCompatibilityRuleMutation(), {
      wrapper: createWrapper(queryClient),
    });

    act(() => mutation.result.current.mutate({ domainId, ruleId: rule.id }));
    await waitFor(() => expect(mutation.result.current.isError).toBe(true));
    expect(queryClient.getQueryData(compatibilityRuleKeys.list(domainId))).toEqual([rule]);

    shouldFail = false;
    await act(async () => mutation.result.current.mutateAsync({ domainId, ruleId: rule.id }));
    expect(queryClient.getQueryData(compatibilityRuleKeys.list(domainId))).toEqual([]);
    expect(
      queryClient.getQueryData(compatibilityRuleKeys.detail(domainId, rule.id)),
    ).toBeUndefined();
  });
});
