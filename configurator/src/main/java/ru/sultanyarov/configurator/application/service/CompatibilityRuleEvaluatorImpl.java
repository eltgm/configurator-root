package ru.sultanyarov.configurator.application.service;

import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.domain.model.AttributeValue;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleCondition;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.DataType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CompatibilityRuleEvaluatorImpl implements CompatibilityRuleEvaluator {

    @Override
    public boolean matches(
            CompatibilityRuleSet ruleSet,
            Component componentA,
            Component componentB
    ) {
        if (ruleSet == null
                || !Boolean.TRUE.equals(ruleSet.enabled())
                || componentA == null
                || componentB == null
                || !ruleSet.componentTypeAId().equals(componentA.getComponentTypeId())
                || !ruleSet.componentTypeBId().equals(componentB.getComponentTypeId())
                || ruleSet.conditions() == null
                || ruleSet.conditions().isEmpty()) {
            return false;
        }

        Map<Long, AttributeValue> valuesA = valuesByDefinitionId(componentA);
        Map<Long, AttributeValue> valuesB = valuesByDefinitionId(componentB);
        return ruleSet.conditions().stream()
                .allMatch(condition -> matches(condition, valuesA, valuesB));
    }

    private static boolean matches(
            CompatibilityRuleCondition condition,
            Map<Long, AttributeValue> valuesA,
            Map<Long, AttributeValue> valuesB
    ) {
        AttributeValue left = valuesA.get(condition.leftAttributeDefinitionId());
        AttributeValue right = valuesB.get(condition.rightAttributeDefinitionId());
        if (left == null
                || right == null
                || left.value() == null
                || right.value() == null
                || left.dataType() == null
                || left.dataType() != right.dataType()) {
            return false;
        }
        if (switch (condition.operator()) {
            case GT, GTE, LT, LTE -> left.dataType() != DataType.NUMBER;
            case EQUALS, NOT_EQUALS -> false;
        }) {
            return false;
        }

        try {
            int comparison = compare(left, right);
            return switch (condition.operator()) {
                case EQUALS -> comparison == 0;
                case NOT_EQUALS -> comparison != 0;
                case GT -> comparison > 0;
                case GTE -> comparison >= 0;
                case LT -> comparison < 0;
                case LTE -> comparison <= 0;
            };
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static int compare(AttributeValue left, AttributeValue right) {
        return switch (left.dataType()) {
            case NUMBER -> new BigDecimal(left.value()).compareTo(new BigDecimal(right.value()));
            case BOOLEAN -> parseBoolean(left.value()).compareTo(parseBoolean(right.value()));
            case STRING, ENUM -> left.value().compareTo(right.value());
        };
    }

    private static Boolean parseBoolean(String value) {
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new IllegalArgumentException("Invalid boolean value");
        }
        return Boolean.valueOf(value);
    }

    private static Map<Long, AttributeValue> valuesByDefinitionId(Component component) {
        List<AttributeValue> values = component.getAttributes() == null
                ? List.of()
                : component.getAttributes();
        return values.stream()
                .collect(Collectors.toMap(AttributeValue::attributeDefinitionId, Function.identity()));
    }
}
