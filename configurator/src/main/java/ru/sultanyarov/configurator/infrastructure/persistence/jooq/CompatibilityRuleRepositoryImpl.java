package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectField;
import org.jooq.exception.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.sultanyarov.configurator.application.port.out.CompatibilityRuleRepository;
import ru.sultanyarov.configurator.common.util.JooqMapperUtils;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleCondition;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleOperator;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;

import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.multiset;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPATIBILITY_RULE_CONDITION;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPATIBILITY_RULE_SET;

@Repository
@RequiredArgsConstructor
public class CompatibilityRuleRepositoryImpl implements CompatibilityRuleRepository {
    private final DSLContext dslContext;

    @Override
    @Transactional
    public Optional<CompatibilityRuleSet> create(CompatibilityRuleSet ruleSet) {
        Long ruleSetId = dslContext.insertInto(COMPATIBILITY_RULE_SET)
                .set(COMPATIBILITY_RULE_SET.DOMAIN_ID, ruleSet.domainId())
                .set(COMPATIBILITY_RULE_SET.NAME, ruleSet.name())
                .set(COMPATIBILITY_RULE_SET.COMPONENT_TYPE_A_ID, ruleSet.componentTypeAId())
                .set(COMPATIBILITY_RULE_SET.COMPONENT_TYPE_B_ID, ruleSet.componentTypeBId())
                .set(COMPATIBILITY_RULE_SET.ENABLED, ruleSet.enabled())
                .onConflict(
                        COMPATIBILITY_RULE_SET.DOMAIN_ID,
                        COMPATIBILITY_RULE_SET.COMPONENT_TYPE_A_ID,
                        COMPATIBILITY_RULE_SET.COMPONENT_TYPE_B_ID,
                        COMPATIBILITY_RULE_SET.NAME
                )
                .doNothing()
                .returning(COMPATIBILITY_RULE_SET.ID)
                .fetchOne(COMPATIBILITY_RULE_SET.ID);

        if (ruleSetId == null) {
            return Optional.empty();
        }

        insertConditions(ruleSetId, ruleSet.conditions());
        return getByIdAndDomainId(ruleSetId, ruleSet.domainId());
    }

    @Override
    public Optional<CompatibilityRuleSet> getByIdAndDomainId(Long ruleSetId, Long domainId) {
        return dslContext.select(
                        COMPATIBILITY_RULE_SET.ID,
                        COMPATIBILITY_RULE_SET.DOMAIN_ID,
                        COMPATIBILITY_RULE_SET.NAME,
                        COMPATIBILITY_RULE_SET.COMPONENT_TYPE_A_ID,
                        COMPATIBILITY_RULE_SET.COMPONENT_TYPE_B_ID,
                        COMPATIBILITY_RULE_SET.ENABLED,
                        COMPATIBILITY_RULE_SET.CREATED_AT,
                        conditionsField()
                )
                .from(COMPATIBILITY_RULE_SET)
                .where(COMPATIBILITY_RULE_SET.ID.eq(ruleSetId))
                .and(COMPATIBILITY_RULE_SET.DOMAIN_ID.eq(domainId))
                .fetchOptional(this::mapRuleSet);
    }

    @Override
    public List<CompatibilityRuleSet> getAllByDomainId(Long domainId) {
        return dslContext.select(
                        COMPATIBILITY_RULE_SET.ID,
                        COMPATIBILITY_RULE_SET.DOMAIN_ID,
                        COMPATIBILITY_RULE_SET.NAME,
                        COMPATIBILITY_RULE_SET.COMPONENT_TYPE_A_ID,
                        COMPATIBILITY_RULE_SET.COMPONENT_TYPE_B_ID,
                        COMPATIBILITY_RULE_SET.ENABLED,
                        COMPATIBILITY_RULE_SET.CREATED_AT,
                        conditionsField()
                )
                .from(COMPATIBILITY_RULE_SET)
                .where(COMPATIBILITY_RULE_SET.DOMAIN_ID.eq(domainId))
                .orderBy(COMPATIBILITY_RULE_SET.ID.asc())
                .fetch(this::mapRuleSet);
    }

