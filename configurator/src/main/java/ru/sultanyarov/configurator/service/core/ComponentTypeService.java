package ru.sultanyarov.configurator.service.core;

import ru.sultanyarov.configurator.domain.model.ComponentType;

import java.util.List;

/**
 * Service interface for managing {@link ComponentType} entities.
 * Provides business logic operations for component types.
 */
public interface ComponentTypeService {
    /**
     * Creates a new component type.
     *
     * @param componentType the component type entity to create
     * @return the created component type with generated ID and timestamps
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if a component type with the same name already exists in the domain
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException            if the domain does not exist
     */
    ComponentType create(ComponentType componentType);

    /**
     * Updates an existing component type.
     *
     * @param id            the unique identifier of the component type to update
     * @param componentType the component type entity with updated values
     * @return the updated component type
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException            if no component type found with the given ID
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if another component type with the same name already exists in the domain
     */
    ComponentType update(Long id, ComponentType componentType);

    /**
     * Deletes a component type by its unique identifier.
     *
     * @param id the unique identifier of the component type to delete
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException                 if no component type found with the given ID
     * @throws ru.sultanyarov.configurator.domain.exception.EntityHasRelatedEntitiesException if the component type has related entities
     */
    void deleteById(Long id);

    /**
     * Retrieves a component type by its unique identifier.
     *
     * @param id the unique identifier of the component type to retrieve
     * @return the component type with the specified ID
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no component type found with the given ID
     */
    ComponentType getById(Long id);

    /**
     * Retrieves all component types belonging to a specific domain.
     *
     * @param domainId the unique identifier of the domain
     * @return a list of component types belonging to the specified domain
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no domain found with the given ID
     */
    List<ComponentType> getByDomainId(Long domainId);
}
