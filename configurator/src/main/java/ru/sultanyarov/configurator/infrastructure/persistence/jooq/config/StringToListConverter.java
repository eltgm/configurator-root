package ru.sultanyarov.configurator.infrastructure.persistence.jooq.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jooq.Converter;
import org.jooq.exception.DataTypeException;

public class StringToListConverter implements Converter<String, Set<String>> {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final CollectionType type =
      objectMapper.getTypeFactory().constructCollectionType(HashSet.class, String.class);

  @Override
  public Set<String> from(String databaseString) {
    if (databaseString == null) {
      return Collections.emptySet();
    }
    try {
      return objectMapper.readValue(databaseString, type);
    } catch (IOException e) {
      throw new DataTypeException("Error converting JSON string to a set of strings", e);
    }
  }

  @Override
  public String to(Set<String> pojoList) {
    if (pojoList == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(pojoList);
    } catch (JsonProcessingException e) {
      throw new DataTypeException("Error converting a set of strings to JSON", e);
    }
  }

  @Override
  @NotNull
  public Class<String> fromType() {
    return String.class;
  }

  @Override
  @NotNull
  @SuppressWarnings("unchecked") // Generic class literals do not exist; jOOQ only needs Set.class.
  public Class<Set<String>> toType() {
    return (Class<Set<String>>) (Class<?>) Set.class;
  }
}
