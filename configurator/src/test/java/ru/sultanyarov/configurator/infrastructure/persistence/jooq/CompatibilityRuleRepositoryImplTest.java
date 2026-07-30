package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleCondition;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleOperator;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.DataType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompatibilityRuleRepositoryImplTest extends AbstractJooqRepositoryTest {
    private CompatibilityRuleRepositoryImpl repository;

    @BeforeEach
    void setUpRepository() {
        repository = new CompatibilityRuleRepositoryImpl(dslContext);

        insertDomain(1L, "Domain");
        insertComponentType(10L, 1L, "Board");
        insertComponentType(20L, 1L, "Switch");
        insertAttributeDefinition(101L, 10L, "socket", DataType.STRING);
        insertAttributeDefinition(102L, 10L, "layout", DataType.STRING);
        insertAttributeDefinition(201L, 20L, "socket", DataType.STRING);
        insertAttributeDefinition(202L, 20L, "layout", DataType.STRING);

        insertDomain(2L, "Foreign domain");
        insertComponentType(30L, 2L, "Foreign board");
        insertComponentType(40L, 2L, "Foreign switch");
        insertAttributeDefinition(301L, 30L, "socket", DataType.STRING);
        insertAttributeDefinition(401L, 40L, "socket", DataType.STRING);
    }

    @Test
    void create_shouldPersistAggregateAndReturnGeneratedFieldsWithOrderedConditions() {
        CompatibilityRuleSet created = repository.create(ruleSet(
                1L,
                "Socket and layout",
                10L,
                20L,
                true,
                List.of(
                        condition(102L, CompatibilityRuleOperator.EQUALS, 202L, null),
                        condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 1)
                )
        )).orElseThrow();

        assertThat(created.id()).isNotNull();
        assertThat(created.domainId()).isEqualTo(1L);
        assertThat(created.name()).isEqualTo("Socket and layout");
        assertThat(created.componentTypeAId()).isEqualTo(10L);
        assertThat(created.componentTypeBId()).isEqualTo(20L);
        assertThat(created.enabled()).isTrue();
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.conditions())
                .extracting(CompatibilityRuleCondition::leftAttributeDefinitionId)
                .containsExactly(101L, 102L);
        assertThat(created.conditions()).allSatisfy(condition -> {
            assertThat(condition.id()).isNotNull();
            assertThat(condition.ruleSetId()).isEqualTo(created.id());
            assertThat(condition.createdAt()).isNotNull();
        });
    }

    @Test
    void create_shouldReturnEmptyForDuplicateDomainTypePairAndName() {
        CompatibilityRuleSet ruleSet = ruleSet(
                1L,
                "Socket",
                10L,
                20L,
                true,
                List.of(condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0))
        );

        assertThat(repository.create(ruleSet)).isPresent();
        assertThat(repository.create(ruleSet)).isEmpty();
        assertThat(dslContext.fetchCount(Tables.COMPATIBILITY_RULE_SET)).isOne();
        assertThat(dslContext.fetchCount(Tables.COMPATIBILITY_RULE_CONDITION)).isOne();
    }

    @Test
    void getByIdAndDomainId_shouldHideRuleSetFromAnotherDomainScope() {
        CompatibilityRuleSet created = repository.create(ruleSet(
                2L,
                "Foreign socket",
                30L,
                40L,
                true,
                List.of(condition(301L, CompatibilityRuleOperator.EQUALS, 401L, 0))
        )).orElseThrow();

        assertThat(repository.getByIdAndDomainId(created.id(), 1L)).isEmpty();
        assertThat(repository.getByIdAndDomainId(created.id(), 2L))
                .contains(created);
    }

    @Test
    void getAllByDomainId_shouldReturnOnlyScopedAggregatesInStableIdOrder() {
        CompatibilityRuleSet first = repository.create(ruleSet(
                1L,
                "First",
                10L,
                20L,
                true,
                List.of(condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0))
        )).orElseThrow();
        CompatibilityRuleSet second = repository.create(ruleSet(
                1L,
                "Second",
                10L,
                20L,
                false,
                List.of(condition(102L, CompatibilityRuleOperator.NOT_EQUALS, 202L, 0))
        )).orElseThrow();
        repository.create(ruleSet(
                2L,
                "Foreign",
                30L,
                40L,
                true,
                List.of(condition(301L, CompatibilityRuleOperator.EQUALS, 401L, 0))
        )).orElseThrow();

        assertThat(repository.getAllByDomainId(1L))
                .extracting(CompatibilityRuleSet::id)
                .containsExactly(first.id(), second.id());
    }

    @Test
    void updateByIdAndDomainId_shouldUpdateMetadataAndReplaceConditions() {
        CompatibilityRuleSet created = repository.create(ruleSet(
                1L,
                "Original",
                10L,
                20L,
                true,
                List.of(
                        condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0),
                        condition(102L, CompatibilityRuleOperator.EQUALS, 202L, 1)
                )
        )).orElseThrow();
        List<Long> originalConditionIds = created.conditions().stream()
                .map(CompatibilityRuleCondition::id)
                .toList();

        CompatibilityRuleSet updated = repository.updateByIdAndDomainId(
                created.id(),
                1L,
                ruleSet(
                        1L,
                        "Updated",
                        10L,
                        20L,
                        false,
                        List.of(condition(101L, CompatibilityRuleOperator.NOT_EQUALS, 201L, 5))
                )
        ).orElseThrow();

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.name()).isEqualTo("Updated");
        assertThat(updated.enabled()).isFalse();
        assertThat(updated.createdAt()).isEqualTo(created.createdAt());
        assertThat(updated.conditions()).singleElement().satisfies(condition -> {
            assertThat(condition.id()).isNotIn(originalConditionIds);
            assertThat(condition.ruleSetId()).isEqualTo(created.id());
            assertThat(condition.operator()).isEqualTo(CompatibilityRuleOperator.NOT_EQUALS);
            assertThat(condition.orderIndex()).isEqualTo(5);
        });
        assertThat(dslContext.fetchCount(
                Tables.COMPATIBILITY_RULE_CONDITION,
                Tables.COMPATIBILITY_RULE_CONDITION.RULE_SET_ID.eq(created.id())
        )).isOne();
    }

    @Test
    void updateByIdAndDomainId_shouldNotModifyRuleSetOutsideDomainScope() {
        CompatibilityRuleSet created = repository.create(ruleSet(
                1L,
                "Original",
                10L,
                20L,
                true,
                List.of(condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0))
        )).orElseThrow();

        assertThat(repository.updateByIdAndDomainId(
                created.id(),
                2L,
                ruleSet(2L, "Foreign update", 30L, 40L, false, List.of())
        )).isEmpty();
        assertThat(repository.getByIdAndDomainId(created.id(), 1L))
                .contains(created);
    }

    @Test
    void updateByIdAndDomainId_shouldTranslateDuplicateBusinessKeyToConflict() {
        repository.create(ruleSet(
                1L,
                "First",
                10L,
                20L,
                true,
                List.of(condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0))
        )).orElseThrow();
        CompatibilityRuleSet second = repository.create(ruleSet(
                1L,
                "Second",
                10L,
                20L,
                true,
                List.of(condition(102L, CompatibilityRuleOperator.EQUALS, 202L, 0))
        )).orElseThrow();

        assertThatThrownBy(() -> repository.updateByIdAndDomainId(
                second.id(),
                1L,
                ruleSet(
                        1L,
                        "First",
                        10L,
                        20L,
                        true,
                        List.of(condition(102L, CompatibilityRuleOperator.EQUALS, 202L, 0))
                )
        ))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage(
                        "Compatibility rule set 'First' already exists for component types 10 and 20 in domain 1"
                );
    }

    @Test
    void deleteByIdAndDomainId_shouldDeleteScopedAggregateAndCascadeConditions() {
        CompatibilityRuleSet created = repository.create(ruleSet(
                1L,
                "Socket",
                10L,
                20L,
                true,
                List.of(condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0))
        )).orElseThrow();

        assertThat(repository.deleteByIdAndDomainId(created.id(), 2L)).isFalse();
        assertThat(repository.deleteByIdAndDomainId(created.id(), 1L)).isTrue();
        assertThat(dslContext.fetchCount(Tables.COMPATIBILITY_RULE_SET)).isZero();
        assertThat(dslContext.fetchCount(Tables.COMPATIBILITY_RULE_CONDITION)).isZero();
    }

    @Test
    void hasByComponentTypeId_shouldCheckBothRuleSetSides() {
        repository.create(ruleSet(
                1L,
                "Socket",
                10L,
                20L,
                true,
                List.of(condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0))
        )).orElseThrow();

        assertThat(repository.hasByComponentTypeId(10L)).isTrue();
        assertThat(repository.hasByComponentTypeId(20L)).isTrue();
        assertThat(repository.hasByComponentTypeId(30L)).isFalse();
    }

    @Test
    void existsByBusinessKey_shouldSupportCreateAndUpdateChecks() {
        CompatibilityRuleSet created = repository.create(ruleSet(
                1L,
                "Socket",
                10L,
                20L,
                true,
                List.of(condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0))
        )).orElseThrow();

        assertThat(repository.existsByBusinessKey(1L, 10L, 20L, "Socket", null)).isTrue();
        assertThat(repository.existsByBusinessKey(1L, 10L, 20L, "Socket", created.id())).isFalse();
        assertThat(repository.existsByBusinessKey(1L, 10L, 20L, "Other", null)).isFalse();
    }

    @Test
    void schema_shouldRejectNonNormalizedComponentTypePair() {
        assertThatThrownBy(() -> repository.create(ruleSet(
                1L,
                "Reversed",
                20L,
                10L,
                true,
                List.of(condition(201L, CompatibilityRuleOperator.EQUALS, 101L, 0))
        )))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void schema_shouldRejectDuplicateConditions() {
        CompatibilityRuleCondition condition =
                condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0);

        assertThatThrownBy(() -> repository.create(ruleSet(
                1L,
                "Duplicate conditions",
                10L,
                20L,
                true,
                List.of(condition, condition)
        )))
                .isInstanceOf(DataAccessException.class);
    }

    private static CompatibilityRuleSet ruleSet(
            Long domainId,
            String name,
            Long componentTypeAId,
            Long componentTypeBId,
            boolean enabled,
            List<CompatibilityRuleCondition> conditions
    ) {
        return CompatibilityRuleSet.builder()
                .domainId(domainId)
                .name(name)
                .componentTypeAId(componentTypeAId)
                .componentTypeBId(componentTypeBId)
                .enabled(enabled)
                .conditions(conditions)
                .build();
    }

    private static CompatibilityRuleCondition condition(
            Long leftAttributeDefinitionId,
            CompatibilityRuleOperator operator,
            Long rightAttributeDefinitionId,
            Integer orderIndex
    ) {
        return CompatibilityRuleCondition.builder()
                .leftAttributeDefinitionId(leftAttributeDefinitionId)
                .operator(operator)
                .rightAttributeDefinitionId(rightAttributeDefinitionId)
                .orderIndex(orderIndex)
                .build();
    }

    private void insertDomain(Long id, String name) {
        dslContext.insertInto(Tables.DOMAIN)
                .set(Tables.DOMAIN.ID, id)
                .set(Tables.DOMAIN.NAME, name)
                .set(Tables.DOMAIN.CREATED_BY_USER_ID, 1L)
                .execute();
    }

    private void insertComponentType(Long id, Long domainId, String name) {
        dslContext.insertInto(Tables.COMPONENT_TYPE)
                .set(Tables.COMPONENT_TYPE.ID, id)
                .set(Tables.COMPONENT_TYPE.DOMAIN_ID, domainId)
                .set(Tables.COMPONENT_TYPE.NAME, name)
                .execute();
    }

    private void insertAttributeDefinition(
            Long id,
            Long componentTypeId,
            String name,
            DataType dataType
    ) {
        dslContext.insertInto(Tables.ATTRIBUTE_DEFINITION)
                .set(Tables.ATTRIBUTE_DEFINITION.ID, id)
                .set(Tables.ATTRIBUTE_DEFINITION.COMPONENT_TYPE_ID, componentTypeId)
                .set(Tables.ATTRIBUTE_DEFINITION.NAME, name)
                .set(Tables.ATTRIBUTE_DEFINITION.LABEL, name)
                .set(Tables.ATTRIBUTE_DEFINITION.DATA_TYPE, dataType.name())
                .execute();
    }
}
