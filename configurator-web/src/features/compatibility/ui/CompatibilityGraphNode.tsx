import { Badge, Box, Paper, Stack, Text } from '@mantine/core';
import { Handle, Position, type Node, type NodeProps } from '@xyflow/react';

import classes from './compatibility-graph.module.css';

export interface CompatibilityGraphNodeData extends Record<string, unknown> {
  name: string;
  componentTypeName: string;
  brand?: string;
  color: string;
  isolated: boolean;
  highlighted: boolean;
  dimmed: boolean;
  isolatedLabel: string;
}

export type CompatibilityFlowNode = Node<CompatibilityGraphNodeData, 'compatibility'>;

export function CompatibilityGraphNode({ data }: NodeProps<CompatibilityFlowNode>) {
  return (
    <Paper
      className={`${classes['node']} ${data.highlighted ? classes['node-highlighted'] : ''} ${
        data.dimmed ? classes['node-dimmed'] : ''
      }`}
      p="sm"
      radius="md"
      withBorder
    >
      <Handle
        className={classes['node-handle']}
        type="target"
        position={Position.Top}
        isConnectable={false}
      />
      <Stack gap={5}>
        <Text fw={700} size="sm" lineClamp={1}>
          {data.name}
        </Text>
        <Box className={classes['node-meta']}>
          <Badge color={data.color} variant="light" size="sm">
            {data.componentTypeName}
          </Badge>
          {data.brand ? (
            <Text c="dimmed" size="xs" lineClamp={1}>
              {data.brand}
            </Text>
          ) : null}
        </Box>
        {data.isolated ? (
          <Text className={classes['isolated-label']} size="xs">
            {data.isolatedLabel}
          </Text>
        ) : null}
      </Stack>
      <Handle
        className={classes['node-handle']}
        type="source"
        position={Position.Bottom}
        isConnectable={false}
      />
    </Paper>
  );
}
