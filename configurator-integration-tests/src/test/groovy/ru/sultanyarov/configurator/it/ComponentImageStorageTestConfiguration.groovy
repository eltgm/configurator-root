package ru.sultanyarov.configurator.it

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import ru.sultanyarov.configurator.application.port.out.ComponentImageStorage
import ru.sultanyarov.configurator.domain.model.ComponentImageUpload
import ru.sultanyarov.configurator.domain.model.StoredImage

import java.util.concurrent.atomic.AtomicLong

@TestConfiguration
class ComponentImageStorageTestConfiguration {
    @Bean
    @Primary
    ComponentImageStorage componentImageStorage() {
        def sequence = new AtomicLong()
        return new ComponentImageStorage() {
            @Override
            StoredImage store(Long componentId, ComponentImageUpload image) {
                def extension = switch (image.contentType()) {
                    case "image/jpeg" -> "jpg"
                    case "image/png" -> "png"
                    case "image/webp" -> "webp"
                    default -> "bin"
                }
                def objectKey = "components/${componentId}/test-${sequence.incrementAndGet()}.${extension}"
                return new StoredImage(
                        objectKey,
                        "http://storage.test/configurator-components/${objectKey}"
                )
            }

            @Override
            void delete(String objectKey) {
                // No external resource is allocated by this in-memory test adapter.
            }
        }
    }
}
