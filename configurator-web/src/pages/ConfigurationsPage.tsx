import { Button, Group, Pagination, Progress, Stack, Text } from '@mantine/core';
import { IconAssembly } from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import {
  configurationListPageSize,
  useConfigurationsQuery,
} from '@/features/configurations/api/configurations';
import { ConfigurationList } from '@/features/configurations/ui/ConfigurationList';
import { useDomainContext } from '@/features/domains/model/domain-context';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { EmptyState, ErrorState, LoadingState, PageHeader } from '@/shared/ui';

function ConfigurationPageContent({ domainId }: { domainId: number }) {
  const { t } = useTranslation();
  const [page, setPage] = useState(0);
  const configurationsQuery = useConfigurationsQuery(domainId, page);
  const totalPages = Math.ceil(
    (configurationsQuery.data?.totalItems ?? 0) / configurationListPageSize,
  );

  return (
    <Stack gap="lg">
      {configurationsQuery.isFetching && !configurationsQuery.isPending ? (
        <Progress
          value={100}
          size="xs"
          animated
          aria-label={t('configurations.states.refreshing')}
        />
      ) : null}
      {configurationsQuery.isPending ? (
        <LoadingState label={t('configurations.states.loading')} />
      ) : null}
      {configurationsQuery.error && !configurationsQuery.data ? (
        <ErrorState
          error={configurationsQuery.error}
          onRetry={() => void configurationsQuery.refetch()}
        />
      ) : null}
      {!configurationsQuery.isPending &&
      !configurationsQuery.error &&
      configurationsQuery.data?.items.length === 0 ? (
        <EmptyState
          title={t('configurations.states.emptyTitle')}
          description={t('configurations.states.emptyDescription')}
          action={
            <Button component={Link} to="/configurator">
              {t('configurations.actions.openConfigurator')}
            </Button>
          }
        />
      ) : null}
      {configurationsQuery.data?.items.length ? (
        <>
          <Text size="sm" c="dimmed" aria-live="polite">
            {t('configurations.states.total', { count: configurationsQuery.data.totalItems })}
          </Text>
          <ConfigurationList configurations={configurationsQuery.data.items} />
        </>
      ) : null}
      {totalPages > 1 ? (
        <Group justify="center">
          <Pagination
            value={page + 1}
            total={totalPages}
            onChange={(nextPage) => setPage(nextPage - 1)}
          />
        </Group>
      ) : null}
    </Stack>
  );
}

export function ConfigurationsPage() {
  const { t } = useTranslation();
  const { selectedDomain, selectedDomainId } = useDomainContext();
  const title = t('configurations.page.title');
  useDocumentTitle(title, t('app.name'));

  return (
    <Stack gap="xl">
      <PageHeader
        title={title}
        description={t('configurations.page.description', { domain: selectedDomain?.name ?? '' })}
        actions={
          <Button
            component={Link}
            to="/configurator"
            leftSection={<IconAssembly size={17} aria-hidden="true" />}
          >
            {t('configurations.actions.openConfigurator')}
          </Button>
        }
      />
      {selectedDomainId === null ? null : (
        <ConfigurationPageContent key={selectedDomainId} domainId={selectedDomainId} />
      )}
    </Stack>
  );
}
