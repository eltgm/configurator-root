package ru.sultanyarov.configurator.application.service;

import lombok.experimental.UtilityClass;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanation;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanationSource;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleMatch;

@UtilityClass
class CompatibilityExplanations {
  static CompatibilityExplanation automatic(CompatibilityRuleMatch match) {
    return CompatibilityExplanation.builder()
        .source(CompatibilityExplanationSource.AUTOMATIC)
        .ruleSetId(match.ruleSetId())
        .ruleSetName(match.ruleSetName())
        .conditions(match.conditions())
        .build();
  }
}
