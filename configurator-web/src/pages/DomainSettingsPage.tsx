import {
  ActionIcon,
  Badge,
  Button,
  Group,
  Modal,
  Paper,
  SimpleGrid,
  Stack,
  Text,
  Title,
  Tooltip,
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconCheck, IconPencil, IconPlus, IconTrash } from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { useDeleteDomainMutation } from '@/features/domains/api/domains';
import { useDomainContext } from '@/features/domains/model/domain-context';
import { CreateDemoDomainButton } from '@/features/domains/ui/CreateDemoDomainButton';
import { DomainFormModal } from '@/features/domains/ui/DomainFormModal';
import type { Domain } from '@/shared/api';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { showSuccessNotification } from '@/shared/notifications/notifications';
import { EmptyState, ErrorState, LoadingState, PageHeader } from '@/shared/ui';

export function DomainSettingsPage() {
  const { t, i18n } = useTranslation();
  const { domains, selectedDomain, selectDomain, isLoading, error, refetch } = useDomainContext();
  const [formOpened, form] = useDisclosure(false);
  const [editingDomain, setEditingDomain] = useState<Domain>();
  const [deletingDomain, setDeletingDomain] = useState<Domain>();
  const deleteDomain = useDeleteDomainMutation();
  const title = t('domains.management.title');
  useDocumentTitle(title, t('app.name'));

  const dateFormatter = new Intl.DateTimeFormat(i18n.resolvedLanguage, {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });

  const openCreate = () => {
    setEditingDomain(undefined);
    form.open();
  };

  const openEdit = (domain: Domain) => {
    setEditingDomain(domain);
    form.open();
  };

  const confirmDelete = async () => {
    if (!deletingDomain) {
      return;
    }

    try {
      await deleteDomain.mutateAsync(deletingDomain.id);
      showSuccessNotification(t('domains.notifications.deleted'));
      setDeletingDomain(undefined);
    } catch {
      // The global mutation policy presents the structured API error.
    }
  };

  return (
    <Stack gap="xl">
      <PageHeader
        title={title}
        description={t('domains.management.description')}
        actions={
          <Group gap="sm">
            <CreateDemoDomainButton variant="light" />
            <Button leftSection={<IconPlus size={18} />} onClick={openCreate}>
              {t('domains.actions.create')}
            </Button>
          </Group>
        }
      />

      {isLoading ? <LoadingState label={t('domains.states.loading')} /> : null}
      {error ? <ErrorState error={error} onRetry={refetch} /> : null}
      {!isLoading && !error && domains.length === 0 ? (
        <EmptyState
          title={t('domains.management.emptyTitle')}
          description={t('domains.management.emptyDescription')}
          action={
            <Button leftSection={<IconPlus size={18} />} onClick={openCreate}>
              {t('domains.actions.create')}
            </Button>
          }
        />
      ) : null}
      {!isLoading && !error && domains.length > 0 ? (
        <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
          {domains.map((domain) => {
            const isSelected = domain.id === selectedDomain?.id;
            return (
              <Paper key={domain.id} p="lg" withBorder>
                <Stack gap="md">
                  <Group justify="space-between" align="flex-start" wrap="nowrap">
                    <Stack gap={4} miw={0}>
                      <Group gap="xs">
                        <Title order={2} size="h3">
                          {domain.name}
                        </Title>
                        {isSelected ? (
                          <Badge leftSection={<IconCheck size={12} />}>
                            {t('domains.management.current')}
                          </Badge>
                        ) : null}
                      </Group>
                      <Text size="sm" c="dimmed">
                        {domain.description || t('domains.management.noDescription')}
                      </Text>
                    </Stack>
                    <Group gap={4} wrap="nowrap">
                      <Tooltip label={t('domains.actions.edit')}>
                        <ActionIcon
                          variant="subtle"
                          aria-label={t('domains.actions.editNamed', { name: domain.name })}
                          onClick={() => openEdit(domain)}
                        >
                          <IconPencil size={18} />
                        </ActionIcon>
                      </Tooltip>
                      <Tooltip label={t('domains.actions.delete')}>
                        <ActionIcon
                          variant="subtle"
                          color="red"
                          aria-label={t('domains.actions.deleteNamed', { name: domain.name })}
                          onClick={() => setDeletingDomain(domain)}
                        >
                          <IconTrash size={18} />
                        </ActionIcon>
                      </Tooltip>
                    </Group>
                  </Group>
                  <Group justify="space-between">
                    <Text size="xs" c="dimmed">
                      {t('domains.management.createdAt', {
                        date: dateFormatter.format(new Date(domain.createdAt)),
                      })}
                    </Text>
                    {!isSelected ? (
                      <Button size="xs" variant="light" onClick={() => selectDomain(domain.id)}>
                        {t('domains.actions.select')}
                      </Button>
                    ) : null}
                  </Group>
                </Stack>
              </Paper>
            );
          })}
        </SimpleGrid>
      ) : null}

      <DomainFormModal
        opened={formOpened}
        domain={editingDomain}
        onClose={form.close}
        onSaved={(domain) => {
          selectDomain(domain.id);
        }}
      />

      <Modal
        opened={Boolean(deletingDomain)}
        onClose={() => {
          if (!deleteDomain.isPending) {
            setDeletingDomain(undefined);
          }
        }}
        title={t('domains.delete.title')}
        centered
        closeOnClickOutside={!deleteDomain.isPending}
        closeOnEscape={!deleteDomain.isPending}
      >
        <Stack gap="md">
          <Text>{t('domains.delete.description', { name: deletingDomain?.name ?? '' })}</Text>
          <Text size="sm" c="red">
            {t('domains.delete.warning')}
          </Text>
          <Group justify="flex-end">
            <Button
              variant="default"
              disabled={deleteDomain.isPending}
              onClick={() => setDeletingDomain(undefined)}
            >
              {t('common.cancel')}
            </Button>
            <Button
              color="red"
              loading={deleteDomain.isPending}
              onClick={() => void confirmDelete()}
            >
              {t('domains.actions.delete')}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}
