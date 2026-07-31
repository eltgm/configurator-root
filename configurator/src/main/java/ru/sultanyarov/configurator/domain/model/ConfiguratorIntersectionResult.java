package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.util.List;

@Builder
public record ConfiguratorIntersectionResult(
        List<Long> componentIds,
        List<IntersectionCompatibleComponentGroup> compatibleByType
) {
}
