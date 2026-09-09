package ru.sultanyarov.configurator.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.exception.ValidationException;

class ConfigurationListFilterTest {
  @Test
  void shouldNormalizeSearchAndPreserveDefaultOrder() {
    assertThat(ConfigurationListFilter.of(null, null, null))
        .isEqualTo(
            new ConfigurationListFilter("", ConfigurationListFilter.SortBy.CREATED_AT, false));
    assertThat(ConfigurationListFilter.of("  Build  ", "name", "asc"))
        .isEqualTo(new ConfigurationListFilter("Build", ConfigurationListFilter.SortBy.NAME, true));
    assertThat(ConfigurationListFilter.of("", "createdAt", "desc").ascending()).isFalse();
  }

  @Test
  void shouldRejectUnsupportedQueryValues() {
    assertThatThrownBy(() -> ConfigurationListFilter.of("x".repeat(256), null, null))
        .isInstanceOf(ValidationException.class);
    assertThatThrownBy(() -> ConfigurationListFilter.of(null, "id", null))
        .isInstanceOf(ValidationException.class);
    assertThatThrownBy(() -> ConfigurationListFilter.of(null, null, "random"))
        .isInstanceOf(ValidationException.class);
  }
}
