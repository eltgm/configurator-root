package ru.sultanyarov.configurator.common.util;

import lombok.experimental.UtilityClass;
import org.jooq.Record;

import java.util.List;

@UtilityClass
public class JooqMapperUtils {
    public static <T> T getOrNull(Record mappedRecord, String fieldName, Class<T> type) {
        return mappedRecord.field(fieldName) == null ? null : mappedRecord.get(fieldName, type);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getListOrNull(Record mappedRecord, String fieldName) {
        return mappedRecord.field(fieldName) == null
                ? null
                : (List<T>) mappedRecord.get(fieldName);
    }
}
