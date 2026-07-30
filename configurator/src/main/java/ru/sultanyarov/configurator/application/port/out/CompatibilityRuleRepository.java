package ru.sultanyarov.configurator.application.port.out;

import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for compatibility rule-set aggregate persistence.
 */
public interface CompatibilityRuleRepository {

    /**
     * Creates a normalized rule-set aggregate.
     *
     * @param ruleSet rule set with component type A id lower than component type B id
     * @return created aggregate, or empty when its domain, type pair and name already exist
     */
    Optional<CompatibilityRuleSet> create(CompatibilityRuleSet ruleSet);

    /**
     * Retrieves a rule set scoped to a domain.
     *
     * @param ruleSetId rule-set identifier
     * @param domainId  domain identifier
     * @return aggregate with ordered conditions, or empty when not found in the scope
     */
    Optional<CompatibilityRuleSet> getByIdAndDomainId(Long ruleSetId, Long domainId);

    /**
     * Retrieves all rule sets of a domain in stable identifier order.
     *
     * @param domainId domain identifier
     * @return rule-set aggregates
     */
    List<CompatibilityRuleSet> getAllByDomainId(Long domainId);

    /**
     * Updates aggregate metadata and replaces all its conditions.
     *
     * @param ruleSetId rule-set identifier
     * @param domainId  domain identifier
     * @param ruleSet   target aggregate state
     * @return updated aggregate, or empty when not found in the scope
     */
    Optional<CompatibilityRuleSet> updateByIdAndDomainId(
            Long ruleSetId,
            Long domainId,
            CompatibilityRuleSet ruleSet
    );

    /**
     * Physically deletes a rule set scoped to a domain.
     *
     * @param ruleSetId rule-set identifier
     * @param domainId  domain identifier
     * @return {@code true} when an aggregate was deleted
     */
    boolean deleteByIdAndDomainId(Long ruleSetId, Long domainId);

    /**
     * Checks whether a component type is referenced by any rule set.
     *
     * @param componentTypeId component-type identifier
     * @return {@code true} when referenced on either side
     */
    boolean hasByComponentTypeId(Long componentTypeId);
}
