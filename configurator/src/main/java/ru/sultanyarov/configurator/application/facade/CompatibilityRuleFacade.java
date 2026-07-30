package ru.sultanyarov.configurator.application.facade;

import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleSet;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.SaveCompatibilityRuleSetRequest;

import java.util.List;

/**
 * REST boundary for automatic compatibility rule-set operations.
 */
public interface CompatibilityRuleFacade {
    CompatibilityRuleSet create(Long domainId, SaveCompatibilityRuleSetRequest request);

    List<CompatibilityRuleSet> getAllByDomainId(Long domainId);

    CompatibilityRuleSet getByIdAndDomainId(Long ruleSetId, Long domainId);

    CompatibilityRuleSet updateByIdAndDomainId(
            Long ruleSetId,
            Long domainId,
            SaveCompatibilityRuleSetRequest request
    );

    void deleteByIdAndDomainId(Long ruleSetId, Long domainId);
}
