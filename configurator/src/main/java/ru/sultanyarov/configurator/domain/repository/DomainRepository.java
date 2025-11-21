package ru.sultanyarov.configurator.domain.repository;

import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;

/**
 * Repository interface for managing Domain entities.
 * Provides methods for CRUD operations and existence checks.
 */
public interface DomainRepository {
    /**
     * Retrieves a domain by its unique identifier.
     *
     * @param id the unique identifier of the domain to retrieve
     * @return the domain with the specified ID
     * @throws ru.sultanyarov.configurator.domain.exception.BusinessException if no domain found with the given ID
     */
    Domain getDomainById(Long id);

    /**
     * Deletes a domain by its unique identifier.
     *
     * @param id the unique identifier of the domain to delete
     */
    void deleteDomainById(Long id);

    /**
     * Creates a new domain.
     *
     * @param domain the domain entity to create
     * @return the created domain with generated ID and timestamps
     * @throws ru.sultanyarov.configurator.domain.exception.BusinessException if domain creation fails
     */
    Domain createDomain(Domain domain);

    /**
     * Updates an existing domain.
     *
     * @param id     the unique identifier of the domain to update
     * @param domain the domain entity with updated values
     * @return the updated domain
     * @throws ru.sultanyarov.configurator.domain.exception.BusinessException if domain update fails
     */
    Domain updateDomain(Long id, Domain domain);

    /**
     * Retrieves a paginated list of domains.
     *
     * @param page     the page number (0-based)
     * @param pageSize the number of items per page
     * @return a page containing domains and pagination information
     */
    Page<Domain> getDomains(int page, int pageSize);

    /**
     * Checks if a domain with the specified name exists.
     *
     * @param name the name to check for existence
     * @return true if a domain with the given name exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Checks if a domain with the specified ID exists.
     *
     * @param id the ID to check for existence
     * @return true if a domain with the given ID exists, false otherwise
     */
    boolean existsById(Long id);
}
