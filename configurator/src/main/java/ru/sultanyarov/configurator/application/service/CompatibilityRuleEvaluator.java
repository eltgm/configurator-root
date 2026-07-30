package ru.sultanyarov.configurator.application.service;

import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.Component;

/**
 * Evaluates one normalized automatic compatibility rule set against two components.
 */
public interface CompatibilityRuleEvaluator {

    /**
     * @param ruleSet   enabled normalized rule set
     * @param componentA component of rule-set type A
     * @param componentB component of rule-set type B
     * @return {@code true} only when every rule-set condition matches
     */
    boolean matches(
            CompatibilityRuleSet ruleSet,
            Component componentA,
            Component componentB
    );
}
