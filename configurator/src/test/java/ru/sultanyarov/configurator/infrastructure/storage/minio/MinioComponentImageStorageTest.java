package ru.sultanyarov.configurator.infrastructure.storage.minio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.domain.exception.ExternalStorageException;
import ru.sultanyarov.configurator.domain.exception.UnsupportedImageFormatException;
import ru.sultanyarov.configurator.domain.model.ComponentImageContent;
import ru.sultanyarov.configurator.domain.model.ComponentImageUpload;
import ru.sultanyarov.configurator.domain.model.StoredImage;

@ExtendWith(MockitoExtension.class)
class MinioComponentImageStorageTest {
  @Mock private MinioClient minioClient;

  private MinioComponentImageStorage storage;

  @BeforeEach
  void setUp() {
    storage =
        new MinioComponentImageStorage(
            minioClient,
            new ComponentImageStorageProperties(
                "http://minio:9000", "access-key", "secret-key", "configurator-components"));
  }

  @Test
  void store_shouldCreateMissingBucketAndUploadObject() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

    StoredImage result = storage.store(7L, new ComponentImageUpload(png(), "image/png"));

    verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    ArgumentCaptor<PutObjectArgs> argsCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);
    verify(minioClient, times(2)).putObject(argsCaptor.capture());
    PutObjectArgs args = argsCaptor.getAllValues().getFirst();
    assertThat(argsCaptor.getAllValues().getLast().object())
        .isEqualTo(args.object() + ".thumbnail-v1.png");
    assertThat(args.bucket()).isEqualTo("configurator-components");
    assertThat(args.object()).startsWith("components/7/").endsWith(".png");
    assertThat(args.contentType().toString()).isEqualTo("image/png");
    assertThat(result.objectKey()).isEqualTo(args.object());
  }

  @Test
  void read_shouldReturnOriginalBytesAndContentType() throws Exception {
    byte[] content = png();
    GetObjectResponse response =
        new GetObjectResponse(
            new Headers.Builder().add("Content-Type", "image/png").build(),
            "configurator-components",
            null,
            "components/7/image.png",
            new ByteArrayInputStream(content));
    when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

    ComponentImageContent result = storage.read("components/7/image.png");

    ArgumentCaptor<GetObjectArgs> argsCaptor = ArgumentCaptor.forClass(GetObjectArgs.class);
    verify(minioClient).getObject(argsCaptor.capture());
    assertThat(argsCaptor.getValue().bucket()).isEqualTo("configurator-components");
    assertThat(argsCaptor.getValue().object()).isEqualTo("components/7/image.png");
    assertThat(result.content()).containsExactly(content);
    assertThat(result.contentType()).isEqualTo("image/png");
  }

  @Test
  void read_shouldWrapStorageFailure() throws Exception {
    when(minioClient.getObject(any(GetObjectArgs.class)))
        .thenThrow(new IllegalStateException("unavailable"));

    assertThatThrownBy(() -> storage.read("components/7/image.png"))
        .isInstanceOf(ExternalStorageException.class)
        .hasMessage("Failed to read component image from external storage")
        .hasCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void read_shouldRejectUnexpectedStoredContentType() throws Exception {
    GetObjectResponse response =
        new GetObjectResponse(
            new Headers.Builder().add("Content-Type", "application/octet-stream").build(),
            "configurator-components",
            null,
            "components/7/image.png",
            new ByteArrayInputStream(png()));
    when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

    assertThatThrownBy(() -> storage.read("components/7/image.png"))
        .isInstanceOf(ExternalStorageException.class)
        .hasCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void store_shouldReuseExistingBucketWithoutCreatingIt() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

    storage.store(7L, new ComponentImageUpload(png(), "image/png"));
    storage.store(7L, new ComponentImageUpload(png(), "image/png"));

    verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    verify(minioClient).bucketExists(any(BucketExistsArgs.class));
  }

  @Test
  void store_shouldWrapStorageFailure() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class)))
        .thenThrow(new IllegalStateException("unavailable"));

    assertThatThrownBy(() -> storage.store(7L, new ComponentImageUpload(png(), "image/png")))
        .isInstanceOf(ExternalStorageException.class)
        .hasMessage("Failed to upload component image to external storage")
        .hasCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void store_shouldRejectUnsupportedContentTypeBeforeCallingMinio() {
    assertThatThrownBy(
            () -> storage.store(7L, new ComponentImageUpload(new byte[] {1}, "image/gif")))
        .isInstanceOf(UnsupportedImageFormatException.class);
  }

  @Test
  void delete_shouldRemoveObject() throws Exception {
    storage.delete("components/7/image.webp");

    ArgumentCaptor<RemoveObjectArgs> argsCaptor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
    verify(minioClient, times(2)).removeObject(argsCaptor.capture());
    assertThat(argsCaptor.getAllValues().getFirst().object())
        .isEqualTo("components/7/image.webp.thumbnail-v1.png");
    assertThat(argsCaptor.getValue().bucket()).isEqualTo("configurator-components");
    assertThat(argsCaptor.getValue().object()).isEqualTo("components/7/image.webp");
  }

  @Test
  void thumbnail_shouldUseCachedObjectWithoutReadingOriginal() throws Exception {
    when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response(png()));
    assertThat(storage.readThumbnail("components/7/image.png").content()).containsExactly(png());
    var captor = ArgumentCaptor.forClass(GetObjectArgs.class);
    verify(minioClient).getObject(captor.capture());
    assertThat(captor.getValue().object()).endsWith(".thumbnail-v1.png");
    verify(minioClient, never()).putObject(any(PutObjectArgs.class));
  }

  @Test
  void thumbnail_shouldGenerateLegacyCopyOnlyForMissingKey() throws Exception {
    var missing = org.mockito.Mockito.mock(io.minio.errors.ErrorResponseException.class);
    var error = org.mockito.Mockito.mock(io.minio.messages.ErrorResponse.class);
    when(missing.errorResponse()).thenReturn(error);
    when(error.code()).thenReturn("NoSuchKey");
    when(minioClient.getObject(any(GetObjectArgs.class)))
        .thenThrow(missing)
        .thenReturn(response(png()));
    var result = storage.readThumbnail("components/7/image.png");
    assertThat(result.contentType()).isEqualTo("image/png");
    var putCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);
    verify(minioClient).putObject(putCaptor.capture());
    assertThat(putCaptor.getValue().object()).isEqualTo("components/7/image.png.thumbnail-v1.png");
  }

  @Test
  void thumbnail_shouldNotRegenerateOnStorageOutage() throws Exception {
    when(minioClient.getObject(any(GetObjectArgs.class)))
        .thenThrow(new IllegalStateException("unavailable"));
    assertThatThrownBy(() -> storage.readThumbnail("components/7/image.png"))
        .isInstanceOf(ExternalStorageException.class);
    verify(minioClient, never()).putObject(any(PutObjectArgs.class));
  }

  @Test
  void store_shouldRemoveBothObjectsWhenThumbnailUploadFails() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    when(minioClient.putObject(any(PutObjectArgs.class)))
        .thenReturn(null)
        .thenThrow(new IllegalStateException("unavailable"));
    assertThatThrownBy(() -> storage.store(7L, new ComponentImageUpload(png(), "image/png")))
        .isInstanceOf(ExternalStorageException.class);
    verify(minioClient, times(2)).removeObject(any(RemoveObjectArgs.class));
  }

  private static GetObjectResponse response(byte[] bytes) {
    return new GetObjectResponse(
        new Headers.Builder().add("Content-Type", "image/png").build(),
        "configurator-components",
        null,
        "image",
        new ByteArrayInputStream(bytes));
  }

  private static byte[] png() {
    return java.util.Base64.getDecoder()
        .decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
  }
}
