import { Button, Group, Pagination, Progress, Stack, Tabs, Text } from '@mantine/core';
import { IconAssembly } from '@tabler/icons-react';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router-dom';

import {
  configurationListPageSize,
  type ConfigurationInventoryFilter,
  useConfigurationsQuery,
} from '@/features/configurations/api/configurations';
import { getConfigurationCopyInitialValues } from '@/features/configurations/model/configuration-operations';
import { useConfigurationExport } from '@/features/configurations/model/useConfigurationExport';
import { ConfigurationList } from '@/features/configurations/ui/ConfigurationList';
import { CreateConfigurationModal } from '@/features/configurations/ui/CreateConfigurationModal';
import { DeleteConfigurationModal } from '@/features/configurations/ui/DeleteConfigurationModal';
import { useDomainContext } from '@/features/domains/model/domain-context';
import type { Configuration } from '@/shared/api';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { EmptyState, ErrorState, LoadingState, PageHeader } from '@/shared/ui';

function ConfigurationPageContent({ domainId }: { domainId: number }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [inventoryFilter, setInventoryFilter] = useState<ConfigurationInventoryFilter>('all');
  const [copyingConfiguration, setCopyingConfiguration] = useState<Configuration>();
  const [deletingConfiguration, setDeletingConfiguration] = useState<Configuration>();
  const { exportConfiguration, exportingConfigurationId } = useConfigurationExport();
  const configurationsQuery = useConfigurationsQuery(
    domainId,
    page,
    configurationListPageSize,
    inventoryFilter,
  );
  const totalPages = Math.ceil(
    (configurationsQuery.data?.totalItems ?? 0) / configurationListPageSize,
  );
  const copyInitialValues = useMemo(
    () =>
      copyingConfiguration
        ? getConfigurationCopyInitialValues(copyingConfiguration, t('configurations.copy.suffix'))
        : undefined,
    [copyingConfiguration, t],
  );

  return (
    <Stack gap="lg">
      <Tabs
        value={inventoryFilter}
        onChange={(value) => {
          if (value === 'all' || value === 'tracked' || value === 'untracked') {
            setInventoryFilter(value);
            setPage(0);
          }
        }}
      >
        <Tabs.List aria-label={t('configurations.inventory.filterLabel')}>
          <Tabs.Tab value="all">{t('configurations.inventory.all')}</Tabs.Tab>
          <Tabs.Tab value="tracked">{t('configurations.inventory.tracked')}</Tabs.Tab>
          <Tabs.Tab value="untracked">{t('configurations.inventory.untracked')}</Tabs.Tab>
        </Tabs.List>
      </Tabs>
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
          <ConfigurationList
            configurations={configurationsQuery.data.items}
            exportingConfigurationId={exportingConfigurationId}
            onCopy={setCopyingConfiguration}
            onExport={(configuration) => void exportConfiguration(configuration)}
            onDelete={setDeletingConfiguration}
          />
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
      {copyingConfiguration ? (
        <CreateConfigurationModal
          opened
          mode="copy"
          domainId={copyingConfiguration.domainId}
          trackInventory={copyingConfiguration.trackInventory}
          componentItems={copyingConfiguration.components.map((component) => ({
            componentId: component.id,
            quantity: component.quantity,
          }))}
          components={copyingConfiguration.components.map((component) => ({
            id: component.id,
            name: component.name,
            typeName: component.componentTypeName,
            ...(component.brand ? { brand: component.brand } : {}),
            archived: component.archived,
            quantity: component.quantity,
            availableQuantity: component.availableQuantity,
          }))}
          initialValues={copyInitialValues}
          onClose={() => setCopyingConfiguration(undefined)}
          onSaved={(copy) => void navigate(`/configurations/${copy.id}`)}
        />
      ) : null}
      <DeleteConfigurationModal
        configuration={deletingConfiguration}
        onClose={() => setDeletingConfiguration(undefined)}
        onDeleted={() => {
          setDeletingConfiguration(undefined);
          if ((configurationsQuery.data?.items.length ?? 0) === 1 && page > 0) {
            setPage((currentPage) => Math.max(0, currentPage - 1));
          }
        }}
      />
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
