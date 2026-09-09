package ru.sultanyarov.configurator.domain.model;

import ru.sultanyarov.configurator.domain.exception.ValidationException;

public record ConfigurationListFilter(String name, SortBy sortBy, boolean ascending) {
  public enum SortBy {
    CREATED_AT,
    NAME
  }

  public static ConfigurationListFilter of(String name, String sortBy, String direction) {
    String normalizedName = name == null ? "" : name.trim();
    if (normalizedName.length() > 255)
      throw new ValidationException("Configuration search is too long");
    SortBy field;
    if (sortBy == null || sortBy.equals("createdAt")) field = SortBy.CREATED_AT;
    else if (sortBy.equals("name")) field = SortBy.NAME;
    else throw new ValidationException("Unsupported configuration sort field");
    if (direction != null && !direction.equals("asc") && !direction.equals("desc")) {
      throw new ValidationException("Unsupported configuration sort direction");
    }
    return new ConfigurationListFilter(normalizedName, field, "asc".equals(direction));
  }
}
