package ru.sultanyarov.configurator.application.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.sultanyarov.configurator.domain.exception.ImageTooLargeException;
import ru.sultanyarov.configurator.domain.exception.UnsupportedImageFormatException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.ComponentImageUpload;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComponentImageValidatorImplTest {
    private final ComponentImageValidator validator = new ComponentImageValidatorImpl();

    @ParameterizedTest
    @MethodSource("validImages")
    void validate_shouldAcceptSupportedImageContent(String contentType, byte[] content) {
        assertThatCode(() -> validator.validate(new ComponentImageUpload(content, contentType), 0))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectEmptyImage() {
        assertThatThrownBy(() -> validator.validate(
                new ComponentImageUpload(new byte[0], "image/png"),
                null
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Image file must not be empty");
    }

    @Test
    void validate_shouldRejectImageAboveTenMebibytes() {
        byte[] content = new byte[(int) ComponentImageValidator.MAX_IMAGE_SIZE_BYTES + 1];

        assertThatThrownBy(() -> validator.validate(
                new ComponentImageUpload(content, "image/png"),
                null
        ))
                .isInstanceOf(ImageTooLargeException.class)
                .hasMessageContaining("10 MiB");
    }

    @Test
    void validate_shouldRejectUnsupportedContentType() {
        assertThatThrownBy(() -> validator.validate(
                new ComponentImageUpload(new byte[]{1, 2, 3}, "image/gif"),
                null
        ))
                .isInstanceOf(UnsupportedImageFormatException.class);
    }

    @Test
    void validate_shouldRejectContentThatDoesNotMatchDeclaredType() {
        assertThatThrownBy(() -> validator.validate(
                new ComponentImageUpload(new byte[]{1, 2, 3}, "image/jpeg"),
                null
        ))
                .isInstanceOf(UnsupportedImageFormatException.class);
    }

    @Test
    void validate_shouldRejectNegativeOrderIndex() {
        assertThatThrownBy(() -> validator.validate(
                new ComponentImageUpload(png(), "image/png"),
                -1
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("order index");
    }

    private static Stream<Arguments> validImages() {
        return Stream.of(
                Arguments.of("image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}),
                Arguments.of("image/png", png()),
                Arguments.of("image/webp", new byte[]{
                        'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'
                })
        );
    }

    private static byte[] png() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
    }
}
