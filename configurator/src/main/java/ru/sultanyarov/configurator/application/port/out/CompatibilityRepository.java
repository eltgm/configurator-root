package ru.sultanyarov.configurator.application.port.out;

import ru.sultanyarov.configurator.domain.model.CompatibilityLink;

import java.util.Optional;

/**
 * Outbound port for compatibility link persistence.
 */
public interface CompatibilityRepository {

    /**
     * Persists an undirected compatibility link.
     *
     * @param compatibilityLink normalized compatibility link with component A id lower than component B id
     * @return the created link, or empty when the normalized pair already exists in the domain
     */
    Optional<CompatibilityLink> create(CompatibilityLink compatibilityLink);
}
