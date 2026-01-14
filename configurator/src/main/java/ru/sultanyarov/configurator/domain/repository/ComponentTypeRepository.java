package ru.sultanyarov.configurator.domain.repository;

import ru.sultanyarov.configurator.domain.model.ComponentType;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link ComponentType} entities.
 * Provides data access operations for component types.
 */
public interface ComponentTypeRepository {
    /**
     * Creates a new component type.
     *
     * @param componentType the component type entity to create
     * @return the created component type with generated ID, or empty if creation failed
     */
    Optional<ComponentType> createComponentType(ComponentType componentType);

    /**
     * Checks if a component type with the specified name exists in the given domain.
     *
     * @param name     the name to check for existence
     * @param domainId the unique identifier of the domain
     * @return {@code true} if a component type with the given name exists in the domain, {@code false} otherwise
     */
    boolean existsByNameAndDomainId(String name, Long domainId);

    /**
     * Checks if a component type with the specified ID exists.
     *
     * @param id the ID to check for existence
     * @return {@code true} if a component type with the given ID exists, {@code false} otherwise
     */
    boolean existsById(Long id);

    /**
     * Updates an existing component type.
     *
     * @param id            the unique identifier of the component type to update
     * @param componentType the component type entity with updated values
     * @return the updated component type, or empty if update failed
     */
    Optional<ComponentType> updateComponentType(Long id, ComponentType componentType);

    /**
     * Deletes a component type by its unique identifier.
     *
     * @param id the unique identifier of the component type to delete
     */
    void deleteComponentTypeById(Long id);

    /**
     * Retrieves a component type by its unique identifier.
     *
     * @param id the unique identifier of the component type to retrieve
     * @return the component type with the specified ID, or empty if not found
     */
    Optional<ComponentType> getComponentTypeById(Long id);

    /**
     * Retrieves all component types belonging to a specific domain.
     *
     * @param domainId the unique identifier of the domain
     * @return a list of component types belonging to the specified domain
     */
    List<ComponentType> getComponentTypesByDomainId(Long domainId);
}
