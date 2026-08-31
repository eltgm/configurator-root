package ru.sultanyarov.configurator.infrastructure.storage.minio;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import java.io.ByteArrayInputStream;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sultanyarov.configurator.application.port.out.ComponentImageStorage;
import ru.sultanyarov.configurator.domain.exception.ExternalStorageException;
import ru.sultanyarov.configurator.domain.exception.UnsupportedImageFormatException;
import ru.sultanyarov.configurator.domain.model.ComponentImageContent;
import ru.sultanyarov.configurator.domain.model.ComponentImageUpload;
import ru.sultanyarov.configurator.domain.model.StoredImage;

@Component
@RequiredArgsConstructor
public class MinioComponentImageStorage implements ComponentImageStorage {
  private static final Set<String> SUPPORTED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  private final MinioClient minioClient;
  private final ComponentImageStorageProperties properties;

  private final ComponentImageThumbnailer thumbnailer = new ComponentImageThumbnailer();
  private volatile boolean bucketReady;

  @Override
  public StoredImage store(Long componentId, ComponentImageUpload image) {
    String objectKey =
        "components/%d/%s.%s"
            .formatted(componentId, UUID.randomUUID(), extensionFor(image.contentType()));
    byte[] content = image.content();
    ComponentImageContent thumbnail = thumbnailer.create(content);

    try {
      ensureBucket();
      put(objectKey, new ComponentImageContent(content, image.contentType()));
      try {
        put(thumbnailKey(objectKey), thumbnail);
      } catch (Exception exception) {
        try {
          delete(objectKey);
        } catch (RuntimeException cleanupException) {
          exception.addSuppressed(cleanupException);
        }
        throw exception;
      }
      return new StoredImage(objectKey);
    } catch (Exception exception) {
      throw new ExternalStorageException(
          exception, "Failed to upload component image to external storage");
    }
  }

  @Override
  public ComponentImageContent read(String objectKey) {
    try (GetObjectResponse response =
        minioClient.getObject(
            GetObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build())) {
      String contentType = response.headers().get("Content-Type");
      if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
        throw new IllegalStateException(
            "Object storage returned unsupported content type for component image");
      }
      return new ComponentImageContent(response.readAllBytes(), contentType);
    } catch (Exception exception) {
      throw new ExternalStorageException(
          exception, "Failed to read component image from external storage");
    }
  }

  @Override
  public void delete(String objectKey) {
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder()
              .bucket(properties.bucket())
              .object(thumbnailKey(objectKey))
              .build());
      minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());
    } catch (Exception exception) {
      throw new ExternalStorageException(
          exception, "Failed to remove component image from external storage");
    }
  }

  @Override
  public ComponentImageContent readThumbnail(String objectKey) {
    try {
      try (GetObjectResponse response =
          minioClient.getObject(
              GetObjectArgs.builder()
                  .bucket(properties.bucket())
                  .object(thumbnailKey(objectKey))
                  .build())) {
        if (!"image/png".equals(response.headers().get("Content-Type"))) {
          throw new IllegalStateException("Unexpected thumbnail content type");
        }
        return new ComponentImageContent(response.readAllBytes(), "image/png");
      } catch (ErrorResponseException exception) {
        if (!"NoSuchKey".equals(exception.errorResponse().code())) {
          throw exception;
        }
        ComponentImageContent thumbnail = thumbnailer.create(read(objectKey).content());
        put(thumbnailKey(objectKey), thumbnail);
        return thumbnail;
      }
    } catch (Exception exception) {
      throw new ExternalStorageException(
          exception, "Failed to read component image thumbnail from external storage");
    }
  }

  private static String thumbnailKey(String objectKey) {
    return objectKey + ".thumbnail-v1.png";
  }

  private void put(String objectKey, ComponentImageContent image) throws Exception {
    byte[] content = image.content();
    minioClient.putObject(
        PutObjectArgs.builder().bucket(properties.bucket()).object(objectKey).stream(
                new ByteArrayInputStream(content), (long) content.length, -1L)
            .contentType(image.contentType())
            .build());
  }

  private void ensureBucket() throws Exception {
    if (bucketReady) {
      return;
    }

    synchronized (this) {
      if (bucketReady) {
        return;
      }
      BucketExistsArgs bucketExistsArgs =
          BucketExistsArgs.builder().bucket(properties.bucket()).build();
      if (!minioClient.bucketExists(bucketExistsArgs)) {
        try {
          minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
        } catch (Exception exception) {
          if (!minioClient.bucketExists(bucketExistsArgs)) {
            throw exception;
          }
        }
      }
      bucketReady = true;
    }
  }

  private String extensionFor(String contentType) {
    return switch (contentType) {
      case "image/jpeg" -> "jpg";
      case "image/png" -> "png";
      case "image/webp" -> "webp";
      default ->
          throw new UnsupportedImageFormatException(
              "Only JPEG, PNG, and WebP images are supported");
    };
  }
}
