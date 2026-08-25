package ru.sultanyarov.configurator.application.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import ru.sultanyarov.configurator.application.port.out.CompatibilityRuleRepository;
import ru.sultanyarov.configurator.application.port.out.ConfiguratorRepository;
import ru.sultanyarov.configurator.domain.exception.ConfigurationConflictException;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ConfiguratorAssemblyStatus;
import ru.sultanyarov.configurator.domain.model.PairCompatibilityStatus;

@org.springframework.stereotype.Component
@RequiredArgsConstructor
class ConfigurationCompatibilityValidator {
  private final ConfiguratorRepository configuratorRepository;
  private final CompatibilityRuleRepository compatibilityRuleRepository;
  private final CompatibilityRuleEvaluator compatibilityRuleEvaluator;

  void validateAssemblyCompatibility(Long domainId, List<Component> components) {
    if (components.size() < 2) {
      return;
    }
    CompatibilityDecisionResolver decisionResolver =
        CompatibilityDecisionResolver.create(
            configuratorRepository.getAllManualCompatibilityLinks(domainId),
            compatibilityRuleRepository.getEnabledByDomainId(domainId),
            compatibilityRuleEvaluator);
    AssemblyCompatibilityEvaluation evaluation =
        AssemblyCompatibilityEvaluator.evaluate(components, decisionResolver);
    if (evaluation.status() == ConfiguratorAssemblyStatus.BLOCKED) {
      var blockedPair =
          evaluation.pairDecisions().stream()
              .filter(decision -> decision.status() == PairCompatibilityStatus.DENIED)
              .findFirst()
              .orElseThrow();
      String blockingRuleNames =
          blockedPair.blockingRules().stream()
              .map(rule -> rule.ruleSetName() == null ? "#" + rule.ruleSetId() : rule.ruleSetName())
              .collect(Collectors.joining(", "));
      throw new ConfigurationConflictException(
          "Components with ids {} and {} are blocked by compatibility rules: {}",
          blockedPair.leftComponentId(),
          blockedPair.rightComponentId(),
          blockingRuleNames);
    }
    if (evaluation.status() == ConfiguratorAssemblyStatus.DISCONNECTED) {
      throw new ConfigurationConflictException(
          "Component with id {} is not connected to the assembly containing component with id {}",
          evaluation.disconnectedComponentId(),
          components.getFirst().getId());
    }
  }
}
