package ru.sultanyarov.configurator.it

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import ru.sultanyarov.configurator.application.port.out.ComponentImageStorage
import ru.sultanyarov.configurator.domain.exception.ExternalStorageException
import ru.sultanyarov.configurator.domain.model.ComponentImageContent
import ru.sultanyarov.configurator.domain.model.ComponentImageUpload
import ru.sultanyarov.configurator.domain.model.StoredImage

import ru.sultanyarov.configurator.infrastructure.storage.minio.ComponentImageThumbnailer

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@TestConfiguration
class ComponentImageStorageTestConfiguration {
    @Bean
    @Primary
    ComponentImageStorage componentImageStorage() {
        def sequence = new AtomicLong()
        def contents = new ConcurrentHashMap<String, ComponentImageContent>()
        return new ComponentImageStorage() {
            @Override
            StoredImage store(Long componentId, ComponentImageUpload image) {
                new ComponentImageThumbnailer().create(image.content())
                def extension = switch (image.contentType()) {
                    case "image/jpeg" -> "jpg"
                    case "image/png" -> "png"
                    case "image/webp" -> "webp"
                    default -> "bin"
                }
                String objectKey =
                        "components/${componentId}/test-${sequence.incrementAndGet()}.${extension}"
                contents.put(objectKey, new ComponentImageContent(image.content(), image.contentType()))
                return new StoredImage(objectKey)
            }

            @Override
            ComponentImageContent read(String objectKey) {
                def content = contents.get(objectKey)
                if (content == null) {
                    throw new ExternalStorageException(
                            new IllegalStateException("Missing in-memory object"),
                            "Failed to read component image from external storage"
                    )
                }
                return content
            }

            @Override
            ComponentImageContent readThumbnail(String objectKey) {
                return new ComponentImageThumbnailer().create(read(objectKey).content())
            }

            @Override
            void delete(String objectKey) {
                contents.remove(objectKey)
            }
        }
    }
}
