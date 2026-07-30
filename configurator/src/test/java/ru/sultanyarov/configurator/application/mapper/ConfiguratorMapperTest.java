package ru.sultanyarov.configurator.application.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.sultanyarov.configurator.domain.model.CompatibilityConditionExplanation;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanation;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanationSource;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleOperator;
import ru.sultanyarov.configurator.domain.model.CompatibleComponent;
import ru.sultanyarov.configurator.domain.model.CompatibleComponentGroup;
import ru.sultanyarov.configurator.domain.model.ConfiguratorResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfiguratorMapperTest {
    private final ConfiguratorMapper mapper = Mappers.getMapper(ConfiguratorMapper.class);

    @Test
    void toDto_shouldMapNestedCompatibleComponents() {
        ConfiguratorResult result = new ConfiguratorResult(
                1L,
                List.of(new CompatibleComponentGroup(
                        20L,
                        "Motherboard",
                        List.of(new CompatibleComponent(
                                2L,
                                "Board",
                                "Brand",
                                20L,
                                List.of(
                                        CompatibilityExplanation.builder()
                                                .source(CompatibilityExplanationSource.MANUAL)
                                                .linkId(11L)
                                                .comment("Manual")
                                                .build(),
                                        CompatibilityExplanation.builder()
                                                .source(CompatibilityExplanationSource.AUTOMATIC)
                                                .ruleSetId(7L)
                                                .ruleSetName("Socket")
                                                .conditions(List.of(conditionExplanation()))
                                                .build()
                                )
                        ))
                ))
        );

        var dto = mapper.toDto(result);

        assertThat(dto.getBaseComponentId()).isEqualTo(1L);
        assertThat(dto.getCompatibleByType()).singleElement().satisfies(group -> {
            assertThat(group.getComponentTypeId()).isEqualTo(20L);
            assertThat(group.getComponentTypeName()).isEqualTo("Motherboard");
            assertThat(group.getComponents()).singleElement().satisfies(component -> {
                assertThat(component.getId()).isEqualTo(2L);
                assertThat(component.getName()).isEqualTo("Board");
                assertThat(component.getBrand()).isEqualTo("Brand");
                assertThat(component.getComponentTypeId()).isEqualTo(20L);
                assertThat(component.getExplanations()).hasSize(2);
                assertThat(component.getExplanations().getFirst()).satisfies(explanation -> {
                    assertThat(explanation.getSource().getValue()).isEqualTo("MANUAL");
                    assertThat(explanation.getLinkId()).isEqualTo(11L);
                    assertThat(explanation.getComment()).isEqualTo("Manual");
                });
                assertThat(component.getExplanations().get(1)).satisfies(explanation -> {
                    assertThat(explanation.getSource().getValue()).isEqualTo("AUTOMATIC");
                    assertThat(explanation.getRuleSetId()).isEqualTo(7L);
                    assertThat(explanation.getRuleSetName()).isEqualTo("Socket");
                    assertThat(explanation.getConditions()).singleElement()
                            .satisfies(condition -> {
                                assertThat(condition.getLeftAttributeName()).isEqualTo("socket");
                                assertThat(condition.getLeftValue()).isEqualTo("AM5");
                                assertThat(condition.getOperator().getValue()).isEqualTo("EQUALS");
                                assertThat(condition.getRightAttributeName()).isEqualTo("socket");
                                assertThat(condition.getRightValue()).isEqualTo("AM5");
                            });
                });
            });
        });
    }

    private static CompatibilityConditionExplanation conditionExplanation() {
        return CompatibilityConditionExplanation.builder()
                .leftAttributeDefinitionId(101L)
                .leftAttributeName("socket")
                .leftValue("AM5")
                .operator(CompatibilityRuleOperator.EQUALS)
                .rightAttributeDefinitionId(201L)
                .rightAttributeName("socket")
                .rightValue("AM5")
                .build();
    }
}
