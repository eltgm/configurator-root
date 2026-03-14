package ru.sultanyarov.configurator.application.facade;

import ru.sultanyarov.configurator.api.inbounds.rest.dto.Component;
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
}
