import {
  Badge,
  Button,
  Group,
  Modal,
  Paper,
  SegmentedControl,
  Stack,
  Text,
  TextInput,
} from '@mantine/core';
import { IconListCheck, IconPlus, IconSearch } from '@tabler/icons-react';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import {
  useCompatibilityRulesQuery,
  useDeleteCompatibilityRuleMutation,
  useUpdateCompatibilityRuleMutation,
} from '@/features/compatibility/api/compatibility-rules';
import {
  filterCompatibilityRules,
  toSaveCompatibilityRuleRequestFromRule,
  type CompatibilityRuleStatusFilter,
} from '@/features/compatibility/model/compatibility-rules';
import { CompatibilityRuleList } from '@/features/compatibility/ui/CompatibilityRuleList';
import { useComponentTypesQuery } from '@/features/component-types/api/component-types';
import { useDomainContext } from '@/features/domains/model/domain-context';
import type { CompatibilityRuleSet } from '@/shared/api';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { showSuccessNotification } from '@/shared/notifications/notifications';
import { EmptyState, ErrorState, LoadingState, PageHeader } from '@/shared/ui';

import classes from './compatibility-rules-page.module.css';

interface CompatibilityRulesContentProps {
  domainId: number;
  domainName: string;
}

function CompatibilityRulesContent({ domainId, domainName }: CompatibilityRulesContentProps) {
  const { t } = useTranslation();
  const rulesQuery = useCompatibilityRulesQuery(domainId);
  const componentTypesQuery = useComponentTypesQuery(domainId);
  const rules = useMemo(() => rulesQuery.data ?? [], [rulesQuery.data]);
  const componentTypes = useMemo(() => componentTypesQuery.data ?? [], [componentTypesQuery.data]);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<CompatibilityRuleStatusFilter>('all');
  const [togglingRuleId, setTogglingRuleId] = useState<number>();
  const [deletingRule, setDeletingRule] = useState<CompatibilityRuleSet>();
  const updateRule = useUpdateCompatibilityRuleMutation();
  const deleteRule = useDeleteCompatibilityRuleMutation();
  const filteredRules = useMemo(
    () => filterCompatibilityRules(rules, componentTypes, search, status),
    [componentTypes, rules, search, status],
  );
  const enabledCount = rules.filter((rule) => rule.enabled).length;
  const isPending = rulesQuery.isPending || componentTypesQuery.isPending;
  const loadError = rulesQuery.error ?? componentTypesQuery.error;

  const toggleRule = async (rule: CompatibilityRuleSet, enabled: boolean) => {
    setTogglingRuleId(rule.id);
    try {
      await updateRule.mutateAsync({
        domainId,
        ruleId: rule.id,
        body: toSaveCompatibilityRuleRequestFromRule(rule, enabled),
      });
      showSuccessNotification(
        t(
          enabled
            ? 'compatibilityRules.notifications.enabled'
            : 'compatibilityRules.notifications.disabled',
        ),
      );
    } catch {
      // The global mutation policy presents the structured API error.
    } finally {
      setTogglingRuleId(undefined);
    }
  };

  const confirmDelete = async () => {
    if (!deletingRule) {
      return;
    }
    try {
      await deleteRule.mutateAsync({ domainId, ruleId: deletingRule.id });
      showSuccessNotification(t('compatibilityRules.notifications.deleted'));
      setDeletingRule(undefined);
    } catch {
      // The global mutation policy presents the structured API error.
    }
  };

  return (
    <Stack gap="xl">
      <PageHeader
        title={t('compatibilityRules.page.title')}
        description={t('compatibilityRules.page.description', { domain: domainName })}
        actions={
          <Button
            component={Link}
            to="/settings/compatibility/rules/new"
            leftSection={<IconPlus size={18} />}
          >
            {t('compatibilityRules.actions.create')}
          </Button>
        }
      />

      {rulesQuery.data ? (
        <Group gap="sm" role="status" aria-live="polite">
          <Badge variant="light" size="lg">
            {t('compatibilityRules.summary.total', { count: rules.length })}
          </Badge>
          <Badge variant="light" size="lg" color="green">
            {t('compatibilityRules.summary.enabled', { count: enabledCount })}
          </Badge>
          <Badge variant="light" size="lg" color="gray">
            {t('compatibilityRules.summary.disabled', { count: rules.length - enabledCount })}
          </Badge>
        </Group>
      ) : null}

      {isPending ? <LoadingState label={t('compatibilityRules.states.loading')} /> : null}
      {loadError && !isPending ? (
        <ErrorState
          error={loadError}
          onRetry={() => {
            void Promise.all([rulesQuery.refetch(), componentTypesQuery.refetch()]);
          }}
        />
      ) : null}

      {!isPending && !loadError && rules.length === 0 ? (
        <EmptyState
          icon={<IconListCheck size={26} stroke={1.7} />}
          title={t('compatibilityRules.states.emptyTitle')}
          description={t('compatibilityRules.states.emptyDescription')}
          action={
            <Button
              component={Link}
              to="/settings/compatibility/rules/new"
              leftSection={<IconPlus size={18} />}
            >
              {t('compatibilityRules.actions.createFirst')}
            </Button>
          }
        />
      ) : null}

      {!isPending && !loadError && rules.length > 0 ? (
        <Stack gap="md">
          <Paper className={classes.filters} p="md" radius="md" withBorder>
            <TextInput
              className={classes.search}
              label={t('compatibilityRules.filters.searchLabel')}
              placeholder={t('compatibilityRules.filters.searchPlaceholder')}
              leftSection={<IconSearch size={17} />}
              value={search}
              onChange={(event) => setSearch(event.currentTarget.value)}
            />
            <Stack gap={5}>
              <Text size="sm" fw={500}>
                {t('compatibilityRules.filters.statusLabel')}
              </Text>
              <SegmentedControl
                value={status}
                onChange={setStatus}
                data={[
                  { value: 'all', label: t('compatibilityRules.filters.all') },
                  { value: 'enabled', label: t('compatibilityRules.filters.enabled') },
                  { value: 'disabled', label: t('compatibilityRules.filters.disabled') },
                ]}
              />
            </Stack>
          </Paper>

          {filteredRules.length > 0 ? (
            <CompatibilityRuleList
              rules={filteredRules}
              componentTypes={componentTypes}
              togglingRuleId={togglingRuleId}
              onToggle={(rule, enabled) => void toggleRule(rule, enabled)}
              onDelete={setDeletingRule}
            />
          ) : (
            <EmptyState
              title={t('compatibilityRules.states.noResultsTitle')}
              description={t('compatibilityRules.states.noResultsDescription')}
              action={
                <Button
                  variant="light"
                  onClick={() => {
                    setSearch('');
                    setStatus('all');
                  }}
                >
                  {t('compatibilityRules.actions.clearFilters')}
                </Button>
              }
            />
          )}
        </Stack>
      ) : null}

      <Modal
        opened={Boolean(deletingRule)}
        onClose={() => {
          if (!deleteRule.isPending) {
            setDeletingRule(undefined);
          }
        }}
        title={t('compatibilityRules.delete.title')}
        centered
        closeOnClickOutside={!deleteRule.isPending}
        closeOnEscape={!deleteRule.isPending}
      >
        <Stack gap="md">
          <Text>
            {t('compatibilityRules.delete.description', { name: deletingRule?.name ?? '' })}
          </Text>
          <Text size="sm" c="red">
            {t('compatibilityRules.delete.warning')}
          </Text>
          <Group justify="flex-end">
            <Button
              variant="default"
              disabled={deleteRule.isPending}
              onClick={() => setDeletingRule(undefined)}
            >
              {t('common.cancel')}
            </Button>
            <Button color="red" loading={deleteRule.isPending} onClick={() => void confirmDelete()}>
              {t('compatibilityRules.actions.delete')}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}

export function CompatibilityRulesPage() {
  const { t } = useTranslation();
  const { selectedDomainId, selectedDomain } = useDomainContext();
  const title = t('compatibilityRules.page.title');
  useDocumentTitle(title, t('app.name'));

  if (selectedDomainId === null) {
    return null;
  }

  return (
    <CompatibilityRulesContent
      key={selectedDomainId}
      domainId={selectedDomainId}
      domainName={selectedDomain?.name ?? ''}
    />
  );
}
