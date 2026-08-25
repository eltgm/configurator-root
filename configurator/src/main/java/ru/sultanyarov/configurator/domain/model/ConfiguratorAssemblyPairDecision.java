package ru.sultanyarov.configurator.domain.model;

import java.util.List;
import lombok.Builder;

@Builder
public record ConfiguratorAssemblyPairDecision(
    Long leftComponentId,
    Long rightComponentId,
    PairCompatibilityStatus status,
    List<CompatibilityExplanation> explanations,
    List<CompatibilityBlockingRule> blockingRules) {}
