package ru.sultanyarov.configurator.application.service;

import ru.sultanyarov.configurator.domain.model.CompatibilityLink;

/**
 * Service for compatibility management use cases.
 */
public interface CompatibilityService {

    /**
     * Creates one undirected compatibility link between active components of a domain.
     *
     * @param compatibilityLink requested compatibility link
     * @return the created normalized compatibility link
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if the domain or a component does not exist
     * @throws ru.sultanyarov.configurator.domain.exception.ValidationException if the link is a self-link or a component belongs to another domain
     * @throws ru.sultanyarov.configurator.domain.exception.ComponentArchivedException if either component is archived
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if the undirected link already exists
     */
    CompatibilityLink create(CompatibilityLink compatibilityLink);

    /**
     * Physically deletes a compatibility link scoped to a domain.
     *
     * @param linkId   the compatibility link identifier
     * @param domainId the domain identifier
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if the domain or scoped link does not exist
     */
    void deleteByIdAndDomainId(Long linkId, Long domainId);
}
