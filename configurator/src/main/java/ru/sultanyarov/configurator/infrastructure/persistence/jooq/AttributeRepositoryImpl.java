package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.RecordMapper;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.application.port.out.AttributeRepository;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.entity.jooq.tables.records.AttributeDefinitionRecord;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.DataType;
import ru.sultanyarov.configurator.infrastructure.persistence.jooq.config.StringToListConverter;

@Repository
@RequiredArgsConstructor
public class AttributeRepositoryImpl implements AttributeRepository {
  private static final ru.sultanyarov.configurator.domain.entity.jooq.tables.AttributeDefinition
      AD = Tables.ATTRIBUTE_DEFINITION;

  private final DSLContext dslContext;
  private final StringToListConverter enumValuesConverter = new StringToListConverter();

  @Override
  public boolean hasByComponentTypeId(Long componentTypeId) {
    return dslContext.fetchExists(
        dslContext.selectFrom(AD).where(AD.COMPONENT_TYPE_ID.eq(componentTypeId)));
  }

  @Override
  public boolean hasByComponentTypeIdAndName(Long id, String name) {
    return dslContext.fetchExists(
        dslContext.selectFrom(AD).where(AD.COMPONENT_TYPE_ID.eq(id)).and(AD.NAME.eq(name)));
  }

  @Override
  public Optional<AttributeDefinition> createAttributeDefinition(
      AttributeDefinition attributeDefinition) {
    return dslContext
        .insertInto(AD)
        .set(AD.COMPONENT_TYPE_ID, attributeDefinition.componentTypeId())
        .set(AD.NAME, attributeDefinition.name())
        .set(AD.LABEL, attributeDefinition.label())
        .set(AD.DATA_TYPE, attributeDefinition.dataType().name())
        .set(AD.ENUM_VALUES_JSON, enumValuesConverter.to(attributeDefinition.enumValues()))
        .set(AD.IS_REQUIRED, attributeDefinition.isRequired())
        .set(AD.ORDER_INDEX, attributeDefinition.orderIndex())
        .returning()
        .fetchOptional(getAttributeDefinitionRecordMapper());
  }

  @Override
  public Optional<AttributeDefinition> updateAttribute(
      Long id, AttributeDefinition attributeDefinition) {
    return dslContext
        .update(AD)
        .set(AD.COMPONENT_TYPE_ID, attributeDefinition.componentTypeId())
        .set(AD.NAME, attributeDefinition.name())
        .set(AD.LABEL, attributeDefinition.label())
        .set(AD.DATA_TYPE, attributeDefinition.dataType().name())
        .set(AD.ENUM_VALUES_JSON, enumValuesConverter.to(attributeDefinition.enumValues()))
        .set(AD.IS_REQUIRED, attributeDefinition.isRequired())
        .set(AD.ORDER_INDEX, attributeDefinition.orderIndex())
        .set(AD.CREATED_AT, attributeDefinition.createdAt())
        .where(AD.ID.eq(id))
        .returning()
        .fetchOptional(getAttributeDefinitionRecordMapper());
  }

  @Override
  public void deleteById(Long id) {
    dslContext.delete(AD).where(AD.ID.eq(id)).execute();
  }

  @Override
  public boolean existsById(Long id) {
    return dslContext.fetchExists(dslContext.selectFrom(AD).where(AD.ID.eq(id)));
  }

  @Override
  public List<AttributeDefinition> getByComponentTypeId(Long componentTypeId) {
    return dslContext
        .selectFrom(AD)
        .where(AD.COMPONENT_TYPE_ID.eq(componentTypeId))
        .fetch(getAttributeDefinitionRecordMapper());
  }

  @Override
  public Optional<AttributeDefinition> getById(Long id) {
    return dslContext
        .selectFrom(AD)
        .where(AD.ID.eq(id))
        .fetchOptional(getAttributeDefinitionRecordMapper());
  }

  private RecordMapper<AttributeDefinitionRecord, AttributeDefinition>
      getAttributeDefinitionRecordMapper() {
    return r ->
        AttributeDefinition.builder()
            .id(r.get(AD.ID))
            .componentTypeId(r.get(AD.COMPONENT_TYPE_ID))
            .name(r.get(AD.NAME))
            .label(r.get(AD.LABEL))
            .dataType(DataType.valueOf(r.get(AD.DATA_TYPE)))
            .enumValues(enumValuesConverter.from(r.get(AD.ENUM_VALUES_JSON)))
            .isRequired(r.get(AD.IS_REQUIRED))
            .orderIndex(r.get(AD.ORDER_INDEX))
            .createdAt(r.get(AD.CREATED_AT))
            .build();
  }
}