    @Override
    @Transactional
    public Optional<CompatibilityRuleSet> updateByIdAndDomainId(
            Long ruleSetId,
            Long domainId,
            CompatibilityRuleSet ruleSet
    ) {
        Long updatedRuleSetId;
        try {
            updatedRuleSetId = dslContext.update(COMPATIBILITY_RULE_SET)
                    .set(COMPATIBILITY_RULE_SET.NAME, ruleSet.name())
                    .set(COMPATIBILITY_RULE_SET.COMPONENT_TYPE_A_ID, ruleSet.componentTypeAId())
                    .set(COMPATIBILITY_RULE_SET.COMPONENT_TYPE_B_ID, ruleSet.componentTypeBId())
                    .set(COMPATIBILITY_RULE_SET.ENABLED, ruleSet.enabled())
                    .where(COMPATIBILITY_RULE_SET.ID.eq(ruleSetId))
                    .and(COMPATIBILITY_RULE_SET.DOMAIN_ID.eq(domainId))
                    .returning(COMPATIBILITY_RULE_SET.ID)
                    .fetchOne(COMPATIBILITY_RULE_SET.ID);
        } catch (DataAccessException exception) {
            if ("23505".equals(exception.sqlState())) {
                throw duplicateException(ruleSet);
            }
            throw exception;
        }

        if (updatedRuleSetId == null) {
            return Optional.empty();
        }

        dslContext.deleteFrom(COMPATIBILITY_RULE_CONDITION)
                .where(COMPATIBILITY_RULE_CONDITION.RULE_SET_ID.eq(ruleSetId))
                .execute();
        insertConditions(ruleSetId, ruleSet.conditions());
        return getByIdAndDomainId(ruleSetId, domainId);
    }

    @Override
    public boolean deleteByIdAndDomainId(Long ruleSetId, Long domainId) {
        return dslContext.deleteFrom(COMPATIBILITY_RULE_SET)
                .where(COMPATIBILITY_RULE_SET.ID.eq(ruleSetId))
                .and(COMPATIBILITY_RULE_SET.DOMAIN_ID.eq(domainId))
                .execute() > 0;
    }

    @Override
    public boolean existsByBusinessKey(
            Long domainId,
            Long componentTypeAId,
            Long componentTypeBId,
            String name,
            Long excludedRuleSetId
    ) {
        var condition = COMPATIBILITY_RULE_SET.DOMAIN_ID.eq(domainId)
                .and(COMPATIBILITY_RULE_SET.COMPONENT_TYPE_A_ID.eq(componentTypeAId))
                .and(COMPATIBILITY_RULE_SET.COMPONENT_TYPE_B_ID.eq(componentTypeBId))
                .and(COMPATIBILITY_RULE_SET.NAME.eq(name));
        if (excludedRuleSetId != null) {
            condition = condition.and(COMPATIBILITY_RULE_SET.ID.ne(excludedRuleSetId));
        }
        return dslContext.fetchExists(
                dslContext.selectFrom(COMPATIBILITY_RULE_SET).where(condition)
        );
    }

    @Override
    public boolean hasByComponentTypeId(Long componentTypeId) {
        return dslContext.fetchExists(
                dslContext.selectFrom(COMPATIBILITY_RULE_SET)
                        .where(COMPATIBILITY_RULE_SET.COMPONENT_TYPE_A_ID.eq(componentTypeId))
                        .or(COMPATIBILITY_RULE_SET.COMPONENT_TYPE_B_ID.eq(componentTypeId))
        );
    }

