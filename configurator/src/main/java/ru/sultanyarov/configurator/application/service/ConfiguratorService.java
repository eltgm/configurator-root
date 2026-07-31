package ru.sultanyarov.configurator.application.service;

import ru.sultanyarov.configurator.domain.model.ConfiguratorBatchResult;
import ru.sultanyarov.configurator.domain.model.ConfiguratorIntersectionResult;
import ru.sultanyarov.configurator.domain.model.ConfiguratorResult;

import java.util.List;

/**
 * Use cases for finding compatible components.
 */
public interface ConfiguratorService {

    /**
     * Finds manual or automatic compatibility for one active base component.
     *
     * @param domainId        domain scope
     * @param baseComponentId selected component
     * @param includeTransitive whether components reachable through multiple edges are included
     * @return compatible active components grouped by type
     */
    ConfiguratorResult getCompatibleComponents(
            Long domainId,
            Long baseComponentId,
            boolean includeTransitive
    );

    /**
     * Calculates independent compatibility results for multiple active base components.
     *
     * @param domainId domain scope
     * @param baseComponentIds unique selected component identifiers in result order
     * @param includeTransitive whether components reachable through multiple edges are included
     * @return one compatibility result per selected component in request order
     */
    ConfiguratorBatchResult searchCompatibleComponents(
            Long domainId,
            List<Long> baseComponentIds,
            boolean includeTransitive
    );

    /**
     * Finds active components compatible with every selected base component.
     *
     * @param domainId domain scope
     * @param baseComponentIds unique selected component identifiers in request order
     * @param includeTransitive whether components reachable through multiple edges are included
     * @return intersection grouped by component type with evidence for every base component
     */
    ConfiguratorIntersectionResult intersectCompatibleComponents(
            Long domainId,
            List<Long> baseComponentIds,
            boolean includeTransitive
    );
}
