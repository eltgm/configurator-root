package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

@Builder
public record CompatibilityGraphEdge(
        Long id,
        Long source,
        Long target,
        String comment
) {
}
