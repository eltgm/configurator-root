package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.util.List;

@Builder
public record IntersectionCompatibleComponent(
        Long id,
        String name,
        String brand,
        Long componentTypeId,
        List<BaseComponentCompatibility> compatibilityByBase
) {
}
