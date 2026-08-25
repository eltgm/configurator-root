package ru.sultanyarov.configurator.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.sultanyarov.configurator.domain.model.CompatibilityBlockingRule;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanation;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanationSource;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.PairCompatibilityDecision;
import ru.sultanyarov.configurator.domain.model.PairCompatibilityStatus;

final class CompatibilityDecisionResolver {
  private final Map<ComponentPair, List<CompatibilityExplanation>> manualByPair;
  private final Map<ComponentTypePair, List<CompatibilityRuleSet>> rulesByTypePair;
  private final CompatibilityRuleEvaluator ruleEvaluator;

  private CompatibilityDecisionResolver(
      Map<ComponentPair, List<CompatibilityExplanation>> manualByPair,
      Map<ComponentTypePair, List<CompatibilityRuleSet>> rulesByTypePair,
      CompatibilityRuleEvaluator ruleEvaluator) {
    this.manualByPair = manualByPair;
    this.rulesByTypePair = rulesByTypePair;
    this.ruleEvaluator = ruleEvaluator;
  }

  static CompatibilityDecisionResolver create(
      List<CompatibilityLink> manualLinks,
      List<CompatibilityRuleSet> automaticRules,
      CompatibilityRuleEvaluator ruleEvaluator) {
    return new CompatibilityDecisionResolver(
        indexManualLinks(manualLinks), indexRules(automaticRules), ruleEvaluator);
  }

  PairCompatibilityDecision resolve(Component left, Component right) {
    List<CompatibilityExplanation> manualExplanations =
        manualByPair.getOrDefault(ComponentPair.of(left.getId(), right.getId()), List.of());
    List<CompatibilityRuleSet> applicableRules =
        rulesByTypePair.getOrDefault(
            ComponentTypePair.of(left.getComponentTypeId(), right.getComponentTypeId()), List.of());
    List<CompatibilityExplanation> automaticExplanations =
        evaluateRules(left, right, applicableRules);

    if (!automaticExplanations.isEmpty()) {
      List<CompatibilityExplanation> explanations =
          new ArrayList<>(manualExplanations.size() + automaticExplanations.size());
      explanations.addAll(manualExplanations);
      explanations.addAll(automaticExplanations);
      return decision(PairCompatibilityStatus.ALLOWED, explanations, List.of());
    }
    if (!applicableRules.isEmpty()) {
      List<CompatibilityBlockingRule> blockingRules =
          applicableRules.stream()
              .map(
                  rule ->
                      CompatibilityBlockingRule.builder()
                          .ruleSetId(rule.id())
                          .ruleSetName(rule.name())
                          .build())
              .toList();
      return decision(PairCompatibilityStatus.DENIED, List.of(), blockingRules);
    }
    if (!manualExplanations.isEmpty()) {
      return decision(PairCompatibilityStatus.ALLOWED, manualExplanations, List.of());
    }
    return decision(PairCompatibilityStatus.UNKNOWN, List.of(), List.of());
  }

  private List<CompatibilityExplanation> evaluateRules(
      Component left, Component right, List<CompatibilityRuleSet> rules) {
    List<CompatibilityExplanation> explanations = new ArrayList<>();
    for (CompatibilityRuleSet rule : rules) {
      boolean leftIsComponentA =
          left.getComponentTypeId().equals(rule.componentTypeAId())
              && right.getComponentTypeId().equals(rule.componentTypeBId());
      Component componentA = leftIsComponentA ? left : right;
      Component componentB = leftIsComponentA ? right : left;
      ruleEvaluator
          .evaluate(rule, componentA, componentB)
          .map(CompatibilityExplanations::automatic)
          .ifPresent(explanations::add);
    }
    return List.copyOf(explanations);
  }

  private static PairCompatibilityDecision decision(
      PairCompatibilityStatus status,
      List<CompatibilityExplanation> explanations,
      List<CompatibilityBlockingRule> blockingRules) {
    return PairCompatibilityDecision.builder()
        .status(status)
        .explanations(List.copyOf(explanations))
        .blockingRules(List.copyOf(blockingRules))
        .build();
  }

  private static Map<ComponentPair, List<CompatibilityExplanation>> indexManualLinks(
      List<CompatibilityLink> links) {
    Map<ComponentPair, List<CompatibilityExplanation>> result = new HashMap<>();
    for (CompatibilityLink link : links) {
      result
          .computeIfAbsent(
              ComponentPair.of(link.componentAId(), link.componentBId()),
              ignored -> new ArrayList<>())
          .add(
              CompatibilityExplanation.builder()
                  .source(CompatibilityExplanationSource.MANUAL)
                  .linkId(link.id())
                  .comment(link.comment())
                  .build());
    }
    return result;
  }

  private static Map<ComponentTypePair, List<CompatibilityRuleSet>> indexRules(
      List<CompatibilityRuleSet> rules) {
    Map<ComponentTypePair, List<CompatibilityRuleSet>> result = new HashMap<>();
    for (CompatibilityRuleSet rule : rules) {
      if (!Boolean.TRUE.equals(rule.enabled())) {
        continue;
      }
      result
          .computeIfAbsent(
              ComponentTypePair.of(rule.componentTypeAId(), rule.componentTypeBId()),
              ignored -> new ArrayList<>())
          .add(rule);
    }
    return result;
  }

  private record ComponentPair(Long first, Long second) {
    private static ComponentPair of(Long left, Long right) {
      return left <= right ? new ComponentPair(left, right) : new ComponentPair(right, left);
    }
  }

  private record ComponentTypePair(Long first, Long second) {
    private static ComponentTypePair of(Long left, Long right) {
      return left <= right
          ? new ComponentTypePair(left, right)
          : new ComponentTypePair(right, left);
    }
  }
}
