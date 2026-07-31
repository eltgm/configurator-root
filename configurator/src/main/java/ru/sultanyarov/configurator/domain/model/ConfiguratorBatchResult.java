package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.util.List;

@Builder
public record ConfiguratorBatchResult(
        List<ConfiguratorResult> results
) {
}
