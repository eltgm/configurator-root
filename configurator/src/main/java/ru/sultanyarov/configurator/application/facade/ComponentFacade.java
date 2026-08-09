package ru.sultanyarov.configurator.application.facade;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.Component;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentImage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateComponentRequest;
import ru.sultanyarov.configurator.domain.model.ComponentImageContent;

/**
 * Facade interface for component management operations. Provides an application boundary for
 * converting transport-layer requests and responses related to {@link Component} entities.
 */
public interface ComponentFacade {
  /**
   * Creates a new component based on the provided request.
   *
   * @param createComponentRequest the request containing component details
   * @return the created component DTO
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if the component type
   *     does not exist
   * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if a
   *     component with the same name already exists within the component type
   * @throws ru.sultanyarov.configurator.domain.exception.ValidationException if the request
   *     validation fails
   * @throws ru.sultanyarov.configurator.domain.exception.BusinessException if the component could
   *     not be created
   */
  Component createComponent(CreateComponentRequest createComponentRequest);

  /**
   * Fully replaces the editable state of an existing component.
   *
   * @param componentId the unique identifier of the component to update
   * @param updateComponentRequest the request containing the target component state
   * @return the updated component DTO
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if the component does
   *     not exist
   * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if another
   *     component with the same name exists within the component type
   * @throws ru.sultanyarov.configurator.domain.exception.ValidationException if the component type
   *     is changed or attribute validation fails
   * @throws ru.sultanyarov.configurator.domain.exception.BusinessException if the component could
   *     not be updated
   */
  Component updateComponent(Long componentId, UpdateComponentRequest updateComponentRequest);

  /**
   * Archives an existing component.
   *
   * @param componentId the unique identifier of the component to archive
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if the component does
   *     not exist
   * @throws ru.sultanyarov.configurator.domain.exception.BusinessException if the component could
   *     not be archived
   */
  void archiveComponent(Long componentId);

  /**
   * Uploads and attaches an image to an active component.
   *
   * @param componentId the unique identifier of the component
   * @param file multipart image file
   * @param orderIndex optional non-negative display order
   * @return the created component image
   */
  ComponentImage uploadComponentImage(Long componentId, MultipartFile file, Integer orderIndex);

  /**
   * Retrieves component images in their deterministic display order.
   *
   * @param componentId the unique identifier of the component
   * @return component images sorted by order index and image identifier
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if the component does
   *     not exist
   */
  List<ComponentImage> getComponentImages(Long componentId);

  /**
   * Retrieves original content for a component image.
   *
   * @param imageId the component image identifier
   * @return original bytes and media type
   */
  ComponentImageContent getComponentImageContent(Long imageId);

  /**
   * Retrieves a paginated list of components belonging to the specified domain.
   *
   * @param domainId the unique identifier of the domain
   * @param componentTypeId the optional unique identifier of the component type to filter by
   * @param name the optional component name to filter by
   * @param page the page number (0-based)
   * @param size the number of items per page
   * @return a page of component DTOs matching the specified filters
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no domain found with
   *     the given ID
   * @throws ru.sultanyarov.configurator.domain.exception.ValidationException if the component type
   *     does not belong to the specified domain
   */
  ComponentPage getComponentsByDomainId(
      Long domainId, Long componentTypeId, String name, Integer page, Integer size);

  /**
   * Retrieves a component by its unique identifier.
   *
   * @param id the unique identifier of the component to retrieve
   * @return the component DTO with the specified ID
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no component found
   *     with the given ID
   */
  Component getComponentById(Long id);
}
