package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

@Builder
public record CompatibleComponent(
        Long id,
        String name,
        String brand,
        Long componentTypeId
) {
}
