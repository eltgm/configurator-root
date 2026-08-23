import { Badge, Button, Group, Modal, Paper, Stack, Text, TextInput } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconCirclesRelation, IconPlus, IconSearch } from '@tabler/icons-react';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import { useCompatibilityGraphQuery } from '@/features/compatibility/api/compatibility-graph';
import { useDeleteCompatibilityLinkMutation } from '@/features/compatibility/api/manual-compatibility';
import {
  filterManualCompatibilityLinks,
  hasAvailableCompatibilityPair,
  toManualCompatibilityLinks,
  type ManualCompatibilityLinkView,
} from '@/features/compatibility/model/manual-compatibility';
import { ManualCompatibilityFormModal } from '@/features/compatibility/ui/ManualCompatibilityFormModal';
import { ManualCompatibilityList } from '@/features/compatibility/ui/ManualCompatibilityList';
import { useDomainContext } from '@/features/domains/model/domain-context';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { showSuccessNotification } from '@/shared/notifications/notifications';
import { EmptyState, ErrorState, LoadingState, PageHeader } from '@/shared/ui';

import classes from './manual-compatibility-page.module.css';

interface ManualCompatibilityContentProps {
  domainId: number;
  domainName: string;
}

function ManualCompatibilityContent({ domainId, domainName }: ManualCompatibilityContentProps) {
  const { t } = useTranslation();
  const graphQuery = useCompatibilityGraphQuery(domainId);
  const graph = graphQuery.data;
  const [search, setSearch] = useState('');
  const [formOpened, form] = useDisclosure(false);
  const [deletingLink, setDeletingLink] = useState<ManualCompatibilityLinkView>();
  const deleteLink = useDeleteCompatibilityLinkMutation();
  const links = useMemo(() => (graph ? toManualCompatibilityLinks(graph) : []), [graph]);
  const filteredLinks = useMemo(
    () => filterManualCompatibilityLinks(links, search),
    [links, search],
  );
  const canCreate = graph ? hasAvailableCompatibilityPair(graph) : false;

  const confirmDelete = async () => {
    if (!deletingLink) {
      return;
    }
    try {
      await deleteLink.mutateAsync({ domainId, linkId: deletingLink.edge.id });
      showSuccessNotification(t('manualCompatibility.notifications.deleted'));
      setDeletingLink(undefined);
    } catch {
      // The global mutation policy presents the structured API error.
    }
  };

  return (
    <Stack gap="xl">
      <PageHeader
        title={t('manualCompatibility.page.title')}
        description={t('manualCompatibility.page.description', { domain: domainName })}
        actions={
          <Button
            className={classes['create-action']}
            leftSection={<IconPlus size={18} />}
            disabled={!canCreate}
            onClick={form.open}
          >
            {t('manualCompatibility.actions.create')}
          </Button>
        }
      />

      {graph ? (
        <Group gap="sm" role="status" aria-live="polite">
          <Badge variant="light" size="lg">
            {t('manualCompatibility.summary.components', { count: graph.nodes.length })}
          </Badge>
          <Badge variant="light" size="lg">
            {t('manualCompatibility.summary.links', { count: links.length })}
          </Badge>
          {graphQuery.isFetching ? (
            <Text size="xs" c="dimmed">
              {t('manualCompatibility.states.refreshing')}
            </Text>
          ) : null}
        </Group>
      ) : null}

      {graphQuery.isPending ? (
        <LoadingState label={t('manualCompatibility.states.loading')} />
      ) : null}
      {graphQuery.error && !graph ? (
        <ErrorState
          error={graphQuery.error}
          onRetry={() => {
            void graphQuery.refetch();
          }}
        />
      ) : null}

      {graph && graph.nodes.length === 0 ? (
        <EmptyState
          icon={<IconCirclesRelation size={26} stroke={1.7} />}
          title={t('manualCompatibility.states.noComponentsTitle')}
          description={t('manualCompatibility.states.noComponentsDescription')}
          action={
            <Button component={Link} to="/components" variant="light">
              {t('manualCompatibility.actions.openCatalog')}
            </Button>
          }
        />
      ) : null}

      {graph && graph.nodes.length === 1 ? (
        <EmptyState
          icon={<IconCirclesRelation size={26} stroke={1.7} />}
          title={t('manualCompatibility.states.oneComponentTitle')}
          description={t('manualCompatibility.states.oneComponentDescription')}
          action={
            <Button component={Link} to="/components/new" leftSection={<IconPlus size={18} />}>
              {t('manualCompatibility.actions.createComponent')}
            </Button>
          }
        />
      ) : null}

      {graph && graph.nodes.length >= 2 && links.length === 0 ? (
        <EmptyState
          icon={<IconCirclesRelation size={26} stroke={1.7} />}
          title={t('manualCompatibility.states.noLinksTitle')}
          description={t('manualCompatibility.states.noLinksDescription')}
          action={
            <Button leftSection={<IconPlus size={18} />} onClick={form.open}>
              {t('manualCompatibility.actions.createFirst')}
            </Button>
          }
        />
      ) : null}

      {graph && links.length > 0 ? (
        <Stack gap="md">
          <Paper p="md" withBorder>
            <TextInput
              label={t('manualCompatibility.search.label')}
              placeholder={t('manualCompatibility.search.placeholder')}
              leftSection={<IconSearch size={17} />}
              value={search}
              onChange={(event) => setSearch(event.currentTarget.value)}
            />
          </Paper>

          {filteredLinks.length > 0 ? (
            <ManualCompatibilityList links={filteredLinks} onDelete={setDeletingLink} />
          ) : (
            <EmptyState
              title={t('manualCompatibility.states.noSearchResultsTitle')}
              description={t('manualCompatibility.states.noSearchResultsDescription')}
              action={
                <Button variant="light" onClick={() => setSearch('')}>
                  {t('manualCompatibility.actions.clearSearch')}
                </Button>
              }
            />
          )}
        </Stack>
      ) : null}

      {graph ? (
        <ManualCompatibilityFormModal
          opened={formOpened}
          domainId={domainId}
          graph={graph}
          onClose={form.close}
        />
      ) : null}

      <Modal
        opened={Boolean(deletingLink)}
        onClose={() => {
          if (!deleteLink.isPending) {
            setDeletingLink(undefined);
          }
        }}
        title={t('manualCompatibility.delete.title')}
        centered
        closeOnClickOutside={!deleteLink.isPending}
        closeOnEscape={!deleteLink.isPending}
      >
        <Stack gap="md">
          <Text>
            {t('manualCompatibility.delete.description', {
              first: deletingLink?.componentA.name ?? '',
              second: deletingLink?.componentB.name ?? '',
            })}
          </Text>
          <Text size="sm" c="red">
            {t('manualCompatibility.delete.warning')}
          </Text>
          <Group justify="flex-end">
            <Button
              variant="default"
              disabled={deleteLink.isPending}
              onClick={() => setDeletingLink(undefined)}
            >
              {t('common.cancel')}
            </Button>
            <Button color="red" loading={deleteLink.isPending} onClick={() => void confirmDelete()}>
              {t('manualCompatibility.actions.delete')}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}

export function ManualCompatibilityPage() {
  const { t } = useTranslation();
  const { selectedDomainId, selectedDomain } = useDomainContext();
  const title = t('manualCompatibility.page.title');
  useDocumentTitle(title, t('app.name'));

  if (selectedDomainId === null) {
    return null;
  }

  return (
    <ManualCompatibilityContent
      key={selectedDomainId}
      domainId={selectedDomainId}
      domainName={selectedDomain?.name ?? ''}
    />
  );
}
