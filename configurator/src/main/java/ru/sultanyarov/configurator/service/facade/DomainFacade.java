package ru.sultanyarov.configurator.service.facade;

import ru.sultanyarov.configurator.domain.dto.CreateDomainRequest;
import ru.sultanyarov.configurator.domain.dto.Domain;
import ru.sultanyarov.configurator.domain.dto.DomainPage;
import ru.sultanyarov.configurator.domain.dto.UpdateDomainRequest;

/**
 * Facade interface for domain management operations.
 * Provides a simplified API for clients to interact with domain entities,
 * handling DTO conversions and request validation.
 */
public interface DomainFacade {
    /**
     * Creates a new domain based on the provided request.
     * Validates that the request contains a name.
     *
     * @param createDomainRequest the request containing domain details
     * @return the created domain DTO
     * @throws ru.sultanyarov.configurator.domain.exception.ValidationException          if the request validation fails
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if a domain with the same name already exists
     */
    Domain createDomain(CreateDomainRequest createDomainRequest);

    /**
     * Retrieves a domain by its unique identifier.
     *
     * @param id the unique identifier of the domain to retrieve
     * @return the domain DTO with the specified ID
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no domain found with the given ID
     */
    Domain getDomainById(Long id);

    /**
     * Deletes a domain by its unique identifier.
     *
     * @param id the unique identifier of the domain to delete
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no domain found with the given ID
     */
    void deleteDomainById(Long id);

    /**
     * Updates an existing domain based on the provided request.
     * Validates that the request contains a name.
     *
     * @param id                  the unique identifier of the domain to update
     * @param updateDomainRequest the request containing updated domain details
     * @return the updated domain DTO
     * @throws ru.sultanyarov.configurator.domain.exception.ValidationException          if the request validation fails
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException            if no domain found with the given ID
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if another domain with the same name already exists
     */
    Domain updateDomain(Long id, UpdateDomainRequest updateDomainRequest);

    /**
     * Retrieves a paginated list of domains.
     *
     * @param page     the page number (0-based)
     * @param pageSize the number of items per page
     * @return a page containing domain DTOs and pagination information
     */
    DomainPage getDomains(int page, int pageSize);
}
