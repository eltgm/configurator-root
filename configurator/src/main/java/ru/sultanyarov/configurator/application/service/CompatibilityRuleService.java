package ru.sultanyarov.configurator.application.service;

import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;

import java.util.List;

/**
 * Application use cases for automatic compatibility rule sets.
 */
public interface CompatibilityRuleService {
    CompatibilityRuleSet create(CompatibilityRuleSet ruleSet);

    List<CompatibilityRuleSet> getAllByDomainId(Long domainId);

    CompatibilityRuleSet getByIdAndDomainId(Long ruleSetId, Long domainId);

    CompatibilityRuleSet updateByIdAndDomainId(
            Long ruleSetId,
            Long domainId,
            CompatibilityRuleSet ruleSet
    );

    void deleteByIdAndDomainId(Long ruleSetId, Long domainId);
}
