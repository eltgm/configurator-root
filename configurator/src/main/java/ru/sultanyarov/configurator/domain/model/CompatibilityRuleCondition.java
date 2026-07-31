package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CompatibilityRuleCondition(
        Long id,
        Long ruleSetId,
        Long leftAttributeDefinitionId,
        CompatibilityRuleOperator operator,
        Long rightAttributeDefinitionId,
        Integer orderIndex,
        LocalDateTime createdAt
) {
}
