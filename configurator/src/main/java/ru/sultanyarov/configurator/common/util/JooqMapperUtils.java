package ru.sultanyarov.configurator.common.util;

import java.util.List;
import lombok.experimental.UtilityClass;
import org.jooq.Record;

@UtilityClass
public class JooqMapperUtils {
  public static <T> T getOrNull(Record mappedRecord, String fieldName, Class<T> type) {
    return mappedRecord.field(fieldName) == null ? null : mappedRecord.get(fieldName, type);
  }

  /**
   * Returns a jOOQ multiset field using the element type established by its mapped query.
   *
   * <p>Java cannot express a {@code Class<List<T>>} token, so the cast is intentionally isolated at
   * this persistence boundary. Callers must use the same element type as the multiset converter
   * that produced the field.
   */
  @SuppressWarnings("unchecked")
  public static <T> List<T> getListOrNull(Record mappedRecord, String fieldName) {
    return mappedRecord.field(fieldName) == null ? null : (List<T>) mappedRecord.get(fieldName);
  }
}
