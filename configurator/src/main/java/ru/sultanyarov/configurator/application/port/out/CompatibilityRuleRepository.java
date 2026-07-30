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
     * Retrieves enabled rule sets that involve a component type.
     *
     * @param domainId       domain identifier
     * @param componentTypeId component-type identifier on either normalized side
     * @return enabled rule-set aggregates in stable identifier order
     */
    List<CompatibilityRuleSet> getEnabledByDomainIdAndComponentTypeId(
            Long domainId,
            Long componentTypeId
    );

    /**
     * Updates aggregate metadata and replaces all its conditions.
     *
     * @param ruleSetId rule-set identifier
     * @param domainId  domain identifier
     * @param ruleSet   target aggregate state
     * @return updated aggregate, or empty when not found in the scope
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException
     *         when the target business key already exists
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
     * Checks the rule-set business key, optionally excluding the aggregate being updated.
     *
     * @param domainId             domain identifier
     * @param componentTypeAId     normalized first component-type identifier
     * @param componentTypeBId     normalized second component-type identifier
     * @param name                 normalized rule-set name
     * @param excludedRuleSetId    aggregate identifier to ignore, or {@code null}
     * @return {@code true} when another aggregate has the same business key
     */
    boolean existsByBusinessKey(
            Long domainId,
            Long componentTypeAId,
            Long componentTypeBId,
            String name,
            Long excludedRuleSetId
    );

    /**
     * Checks whether a component type is referenced by any rule set.
     *
     * @param componentTypeId component-type identifier
     * @return {@code true} when referenced on either side
     */
    boolean hasByComponentTypeId(Long componentTypeId);
}
