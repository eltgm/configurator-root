import {
  ActionIcon,
  Badge,
  Button,
  Divider,
  Group,
  Menu,
  Modal,
  Paper,
  ScrollArea,
  Stack,
  Text,
  Title,
  Tooltip,
  UnstyledButton,
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import {
  IconBraces,
  IconCheck,
  IconLink,
  IconPencil,
  IconPlus,
  IconTrash,
  IconUnlink,
} from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import {
  useAttributesQuery,
  useDetachAttributeMutation,
} from '@/features/attributes/api/attributes';
import { AttachAttributeModal } from '@/features/attributes/ui/AttachAttributeModal';
import { AttributeFormModal } from '@/features/attributes/ui/AttributeFormModal';
import {
  useComponentTypesQuery,
  useDeleteComponentTypeMutation,
} from '@/features/component-types/api/component-types';
import { ComponentTypeFormModal } from '@/features/component-types/ui/ComponentTypeFormModal';
import { useDomainContext } from '@/features/domains/model/domain-context';
import type { AttributeDefinition, ComponentType } from '@/shared/api';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { showSuccessNotification } from '@/shared/notifications/notifications';
import { EmptyState, ErrorState, LoadingState, PageHeader } from '@/shared/ui';

import classes from './component-types-page.module.css';

export function ComponentTypesPage() {
  const { t } = useTranslation();
  const { selectedDomainId, selectedDomain } = useDomainContext();
  const typesQuery = useComponentTypesQuery(selectedDomainId);
  const componentTypes = typesQuery.data ?? [];
  const [preferredTypeId, setPreferredTypeId] = useState<number>();
  const selectedType =
    componentTypes.find((componentType) => componentType.id === preferredTypeId) ??
    componentTypes[0];
  const attributesQuery = useAttributesQuery(selectedDomainId, selectedType?.id ?? null);
  const [typeFormOpened, typeForm] = useDisclosure(false);
  const [attributeFormOpened, attributeForm] = useDisclosure(false);
  const [attachFormOpened, attachForm] = useDisclosure(false);
  const [editingType, setEditingType] = useState<ComponentType>();
  const [editingAttribute, setEditingAttribute] = useState<AttributeDefinition>();
  const [deletingType, setDeletingType] = useState<ComponentType>();
  const [detachingAttribute, setDetachingAttribute] = useState<AttributeDefinition>();
  const deleteType = useDeleteComponentTypeMutation();
  const detachAttribute = useDetachAttributeMutation();
  const title = t('componentTypes.page.title');
  useDocumentTitle(title, t('app.name'));

  const openCreateType = () => {
    setEditingType(undefined);
    typeForm.open();
  };

  const openEditType = (componentType: ComponentType) => {
    setEditingType(componentType);
    typeForm.open();
  };

  const openCreateAttribute = () => {
    setEditingAttribute(undefined);
    attributeForm.open();
  };

  const openEditAttribute = (attribute: AttributeDefinition) => {
    setEditingAttribute(attribute);
    attributeForm.open();
  };

  const confirmDelete = async () => {
    if (!deletingType || selectedDomainId === null) {
      return;
    }
    try {
      await deleteType.mutateAsync({ domainId: selectedDomainId, id: deletingType.id });
      showSuccessNotification(t('componentTypes.notifications.deleted'));
      setDeletingType(undefined);
    } catch {
      // The global mutation policy presents the structured API error.
    }
  };

  const confirmDetach = async () => {
    if (!detachingAttribute || !selectedType || selectedDomainId === null) {
      return;
    }
    try {
      await detachAttribute.mutateAsync({
        domainId: selectedDomainId,
        componentTypeId: selectedType.id,
        attributeId: detachingAttribute.id,
      });
      showSuccessNotification(t('attributes.notifications.detached'));
      setDetachingAttribute(undefined);
    } catch {
      // The global mutation policy presents the structured API error.
    }
  };

  return (
    <Stack gap="xl">
      <PageHeader
        title={title}
        description={t('componentTypes.page.description', { domain: selectedDomain?.name ?? '' })}
        actions={
          <Button leftSection={<IconPlus size={18} />} onClick={openCreateType}>
            {t('componentTypes.actions.create')}
          </Button>
        }
      />

      {typesQuery.isPending ? <LoadingState label={t('componentTypes.states.loading')} /> : null}
      {typesQuery.error ? (
        <ErrorState
          error={typesQuery.error}
          onRetry={() => {
            void typesQuery.refetch();
          }}
        />
      ) : null}
      {!typesQuery.isPending && !typesQuery.error && componentTypes.length === 0 ? (
        <EmptyState
          title={t('componentTypes.states.emptyTitle')}
          description={t('componentTypes.states.emptyDescription')}
          action={
            <Button leftSection={<IconPlus size={18} />} onClick={openCreateType}>
              {t('componentTypes.actions.create')}
            </Button>
          }
        />
      ) : null}

      {!typesQuery.isPending && !typesQuery.error && selectedType ? (
        <div className={classes.layout}>
          <Paper className={classes.master} withBorder>
            <Group justify="space-between" p="md">
              <Title order={2} size="h4">
                {t('componentTypes.list.title')}
              </Title>
              <Badge variant="light">{componentTypes.length}</Badge>
            </Group>
            <Divider />
            <ScrollArea.Autosize mah={620} type="auto">
              <Stack gap={6} p="sm">
                {componentTypes.map((componentType) => {
                  const selected = componentType.id === selectedType.id;
                  return (
                    <UnstyledButton
                      key={componentType.id}
                      className={classes['type-option']}
                      data-selected={selected || undefined}
                      aria-pressed={selected}
                      onClick={() => setPreferredTypeId(componentType.id)}
                    >
                      <Group justify="space-between" wrap="nowrap" align="flex-start">
                        <Stack gap={2} miw={0}>
                          <Text fw={600} truncate>
                            {componentType.name}
                          </Text>
                          <Text size="xs" c="dimmed" truncate>
                            {componentType.code || t('componentTypes.list.noCode')}
                          </Text>
                        </Stack>
                        {selected ? <IconCheck size={17} aria-hidden="true" /> : null}
                      </Group>
                    </UnstyledButton>
                  );
                })}
              </Stack>
            </ScrollArea.Autosize>
          </Paper>

          <Paper className={classes.detail} p={{ base: 'md', sm: 'lg' }} withBorder>
            <Stack gap="lg">
              <Group justify="space-between" align="flex-start" wrap="nowrap">
                <Stack gap={5} miw={0}>
                  <Group gap="xs">
                    <Title order={2} size="h3">
                      {selectedType.name}
                    </Title>
                    {selectedType.code ? (
                      <Badge variant="outline">{selectedType.code}</Badge>
                    ) : null}
                  </Group>
                  <Text c="dimmed" size="sm">
                    {selectedType.description || t('componentTypes.list.noDescription')}
                  </Text>
                  {selectedType.orderIndex !== undefined ? (
                    <Text size="xs" c="dimmed">
                      {t('componentTypes.list.orderIndex', { value: selectedType.orderIndex })}
                    </Text>
                  ) : null}
                </Stack>
                <Group gap={4} wrap="nowrap">
                  <Tooltip label={t('componentTypes.actions.edit')}>
                    <ActionIcon
                      variant="subtle"
                      aria-label={t('componentTypes.actions.editNamed', {
                        name: selectedType.name,
                      })}
                      onClick={() => openEditType(selectedType)}
                    >
                      <IconPencil size={18} />
                    </ActionIcon>
                  </Tooltip>
                  <Tooltip label={t('componentTypes.actions.delete')}>
                    <ActionIcon
                      variant="subtle"
                      color="red"
                      aria-label={t('componentTypes.actions.deleteNamed', {
                        name: selectedType.name,
                      })}
                      onClick={() => setDeletingType(selectedType)}
                    >
                      <IconTrash size={18} />
                    </ActionIcon>
                  </Tooltip>
                </Group>
              </Group>

              <Divider />

              <Group justify="space-between">
                <Stack gap={2}>
                  <Title order={3} size="h4">
                    {t('attributes.list.title')}
                  </Title>
                  <Text size="sm" c="dimmed">
                    {t('attributes.list.description')}
                  </Text>
                </Stack>
                <Menu position="bottom-end" withinPortal>
                  <Menu.Target>
                    <Button size="xs" variant="light" leftSection={<IconPlus size={16} />}>
                      {t('attributes.actions.add')}
                    </Button>
                  </Menu.Target>
                  <Menu.Dropdown>
                    <Menu.Item leftSection={<IconPlus size={16} />} onClick={openCreateAttribute}>
                      {t('attributes.actions.createNew')}
                    </Menu.Item>
                    <Menu.Item leftSection={<IconLink size={16} />} onClick={attachForm.open}>
                      {t('attributes.actions.useExisting')}
                    </Menu.Item>
                  </Menu.Dropdown>
                </Menu>
              </Group>

              {attributesQuery.isPending ? (
                <LoadingState label={t('attributes.states.loading')} />
              ) : null}
              {attributesQuery.error ? (
                <ErrorState
                  error={attributesQuery.error}
                  onRetry={() => {
                    void attributesQuery.refetch();
                  }}
                />
              ) : null}
              {!attributesQuery.isPending &&
              !attributesQuery.error &&
              attributesQuery.data?.length === 0 ? (
                <EmptyState
                  icon={<IconBraces size={26} stroke={1.7} />}
                  title={t('attributes.states.emptyTitle')}
                  description={t('attributes.states.emptyDescription')}
                  action={
                    <Button size="sm" onClick={openCreateAttribute}>
                      {t('attributes.actions.create')}
                    </Button>
                  }
                />
              ) : null}
              {!attributesQuery.isPending &&
              !attributesQuery.error &&
              attributesQuery.data?.length ? (
                <Stack gap="sm">
                  {attributesQuery.data.map((attribute) => (
                    <Paper
                      key={attribute.id}
                      p="md"
                      bg="var(--mantine-color-default-hover)"
                      withBorder
                    >
                      <Group justify="space-between" align="flex-start" wrap="nowrap">
                        <Stack gap={5} miw={0}>
                          <Group gap="xs">
                            <Text fw={600}>{attribute.label}</Text>
                            <Badge size="sm" variant="light">
                              {t(`attributes.dataTypes.${attribute.dataType}`)}
                            </Badge>
                            {attribute.isRequired ? (
                              <Badge size="sm" color="orange" variant="light">
                                {t('attributes.list.required')}
                              </Badge>
                            ) : null}
                          </Group>
                          <Text size="xs" c="dimmed">
                            {attribute.name}
                          </Text>
                          {attribute.enumValues?.length ? (
                            <Text size="sm">
                              {t('attributes.list.enumValues', {
                                values: attribute.enumValues.join(', '),
                              })}
                            </Text>
                          ) : null}
                          {attribute.orderIndex !== undefined ? (
                            <Text size="xs" c="dimmed">
                              {t('attributes.list.orderIndex', { value: attribute.orderIndex })}
                            </Text>
                          ) : null}
                        </Stack>
                        <Group gap={4} wrap="nowrap">
                          <Tooltip label={t('attributes.actions.edit')}>
                            <ActionIcon
                              variant="subtle"
                              aria-label={t('attributes.actions.editNamed', {
                                name: attribute.label,
                              })}
                              onClick={() => openEditAttribute(attribute)}
                            >
                              <IconPencil size={18} />
                            </ActionIcon>
                          </Tooltip>
                          <Tooltip label={t('attributes.actions.detach')}>
                            <ActionIcon
                              variant="subtle"
                              color="red"
                              aria-label={t('attributes.actions.detachNamed', {
                                name: attribute.label,
                              })}
                              onClick={() => setDetachingAttribute(attribute)}
                            >
                              <IconUnlink size={18} />
                            </ActionIcon>
                          </Tooltip>
                        </Group>
                      </Group>
                    </Paper>
                  ))}
                </Stack>
              ) : null}
            </Stack>
          </Paper>
        </div>
      ) : null}

      {selectedDomainId !== null ? (
        <ComponentTypeFormModal
          opened={typeFormOpened}
          domainId={selectedDomainId}
          componentType={editingType}
          onClose={typeForm.close}
          onSaved={(componentType) => setPreferredTypeId(componentType.id)}
        />
      ) : null}
      {selectedDomainId !== null && selectedType ? (
        <AttributeFormModal
          opened={attributeFormOpened}
          domainId={selectedDomainId}
          componentTypeId={selectedType.id}
          attribute={editingAttribute}
          onClose={attributeForm.close}
          onSaved={() => undefined}
        />
      ) : null}
      {selectedDomainId !== null && selectedType ? (
        <AttachAttributeModal
          opened={attachFormOpened}
          domainId={selectedDomainId}
          componentTypeId={selectedType.id}
          linkedAttributes={attributesQuery.data ?? []}
          onClose={attachForm.close}
        />
      ) : null}

      <Modal
        opened={Boolean(deletingType)}
        onClose={() => {
          if (!deleteType.isPending) {
            setDeletingType(undefined);
          }
        }}
        title={t('componentTypes.delete.title')}
        centered
        closeOnClickOutside={!deleteType.isPending}
        closeOnEscape={!deleteType.isPending}
      >
        <Stack gap="md">
          <Text>{t('componentTypes.delete.description', { name: deletingType?.name ?? '' })}</Text>
          <Text size="sm" c="red">
            {t('componentTypes.delete.warning')}
          </Text>
          <Group justify="flex-end">
            <Button
              variant="default"
              disabled={deleteType.isPending}
              onClick={() => setDeletingType(undefined)}
            >
              {t('common.cancel')}
            </Button>
            <Button color="red" loading={deleteType.isPending} onClick={() => void confirmDelete()}>
              {t('componentTypes.actions.delete')}
            </Button>
          </Group>
        </Stack>
      </Modal>

      <Modal
        opened={Boolean(detachingAttribute)}
        onClose={() => !detachAttribute.isPending && setDetachingAttribute(undefined)}
        title={t('attributes.detach.title')}
        centered
        closeOnClickOutside={!detachAttribute.isPending}
        closeOnEscape={!detachAttribute.isPending}
      >
        <Stack gap="md">
          <Text>
            {t('attributes.detach.description', { name: detachingAttribute?.label ?? '' })}
          </Text>
          <Text size="sm" c="red">
            {t('attributes.detach.warning')}
          </Text>
          <Group justify="flex-end">
            <Button
              variant="default"
              disabled={detachAttribute.isPending}
              onClick={() => setDetachingAttribute(undefined)}
            >
              {t('common.cancel')}
            </Button>
            <Button
              color="red"
              loading={detachAttribute.isPending}
              onClick={() => void confirmDetach()}
            >
              {t('attributes.actions.detach')}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}
