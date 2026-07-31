package ru.sultanyarov.configurator.application.validator;

import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleCondition;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleOperator;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.DataType;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CompatibilityRuleValidatorImpl implements CompatibilityRuleValidator {

    @Override
    public void validate(
            CompatibilityRuleSet ruleSet,
            ComponentType componentTypeA,
            ComponentType componentTypeB
    ) {
        validateRuleSetFields(ruleSet);
        validateComponentTypes(ruleSet, componentTypeA, componentTypeB);

        Map<Long, AttributeDefinition> attributesA = attributesById(componentTypeA);
        Map<Long, AttributeDefinition> attributesB = attributesById(componentTypeB);
        var conditionKeys = new HashSet<ConditionKey>();

        for (CompatibilityRuleCondition condition : ruleSet.conditions()) {
            validateConditionFields(condition);
            AttributeDefinition leftAttribute = attributesA.get(condition.leftAttributeDefinitionId());
            if (leftAttribute == null) {
                throw new ValidationException(
                        "Attribute {} does not belong to component type {}",
                        condition.leftAttributeDefinitionId(),
                        componentTypeA.id()
                );
            }
            AttributeDefinition rightAttribute = attributesB.get(condition.rightAttributeDefinitionId());
            if (rightAttribute == null) {
                throw new ValidationException(
                        "Attribute {} does not belong to component type {}",
                        condition.rightAttributeDefinitionId(),
                        componentTypeB.id()
                );
            }
            validateDataTypes(condition, leftAttribute, rightAttribute);
            if (!conditionKeys.add(new ConditionKey(
                    condition.leftAttributeDefinitionId(),
                    condition.operator(),
                    condition.rightAttributeDefinitionId()
            ))) {
                throw new ValidationException("Compatibility rule conditions must be unique");
            }
        }
    }

    private static void validateRuleSetFields(CompatibilityRuleSet ruleSet) {
        if (ruleSet == null
                || ruleSet.domainId() == null
                || ruleSet.name() == null
                || ruleSet.name().isBlank()
                || ruleSet.componentTypeAId() == null
                || ruleSet.componentTypeBId() == null
                || ruleSet.enabled() == null) {
            throw new ValidationException("Compatibility rule set has missing required fields");
        }
        if (ruleSet.name().length() > 255) {
            throw new ValidationException("Compatibility rule set name must not exceed 255 characters");
        }
        if (ruleSet.componentTypeAId().equals(ruleSet.componentTypeBId())) {
            throw new ValidationException("Compatibility rule set must connect different component types");
        }
        if (ruleSet.componentTypeAId() >= ruleSet.componentTypeBId()) {
            throw new ValidationException("Compatibility rule-set component types are not normalized");
        }
        if (ruleSet.conditions() == null || ruleSet.conditions().isEmpty()) {
            throw new ValidationException("Compatibility rule set must contain at least one condition");
        }
    }

    private static void validateComponentTypes(
            CompatibilityRuleSet ruleSet,
            ComponentType componentTypeA,
            ComponentType componentTypeB
    ) {
        if (!ruleSet.componentTypeAId().equals(componentTypeA.id())
                || !ruleSet.componentTypeBId().equals(componentTypeB.id())) {
            throw new ValidationException("Compatibility rule-set component types do not match loaded types");
        }
        if (!ruleSet.domainId().equals(componentTypeA.domainId())
                || !ruleSet.domainId().equals(componentTypeB.domainId())) {
            throw new ValidationException(
                    "Compatibility rule-set component types must belong to domain {}",
                    ruleSet.domainId()
            );
        }
    }

    private static void validateConditionFields(CompatibilityRuleCondition condition) {
        if (condition == null
                || condition.leftAttributeDefinitionId() == null
                || condition.operator() == null
                || condition.rightAttributeDefinitionId() == null
                || condition.orderIndex() == null) {
            throw new ValidationException("Compatibility rule condition has missing required fields");
        }
        if (condition.orderIndex() < 0) {
            throw new ValidationException("Compatibility rule condition order index must be non-negative");
        }
    }

    private static void validateDataTypes(
            CompatibilityRuleCondition condition,
            AttributeDefinition leftAttribute,
            AttributeDefinition rightAttribute
    ) {
        if (leftAttribute.dataType() != rightAttribute.dataType()) {
            throw new ValidationException(
                    "Compatibility rule attributes {} and {} must have the same data type",
                    leftAttribute.id(),
                    rightAttribute.id()
            );
        }
        if (isDirectional(condition.operator()) && leftAttribute.dataType() != DataType.NUMBER) {
            throw new ValidationException(
                    "Operator {} is supported only for NUMBER attributes",
                    condition.operator()
            );
        }
    }

    private static boolean isDirectional(CompatibilityRuleOperator operator) {
        return switch (operator) {
            case GT, GTE, LT, LTE -> true;
            case EQUALS, NOT_EQUALS -> false;
        };
    }

    private static Map<Long, AttributeDefinition> attributesById(ComponentType componentType) {
        List<AttributeDefinition> attributes = componentType.attributeDefinitions() == null
                ? List.of()
                : componentType.attributeDefinitions();
        return attributes.stream()
                .collect(Collectors.toMap(AttributeDefinition::id, Function.identity()));
    }

    private record ConditionKey(
            Long leftAttributeDefinitionId,
            CompatibilityRuleOperator operator,
            Long rightAttributeDefinitionId
    ) {
    }
}
