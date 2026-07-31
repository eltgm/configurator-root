package ru.sultanyarov.configurator.application.port.out;

import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.CompatibilityGraph;

import java.util.Optional;

/**
 * Outbound port for compatibility link persistence.
 */
public interface CompatibilityRepository {

    /**
     * Loads the compatibility graph for a domain.
     *
     * @param domainId the domain identifier
     * @return active domain components and links between them, sorted by identifier
     */
    CompatibilityGraph getGraphByDomainId(Long domainId);

    /**
     * Persists an undirected compatibility link.
     *
     * @param compatibilityLink normalized compatibility link with component A id lower than component B id
     * @return the created link, or empty when the normalized pair already exists in the domain
     */
    Optional<CompatibilityLink> create(CompatibilityLink compatibilityLink);

    /**
     * Deletes a compatibility link only when it belongs to the specified domain.
     *
     * @param linkId   the compatibility link identifier
     * @param domainId the domain identifier
     * @return {@code true} when a row was deleted
     */
    boolean deleteByIdAndDomainId(Long linkId, Long domainId);
}
