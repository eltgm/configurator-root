package ru.sultanyarov.configurator.application.service;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.sultanyarov.configurator.application.port.out.AttributeRepository;
import ru.sultanyarov.configurator.application.port.out.AttributeValueRepository;
import ru.sultanyarov.configurator.application.port.out.ComponentTypeRepository;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.DataType;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttributeServiceImpl implements AttributeService {
  private final AttributeRepository attributeRepository;
  private final AttributeValueRepository attributeValueRepository;
  private final ComponentTypeRepository componentTypeRepository;

  @Override
  public AttributeDefinition create(AttributeDefinition attributeDefinition) {
    log.debug("create attribute definition {}", attributeDefinition);
    validateIsComponentTypeExists(attributeDefinition.componentTypeId());
    validateIsNoAttributeDefinitionWithSameNameAndComponentType(attributeDefinition);
    validateIsDataTypeCorrect(attributeDefinition);

    return attributeRepository
        .createAttributeDefinition(attributeDefinition)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Component type with id {} not found while creating attribute definition",
                    attributeDefinition.componentTypeId()));
  }

  private void validateIsComponentTypeExists(Long componentTypeId) {
    if (!componentTypeRepository.existsById(componentTypeId)) {
      throw new NotFoundException(
          "Component type with id {} not found while creating attribute definition",
          componentTypeId);
    }
  }

  private void validateIsNoAttributeDefinitionWithSameNameAndComponentType(
      AttributeDefinition attributeDefinition) {
    if (attributeRepository.hasByComponentTypeIdAndName(
        attributeDefinition.componentTypeId(), attributeDefinition.name())) {
      throw new EntityAlreadyExistsException(
          "Attribute definition with name {} already exists for component type with id {}",
          attributeDefinition.name(),
          attributeDefinition.componentTypeId());
    }
  }

  private void validateIsDataTypeCorrect(AttributeDefinition attributeDefinition) {
    if (attributeDefinition.dataType() == DataType.ENUM) {
      if (CollectionUtils.isEmpty(attributeDefinition.enumValues())) {
        throw new ValidationException("Enum values must be provided for enum data type");
      }
    }
  }

  @Override
  @Transactional
  public AttributeDefinition update(Long id, AttributeDefinition attributeDefinition) {
    log.debug("update attribute definition {} with id {}", attributeDefinition, id);
    AttributeDefinition existedAttributeDefinition = getById(id);
    AttributeDefinition updatedAttributeDefinition =
        merge(existedAttributeDefinition, attributeDefinition);
    if (!Objects.equals(existedAttributeDefinition.name(), updatedAttributeDefinition.name())) {
      validateIsNoAttributeDefinitionWithSameNameAndComponentType(updatedAttributeDefinition);
    }
    validateIsDataTypeCorrect(updatedAttributeDefinition);
    validateDataTypeCanBeChanged(existedAttributeDefinition, updatedAttributeDefinition);

    return attributeRepository
        .updateAttribute(id, updatedAttributeDefinition)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Attribute definition with id {} not found while updating", id));
  }

  private static AttributeDefinition merge(
      AttributeDefinition existing, AttributeDefinition replacement) {
    return AttributeDefinition.builder()
        .id(existing.id())
        .componentTypeId(existing.componentTypeId())
        .name(replacement.name())
        .label(replacement.label())
        .dataType(replacement.dataType())
        .enumValues(replacement.enumValues())
        .isRequired(replacement.isRequired())
        .orderIndex(replacement.orderIndex())
        .createdAt(existing.createdAt())
        .build();
  }

  private void validateDataTypeCanBeChanged(
      AttributeDefinition existing, AttributeDefinition replacement) {
    if (existing.dataType() != replacement.dataType()
        && attributeValueRepository.existsByAttributeDefinitionId(existing.id())) {
      throw new ValidationException(
          "Cannot change data type of attribute definition with id {} because it has persisted values",
          existing.id());
    }
  }

  @Override
  public void deleteById(Long id) {
    log.debug("delete attribute definition with id {}", id);
    ensureAttributeExists(id);

    attributeRepository.deleteById(id);
  }

  private void ensureAttributeExists(Long id) {
    if (!attributeRepository.existsById(id)) {
      throw new NotFoundException(
          "Attribute definition with id {} not found while updating or deleting", id);
    }
  }

  @Override
  public List<AttributeDefinition> getByComponentTypeId(Long componentTypeId) {
    log.debug("get attribute definitions by component type id {}", componentTypeId);
    validateIsComponentTypeExists(componentTypeId);

    return attributeRepository.getByComponentTypeId(componentTypeId);
  }

  @Override
  public AttributeDefinition getById(Long id) {
    return attributeRepository
        .getById(id)
        .orElseThrow(() -> new NotFoundException("Attribute definition with id {} not found", id));
  }
}
