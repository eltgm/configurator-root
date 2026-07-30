package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

@Builder
public record CompatibilityConditionExplanation(
        Long leftAttributeDefinitionId,
        String leftAttributeName,
        String leftValue,
        CompatibilityRuleOperator operator,
        Long rightAttributeDefinitionId,
        String rightAttributeName,
        String rightValue
) {
}
