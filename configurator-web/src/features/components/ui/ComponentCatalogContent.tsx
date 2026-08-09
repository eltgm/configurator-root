import {
  Badge,
  Box,
  Button,
  Group,
  Image,
  Paper,
  SimpleGrid,
  Stack,
  Table,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core';
import { IconArchive, IconPhotoOff, IconRestore } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';

import { toComponentImageUrl } from '@/features/components/model/catalog-preferences';
import type { Component, ComponentImage, ComponentType } from '@/shared/api';

import classes from './component-catalog-content.module.css';

interface ComponentCatalogContentProps {
  components: ReadonlyArray<Component>;
  componentTypes: ReadonlyArray<ComponentType>;
  view: 'cards' | 'table';
  archived: boolean;
  pendingComponentId?: number | undefined;
  onArchive: (component: Component) => void;
  onRestore: (component: Component) => void;
}

function getPrimaryImage(images: ReadonlyArray<ComponentImage> | undefined) {
  return images?.toSorted(
    (left, right) =>
      (left.orderIndex ?? Number.MAX_SAFE_INTEGER) -
        (right.orderIndex ?? Number.MAX_SAFE_INTEGER) || left.id - right.id,
  )[0];
}

function ComponentPreview({
  component,
  compact = false,
}: {
  component: Component;
  compact?: boolean;
}) {
  const image = getPrimaryImage(component.images);
  return (
    <Box className={compact ? classes['preview-compact'] : classes.preview}>
      {image ? (
        <Image src={toComponentImageUrl(image.url)} alt="" fit="cover" h="100%" w="100%" />
      ) : (
        <ThemeIcon size={compact ? 34 : 56} radius="xl" variant="light" aria-hidden="true">
          <IconPhotoOff size={compact ? 18 : 28} stroke={1.6} />
        </ThemeIcon>
      )}
    </Box>
  );
}

function ComponentAction({
  component,
  archived,
  pending,
  onArchive,
  onRestore,
}: {
  component: Component;
  archived: boolean;
  pending: boolean;
  onArchive: (component: Component) => void;
  onRestore: (component: Component) => void;
}) {
  const { t } = useTranslation();
  return archived ? (
    <Button
      size="xs"
      variant="light"
      leftSection={<IconRestore size={15} />}
      loading={pending}
      onClick={() => onRestore(component)}
    >
      {t('components.actions.restore')}
    </Button>
  ) : (
    <Button
      size="xs"
      variant="subtle"
      color="gray"
      leftSection={<IconArchive size={15} />}
      onClick={() => onArchive(component)}
    >
      {t('components.actions.archive')}
    </Button>
  );
}

function ComponentAttributes({ component }: { component: Component }) {
  const { t } = useTranslation();
  const attributes = component.attributes ?? [];
  if (attributes.length === 0) {
    return (
      <Text size="sm" c="dimmed">
        {t('components.item.noAttributes')}
      </Text>
    );
  }
  return (
    <Stack gap={4}>
      {attributes.slice(0, 4).map((attribute) => (
        <Group key={attribute.attributeDefinitionId} justify="space-between" gap="sm" wrap="nowrap">
          <Text size="xs" c="dimmed" truncate>
            {attribute.label}
          </Text>
          <Text size="xs" fw={600} truncate>
            {attribute.value || t('components.item.noValue')}
          </Text>
        </Group>
      ))}
      {attributes.length > 4 ? (
        <Text size="xs" c="dimmed">
          {t('components.item.moreAttributes', { count: attributes.length - 4 })}
        </Text>
      ) : null}
    </Stack>
  );
}

export function ComponentCatalogContent({
  components,
  componentTypes,
  view,
  archived,
  pendingComponentId,
  onArchive,
  onRestore,
}: ComponentCatalogContentProps) {
  const { t, i18n } = useTranslation();
  const typeNames = new Map(componentTypes.map((type) => [type.id, type.name]));
  const dateFormatter = new Intl.DateTimeFormat(i18n.resolvedLanguage, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
  const typeName = (component: Component) =>
    typeNames.get(component.componentTypeId) ?? t('components.item.unknownType');

  if (view === 'cards') {
    return (
      <SimpleGrid cols={{ base: 1, sm: 2, xl: 3 }} spacing="md">
        {components.map((component) => (
          <Paper key={component.id} className={classes.card} withBorder>
            <ComponentPreview component={component} />
            <Stack p="md" gap="md" className={classes['card-body']}>
              <Stack gap={5}>
                <Group justify="space-between" align="flex-start" wrap="nowrap">
                  <Title order={2} size="h4">
                    {component.name}
                  </Title>
                  {archived ? <Badge color="gray">{t('components.item.archived')}</Badge> : null}
                </Group>
                <Text size="sm" c="dimmed">
                  {[component.brand, typeName(component)].filter(Boolean).join(' · ')}
                </Text>
              </Stack>
              <ComponentAttributes component={component} />
              <Group justify="space-between" mt="auto">
                <Text size="xs" c="dimmed">
                  {dateFormatter.format(new Date(component.createdAt))}
                </Text>
                <ComponentAction
                  component={component}
                  archived={archived}
                  pending={pendingComponentId === component.id}
                  onArchive={onArchive}
                  onRestore={onRestore}
                />
              </Group>
            </Stack>
          </Paper>
        ))}
      </SimpleGrid>
    );
  }

  return (
    <>
      <Paper className={classes['desktop-table']} data-testid="desktop-component-table" withBorder>
        <Table verticalSpacing="sm" horizontalSpacing="md" highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{t('components.table.component')}</Table.Th>
              <Table.Th>{t('components.table.brand')}</Table.Th>
              <Table.Th>{t('components.table.type')}</Table.Th>
              <Table.Th>{t('components.table.createdAt')}</Table.Th>
              <Table.Th>{t('components.table.actions')}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {components.map((component) => (
              <Table.Tr key={component.id}>
                <Table.Td>
                  <Group gap="sm" wrap="nowrap">
                    <ComponentPreview component={component} compact />
                    <Text fw={600}>{component.name}</Text>
                  </Group>
                </Table.Td>
                <Table.Td>{component.brand || t('components.item.noBrand')}</Table.Td>
                <Table.Td>{typeName(component)}</Table.Td>
                <Table.Td>{dateFormatter.format(new Date(component.createdAt))}</Table.Td>
                <Table.Td>
                  <ComponentAction
                    component={component}
                    archived={archived}
                    pending={pendingComponentId === component.id}
                    onArchive={onArchive}
                    onRestore={onRestore}
                  />
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Paper>

      <Stack className={classes['mobile-list']} data-testid="mobile-component-list" gap="sm">
        {components.map((component) => (
          <Paper key={component.id} p="md" withBorder>
            <Group align="flex-start" wrap="nowrap">
              <ComponentPreview component={component} compact />
              <Stack gap={7} flex={1} miw={0}>
                <Stack gap={2}>
                  <Text fw={600} truncate>
                    {component.name}
                  </Text>
                  <Text size="xs" c="dimmed" truncate>
                    {[component.brand, typeName(component)].filter(Boolean).join(' · ')}
                  </Text>
                </Stack>
                <Group justify="space-between" align="center">
                  <Text size="xs" c="dimmed">
                    {dateFormatter.format(new Date(component.createdAt))}
                  </Text>
                  <ComponentAction
                    component={component}
                    archived={archived}
                    pending={pendingComponentId === component.id}
                    onArchive={onArchive}
                    onRestore={onRestore}
                  />
                </Group>
              </Stack>
            </Group>
          </Paper>
        ))}
      </Stack>
    </>
  );
}
