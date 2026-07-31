package ru.sultanyarov.configurator.infrastructure.storage.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sultanyarov.configurator.application.port.out.ComponentImageStorage;
import ru.sultanyarov.configurator.domain.exception.ExternalStorageException;
import ru.sultanyarov.configurator.domain.exception.UnsupportedImageFormatException;
import ru.sultanyarov.configurator.domain.model.ComponentImageUpload;
import ru.sultanyarov.configurator.domain.model.StoredImage;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MinioComponentImageStorage implements ComponentImageStorage {
    private final MinioClient minioClient;
    private final ComponentImageStorageProperties properties;

    private volatile boolean bucketReady;

    @Override
    public StoredImage store(Long componentId, ComponentImageUpload image) {
        String objectKey = "components/%d/%s.%s".formatted(
                componentId,
                UUID.randomUUID(),
                extensionFor(image.contentType())
        );
        byte[] content = image.content();

        try {
            ensureBucket();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(content), (long) content.length, -1L)
                            .contentType(image.contentType())
                            .build()
            );
            return new StoredImage(objectKey, publicUrl(objectKey));
        } catch (Exception exception) {
            throw new ExternalStorageException(
                    exception,
                    "Failed to upload component image to external storage"
            );
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception exception) {
            throw new ExternalStorageException(
                    exception,
                    "Failed to remove component image from external storage"
            );
        }
    }

    private void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }

        synchronized (this) {
            if (bucketReady) {
                return;
            }
            BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder()
                    .bucket(properties.bucket())
                    .build();
            if (!minioClient.bucketExists(bucketExistsArgs)) {
                try {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder()
                                    .bucket(properties.bucket())
                                    .build()
                    );
                } catch (Exception exception) {
                    if (!minioClient.bucketExists(bucketExistsArgs)) {
                        throw exception;
                    }
                }
            }
            bucketReady = true;
        }
    }

    private String publicUrl(String objectKey) {
        String baseUrl = properties.publicUrl().endsWith("/")
                ? properties.publicUrl().substring(0, properties.publicUrl().length() - 1)
                : properties.publicUrl();
        return "%s/%s/%s".formatted(baseUrl, properties.bucket(), objectKey);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new UnsupportedImageFormatException(
                    "Only JPEG, PNG, and WebP images are supported"
            );
        };
    }
}
