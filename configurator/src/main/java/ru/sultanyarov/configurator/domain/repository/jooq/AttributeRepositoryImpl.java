package ru.sultanyarov.configurator.domain.repository.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.RecordMapper;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.application.util.StringToListConverter;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.entity.jooq.tables.records.AttributeDefinitionRecord;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.DataType;
import ru.sultanyarov.configurator.domain.repository.AttributeRepository;

import java.util.List;
import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class AttributeRepositoryImpl implements AttributeRepository {
    private final DSLContext dslContext;
    private final StringToListConverter enumValuesConverter = new StringToListConverter();
    private static final ru.sultanyarov.configurator.domain.entity.jooq.tables.AttributeDefinition ad = Tables.ATTRIBUTE_DEFINITION;

    @Override
    public boolean hasByComponentTypeId(Long id) {
        return dslContext.fetchExists(
                dslContext.selectFrom(Tables.ATTRIBUTE_DEFINITION)
                        .where(Tables.ATTRIBUTE_DEFINITION.COMPONENT_TYPE_ID.eq(id))
        );
    }

    @Override
    public boolean hasByComponentTypeIdAndName(Long id, String name) {
        return dslContext.fetchExists(
                dslContext.selectFrom(Tables.ATTRIBUTE_DEFINITION)
                        .where(Tables.ATTRIBUTE_DEFINITION.COMPONENT_TYPE_ID.eq(id))
                        .and(Tables.ATTRIBUTE_DEFINITION.NAME.eq(name))
        );
    }

    @Override
    public Optional<AttributeDefinition> createAttributeDefinition(AttributeDefinition attributeDefinition) {
        return dslContext.insertInto(ad)
                .set(ad.COMPONENT_TYPE_ID, attributeDefinition.componentTypeId())
                .set(ad.NAME, attributeDefinition.name())
                .set(ad.LABEL, attributeDefinition.label())
                .set(ad.DATA_TYPE, attributeDefinition.dataType().name())
                .set(ad.ENUM_VALUES_JSON, enumValuesConverter.to(attributeDefinition.enumValues()))
                .set(ad.IS_REQUIRED, attributeDefinition.isRequired())
                .set(ad.ORDER_INDEX, attributeDefinition.orderIndex())
                .returning()
                .fetchOptional(getAttributeDefinitionRecordMapper());
    }

    @Override
    public Optional<AttributeDefinition> updateAttribute(Long id, AttributeDefinition attributeDefinition) {
        return dslContext.update(Tables.ATTRIBUTE_DEFINITION)
                .set(Tables.ATTRIBUTE_DEFINITION.COMPONENT_TYPE_ID, attributeDefinition.componentTypeId())
                .set(Tables.ATTRIBUTE_DEFINITION.NAME, attributeDefinition.name())
                .set(Tables.ATTRIBUTE_DEFINITION.LABEL, attributeDefinition.label())
                .set(Tables.ATTRIBUTE_DEFINITION.DATA_TYPE, attributeDefinition.dataType().name())
                .set(Tables.ATTRIBUTE_DEFINITION.ENUM_VALUES_JSON, new StringToListConverter().to(attributeDefinition.enumValues()))
                .set(Tables.ATTRIBUTE_DEFINITION.IS_REQUIRED, attributeDefinition.isRequired())
                .set(Tables.ATTRIBUTE_DEFINITION.ORDER_INDEX, attributeDefinition.orderIndex())
                .set(Tables.ATTRIBUTE_DEFINITION.CREATED_AT, attributeDefinition.createdAt())
                .where(Tables.ATTRIBUTE_DEFINITION.ID.eq(id))
                .returning()
                .fetchOptional(getAttributeDefinitionRecordMapper());
    }

    @Override
    public void deleteById(Long id) {
        dslContext.delete(Tables.ATTRIBUTE_DEFINITION)
                .where(Tables.ATTRIBUTE_DEFINITION.ID.eq(id))
                .execute();
    }

    @Override
    public boolean existsById(Long id) {
        return dslContext.fetchExists(
                dslContext.selectFrom(Tables.ATTRIBUTE_DEFINITION)
                        .where(Tables.ATTRIBUTE_DEFINITION.ID.eq(id))
        );
    }

    @Override
    public List<AttributeDefinition> getByComponentTypeId(Long componentTypeId) {
        return dslContext.selectFrom(Tables.ATTRIBUTE_DEFINITION)
                .where(Tables.ATTRIBUTE_DEFINITION.COMPONENT_TYPE_ID.eq(componentTypeId))
                .fetch(getAttributeDefinitionRecordMapper());
    }

    @Override
    public Optional<AttributeDefinition> getById(Long id) {
        return dslContext.selectFrom(Tables.ATTRIBUTE_DEFINITION)
                .where(Tables.ATTRIBUTE_DEFINITION.ID.eq(id))
                .fetchOptional(getAttributeDefinitionRecordMapper());
    }

    private RecordMapper<AttributeDefinitionRecord, AttributeDefinition> getAttributeDefinitionRecordMapper() {
        return r -> AttributeDefinition.builder()
                .id(r.get(ad.ID))
                .componentTypeId(r.get(ad.COMPONENT_TYPE_ID))
                .name(r.get(ad.NAME))
                .label(r.get(ad.LABEL))
                .dataType(DataType.valueOf(r.get(ad.DATA_TYPE)))
                .enumValues(enumValuesConverter.from(r.get(ad.ENUM_VALUES_JSON)))
                .isRequired(r.get(ad.IS_REQUIRED))
                .orderIndex(r.get(ad.ORDER_INDEX))
                .createdAt(r.get(ad.CREATED_AT))
                .build();
    }
}
