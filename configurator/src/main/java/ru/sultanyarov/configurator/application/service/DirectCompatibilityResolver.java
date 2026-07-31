package ru.sultanyarov.configurator.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanation;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanationSource;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.CompatibleComponent;
import ru.sultanyarov.configurator.domain.model.Component;

@UtilityClass
class DirectCompatibilityResolver {
  static Map<Long, List<CompatibleComponent>> resolve(
      Component baseComponent,
      List<Component> candidates,
      List<CompatibilityLink> manualLinks,
      List<CompatibilityRuleSet> automaticRules,
      CompatibilityRuleEvaluator ruleEvaluator) {
    Map<Long, List<CompatibilityRuleSet>> rulesByCandidateType =
        indexRulesByCandidateType(automaticRules, baseComponent.getComponentTypeId());
    Map<Long, List<CompatibilityExplanation>> manualExplanations =
        indexManualExplanations(manualLinks, baseComponent.getId());
    Map<Long, List<CompatibleComponent>> compatibleByType = new HashMap<>();

    for (Component candidate : candidates) {
      List<CompatibilityExplanation> explanations =
          collectExplanations(
              baseComponent, candidate, manualExplanations, rulesByCandidateType, ruleEvaluator);
      if (!explanations.isEmpty()) {
        compatibleByType
            .computeIfAbsent(candidate.getComponentTypeId(), ignored -> new ArrayList<>())
            .add(toCompatibleComponent(candidate, explanations));
      }
    }
    return compatibleByType;
  }

  private static List<CompatibilityExplanation> collectExplanations(
      Component baseComponent,
      Component candidate,
      Map<Long, List<CompatibilityExplanation>> manualExplanations,
      Map<Long, List<CompatibilityRuleSet>> rulesByCandidateType,
      CompatibilityRuleEvaluator ruleEvaluator) {
    List<CompatibilityExplanation> explanations =
        new ArrayList<>(manualExplanations.getOrDefault(candidate.getId(), List.of()));
    explanations.addAll(
        evaluateRules(
            baseComponent,
            candidate,
            rulesByCandidateType.getOrDefault(candidate.getComponentTypeId(), List.of()),
            ruleEvaluator));
    return explanations;
  }

  private static List<CompatibilityExplanation> evaluateRules(
      Component baseComponent,
      Component candidate,
      List<CompatibilityRuleSet> rules,
      CompatibilityRuleEvaluator ruleEvaluator) {
    List<CompatibilityExplanation> explanations = new ArrayList<>();
    for (CompatibilityRuleSet rule : rules) {
      boolean baseIsComponentA = baseComponent.getComponentTypeId().equals(rule.componentTypeAId());
      Component componentA = baseIsComponentA ? baseComponent : candidate;
      Component componentB = baseIsComponentA ? candidate : baseComponent;
      ruleEvaluator
          .evaluate(rule, componentA, componentB)
          .map(CompatibilityExplanations::automatic)
          .ifPresent(explanations::add);
    }
    return explanations;
  }

  private static Map<Long, List<CompatibilityExplanation>> indexManualExplanations(
      List<CompatibilityLink> links, Long baseComponentId) {
    Map<Long, List<CompatibilityExplanation>> result = new HashMap<>();
    for (CompatibilityLink link : links) {
      Long candidateId =
          baseComponentId.equals(link.componentAId()) ? link.componentBId() : link.componentAId();
      result
          .computeIfAbsent(candidateId, ignored -> new ArrayList<>())
          .add(
              CompatibilityExplanation.builder()
                  .source(CompatibilityExplanationSource.MANUAL)
                  .linkId(link.id())
                  .comment(link.comment())
                  .build());
    }
    return result;
  }

  private static Map<Long, List<CompatibilityRuleSet>> indexRulesByCandidateType(
      List<CompatibilityRuleSet> rules, Long baseComponentTypeId) {
    Map<Long, List<CompatibilityRuleSet>> result = new HashMap<>();
    for (CompatibilityRuleSet rule : rules) {
      Long candidateTypeId =
          baseComponentTypeId.equals(rule.componentTypeAId())
              ? rule.componentTypeBId()
              : rule.componentTypeAId();
      result.computeIfAbsent(candidateTypeId, ignored -> new ArrayList<>()).add(rule);
    }
    return result;
  }

  private static CompatibleComponent toCompatibleComponent(
      Component component, List<CompatibilityExplanation> explanations) {
    return CompatibleComponent.builder()
        .id(component.getId())
        .name(component.getName())
        .brand(component.getBrand())
        .componentTypeId(component.getComponentTypeId())
        .explanations(List.copyOf(explanations))
        .build();
  }
}
