import {
  ActionIcon,
  Badge,
  Button,
  Group,
  Paper,
  Select,
  Stack,
  Text,
  Tooltip,
} from '@mantine/core';
import { IconFocusCentered, IconRefresh, IconSearch } from '@tabler/icons-react';
import {
  Background,
  Controls,
  MiniMap,
  ReactFlow,
  ReactFlowProvider,
  useNodesState,
  useReactFlow,
  type Edge,
  type NodeTypes,
} from '@xyflow/react';
import { useReducedMotion } from '@mantine/hooks';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';

import {
  buildCompatibilityTypeLegend,
  getCompatibilityGraphHighlight,
  getCompatibilityTypeColor,
  type CompatibilityGraphIndex,
  type CompatibilityGraphSelection,
} from '@/features/compatibility/model/compatibility-graph';
import {
  calculateCompatibilityGraphLayout,
  COMPATIBILITY_NODE_HEIGHT,
  COMPATIBILITY_NODE_WIDTH,
} from '@/features/compatibility/model/compatibility-graph-layout';
import {
  CompatibilityGraphNode,
  type CompatibilityFlowNode,
} from '@/features/compatibility/ui/CompatibilityGraphNode';

import { CompatibilityGraphDetails } from './CompatibilityGraphDetails';
import classes from './compatibility-graph.module.css';
import '@xyflow/react/dist/style.css';

const nodeTypes: NodeTypes = { compatibility: CompatibilityGraphNode };

interface CompatibilityGraphCanvasProps {
  index: CompatibilityGraphIndex;
}

