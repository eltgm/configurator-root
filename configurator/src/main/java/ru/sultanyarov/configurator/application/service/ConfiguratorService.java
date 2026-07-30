package ru.sultanyarov.configurator.application.service;

import ru.sultanyarov.configurator.domain.model.ConfiguratorResult;

/**
 * Use cases for finding compatible components.
 */
public interface ConfiguratorService {

    /**
     * Finds direct manual or automatic compatibility for one active base component.
     *
     * @param domainId        domain scope
     * @param baseComponentId selected component
     * @return compatible active components grouped by type
     */
    ConfiguratorResult getCompatibleComponents(Long domainId, Long baseComponentId);
}
