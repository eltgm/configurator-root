package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

@Builder
public record CompatibilityBlockingRule(Long ruleSetId, String ruleSetName) {}
