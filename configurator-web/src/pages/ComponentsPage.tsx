import {
  Button,
  Group,
  Modal,
  Pagination,
  Paper,
  Progress,
  SegmentedControl,
  Select,
  Stack,
  Text,
  TextInput,
} from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { IconCards, IconList, IconPlus, IconSearch } from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import { useComponentTypesQuery } from '@/features/component-types/api/component-types';
import {
  componentCatalogPageSize,
  useArchiveComponentMutation,
  useComponentsQuery,
  useRestoreComponentMutation,
} from '@/features/components/api/components';
import {
  readComponentCatalogView,
  saveComponentCatalogView,
  type ComponentCatalogView,
} from '@/features/components/model/catalog-preferences';
import { ComponentCatalogContent } from '@/features/components/ui/ComponentCatalogContent';
import { useDomainContext } from '@/features/domains/model/domain-context';
import type { Component } from '@/shared/api';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { showSuccessNotification } from '@/shared/notifications/notifications';
import { EmptyState, ErrorState, LoadingState, PageHeader } from '@/shared/ui';

import classes from './components-page.module.css';

export function ComponentsPage() {
  const { t } = useTranslation();
  const { selectedDomainId, selectedDomain } = useDomainContext();
  const [search, setSearch] = useState('');
  const [debouncedSearch] = useDebouncedValue(search, 300);
  const [requestedTypeId, setRequestedTypeId] = useState<number>();
  const [archived, setArchived] = useState(false);
  const [page, setPage] = useState(0);
  const [view, setView] = useState<ComponentCatalogView>(readComponentCatalogView);
  const [componentToArchive, setComponentToArchive] = useState<Component>();
  const componentTypesQuery = useComponentTypesQuery(selectedDomainId);
  const componentTypes = componentTypesQuery.data ?? [];
  const componentTypeId = componentTypes.some((type) => type.id === requestedTypeId)
    ? requestedTypeId
    : undefined;
  const catalogQuery = useComponentsQuery(selectedDomainId, {
    ...(componentTypeId === undefined ? {} : { componentTypeId }),
    name: debouncedSearch,
    archived,
    page,
    size: componentCatalogPageSize,
  });
  const archiveComponent = useArchiveComponentMutation();
  const restoreComponent = useRestoreComponentMutation();
  const title = t('components.page.title');
  useDocumentTitle(title, t('app.name'));

  const changeView = (nextView: string) => {
    const normalizedView: ComponentCatalogView = nextView === 'table' ? 'table' : 'cards';
    setView(normalizedView);
    saveComponentCatalogView(normalizedView);
  };

  const changeArchiveMode = (nextMode: string) => {
    setArchived(nextMode === 'archive');
    setPage(0);
  };

  const resetFilters = () => {
    setSearch('');
    setRequestedTypeId(undefined);
    setPage(0);
  };

  const confirmArchive = async () => {
    if (!componentToArchive || selectedDomainId === null) {
      return;
    }
    try {
      await archiveComponent.mutateAsync({ domainId: selectedDomainId, id: componentToArchive.id });
      showSuccessNotification(t('components.notifications.archived'));
      if (catalogQuery.data?.items.length === 1 && page > 0) {
        setPage(page - 1);
      }
      setComponentToArchive(undefined);
    } catch {
      // The global mutation policy presents the structured API error.
    }
  };

  const restore = async (component: Component) => {
    if (selectedDomainId === null) {
      return;
    }
    try {
      await restoreComponent.mutateAsync({ domainId: selectedDomainId, id: component.id });
      showSuccessNotification(t('components.notifications.restored'));
      if (catalogQuery.data?.items.length === 1 && page > 0) {
        setPage(page - 1);
      }
    } catch {
      // The global mutation policy presents the structured API error.
    }
  };

  const hasFilters = Boolean(search.trim()) || componentTypeId !== undefined;
  const totalPages = Math.ceil((catalogQuery.data?.totalItems ?? 0) / componentCatalogPageSize);

  return (
    <Stack gap="xl">
      <PageHeader
        title={title}
        description={t('components.page.description', { domain: selectedDomain?.name ?? '' })}
        actions={
          <Group className={classes['header-actions']}>
            <SegmentedControl
              aria-label={t('components.view.label')}
              value={view}
              onChange={changeView}
              data={[
                {
                  value: 'cards',
                  label: (
                    <Group gap={6} wrap="nowrap">
                      <IconCards size={16} />
                      <span>{t('components.view.cards')}</span>
                    </Group>
                  ),
                },
                {
                  value: 'table',
                  label: (
                    <Group gap={6} wrap="nowrap">
                      <IconList size={16} />
                      <span>{t('components.view.table')}</span>
                    </Group>
                  ),
                },
              ]}
            />
            {!archived ? (
              <Button component={Link} to="/components/new" leftSection={<IconPlus size={16} />}>
                {t('components.actions.create')}
              </Button>
            ) : null}
          </Group>
        }
      />

      <Paper p="md" withBorder>
        <Stack gap="md">
          <div className={classes.filters}>
            <TextInput
              label={t('components.filters.search')}
              placeholder={t('components.filters.searchPlaceholder')}
              leftSection={<IconSearch size={17} />}
              value={search}
              onChange={(event) => {
                setSearch(event.currentTarget.value);
                setPage(0);
              }}
            />
            <Select
              label={t('components.filters.type')}
              placeholder={t('components.filters.allTypes')}
              data={componentTypes.map((type) => ({ value: String(type.id), label: type.name }))}
              value={componentTypeId === undefined ? null : String(componentTypeId)}
              onChange={(value) => {
                setRequestedTypeId(value === null ? undefined : Number(value));
                setPage(0);
              }}
              clearable
              searchable
              disabled={componentTypesQuery.isPending || Boolean(componentTypesQuery.error)}
            />
            <Stack gap={5}>
              <Text size="sm" fw={500}>
                {t('components.filters.section')}
              </Text>
              <SegmentedControl
                fullWidth
                value={archived ? 'archive' : 'active'}
                onChange={changeArchiveMode}
                data={[
                  { value: 'active', label: t('components.filters.active') },
                  { value: 'archive', label: t('components.filters.archive') },
                ]}
              />
            </Stack>
          </div>
          <Group justify="space-between">
            <Text size="sm" c="dimmed" aria-live="polite">
              {t('components.states.total', { count: catalogQuery.data?.totalItems ?? 0 })}
            </Text>
            {hasFilters ? (
              <Button size="xs" variant="subtle" onClick={resetFilters}>
                {t('components.filters.reset')}
              </Button>
            ) : null}
          </Group>
          {catalogQuery.isFetching && !catalogQuery.isPending ? (
            <Progress
              value={100}
              size="xs"
              animated
              aria-label={t('components.states.refreshing')}
            />
          ) : null}
        </Stack>
      </Paper>

      {catalogQuery.isPending ? <LoadingState label={t('components.states.loading')} /> : null}
      {catalogQuery.error && !catalogQuery.data ? (
        <ErrorState
          error={catalogQuery.error}
          onRetry={() => {
            void catalogQuery.refetch();
          }}
        />
      ) : null}

      {!catalogQuery.isPending && !catalogQuery.error && catalogQuery.data?.items.length === 0 ? (
        <EmptyState
          title={
            hasFilters
              ? t('components.states.filteredEmptyTitle')
              : archived
                ? t('components.states.archiveEmptyTitle')
                : t('components.states.emptyTitle')
          }
          description={
            hasFilters
              ? t('components.states.filteredEmptyDescription')
              : archived
                ? t('components.states.archiveEmptyDescription')
                : t('components.states.emptyDescription')
          }
          action={
            hasFilters ? (
              <Button variant="light" onClick={resetFilters}>
                {t('components.filters.reset')}
              </Button>
            ) : !archived ? (
              <Button component={Link} to="/components/new">
                {t('components.actions.create')}
              </Button>
            ) : undefined
          }
        />
      ) : null}

      {catalogQuery.data?.items.length ? (
        <ComponentCatalogContent
          components={catalogQuery.data.items}
          componentTypes={componentTypes}
          view={view}
          archived={archived}
          pendingComponentId={
            restoreComponent.isPending ? restoreComponent.variables?.id : undefined
          }
          onArchive={setComponentToArchive}
          onRestore={(component) => void restore(component)}
        />
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

      <Modal
        opened={Boolean(componentToArchive)}
        onClose={() => {
          if (!archiveComponent.isPending) {
            setComponentToArchive(undefined);
          }
        }}
        title={t('components.archive.title')}
        centered
        closeOnClickOutside={!archiveComponent.isPending}
        closeOnEscape={!archiveComponent.isPending}
      >
        <Stack gap="md">
          <Text>
            {t('components.archive.description', { name: componentToArchive?.name ?? '' })}
          </Text>
          <Text size="sm" c="dimmed">
            {t('components.archive.hint')}
          </Text>
          <Group justify="flex-end">
            <Button
              variant="default"
              disabled={archiveComponent.isPending}
              onClick={() => setComponentToArchive(undefined)}
            >
              {t('common.cancel')}
            </Button>
            <Button
              color="orange"
              loading={archiveComponent.isPending}
              onClick={() => void confirmArchive()}
            >
              {t('components.actions.archive')}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}
