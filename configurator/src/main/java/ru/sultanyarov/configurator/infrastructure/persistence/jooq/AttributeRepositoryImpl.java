package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
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
  private static final ru.sultanyarov.configurator.domain.entity.jooq.tables.ComponentTypeAttribute
      CTA = Tables.COMPONENT_TYPE_ATTRIBUTE;

  private final DSLContext dslContext;
  private final StringToListConverter enumValuesConverter = new StringToListConverter();

  @Override
  public boolean hasByComponentTypeId(Long componentTypeId) {
    return dslContext.fetchExists(
        dslContext.selectFrom(CTA).where(CTA.COMPONENT_TYPE_ID.eq(componentTypeId)));
  }

  @Override
  public boolean hasByComponentTypeIdAndName(Long id, String name) {
    return dslContext.fetchExists(
        dslContext
            .selectOne()
            .from(CTA)
            .join(AD)
            .on(AD.ID.eq(CTA.ATTRIBUTE_DEFINITION_ID))
            .where(CTA.COMPONENT_TYPE_ID.eq(id))
            .and(AD.NAME.eq(name)));
  }

  @Override
  public Optional<AttributeDefinition> createAttributeDefinition(
      AttributeDefinition attributeDefinition) {
    return dslContext
        .insertInto(AD)
        .set(AD.DOMAIN_ID, attributeDefinition.domainId())
        .set(AD.NAME, attributeDefinition.name())
        .set(AD.LABEL, attributeDefinition.label())
        .set(AD.DATA_TYPE, attributeDefinition.dataType().name())
        .set(AD.ENUM_VALUES_JSON, enumValuesConverter.to(attributeDefinition.enumValues()))
        .returning()
        .fetchOptional(getAttributeDefinitionRecordMapper());
  }

  @Override
  public Optional<AttributeDefinition> updateAttribute(
      Long id, AttributeDefinition attributeDefinition) {
    return dslContext
        .update(AD)
        .set(AD.NAME, attributeDefinition.name())
        .set(AD.LABEL, attributeDefinition.label())
        .set(AD.DATA_TYPE, attributeDefinition.dataType().name())
        .set(AD.ENUM_VALUES_JSON, enumValuesConverter.to(attributeDefinition.enumValues()))
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
  public boolean existsByIdAndDomainId(Long id, Long domainId) {
    return dslContext.fetchExists(
        dslContext.selectFrom(AD).where(AD.ID.eq(id)).and(AD.DOMAIN_ID.eq(domainId)));
  }

  @Override
  public List<AttributeDefinition> getByDomainId(Long domainId) {
    return dslContext
        .selectFrom(AD)
        .where(AD.DOMAIN_ID.eq(domainId))
        .orderBy(AD.LABEL.asc(), AD.NAME.asc(), AD.ID.asc())
        .fetch(getAttributeDefinitionRecordMapper());
  }

  @Override
  public List<AttributeDefinition> getByComponentTypeId(Long componentTypeId) {
    return dslContext
        .select(
            AD.ID,
            AD.DOMAIN_ID,
            AD.NAME,
            AD.LABEL,
            AD.DATA_TYPE,
            AD.ENUM_VALUES_JSON,
            AD.CREATED_AT,
            CTA.COMPONENT_TYPE_ID,
            CTA.IS_REQUIRED,
            CTA.ORDER_INDEX)
        .from(CTA)
        .join(AD)
        .on(AD.ID.eq(CTA.ATTRIBUTE_DEFINITION_ID))
        .where(CTA.COMPONENT_TYPE_ID.eq(componentTypeId))
        .orderBy(CTA.ORDER_INDEX.asc().nullsLast(), AD.LABEL.asc(), AD.NAME.asc(), AD.ID.asc())
        .fetch(getLinkedAttributeDefinitionRecordMapper());
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
            .domainId(r.get(AD.DOMAIN_ID))
            .name(r.get(AD.NAME))
            .label(r.get(AD.LABEL))
            .dataType(DataType.valueOf(r.get(AD.DATA_TYPE)))
            .enumValues(enumValuesConverter.from(r.get(AD.ENUM_VALUES_JSON)))
            .createdAt(r.get(AD.CREATED_AT))
            .build();
  }

  private RecordMapper<Record, AttributeDefinition> getLinkedAttributeDefinitionRecordMapper() {
    return r ->
        AttributeDefinition.builder()
            .id(r.get(AD.ID))
            .domainId(r.get(AD.DOMAIN_ID))
            .componentTypeId(r.get(CTA.COMPONENT_TYPE_ID))
            .name(r.get(AD.NAME))
            .label(r.get(AD.LABEL))
            .dataType(DataType.valueOf(r.get(AD.DATA_TYPE)))
            .enumValues(enumValuesConverter.from(r.get(AD.ENUM_VALUES_JSON)))
            .isRequired(r.get(CTA.IS_REQUIRED))
            .orderIndex(r.get(CTA.ORDER_INDEX))
            .createdAt(r.get(AD.CREATED_AT))
            .build();
  }
}
