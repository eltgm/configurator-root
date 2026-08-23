import { Badge, Button, Group, Image, Paper, Stack, Text, ThemeIcon } from '@mantine/core';
import { IconCircleCheck, IconPhotoOff, IconPlus } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import {
  getPrimaryComponentImage,
  toComponentImageUrl,
} from '@/features/components/model/catalog-preferences';
import type { ConfiguratorComponentSelection } from '@/features/configurator/model/configurator-compatibility';
import type { Component } from '@/shared/api';

import classes from './configurator-workspace.module.css';

export interface ConfiguratorBrowserCardComponent extends ConfiguratorComponentSelection {
  componentTypeName?: string;
  images?: Component['images'];
}

interface ConfiguratorCandidateCardProps {
  component: ConfiguratorBrowserCardComponent;
  componentTypeName?: string;
  catalogMode: boolean;
  replacementMode: boolean;
  onSelect: (component: ConfiguratorComponentSelection) => void;
}

export function ConfiguratorCandidateCard({
  component,
  componentTypeName,
  catalogMode,
  replacementMode,
  onSelect,
}: ConfiguratorCandidateCardProps) {
  const { t } = useTranslation();
  const image = getPrimaryComponentImage(component.images);

  return (
    <Paper className={classes['browser-card']} p="sm" withBorder>
      <Group align="stretch" wrap="nowrap">
        <div className={classes.preview}>
          {image ? (
            <Image src={toComponentImageUrl(image.url)} alt="" fit="cover" h="100%" w="100%" />
          ) : (
            <ThemeIcon size={40} radius="xl" variant="light" aria-hidden="true">
              {catalogMode ? <IconPhotoOff size={22} /> : <IconCircleCheck size={22} />}
            </ThemeIcon>
          )}
        </div>
        <Stack gap={7} flex={1} miw={0}>
          <Stack gap={2}>
            <Text component={Link} to={`/components/${component.id}`} fw={650} truncate>
              {component.name}
            </Text>
            <Text size="xs" c="dimmed" truncate>
              {[component.brand, component.componentTypeName ?? componentTypeName]
                .filter(Boolean)
                .join(' · ')}
            </Text>
          </Stack>
          <Group justify="space-between" align="center" mt="auto">
            {!catalogMode ? (
              <Badge color="green" variant="light">
                {t('configurator.browser.direct')}
              </Badge>
            ) : (
              <span />
            )}
            <Button
              size="xs"
              leftSection={<IconPlus size={14} />}
              onClick={() => onSelect(component)}
            >
              {replacementMode
                ? t('configurator.browser.selectReplacement')
                : t('configurator.browser.add')}
            </Button>
          </Group>
        </Stack>
      </Group>
    </Paper>
  );
}
