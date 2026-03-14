package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.NumberUtils;
import ru.sultanyarov.configurator.application.port.out.AttributeValueRepository;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.model.AttributeValue;
import ru.sultanyarov.configurator.domain.model.DataType;

import java.math.BigInteger;
import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

@Repository
@RequiredArgsConstructor
public class AttributeValueRepositoryImpl implements AttributeValueRepository {
    private static final ru.sultanyarov.configurator.domain.entity.jooq.tables.AttributeValue AV = Tables.ATTRIBUTE_VALUE;

    private final DSLContext dslContext;

    @Override
    public List<AttributeValue> createAttributeValues(List<AttributeValue> newComponentAttributes, Long componentId) {
        if (isEmpty(newComponentAttributes)) {
            return List.of();
        }

        var insert = dslContext.insertInto(
                Tables.ATTRIBUTE_VALUE,
                Tables.ATTRIBUTE_VALUE.COMPONENT_ID,
                Tables.ATTRIBUTE_VALUE.ATTRIBUTE_DEFINITION_ID,
                Tables.ATTRIBUTE_VALUE.VALUE_STRING,
                Tables.ATTRIBUTE_VALUE.VALUE_NUMBER,
                Tables.ATTRIBUTE_VALUE.VALUE_BOOLEAN
        );

        for (AttributeValue newComponentAttribute : newComponentAttributes) {
            insert = insert.values(
                    componentId,
                    newComponentAttribute.attributeDefinitionId(),
                    newComponentAttribute.dataType() == DataType.STRING || newComponentAttribute.dataType() == DataType.ENUM ? newComponentAttribute.value() : null,
                    newComponentAttribute.dataType() == DataType.NUMBER ? NumberUtils.parseNumber(newComponentAttribute.value(), BigInteger.class) : null,
                    newComponentAttribute.dataType() == DataType.BOOLEAN ? Boolean.valueOf(newComponentAttribute.value()) : null
            );
        }

        var inserted = insert
                .returning(AV.ID)
                .fetch(AV.ID);

        return fetchFullAttributeValuesByIds(inserted);
    }

    private List<AttributeValue> fetchFullAttributeValuesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return dslContext
                .select(
                        Tables.ATTRIBUTE_VALUE.ID,
                        Tables.ATTRIBUTE_VALUE.ATTRIBUTE_DEFINITION_ID,
                        Tables.ATTRIBUTE_DEFINITION.NAME,
                        Tables.ATTRIBUTE_DEFINITION.LABEL,
                        Tables.ATTRIBUTE_DEFINITION.DATA_TYPE,
                        Tables.ATTRIBUTE_VALUE.VALUE_STRING,
                        Tables.ATTRIBUTE_VALUE.VALUE_NUMBER,
                        Tables.ATTRIBUTE_VALUE.VALUE_BOOLEAN
                )
                .from(Tables.ATTRIBUTE_VALUE)
                .join(Tables.ATTRIBUTE_DEFINITION)
                .on(AV.ATTRIBUTE_DEFINITION_ID.eq(Tables.ATTRIBUTE_DEFINITION.ID))
                .where(AV.ID.in(ids))
                .fetch(getAttributeValueRecordMapper());
    }

    private RecordMapper<Record, AttributeValue> getAttributeValueRecordMapper() {
        return attributeValueRecord -> {
            String value = firstNotNullValueAsString(attributeValueRecord);

            return AttributeValue.builder()
                    .id(attributeValueRecord.get(AV.ID))
                    .attributeDefinitionId(attributeValueRecord.get(AV.ATTRIBUTE_DEFINITION_ID))
                    .name(attributeValueRecord.get(Tables.ATTRIBUTE_DEFINITION.NAME))
                    .label(attributeValueRecord.get(Tables.ATTRIBUTE_DEFINITION.LABEL))
                    .dataType(DataType.valueOf(attributeValueRecord.get(Tables.ATTRIBUTE_DEFINITION.DATA_TYPE)))
                    .value(value)
                    .build();
        };
    }

    private String firstNotNullValueAsString(Record attributeValueRecord) {
        Object rawValue = ObjectUtils.firstNonNull(
                attributeValueRecord.get(AV.VALUE_STRING),
                attributeValueRecord.get(AV.VALUE_NUMBER),
                attributeValueRecord.get(AV.VALUE_BOOLEAN)
        );

        return rawValue == null ? null : String.valueOf(rawValue);
    }
}
