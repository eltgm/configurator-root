package ru.sultanyarov.configurator.application.validator;

import org.springframework.stereotype.Component;
import ru.sultanyarov.configurator.domain.exception.ImageTooLargeException;
import ru.sultanyarov.configurator.domain.exception.UnsupportedImageFormatException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.ComponentImageUpload;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class ComponentImageValidatorImpl implements ComponentImageValidator {
    private static final String JPEG = "image/jpeg";
    private static final String PNG = "image/png";
    private static final String WEBP = "image/webp";
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(JPEG, PNG, WEBP);

    @Override
    public void validate(ComponentImageUpload image, Integer orderIndex) {
        if (image == null || image.size() == 0) {
            throw new ValidationException("Image file must not be empty");
        }
        if (image.size() > MAX_IMAGE_SIZE_BYTES) {
            throw new ImageTooLargeException("Image file must not exceed 10 MiB");
        }
        if (orderIndex != null && orderIndex < 0) {
            throw new ValidationException("Image order index must not be negative");
        }
        if (!SUPPORTED_CONTENT_TYPES.contains(image.contentType())
                || !matchesContent(image.content(), image.contentType())) {
            throw new UnsupportedImageFormatException("Only JPEG, PNG, and WebP images are supported");
        }
    }

    private boolean matchesContent(byte[] content, String contentType) {
        return switch (contentType) {
            case JPEG -> startsWith(content, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case PNG -> startsWith(content, new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
            });
            case WEBP -> content.length >= 12
                    && new String(content, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(content, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            default -> false;
        };
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (content[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
