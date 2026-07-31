package ru.sultanyarov.configurator.application.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleConditionInput;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.SaveCompatibilityRuleSetRequest;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleCondition;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleOperator;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompatibilityRuleMapperTest {
    private final CompatibilityRuleMapper mapper = Mappers.getMapper(CompatibilityRuleMapper.class);

    @Test
    void toEntity_shouldMapRequestAndNestedCondition() {
        var input = new CompatibilityRuleConditionInput(
                101L,
                ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleOperator.GTE,
                201L
        ).orderIndex(3);
        var request = new SaveCompatibilityRuleSetRequest(
                "Rule",
                10L,
                20L,
                true,
                List.of(input)
        );

        CompatibilityRuleSet result = mapper.toEntity(1L, request);

        assertThat(result.id()).isNull();
        assertThat(result.domainId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Rule");
        assertThat(result.conditions()).singleElement().satisfies(condition -> {
            assertThat(condition.leftAttributeDefinitionId()).isEqualTo(101L);
            assertThat(condition.operator()).isEqualTo(CompatibilityRuleOperator.GTE);
            assertThat(condition.rightAttributeDefinitionId()).isEqualTo(201L);
            assertThat(condition.orderIndex()).isEqualTo(3);
        });
    }

    @Test
    void toDto_shouldMapAggregateAndGeneratedFields() {
        LocalDateTime createdAt = LocalDateTime.now();
        CompatibilityRuleCondition condition = CompatibilityRuleCondition.builder()
                .id(8L)
                .ruleSetId(7L)
                .leftAttributeDefinitionId(101L)
                .operator(CompatibilityRuleOperator.EQUALS)
                .rightAttributeDefinitionId(201L)
                .orderIndex(0)
                .createdAt(createdAt)
                .build();
        CompatibilityRuleSet ruleSet = CompatibilityRuleSet.builder()
                .id(7L)
                .domainId(1L)
                .name("Rule")
                .componentTypeAId(10L)
                .componentTypeBId(20L)
                .enabled(true)
                .conditions(List.of(condition))
                .createdAt(createdAt)
                .build();

        var result = mapper.toDto(ruleSet);

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getConditions()).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(8L);
            assertThat(dto.getOperator())
                    .isEqualTo(ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleOperator.EQUALS);
            assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
        });
    }
}
