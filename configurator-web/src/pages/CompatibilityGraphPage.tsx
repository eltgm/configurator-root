import { Alert, Badge, Button, Group, Stack, Text } from '@mantine/core';
import { IconCirclesRelation, IconInfoCircle, IconSettings } from '@tabler/icons-react';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import { useCompatibilityGraphQuery } from '@/features/compatibility/api/compatibility-graph';
import {
  buildCompatibilityGraphIndex,
  countIsolatedCompatibilityNodes,
} from '@/features/compatibility/model/compatibility-graph';
import { CompatibilityGraphCanvas } from '@/features/compatibility/ui/CompatibilityGraphCanvas';
import { useDomainContext } from '@/features/domains/model/domain-context';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { EmptyState, ErrorState, LoadingState, PageHeader } from '@/shared/ui';

interface CompatibilityGraphContentProps {
  domainId: number;
  domainName: string;
}

function CompatibilityGraphContent({ domainId, domainName }: CompatibilityGraphContentProps) {
  const { t } = useTranslation();
  const graphQuery = useCompatibilityGraphQuery(domainId);
  const index = useMemo(
    () => (graphQuery.data ? buildCompatibilityGraphIndex(graphQuery.data) : undefined),
    [graphQuery.data],
  );
  const isolatedCount = index ? countIsolatedCompatibilityNodes(index) : 0;
  const graphVersion = useMemo(
    () => (graphQuery.data ? JSON.stringify(graphQuery.data) : ''),
    [graphQuery.data],
  );

  return (
    <Stack gap="xl">
      <PageHeader
        title={t('compatibilityGraph.page.title')}
        description={t('compatibilityGraph.page.description', { domain: domainName })}
        actions={
          <Button
            component={Link}
            to="/settings/compatibility/manual"
            variant="light"
            leftSection={<IconSettings size={18} />}
          >
            {t('compatibilityGraph.actions.manage')}
          </Button>
        }
      />

      <Alert icon={<IconInfoCircle size={18} />} title={t('compatibilityGraph.scope.title')}>
        {t('compatibilityGraph.scope.description')}
      </Alert>

      {index ? (
        <Group gap="sm" role="status" aria-live="polite">
          <Badge variant="light" size="lg">
            {t('compatibilityGraph.summary.components', { count: index.nodes.length })}
          </Badge>
          <Badge variant="light" size="lg">
            {t('compatibilityGraph.summary.links', { count: index.edges.length })}
          </Badge>
          <Badge variant="light" size="lg" color={isolatedCount > 0 ? 'orange' : 'gray'}>
            {t('compatibilityGraph.summary.isolated', { count: isolatedCount })}
          </Badge>
          {graphQuery.isFetching ? (
            <Text size="xs" c="dimmed">
              {t('compatibilityGraph.states.refreshing')}
            </Text>
          ) : null}
        </Group>
      ) : null}

      {graphQuery.isPending ? (
        <LoadingState label={t('compatibilityGraph.states.loading')} />
      ) : null}
      {graphQuery.error && !index ? (
        <ErrorState error={graphQuery.error} onRetry={() => void graphQuery.refetch()} />
      ) : null}
      {index && index.nodes.length === 0 ? (
        <EmptyState
          icon={<IconCirclesRelation size={26} stroke={1.7} />}
          title={t('compatibilityGraph.states.emptyTitle')}
          description={t('compatibilityGraph.states.emptyDescription')}
          action={
            <Button component={Link} to="/components/new">
              {t('compatibilityGraph.actions.createComponent')}
            </Button>
          }
        />
      ) : null}
      {index && index.nodes.length > 0 ? (
        <CompatibilityGraphCanvas key={graphVersion} index={index} />
      ) : null}
    </Stack>
  );
}

export function CompatibilityGraphPage() {
  const { t } = useTranslation();
  const { selectedDomainId, selectedDomain } = useDomainContext();
  const title = t('compatibilityGraph.page.title');
  useDocumentTitle(title, t('app.name'));

  if (selectedDomainId === null) {
    return null;
  }

  return (
    <CompatibilityGraphContent
      key={selectedDomainId}
      domainId={selectedDomainId}
      domainName={selectedDomain?.name ?? ''}
    />
  );
}
