package ru.sultanyarov.configurator.application.service;

import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sultanyarov.configurator.application.port.out.ComponentImageStorage;
import ru.sultanyarov.configurator.application.port.out.ComponentRepository;
import ru.sultanyarov.configurator.application.validator.ComponentImageValidator;
import ru.sultanyarov.configurator.application.validator.ComponentValidator;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.ComponentArchivedException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.AttributeValue;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentImage;
import ru.sultanyarov.configurator.domain.model.ComponentImageUpload;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;
import ru.sultanyarov.configurator.domain.model.StoredImage;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentServiceImpl implements ComponentService {
  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_PAGE_SIZE = 10;
  private static final int MAX_PAGE_SIZE = 100;

  private final ComponentRepository componentRepository;
  private final ComponentTypeService componentTypeService;
  private final AttributeValueService attributeValueService;
  private final ComponentValidator componentValidator;
  private final ComponentImageValidator componentImageValidator;
  private final ComponentImageStorage componentImageStorage;
  private final DomainService domainService;

  @Override
  @Transactional
  public Component create(Component componentToCreate) {
    log.debug("create component {}", componentToCreate);
    Long componentTypeId = componentToCreate.getComponentTypeId();

    ComponentType componentType = componentTypeService.getById(componentTypeId);
    Map<Long, AttributeDefinition> componentTypeAttributesMap =
        getComponentAttributesDefinitionsMap(componentType);

    componentValidator.validateCreation(
        componentToCreate, componentType, componentTypeAttributesMap);

    List<AttributeValue> newComponentAttributes =
        enrichAttributes(componentToCreate.getAttributes(), componentTypeAttributesMap);
    componentToCreate.setAttributes(newComponentAttributes);

    var createdComponent =
        componentRepository
            .createComponent(componentToCreate)
            .orElseThrow(() -> new BusinessException("Failed to create component"));
    createdComponent.setAttributes(
        attributeValueService.createAttributeValues(
            newComponentAttributes, createdComponent.getId()));
    createdComponent.setImages(List.of());
    return createdComponent;
  }

  private Map<Long, AttributeDefinition> getComponentAttributesDefinitionsMap(
      ComponentType componentType) {
    List<AttributeDefinition> attributeDefinitions =
        componentType.attributeDefinitions() == null
            ? List.of()
            : componentType.attributeDefinitions();
    return attributeDefinitions.stream()
        .collect(toMap(AttributeDefinition::id, attributeDefinition -> attributeDefinition));
  }

  private List<AttributeValue> enrichAttributes(
      List<AttributeValue> attributes,
      Map<Long, AttributeDefinition> providedAttributeDefinitions) {
    return attributes.stream()
        .map(
            attributeValue -> {
              AttributeDefinition attributeDefinition =
                  providedAttributeDefinitions.get(attributeValue.attributeDefinitionId());
              return AttributeValue.builder()
                  .id(attributeValue.id())
                  .attributeDefinitionId(attributeValue.attributeDefinitionId())
                  .name(attributeDefinition.name())
                  .label(attributeDefinition.label())
                  .dataType(attributeDefinition.dataType())
                  .value(attributeValue.value())
                  .build();
            })
        .toList();
  }

  @Override
  @Transactional
  public Component update(Long id, Component component) {
    log.debug("update component with id {}", id);

    Component existingComponent = getById(id);
    ComponentType componentType =
        componentTypeService.getById(existingComponent.getComponentTypeId());
    Map<Long, AttributeDefinition> componentTypeAttributesMap =
        getComponentAttributesDefinitionsMap(componentType);

    component.setName(component.getName().trim());
    componentValidator.validateUpdate(
        component, existingComponent, componentType, componentTypeAttributesMap);

    List<AttributeValue> targetAttributes =
        enrichAttributes(
            component.getAttributes() == null ? List.of() : component.getAttributes(),
            componentTypeAttributesMap);

    Component updatedComponent =
        componentRepository
            .updateComponent(id, component)
            .orElseThrow(() -> new BusinessException("Failed to update component with id {}", id));
    updatedComponent.setAttributes(
        attributeValueService.replaceAttributeValues(targetAttributes, id));
    updatedComponent.setImages(
        existingComponent.getImages() == null ? List.of() : existingComponent.getImages());
    return updatedComponent;
  }

  @Override
  public void archiveById(Long id) {
    log.debug("archive component with id {}", id);
    Component component = getById(id);
    if (Boolean.TRUE.equals(component.getArchived())) {
      return;
    }

    if (!componentRepository.archiveComponentById(id)) {
      throw new BusinessException("Failed to archive component with id {}", id);
    }
  }

  @Override
  public ComponentImage uploadImage(Long id, ComponentImageUpload image, Integer orderIndex) {
    log.debug("upload image for component with id {}", id);
    Component component = getById(id);
    if (Boolean.TRUE.equals(component.getArchived())) {
      throw new ComponentArchivedException(
          "Cannot upload image for archived component with id {}", id);
    }

    componentImageValidator.validate(image, orderIndex);
    int resolvedOrderIndex =
        orderIndex == null ? componentRepository.getNextImageOrderIndex(id) : orderIndex;
    StoredImage storedImage = componentImageStorage.store(id, image);

    try {
      return componentRepository
          .createImage(
              ComponentImage.builder()
                  .componentId(id)
                  .url(storedImage.url())
                  .orderIndex(resolvedOrderIndex)
                  .build())
          .orElseThrow(
              () ->
                  new BusinessException(
                      "Failed to persist image metadata for component with id {}", id));
    } catch (RuntimeException exception) {
      compensateStoredImage(storedImage, exception);
      throw exception;
    }
  }

  @Override
  public List<ComponentImage> getImagesByComponentId(Long id) {
    log.debug("get images for component with id {}", id);
    Component component = getById(id);
    return component.getImages() == null ? List.of() : List.copyOf(component.getImages());
  }

  private void compensateStoredImage(StoredImage storedImage, RuntimeException originalException) {
    try {
      componentImageStorage.delete(storedImage.objectKey());
    } catch (RuntimeException cleanupException) {
      log.error(
          "Failed to remove image {} after metadata persistence failure",
          storedImage.objectKey(),
          cleanupException);
      originalException.addSuppressed(cleanupException);
    }
  }

  @Override
  public Component getById(Long id) {
    log.debug("get component by id {}", id);

    return componentRepository
        .getById(id)
        .orElseThrow(() -> new NotFoundException("Component with id {} not found", id));
  }

  @Override
  public Page<Component> getByPageByDomainId(
      Long domainId, Long componentTypeId, String name, Integer page, Integer size) {
    log.debug("get component by domain id {}, component type id {}", domainId, componentTypeId);
    Domain domain = domainService.getById(domainId);
    validateComponentType(componentTypeId, domain);
    int resolvedPage = page == null ? DEFAULT_PAGE : page;
    int resolvedSize = size == null ? DEFAULT_PAGE_SIZE : size;
    validatePagination(resolvedPage, resolvedSize);

    return componentRepository.findPageByDomainIdComponentTypeIdName(
        domainId, componentTypeId, name, resolvedPage, resolvedSize);
  }

  private static void validatePagination(int page, int size) {
    if (page < 0) {
      throw new ValidationException("Page index must be greater than or equal to zero");
    }
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new ValidationException("Page size must be between 1 and {}", MAX_PAGE_SIZE);
    }
  }

  private void validateComponentType(Long componentTypeId, Domain domain) {
    if (componentTypeId != null) {
      boolean isComponentTypeBelongsToDomain =
          domain.componentTypes().stream()
              .anyMatch(componentType -> componentType.id().equals(componentTypeId));

      if (!isComponentTypeBelongsToDomain) {
        throw new ValidationException("Тип компонента не принадлежит указанному домену");
      }
    }
  }
}
