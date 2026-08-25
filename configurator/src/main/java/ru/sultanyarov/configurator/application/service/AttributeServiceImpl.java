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
import ru.sultanyarov.configurator.application.port.out.CompatibilityRuleRepository;
import ru.sultanyarov.configurator.application.port.out.ComponentTypeAttributeRepository;
import ru.sultanyarov.configurator.application.port.out.ComponentTypeRepository;
import ru.sultanyarov.configurator.application.port.out.DomainRepository;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.EntityHasRelatedEntitiesException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.ComponentTypeAttribute;
import ru.sultanyarov.configurator.domain.model.DataType;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttributeServiceImpl implements AttributeService {
  private final AttributeRepository attributeRepository;
  private final AttributeValueRepository attributeValueRepository;
  private final ComponentTypeRepository componentTypeRepository;
  private final ComponentTypeAttributeRepository componentTypeAttributeRepository;
  private final DomainRepository domainRepository;
  private final CompatibilityRuleRepository compatibilityRuleRepository;

  @Override
  @Transactional
  public AttributeDefinition create(AttributeDefinition attributeDefinition) {
    log.debug("create and attach attribute definition {}", attributeDefinition);
    ComponentType componentType = getComponentType(attributeDefinition.componentTypeId());
    validateNoAttributeWithSameName(componentType.id(), attributeDefinition.name());

    AttributeDefinition created = createInDomain(componentType.domainId(), attributeDefinition);
    ComponentTypeAttribute link =
        saveLink(
            componentType.id(),
            created.id(),
            attributeDefinition.isRequired(),
            attributeDefinition.orderIndex());
    return withLink(created, link);
  }

  @Override
  public AttributeDefinition createInDomain(
      Long domainId, AttributeDefinition attributeDefinition) {
    log.debug("create catalog attribute definition in domain {}", domainId);
    ensureDomainExists(domainId);
    validateIsDataTypeCorrect(attributeDefinition);
    AttributeDefinition catalogDefinition =
        AttributeDefinition.builder()
            .domainId(domainId)
            .name(attributeDefinition.name())
            .label(attributeDefinition.label())
            .dataType(attributeDefinition.dataType())
            .enumValues(attributeDefinition.enumValues())
            .build();
    return attributeRepository
        .createAttributeDefinition(catalogDefinition)
        .orElseThrow(() -> new BusinessException("Failed to create attribute definition"));
  }

  @Override
  @Transactional
  public AttributeDefinition attachToComponentType(
      Long componentTypeId, Long attributeDefinitionId, Boolean isRequired, Integer orderIndex) {
    ComponentType componentType = getComponentType(componentTypeId);
    AttributeDefinition definition = getById(attributeDefinitionId);
    if (!Objects.equals(componentType.domainId(), definition.domainId())) {
      throw new ValidationException(
          "Attribute definition with id {} does not belong to component type domain {}",
          attributeDefinitionId,
          componentType.domainId());
    }
    if (!componentTypeAttributeRepository.exists(componentTypeId, attributeDefinitionId)) {
      validateNoAttributeWithSameName(componentTypeId, definition.name());
    }
    return withLink(
        definition, saveLink(componentTypeId, attributeDefinitionId, isRequired, orderIndex));
  }

  @Override
  @Transactional
  public void detachFromComponentType(Long componentTypeId, Long attributeDefinitionId) {
    getComponentType(componentTypeId);
    getById(attributeDefinitionId);
    if (!componentTypeAttributeRepository.exists(componentTypeId, attributeDefinitionId)) {
      throw new NotFoundException(
          "Attribute definition with id {} is not linked to component type with id {}",
          attributeDefinitionId,
          componentTypeId);
    }
    attributeValueRepository.deleteByAttributeDefinitionIdAndComponentTypeId(
        attributeDefinitionId, componentTypeId);
    componentTypeAttributeRepository.delete(componentTypeId, attributeDefinitionId);
  }

  @Override
  @Transactional
  public AttributeDefinition update(Long id, AttributeDefinition attributeDefinition) {
    log.debug("update attribute definition {} with id {}", attributeDefinition, id);
    AttributeDefinition existing = getById(id);
    AttributeDefinition updated = merge(existing, attributeDefinition);
    if (!Objects.equals(existing.name(), updated.name())) {
      componentTypeAttributeRepository
          .getComponentTypeIdsByAttributeDefinitionId(id)
          .forEach(
              componentTypeId -> validateNoAttributeWithSameName(componentTypeId, updated.name()));
    }
    validateIsDataTypeCorrect(updated);
    validateDataTypeCanBeChanged(existing, updated);

    return attributeRepository
        .updateAttribute(id, updated)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Attribute definition with id {} not found while updating", id));
  }

  @Override
  @Transactional
  public void deleteById(Long id) {
    log.debug("delete catalog attribute definition with id {}", id);
    ensureAttributeExists(id);
    if (compatibilityRuleRepository.hasByAttributeDefinitionId(id)) {
      throw new EntityHasRelatedEntitiesException(
          "Cannot delete attribute definition with id {} because it is used by compatibility rules",
          id);
    }
    attributeRepository.deleteById(id);
  }

  @Override
  public List<AttributeDefinition> getByComponentTypeId(Long componentTypeId) {
    getComponentType(componentTypeId);
    return attributeRepository.getByComponentTypeId(componentTypeId);
  }

  @Override
  public List<AttributeDefinition> getByDomainId(Long domainId) {
    ensureDomainExists(domainId);
    return attributeRepository.getByDomainId(domainId);
  }

  @Override
  public List<Long> getComponentTypeIds(Long attributeDefinitionId) {
    ensureAttributeExists(attributeDefinitionId);
    return componentTypeAttributeRepository.getComponentTypeIdsByAttributeDefinitionId(
        attributeDefinitionId);
  }

  @Override
  public AttributeDefinition getById(Long id) {
    return attributeRepository
        .getById(id)
        .orElseThrow(() -> new NotFoundException("Attribute definition with id {} not found", id));
  }

  private ComponentType getComponentType(Long componentTypeId) {
    if (componentTypeId == null) {
      throw new ValidationException("Component type id is required");
    }
    return componentTypeRepository
        .getComponentTypeById(componentTypeId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Component type with id {} not found while managing attribute definition",
                    componentTypeId));
  }

  private void ensureDomainExists(Long domainId) {
    if (domainId == null || !domainRepository.existsById(domainId)) {
      throw new NotFoundException("Domain with id {} does not exist", domainId);
    }
  }

  private void validateNoAttributeWithSameName(Long componentTypeId, String name) {
    if (attributeRepository.hasByComponentTypeIdAndName(componentTypeId, name)) {
      throw new EntityAlreadyExistsException(
          "Attribute definition with name {} already exists for component type with id {}",
          name,
          componentTypeId);
    }
  }

  private ComponentTypeAttribute saveLink(
      Long componentTypeId, Long attributeDefinitionId, Boolean isRequired, Integer orderIndex) {
    return componentTypeAttributeRepository
        .save(
            ComponentTypeAttribute.builder()
                .componentTypeId(componentTypeId)
                .attributeDefinitionId(attributeDefinitionId)
                .isRequired(Boolean.TRUE.equals(isRequired))
                .orderIndex(orderIndex)
                .build())
        .orElseThrow(
            () ->
                new BusinessException(
                    "Failed to link attribute definition with id {} to component type with id {}",
                    attributeDefinitionId,
                    componentTypeId));
  }

  private static AttributeDefinition withLink(
      AttributeDefinition definition, ComponentTypeAttribute link) {
    return AttributeDefinition.builder()
        .id(definition.id())
        .domainId(definition.domainId())
        .componentTypeId(link.componentTypeId())
        .name(definition.name())
        .label(definition.label())
        .dataType(definition.dataType())
        .enumValues(definition.enumValues())
        .isRequired(link.isRequired())
        .orderIndex(link.orderIndex())
        .createdAt(definition.createdAt())
        .build();
  }

  private static AttributeDefinition merge(
      AttributeDefinition existing, AttributeDefinition replacement) {
    return AttributeDefinition.builder()
        .id(existing.id())
        .domainId(existing.domainId())
        .name(replacement.name())
        .label(replacement.label())
        .dataType(replacement.dataType())
        .enumValues(replacement.enumValues())
        .createdAt(existing.createdAt())
        .build();
  }

  private void validateIsDataTypeCorrect(AttributeDefinition attributeDefinition) {
    if (attributeDefinition.dataType() == DataType.ENUM
        && CollectionUtils.isEmpty(attributeDefinition.enumValues())) {
      throw new ValidationException("Enum values must be provided for enum data type");
    }
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

  private void ensureAttributeExists(Long id) {
    if (!attributeRepository.existsById(id)) {
      throw new NotFoundException(
          "Attribute definition with id {} not found while updating or deleting", id);
    }
  }
}
