import {
  Alert,
  Badge,
  Button,
  Group,
  Image,
  Paper,
  Skeleton,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core';
import { IconAlertTriangle, IconPhotoOff, IconRefresh, IconTrash } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import {
  getPrimaryComponentImage,
  toComponentImageUrl,
} from '@/features/components/model/catalog-preferences';
import type { ConfiguratorDraftSlot } from '@/features/configurator/model/use-configurator-draft';
import type { ComponentType } from '@/shared/api';
import { EmptyState } from '@/shared/ui';

import classes from './configurator-workspace.module.css';

interface CurrentAssemblyProps {
  slots: ReadonlyArray<ConfiguratorDraftSlot>;
  componentTypes: ReadonlyArray<ComponentType>;
  onRemove: (componentId: number) => void;
  onClear: () => void;
}

export function CurrentAssembly({
  slots,
  componentTypes,
  onRemove,
  onClear,
}: CurrentAssemblyProps) {
  const { t } = useTranslation();
  const typeNames = new Map(componentTypes.map((type) => [type.id, type.name]));

  return (
    <Paper
      component="section"
      aria-labelledby="current-assembly-title"
      className={classes.assembly}
      p="lg"
      withBorder
    >
      <Stack gap="lg">
        <Group justify="space-between" align="flex-start" wrap="nowrap">
          <Stack gap={4}>
            <Title id="current-assembly-title" order={2} size="h3">
              {t('configurator.assembly.title')}
            </Title>
            <Text size="sm" c="dimmed">
              {t('configurator.assembly.count', { count: slots.length })}
            </Text>
          </Stack>
          {slots.length > 0 ? (
            <Button size="xs" variant="subtle" color="red" onClick={onClear}>
              {t('configurator.assembly.clear')}
            </Button>
          ) : null}
        </Group>

        {slots.length === 0 ? (
          <EmptyState
            title={t('configurator.assembly.emptyTitle')}
            description={t('configurator.assembly.emptyDescription')}
          />
        ) : (
          <Stack gap="sm">
            {slots.map((slot) => {
              const typeName =
                typeNames.get(slot.item.componentTypeId) ??
                t('configurator.assembly.unknownType', { id: slot.item.componentTypeId });
              if (slot.status === 'loading') {
                return (
                  <Paper key={slot.item.componentId} p="sm" withBorder>
                    <Group wrap="nowrap">
                      <Skeleton height={56} width={56} radius="md" />
                      <Stack gap={6} flex={1}>
                        <Skeleton height={14} width="45%" />
                        <Skeleton height={12} width="70%" />
                      </Stack>
                    </Group>
                  </Paper>
                );
              }
              if (slot.status === 'error' || !slot.component) {
                return (
                  <Alert
                    key={slot.item.componentId}
                    color="orange"
                    icon={<IconAlertTriangle aria-hidden="true" />}
                    title={typeName}
                  >
                    <Stack gap="sm">
                      <Text size="sm">
                        {t('configurator.assembly.unavailable', { id: slot.item.componentId })}
                      </Text>
                      <Group gap="xs">
                        <Button
                          size="xs"
                          variant="light"
                          leftSection={<IconRefresh size={14} />}
                          onClick={slot.retry}
                        >
                          {t('configurator.assembly.retry')}
                        </Button>
                        <Button
                          size="xs"
                          variant="subtle"
                          color="red"
                          onClick={() => onRemove(slot.item.componentId)}
                        >
                          {t('configurator.assembly.remove')}
                        </Button>
                      </Group>
                    </Stack>
                  </Alert>
                );
              }
              const component = slot.component;
              const image = getPrimaryComponentImage(component.images);
              return (
                <Paper key={component.id} className={classes['assembly-card']} p="sm" withBorder>
                  <Group align="center" wrap="nowrap">
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
                    <Stack gap={3} flex={1} miw={0}>
                      <Group gap="xs" wrap="nowrap">
                        <Text component={Link} to={`/components/${component.id}`} fw={650} truncate>
                          {component.name}
                        </Text>
                        {component.archived ? (
                          <Badge color="gray" size="xs">
                            {t('configurator.assembly.archived')}
                          </Badge>
                        ) : null}
                      </Group>
                      <Text size="xs" c="dimmed" truncate>
                        {[component.brand, typeName].filter(Boolean).join(' · ')}
                      </Text>
                    </Stack>
                    <Button
                      size="compact-sm"
                      variant="subtle"
                      color="red"
                      aria-label={t('configurator.assembly.removeNamed', {
                        name: component.name,
                      })}
                      onClick={() => onRemove(component.id)}
                    >
                      <IconTrash size={17} aria-hidden="true" />
                    </Button>
                  </Group>
                </Paper>
              );
            })}
          </Stack>
        )}
      </Stack>
    </Paper>
  );
}
