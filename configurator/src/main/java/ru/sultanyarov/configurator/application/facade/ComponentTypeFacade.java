package ru.sultanyarov.configurator.application.facade;

import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentType;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentTypeRequest;

import java.util.List;

/**
 * Facade interface for component type management operations.
 * Provides an application boundary for converting transport-layer requests and responses
 * related to {@link ComponentType} entities.
 */
public interface ComponentTypeFacade {
    /**
     * Creates a new component type in the specified domain based on the provided request.
     *
     * @param domainId                   the unique identifier of the domain where the component type will be created
     * @param createComponentTypeRequest the request containing component type details
     * @return the created component type DTO
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException            if the domain does not exist
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if a component type with the same name already exists in the domain
     */
    ComponentType createComponentType(Long domainId, CreateComponentTypeRequest createComponentTypeRequest);

    /**
     * Updates an existing component type based on the provided request.
     *
     * @param componentTypeId            the unique identifier of the component type to update
     * @param createComponentTypeRequest the request containing updated component type details
     * @return the updated component type DTO
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException            if no component type found with the given ID
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if another component type with the same name already exists in the domain
     * @throws ru.sultanyarov.configurator.domain.exception.ValidationException          if the request validation fails
     */
    ComponentType updateComponentType(Long componentTypeId, CreateComponentTypeRequest createComponentTypeRequest);

    /**
     * Deletes a component type by its unique identifier.
     *
     * @param id the unique identifier of the component type to delete
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException                 if no component type found with the given ID
     * @throws ru.sultanyarov.configurator.domain.exception.EntityHasRelatedEntitiesException if the component type has related entities
     */
    void deleteComponentType(Long id);

    /**
     * Retrieves a component type by its unique identifier.
     *
     * @param id the unique identifier of the component type to retrieve
     * @return the component type DTO with the specified ID
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no component type found with the given ID
     */
    ComponentType getComponentType(Long id);

    /**
     * Retrieves all component types belonging to a specific domain.
     *
     * @param domainId the unique identifier of the domain
     * @return a list of component type DTOs belonging to the specified domain
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no domain found with the given ID
     */
    List<ComponentType> getComponentTypesByDomainId(Long domainId);
}
