package ru.sultanyarov.configurator.infrastructure.persistence.jooq.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.jooq.exception.DataTypeException;
import org.junit.jupiter.api.Test;

class StringToListConverterTest {

  private final StringToListConverter converter = new StringToListConverter();

  @Test
  void from_shouldConvertJsonArrayToSet() {
    assertThat(converter.from("[\"INTEGER\",\"STRING\",\"INTEGER\"]"))
        .containsExactlyInAnyOrder("INTEGER", "STRING");
  }

  @Test
  void from_shouldReturnEmptySetForNull() {
    assertThat(converter.from(null)).isEmpty();
  }

  @Test
  void from_shouldRejectMalformedJson() {
    assertThatThrownBy(() -> converter.from("not-json"))
        .isInstanceOf(DataTypeException.class)
        .hasMessage("Error converting JSON string to a set of strings");
  }

  @Test
  void to_shouldSerializeSetAndPreserveNull() {
    assertThat(converter.to(Set.of("STRING"))).isEqualTo("[\"STRING\"]");
    assertThat(converter.to(null)).isNull();
  }

  @Test
  void types_shouldExposeJooqConversionTypes() {
    assertThat(converter.fromType()).isEqualTo(String.class);
    assertThat(converter.toType()).isEqualTo(Set.class);
  }
}
