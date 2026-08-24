import { ActionIcon, Button, Group, Menu, Stack, Text, Tooltip } from '@mantine/core';
import {
  IconCopy,
  IconDotsVertical,
  IconDownload,
  IconEdit,
  IconEye,
  IconTrash,
} from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import { canCopyConfiguration } from '@/features/configurations/model/configuration-operations';
import type { Configuration } from '@/shared/api';

interface ConfigurationActionsProps {
  configuration: Configuration;
  variant: 'buttons' | 'menu';
  isExporting: boolean;
  onCopy: () => void;
  onExport: () => void;
  onDelete: () => void;
}

export function ConfigurationActions({
  configuration,
  variant,
  isExporting,
  onCopy,
  onExport,
  onDelete,
}: ConfigurationActionsProps) {
  const { t } = useTranslation();
  const copyAllowed = canCopyConfiguration(configuration);
  const copyReason = copyAllowed ? undefined : t('configurations.copy.archivedReason');

  if (variant === 'menu') {
    return (
      <Menu position="bottom-end" withinPortal>
        <Menu.Target>
          <ActionIcon
            variant="subtle"
            aria-label={t('configurations.actions.menuNamed', { name: configuration.name })}
          >
            <IconDotsVertical size={18} aria-hidden="true" />
          </ActionIcon>
        </Menu.Target>
        <Menu.Dropdown>
          <Menu.Item
            component={Link}
            to={`/configurations/${configuration.id}`}
            leftSection={<IconEye size={16} aria-hidden="true" />}
          >
            {t('configurations.actions.open')}
          </Menu.Item>
          <Menu.Item
            component={Link}
            to={`/configurations/${configuration.id}/edit`}
            leftSection={<IconEdit size={16} aria-hidden="true" />}
          >
            {t('configurations.actions.edit')}
          </Menu.Item>
          <Menu.Item
            disabled={!copyAllowed}
            title={copyReason}
            leftSection={<IconCopy size={16} aria-hidden="true" />}
            onClick={onCopy}
          >
            <Stack gap={0}>
              <Text size="sm">{t('configurations.actions.copy')}</Text>
              {copyReason ? (
                <Text size="xs" c="dimmed">
                  {copyReason}
                </Text>
              ) : null}
            </Stack>
          </Menu.Item>
          <Menu.Item
            disabled={isExporting}
            leftSection={<IconDownload size={16} aria-hidden="true" />}
            onClick={onExport}
          >
            {isExporting
              ? t('configurations.actions.exporting')
              : t('configurations.actions.export')}
          </Menu.Item>
          <Menu.Divider />
          <Menu.Item
            color="red"
            leftSection={<IconTrash size={16} aria-hidden="true" />}
            onClick={onDelete}
          >
            {t('configurations.actions.delete')}
          </Menu.Item>
        </Menu.Dropdown>
      </Menu>
    );
  }

  return (
    <Stack gap="xs" align="flex-end">
      <Group gap="sm" wrap="wrap" justify="flex-end">
        <Tooltip label={copyReason} disabled={copyAllowed}>
          <span>
            <Button
              variant="light"
              disabled={!copyAllowed}
              leftSection={<IconCopy size={16} aria-hidden="true" />}
              onClick={onCopy}
            >
              {t('configurations.actions.copy')}
            </Button>
          </span>
        </Tooltip>
        <Button
          variant="light"
          loading={isExporting}
          leftSection={<IconDownload size={16} aria-hidden="true" />}
          onClick={onExport}
        >
          {t('configurations.actions.export')}
        </Button>
        <Button
          variant="subtle"
          color="red"
          leftSection={<IconTrash size={16} aria-hidden="true" />}
          onClick={onDelete}
        >
          {t('configurations.actions.delete')}
        </Button>
      </Group>
      {copyReason ? (
        <Text size="xs" c="dimmed">
          {copyReason}
        </Text>
      ) : null}
    </Stack>
  );
}
