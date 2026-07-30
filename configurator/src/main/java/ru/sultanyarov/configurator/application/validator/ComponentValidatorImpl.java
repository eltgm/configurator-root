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
import java.util.Objects;

import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentValidatorImpl implements ComponentValidator {
    @Override
    public void validateCreation(Component componentToCreate, ComponentType componentType, Map<Long, AttributeDefinition> componentTypeAttributesMap) {
        List<Component> existedComponents = componentType.components() == null ? List.of() : componentType.components();
        validateIsNameUnique(componentToCreate, existedComponents, null);
        validateAttributes(componentToCreate, componentType, componentTypeAttributesMap);
    }

    @Override
    public void validateUpdate(
            Component componentToUpdate,
            Component existingComponent,
            ComponentType componentType,
            Map<Long, AttributeDefinition> componentTypeAttributesMap
    ) {
        if (!Objects.equals(existingComponent.getComponentTypeId(), componentToUpdate.getComponentTypeId())) {
            throw new ValidationException("Changing component type is not supported");
        }

        List<Component> existedComponents = componentType.components() == null ? List.of() : componentType.components();
        validateIsNameUnique(componentToUpdate, existedComponents, existingComponent.getId());
        validateAttributes(componentToUpdate, componentType, componentTypeAttributesMap);
    }

    private void validateAttributes(
            Component component,
            ComponentType componentType,
            Map<Long, AttributeDefinition> componentTypeAttributesMap
    ) {
        List<AttributeValue> componentAttributes = component.getAttributes() == null ? List.of() : component.getAttributes();
        Map<Long, AttributeValue> componentAttributesMap = HashMap.newHashMap(componentAttributes.size());
        for (AttributeValue componentAttribute : componentAttributes) {
            componentAttributesMap.put(componentAttribute.attributeDefinitionId(), componentAttribute);
        }

        validateIsNoDuplicate(componentAttributesMap, componentAttributes);
        List<AttributeDefinition> attributeDefinitions = componentType.attributeDefinitions() == null ? List.of() : componentType.attributeDefinitions();
        validateIsNoInvalidAttributesForComponent(componentAttributesMap, componentTypeAttributesMap, componentType);
        validateHasRequiredAttributes(attributeDefinitions, componentAttributesMap);
        validateAttributeValues(componentAttributes, componentTypeAttributesMap);
    }

    private void validateIsNameUnique(Component component, List<Component> existedComponents, Long ignoredComponentId) {
        boolean isNameNotUnique = existedComponents
                .stream()
                .filter(existedComponent -> ignoredComponentId == null
                        || !Objects.equals(existedComponent.getId(), ignoredComponentId))
                .map(Component::getName)
                .anyMatch(component.getName()::equals);
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

    private void validateAttributeValues(List<AttributeValue> newComponentAttributes, Map<Long, AttributeDefinition> componentTypeAttributesMap) {
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
