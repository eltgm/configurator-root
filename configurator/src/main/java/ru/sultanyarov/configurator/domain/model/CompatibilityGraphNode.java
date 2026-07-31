package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

@Builder
public record CompatibilityGraphNode(
        Long id,
        String name,
        Long componentTypeId,
        String componentTypeName,
        String brand
) {
}
