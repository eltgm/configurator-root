package ru.sultanyarov.configurator.domain.repository.jooq;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import org.springframework.util.NumberUtils;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.model.AttributeValue;
import ru.sultanyarov.configurator.domain.model.DataType;
import ru.sultanyarov.configurator.domain.repository.AttributeValueRepository;

import java.math.BigInteger;
import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

@Repository
@RequiredArgsConstructor
public class AttributeValueRepositoryImpl implements AttributeValueRepository {
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
                    newComponentAttribute.dataType() == DataType.STRING ? newComponentAttribute.value() : null,
                    newComponentAttribute.dataType() == DataType.NUMBER ? NumberUtils.parseNumber(newComponentAttribute.value(), BigInteger.class) : null,
                    newComponentAttribute.dataType() == DataType.BOOLEAN ? Boolean.valueOf(newComponentAttribute.value()) : null
            );
        }

        var inserted = insert
                .returning(Tables.ATTRIBUTE_VALUE.ID)
                .fetch(Tables.ATTRIBUTE_VALUE.ID);

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
                .on(Tables.ATTRIBUTE_VALUE.ATTRIBUTE_DEFINITION_ID.eq(Tables.ATTRIBUTE_DEFINITION.ID))
                .where(Tables.ATTRIBUTE_VALUE.ID.in(ids))
                .fetch(record -> {
                    String value = firstNotNullValueAsString(record);

                    return AttributeValue.builder()
                            .id(record.get(Tables.ATTRIBUTE_VALUE.ID))
                            .attributeDefinitionId(record.get(Tables.ATTRIBUTE_VALUE.ATTRIBUTE_DEFINITION_ID))
                            .name(record.get(Tables.ATTRIBUTE_DEFINITION.NAME))
                            .label(record.get(Tables.ATTRIBUTE_DEFINITION.LABEL))
                            .dataType(DataType.valueOf(record.get(Tables.ATTRIBUTE_DEFINITION.DATA_TYPE)))
                            .value(value)
                            .build();
                });
    }

    private String firstNotNullValueAsString(
            Record record
    ) {
        Object rawValue = ObjectUtils.firstNonNull(
                record.get(Tables.ATTRIBUTE_VALUE.VALUE_STRING),
                record.get(Tables.ATTRIBUTE_VALUE.VALUE_NUMBER),
                record.get(Tables.ATTRIBUTE_VALUE.VALUE_BOOLEAN)
        );

        return rawValue == null ? null : String.valueOf(rawValue);
    }
}
