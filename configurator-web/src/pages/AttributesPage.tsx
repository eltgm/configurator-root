import {
  ActionIcon,
  Badge,
  Button,
  Group,
  Modal,
  Paper,
  Stack,
  Text,
  Title,
  Tooltip,
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconBraces, IconPencil, IconPlus, IconTrash } from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import {
  useAttributeCatalogQuery,
  useDeleteAttributeMutation,
} from '@/features/attributes/api/attributes';
import { AttributeFormModal } from '@/features/attributes/ui/AttributeFormModal';
import { useComponentTypesQuery } from '@/features/component-types/api/component-types';
import { useDomainContext } from '@/features/domains/model/domain-context';
import type { AttributeDefinition } from '@/shared/api';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { showSuccessNotification } from '@/shared/notifications/notifications';
import { EmptyState, ErrorState, LoadingState, PageHeader } from '@/shared/ui';

export function AttributesPage() {
  const { t } = useTranslation();
  const { selectedDomainId, selectedDomain } = useDomainContext();
  const catalogQuery = useAttributeCatalogQuery(selectedDomainId);
  const typesQuery = useComponentTypesQuery(selectedDomainId);
  const deleteAttribute = useDeleteAttributeMutation();
  const [formOpened, form] = useDisclosure(false);
  const [editingAttribute, setEditingAttribute] = useState<AttributeDefinition>();
  const [deletingAttribute, setDeletingAttribute] = useState<AttributeDefinition>();
  const title = t('attributes.catalog.title');
  useDocumentTitle(title, t('app.name'));

  const typeNames = new Map((typesQuery.data ?? []).map((type) => [type.id, type.name]));

  const openCreate = () => {
    setEditingAttribute(undefined);
    form.open();
  };

  const openEdit = (attribute: AttributeDefinition) => {
    setEditingAttribute(attribute);
    form.open();
  };

  const confirmDelete = async () => {
    if (!deletingAttribute || selectedDomainId === null) {
      return;
    }
    try {
      await deleteAttribute.mutateAsync({
        domainId: selectedDomainId,
        attributeId: deletingAttribute.id,
      });
      showSuccessNotification(t('attributes.notifications.deleted'));
      setDeletingAttribute(undefined);
    } catch {
      // Keep the confirmation open so the normalized 409 remains understandable in context.
    }
  };

  return (
    <Stack gap="xl">
      <PageHeader
        title={title}
        description={t('attributes.catalog.description', { domain: selectedDomain?.name ?? '' })}
        actions={
          <Button leftSection={<IconPlus size={18} />} onClick={openCreate}>
            {t('attributes.actions.createCatalog')}
          </Button>
        }
      />

      {catalogQuery.isPending ? <LoadingState label={t('attributes.states.loading')} /> : null}
      {catalogQuery.error ? (
        <ErrorState error={catalogQuery.error} onRetry={() => void catalogQuery.refetch()} />
      ) : null}
      {!catalogQuery.isPending && !catalogQuery.error && catalogQuery.data?.length === 0 ? (
        <EmptyState
          icon={<IconBraces size={26} stroke={1.7} />}
          title={t('attributes.catalog.emptyTitle')}
          description={t('attributes.catalog.emptyDescription')}
          action={<Button onClick={openCreate}>{t('attributes.actions.createCatalog')}</Button>}
        />
      ) : null}

      {catalogQuery.data?.length ? (
        <Stack gap="sm">
          {catalogQuery.data.map((attribute) => (
            <Paper key={attribute.id} p="md" withBorder>
              <Group justify="space-between" align="flex-start" wrap="nowrap">
                <Stack gap={6} miw={0}>
                  <Group gap="xs">
                    <Title order={2} size="h4">
                      {attribute.label}
                    </Title>
                    <Badge variant="light">{t(`attributes.dataTypes.${attribute.dataType}`)}</Badge>
                  </Group>
                  <Text size="sm" c="dimmed">
                    {attribute.name}
                  </Text>
                  {attribute.enumValues?.length ? (
                    <Text size="sm">
                      {t('attributes.list.enumValues', { values: attribute.enumValues.join(', ') })}
                    </Text>
                  ) : null}
                  <Group gap="xs">
                    {(attribute.componentTypeIds ?? []).map((typeId) => (
                      <Badge key={typeId} variant="outline" color="gray">
                        {typeNames.get(typeId) ?? `#${typeId}`}
                      </Badge>
                    ))}
                    {!attribute.componentTypeIds?.length ? (
                      <Text size="xs" c="dimmed">
                        {t('attributes.catalog.notUsed')}
                      </Text>
                    ) : null}
                  </Group>
                </Stack>
                <Group gap={4} wrap="nowrap">
                  <Tooltip label={t('attributes.actions.edit')}>
                    <ActionIcon
                      variant="subtle"
                      aria-label={t('attributes.actions.editNamed', { name: attribute.label })}
                      onClick={() => openEdit(attribute)}
                    >
                      <IconPencil size={18} />
                    </ActionIcon>
                  </Tooltip>
                  <Tooltip label={t('attributes.actions.delete')}>
                    <ActionIcon
                      variant="subtle"
                      color="red"
                      aria-label={t('attributes.actions.deleteNamed', { name: attribute.label })}
                      onClick={() => setDeletingAttribute(attribute)}
                    >
                      <IconTrash size={18} />
                    </ActionIcon>
                  </Tooltip>
                </Group>
              </Group>
            </Paper>
          ))}
        </Stack>
      ) : null}

      {selectedDomainId !== null ? (
        <AttributeFormModal
          opened={formOpened}
          domainId={selectedDomainId}
          catalogOnly
          attribute={editingAttribute}
          onClose={form.close}
          onSaved={() => undefined}
        />
      ) : null}

      <Modal
        opened={Boolean(deletingAttribute)}
        onClose={() => !deleteAttribute.isPending && setDeletingAttribute(undefined)}
        title={t('attributes.delete.title')}
        centered
        closeOnClickOutside={!deleteAttribute.isPending}
        closeOnEscape={!deleteAttribute.isPending}
      >
        <Stack gap="md">
          <Text>
            {t('attributes.delete.description', { name: deletingAttribute?.label ?? '' })}
          </Text>
          <Text size="sm" c="red">
            {t('attributes.delete.warning')}
          </Text>
          <Group justify="flex-end">
            <Button
              variant="default"
              disabled={deleteAttribute.isPending}
              onClick={() => setDeletingAttribute(undefined)}
            >
              {t('common.cancel')}
            </Button>
            <Button
              color="red"
              loading={deleteAttribute.isPending}
              onClick={() => void confirmDelete()}
            >
              {t('attributes.actions.delete')}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}
