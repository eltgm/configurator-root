package ru.sultanyarov.configurator.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import ru.sultanyarov.configurator.application.port.out.CompatibilityRuleRepository;
import ru.sultanyarov.configurator.application.port.out.ConfiguratorRepository;
import ru.sultanyarov.configurator.domain.exception.ConfigurationConflictException;
import ru.sultanyarov.configurator.domain.model.Component;

@org.springframework.stereotype.Component
@RequiredArgsConstructor
class ConfigurationCompatibilityValidator {
  private final ConfiguratorRepository configuratorRepository;
  private final CompatibilityRuleRepository compatibilityRuleRepository;
  private final CompatibilityRuleEvaluator compatibilityRuleEvaluator;

  void validatePairwiseDirectCompatibility(Long domainId, List<Component> components) {
    if (components.size() < 2) {
      return;
    }
    CompatibilityGraphContext graphContext =
        CompatibilityGraphBuilder.build(
            components,
            configuratorRepository.getAllManualCompatibilityLinks(domainId),
            compatibilityRuleRepository.getEnabledByDomainId(domainId),
            compatibilityRuleEvaluator);

    for (int leftIndex = 0; leftIndex < components.size(); leftIndex++) {
      for (int rightIndex = leftIndex + 1; rightIndex < components.size(); rightIndex++) {
        Component left = components.get(leftIndex);
        Component right = components.get(rightIndex);
        if (!hasDirectEdge(graphContext, left.getId(), right.getId())) {
          throw new ConfigurationConflictException(
              "Components with ids {} and {} are not directly compatible",
              left.getId(),
              right.getId());
        }
      }
    }
  }

  private static boolean hasDirectEdge(
      CompatibilityGraphContext graphContext, Long sourceId, Long targetId) {
    return graphContext.graph().getOrDefault(sourceId, java.util.Map.of()).containsKey(targetId);
  }
}
