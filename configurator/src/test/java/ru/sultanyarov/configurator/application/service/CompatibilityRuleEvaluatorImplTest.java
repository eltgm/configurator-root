package ru.sultanyarov.configurator.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.sultanyarov.configurator.domain.model.AttributeValue;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleCondition;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleOperator;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.DataType;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CompatibilityRuleEvaluatorImplTest {
    private final CompatibilityRuleEvaluator evaluator = new CompatibilityRuleEvaluatorImpl();

    @ParameterizedTest
    @MethodSource("matchingComparisons")
    void matches_shouldEvaluateSupportedOperators(
            DataType dataType,
            String left,
            CompatibilityRuleOperator operator,
            String right
    ) {
        assertThat(evaluator.matches(
                rule(true, condition(101L, operator, 201L)),
                component(1L, 10L, value(101L, dataType, left)),
                component(2L, 20L, value(201L, dataType, right))
        )).isTrue();
    }

    @Test
    void matches_shouldRequireEveryCondition() {
        CompatibilityRuleSet rule = rule(
                true,
                condition(101L, CompatibilityRuleOperator.EQUALS, 201L),
                condition(102L, CompatibilityRuleOperator.GTE, 202L)
        );

        assertThat(evaluator.matches(
                rule,
                component(
                        1L,
                        10L,
                        value(101L, DataType.STRING, "AM5"),
                        value(102L, DataType.NUMBER, "100")
                ),
                component(
                        2L,
                        20L,
                        value(201L, DataType.STRING, "AM5"),
                        value(202L, DataType.NUMBER, "200")
                )
        )).isFalse();
    }

    @Test
    void matches_shouldTreatNumericallyEqualScalesAsEqual() {
        assertThat(evaluator.matches(
                rule(true, condition(101L, CompatibilityRuleOperator.EQUALS, 201L)),
                component(1L, 10L, value(101L, DataType.NUMBER, "1.0")),
                component(2L, 20L, value(201L, DataType.NUMBER, "1.00"))
        )).isTrue();
    }

    @Test
    void matches_shouldReturnFalseForMissingAttributeValue() {
        assertThat(evaluator.matches(
                rule(true, condition(101L, CompatibilityRuleOperator.EQUALS, 201L)),
                component(1L, 10L, value(101L, DataType.STRING, "AM5")),
                component(2L, 20L)
        )).isFalse();
    }

    @Test
    void matches_shouldReturnFalseForDisabledOrEmptyRule() {
        Component componentA = component(1L, 10L, value(101L, DataType.STRING, "AM5"));
        Component componentB = component(2L, 20L, value(201L, DataType.STRING, "AM5"));

        assertThat(evaluator.matches(
                rule(false, condition(101L, CompatibilityRuleOperator.EQUALS, 201L)),
                componentA,
                componentB
        )).isFalse();
        assertThat(evaluator.matches(rule(true), componentA, componentB)).isFalse();
    }

    @Test
    void matches_shouldReturnFalseForWrongComponentOrientation() {
        CompatibilityRuleSet rule =
                rule(true, condition(101L, CompatibilityRuleOperator.EQUALS, 201L));

        assertThat(evaluator.matches(
                rule,
                component(2L, 20L, value(201L, DataType.STRING, "AM5")),
                component(1L, 10L, value(101L, DataType.STRING, "AM5"))
        )).isFalse();
    }

    @Test
    void matches_shouldRejectDirectionalOperatorForNonNumberAndMalformedValues() {
        assertThat(evaluator.matches(
                rule(true, condition(101L, CompatibilityRuleOperator.GT, 201L)),
                component(1L, 10L, value(101L, DataType.STRING, "Z")),
                component(2L, 20L, value(201L, DataType.STRING, "A"))
        )).isFalse();
        assertThat(evaluator.matches(
                rule(true, condition(101L, CompatibilityRuleOperator.GT, 201L)),
                component(1L, 10L, value(101L, DataType.NUMBER, "invalid")),
                component(2L, 20L, value(201L, DataType.NUMBER, "1"))
        )).isFalse();
    }

    private static Stream<Object[]> matchingComparisons() {
        return Stream.of(
                new Object[]{DataType.STRING, "AM5", CompatibilityRuleOperator.EQUALS, "AM5"},
                new Object[]{DataType.ENUM, "DDR5", CompatibilityRuleOperator.NOT_EQUALS, "DDR4"},
                new Object[]{DataType.BOOLEAN, "true", CompatibilityRuleOperator.EQUALS, "true"},
                new Object[]{DataType.NUMBER, "200", CompatibilityRuleOperator.GT, "100"},
                new Object[]{DataType.NUMBER, "200", CompatibilityRuleOperator.GTE, "200"},
                new Object[]{DataType.NUMBER, "100", CompatibilityRuleOperator.LT, "200"},
                new Object[]{DataType.NUMBER, "100", CompatibilityRuleOperator.LTE, "100"}
        );
    }

    private static CompatibilityRuleSet rule(
            boolean enabled,
            CompatibilityRuleCondition... conditions
    ) {
        return CompatibilityRuleSet.builder()
                .domainId(1L)
                .componentTypeAId(10L)
                .componentTypeBId(20L)
                .enabled(enabled)
                .conditions(List.of(conditions))
                .build();
    }

    private static CompatibilityRuleCondition condition(
            Long leftId,
            CompatibilityRuleOperator operator,
            Long rightId
    ) {
        return CompatibilityRuleCondition.builder()
                .leftAttributeDefinitionId(leftId)
                .operator(operator)
                .rightAttributeDefinitionId(rightId)
                .build();
    }

    private static Component component(
            Long id,
            Long componentTypeId,
            AttributeValue... values
    ) {
        return Component.builder()
                .id(id)
                .componentTypeId(componentTypeId)
                .attributes(List.of(values))
                .archived(false)
                .build();
    }

    private static AttributeValue value(
            Long definitionId,
            DataType dataType,
            String value
    ) {
        return AttributeValue.builder()
                .attributeDefinitionId(definitionId)
                .dataType(dataType)
                .value(value)
                .build();
    }
}
