package ru.sultanyarov.configurator.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.CompatibleComponent;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.PairCompatibilityStatus;

@UtilityClass
class DirectCompatibilityResolver {
  static Map<Long, List<CompatibleComponent>> resolve(
      Component baseComponent,
      List<Component> candidates,
      List<CompatibilityLink> manualLinks,
      List<CompatibilityRuleSet> automaticRules,
      CompatibilityRuleEvaluator ruleEvaluator) {
    CompatibilityDecisionResolver decisionResolver =
        CompatibilityDecisionResolver.create(manualLinks, automaticRules, ruleEvaluator);
    Map<Long, List<CompatibleComponent>> compatibleByType = new HashMap<>();

    for (Component candidate : candidates) {
      var decision = decisionResolver.resolve(baseComponent, candidate);
      if (decision.status() == PairCompatibilityStatus.ALLOWED) {
        compatibleByType
            .computeIfAbsent(candidate.getComponentTypeId(), ignored -> new java.util.ArrayList<>())
            .add(toCompatibleComponent(candidate, decision.explanations()));
      }
    }
    return compatibleByType;
  }

  private static CompatibleComponent toCompatibleComponent(
      Component component,
      List<ru.sultanyarov.configurator.domain.model.CompatibilityExplanation> explanations) {
    return CompatibleComponent.builder()
        .id(component.getId())
        .name(component.getName())
        .brand(component.getBrand())
        .componentTypeId(component.getComponentTypeId())
        .primaryImage(component.getPrimaryImage())
        .explanations(List.copyOf(explanations))
        .build();
  }
}
