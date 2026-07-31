package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.util.List;

@Builder
public record BaseComponentCompatibility(
        Long baseComponentId,
        List<CompatibilityExplanation> explanations
) {
}
