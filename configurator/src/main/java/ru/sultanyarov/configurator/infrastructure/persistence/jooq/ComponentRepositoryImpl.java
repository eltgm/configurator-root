package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import lombok.*;
import lombok.extern.slf4j.*;
import org.apache.commons.lang3.*;
import org.jooq.*;
import org.jooq.Record;
import org.springframework.stereotype.*;
import ru.sultanyarov.configurator.application.port.out.*;
import ru.sultanyarov.configurator.common.util.*;
import ru.sultanyarov.configurator.domain.entity.jooq.*;
import ru.sultanyarov.configurator.domain.model.*;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.DataType;

import java.util.*;

import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.multiset;
import static ru.sultanyarov.configurator.common.util.PaginationHelper.*;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ComponentRepositoryImpl implements ComponentRepository {
    private static final ru.sultanyarov.configurator.domain.entity.jooq.tables.Component COMPONENT = Tables.COMPONENT;

    private final DSLContext dslContext;

    @Override
    public Optional<Component> createComponent(Component componentToCreate) {
        return dslContext.insertInto(COMPONENT)
                .set(dslContext.newRecord(COMPONENT, componentToCreate))
                .returning()
                .fetchOptional(getComponentRecordMapper());
    }

    @Override
    public Optional<Component> getById(Long id) {
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
        fields.add(imagesField());

        return dslContext.select(fields)
                .from(COMPONENT)
                .where(COMPONENT.ID.eq(id))
                .fetchOptional(getComponentRecordMapper());
    }

    @Override
    public Optional<Component> updateComponent(Long id, Component component) {
        return dslContext.update(COMPONENT)
                .set(COMPONENT.NAME, component.getName())
                .set(COMPONENT.BRAND, component.getBrand())
                .set(COMPONENT.DESCRIPTION, component.getDescription())
                .where(COMPONENT.ID.eq(id))
                .returning()
                .fetchOptional(getComponentRecordMapper());
    }

    @Override
    public boolean archiveComponentById(Long id) {
        return dslContext.update(COMPONENT)
                .set(COMPONENT.ARCHIVED, true)
                .where(COMPONENT.ID.eq(id))
                .execute() > 0;
    }

    @Override
    public Optional<ComponentImage> createImage(ComponentImage image) {
        var componentImage = Tables.COMPONENT_IMAGE;
        return dslContext.insertInto(componentImage)
                .set(componentImage.COMPONENT_ID, image.componentId())
                .set(componentImage.FILE_PATH, image.url())
                .set(componentImage.ORDER_INDEX, image.orderIndex())
                .returning()
                .fetchOptional(record -> ComponentImage.builder()
                        .id(record.get(componentImage.ID))
                        .componentId(record.get(componentImage.COMPONENT_ID))
                        .url(record.get(componentImage.FILE_PATH))
                        .orderIndex(record.get(componentImage.ORDER_INDEX))
                        .build());
    }

    @Override
    public int getNextImageOrderIndex(Long componentId) {
        var componentImage = Tables.COMPONENT_IMAGE;
        Field<Integer> maximumOrderIndexField = max(componentImage.ORDER_INDEX);
        Integer maximumOrderIndex = dslContext.select(maximumOrderIndexField)
                .from(componentImage)
                .where(componentImage.COMPONENT_ID.eq(componentId))
                .fetchOne(maximumOrderIndexField);
        return maximumOrderIndex == null ? 0 : maximumOrderIndex + 1;
    }

    @Override
    public boolean hasByComponentTypeId(Long componentTypeId) {
        return dslContext.fetchExists(
                dslContext.selectFrom(COMPONENT)
                        .where(COMPONENT.COMPONENT_TYPE_ID.eq(componentTypeId))
        );
    }

    @Override
    public Page<Component> findPageByDomainIdComponentTypeIdName(Long domainId, Long componentTypeId, String name, Integer page, Integer size) {
        Condition condition = COMPONENT.COMPONENT_TYPE_ID.in(
                dslContext.select(COMPONENT_TYPE.ID)
                        .from(COMPONENT_TYPE)
                        .where(COMPONENT_TYPE.DOMAIN_ID.eq(domainId))
        );

        if (componentTypeId != null) {
            condition = condition.and(COMPONENT.COMPONENT_TYPE_ID.eq(componentTypeId));
        }

        if (name != null && !name.isBlank()) {
            condition = condition.and(COMPONENT.NAME.eq(name));
        }

        return jooqPage(
                dslContext,
                dslContext
                        .selectFrom(COMPONENT)
                        .where(condition)
                        .orderBy(List.of(COMPONENT.ID)),
                condition,
                COMPONENT,
                page,
                size,
                getComponentRecordMapper()
        );
    }

    private RecordMapper<org.jooq.Record, Component> getComponentRecordMapper() {
        return componentRecord -> Component.builder()
                .id(componentRecord.get(COMPONENT.ID))
                .componentTypeId(componentRecord.get(COMPONENT.COMPONENT_TYPE_ID))
                .name(componentRecord.get(COMPONENT.NAME))
                .brand(componentRecord.get(COMPONENT.BRAND))
                .description(componentRecord.get(COMPONENT.DESCRIPTION))
                .archived(componentRecord.get(COMPONENT.ARCHIVED))
                .createdAt(componentRecord.get(COMPONENT.CREATED_AT))
                .attributes(JooqMapperUtils.getListOrNull(componentRecord, "attributes"))
                .images(JooqMapperUtils.getListOrNull(componentRecord, "images"))
                .build();
    }

    private SelectField<List<AttributeValue>> attributeValuesField() {
        var attributeValue = Tables.ATTRIBUTE_VALUE;
        var attributeDefinition = Tables.ATTRIBUTE_DEFINITION;

        return multiset(
                dslContext.select(
                                attributeValue.ID,
                                attributeValue.ATTRIBUTE_DEFINITION_ID,
                                attributeDefinition.NAME,
                                attributeDefinition.LABEL,
                                attributeDefinition.DATA_TYPE,
                                attributeValue.VALUE_STRING,
                                attributeValue.VALUE_NUMBER,
                                attributeValue.VALUE_BOOLEAN
                        )
                        .from(attributeValue)
                        .join(attributeDefinition)
                        .on(attributeDefinition.ID.eq(attributeValue.ATTRIBUTE_DEFINITION_ID))
                        .where(attributeValue.COMPONENT_ID.eq(COMPONENT.ID))
                        .orderBy(attributeDefinition.ORDER_INDEX, attributeValue.ID)
        )
                .convertFrom(result -> result.map(this::mapAttributeValue))
                .as("attributes");
    }

    private AttributeValue mapAttributeValue(Record record) {
        var attributeValue = Tables.ATTRIBUTE_VALUE;
        var attributeDefinition = Tables.ATTRIBUTE_DEFINITION;
        Object rawValue = ObjectUtils.firstNonNull(
                record.get(attributeValue.VALUE_STRING),
                record.get(attributeValue.VALUE_NUMBER),
                record.get(attributeValue.VALUE_BOOLEAN)
        );

        return AttributeValue.builder()
                .id(record.get(attributeValue.ID))
                .attributeDefinitionId(record.get(attributeValue.ATTRIBUTE_DEFINITION_ID))
                .name(record.get(attributeDefinition.NAME))
                .label(record.get(attributeDefinition.LABEL))
                .dataType(DataType.valueOf(record.get(attributeDefinition.DATA_TYPE)))
                .value(rawValue == null ? null : String.valueOf(rawValue))
                .build();
    }

    private SelectField<List<ComponentImage>> imagesField() {
        var componentImage = Tables.COMPONENT_IMAGE;

        return multiset(
                dslContext.select(
                                componentImage.ID,
                                componentImage.COMPONENT_ID,
                                componentImage.FILE_PATH,
                                componentImage.ORDER_INDEX
                        )
                        .from(componentImage)
                        .where(componentImage.COMPONENT_ID.eq(COMPONENT.ID))
                        .orderBy(componentImage.ORDER_INDEX, componentImage.ID)
        )
                .convertFrom(result -> result.map(record -> ComponentImage.builder()
                        .id(record.get(componentImage.ID))
                        .componentId(record.get(componentImage.COMPONENT_ID))
                        .url(record.get(componentImage.FILE_PATH))
                        .orderIndex(record.get(componentImage.ORDER_INDEX))
                        .build()))
                .as("images");
    }
}
