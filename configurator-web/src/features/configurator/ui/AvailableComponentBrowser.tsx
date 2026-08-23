import {
  Badge,
  Button,
  Group,
  Image,
  Pagination,
  Paper,
  Progress,
  Select,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  ThemeIcon,
  Title,
} from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { IconPhotoOff, IconPlus, IconSearch } from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import { componentCatalogPageSize, useComponentsQuery } from '@/features/components/api/components';
import {
  getPrimaryComponentImage,
  toComponentImageUrl,
} from '@/features/components/model/catalog-preferences';
import type { ConfiguratorDraftItem } from '@/features/configurator/model/configurator-draft';
import type { Component, ComponentType } from '@/shared/api';
import { EmptyState, ErrorState, LoadingState } from '@/shared/ui';

import classes from './configurator-workspace.module.css';

interface AvailableComponentBrowserProps {
  domainId: number;
  componentTypes: ReadonlyArray<ComponentType>;
  componentTypesLoading: boolean;
  componentTypesUnavailable: boolean;
  selectedItems: ReadonlyArray<ConfiguratorDraftItem>;
  onSelect: (component: Component) => void;
}

export function AvailableComponentBrowser({
  domainId,
  componentTypes,
  componentTypesLoading,
  componentTypesUnavailable,
  selectedItems,
  onSelect,
}: AvailableComponentBrowserProps) {
  const { t } = useTranslation();
  const [search, setSearch] = useState('');
  const [debouncedSearch] = useDebouncedValue(search, 300);
  const [componentTypeId, setComponentTypeId] = useState<number>();
  const [page, setPage] = useState(0);
  const query = useComponentsQuery(domainId, {
    ...(componentTypeId === undefined ? {} : { componentTypeId }),
    name: debouncedSearch,
    archived: false,
    page,
    size: componentCatalogPageSize,
  });
  const typeNames = new Map(componentTypes.map((type) => [type.id, type.name]));
  const totalPages = Math.ceil((query.data?.totalItems ?? 0) / componentCatalogPageSize);
  const hasFilters = Boolean(search.trim()) || componentTypeId !== undefined;

  const resetFilters = () => {
    setSearch('');
    setComponentTypeId(undefined);
    setPage(0);
  };

  return (
    <Paper
      component="section"
      aria-labelledby="available-components-title"
      className={classes.browser}
      p="lg"
      withBorder
    >
      <Stack gap="lg">
        <Stack gap={4}>
          <Title id="available-components-title" order={2} size="h3">
            {t('configurator.browser.title')}
          </Title>
          <Text size="sm" c="dimmed">
            {t('configurator.browser.description')}
          </Text>
        </Stack>
        <div className={classes.filters}>
          <TextInput
            aria-label={t('configurator.browser.search')}
            placeholder={t('configurator.browser.searchPlaceholder')}
            leftSection={<IconSearch size={17} aria-hidden="true" />}
            value={search}
            onChange={(event) => {
              setSearch(event.currentTarget.value);
              setPage(0);
            }}
          />
          <Select
            aria-label={t('configurator.browser.type')}
            placeholder={t('configurator.browser.allTypes')}
            data={componentTypes.map((type) => ({ value: String(type.id), label: type.name }))}
            value={componentTypeId === undefined ? null : String(componentTypeId)}
            onChange={(value) => {
              setComponentTypeId(value === null ? undefined : Number(value));
              setPage(0);
            }}
            clearable
            searchable
            disabled={componentTypesLoading || componentTypesUnavailable}
            comboboxProps={{ withinPortal: false }}
          />
        </div>
        <Group justify="space-between">
          <Text size="sm" c="dimmed" aria-live="polite">
            {t('configurator.browser.total', { count: query.data?.totalItems ?? 0 })}
          </Text>
          {hasFilters ? (
            <Button size="xs" variant="subtle" onClick={resetFilters}>
              {t('configurator.browser.reset')}
            </Button>
          ) : null}
        </Group>
        {query.isFetching && !query.isPending ? (
          <Progress
            value={100}
            size="xs"
            animated
            aria-label={t('configurator.browser.refreshing')}
          />
        ) : null}

        {query.isPending ? <LoadingState label={t('configurator.browser.loading')} /> : null}
        {query.error && !query.data ? (
          <ErrorState error={query.error} onRetry={() => void query.refetch()} />
        ) : null}
        {!query.isPending && !query.error && query.data?.items.length === 0 ? (
          <EmptyState
            title={
              hasFilters
                ? t('configurator.browser.filteredEmptyTitle')
                : t('configurator.browser.emptyTitle')
            }
            description={
              hasFilters
                ? t('configurator.browser.filteredEmptyDescription')
                : t('configurator.browser.emptyDescription')
            }
            action={
              hasFilters ? (
                <Button variant="light" onClick={resetFilters}>
                  {t('configurator.browser.reset')}
                </Button>
              ) : (
                <Button component={Link} to="/components/new" variant="light">
                  {t('configurator.browser.createComponent')}
                </Button>
              )
            }
          />
        ) : null}

        {query.data?.items.length ? (
          <SimpleGrid cols={{ base: 1, md: 2 }} spacing="sm">
            {query.data.items.map((component) => {
              const image = getPrimaryComponentImage(component.images);
              const selected = selectedItems.some((item) => item.componentId === component.id);
              const replaces = selectedItems.some(
                (item) =>
                  item.componentTypeId === component.componentTypeId &&
                  item.componentId !== component.id,
              );
              return (
                <Paper key={component.id} className={classes['browser-card']} p="sm" withBorder>
                  <Group align="stretch" wrap="nowrap">
                    <div className={classes.preview}>
                      {image ? (
                        <Image
                          src={toComponentImageUrl(image.url)}
                          alt=""
                          fit="cover"
                          h="100%"
                          w="100%"
                        />
                      ) : (
                        <ThemeIcon size={40} radius="xl" variant="light" aria-hidden="true">
                          <IconPhotoOff size={22} />
                        </ThemeIcon>
                      )}
                    </div>
                    <Stack gap={7} flex={1} miw={0}>
                      <Stack gap={2}>
                        <Text component={Link} to={`/components/${component.id}`} fw={650} truncate>
                          {component.name}
                        </Text>
                        <Text size="xs" c="dimmed" truncate>
                          {[component.brand, typeNames.get(component.componentTypeId)]
                            .filter(Boolean)
                            .join(' · ')}
                        </Text>
                      </Stack>
                      <Group justify="space-between" align="center" mt="auto">
                        {replaces ? (
                          <Badge color="yellow" variant="light">
                            {t('configurator.browser.replaces')}
                          </Badge>
                        ) : (
                          <span />
                        )}
                        <Button
                          size="xs"
                          variant={selected ? 'light' : 'filled'}
                          leftSection={selected ? undefined : <IconPlus size={14} />}
                          disabled={selected}
                          onClick={() => onSelect(component)}
                        >
                          {selected
                            ? t('configurator.browser.selected')
                            : replaces
                              ? t('configurator.browser.replace')
                              : t('configurator.browser.add')}
                        </Button>
                      </Group>
                    </Stack>
                  </Group>
                </Paper>
              );
            })}
          </SimpleGrid>
        ) : null}

        {totalPages > 1 ? (
          <Group justify="center">
            <Pagination
              value={page + 1}
              total={totalPages}
              onChange={(value) => setPage(value - 1)}
              getItemProps={(value) => ({
                'aria-label': t('configurator.browser.page', { page: value }),
              })}
            />
          </Group>
        ) : null}
      </Stack>
    </Paper>
  );
}
