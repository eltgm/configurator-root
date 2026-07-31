package ru.sultanyarov.configurator.application.validator;

import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.ComponentType;

/**
 * Validates an attribute-to-attribute compatibility rule set.
 */
public interface CompatibilityRuleValidator {

    /**
     * Validates a normalized rule set against its component types and attributes.
     *
     * @param ruleSet       normalized rule set
     * @param componentTypeA normalized first component type
     * @param componentTypeB normalized second component type
     */
    void validate(
            CompatibilityRuleSet ruleSet,
            ComponentType componentTypeA,
            ComponentType componentTypeB
    );
}
