package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.SelectField;
import org.jooq.SelectFieldOrAsterisk;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.application.port.out.ConfiguratorRepository;
import ru.sultanyarov.configurator.common.util.JooqMapperUtils;
import ru.sultanyarov.configurator.domain.model.AttributeValue;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.DataType;

import java.util.ArrayList;
import java.util.List;

import static org.jooq.impl.DSL.multiset;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.ATTRIBUTE_DEFINITION;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.ATTRIBUTE_VALUE;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPATIBILITY_LINK;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPONENT;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPONENT_TYPE;

@Repository
@RequiredArgsConstructor
public class ConfiguratorRepositoryImpl implements ConfiguratorRepository {
    private final DSLContext dslContext;

    @Override
    public List<Component> getActiveCandidates(Long domainId, Long baseComponentId) {
        List<SelectFieldOrAsterisk> fields = new ArrayList<>(List.of(
                COMPONENT.ID,
                COMPONENT.COMPONENT_TYPE_ID,
                COMPONENT.NAME,
                COMPONENT.BRAND,
                COMPONENT.DESCRIPTION,
                COMPONENT.ARCHIVED,
                COMPONENT.CREATED_AT
        ));
        fields.add(attributeValuesField());

        return dslContext.select(fields)
                .from(COMPONENT)
                .join(COMPONENT_TYPE)
                .on(COMPONENT_TYPE.ID.eq(COMPONENT.COMPONENT_TYPE_ID))
                .where(COMPONENT_TYPE.DOMAIN_ID.eq(domainId))
                .and(COMPONENT.ARCHIVED.isFalse())
                .and(COMPONENT.ID.ne(baseComponentId))
                .orderBy(
                        COMPONENT_TYPE.ORDER_INDEX.asc().nullsLast(),
                        COMPONENT_TYPE.ID.asc(),
                        COMPONENT.ID.asc()
                )
                .fetch(componentMapper());
    }

    @Override
    public List<Component> getActiveComponents(Long domainId) {
        List<SelectFieldOrAsterisk> fields = new ArrayList<>(List.of(
                COMPONENT.ID,
                COMPONENT.COMPONENT_TYPE_ID,
                COMPONENT.NAME,
                COMPONENT.BRAND,
                COMPONENT.DESCRIPTION,
                COMPONENT.ARCHIVED,
                COMPONENT.CREATED_AT
        ));
        fields.add(attributeValuesField());

        return dslContext.select(fields)
                .from(COMPONENT)
                .join(COMPONENT_TYPE)
                .on(COMPONENT_TYPE.ID.eq(COMPONENT.COMPONENT_TYPE_ID))
                .where(COMPONENT_TYPE.DOMAIN_ID.eq(domainId))
                .and(COMPONENT.ARCHIVED.isFalse())
                .orderBy(
                        COMPONENT_TYPE.ORDER_INDEX.asc().nullsLast(),
                        COMPONENT_TYPE.ID.asc(),
                        COMPONENT.ID.asc()
                )
                .fetch(componentMapper());
    }

    @Override
    public List<CompatibilityLink> getManualCompatibilityLinks(
            Long domainId,
            Long baseComponentId
    ) {
        return dslContext.select(
                        COMPATIBILITY_LINK.ID,
                        COMPATIBILITY_LINK.DOMAIN_ID,
                        COMPATIBILITY_LINK.COMPONENT_A_ID,
                        COMPATIBILITY_LINK.COMPONENT_B_ID,
                        COMPATIBILITY_LINK.COMMENT
                )
                .from(COMPATIBILITY_LINK)
                .where(COMPATIBILITY_LINK.DOMAIN_ID.eq(domainId))
                .and(
                        COMPATIBILITY_LINK.COMPONENT_A_ID.eq(baseComponentId)
                                .or(COMPATIBILITY_LINK.COMPONENT_B_ID.eq(baseComponentId))
                )
                .orderBy(COMPATIBILITY_LINK.ID.asc())
                .fetch(record -> CompatibilityLink.builder()
                        .id(record.get(COMPATIBILITY_LINK.ID))
                        .domainId(record.get(COMPATIBILITY_LINK.DOMAIN_ID))
                        .componentAId(record.get(COMPATIBILITY_LINK.COMPONENT_A_ID))
                        .componentBId(record.get(COMPATIBILITY_LINK.COMPONENT_B_ID))
                        .comment(record.get(COMPATIBILITY_LINK.COMMENT))
                        .build());
    }

