import {
  forceCenter,
  forceCollide,
  forceLink,
  forceManyBody,
  forceSimulation,
  type SimulationLinkDatum,
  type SimulationNodeDatum,
} from 'd3-force';

import type { GraphResponse } from '@/shared/api';

export const COMPATIBILITY_NODE_WIDTH = 232;
export const COMPATIBILITY_NODE_HEIGHT = 88;
const LAYOUT_PADDING = 32;

export interface CompatibilityNodePosition {
  x: number;
  y: number;
}

interface LayoutNode extends SimulationNodeDatum {
  id: number;
}

function seededRandom(seed: number) {
  let state = seed >>> 0;
  return () => {
    state = (state * 1_664_525 + 1_013_904_223) >>> 0;
    return state / 4_294_967_296;
  };
}

export function calculateCompatibilityGraphLayout(
  graph: GraphResponse,
): ReadonlyMap<number, CompatibilityNodePosition> {
  const sourceNodes = [...graph.nodes].sort((left, right) => left.id - right.id);
  if (sourceNodes.length === 0) {
    return new Map();
  }
  if (sourceNodes.length === 1) {
    return new Map([[sourceNodes[0]!.id, { x: LAYOUT_PADDING, y: LAYOUT_PADDING }]]);
  }

  const radius = Math.max(180, sourceNodes.length * 34);
  const nodes: LayoutNode[] = sourceNodes.map((node, index) => {
    const angle = (index / sourceNodes.length) * Math.PI * 2;
    return { id: node.id, x: Math.cos(angle) * radius, y: Math.sin(angle) * radius };
  });
  const nodeIds = new Set(nodes.map((node) => node.id));
  const links: SimulationLinkDatum<LayoutNode>[] = graph.edges
    .filter(
      (edge) => edge.source !== edge.target && nodeIds.has(edge.source) && nodeIds.has(edge.target),
    )
    .map((edge) => ({ source: edge.source, target: edge.target }));

  const simulation = forceSimulation(nodes)
    .randomSource(seededRandom(0x9_18_20_26))
    .force(
      'link',
      forceLink<LayoutNode, SimulationLinkDatum<LayoutNode>>(links)
        .id((node) => node.id)
        .distance(250)
        .strength(0.42),
    )
    .force('charge', forceManyBody().strength(-760))
    .force('collision', forceCollide(Math.max(COMPATIBILITY_NODE_WIDTH, 150) / 2 + 18))
    .force('center', forceCenter(0, 0).strength(0.08))
    .stop();

  for (let tick = 0; tick < 260; tick += 1) {
    simulation.tick();
  }
  simulation.stop();

  const minX = Math.min(...nodes.map((node) => node.x ?? 0));
  const minY = Math.min(...nodes.map((node) => node.y ?? 0));
  return new Map(
    nodes.map((node) => [
      node.id,
      {
        x: (node.x ?? 0) - minX + LAYOUT_PADDING,
        y: (node.y ?? 0) - minY + LAYOUT_PADDING,
      },
    ]),
  );
}
