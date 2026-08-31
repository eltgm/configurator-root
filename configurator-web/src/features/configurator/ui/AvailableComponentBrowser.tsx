import {
  Accordion,
  Alert,
  Badge,
  Button,
  Group,
  Pagination,
  Paper,
  Progress,
  Select,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { IconAlertTriangle, IconSearch, IconX } from '@tabler/icons-react';
import { useMemo, useState, type Ref } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import { componentCatalogPageSize, useComponentsQuery } from '@/features/components/api/components';
import {
  useAssemblyCandidatesQuery,
  useCompatibilityIntersectionQuery,
  useDirectCompatibilityQuery,
} from '@/features/configurator/api/configurator-compatibility';
import type { ConfiguratorDraftItem } from '@/features/configurator/model/configurator-draft';
import {
  blockedCandidatesFromAssemblyResponse,
  candidatesFromAssemblyResponse,
  candidatesFromDirectResponse,
  candidatesFromIntersectionResponse,
  filterConfiguratorCandidates,
  type ConfiguratorCandidate,
  type ConfiguratorComponentSelection,
} from '@/features/configurator/model/configurator-compatibility';
import {
  ConfiguratorCandidateCard,
  type ConfiguratorBrowserCardComponent,
} from '@/features/configurator/ui/ConfiguratorCandidateCard';
import {
  CompatibilityExplanationDrawer,
  type CompatibilityExplanationGroup,
} from '@/features/configurator/ui/CompatibilityExplanationDrawer';
import type { ComponentType } from '@/shared/api';
import { EmptyState, ErrorState, LoadingState } from '@/shared/ui';

import classes from './configurator-workspace.module.css';

interface AvailableComponentBrowserProps {
  headingRef?: Ref<HTMLHeadingElement>;
  domainId: number;
  componentTypes: ReadonlyArray<ComponentType>;
  componentTypesLoading: boolean;
  componentTypesUnavailable: boolean;
  selectedItems: ReadonlyArray<ConfiguratorDraftItem>;
  baseComponentIds: ReadonlyArray<number>;
  baseComponentNames: ReadonlyMap<number, string>;
  includeTransitive: boolean;
  compatibilityBlocked: boolean;
  replacementTarget?: ConfiguratorComponentSelection;
  onCancelReplacement: () => void;
  onSelect: (component: ConfiguratorComponentSelection) => void;
}

export function AvailableComponentBrowser({
  headingRef,
  domainId,
  componentTypes,
  componentTypesLoading,
  componentTypesUnavailable,
  selectedItems,
  baseComponentIds,
  baseComponentNames,
  includeTransitive,
  compatibilityBlocked,
  replacementTarget,
  onCancelReplacement,
  onSelect,
}: AvailableComponentBrowserProps) {
  const { t } = useTranslation();
  const [search, setSearch] = useState('');
  const [debouncedSearch] = useDebouncedValue(search, 300);
  const [componentTypeId, setComponentTypeId] = useState<number>();
  const [page, setPage] = useState(0);
  const [explanationCandidate, setExplanationCandidate] = useState<ConfiguratorCandidate>();
  const selectedTypeIds = useMemo(
    () => new Set(selectedItems.map((item) => item.componentTypeId)),
    [selectedItems],
  );
  const catalogMode =
    selectedItems.length === 0 || Boolean(replacementTarget && baseComponentIds.length === 0);
  const availableComponentTypeId =
    componentTypeId === undefined ||
    selectedItems.length === 0 ||
    !selectedTypeIds.has(componentTypeId)
      ? componentTypeId
      : undefined;
  const effectiveTypeId = replacementTarget?.componentTypeId ?? availableComponentTypeId;
  const catalogQuery = useComponentsQuery(
    domainId,
    {
      ...(effectiveTypeId === undefined ? {} : { componentTypeId: effectiveTypeId }),
      name: debouncedSearch,
      archived: false,
      page,
      size: componentCatalogPageSize,
    },
    catalogMode && !compatibilityBlocked,
  );
  const directQuery = useDirectCompatibilityQuery(
    domainId,
    baseComponentIds.length === 1 ? baseComponentIds[0]! : null,
    includeTransitive,
    includeTransitive && !catalogMode && !compatibilityBlocked,
  );
  const intersectionQuery = useCompatibilityIntersectionQuery(
    domainId,
    baseComponentIds,
    includeTransitive,
    includeTransitive && !catalogMode && !compatibilityBlocked,
  );
  const assemblyQuery = useAssemblyCandidatesQuery(
    domainId,
    baseComponentIds,
    !includeTransitive && !catalogMode && !compatibilityBlocked,
  );

  const compatibilityCandidates = useMemo(() => {
    if (!includeTransitive && assemblyQuery.data) {
      return candidatesFromAssemblyResponse(assemblyQuery.data);
    }
    if (baseComponentIds.length === 1 && directQuery.data) {
      return candidatesFromDirectResponse(directQuery.data);
    }
    if (baseComponentIds.length >= 2 && intersectionQuery.data) {
      return candidatesFromIntersectionResponse(intersectionQuery.data);
    }
    return [];
  }, [
    assemblyQuery.data,
    baseComponentIds.length,
    directQuery.data,
    includeTransitive,
    intersectionQuery.data,
  ]);
  const blockedCandidates = useMemo(() => {
    if (includeTransitive || !assemblyQuery.data) return [];
    const normalizedSearch = search.trim().toLocaleLowerCase();
    return blockedCandidatesFromAssemblyResponse(assemblyQuery.data).filter(
      (candidate) =>
        candidate.id !== replacementTarget?.id &&
        (effectiveTypeId === undefined || candidate.componentTypeId === effectiveTypeId) &&
        (replacementTarget || !selectedTypeIds.has(candidate.componentTypeId)) &&
        (!normalizedSearch ||
          candidate.name.toLocaleLowerCase().includes(normalizedSearch) ||
          candidate.brand?.toLocaleLowerCase().includes(normalizedSearch)),
    );
  }, [
    assemblyQuery.data,
    effectiveTypeId,
    includeTransitive,
    replacementTarget,
    search,
    selectedTypeIds,
  ]);
  const filteredCompatibilityCandidates = useMemo(
    () =>
      filterConfiguratorCandidates(compatibilityCandidates, {
        search,
        ...(effectiveTypeId === undefined ? {} : { componentTypeId: effectiveTypeId }),
        ...(replacementTarget
          ? { excludedComponentId: replacementTarget.id }
          : { excludedComponentTypeIds: selectedTypeIds }),
      }),
    [compatibilityCandidates, effectiveTypeId, replacementTarget, search, selectedTypeIds],
  );
  const totalPages = catalogMode
    ? Math.ceil((catalogQuery.data?.totalItems ?? 0) / componentCatalogPageSize)
    : Math.ceil(filteredCompatibilityCandidates.length / componentCatalogPageSize);
  const visibleCompatibilityCandidates = filteredCompatibilityCandidates.slice(
    page * componentCatalogPageSize,
    (page + 1) * componentCatalogPageSize,
  );
  const catalogComponents = (catalogQuery.data?.items ?? []).filter(
    (component) => component.id !== replacementTarget?.id,
  );
  const visibleComponents: ReadonlyArray<ConfiguratorBrowserCardComponent> = catalogMode
    ? catalogComponents
    : visibleCompatibilityCandidates;
  const contextQuery = includeTransitive
    ? baseComponentIds.length === 1
      ? directQuery
      : intersectionQuery
    : assemblyQuery;
  const isPending = catalogMode ? catalogQuery.isPending : contextQuery.isPending;
  const isRefreshing = catalogMode ? catalogQuery.isFetching : contextQuery.isFetching;
  const error = catalogMode ? catalogQuery.error : contextQuery.error;
  const totalItems = catalogMode
    ? (catalogQuery.data?.totalItems ?? 0)
    : filteredCompatibilityCandidates.length;
  const typeOptions = componentTypes
    .filter(
      (type) =>
        replacementTarget?.componentTypeId === type.id ||
        selectedItems.length === 0 ||
        !selectedTypeIds.has(type.id),
    )
    .map((type) => ({ value: String(type.id), label: type.name }));
  const typeNames = new Map(componentTypes.map((type) => [type.id, type.name]));
  const hasFilters =
    Boolean(search.trim()) || (!replacementTarget && availableComponentTypeId !== undefined);
  const explanationGroups: CompatibilityExplanationGroup[] =
    explanationCandidate?.compatibilityByBase.map((entry) => ({
      key: String(entry.baseComponentId),
      title: t('configurator.explanations.baseTitle', {
        name:
          baseComponentNames.get(entry.baseComponentId) ??
          t('configurator.explanations.path.unknownComponent', { id: entry.baseComponentId }),
      }),
      relation: entry.relation,
      explanations: entry.explanations,
    })) ?? [];

  const resetFilters = () => {
    setSearch('');
    if (!replacementTarget) {
      setComponentTypeId(undefined);
    }
    setPage(0);
  };

  return (
    <>
      <Paper
        id="available-components-browser"
        component="section"
        aria-labelledby="available-components-title"
        className={classes.browser}
        p="lg"
        withBorder
      >
        <Stack gap="lg">
          <Group justify="space-between" align="flex-start" wrap="wrap">
            <Stack gap={4} className={classes['browser-heading']}>
              <Title
                ref={headingRef}
                tabIndex={headingRef ? -1 : undefined}
                id="available-components-title"
                order={2}
                size="h3"
              >
                {replacementTarget
                  ? t('configurator.browser.replacementTitle')
                  : t('configurator.browser.title')}
              </Title>
              <Text size="sm" c="dimmed">
                {replacementTarget
                  ? t('configurator.browser.replacementDescription', {
                      name: replacementTarget.name,
                    })
                  : selectedItems.length === 0
                    ? t('configurator.browser.description')
                    : t(
                        includeTransitive
                          ? 'configurator.browser.transitiveDescription'
                          : 'configurator.browser.compatibleDescription',
                        {
                          count: baseComponentIds.length,
                        },
                      )}
              </Text>
            </Stack>
            {replacementTarget ? (
              <Button
                className={classes['cancel-replacement']}
                size="xs"
                variant="subtle"
                leftSection={<IconX size={14} />}
                onClick={onCancelReplacement}
              >
                {t('configurator.browser.cancelReplacement')}
              </Button>
            ) : null}
          </Group>

          {compatibilityBlocked ? (
            <Alert
              color="orange"
              icon={<IconAlertTriangle aria-hidden="true" />}
              title={t('configurator.browser.blockedTitle')}
            >
              {t('configurator.browser.blockedDescription')}
            </Alert>
          ) : (
            <>
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
                  data={typeOptions}
                  value={effectiveTypeId === undefined ? null : String(effectiveTypeId)}
                  onChange={(value) => {
                    setComponentTypeId(value === null ? undefined : Number(value));
                    setPage(0);
                  }}
                  clearable={!replacementTarget}
                  searchable
                  disabled={
                    Boolean(replacementTarget) || componentTypesLoading || componentTypesUnavailable
                  }
                  comboboxProps={{ withinPortal: false }}
                />
              </div>
              <Group justify="space-between">
                <Text size="sm" c="dimmed" aria-live="polite">
                  {t('configurator.browser.total', { count: totalItems })}
                </Text>
                {hasFilters ? (
                  <Button size="xs" variant="subtle" onClick={resetFilters}>
                    {t('configurator.browser.reset')}
                  </Button>
                ) : null}
              </Group>
              {isRefreshing && !isPending ? (
                <Progress
                  value={100}
                  size="xs"
                  animated
                  aria-label={t('configurator.browser.refreshing')}
                />
              ) : null}

              {isPending ? <LoadingState label={t('configurator.browser.loading')} /> : null}
              {error ? (
                <ErrorState
                  error={error}
                  onRetry={() =>
                    void (catalogMode ? catalogQuery.refetch() : contextQuery.refetch())
                  }
                />
              ) : null}
              {!isPending && !error && visibleComponents.length === 0 ? (
                <EmptyState
                  title={
                    hasFilters
                      ? t('configurator.browser.filteredEmptyTitle')
                      : catalogMode
                        ? t('configurator.browser.emptyTitle')
                        : t('configurator.browser.compatibleEmptyTitle')
                  }
                  description={
                    hasFilters
                      ? t('configurator.browser.filteredEmptyDescription')
                      : catalogMode
                        ? t('configurator.browser.emptyDescription')
                        : t('configurator.browser.compatibleEmptyDescription')
                  }
                  action={
                    hasFilters ? (
                      <Button variant="light" onClick={resetFilters}>
                        {t('configurator.browser.reset')}
                      </Button>
                    ) : catalogMode && selectedItems.length === 0 ? (
                      <Button component={Link} to="/components/new" variant="light">
                        {t('configurator.browser.createComponent')}
                      </Button>
                    ) : undefined
                  }
                />
              ) : null}

              {visibleComponents.length > 0 ? (
                <SimpleGrid cols={{ base: 1, md: 2 }} spacing="sm">
                  {visibleComponents.map((component) => (
                    <ConfiguratorCandidateCard
                      key={component.id}
                      component={component}
                      {...(typeNames.get(component.componentTypeId)
                        ? { componentTypeName: typeNames.get(component.componentTypeId)! }
                        : {})}
                      catalogMode={catalogMode}
                      replacementMode={Boolean(replacementTarget)}
                      onExplain={setExplanationCandidate}
                      onSelect={onSelect}
                    />
                  ))}
                </SimpleGrid>
              ) : null}

              {!catalogMode && blockedCandidates.length > 0 ? (
                <Accordion variant="contained">
                  <Accordion.Item value="blocked-candidates">
                    <Accordion.Control>
                      {t('configurator.browser.unavailableTitle', {
                        count: blockedCandidates.length,
                      })}
                    </Accordion.Control>
                    <Accordion.Panel>
                      <Stack gap="sm">
                        <Text size="sm" c="dimmed">
                          {t('configurator.browser.unavailableDescription')}
                        </Text>
                        {blockedCandidates.slice(0, componentCatalogPageSize).map((candidate) => (
                          <Paper key={candidate.id} p="sm" withBorder>
                            <Stack gap="xs">
                              <Group justify="space-between" align="flex-start">
                                <Stack gap={2}>
                                  <Text
                                    component={Link}
                                    to={`/components/${candidate.id}`}
                                    fw={650}
                                  >
                                    {candidate.name}
                                  </Text>
                                  <Text size="xs" c="dimmed">
                                    {[candidate.brand, candidate.componentTypeName]
                                      .filter(Boolean)
                                      .join(' · ')}
                                  </Text>
                                </Stack>
                                <Badge color="red" variant="light">
                                  {t('configurator.browser.unavailableBadge')}
                                </Badge>
                              </Group>
                              {candidate.blockingByBase.map((entry) => (
                                <Text key={entry.baseComponentId} size="sm">
                                  {t('configurator.browser.blockingReason', {
                                    component:
                                      baseComponentNames.get(entry.baseComponentId) ??
                                      `#${entry.baseComponentId}`,
                                    rules: entry.blockingRules
                                      .map((rule) => rule.ruleSetName)
                                      .join(', '),
                                  })}
                                </Text>
                              ))}
                            </Stack>
                          </Paper>
                        ))}
                      </Stack>
                    </Accordion.Panel>
                  </Accordion.Item>
                </Accordion>
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
            </>
          )}
        </Stack>
      </Paper>
      <CompatibilityExplanationDrawer
        opened={Boolean(explanationCandidate)}
        onClose={() => setExplanationCandidate(undefined)}
        domainId={domainId}
        title={
          explanationCandidate
            ? t('configurator.explanations.candidateTitle', {
                name: explanationCandidate.name,
              })
            : ''
        }
        groups={explanationGroups}
      />
    </>
  );
}
