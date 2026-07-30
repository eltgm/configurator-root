package ru.sultanyarov.configurator.application.facade;

import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityLink;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateCompatibilityLinkRequest;

/**
 * Application boundary for compatibility REST operations.
 */
public interface CompatibilityFacade {

    /**
     * Creates a compatibility link in the specified domain.
     *
     * @param domainId the domain identifier
     * @param request  requested component pair and optional comment
     * @return the created normalized compatibility link DTO
     */
    CompatibilityLink createCompatibilityLink(Long domainId, CreateCompatibilityLinkRequest request);
}
