package ru.sultanyarov.configurator.application.facade;

import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorResponse;

/**
 * REST boundary for configurator operations.
 */
public interface ConfiguratorFacade {
    ConfiguratorResponse getCompatibleComponents(
            Long domainId,
            Long baseComponentId,
            boolean includeTransitive
    );
}
