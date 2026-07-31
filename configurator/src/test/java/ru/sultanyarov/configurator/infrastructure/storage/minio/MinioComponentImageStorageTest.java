package ru.sultanyarov.configurator.infrastructure.storage.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.domain.exception.ExternalStorageException;
import ru.sultanyarov.configurator.domain.exception.UnsupportedImageFormatException;
import ru.sultanyarov.configurator.domain.model.ComponentImageUpload;
import ru.sultanyarov.configurator.domain.model.StoredImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioComponentImageStorageTest {
    @Mock
    private MinioClient minioClient;

    private MinioComponentImageStorage storage;

    @BeforeEach
    void setUp() {
        storage = new MinioComponentImageStorage(
                minioClient,
                new ComponentImageStorageProperties(
                        "http://minio:9000",
                        "access-key",
                        "secret-key",
                        "configurator-components",
                        "https://cdn.example.com/"
                )
        );
    }

    @Test
    void store_shouldCreateMissingBucketAndUploadObject() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        StoredImage result = storage.store(
                7L,
                new ComponentImageUpload(png(), "image/png")
        );

        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        ArgumentCaptor<PutObjectArgs> argsCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(argsCaptor.capture());
        PutObjectArgs args = argsCaptor.getValue();
        assertThat(args.bucket()).isEqualTo("configurator-components");
        assertThat(args.object()).startsWith("components/7/").endsWith(".png");
        assertThat(args.contentType().toString()).isEqualTo("image/png");
        assertThat(result.objectKey()).isEqualTo(args.object());
        assertThat(result.url())
                .isEqualTo("https://cdn.example.com/configurator-components/" + result.objectKey());
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

        assertThatThrownBy(() -> storage.store(
                7L,
                new ComponentImageUpload(png(), "image/png")
        ))
                .isInstanceOf(ExternalStorageException.class)
                .hasMessage("Failed to upload component image to external storage")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void store_shouldRejectUnsupportedContentTypeBeforeCallingMinio() {
        assertThatThrownBy(() -> storage.store(
                7L,
                new ComponentImageUpload(new byte[]{1}, "image/gif")
        ))
                .isInstanceOf(UnsupportedImageFormatException.class);
    }

    @Test
    void delete_shouldRemoveObject() throws Exception {
        storage.delete("components/7/image.webp");

        ArgumentCaptor<RemoveObjectArgs> argsCaptor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(argsCaptor.capture());
        assertThat(argsCaptor.getValue().bucket()).isEqualTo("configurator-components");
        assertThat(argsCaptor.getValue().object()).isEqualTo("components/7/image.webp");
    }

    private static byte[] png() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
    }
}
