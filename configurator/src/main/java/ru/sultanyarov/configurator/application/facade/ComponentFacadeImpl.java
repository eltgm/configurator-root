package ru.sultanyarov.configurator.application.facade;

import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.Component;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentImage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateComponentRequest;
import ru.sultanyarov.configurator.application.mapper.ComponentMapper;
import ru.sultanyarov.configurator.application.service.ComponentService;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.ComponentImageContent;
import ru.sultanyarov.configurator.domain.model.ComponentImageUpload;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentFacadeImpl implements ComponentFacade {
  private final ComponentService componentService;
  private final ComponentMapper componentMapper;

  @Override
  public Component createComponent(CreateComponentRequest createComponentRequest) {
    log.info("Creating component");
    return componentMapper.toDto(
        componentService.create(componentMapper.toEntity(createComponentRequest)));
  }

  @Override
  public Component updateComponent(
      Long componentId, UpdateComponentRequest updateComponentRequest) {
    log.info("Updating component with id {}", componentId);
    return componentMapper.toDto(
        componentService.update(componentId, componentMapper.toEntity(updateComponentRequest)));
  }

  @Override
  public void archiveComponent(Long componentId) {
    log.info("Archiving component with id {}", componentId);
    componentService.archiveById(componentId);
  }

  @Override
  public Component restoreComponent(Long componentId) {
    log.info("Restoring component with id {}", componentId);
    return componentMapper.toDto(componentService.restoreById(componentId));
  }

  @Override
  public ComponentImage uploadComponentImage(
      Long componentId, MultipartFile file, Integer orderIndex) {
    log.info("Uploading image for component with id {}", componentId);
    if (file == null) {
      throw new ValidationException("Image file is required");
    }

    try {
      return componentMapper.toDto(
          componentService.uploadImage(
              componentId,
              new ComponentImageUpload(file.getBytes(), file.getContentType()),
              orderIndex));
    } catch (IOException exception) {
      throw new BusinessException(exception, "Failed to read uploaded image");
    }
  }

  @Override
  public List<ComponentImage> getComponentImages(Long componentId) {
    log.info("Getting images for component with id {}", componentId);
    return componentService.getImagesByComponentId(componentId).stream()
        .map(componentMapper::toDto)
        .toList();
  }

  @Override
  public ComponentImageContent getComponentImageContent(Long imageId) {
    log.info("Getting content for component image with id {}", imageId);
    return componentService.getImageContent(imageId);
  }

  @Override
  public ComponentImageContent getComponentImageThumbnail(Long imageId) {
    return componentService.getImageThumbnail(imageId);
  }

  @Override
  public void deleteComponentImage(Long imageId) {
    log.info("Deleting component image with id {}", imageId);
    componentService.deleteImage(imageId);
  }

  @Override
  public List<ComponentImage> reorderComponentImages(Long componentId, List<Long> orderedImageIds) {
    log.info("Replacing image order for component with id {}", componentId);
    return componentService.reorderImages(componentId, orderedImageIds).stream()
        .map(componentMapper::toDto)
        .toList();
  }

  @Override
  public ComponentPage getComponentsByDomainId(
      Long domainId,
      Long componentTypeId,
      String name,
      Boolean archived,
      Integer page,
      Integer size) {
    log.info("Getting components by domain");
    return componentMapper.toComponentPageDto(
        componentService.getByPageByDomainId(
            domainId, componentTypeId, name, archived, page, size));
  }

  @Override
  public Component getComponentById(Long id) {
    log.info("Getting component");
    return componentMapper.toDto(componentService.getById(id));
  }
}
