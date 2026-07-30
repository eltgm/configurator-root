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
    void evaluate_shouldEvaluateSupportedOperators(
            DataType dataType,
            String left,
            CompatibilityRuleOperator operator,
            String right
    ) {
        assertThat(evaluator.evaluate(
                rule(true, condition(101L, operator, 201L)),
                component(1L, 10L, value(101L, dataType, left)),
                component(2L, 20L, value(201L, dataType, right))
        )).isPresent();
    }

    @Test
    void evaluate_shouldReturnRuleAndConditionExplanationsWhenEveryConditionMatches() {
        CompatibilityRuleSet rule = rule(
                true,
                condition(101L, CompatibilityRuleOperator.EQUALS, 201L),
                condition(102L, CompatibilityRuleOperator.LTE, 202L)
        );

        assertThat(evaluator.evaluate(
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
        )).hasValueSatisfying(match -> {
            assertThat(match.ruleSetId()).isEqualTo(7L);
            assertThat(match.ruleSetName()).isEqualTo("Rule");
            assertThat(match.conditions()).hasSize(2);
            assertThat(match.conditions().getFirst()).satisfies(explanation -> {
                assertThat(explanation.leftAttributeDefinitionId()).isEqualTo(101L);
                assertThat(explanation.leftAttributeName()).isEqualTo("attribute-101");
                assertThat(explanation.leftValue()).isEqualTo("AM5");
                assertThat(explanation.operator()).isEqualTo(CompatibilityRuleOperator.EQUALS);
                assertThat(explanation.rightAttributeDefinitionId()).isEqualTo(201L);
                assertThat(explanation.rightAttributeName()).isEqualTo("attribute-201");
                assertThat(explanation.rightValue()).isEqualTo("AM5");
            });
        });
    }

    @Test
    void evaluate_shouldRequireEveryCondition() {
        CompatibilityRuleSet rule = rule(
                true,
                condition(101L, CompatibilityRuleOperator.EQUALS, 201L),
                condition(102L, CompatibilityRuleOperator.GTE, 202L)
        );

        assertThat(evaluator.evaluate(
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
        )).isEmpty();
    }

    @Test
    void evaluate_shouldTreatNumericallyEqualScalesAsEqual() {
        assertThat(evaluator.evaluate(
                rule(true, condition(101L, CompatibilityRuleOperator.EQUALS, 201L)),
                component(1L, 10L, value(101L, DataType.NUMBER, "1.0")),
                component(2L, 20L, value(201L, DataType.NUMBER, "1.00"))
        )).isPresent();
    }

    @Test
    void evaluate_shouldReturnEmptyForMissingAttributeValue() {
        assertThat(evaluator.evaluate(
                rule(true, condition(101L, CompatibilityRuleOperator.EQUALS, 201L)),
                component(1L, 10L, value(101L, DataType.STRING, "AM5")),
                component(2L, 20L)
        )).isEmpty();
    }

    @Test
    void evaluate_shouldReturnEmptyForDisabledOrEmptyRule() {
        Component componentA = component(1L, 10L, value(101L, DataType.STRING, "AM5"));
        Component componentB = component(2L, 20L, value(201L, DataType.STRING, "AM5"));

        assertThat(evaluator.evaluate(
                rule(false, condition(101L, CompatibilityRuleOperator.EQUALS, 201L)),
                componentA,
                componentB
        )).isEmpty();
        assertThat(evaluator.evaluate(rule(true), componentA, componentB)).isEmpty();
    }

    @Test
    void evaluate_shouldReturnEmptyForWrongComponentOrientation() {
        CompatibilityRuleSet rule =
                rule(true, condition(101L, CompatibilityRuleOperator.EQUALS, 201L));

        assertThat(evaluator.evaluate(
                rule,
                component(2L, 20L, value(201L, DataType.STRING, "AM5")),
                component(1L, 10L, value(101L, DataType.STRING, "AM5"))
        )).isEmpty();
    }

    @Test
    void evaluate_shouldRejectDirectionalOperatorForNonNumberAndMalformedValues() {
        assertThat(evaluator.evaluate(
                rule(true, condition(101L, CompatibilityRuleOperator.GT, 201L)),
                component(1L, 10L, value(101L, DataType.STRING, "Z")),
                component(2L, 20L, value(201L, DataType.STRING, "A"))
        )).isEmpty();
        assertThat(evaluator.evaluate(
                rule(true, condition(101L, CompatibilityRuleOperator.GT, 201L)),
                component(1L, 10L, value(101L, DataType.NUMBER, "invalid")),
                component(2L, 20L, value(201L, DataType.NUMBER, "1"))
        )).isEmpty();
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
                .id(7L)
                .domainId(1L)
                .name("Rule")
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
                .name("attribute-" + definitionId)
                .dataType(dataType)
                .value(value)
                .build();
    }
}