    @Override
    public List<CompatibilityLink> getAllManualCompatibilityLinks(Long domainId) {
        return dslContext.select(
                        COMPATIBILITY_LINK.ID,
                        COMPATIBILITY_LINK.DOMAIN_ID,
                        COMPATIBILITY_LINK.COMPONENT_A_ID,
                        COMPATIBILITY_LINK.COMPONENT_B_ID,
                        COMPATIBILITY_LINK.COMMENT
                )
                .from(COMPATIBILITY_LINK)
                .where(COMPATIBILITY_LINK.DOMAIN_ID.eq(domainId))
                .orderBy(COMPATIBILITY_LINK.ID.asc())
                .fetch(record -> CompatibilityLink.builder()
                        .id(record.get(COMPATIBILITY_LINK.ID))
                        .domainId(record.get(COMPATIBILITY_LINK.DOMAIN_ID))
                        .componentAId(record.get(COMPATIBILITY_LINK.COMPONENT_A_ID))
                        .componentBId(record.get(COMPATIBILITY_LINK.COMPONENT_B_ID))
                        .comment(record.get(COMPATIBILITY_LINK.COMMENT))
                        .build());
    }

    private SelectField<List<AttributeValue>> attributeValuesField() {
        return multiset(
                dslContext.select(
                                ATTRIBUTE_VALUE.ID,
                                ATTRIBUTE_VALUE.ATTRIBUTE_DEFINITION_ID,
                                ATTRIBUTE_DEFINITION.NAME,
                                ATTRIBUTE_DEFINITION.LABEL,
                                ATTRIBUTE_DEFINITION.DATA_TYPE,
                                ATTRIBUTE_VALUE.VALUE_STRING,
                                ATTRIBUTE_VALUE.VALUE_NUMBER,
                                ATTRIBUTE_VALUE.VALUE_BOOLEAN
                        )
                        .from(ATTRIBUTE_VALUE)
                        .join(ATTRIBUTE_DEFINITION)
                        .on(ATTRIBUTE_DEFINITION.ID.eq(ATTRIBUTE_VALUE.ATTRIBUTE_DEFINITION_ID))
                        .where(ATTRIBUTE_VALUE.COMPONENT_ID.eq(COMPONENT.ID))
                        .orderBy(ATTRIBUTE_DEFINITION.ORDER_INDEX, ATTRIBUTE_VALUE.ID)
        )
                .convertFrom(result -> result.map(this::mapAttributeValue))
                .as("attributes");
    }

    private RecordMapper<Record, Component> componentMapper() {
        return record -> Component.builder()
                .id(record.get(COMPONENT.ID))
                .componentTypeId(record.get(COMPONENT.COMPONENT_TYPE_ID))
                .name(record.get(COMPONENT.NAME))
                .brand(record.get(COMPONENT.BRAND))
                .description(record.get(COMPONENT.DESCRIPTION))
                .archived(record.get(COMPONENT.ARCHIVED))
                .attributes(JooqMapperUtils.getListOrNull(record, "attributes"))
                .createdAt(record.get(COMPONENT.CREATED_AT))
                .build();
    }

    private AttributeValue mapAttributeValue(Record record) {
        Object rawValue = ObjectUtils.firstNonNull(
                record.get(ATTRIBUTE_VALUE.VALUE_STRING),
                record.get(ATTRIBUTE_VALUE.VALUE_NUMBER),
                record.get(ATTRIBUTE_VALUE.VALUE_BOOLEAN)
        );
        return AttributeValue.builder()
                .id(record.get(ATTRIBUTE_VALUE.ID))
                .attributeDefinitionId(record.get(ATTRIBUTE_VALUE.ATTRIBUTE_DEFINITION_ID))
                .name(record.get(ATTRIBUTE_DEFINITION.NAME))
                .label(record.get(ATTRIBUTE_DEFINITION.LABEL))
                .dataType(DataType.valueOf(record.get(ATTRIBUTE_DEFINITION.DATA_TYPE)))
                .value(rawValue == null ? null : String.valueOf(rawValue))
                .build();
    }
}
