package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

@Builder
public record CompatibilityLink(
        Long id,
        Long domainId,
        Long componentAId,
        Long componentBId,
        String comment
) {
}
