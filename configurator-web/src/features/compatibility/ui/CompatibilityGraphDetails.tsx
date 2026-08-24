import { Anchor, Badge, Button, Divider, Group, Paper, Stack, Text, Title } from '@mantine/core';
import { IconArrowRight, IconCirclesRelation } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import {
  getCompatibilityGraphNeighbors,
  getCompatibilityTypeColor,
  type CompatibilityGraphIndex,
  type CompatibilityGraphSelection,
  type CompatibilityTypeLegendItem,
} from '@/features/compatibility/model/compatibility-graph';

interface CompatibilityGraphDetailsProps {
  index: CompatibilityGraphIndex;
  legend: CompatibilityTypeLegendItem[];
  selection: CompatibilityGraphSelection;
  onSelectNode: (nodeId: number) => void;
}

export function CompatibilityGraphDetails({
  index,
  legend,
  selection,
  onSelectNode,
}: CompatibilityGraphDetailsProps) {
  const { t } = useTranslation();
  const node = selection.nodeId === undefined ? undefined : index.nodeById.get(selection.nodeId);
  const edge = selection.edgeId === undefined ? undefined : index.edgeById.get(selection.edgeId);

  if (node) {
    const neighbors = getCompatibilityGraphNeighbors(index, node.id);
    return (
      <Paper p="lg" radius="md" withBorder aria-live="polite">
        <Stack gap="md">
          <Stack gap={5}>
            <Text c="dimmed" size="xs" fw={700} tt="uppercase">
              {t('compatibilityGraph.details.component')}
            </Text>
            <Title order={2} size="h3">
              {node.name}
            </Title>
            <Group gap="xs">
              <Badge
                color={getCompatibilityTypeColor(legend, node.componentTypeId)}
                variant="light"
              >
                {node.componentTypeName}
              </Badge>
              {node.brand ? <Text size="sm">{node.brand}</Text> : null}
            </Group>
          </Stack>
          <Divider />
          <Stack gap="xs">
            <Text fw={600} size="sm">
              {t('compatibilityGraph.details.neighbors', { count: neighbors.length })}
            </Text>
            {neighbors.length > 0 ? (
              neighbors.map((neighbor) => (
                <Button
                  key={neighbor.id}
                  variant="subtle"
                  justify="space-between"
                  rightSection={<IconArrowRight size={16} />}
                  onClick={() => onSelectNode(neighbor.id)}
                >
                  {neighbor.name}
                </Button>
              ))
            ) : (
              <Text c="dimmed" size="sm">
                {t('compatibilityGraph.details.isolated')}
              </Text>
            )}
          </Stack>
          <Anchor component={Link} to={`/components/${node.id}`} fw={600} size="sm">
            {t('compatibilityGraph.actions.openComponent')}
          </Anchor>
        </Stack>
      </Paper>
    );
  }

  if (edge) {
    const source = index.nodeById.get(edge.source);
    const target = index.nodeById.get(edge.target);
    return (
      <Paper p="lg" radius="md" withBorder aria-live="polite">
        <Stack gap="md">
          <Stack gap={5}>
            <Text c="dimmed" size="xs" fw={700} tt="uppercase">
              {t('compatibilityGraph.details.link')}
            </Text>
            <Title order={2} size="h3">
              {t('compatibilityGraph.details.linkTitle')}
            </Title>
          </Stack>
          <Button variant="light" onClick={() => onSelectNode(edge.source)}>
            {source?.name}
          </Button>
          <Text ta="center" c="dimmed" size="sm">
            {t('compatibilityGraph.details.compatibleWith')}
          </Text>
          <Button variant="light" onClick={() => onSelectNode(edge.target)}>
            {target?.name}
          </Button>
          <Divider />
          <Stack gap={4}>
            <Text fw={600} size="sm">
              {t('compatibilityGraph.details.comment')}
            </Text>
            <Text {...(!edge.comment ? { c: 'dimmed' } : {})} size="sm">
              {edge.comment || t('compatibilityGraph.details.noComment')}
            </Text>
          </Stack>
        </Stack>
      </Paper>
    );
  }

  return (
    <Paper p="lg" radius="md" withBorder aria-live="polite">
      <Stack align="center" gap="sm" ta="center">
        <IconCirclesRelation size={28} stroke={1.6} />
        <Title order={2} size="h4">
          {t('compatibilityGraph.details.emptyTitle')}
        </Title>
        <Text c="dimmed" size="sm">
          {t('compatibilityGraph.details.emptyDescription')}
        </Text>
      </Stack>
    </Paper>
  );
}
