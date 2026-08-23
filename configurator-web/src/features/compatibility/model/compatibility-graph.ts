import type { GraphEdge, GraphNode, GraphResponse } from '@/shared/api';

const TYPE_COLOR_PALETTE = [
  'indigo',
  'teal',
  'orange',
  'grape',
  'cyan',
  'pink',
  'lime',
  'blue',
] as const;

export interface CompatibilityGraphIndex {
  nodes: GraphNode[];
  edges: GraphEdge[];
  nodeById: ReadonlyMap<number, GraphNode>;
  edgeById: ReadonlyMap<number, GraphEdge>;
  neighborIdsByNodeId: ReadonlyMap<number, ReadonlySet<number>>;
  edgeIdsByNodeId: ReadonlyMap<number, ReadonlySet<number>>;
}

export interface CompatibilityTypeLegendItem {
  componentTypeId: number;
  label: string;
  color: (typeof TYPE_COLOR_PALETTE)[number];
}

export interface CompatibilityGraphSelection {
  nodeId?: number;
  edgeId?: number;
}

export interface CompatibilityGraphHighlight {
  highlightedNodeIds: ReadonlySet<number>;
  highlightedEdgeIds: ReadonlySet<number>;
  hasSelection: boolean;
}

function compareNodes(left: GraphNode, right: GraphNode) {
  return (
    left.componentTypeName.localeCompare(right.componentTypeName) ||
    left.name.localeCompare(right.name) ||
    left.id - right.id
  );
}

export function buildCompatibilityGraphIndex(graph: GraphResponse): CompatibilityGraphIndex {
  const nodes = [...graph.nodes].sort(compareNodes);
  const nodeById = new Map(nodes.map((node) => [node.id, node]));
  const neighborIdsByNodeId = new Map<number, Set<number>>();
  const edgeIdsByNodeId = new Map<number, Set<number>>();

  for (const node of nodes) {
    neighborIdsByNodeId.set(node.id, new Set());
    edgeIdsByNodeId.set(node.id, new Set());
  }

  const seenEdgeIds = new Set<number>();
  const edges = graph.edges
    .filter((edge) => {
      if (
        seenEdgeIds.has(edge.id) ||
        edge.source === edge.target ||
        !nodeById.has(edge.source) ||
        !nodeById.has(edge.target)
      ) {
        return false;
      }
      seenEdgeIds.add(edge.id);
      return true;
    })
    .sort((left, right) => left.id - right.id);

  for (const edge of edges) {
    neighborIdsByNodeId.get(edge.source)?.add(edge.target);
    neighborIdsByNodeId.get(edge.target)?.add(edge.source);
    edgeIdsByNodeId.get(edge.source)?.add(edge.id);
    edgeIdsByNodeId.get(edge.target)?.add(edge.id);
  }

  return {
    nodes,
    edges,
    nodeById,
    edgeById: new Map(edges.map((edge) => [edge.id, edge])),
    neighborIdsByNodeId,
    edgeIdsByNodeId,
  };
}

export function buildCompatibilityTypeLegend(
  index: CompatibilityGraphIndex,
): CompatibilityTypeLegendItem[] {
  const types = new Map<number, string>();
  for (const node of index.nodes) {
    if (!types.has(node.componentTypeId)) {
      types.set(node.componentTypeId, node.componentTypeName);
    }
  }

  return [...types.entries()]
    .sort((left, right) => left[1].localeCompare(right[1]) || left[0] - right[0])
    .map(([componentTypeId, label], indexValue) => ({
      componentTypeId,
      label,
      color: TYPE_COLOR_PALETTE[indexValue % TYPE_COLOR_PALETTE.length]!,
    }));
}

export function getCompatibilityGraphHighlight(
  index: CompatibilityGraphIndex,
  selection: CompatibilityGraphSelection,
): CompatibilityGraphHighlight {
  if (selection.nodeId !== undefined && index.nodeById.has(selection.nodeId)) {
    return {
      highlightedNodeIds: new Set([
        selection.nodeId,
        ...(index.neighborIdsByNodeId.get(selection.nodeId) ?? []),
      ]),
      highlightedEdgeIds: new Set(index.edgeIdsByNodeId.get(selection.nodeId) ?? []),
      hasSelection: true,
    };
  }

  const edge = selection.edgeId === undefined ? undefined : index.edgeById.get(selection.edgeId);
  if (edge) {
    return {
      highlightedNodeIds: new Set([edge.source, edge.target]),
      highlightedEdgeIds: new Set([edge.id]),
      hasSelection: true,
    };
  }

  return {
    highlightedNodeIds: new Set(index.nodeById.keys()),
    highlightedEdgeIds: new Set(index.edgeById.keys()),
    hasSelection: false,
  };
}

export function getCompatibilityGraphNeighbors(
  index: CompatibilityGraphIndex,
  nodeId: number,
): GraphNode[] {
  return [...(index.neighborIdsByNodeId.get(nodeId) ?? [])]
    .map((neighborId) => index.nodeById.get(neighborId))
    .filter((node): node is GraphNode => node !== undefined)
    .sort(compareNodes);
}

export function countIsolatedCompatibilityNodes(index: CompatibilityGraphIndex) {
  return index.nodes.filter((node) => index.neighborIdsByNodeId.get(node.id)?.size === 0).length;
}

export function getCompatibilityTypeColor(
  legend: CompatibilityTypeLegendItem[],
  componentTypeId: number,
) {
  return legend.find((item) => item.componentTypeId === componentTypeId)?.color ?? 'gray';
}
