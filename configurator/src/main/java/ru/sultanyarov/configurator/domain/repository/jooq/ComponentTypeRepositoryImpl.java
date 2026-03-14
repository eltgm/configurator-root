package ru.sultanyarov.configurator.domain.repository.jooq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.RecordMapper;
import org.jooq.SelectField;
import org.jooq.SelectFieldOrAsterisk;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.entity.jooq.tables.records.ComponentTypeRecord;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.DataType;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.repository.ComponentTypeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.row;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ComponentTypeRepositoryImpl implements ComponentTypeRepository {
    private final DSLContext dslContext;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<ComponentType> createComponentType(ComponentType componentType) {
        return dslContext.insertInto(Tables.COMPONENT_TYPE)
                .set(dslContext.newRecord(Tables.COMPONENT_TYPE, componentType))
                .returning()
                .fetchOptional(getComponentTypeRecordMapper());
    }

    @Override
    public boolean existsByNameAndDomainId(String name, Long domainId) {
        return dslContext.fetchExists(
                dslContext.selectFrom(Tables.COMPONENT_TYPE)
                        .where(Tables.COMPONENT_TYPE.NAME.eq(name))
                        .and(Tables.COMPONENT_TYPE.DOMAIN_ID.eq(domainId))
        );
    }

    @Override
    public boolean existsById(Long id) {
        return dslContext.fetchExists(
                dslContext.selectFrom(Tables.COMPONENT_TYPE)
                        .where(Tables.COMPONENT_TYPE.ID.eq(id))
        );
    }

    @Override
    public Optional<ComponentType> updateComponentType(Long id, ComponentType componentType) {
        return dslContext.update(Tables.COMPONENT_TYPE)
                .set(dslContext.newRecord(Tables.COMPONENT_TYPE, componentType))
                .where(Tables.COMPONENT_TYPE.ID.eq(id))
                .returning()
                .fetchOptional(getComponentTypeRecordMapper());
    }

    @Override
    public void deleteComponentTypeById(Long id) {
        dslContext.deleteFrom(Tables.COMPONENT_TYPE)
                .where(Tables.COMPONENT_TYPE.ID.eq(id))
                .execute();
    }

    @Override
    public Optional<ComponentType> getComponentTypeById(Long id) {
        var fields = new ArrayList<>(baseFields());
        fields.add(domainField());
        fields.add(attributeDefinitionsFields());
        fields.add(componentsFields());

        return dslContext.select(fields)
                .from(Tables.COMPONENT_TYPE)
                .join(Tables.DOMAIN).on(Tables.DOMAIN.ID.eq(Tables.COMPONENT_TYPE.DOMAIN_ID))
                .where(Tables.COMPONENT_TYPE.ID.eq(id))
                .fetchOptional(
                        record -> ComponentType.builder()
                                .id(record.get(Tables.COMPONENT_TYPE.ID))
                                .domainId(record.get(Tables.COMPONENT_TYPE.DOMAIN_ID))
                                .domain((Domain) record.get("domain"))
                                .attributeDefinitions((List<AttributeDefinition>) record.get("attributeDefinitions"))
                                .components((List<Component>) record.get("components"))
                                .name(record.get(Tables.COMPONENT_TYPE.NAME))
                                .code(record.get(Tables.COMPONENT_TYPE.CODE))
                                .description(record.get(Tables.COMPONENT_TYPE.DESCRIPTION))
                                .orderIndex(record.get(Tables.COMPONENT_TYPE.ORDER_INDEX))
                                .createdAt(record.get(Tables.COMPONENT_TYPE.CREATED_AT))
                                .build()
                );
    }

    private SelectFieldOrAsterisk componentsFields() {
        return multiset(
                dslContext.select(Tables.COMPONENT.ID,
                                Tables.COMPONENT.COMPONENT_TYPE_ID,
                                Tables.COMPONENT.NAME)
                        .from(Tables.COMPONENT)
                        .where(Tables.COMPONENT.COMPONENT_TYPE_ID.eq(Tables.COMPONENT_TYPE.ID))
        )
                .convertFrom(rs -> rs.map(
                        componentRecord ->
                                Component.builder()
                                        .id(componentRecord.get(Tables.COMPONENT.ID))
                                        .componentTypeId(componentRecord.get(Tables.COMPONENT.COMPONENT_TYPE_ID))
                                        .name(componentRecord.get(Tables.COMPONENT.NAME))
                                        .build()
                ))
                .as("components");
    }

    private SelectField<List<AttributeDefinition>> attributeDefinitionsFields() {
        //чтобы избежать дедупликации при связи 1->многие
        return multiset(
                dslContext.select(Tables.ATTRIBUTE_DEFINITION.ID,
                                Tables.ATTRIBUTE_DEFINITION.COMPONENT_TYPE_ID,
                                Tables.ATTRIBUTE_DEFINITION.NAME,
                                Tables.ATTRIBUTE_DEFINITION.LABEL,
                                Tables.ATTRIBUTE_DEFINITION.DATA_TYPE,
                                Tables.ATTRIBUTE_DEFINITION.ENUM_VALUES_JSON,
                                Tables.ATTRIBUTE_DEFINITION.IS_REQUIRED,
                                Tables.ATTRIBUTE_DEFINITION.ORDER_INDEX,
                                Tables.ATTRIBUTE_DEFINITION.CREATED_AT)
                        .from(Tables.ATTRIBUTE_DEFINITION)
                        .where(Tables.ATTRIBUTE_DEFINITION.COMPONENT_TYPE_ID.eq(Tables.COMPONENT_TYPE.ID))
        )
                .convertFrom(rs -> rs.map(
                        attributeDefinitionRecord ->
                                AttributeDefinition.builder()
                                        .id(attributeDefinitionRecord.get(Tables.ATTRIBUTE_DEFINITION.ID))
                                        .componentTypeId(attributeDefinitionRecord.get(Tables.ATTRIBUTE_DEFINITION.COMPONENT_TYPE_ID))
                                        .name(attributeDefinitionRecord.get(Tables.ATTRIBUTE_DEFINITION.NAME))
                                        .label(attributeDefinitionRecord.get(Tables.ATTRIBUTE_DEFINITION.LABEL))
                                        .dataType(DataType.valueOf(attributeDefinitionRecord.get(Tables.ATTRIBUTE_DEFINITION.DATA_TYPE)))
                                        .enumValues(objectMapper.convertValue(attributeDefinitionRecord.get(Tables.ATTRIBUTE_DEFINITION.ENUM_VALUES_JSON), Set.class))
                                        .isRequired(attributeDefinitionRecord.get(Tables.ATTRIBUTE_DEFINITION.IS_REQUIRED))
                                        .orderIndex(attributeDefinitionRecord.get(Tables.ATTRIBUTE_DEFINITION.ORDER_INDEX))
                                        .createdAt(attributeDefinitionRecord.get(Tables.ATTRIBUTE_DEFINITION.CREATED_AT))
                                        .build()
                ))
                .as("attributeDefinitions");
    }

    @Override
    public List<ComponentType> getComponentTypesByDomainId(Long domainId) {
        return dslContext.selectFrom(Tables.COMPONENT_TYPE)
                .where(Tables.COMPONENT_TYPE.DOMAIN_ID.eq(domainId))
                .fetch(getComponentTypeRecordMapper());
    }

    private RecordMapper<ComponentTypeRecord, ComponentType> getComponentTypeRecordMapper() {
        return record -> ComponentType.builder()
                .id(record.getId())
                .domainId(record.getDomainId())
                .name(record.getName())
                .code(record.getCode())
                .description(record.getDescription())
                .orderIndex(record.getOrderIndex())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private SelectField<Domain> domainField() {
        return row(
                Tables.DOMAIN.ID,
                Tables.DOMAIN.NAME,
                Tables.DOMAIN.DESCRIPTION,
                Tables.DOMAIN.CREATED_BY_USER_ID,
                Tables.DOMAIN.CREATED_AT
        )
                .convertFrom(rs -> rs.map(
                        attributeDefinitionRecord ->
                                Domain.builder()
                                        .id(attributeDefinitionRecord.get(Tables.DOMAIN.ID))
                                        .name(attributeDefinitionRecord.get(Tables.DOMAIN.NAME))
                                        .description(attributeDefinitionRecord.get(Tables.DOMAIN.DESCRIPTION))
                                        .createdByUserId(attributeDefinitionRecord.get(Tables.DOMAIN.CREATED_BY_USER_ID))
                                        .createdAt(attributeDefinitionRecord.get(Tables.DOMAIN.CREATED_AT))
                                        .build())
                )
                .as("domain");
    }

    private List<SelectFieldOrAsterisk> baseFields() {
        return List.of(
                Tables.COMPONENT_TYPE.ID,
                Tables.COMPONENT_TYPE.DOMAIN_ID,
                Tables.COMPONENT_TYPE.NAME,
                Tables.COMPONENT_TYPE.CODE,
                Tables.COMPONENT_TYPE.DESCRIPTION,
                Tables.COMPONENT_TYPE.ORDER_INDEX,
                Tables.COMPONENT_TYPE.CREATED_AT
        );
    }
}
