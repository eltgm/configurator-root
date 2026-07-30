package ru.sultanyarov.configurator.application.facade;

import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityLink;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateCompatibilityLinkRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.GraphResponse;

/**
 * Application boundary for compatibility REST operations.
 */
public interface CompatibilityFacade {

    /**
     * Returns the compatibility graph for a domain.
     *
     * @param domainId the domain identifier
     * @return graph response with active nodes and links between them
     */
    GraphResponse getCompatibilityGraph(Long domainId);

    /**
     * Creates a compatibility link in the specified domain.
     *
     * @param domainId the domain identifier
     * @param request  requested component pair and optional comment
     * @return the created normalized compatibility link DTO
     */
    CompatibilityLink createCompatibilityLink(Long domainId, CreateCompatibilityLinkRequest request);

    /**
     * Deletes a compatibility link scoped to a domain.
     *
     * @param domainId the domain identifier
     * @param linkId   the compatibility link identifier
     */
    void deleteCompatibilityLink(Long domainId, Long linkId);
}
