package ru.sultanyarov.configurator.application.facade;

import static org.springframework.util.StringUtils.hasText;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentTypeAttributeSettingsRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateAttributeDefinitionRequest;
import ru.sultanyarov.configurator.application.mapper.AttributeDefinitionMapper;
import ru.sultanyarov.configurator.application.service.AttributeService;
import ru.sultanyarov.configurator.domain.exception.ValidationException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttributesFacadeImpl implements AttributesFacade {
  private final AttributeService attributeService;
  private final AttributeDefinitionMapper attributeDefinitionMapper;

  @Override
  public ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition updateAttribute(
      Long id, CreateAttributeDefinitionRequest createAttributeDefinitionRequest) {
    log.info("Start update attribute by id: {}", id);
    validateCreateAttributeDefinitionRequest(createAttributeDefinitionRequest);
    return toDtoWithLinks(
        attributeService.update(
            id, attributeDefinitionMapper.toModel(createAttributeDefinitionRequest)));
  }

  @Override
  public ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition createAttribute(
      Long componentTypeId, CreateAttributeDefinitionRequest createAttributeDefinitionRequest) {
    log.info("Start create attribute");
    validateCreateAttributeDefinitionRequest(createAttributeDefinitionRequest);

    return attributeDefinitionMapper.toDto(
        attributeService.create(
            attributeDefinitionMapper.toModel(componentTypeId, createAttributeDefinitionRequest)));
  }

  @Override
  public ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition
      createCatalogAttribute(
          Long domainId, CreateAttributeDefinitionRequest createAttributeDefinitionRequest) {
    validateCreateAttributeDefinitionRequest(createAttributeDefinitionRequest);
    return toDtoWithLinks(
        attributeService.createInDomain(
            domainId, attributeDefinitionMapper.toModel(createAttributeDefinitionRequest)));
  }

  @Override
  public ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition attachAttribute(
      Long componentTypeId, Long attributeId, ComponentTypeAttributeSettingsRequest settings) {
    return toDtoWithLinks(
        attributeService.attachToComponentType(
            componentTypeId, attributeId, settings.getIsRequired(), settings.getOrderIndex()));
  }

  @Override
  public void detachAttribute(Long componentTypeId, Long attributeId) {
    attributeService.detachFromComponentType(componentTypeId, attributeId);
  }

  @Override
  public void deleteAttribute(Long id) {
    attributeService.deleteById(id);
  }

  private void validateCreateAttributeDefinitionRequest(
      CreateAttributeDefinitionRequest createAttributeDefinitionRequest) {
    String name = createAttributeDefinitionRequest.getName();
    validateName(name);
  }

  private static void validateName(String name) {
    if (!hasText(name)) {
      throw new ValidationException("Name is required");
    }
  }

  @Override
  public List<ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition>
      getAttributesByComponentTypeId(Long componentTypeId) {
    log.info("Start get attributes by component type id: {}", componentTypeId);
    return attributeDefinitionMapper.toDtoList(
        attributeService.getByComponentTypeId(componentTypeId));
  }

  @Override
  public List<ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition>
      getAttributesByDomainId(Long domainId) {
    return attributeService.getByDomainId(domainId).stream().map(this::toDtoWithLinks).toList();
  }

  private ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition toDtoWithLinks(
      ru.sultanyarov.configurator.domain.model.AttributeDefinition model) {
    var dto = attributeDefinitionMapper.toDto(model);
    dto.setComponentTypeIds(attributeService.getComponentTypeIds(model.id()));
    return dto;
  }
}
