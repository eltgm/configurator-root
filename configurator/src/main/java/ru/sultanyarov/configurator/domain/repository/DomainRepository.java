package ru.sultanyarov.configurator.domain.repository;

import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;

import java.util.Optional;

/**
 * Repository interface for managing {@link Domain} entities.
 * Provides data access operations for domains.
 */
public interface DomainRepository {
    /**
     * Retrieves a domain by its unique identifier.
     *
     * @param id the unique identifier of the domain to retrieve
     * @return the domain with the specified ID, or empty if not found
     */
    Optional<Domain> getDomainById(Long id);

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
     * @return the created domain with generated ID and timestamps, or empty if creation failed
     */
    Optional<Domain> createDomain(Domain domain);

    /**
     * Updates an existing domain.
     *
     * @param id     the unique identifier of the domain to update
     * @param domain the domain entity with updated values
     * @return the updated domain, or empty if update failed
     */
    Optional<Domain> updateDomain(Long id, Domain domain);

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
     * @return {@code true} if a domain with the given name exists, {@code false} otherwise
     */
    boolean existsByName(String name);

    /**
     * Checks if a domain with the specified ID exists.
     *
     * @param id the ID to check for existence
     * @return {@code true} if a domain with the given ID exists, {@code false} otherwise
     */
    boolean existsById(Long id);
}
