package ru.sultanyarov.configurator.service.core;

import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;

/**
 * Service interface for managing {@link Domain} entities.
 * Provides business logic operations for domains.
 */
public interface DomainService {
    /**
     * Retrieves a paginated list of domains.
     *
     * @param page     the page number (0-based)
     * @param pageSize the number of items per page
     * @return a page containing domains and pagination information
     */
    Page<Domain> getPage(int page, int pageSize);

    /**
     * Retrieves a domain by its unique identifier.
     *
     * @param id the unique identifier of the domain to retrieve
     * @return the domain with the specified ID
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no domain found with the given ID
     */
    Domain getById(Long id);

    /**
     * Deletes a domain by its unique identifier.
     *
     * @param id the unique identifier of the domain to delete
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no domain found with the given ID
     */
    void deleteById(Long id);

    /**
     * Creates a new domain.
     *
     * @param domain the domain entity to create
     * @return the created domain with generated ID and timestamps
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if a domain with the same name already exists
     */
    Domain create(Domain domain);

    /**
     * Updates an existing domain.
     *
     * @param id     the unique identifier of the domain to update
     * @param domain the domain entity with updated values
     * @return the updated domain
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException            if no domain found with the given ID
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if another domain with the same name already exists
     */
    Domain update(Long id, Domain domain);
}
