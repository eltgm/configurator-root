package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.util.List;

@Builder
public record ConfiguratorResult(
        Long baseComponentId,
        List<CompatibleComponentGroup> compatibleByType
) {
}
