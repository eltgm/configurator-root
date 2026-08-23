import {
  Badge,
  Button,
  Divider,
  Group,
  Modal,
  Paper,
  SimpleGrid,
  Stack,
  Text,
  Title,
} from '@mantine/core';
import { IconArchive, IconArrowLeft, IconEdit, IconRestore } from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';

import { useComponentTypesQuery } from '@/features/component-types/api/component-types';
import {
  useArchiveComponentMutation,
  useComponentQuery,
  useRestoreComponentMutation,
} from '@/features/components/api/components';
import { ComponentImageGallery } from '@/features/components/ui/ComponentImageGallery';
import { useDomainContext } from '@/features/domains/model/domain-context';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { showSuccessNotification } from '@/shared/notifications/notifications';
import { EmptyState, ErrorState, LoadingState, PageHeader } from '@/shared/ui';

import classes from './component-details-page.module.css';

export function ComponentDetailsPage() {
  const { t, i18n } = useTranslation();
  const { componentId: rawComponentId } = useParams();
  const { selectedDomainId } = useDomainContext();
  const parsedId = Number(rawComponentId);
  const componentId = Number.isInteger(parsedId) && parsedId > 0 ? parsedId : null;
  const componentQuery = useComponentQuery(selectedDomainId, componentId);
  const componentTypesQuery = useComponentTypesQuery(selectedDomainId);
  const archiveComponent = useArchiveComponentMutation();
  const restoreComponent = useRestoreComponentMutation();
  const [archiveOpened, setArchiveOpened] = useState(false);
  const component = componentQuery.data;
  useDocumentTitle(component?.name ?? t('components.detail.title'), t('app.name'));

  if (selectedDomainId === null) return null;
  if (componentId === null) {
    return (
      <EmptyState
        title={t('components.detail.notFoundTitle')}
        description={t('components.detail.notFoundDescription')}
      />
    );
  }
  if (componentQuery.isPending || componentTypesQuery.isPending) {
    return <LoadingState label={t('components.detail.loading')} />;
  }
  if (componentQuery.error) {
    return (
      <ErrorState error={componentQuery.error} onRetry={() => void componentQuery.refetch()} />
    );
  }
  if (componentTypesQuery.error) {
    return (
      <ErrorState
        error={componentTypesQuery.error}
        onRetry={() => void componentTypesQuery.refetch()}
      />
    );
  }

  const componentType = componentTypesQuery.data?.find(
    (type) => type.id === component?.componentTypeId,
  );
  if (!component || !componentType) {
    return (
      <EmptyState
        title={t('components.detail.notFoundTitle')}
        description={t('components.detail.notFoundDescription')}
      />
    );
  }
  const date = new Intl.DateTimeFormat(i18n.resolvedLanguage, { dateStyle: 'long' }).format(
    new Date(component.createdAt),
  );

  const archive = async () => {
    try {
      await archiveComponent.mutateAsync({ domainId: selectedDomainId, id: component.id });
      showSuccessNotification(t('components.notifications.archived'));
      setArchiveOpened(false);
    } catch {
      // Global mutation handling shows a safe structured error.
    }
  };
  const restore = async () => {
    try {
      await restoreComponent.mutateAsync({ domainId: selectedDomainId, id: component.id });
      showSuccessNotification(t('components.notifications.restored'));
    } catch {
      // Global mutation handling shows a safe structured error.
    }
  };

  return (
    <Stack gap="xl">
      <PageHeader
        title={component.name}
        description={t('components.detail.subtitle', { type: componentType.name })}
        actions={
          <Group>
            <Button
              component={Link}
              to="/components"
              variant="default"
              leftSection={<IconArrowLeft size={16} />}
            >
              {t('components.actions.backToCatalog')}
            </Button>
            {component.archived ? (
              <Button
                leftSection={<IconRestore size={16} />}
                loading={restoreComponent.isPending}
                onClick={() => void restore()}
              >
                {t('components.actions.restore')}
              </Button>
            ) : (
              <>
                <Button
                  component={Link}
                  to={`/components/${component.id}/edit`}
                  leftSection={<IconEdit size={16} />}
                >
                  {t('components.actions.edit')}
                </Button>
                <Button
                  color="orange"
                  variant="light"
                  leftSection={<IconArchive size={16} />}
                  onClick={() => setArchiveOpened(true)}
                >
                  {t('components.actions.archive')}
                </Button>
              </>
            )}
          </Group>
        }
      />

      {component.archived ? (
        <Badge size="lg" color="gray" w="fit-content">
          {t('components.item.archived')}
        </Badge>
      ) : null}
      <div className={classes.layout}>
        <Stack gap="lg">
          <Paper p="lg" withBorder>
            <Stack gap="md">
              <Title order={2} size="h3">
                {t('components.detail.sections.about')}
              </Title>
              <Divider />
              <SimpleGrid cols={{ base: 1, sm: 2 }}>
                <Stack gap={2}>
                  <Text size="sm" c="dimmed">
                    {t('components.detail.type')}
                  </Text>
                  <Text fw={600}>{componentType.name}</Text>
                </Stack>
                <Stack gap={2}>
                  <Text size="sm" c="dimmed">
                    {t('components.detail.brand')}
                  </Text>
                  <Text fw={600}>{component.brand || t('components.item.noBrand')}</Text>
                </Stack>
                <Stack gap={2}>
                  <Text size="sm" c="dimmed">
                    {t('components.detail.createdAt')}
                  </Text>
                  <Text fw={600}>{date}</Text>
                </Stack>
              </SimpleGrid>
              <Stack gap={2}>
                <Text size="sm" c="dimmed">
                  {t('components.detail.description')}
                </Text>
                <Text>{component.description || t('components.detail.noDescription')}</Text>
              </Stack>
            </Stack>
          </Paper>

          <Paper p="lg" withBorder>
            <Stack gap="md">
              <Title order={2} size="h3">
                {t('components.detail.sections.attributes')}
              </Title>
              <Divider />
              {(component.attributes ?? []).length ? (
                <Stack gap="sm">
                  {component.attributes?.map((attribute) => (
                    <Group
                      key={attribute.attributeDefinitionId}
                      justify="space-between"
                      align="flex-start"
                      wrap="nowrap"
                    >
                      <Stack gap={1}>
                        <Text fw={500}>{attribute.label}</Text>
                        <Text size="xs" c="dimmed">
                          {attribute.name}
                        </Text>
                      </Stack>
                      <Text fw={600}>{attribute.value || t('components.item.noValue')}</Text>
                    </Group>
                  ))}
                </Stack>
              ) : (
                <Text c="dimmed">{t('components.detail.noAttributes')}</Text>
              )}
            </Stack>
          </Paper>
        </Stack>

        <ComponentImageGallery
          key={component.id + '-' + component.archived}
          domainId={selectedDomainId}
          componentId={component.id}
          componentName={component.name}
          archived={component.archived}
        />
      </div>

      <Modal
        opened={archiveOpened}
        onClose={() => !archiveComponent.isPending && setArchiveOpened(false)}
        title={t('components.archive.title')}
        centered
        closeOnClickOutside={!archiveComponent.isPending}
        closeOnEscape={!archiveComponent.isPending}
      >
        <Stack gap="md">
          <Text>{t('components.archive.description', { name: component.name })}</Text>
          <Text size="sm" c="dimmed">
            {t('components.archive.hint')}
          </Text>
          <Group justify="flex-end">
            <Button
              variant="default"
              disabled={archiveComponent.isPending}
              onClick={() => setArchiveOpened(false)}
            >
              {t('common.cancel')}
            </Button>
            <Button
              color="orange"
              loading={archiveComponent.isPending}
              onClick={() => void archive()}
            >
              {t('components.actions.archive')}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}
