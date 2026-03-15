package ru.sultanyarov.configurator.application.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.AttributeValue;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentValidatorImpl implements ComponentValidator {
    @Override
    public void validateCreation(Component componentToCreate, ComponentType componentType, Map<Long, AttributeDefinition> componentTypeAttributesMap) {
        List<Component> existedComponents = componentType.components() == null ? List.of() : componentType.components();
        validateIsNameUnique(componentToCreate, existedComponents);

        List<AttributeValue> newComponentAttributes = componentToCreate.getAttributes() == null ? List.of() : componentToCreate.getAttributes();
        Map<Long, AttributeValue> newComponentAttributesMap = HashMap.newHashMap(newComponentAttributes.size());
        for (AttributeValue createdAttribute : newComponentAttributes) {
            newComponentAttributesMap.put(createdAttribute.attributeDefinitionId(), createdAttribute);
        }

        validateIsNoDuplicate(newComponentAttributesMap, newComponentAttributes);

        List<AttributeDefinition> attributeDefinitions = componentType.attributeDefinitions() == null ? List.of() : componentType.attributeDefinitions();
        validateIsNoInvalidAttributesForComponent(newComponentAttributesMap, componentTypeAttributesMap, componentType);
        validateHasRequiredAttributes(attributeDefinitions, newComponentAttributesMap);
        validateAttributes(newComponentAttributes, componentTypeAttributesMap);
    }

    private void validateIsNameUnique(Component componentToCreate, List<Component> existedComponents) {
        boolean isNameNotUnique = existedComponents
                .stream()
                .map(Component::getName)
                .anyMatch(componentToCreate.getName()::equals);
        if (isNameNotUnique) {
            throw new EntityAlreadyExistsException("Component name must be unique");
        }
    }

    private void validateIsNoDuplicate(Map<Long, AttributeValue> newComponentAttributesMap, List<AttributeValue> newComponentAttributes) {
        if (newComponentAttributesMap.size() != newComponentAttributes.size()) {
            throw new ValidationException("Repeated attribute ids");
        }
    }

    private void validateIsNoInvalidAttributesForComponent(Map<Long, AttributeValue> newComponentAttributesMap, Map<Long, AttributeDefinition> componentTypeAttributesMap, ComponentType componentType) {
        var attributesToAddIdsCopy = new HashSet<>(newComponentAttributesMap.keySet());
        attributesToAddIdsCopy.removeAll(componentTypeAttributesMap.keySet());
        if (!attributesToAddIdsCopy.isEmpty()) {
            throw new ValidationException("Incorrect attributes for component type {}", componentType.id());
        }
    }

    private void validateHasRequiredAttributes(List<AttributeDefinition> attributeDefinitions, Map<Long, AttributeValue> newComponentAttributesMap) {
        List<Long> requiredAttributesIds = attributeDefinitions
                .stream()
                .filter(AttributeDefinition::isRequired)
                .map(AttributeDefinition::id)
                .toList();

        for (Long requiredAttributesId : requiredAttributesIds) {
            if (!newComponentAttributesMap.containsKey(requiredAttributesId) || isEmpty(newComponentAttributesMap.get(requiredAttributesId).value())) {
                throw new ValidationException("Required attribute {} is missing", requiredAttributesId);
            }
        }
    }

    private void validateAttributes(List<AttributeValue> newComponentAttributes, Map<Long, AttributeDefinition> componentTypeAttributesMap) {
        newComponentAttributes.forEach(attributeValue -> {
            AttributeDefinition attributeDefinition = componentTypeAttributesMap.get(attributeValue.attributeDefinitionId());

            String createdValue = attributeValue.value();
            switch (attributeDefinition.dataType()) {
                case STRING -> {
                    if (createdValue == null) {
                        throw new ValidationException("Incorrect string value");
                    }
                }
                case NUMBER -> {
                    if (!NumberUtils.isCreatable(createdValue)) {
                        throw new ValidationException("Incorrect number value");
                    }
                }
                case BOOLEAN -> {
                    if (!("true".equals(createdValue) || "false".equals(createdValue))) {
                        throw new ValidationException("Incorrect boolean value");
                    }

                }
                case ENUM -> {
                    if (!attributeDefinition.enumValues().contains(createdValue)) {
                        throw new ValidationException("Incorrect enum value");
                    }
                }
            }
        });
    }
}
