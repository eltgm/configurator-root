package ru.sultanyarov.configurator.infrastructure.storage.minio;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import ru.sultanyarov.configurator.domain.exception.ImageTooLargeException;
import ru.sultanyarov.configurator.domain.exception.UnsupportedImageFormatException;
import ru.sultanyarov.configurator.domain.model.ComponentImageContent;

/** Produces bounded, metadata-free PNG previews without changing the stored original. */
public final class ComponentImageThumbnailer {
  public static final int MAX_SIDE = 512;
  private static final long MAX_PIXELS = 40_000_000;

  public ComponentImageContent create(byte[] content) {
    try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw new UnsupportedImageFormatException("Image cannot be decoded");
      }
      ImageReader reader = readers.next();
      try {
        reader.setInput(input, true, true);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
          throw new ImageTooLargeException("Decoded image must not exceed 40 megapixels");
        }
        var parameters = reader.getDefaultReadParam();
        int sample = Math.max(1, Math.max(width, height) / (MAX_SIDE * 2));
        parameters.setSourceSubsampling(sample, sample, 0, 0);
        BufferedImage source = reader.read(0, parameters);
        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(width, height));
        BufferedImage preview =
            new BufferedImage(
                Math.max(1, (int) Math.round(width * scale)),
                Math.max(1, (int) Math.round(height * scale)),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = preview.createGraphics();
        try {
          graphics.setRenderingHint(
              RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
          graphics.drawImage(source, 0, 0, preview.getWidth(), preview.getHeight(), null);
        } finally {
          graphics.dispose();
          source.flush();
        }
        preview = orient(preview, orientation(content));
        var output = new ByteArrayOutputStream();
        ImageIO.write(preview, "png", output);
        preview.flush();
        return new ComponentImageContent(output.toByteArray(), "image/png");
      } finally {
        reader.dispose();
      }
    } catch (ImageTooLargeException | UnsupportedImageFormatException exception) {
      throw exception;
    } catch (IOException | RuntimeException exception) {
      throw new UnsupportedImageFormatException("Image is damaged or cannot be decoded");
    }
  }

  private static int orientation(byte[] content) {
    try {
      var metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(content));
      var exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
      Integer value = exif == null ? null : exif.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
      return value == null ? 1 : value;
    } catch (ImageProcessingException | IOException exception) {
      // Optional metadata must not prevent a valid image from being displayed.
      return 1;
    }
  }

  private static BufferedImage orient(BufferedImage source, int orientation) {
    if (orientation < 2 || orientation > 8) return source;
    int width = source.getWidth();
    int height = source.getHeight();
    var target =
        new BufferedImage(
            orientation >= 5 ? height : width,
            orientation >= 5 ? width : height,
            BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int targetX =
            switch (orientation) {
              case 2, 3 -> width - 1 - x;
              case 5, 8 -> y;
              case 6, 7 -> height - 1 - y;
              default -> x;
            };
        int targetY =
            switch (orientation) {
              case 3, 4 -> height - 1 - y;
              case 5, 6 -> x;
              case 7, 8 -> width - 1 - x;
              default -> y;
            };
        target.setRGB(targetX, targetY, source.getRGB(x, y));
      }
    }
    source.flush();
    return target;
  }
}
