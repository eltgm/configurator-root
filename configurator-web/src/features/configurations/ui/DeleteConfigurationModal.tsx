import { Button, Group, Modal, Stack, Text } from '@mantine/core';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';

import { useDeleteConfigurationMutation } from '@/features/configurations/api/configurations';
import type { Configuration } from '@/shared/api';
import { showSuccessNotification } from '@/shared/notifications/notifications';
import { ErrorState } from '@/shared/ui';

interface DeleteConfigurationModalProps {
  configuration: Configuration | undefined;
  onClose: () => void;
  onDeleted: (configuration: Configuration) => void;
}

export function DeleteConfigurationModal({
  configuration,
  onClose,
  onDeleted,
}: DeleteConfigurationModalProps) {
  const { t } = useTranslation();
  const deleteConfiguration = useDeleteConfigurationMutation();
  const resetMutation = deleteConfiguration.reset;

  useEffect(() => {
    if (configuration) {
      resetMutation();
    }
  }, [configuration, resetMutation]);

  const close = () => {
    if (!deleteConfiguration.isPending) {
      onClose();
    }
  };

  const confirmDelete = async () => {
    if (!configuration) return;

    try {
      await deleteConfiguration.mutateAsync({
        domainId: configuration.domainId,
        configurationId: configuration.id,
      });
      showSuccessNotification(t('configurations.notifications.deleted'));
      onDeleted(configuration);
    } catch {
      // The modal keeps the normalized mutation error visible for a deliberate retry.
    }
  };

  return (
    <Modal
      opened={Boolean(configuration)}
      onClose={close}
      title={t('configurations.delete.title')}
      centered
      closeOnClickOutside={!deleteConfiguration.isPending}
      closeOnEscape={!deleteConfiguration.isPending}
    >
      <Stack gap="md">
        <Text>{t('configurations.delete.description', { name: configuration?.name ?? '' })}</Text>
        <Text size="sm" c="red">
          {t('configurations.delete.warning')}
        </Text>
        {deleteConfiguration.error ? <ErrorState error={deleteConfiguration.error} /> : null}
        <Group justify="flex-end">
          <Button variant="default" disabled={deleteConfiguration.isPending} onClick={close}>
            {t('common.cancel')}
          </Button>
          <Button
            color="red"
            loading={deleteConfiguration.isPending}
            onClick={() => void confirmDelete()}
          >
            {t('configurations.actions.delete')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
