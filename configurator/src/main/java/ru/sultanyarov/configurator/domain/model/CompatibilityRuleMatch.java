package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.util.List;

@Builder
public record CompatibilityRuleMatch(
        Long ruleSetId,
        String ruleSetName,
        List<CompatibilityConditionExplanation> conditions
) {
}
