import { describe, expect, it } from 'vitest';

import {
  compatibilityPairKey,
  filterManualCompatibilityLinks,
  getAvailableTargetNodes,
  hasAvailableCompatibilityPair,
  hasCompatibilityPair,
  toManualCompatibilityLinks,
} from '@/features/compatibility/model/manual-compatibility';
import type { GraphResponse } from '@/shared/api';

const graph: GraphResponse = {
  nodes: [
    { id: 3, name: 'Radeon', componentTypeId: 12, componentTypeName: 'Видеокарта', brand: 'AMD' },
    { id: 1, name: 'Ryzen', componentTypeId: 11, componentTypeName: 'Процессор', brand: 'AMD' },
    { id: 2, name: 'B650', componentTypeId: 13, componentTypeName: 'Материнская плата' },
  ],
  edges: [{ id: 101, source: 1, target: 2, comment: 'AM5' }],
};

describe('manual compatibility model', () => {
  it('normalizes an undirected pair regardless of the submitted order', () => {
    expect(compatibilityPairKey(8, 3)).toBe('3:8');
    expect(compatibilityPairKey(3, 8)).toBe('3:8');
    expect(hasCompatibilityPair(graph.edges, 2, 1)).toBe(true);
  });

  it('excludes the source and existing neighbours from available targets', () => {
    expect(getAvailableTargetNodes(graph, 1).map((node) => node.id)).toEqual([3]);
    expect(getAvailableTargetNodes(graph, null)).toEqual([]);
    expect(hasAvailableCompatibilityPair(graph)).toBe(true);
    expect(
      hasAvailableCompatibilityPair({
        nodes: graph.nodes.slice(0, 2),
        edges: [{ id: 102, source: 3, target: 1 }],
      }),
    ).toBe(false);
  });

  it('indexes valid edges, ignores missing nodes and sorts links by component names', () => {
    const links = toManualCompatibilityLinks({
      nodes: graph.nodes,
      edges: [
        { id: 103, source: 3, target: 1 },
        { id: 102, source: 2, target: 3 },
        { id: 999, source: 1, target: 99 },
      ],
    });

    expect(links.map((link) => link.edge.id)).toEqual([102, 103]);
  });

  it('searches both components, metadata and comments without mutating the source', () => {
    const links = toManualCompatibilityLinks(graph);

    expect(filterManualCompatibilityLinks(links, 'материнская')).toHaveLength(1);
    expect(filterManualCompatibilityLinks(links, 'amd')).toHaveLength(1);
    expect(filterManualCompatibilityLinks(links, ' am5 ')).toHaveLength(1);
    expect(filterManualCompatibilityLinks(links, 'intel')).toEqual([]);
    expect(filterManualCompatibilityLinks(links, '')).not.toBe(links);
  });
});
