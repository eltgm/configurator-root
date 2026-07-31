package ru.sultanyarov.configurator.application.facade;

import static org.springframework.util.StringUtils.hasText;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
    return attributeDefinitionMapper.toDto(
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
}
