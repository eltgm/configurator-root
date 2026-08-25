package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.row;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.SelectField;
import org.jooq.SelectFieldOrAsterisk;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.application.port.out.ComponentTypeRepository;
import ru.sultanyarov.configurator.common.util.JooqMapperUtils;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.DataType;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.infrastructure.persistence.jooq.config.StringToListConverter;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ComponentTypeRepositoryImpl implements ComponentTypeRepository {
  private static final ru.sultanyarov.configurator.domain.entity.jooq.tables.ComponentType CT =
      Tables.COMPONENT_TYPE;
  private static final ru.sultanyarov.configurator.domain.entity.jooq.tables.AttributeDefinition
      AD = Tables.ATTRIBUTE_DEFINITION;
  private static final ru.sultanyarov.configurator.domain.entity.jooq.tables.ComponentTypeAttribute
      CTA = Tables.COMPONENT_TYPE_ATTRIBUTE;

  private final DSLContext dslContext;
  private final StringToListConverter stringToListConverter = new StringToListConverter();

  @Override
  public Optional<ComponentType> createComponentType(ComponentType componentType) {
    return dslContext
        .insertInto(CT)
        .set(dslContext.newRecord(CT, componentType))
        .returning()
        .fetchOptional(getComponentTypeRecordMapper());
  }

  @Override
  public boolean existsByNameAndDomainId(String name, Long domainId) {
    return dslContext.fetchExists(
        dslContext.selectFrom(CT).where(CT.NAME.eq(name)).and(CT.DOMAIN_ID.eq(domainId)));
  }

  @Override
  public boolean existsById(Long id) {
    return dslContext.fetchExists(dslContext.selectFrom(CT).where(CT.ID.eq(id)));
  }

  @Override
  public Optional<ComponentType> updateComponentType(Long id, ComponentType componentType) {
    return dslContext
        .update(CT)
        .set(dslContext.newRecord(CT, componentType))
        .where(CT.ID.eq(id))
        .returning()
        .fetchOptional(getComponentTypeRecordMapper());
  }

  @Override
  public void deleteComponentTypeById(Long id) {
    dslContext.deleteFrom(CT).where(CT.ID.eq(id)).execute();
  }

  @Override
  public Optional<ComponentType> getComponentTypeById(Long id) {
    var fields = new ArrayList<>(baseFields());
    fields.add(domainField());
    fields.add(attributeDefinitionsFields());
    fields.add(componentsFields());

    return dslContext
        .select(fields)
        .from(CT)
        .join(Tables.DOMAIN)
        .on(Tables.DOMAIN.ID.eq(CT.DOMAIN_ID))
        .where(CT.ID.eq(id))
        .fetchOptional(getComponentTypeRecordMapper());
  }

  private SelectFieldOrAsterisk componentsFields() {
    return multiset(
            dslContext
                .select(
                    Tables.COMPONENT.ID, Tables.COMPONENT.COMPONENT_TYPE_ID, Tables.COMPONENT.NAME)
                .from(Tables.COMPONENT)
                .where(Tables.COMPONENT.COMPONENT_TYPE_ID.eq(CT.ID)))
        .convertFrom(
            rs ->
                rs.map(
                    componentRecord ->
                        Component.builder()
                            .id(componentRecord.get(Tables.COMPONENT.ID))
                            .componentTypeId(
                                componentRecord.get(Tables.COMPONENT.COMPONENT_TYPE_ID))
                            .name(componentRecord.get(Tables.COMPONENT.NAME))
                            .build()))
        .as("components");
  }

  private SelectField<List<AttributeDefinition>> attributeDefinitionsFields() {
    // чтобы избежать дедупликации при связи 1->многие
    return multiset(
            dslContext
                .select(
                    AD.ID,
                    AD.DOMAIN_ID,
                    CTA.COMPONENT_TYPE_ID,
                    AD.NAME,
                    AD.LABEL,
                    AD.DATA_TYPE,
                    AD.ENUM_VALUES_JSON,
                    CTA.IS_REQUIRED,
                    CTA.ORDER_INDEX,
                    AD.CREATED_AT)
                .from(CTA)
                .join(AD)
                .on(AD.ID.eq(CTA.ATTRIBUTE_DEFINITION_ID))
                .where(CTA.COMPONENT_TYPE_ID.eq(CT.ID))
                .orderBy(CTA.ORDER_INDEX.asc().nullsLast(), AD.LABEL, AD.NAME, AD.ID))
        .convertFrom(rs -> rs.map(getAttributeDefinitionRecordMapper()))
        .as("attributeDefinitions");
  }

  @Override
  public List<ComponentType> getComponentTypesByDomainId(Long domainId) {
    return dslContext
        .selectFrom(CT)
        .where(CT.DOMAIN_ID.eq(domainId))
        .fetch(getComponentTypeRecordMapper());
  }

  @Override
  public boolean hasByDomainId(Long domainId) {
    return dslContext.fetchExists(dslContext.selectFrom(CT).where(CT.DOMAIN_ID.eq(domainId)));
  }

  private RecordMapper<Record, ComponentType> getComponentTypeRecordMapper() {
    return componentTypeRecord ->
        ComponentType.builder()
            .id(componentTypeRecord.get(CT.ID))
            .domainId(componentTypeRecord.get(CT.DOMAIN_ID))
            .domain(JooqMapperUtils.getOrNull(componentTypeRecord, "domain", Domain.class))
            .attributeDefinitions(
                JooqMapperUtils.getListOrNull(componentTypeRecord, "attributeDefinitions"))
            .components(JooqMapperUtils.getListOrNull(componentTypeRecord, "components"))
            .name(componentTypeRecord.get(CT.NAME))
            .code(componentTypeRecord.get(CT.CODE))
            .description(componentTypeRecord.get(CT.DESCRIPTION))
            .orderIndex(componentTypeRecord.get(CT.ORDER_INDEX))
            .createdAt(componentTypeRecord.get(CT.CREATED_AT))
            .build();
  }

  private SelectField<Domain> domainField() {
    return row(
            Tables.DOMAIN.ID,
            Tables.DOMAIN.NAME,
            Tables.DOMAIN.DESCRIPTION,
            Tables.DOMAIN.CREATED_BY_USER_ID,
            Tables.DOMAIN.CREATED_AT)
        .convertFrom(rs -> rs.map(getRecordDomainRecordMapper()))
        .as("domain");
  }

  private RecordMapper<Record, Domain> getRecordDomainRecordMapper() {
    return domainRecord ->
        Domain.builder()
            .id(domainRecord.get(0, Long.class))
            .name(domainRecord.get(1, String.class))
            .description(domainRecord.get(2, String.class))
            .createdByUserId(domainRecord.get(3, Long.class))
            .createdAt(domainRecord.get(4, Tables.DOMAIN.CREATED_AT.getType()))
            .build();
  }

  private List<SelectFieldOrAsterisk> baseFields() {
    return List.of(
        CT.ID, CT.DOMAIN_ID, CT.NAME, CT.CODE, CT.DESCRIPTION, CT.ORDER_INDEX, CT.CREATED_AT);
  }

  private RecordMapper<Record, AttributeDefinition> getAttributeDefinitionRecordMapper() {
    return attributeDefinitionRecord ->
        AttributeDefinition.builder()
            .id(attributeDefinitionRecord.get(Tables.ATTRIBUTE_DEFINITION.ID))
            .domainId(attributeDefinitionRecord.get(AD.DOMAIN_ID))
            .componentTypeId(attributeDefinitionRecord.get(CTA.COMPONENT_TYPE_ID))
            .name(attributeDefinitionRecord.get(Tables.ATTRIBUTE_DEFINITION.NAME))
            .label(attributeDefinitionRecord.get(Tables.ATTRIBUTE_DEFINITION.LABEL))
            .dataType(
                DataType.valueOf(
                    attributeDefinitionRecord.get(Tables.ATTRIBUTE_DEFINITION.DATA_TYPE)))
            .enumValues(
                stringToListConverter.from(
                    attributeDefinitionRecord.get(Tables.ATTRIBUTE_DEFINITION.ENUM_VALUES_JSON)))
            .isRequired(attributeDefinitionRecord.get(CTA.IS_REQUIRED))
            .orderIndex(attributeDefinitionRecord.get(CTA.ORDER_INDEX))
            .createdAt(attributeDefinitionRecord.get(Tables.ATTRIBUTE_DEFINITION.CREATED_AT))
            .build();
  }
}
