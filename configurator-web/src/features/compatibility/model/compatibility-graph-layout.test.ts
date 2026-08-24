import { describe, expect, it } from 'vitest';

import { calculateCompatibilityGraphLayout } from '@/features/compatibility/model/compatibility-graph-layout';
import type { GraphResponse } from '@/shared/api';

const graph: GraphResponse = {
  nodes: [
    { id: 1, name: 'A', componentTypeId: 1, componentTypeName: 'Type A' },
    { id: 2, name: 'B', componentTypeId: 2, componentTypeName: 'Type B' },
    { id: 3, name: 'C', componentTypeId: 3, componentTypeName: 'Type C' },
    { id: 4, name: 'D', componentTypeId: 4, componentTypeName: 'Type D' },
  ],
  edges: [
    { id: 1, source: 1, target: 2 },
    { id: 2, source: 2, target: 3 },
    { id: 3, source: 3, target: 1 },
  ],
};

describe('compatibility graph layout', () => {
  it('is deterministic and does not mutate the API graph', () => {
    const snapshot = structuredClone(graph);

    expect([...calculateCompatibilityGraphLayout(graph)]).toEqual([
      ...calculateCompatibilityGraphLayout(graph),
    ]);
    expect(graph).toEqual(snapshot);
  });

  it('returns finite distinct coordinates for connected and isolated nodes', () => {
    const positions = calculateCompatibilityGraphLayout(graph);
    const values = [...positions.values()];

    expect(values).toHaveLength(4);
    expect(values.every(({ x, y }) => Number.isFinite(x) && Number.isFinite(y))).toBe(true);
    expect(new Set(values.map(({ x, y }) => `${Math.round(x)}:${Math.round(y)}`)).size).toBe(4);
  });

  it('handles empty and single-node graphs', () => {
    expect(calculateCompatibilityGraphLayout({ nodes: [], edges: [] }).size).toBe(0);
    expect(
      calculateCompatibilityGraphLayout({ nodes: [graph.nodes[0]!], edges: [] }).get(1),
    ).toEqual({ x: 32, y: 32 });
  });
});
