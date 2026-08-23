import type { GraphEdge, GraphNode, GraphResponse } from '@/shared/api';

const nodeCollator = new Intl.Collator(undefined, { numeric: true, sensitivity: 'base' });

export interface ManualCompatibilityLinkView {
  edge: GraphEdge;
  componentA: GraphNode;
  componentB: GraphNode;
}

export function compareGraphNodes(left: GraphNode, right: GraphNode): number {
  return (
    nodeCollator.compare(left.name, right.name) ||
    nodeCollator.compare(left.componentTypeName, right.componentTypeName) ||
    left.id - right.id
  );
}

export function sortGraphNodes(nodes: ReadonlyArray<GraphNode>): Array<GraphNode> {
  return [...nodes].sort(compareGraphNodes);
}

export function compatibilityPairKey(firstId: number, secondId: number): string {
  return firstId < secondId ? `${firstId}:${secondId}` : `${secondId}:${firstId}`;
}

export function buildCompatibilityPairSet(edges: ReadonlyArray<GraphEdge>): Set<string> {
  return new Set(edges.map((edge) => compatibilityPairKey(edge.source, edge.target)));
}

export function hasCompatibilityPair(
  edges: ReadonlyArray<GraphEdge>,
  firstId: number,
  secondId: number,
): boolean {
  return buildCompatibilityPairSet(edges).has(compatibilityPairKey(firstId, secondId));
}

export function getAvailableTargetNodes(
  graph: GraphResponse,
  sourceId: number | null,
): Array<GraphNode> {
  if (sourceId === null) {
    return [];
  }
  const existingPairs = buildCompatibilityPairSet(graph.edges);
  return sortGraphNodes(
    graph.nodes.filter(
      (node) => node.id !== sourceId && !existingPairs.has(compatibilityPairKey(sourceId, node.id)),
    ),
  );
}

export function hasAvailableCompatibilityPair(graph: GraphResponse): boolean {
  return graph.nodes.some((node) => getAvailableTargetNodes(graph, node.id).length > 0);
}

export function toManualCompatibilityLinks(
  graph: GraphResponse,
): Array<ManualCompatibilityLinkView> {
  const nodesById = new Map(graph.nodes.map((node) => [node.id, node]));
  return graph.edges
    .flatMap((edge) => {
      const componentA = nodesById.get(edge.source);
      const componentB = nodesById.get(edge.target);
      return componentA && componentB ? [{ edge, componentA, componentB }] : [];
    })
    .sort(
      (left, right) =>
        compareGraphNodes(left.componentA, right.componentA) ||
        compareGraphNodes(left.componentB, right.componentB) ||
        left.edge.id - right.edge.id,
    );
}

export function filterManualCompatibilityLinks(
  links: ReadonlyArray<ManualCompatibilityLinkView>,
  search: string,
): Array<ManualCompatibilityLinkView> {
  const normalizedSearch = search.trim().toLocaleLowerCase();
  if (!normalizedSearch) {
    return [...links];
  }
  return links.filter(({ edge, componentA, componentB }) =>
    [
      componentA.name,
      componentA.componentTypeName,
      componentA.brand,
      componentB.name,
      componentB.componentTypeName,
      componentB.brand,
      edge.comment,
    ].some((value) => value?.toLocaleLowerCase().includes(normalizedSearch)),
  );
}

export function graphNodeOptionLabel(node: GraphNode): string {
  const brand = node.brand ? ` · ${node.brand}` : '';
  return `${node.name} — ${node.componentTypeName}${brand}`;
}
