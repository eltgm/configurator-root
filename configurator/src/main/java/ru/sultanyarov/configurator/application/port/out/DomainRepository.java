package ru.sultanyarov.configurator.application.port.out;

import java.util.Optional;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;

/**
 * Outbound port for persisting and retrieving {@link Domain} entities. Defines persistence
 * operations required by the application layer.
 */
public interface DomainRepository {
  /** Locks the domain until transaction completion; returns false if it does not exist. */
  boolean lockById(Long id);

  /** Locks types, then components, to prevent inserts racing with recursive deletion. */
  void lockContentsByDomainId(Long id);

  /** Deletes rules and components; their owned children are removed by foreign keys. */
  void deleteContentsByDomainId(Long id);

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
   * @param id the unique identifier of the domain to update
   * @param domain the domain entity with updated values
   * @return the updated domain, or empty if update failed
   */
  Optional<Domain> updateDomain(Long id, Domain domain);

  /**
   * Retrieves a paginated list of domains.
   *
   * @param page the page number (0-based)
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