function CompatibilityGraphCanvasInner({ index }: CompatibilityGraphCanvasProps) {
  const { t } = useTranslation();
  const reduceMotion = useReducedMotion();
  const motionDuration = reduceMotion ? 0 : 350;
  const { fitView, setCenter } = useReactFlow<CompatibilityFlowNode>();
  const legend = useMemo(() => buildCompatibilityTypeLegend(index), [index]);
  const layout = useMemo(
    () => calculateCompatibilityGraphLayout({ nodes: index.nodes, edges: index.edges }),
    [index],
  );
  const [selection, setSelection] = useState<CompatibilityGraphSelection>({});
  const highlight = useMemo(
    () => getCompatibilityGraphHighlight(index, selection),
    [index, selection],
  );

  const createNodes = useCallback(
    (): CompatibilityFlowNode[] =>
      index.nodes.map((node) => ({
        id: String(node.id),
        type: 'compatibility',
        position: layout.get(node.id) ?? { x: 0, y: 0 },
        width: COMPATIBILITY_NODE_WIDTH,
        height: COMPATIBILITY_NODE_HEIGHT,
        ariaLabel: t('compatibilityGraph.a11y.node', {
          name: node.name,
          type: node.componentTypeName,
        }),
        data: {
          name: node.name,
          componentTypeName: node.componentTypeName,
          ...(node.brand ? { brand: node.brand } : {}),
          color: getCompatibilityTypeColor(legend, node.componentTypeId),
          isolated: index.neighborIdsByNodeId.get(node.id)?.size === 0,
          highlighted: highlight.highlightedNodeIds.has(node.id),
          dimmed: highlight.hasSelection && !highlight.highlightedNodeIds.has(node.id),
          isolatedLabel: t('compatibilityGraph.node.isolated'),
        },
      })),
    [highlight, index, layout, legend, t],
  );
  const [nodes, setNodes, onNodesChange] = useNodesState<CompatibilityFlowNode>(createNodes());

  useEffect(() => {
    setNodes((currentNodes) =>
      currentNodes.map((node) => ({
        ...node,
        data: {
          ...node.data,
          highlighted: highlight.highlightedNodeIds.has(Number(node.id)),
          dimmed: highlight.hasSelection && !highlight.highlightedNodeIds.has(Number(node.id)),
        },
      })),
    );
  }, [highlight, setNodes]);

  useEffect(() => {
    const clearSelection = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setSelection({});
      }
    };
    window.addEventListener('keydown', clearSelection);
    return () => window.removeEventListener('keydown', clearSelection);
  }, []);

  const edges = useMemo<Edge[]>(
    () =>
      index.edges.map((edge) => {
        const sourceName = index.nodeById.get(edge.source)?.name ?? String(edge.source);
        const targetName = index.nodeById.get(edge.target)?.name ?? String(edge.target);
        const isHighlighted = highlight.highlightedEdgeIds.has(edge.id);
        return {
          id: String(edge.id),
          source: String(edge.source),
          target: String(edge.target),
          ariaLabel: t('compatibilityGraph.a11y.edge', { source: sourceName, target: targetName }),
          focusable: true,
          selectable: true,
          interactionWidth: 24,
          className: `${classes['edge']} ${
            highlight.hasSelection && !isHighlighted ? classes['edge-dimmed'] : ''
          } ${isHighlighted && highlight.hasSelection ? classes['edge-highlighted'] : ''}`,
          style: { strokeWidth: isHighlighted && highlight.hasSelection ? 3 : 2 },
        };
      }),
    [highlight, index, t],
  );

  const selectNode = useCallback(
    (nodeId: number, center = true) => {
      setSelection({ nodeId });
      if (center) {
        const node = nodes.find((item) => Number(item.id) === nodeId);
        if (node) {
          void setCenter(
            node.position.x + COMPATIBILITY_NODE_WIDTH / 2,
            node.position.y + COMPATIBILITY_NODE_HEIGHT / 2,
            { zoom: 1.15, duration: motionDuration },
          );
        }
      }
    },
    [motionDuration, nodes, setCenter],
  );

  const resetLayout = () => {
    setSelection({});
    setNodes(createNodes());
    window.requestAnimationFrame(() => {
      void fitView({ padding: 0.18, duration: motionDuration });
    });
  };

  const searchOptions = index.nodes.map((node) => ({
    value: String(node.id),
    label: `${node.name} · ${node.componentTypeName}${node.brand ? ` · ${node.brand}` : ''}`,
  }));

  return (
    <Stack gap="md">
      <Paper p="md" radius="md" withBorder>
        <Group align="flex-end" justify="space-between">
          <Select
            className={classes['search']}
            label={t('compatibilityGraph.search.label')}
            placeholder={t('compatibilityGraph.search.placeholder')}
            leftSection={<IconSearch size={17} />}
            data={searchOptions}
            value={selection.nodeId === undefined ? null : String(selection.nodeId)}
            onChange={(value) => {
              if (value) {
                selectNode(Number(value));
              } else {
                setSelection({});
              }
            }}
            searchable
            clearable
            nothingFoundMessage={t('compatibilityGraph.search.nothingFound')}
          />
          <Group gap="xs">
            <Tooltip label={t('compatibilityGraph.actions.fit')}>
              <ActionIcon
                variant="default"
                size="lg"
                aria-label={t('compatibilityGraph.actions.fit')}
                onClick={() => void fitView({ padding: 0.18, duration: motionDuration })}
              >
                <IconFocusCentered size={18} />
              </ActionIcon>
            </Tooltip>
            <Button variant="default" leftSection={<IconRefresh size={17} />} onClick={resetLayout}>
              {t('compatibilityGraph.actions.reset')}
            </Button>
          </Group>
        </Group>
        <Text id="compatibility-graph-instructions" size="xs" c="dimmed" mt="sm">
          {t('compatibilityGraph.a11y.instructions')}
        </Text>
      </Paper>

      <div className={classes['explorer']}>
        <Paper
          className={classes['canvas']}
          radius="md"
          withBorder
          data-testid="compatibility-graph-canvas"
        >
          <ReactFlow<CompatibilityFlowNode>
            nodes={nodes}
            edges={edges}
            nodeTypes={nodeTypes}
            onNodesChange={onNodesChange}
            onNodeClick={(_event, node) => selectNode(Number(node.id), false)}
            onEdgeClick={(_event, edge) => setSelection({ edgeId: Number(edge.id) })}
            onPaneClick={() => setSelection({})}
            nodesConnectable={false}
            nodesDraggable={false}
            edgesReconnectable={false}
            deleteKeyCode={null}
            multiSelectionKeyCode={null}
            selectionKeyCode={null}
            fitView
            fitViewOptions={{ padding: 0.18 }}
            minZoom={0.25}
            maxZoom={2}
            aria-describedby="compatibility-graph-instructions"
            ariaLabelConfig={{
              'controls.ariaLabel': t('compatibilityGraph.a11y.controls'),
              'controls.zoomIn.ariaLabel': t('compatibilityGraph.a11y.zoomIn'),
              'controls.zoomOut.ariaLabel': t('compatibilityGraph.a11y.zoomOut'),
              'controls.fitView.ariaLabel': t('compatibilityGraph.a11y.fit'),
              'minimap.ariaLabel': t('compatibilityGraph.a11y.minimap'),
            }}
          >
            <Background gap={22} size={1} />
            <Controls aria-label={t('compatibilityGraph.a11y.controls')} showInteractive={false} />
            <MiniMap<CompatibilityFlowNode>
              className={classes['minimap']}
              ariaLabel={t('compatibilityGraph.a11y.minimap')}
              nodeColor={(node) => `var(--mantine-color-${node.data.color}-6)`}
              nodeBorderRadius={8}
            />
          </ReactFlow>
        </Paper>

        <aside className={classes['details']} aria-label={t('compatibilityGraph.details.panel')}>
          <CompatibilityGraphDetails
            index={index}
            legend={legend}
            selection={selection}
            onSelectNode={selectNode}
          />
          <Paper p="md" radius="md" withBorder>
            <Stack gap="xs">
              <Text fw={700} size="sm">
                {t('compatibilityGraph.legend.title')}
              </Text>
              <Group gap="xs">
                {legend.map((item) => (
                  <Badge key={item.componentTypeId} color={item.color} variant="light">
                    {item.label}
                  </Badge>
                ))}
              </Group>
            </Stack>
          </Paper>
        </aside>
      </div>
    </Stack>
  );
}

export function CompatibilityGraphCanvas(props: CompatibilityGraphCanvasProps) {
  return (
    <ReactFlowProvider>
      <CompatibilityGraphCanvasInner {...props} />
    </ReactFlowProvider>
  );
}
