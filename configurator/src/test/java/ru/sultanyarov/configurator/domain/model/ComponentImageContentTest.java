package ru.sultanyarov.configurator.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ComponentImageContentTest {
  @Test
  void shouldDefensivelyCopyContent() {
    byte[] source = new byte[] {1, 2, 3};
    ComponentImageContent content = new ComponentImageContent(source, "image/png");

    source[0] = 9;
    byte[] returned = content.content();
    returned[1] = 9;

    assertThat(content.content()).containsExactly(1, 2, 3);
    assertThat(content.contentLength()).isEqualTo(3);
  }

  @Test
  void shouldRejectMissingMetadata() {
    assertThatThrownBy(() -> new ComponentImageContent(null, "image/png"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ComponentImageContent(new byte[] {1}, " "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
