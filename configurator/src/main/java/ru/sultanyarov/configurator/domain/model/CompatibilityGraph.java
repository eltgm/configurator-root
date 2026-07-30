package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.util.List;

@Builder
public record CompatibilityGraph(
        List<CompatibilityGraphNode> nodes,
        List<CompatibilityGraphEdge> edges
) {
}
