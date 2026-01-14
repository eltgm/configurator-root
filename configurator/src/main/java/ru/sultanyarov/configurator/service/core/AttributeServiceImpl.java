package ru.sultanyarov.configurator.service.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.sultanyarov.configurator.domain.dto.CreateAttributeDefinitionRequest;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.DataType;
import ru.sultanyarov.configurator.domain.repository.AttributeRepository;
import ru.sultanyarov.configurator.domain.repository.ComponentTypeRepository;
import ru.sultanyarov.configurator.service.mapper.AttributeMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttributeServiceImpl implements AttributeService {
    private final AttributeRepository attributeRepository;
    private final ComponentTypeRepository componentTypeRepository;
    private final AttributeMapper attributeMapper;

    @Override
    public AttributeDefinition create(AttributeDefinition attributeDefinition) {
        log.debug("create attribute definition {}", attributeDefinition);
        ensureComponentTypeExists(attributeDefinition.componentTypeId());
        ensureNoAttributeDefinitionWithSameNameAndComponentType(attributeDefinition);
        ensureDataTypeCorrect(attributeDefinition);

        return attributeRepository.createAttributeDefinition(attributeDefinition)
                .orElseThrow(() -> new NotFoundException("Component type with id {} not found while creating attribute definition"));
    }

    private void ensureComponentTypeExists(Long componentId) {
        if (!componentTypeRepository.existsById(componentId)) {
            throw new NotFoundException("Component type with id {} not found while creating attribute definition");
        }
    }

    private void ensureNoAttributeDefinitionWithSameNameAndComponentType(AttributeDefinition attributeDefinition) {
        if (attributeRepository.hasByComponentTypeIdAndName(attributeDefinition.componentTypeId(), attributeDefinition.name())) {
            throw new EntityAlreadyExistsException("Attribute definition with name {} already exists for component type with id {}",
                    attributeDefinition.name(), attributeDefinition.componentTypeId());
        }
    }

    private void ensureDataTypeCorrect(AttributeDefinition attributeDefinition) {
        if (attributeDefinition.dataType() == DataType.ENUM) {
            if (CollectionUtils.isEmpty(attributeDefinition.enumValues())) {
                throw new ValidationException("Enum values must be provided for enum data type");
            }
        }
    }

    @Override
    public AttributeDefinition update(Long id, CreateAttributeDefinitionRequest createAttributeDefinitionRequest) {
        log.debug("update attribute definition {} with id {}", createAttributeDefinitionRequest, id);
        AttributeDefinition existedAttributeDefinition = getById(id);
        AttributeDefinition updatedAttributeDefinition = attributeMapper.updateModel(existedAttributeDefinition, createAttributeDefinitionRequest);
        ensureNoAttributeDefinitionWithSameNameAndComponentType(updatedAttributeDefinition);

        return attributeRepository.updateAttribute(id, updatedAttributeDefinition)
                .orElseThrow(() -> new NotFoundException("Attribute definition with id {} not found while updating", id));
    }

    @Override
    public void deleteById(Long id) {
        log.debug("delete attribute definition with id {}", id);
        ensureAttributeExists(id);

        attributeRepository.deleteById(id);
    }

    private void ensureAttributeExists(Long id) {
        if (!attributeRepository.existsById(id)) {
            throw new NotFoundException("Attribute definition with id {} not found while updating or deleting");
        }
    }

    @Override
    public List<AttributeDefinition> getByComponentTypeId(Long componentTypeId) {
        log.debug("get attribute definitions by component type id {}", componentTypeId);
        ensureComponentTypeExists(componentTypeId);

        return attributeRepository.getByComponentTypeId(componentTypeId);
    }

    @Override
    public AttributeDefinition getById(Long id) {
        return attributeRepository.getById(id)
                .orElseThrow(() -> new NotFoundException("Attribute definition with id {} not found", id));
    }
}
