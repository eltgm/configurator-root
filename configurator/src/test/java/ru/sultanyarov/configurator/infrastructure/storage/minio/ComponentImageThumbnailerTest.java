package ru.sultanyarov.configurator.infrastructure.storage.minio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.exception.ImageTooLargeException;
import ru.sultanyarov.configurator.domain.exception.UnsupportedImageFormatException;

class ComponentImageThumbnailerTest {
  private final ComponentImageThumbnailer thumbnailer = new ComponentImageThumbnailer();

  @Test
  void shouldFitLandscapeAndPortraitWithoutCroppingAndPreserveAlpha() throws Exception {
    for (int[] size : new int[][] {{2048, 1024}, {1024, 2048}}) {
      BufferedImage original = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB);
      original.setRGB(0, 0, 0x00ffffff);
      var result = thumbnailer.create(encode(original, "png"));
      BufferedImage preview = ImageIO.read(new ByteArrayInputStream(result.content()));
      assertThat(preview.getWidth()).isEqualTo(size[0] / 4);
      assertThat(preview.getHeight()).isEqualTo(size[1] / 4);
      assertThat(preview.getColorModel().hasAlpha()).isTrue();
      assertThat(preview.getRGB(0, 0) >>> 24).isZero();
      assertThat(result.contentType()).isEqualTo("image/png");
    }
  }

  @Test
  void shouldDecodeJpegAndNotUpscaleSmallImages() throws Exception {
    var original = new BufferedImage(32, 16, BufferedImage.TYPE_INT_RGB);
    byte[] bytes = encode(original, "jpeg");
    byte[] unchanged = bytes.clone();
    var preview = ImageIO.read(new ByteArrayInputStream(thumbnailer.create(bytes).content()));
    assertThat(preview.getWidth()).isEqualTo(32);
    assertThat(preview.getHeight()).isEqualTo(16);
    assertThat(bytes).containsExactly(unchanged);
  }

  @Test
  void shouldDecodeWebpWithoutNativeLibraries() throws Exception {
    byte[] webp =
        Base64.getDecoder().decode("UklGRh4AAABXRUJQVlA4TBEAAAAvAAAAAAfQ//73v/+BiOh/AAA=");
    var preview = ImageIO.read(new ByteArrayInputStream(thumbnailer.create(webp).content()));
    assertThat(preview.getWidth()).isEqualTo(1);
    assertThat(preview.getHeight()).isEqualTo(1);
  }

  @Test
  void shouldRejectDamagedImagesAndOversizedDimensionsBeforeDecoding() throws Exception {
    assertThatThrownBy(() -> thumbnailer.create(new byte[] {1, 2, 3}))
        .isInstanceOf(UnsupportedImageFormatException.class);
    byte[] png = encode(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png");
    ByteBuffer.wrap(png).putInt(16, 100_000).putInt(20, 100_000);
    assertThatThrownBy(() -> thumbnailer.create(png)).isInstanceOf(ImageTooLargeException.class);
    assertThatThrownBy(() -> thumbnailer.create(java.util.Arrays.copyOf(png, 12)))
        .isInstanceOf(UnsupportedImageFormatException.class);
  }

  @Test
  void shouldApplyExifRotationAndMirroringAndStripTheMetadata() throws Exception {
    var original = new BufferedImage(32, 16, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < 16; y++) {
      for (int x = 0; x < 32; x++) original.setRGB(x, y, x < 16 ? 0xff0000 : 0x0000ff);
    }
    byte[] jpeg = encode(original, "jpeg");
    for (int orientation = 1; orientation <= 8; orientation++) {
      byte[] exif =
          new byte[] {
            (byte) 0xff,
            (byte) 0xe1,
            0,
            34,
            'E',
            'x',
            'i',
            'f',
            0,
            0,
            'I',
            'I',
            42,
            0,
            8,
            0,
            0,
            0,
            1,
            0,
            0x12,
            1,
            3,
            0,
            1,
            0,
            0,
            0,
            (byte) orientation,
            0,
            0,
            0,
            0,
            0,
            0,
            0
          };
      var input = new ByteArrayOutputStream();
      input.write(jpeg, 0, 2);
      input.write(exif);
      input.write(jpeg, 2, jpeg.length - 2);
      var result = thumbnailer.create(input.toByteArray());
      var preview = ImageIO.read(new ByteArrayInputStream(result.content()));
      assertThat(preview.getWidth()).isEqualTo(orientation >= 5 ? 16 : 32);
      assertThat(preview.getHeight()).isEqualTo(orientation >= 5 ? 32 : 16);
      int corner = preview.getRGB(2, 2);
      boolean redFirst =
          orientation == 1 || orientation == 4 || orientation == 5 || orientation == 6;
      assertThat((corner >> 16 & 255) > (corner & 255)).isEqualTo(redFirst);
    }
  }

  private static byte[] encode(BufferedImage image, String format) throws Exception {
    var output = new ByteArrayOutputStream();
    ImageIO.write(image, format, output);
    return output.toByteArray();
  }
}
