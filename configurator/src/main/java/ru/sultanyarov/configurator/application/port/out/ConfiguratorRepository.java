package ru.sultanyarov.configurator.application.port.out;

import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.Component;

import java.util.List;

/**
 * Outbound read port for the single-component configurator use case.
 */
public interface ConfiguratorRepository {

    /**
     * Loads active candidate components of a domain with their attribute values.
     *
     * @param domainId        domain identifier
     * @param baseComponentId component to exclude from candidates
     * @return candidates in stable component-type and component order
     */
    List<Component> getActiveCandidates(Long domainId, Long baseComponentId);

    /**
     * Loads all active components of a domain with their attribute values.
     *
     * @param domainId domain identifier
     * @return active components in stable component-type and component order
     */
    List<Component> getActiveComponents(Long domainId);

    /**
     * Loads direct manual compatibility links connected to the base component.
     *
     * @param domainId        domain identifier
     * @param baseComponentId selected component identifier
     * @return links in stable identifier order
     */
    List<CompatibilityLink> getManualCompatibilityLinks(Long domainId, Long baseComponentId);

    /**
     * Loads all direct manual compatibility links of a domain for graph traversal.
     *
     * @param domainId domain identifier
     * @return links in stable identifier order
     */
    List<CompatibilityLink> getAllManualCompatibilityLinks(Long domainId);
}
