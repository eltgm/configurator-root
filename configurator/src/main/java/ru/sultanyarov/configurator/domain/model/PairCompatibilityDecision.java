package ru.sultanyarov.configurator.domain.model;

import java.util.List;
import lombok.Builder;

@Builder
public record PairCompatibilityDecision(
    PairCompatibilityStatus status,
    List<CompatibilityExplanation> explanations,
    List<CompatibilityBlockingRule> blockingRules) {}
