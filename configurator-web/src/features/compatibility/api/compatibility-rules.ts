import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  apiData,
  client,
  deleteDomainsByIdCompatibilityRulesByRuleId,
  getDomainsByIdCompatibilityRules,
  getDomainsByIdCompatibilityRulesByRuleId,
  postDomainsByIdCompatibilityRules,
  putDomainsByIdCompatibilityRulesByRuleId,
  type CompatibilityRuleSet,
  type SaveCompatibilityRuleSetRequest,
} from '@/shared/api';

export const compatibilityRuleKeys = {
  root: (domainId: number | null) => ['domains', domainId, 'compatibility', 'rules'] as const,
  list: (domainId: number | null) => [...compatibilityRuleKeys.root(domainId), 'list'] as const,
  detail: (domainId: number | null, ruleId: number | null) =>
    [...compatibilityRuleKeys.root(domainId), 'detail', ruleId] as const,
};

export function sortCompatibilityRules(rules: ReadonlyArray<CompatibilityRuleSet>) {
  return [...rules].sort((left, right) => left.id - right.id);
}

export async function fetchCompatibilityRules(domainId: number) {
  const rules = await apiData(
    getDomainsByIdCompatibilityRules({ client, path: { id: domainId }, throwOnError: true }),
  );
  return sortCompatibilityRules(rules);
}

export async function fetchCompatibilityRule(domainId: number, ruleId: number) {
  return apiData(
    getDomainsByIdCompatibilityRulesByRuleId({
      client,
      path: { id: domainId, ruleId },
      throwOnError: true,
    }),
  );
}

export function useCompatibilityRulesQuery(domainId: number | null) {
  return useQuery({
    queryKey: compatibilityRuleKeys.list(domainId),
    queryFn: () => (domainId === null ? Promise.resolve([]) : fetchCompatibilityRules(domainId)),
    enabled: domainId !== null,
  });
}

export function useCompatibilityRuleQuery(domainId: number | null, ruleId: number | null) {
  return useQuery({
    queryKey: compatibilityRuleKeys.detail(domainId, ruleId),
    queryFn: () =>
      domainId === null || ruleId === null
        ? Promise.reject(new Error('Domain and rule are required'))
        : fetchCompatibilityRule(domainId, ruleId),
    enabled: domainId !== null && ruleId !== null,
  });
}

interface CreateCompatibilityRuleVariables {
  domainId: number;
  body: SaveCompatibilityRuleSetRequest;
}

export function useCreateCompatibilityRuleMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ domainId, body }: CreateCompatibilityRuleVariables) =>
      apiData(
        postDomainsByIdCompatibilityRules({
          client,
          path: { id: domainId },
          body,
          throwOnError: true,
        }),
      ),
    onSuccess: async (createdRule, { domainId }) => {
      queryClient.setQueryData<Array<CompatibilityRuleSet>>(
        compatibilityRuleKeys.list(domainId),
        (rules = []) => sortCompatibilityRules([...rules, createdRule]),
      );
      queryClient.setQueryData(compatibilityRuleKeys.detail(domainId, createdRule.id), createdRule);
      await queryClient.invalidateQueries({ queryKey: compatibilityRuleKeys.root(domainId) });
    },
  });
}

interface UpdateCompatibilityRuleVariables {
  domainId: number;
  ruleId: number;
  body: SaveCompatibilityRuleSetRequest;
}

export function useUpdateCompatibilityRuleMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ domainId, ruleId, body }: UpdateCompatibilityRuleVariables) =>
      apiData(
        putDomainsByIdCompatibilityRulesByRuleId({
          client,
          path: { id: domainId, ruleId },
          body,
          throwOnError: true,
        }),
      ),
    onSuccess: async (updatedRule, { domainId }) => {
      queryClient.setQueryData<Array<CompatibilityRuleSet>>(
        compatibilityRuleKeys.list(domainId),
        (rules = []) =>
          sortCompatibilityRules(
            rules.map((rule) => (rule.id === updatedRule.id ? updatedRule : rule)),
          ),
      );
      queryClient.setQueryData(compatibilityRuleKeys.detail(domainId, updatedRule.id), updatedRule);
      await queryClient.invalidateQueries({ queryKey: compatibilityRuleKeys.root(domainId) });
    },
  });
}

interface DeleteCompatibilityRuleVariables {
  domainId: number;
  ruleId: number;
}

export function useDeleteCompatibilityRuleMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ domainId, ruleId }: DeleteCompatibilityRuleVariables) =>
      apiData(
        deleteDomainsByIdCompatibilityRulesByRuleId({
          client,
          path: { id: domainId, ruleId },
          throwOnError: true,
        }),
      ),
    onSuccess: async (_response, { domainId, ruleId }) => {
      queryClient.setQueryData<Array<CompatibilityRuleSet>>(
        compatibilityRuleKeys.list(domainId),
        (rules = []) => rules.filter((rule) => rule.id !== ruleId),
      );
      queryClient.removeQueries({
        queryKey: compatibilityRuleKeys.detail(domainId, ruleId),
        exact: true,
      });
      await queryClient.invalidateQueries({ queryKey: compatibilityRuleKeys.root(domainId) });
    },
  });
}
