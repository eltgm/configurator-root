package ru.sultanyarov.configurator.application.service;

import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.domain.model.AttributeValue;
import ru.sultanyarov.configurator.domain.model.CompatibilityConditionExplanation;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleCondition;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleMatch;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.DataType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CompatibilityRuleEvaluatorImpl implements CompatibilityRuleEvaluator {

    @Override
    public Optional<CompatibilityRuleMatch> evaluate(
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
            return Optional.empty();
        }

        Map<Long, AttributeValue> valuesA = valuesByDefinitionId(componentA);
        Map<Long, AttributeValue> valuesB = valuesByDefinitionId(componentB);
        List<CompatibilityConditionExplanation> explanations = new ArrayList<>();
        for (CompatibilityRuleCondition condition : ruleSet.conditions()) {
            Optional<CompatibilityConditionExplanation> explanation =
                    evaluate(condition, valuesA, valuesB);
            if (explanation.isEmpty()) {
                return Optional.empty();
            }
            explanations.add(explanation.get());
        }
        return Optional.of(CompatibilityRuleMatch.builder()
                .ruleSetId(ruleSet.id())
                .ruleSetName(ruleSet.name())
                .conditions(List.copyOf(explanations))
                .build());
    }

    private static Optional<CompatibilityConditionExplanation> evaluate(
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
            return Optional.empty();
        }
        if (switch (condition.operator()) {
            case GT, GTE, LT, LTE -> left.dataType() != DataType.NUMBER;
            case EQUALS, NOT_EQUALS -> false;
        }) {
            return Optional.empty();
        }

        try {
            int comparison = compare(left, right);
            boolean matches = switch (condition.operator()) {
                case EQUALS -> comparison == 0;
                case NOT_EQUALS -> comparison != 0;
                case GT -> comparison > 0;
                case GTE -> comparison >= 0;
                case LT -> comparison < 0;
                case LTE -> comparison <= 0;
            };
            if (!matches) {
                return Optional.empty();
            }
            return Optional.of(CompatibilityConditionExplanation.builder()
                    .leftAttributeDefinitionId(condition.leftAttributeDefinitionId())
                    .leftAttributeName(left.name())
                    .leftValue(left.value())
                    .operator(condition.operator())
                    .rightAttributeDefinitionId(condition.rightAttributeDefinitionId())
                    .rightAttributeName(right.name())
                    .rightValue(right.value())
                    .build());
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
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
