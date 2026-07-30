package ru.sultanyarov.configurator.application.validator;

import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleCondition;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleOperator;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.DataType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompatibilityRuleValidatorImplTest {
    private final CompatibilityRuleValidator validator = new CompatibilityRuleValidatorImpl();

    @Test
    void validate_shouldAcceptEqualStringAttributes() {
        CompatibilityRuleSet ruleSet = ruleSet(
                condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0)
        );

        assertThatCode(() -> validator.validate(
                ruleSet,
                componentType(10L, 1L, attribute(101L, 10L, DataType.STRING)),
                componentType(20L, 1L, attribute(201L, 20L, DataType.STRING))
        )).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldAcceptDirectionalNumberAttributes() {
        CompatibilityRuleSet ruleSet = ruleSet(
                condition(101L, CompatibilityRuleOperator.GTE, 201L, 0)
        );

        assertThatCode(() -> validator.validate(
                ruleSet,
                componentType(10L, 1L, attribute(101L, 10L, DataType.NUMBER)),
                componentType(20L, 1L, attribute(201L, 20L, DataType.NUMBER))
        )).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectEmptyConditions() {
        CompatibilityRuleSet ruleSet = CompatibilityRuleSet.builder()
                .domainId(1L)
                .name("Rule")
                .componentTypeAId(10L)
                .componentTypeBId(20L)
                .enabled(true)
                .conditions(List.of())
                .build();

        assertThatThrownBy(() -> validator.validate(
                ruleSet,
                componentType(10L, 1L),
                componentType(20L, 1L)
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Compatibility rule set must contain at least one condition");
    }

    @Test
    void validate_shouldRejectComponentTypeOutsideDomain() {
        assertThatThrownBy(() -> validator.validate(
                ruleSet(condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0)),
                componentType(10L, 1L, attribute(101L, 10L, DataType.STRING)),
                componentType(20L, 2L, attribute(201L, 20L, DataType.STRING))
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Compatibility rule-set component types must belong to domain 1");
    }

    @Test
    void validate_shouldRejectAttributeOnWrongSide() {
        assertThatThrownBy(() -> validator.validate(
                ruleSet(condition(201L, CompatibilityRuleOperator.EQUALS, 101L, 0)),
                componentType(10L, 1L, attribute(101L, 10L, DataType.STRING)),
                componentType(20L, 1L, attribute(201L, 20L, DataType.STRING))
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Attribute 201 does not belong to component type 10");
    }

    @Test
    void validate_shouldRejectDifferentAttributeDataTypes() {
        assertThatThrownBy(() -> validator.validate(
                ruleSet(condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0)),
                componentType(10L, 1L, attribute(101L, 10L, DataType.STRING)),
                componentType(20L, 1L, attribute(201L, 20L, DataType.NUMBER))
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Compatibility rule attributes 101 and 201 must have the same data type");
    }

    @Test
    void validate_shouldRejectDirectionalOperatorForNonNumberAttributes() {
        assertThatThrownBy(() -> validator.validate(
                ruleSet(condition(101L, CompatibilityRuleOperator.GT, 201L, 0)),
                componentType(10L, 1L, attribute(101L, 10L, DataType.STRING)),
                componentType(20L, 1L, attribute(201L, 20L, DataType.STRING))
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Operator GT is supported only for NUMBER attributes");
    }

    @Test
    void validate_shouldRejectDuplicateConditions() {
        CompatibilityRuleCondition condition =
                condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0);
        CompatibilityRuleSet ruleSet = CompatibilityRuleSet.builder()
                .domainId(1L)
                .name("Rule")
                .componentTypeAId(10L)
                .componentTypeBId(20L)
                .enabled(true)
                .conditions(List.of(
                        condition,
                        condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 1)
                ))
                .build();

        assertThatThrownBy(() -> validator.validate(
                ruleSet,
                componentType(10L, 1L, attribute(101L, 10L, DataType.STRING)),
                componentType(20L, 1L, attribute(201L, 20L, DataType.STRING))
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Compatibility rule conditions must be unique");
    }

    @Test
    void validate_shouldRejectNegativeOrderIndex() {
        assertThatThrownBy(() -> validator.validate(
                ruleSet(condition(101L, CompatibilityRuleOperator.EQUALS, 201L, -1)),
                componentType(10L, 1L, attribute(101L, 10L, DataType.STRING)),
                componentType(20L, 1L, attribute(201L, 20L, DataType.STRING))
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Compatibility rule condition order index must be non-negative");
    }

    private static CompatibilityRuleSet ruleSet(CompatibilityRuleCondition... conditions) {
        return CompatibilityRuleSet.builder()
                .domainId(1L)
                .name("Rule")
                .componentTypeAId(10L)
                .componentTypeBId(20L)
                .enabled(true)
                .conditions(List.of(conditions))
                .build();
    }

    private static CompatibilityRuleCondition condition(
            Long leftId,
            CompatibilityRuleOperator operator,
            Long rightId,
            Integer orderIndex
    ) {
        return CompatibilityRuleCondition.builder()
                .leftAttributeDefinitionId(leftId)
                .operator(operator)
                .rightAttributeDefinitionId(rightId)
                .orderIndex(orderIndex)
                .build();
    }

    private static ComponentType componentType(
            Long id,
            Long domainId,
            AttributeDefinition... attributes
    ) {
        return ComponentType.builder()
                .id(id)
                .domainId(domainId)
                .attributeDefinitions(List.of(attributes))
                .build();
    }

    private static AttributeDefinition attribute(
            Long id,
            Long componentTypeId,
            DataType dataType
    ) {
        return AttributeDefinition.builder()
                .id(id)
                .componentTypeId(componentTypeId)
                .dataType(dataType)
                .build();
    }
}
