package ru.sultanyarov.configurator.application.service;

import ru.sultanyarov.configurator.domain.model.CompatibilityRuleMatch;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.Component;

import java.util.Optional;

/**
 * Evaluates one normalized automatic compatibility rule set against two components.
 */
public interface CompatibilityRuleEvaluator {

    /**
     * @param ruleSet   enabled normalized rule set
     * @param componentA component of rule-set type A
     * @param componentB component of rule-set type B
     * @return matched rule with successful condition details, or empty when any condition fails
     */
    Optional<CompatibilityRuleMatch> evaluate(
            CompatibilityRuleSet ruleSet,
            Component componentA,
            Component componentB
    );
}