    private SelectField<List<CompatibilityRuleCondition>> conditionsField() {
        return multiset(
                dslContext.select(
                                COMPATIBILITY_RULE_CONDITION.ID,
                                COMPATIBILITY_RULE_CONDITION.RULE_SET_ID,
                                COMPATIBILITY_RULE_CONDITION.LEFT_ATTRIBUTE_DEFINITION_ID,
                                COMPATIBILITY_RULE_CONDITION.OPERATOR,
                                COMPATIBILITY_RULE_CONDITION.RIGHT_ATTRIBUTE_DEFINITION_ID,
                                COMPATIBILITY_RULE_CONDITION.ORDER_INDEX,
                                COMPATIBILITY_RULE_CONDITION.CREATED_AT
                        )
                        .from(COMPATIBILITY_RULE_CONDITION)
                        .where(COMPATIBILITY_RULE_CONDITION.RULE_SET_ID.eq(COMPATIBILITY_RULE_SET.ID))
                        .orderBy(
                                COMPATIBILITY_RULE_CONDITION.ORDER_INDEX.asc().nullsLast(),
                                COMPATIBILITY_RULE_CONDITION.ID.asc()
                        )
        )
                .convertFrom(result -> result.map(this::mapCondition))
                .as("conditions");
    }

    private void insertConditions(Long ruleSetId, List<CompatibilityRuleCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return;
        }

        var insertQueries = conditions.stream()
                .map(condition -> dslContext.insertInto(COMPATIBILITY_RULE_CONDITION)
                        .set(COMPATIBILITY_RULE_CONDITION.RULE_SET_ID, ruleSetId)
                        .set(
                                COMPATIBILITY_RULE_CONDITION.LEFT_ATTRIBUTE_DEFINITION_ID,
                                condition.leftAttributeDefinitionId()
                        )
                        .set(COMPATIBILITY_RULE_CONDITION.OPERATOR, condition.operator().name())
                        .set(
                                COMPATIBILITY_RULE_CONDITION.RIGHT_ATTRIBUTE_DEFINITION_ID,
                                condition.rightAttributeDefinitionId()
                        )
                        .set(COMPATIBILITY_RULE_CONDITION.ORDER_INDEX, condition.orderIndex()))
                .toList();
        dslContext.batch(insertQueries).execute();
    }

    private CompatibilityRuleSet mapRuleSet(Record record) {
        List<CompatibilityRuleCondition> conditions = JooqMapperUtils.getListOrNull(record, "conditions");
        return CompatibilityRuleSet.builder()
                .id(record.get(COMPATIBILITY_RULE_SET.ID))
                .domainId(record.get(COMPATIBILITY_RULE_SET.DOMAIN_ID))
                .name(record.get(COMPATIBILITY_RULE_SET.NAME))
                .componentTypeAId(record.get(COMPATIBILITY_RULE_SET.COMPONENT_TYPE_A_ID))
                .componentTypeBId(record.get(COMPATIBILITY_RULE_SET.COMPONENT_TYPE_B_ID))
                .enabled(record.get(COMPATIBILITY_RULE_SET.ENABLED))
                .conditions(conditions == null ? List.of() : conditions)
                .createdAt(record.get(COMPATIBILITY_RULE_SET.CREATED_AT))
                .build();
    }

    private CompatibilityRuleCondition mapCondition(Record record) {
        return CompatibilityRuleCondition.builder()
                .id(record.get(COMPATIBILITY_RULE_CONDITION.ID))
                .ruleSetId(record.get(COMPATIBILITY_RULE_CONDITION.RULE_SET_ID))
                .leftAttributeDefinitionId(
                        record.get(COMPATIBILITY_RULE_CONDITION.LEFT_ATTRIBUTE_DEFINITION_ID)
                )
                .operator(CompatibilityRuleOperator.valueOf(
                        record.get(COMPATIBILITY_RULE_CONDITION.OPERATOR)
                ))
                .rightAttributeDefinitionId(
                        record.get(COMPATIBILITY_RULE_CONDITION.RIGHT_ATTRIBUTE_DEFINITION_ID)
                )
                .orderIndex(record.get(COMPATIBILITY_RULE_CONDITION.ORDER_INDEX))
                .createdAt(record.get(COMPATIBILITY_RULE_CONDITION.CREATED_AT))
                .build();
    }

    private static EntityAlreadyExistsException duplicateException(CompatibilityRuleSet ruleSet) {
        return new EntityAlreadyExistsException(
                "Compatibility rule set '{}' already exists for component types {} and {} in domain {}",
                ruleSet.name(),
                ruleSet.componentTypeAId(),
                ruleSet.componentTypeBId(),
                ruleSet.domainId()
        );
    }
}
