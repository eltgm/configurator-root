package ru.sultanyarov.configurator.application.facade;

import ru.sultanyarov.configurator.api.inbounds.rest.dto.Component;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest;

/**
 * Facade interface for component management operations.
 * Provides an application boundary for converting transport-layer requests and responses
 * related to {@link Component} entities.
 */
public interface ComponentFacade {
    /**
     * Creates a new component based on the provided request.
     *
     * @param createComponentRequest the request containing component details
     * @return the created component DTO
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException            if the component type does not exist
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if a component with the same name already exists within the component type
     * @throws ru.sultanyarov.configurator.domain.exception.ValidationException          if the request validation fails
     * @throws ru.sultanyarov.configurator.domain.exception.BusinessException            if the component could not be created
     */
    Component createComponent(CreateComponentRequest createComponentRequest);

    /**
     * Retrieves a paginated list of components belonging to the specified domain.
     *
     * @param domainId        the unique identifier of the domain
     * @param componentTypeId the optional unique identifier of the component type to filter by
     * @param name            the optional component name to filter by
     * @param page            the page number (0-based)
     * @param size            the number of items per page
     * @return a page of component DTOs matching the specified filters
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException   if no domain found with the given ID
     * @throws ru.sultanyarov.configurator.domain.exception.ValidationException if the component type does not belong to the specified domain
     */
    ComponentPage getComponentsByDomainId(Long domainId, Long componentTypeId, String name, Integer page, Integer size);
}
