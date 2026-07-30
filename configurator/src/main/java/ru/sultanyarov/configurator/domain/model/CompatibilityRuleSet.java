package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record CompatibilityRuleSet(
        Long id,
        Long domainId,
        String name,
        Long componentTypeAId,
        Long componentTypeBId,
        Boolean enabled,
        List<CompatibilityRuleCondition> conditions,
        LocalDateTime createdAt
) {
}
