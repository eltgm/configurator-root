package ru.sultanyarov.configurator.application.port.out;

import ru.sultanyarov.configurator.domain.model.Component;

import java.util.List;
import java.util.Set;

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
     * Loads identifiers connected to the base component by a direct manual compatibility link.
     *
     * @param domainId        domain identifier
     * @param baseComponentId selected component identifier
     * @return linked component identifiers
     */
    Set<Long> getManuallyCompatibleComponentIds(Long domainId, Long baseComponentId);
}
