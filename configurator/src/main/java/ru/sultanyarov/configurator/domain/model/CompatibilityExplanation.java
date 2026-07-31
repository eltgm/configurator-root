package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.util.List;

@Builder
public record CompatibilityExplanation(
        CompatibilityExplanationSource source,
        Long linkId,
        String comment,
        Long ruleSetId,
        String ruleSetName,
        List<CompatibilityConditionExplanation> conditions,
        List<Long> pathComponentIds
) {
}
