import { describe, expect, it } from 'vitest';

import {
  buildCompatibilityGraphIndex,
  buildCompatibilityTypeLegend,
  countIsolatedCompatibilityNodes,
  getCompatibilityGraphHighlight,
  getCompatibilityGraphNeighbors,
  getCompatibilityTypeColor,
} from '@/features/compatibility/model/compatibility-graph';
import type { GraphResponse } from '@/shared/api';

const graph: GraphResponse = {
  nodes: [
    { id: 3, name: 'Case', componentTypeId: 13, componentTypeName: 'Case' },
    { id: 2, name: 'Board', componentTypeId: 12, componentTypeName: 'Motherboard' },
    { id: 1, name: 'CPU', componentTypeId: 11, componentTypeName: 'Processor' },
  ],
  edges: [
    { id: 22, source: 1, target: 2 },
    { id: 23, source: 1, target: 99 },
    { id: 22, source: 2, target: 1 },
    { id: 24, source: 3, target: 3 },
  ],
};

describe('compatibility graph model', () => {
  it('indexes valid undirected edges and keeps isolated nodes', () => {
    const index = buildCompatibilityGraphIndex(graph);

    expect(index.nodes.map((node) => node.id)).toEqual([3, 2, 1]);
    expect(index.edges).toEqual([{ id: 22, source: 1, target: 2 }]);
    expect([...index.neighborIdsByNodeId.get(1)!]).toEqual([2]);
    expect([...index.neighborIdsByNodeId.get(2)!]).toEqual([1]);
    expect(index.neighborIdsByNodeId.get(3)?.size).toBe(0);
    expect(countIsolatedCompatibilityNodes(index)).toBe(1);
    expect(graph.edges).toHaveLength(4);
  });

  it('builds deterministic colors and sorted neighbor details', () => {
    const index = buildCompatibilityGraphIndex(graph);
    const legend = buildCompatibilityTypeLegend(index);

    expect(legend.map((item) => item.label)).toEqual(['Case', 'Motherboard', 'Processor']);
    expect(buildCompatibilityTypeLegend(index)).toEqual(legend);
    expect(getCompatibilityTypeColor(legend, 12)).toBe('teal');
    expect(getCompatibilityTypeColor(legend, 999)).toBe('gray');
    expect(getCompatibilityGraphNeighbors(index, 1).map((node) => node.name)).toEqual(['Board']);
  });

  it('highlights a selected node with its neighbors and incident edges', () => {
    const index = buildCompatibilityGraphIndex(graph);
    const highlight = getCompatibilityGraphHighlight(index, { nodeId: 1 });

    expect(highlight.hasSelection).toBe(true);
    expect([...highlight.highlightedNodeIds]).toEqual([1, 2]);
    expect([...highlight.highlightedEdgeIds]).toEqual([22]);
  });

  it('highlights endpoints for an edge and falls back for a stale selection', () => {
    const index = buildCompatibilityGraphIndex(graph);

    expect([...getCompatibilityGraphHighlight(index, { edgeId: 22 }).highlightedNodeIds]).toEqual([
      1, 2,
    ]);
    expect(getCompatibilityGraphHighlight(index, { nodeId: 999 }).hasSelection).toBe(false);
  });
});
